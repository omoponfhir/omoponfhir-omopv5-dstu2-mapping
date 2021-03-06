/*******************************************************************************
 * Copyright (c) 2019 Georgia Tech Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package edu.gatech.chai.omoponfhir.omopv5.dstu2.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ca.uhn.fhir.model.dstu2.resource.BaseResource;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle.Entry;
import ca.uhn.fhir.model.dstu2.resource.Bundle.EntryResponse;
import ca.uhn.fhir.model.dstu2.valueset.HTTPVerbEnum;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;

import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.hl7.fhir.exceptions.FHIRException;

import edu.gatech.chai.omoponfhir.omopv5.dstu2.model.USCorePatient;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.PatientResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.utilities.ExtensionUtil;
import edu.gatech.chai.omopv5.dba.service.FPersonService;
import edu.gatech.chai.omopv5.dba.service.MeasurementService;
import edu.gatech.chai.omopv5.dba.service.ObservationService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.dba.service.TransactionService;
import edu.gatech.chai.omopv5.model.entity.BaseEntity;
import edu.gatech.chai.omopv5.model.entity.FPerson;
import edu.gatech.chai.omopv5.model.entity.Measurement;

public class OmopTransaction {

	private static OmopTransaction omopTransaction = new OmopTransaction();
	private TransactionService myService;
	private FPersonService fPersonService;
	private ObservationService observationService;
	private MeasurementService measurementService;
	private WebApplicationContext myContext;

	public OmopTransaction(WebApplicationContext context) {
		this.myContext = context;
		initialize(context);
	}

	public OmopTransaction() {
		this.myContext = ContextLoaderListener.getCurrentWebApplicationContext();
		initialize(this.myContext);
	}

	private void initialize(WebApplicationContext context) {
		myService = context.getBean(TransactionService.class);
		fPersonService = context.getBean(FPersonService.class);
		observationService = context.getBean(ObservationService.class);
		measurementService = context.getBean(MeasurementService.class);
	}

	public static OmopTransaction getInstance() {
		return omopTransaction;
	}

	private void addBaseEntity(Map<String, List<BaseEntity>> entityToCreate, String key, BaseEntity entity) {
		if (key == null)
			key = "";

		List<BaseEntity> list = entityToCreate.get(key);
		if (list == null) {
			list = new ArrayList<BaseEntity>();
			entityToCreate.put(key, list);
		}
		list.add(entity);
	}

	private IdDt linkToPatient(ResourceReferenceDt subject, Map<String, Long> patientMap) {
		if (subject == null || subject.isEmpty()) {
			// We must have subject information to link this to patient.
			// This is OMOP requirement. We skip this for Transaction Messages.
			return null;
		}

		Long fhirId = patientMap.get(subject.getReference());
		if (fhirId == null || fhirId == 0L) {
			// See if we have this patient in OMOP DB.
			IIdType referenceIdType = subject.getReferenceElement();
			if (referenceIdType == null || referenceIdType.isEmpty()) {
				// Giving up...
				return null;
			}
			try {
				fhirId = referenceIdType.getIdPartAsLong();
			} catch (Exception e) {
				// Giving up...
				return null;
			}
			if (fhirId == null || fhirId == 0L) {
				// giving up again...
				return null;
			}
			Long omopId = IdMapping.getOMOPfromFHIR(fhirId, referenceIdType.getResourceType());
			if (omopId == null || omopId == 0L) {
				// giving up... :(
				return null;
			}
			FPerson refFPerson = fPersonService.findById(omopId);
			if (refFPerson == null) {
				// giving up...
				return null;
			}
			return new IdDt("Patient", refFPerson.getId());
		} else {
			return new IdDt("Patient", fhirId);
		}
	}

	public void addResponseEntry(List<Entry> responseEntries, String status, String location) {
		Entry entryBundle = new Entry();
		UUID uuid = UUID.randomUUID();
		entryBundle.setFullUrl("urn:uuid:" + uuid.toString());
		EntryResponse responseBundle = new EntryResponse();
		responseBundle.setStatus(status);
		if (location != null)
			responseBundle.setLocation(location);
		entryBundle.setResponse(responseBundle);
		responseEntries.add(entryBundle);
	}

	public List<Entry> executeRequests(Map<HTTPVerbEnum, Object> entries) throws FHIRException {
		List<Entry> responseEntries = new ArrayList<Entry>();

		List<BaseResource> postList = (List<BaseResource>) entries.get(HTTPVerbEnum.POST);
		List<BaseResource> putList = (List<BaseResource>) entries.get(HTTPVerbEnum.PUT);
		List<BaseResource> getList = (List<BaseResource>) entries.get(HTTPVerbEnum.GET);

		Map<String, Long> patientMap = new HashMap<String, Long>();

		// do patient first.
		for (BaseResource resource : postList) {
//			if (resource.getResourceType() == ResourceTypeEnum.PATIENT) {
			if (resource instanceof Patient) {
				String originalId = resource.getId().getValue();

				Long fhirId = OmopPatient.getInstance().toDbase(ExtensionUtil.usCorePatientFromResource(resource),
						null);
//				OmopPatient patientMappingInstance = new OmopPatient(myContext);
//				FPerson fPerson = patientMappingInstance.constructOmop(null, (Patient) resource);
//				FPerson retFPerson = fPersonService.create(fPerson);
//				Long fhirId = IdMapping.getFHIRfromOMOP(retFPerson.getIdAsLong(), PatientResourceProvider.getType());
				patientMap.put(originalId, fhirId);
				System.out.println("Adding patient info to patientMap " + originalId + "->" + fhirId);
				addResponseEntry(responseEntries, "201 Created", "Patient/" + fhirId);
			}
		}

		// Now process the rest.
		for (BaseResource resource : postList) {
//			if (resource.getResourceType() == ResourceTypeEnum.PATIENT) {
			if (resource instanceof Patient) {
				// already done.
				continue;
			}

//			if (resource.getResourceType() == ResourceTypeEnum.OBSERVATION) {
			if (resource instanceof Observation) {
				Observation observation = (Observation) resource;
				ResourceReferenceDt subject = observation.getSubject();
				IdDt refIdType = linkToPatient(subject, patientMap);
				if (refIdType == null)
					continue;
				observation.setSubject(new ResourceReferenceDt(refIdType));

				Long fhirId = OmopObservation.getInstance().toDbase(observation, null);
				if (fhirId == null)
					addResponseEntry(responseEntries, "400 Bad Request", null);
				else
					addResponseEntry(responseEntries, "201 Created", "Observation/" + fhirId);

			}
		}

		for (BaseResource resource : putList) {
//			if (resource.getResourceType() == ResourceTypeEnum.PATIENT) {
			if (resource instanceof Patient) {
				// This is PUT. We must have fhirId that we want to update.
				USCorePatient patient = ExtensionUtil.usCorePatientFromResource(resource);
				IdDt fhirIdType = patient.getId();
				Long fhirId = OmopPatient.getInstance().toDbase(patient, fhirIdType);
				patientMap.put(resource.getId().getValue(), fhirId);

				addResponseEntry(responseEntries, "201 Created", "Patient/" + fhirId);
			}
//			else if (resource.getResourceType() == ResourceTypeEnum.OBSERVATION) {
				else if (resource instanceof Observation) {
				Observation observation = (Observation) resource;
				ResourceReferenceDt subject = observation.getSubject();
				IdDt refIdType = linkToPatient(subject, patientMap);
				if (refIdType == null)
					continue;
				observation.setSubject(new ResourceReferenceDt(refIdType));

				IdDt fhirIdType = observation.getId();
				Long fhirId = OmopObservation.getInstance().toDbase(observation, fhirIdType);

				addResponseEntry(responseEntries, "201 Created", "Observation/" + fhirId);
			}
		}

		// TODO: HTTPVerb.GET must be implemented here.

		return responseEntries;
	}

	/**
	 * 
	 * @param entries
	 * @return
	 * @throws FHIRException
	 * 
	 *                       Transaction type of Transaction operation is atomic. If
	 *                       one fails, all should be dropped. Thus, we can't
	 *                       process one entry at a time. We should do this in one
	 *                       JPA transaction so that if one failed, all can be
	 *                       rolled back.
	 */
	public List<Entry> executeTransaction(Map<HTTPVerbEnum, Object> entries) throws FHIRException {
		List<Entry> responseEntries = new ArrayList<Entry>();
		Map<String, List<BaseEntity>> entityToCreate = new HashMap<String, List<BaseEntity>>();

		/*
		 * POST Transaction.
		 */
		@SuppressWarnings("unchecked")
		List<BaseResource> postList = (List<BaseResource>) entries.get(HTTPVerbEnum.POST);
		String keyString;
		for (BaseResource resource : postList) {
			if (resource instanceof Patient) {
				FPerson fPerson = OmopPatient.getInstance().constructOmop(null,
						ExtensionUtil.usCorePatientFromResource(resource));
				keyString = resource.getId() + "^FPerson";
				addBaseEntity(entityToCreate, keyString, fPerson);
				System.out.println("key:" + keyString + ", fPerson");
			}

			else if (resource instanceof Observation) {
				Observation observation = (Observation) resource;
				Map<String, Object> obsEntityMap = OmopObservation.getInstance()
						.constructOmopMeasurementObservation(null, observation);
				if (obsEntityMap != null && !obsEntityMap.isEmpty()) {
					if (((String) obsEntityMap.get("type")).equalsIgnoreCase("Measurement")) {
						keyString = observation.getSubject().getReference() + "^Measurement";
						List<Measurement> measurements = (List<Measurement>) obsEntityMap.get("entity");
						for (Measurement measurement : measurements) {
							addBaseEntity(entityToCreate, keyString, measurement);
						}
					} else {
						keyString = observation.getSubject().getReference() + "^Observation";
						edu.gatech.chai.omopv5.model.entity.Observation omopObservation = (edu.gatech.chai.omopv5.model.entity.Observation) obsEntityMap
								.get("entity");
						addBaseEntity(entityToCreate, keyString, omopObservation);
					}
				} else {
					return null;
				}
			}
		}

		List<Entry> retVal = new ArrayList<Entry>();
		if (entityToCreate.size() > 0) {
			int performStatus = myService.writeTransaction(entityToCreate);
			if (performStatus < 0) {
				// This is an error.
				// TODO: respond accordingly
				return retVal;
			}
			for (String myKeyString : entityToCreate.keySet()) {
				List<BaseEntity> entities = entityToCreate.get(myKeyString);
				String[] myKeyInfo = myKeyString.split("\\^");
				if (myKeyInfo.length != 2) {
					return null;
				}
				System.out.println("About to check response from JPA transaction");
				String entityName = myKeyInfo[1];
				for (BaseEntity entity : entities) {
					Entry bundleEntryComponent = new Entry();
					BaseResource fhirResource;
					if (entityName.equals("FPerson")) {
						// This is Person table.
						// Constructing FHIR to respond.
						System.out.println("Created FPerson ID: " + entity.getIdAsLong());
						fhirResource = OmopPatient.getInstance().constructFHIR(
								IdMapping.getFHIRfromOMOP(entity.getIdAsLong(), PatientResourceProvider.getType()),
								(FPerson) entity);
						bundleEntryComponent.setResource(fhirResource);
						// It was success full, so we return 201 Created.
//						EntryResponse responseComponent = new EntryResponse(new StringDt("201 Created"));
						EntryResponse responseComponent = new EntryResponse();
						responseComponent.setStatus(new StringDt("201 Created"));
						responseComponent.setLocation(PatientResourceProvider.getType() + "/" + fhirResource.getId());
						bundleEntryComponent.setResponse(responseComponent);
						retVal.add(bundleEntryComponent);
					} else if (entityName.equals("Measurement")) {

					}
				}
			}
		}
		return retVal;
	}

	public List<ParameterWrapper> mapParameter(String parameter, Object value) {
		// TODO Auto-generated method stub
		return null;
	}
}
