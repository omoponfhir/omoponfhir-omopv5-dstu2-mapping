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
package edu.gatech.chai.omoponfhir.omopv5.stu3.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

//import org.hl7.fhir.dstu3.model.IdType;
import ca.uhn.fhir.model.primitive.IdDt;
//import org.hl7.fhir.dstu3.model.Observation;
import ca.uhn.fhir.model.dstu2.resource.Observation;
//import org.hl7.fhir.dstu3.model.Reference;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
//import org.hl7.fhir.dstu3.model.Resource;
import ca.uhn.fhir.model.dstu2.resource.BaseResource;
//import org.hl7.fhir.dstu3.model.ResourceType;
import ca.uhn.fhir.model.dstu2.valueset.ResourceTypeEnum;
//import org.hl7.fhir.exceptions.FHIRException;
import edu.gatech.chai.omoponfhir.omopv5.stu3.utilities.FHIRException;
//import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import ca.uhn.fhir.model.dstu2.resource.Bundle.Entry;
//import org.hl7.fhir.dstu3.model.Bundle.BundleEntryResponseComponent;
import ca.uhn.fhir.model.dstu2.resource.Bundle.EntryResponse;
import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import edu.gatech.chai.omoponfhir.omopv5.stu3.model.USCorePatient;
import edu.gatech.chai.omoponfhir.omopv5.stu3.utilities.ExtensionUtil;
import edu.gatech.chai.omopv5.dba.service.FPersonService;
import edu.gatech.chai.omopv5.dba.service.MeasurementService;
import edu.gatech.chai.omopv5.dba.service.ObservationService;
import edu.gatech.chai.omopv5.model.entity.FPerson;

public class OmopServerOperations {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OmopServerOperations.class);

	private static OmopTransaction omopTransaction = new OmopTransaction();
	private FPersonService fPersonService;
	private ObservationService observationService;
	private MeasurementService measurementService;

	public OmopServerOperations(WebApplicationContext context) {
		initialize(context);
	}

	public OmopServerOperations() {
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}

	private void initialize(WebApplicationContext context) {
		fPersonService = context.getBean(FPersonService.class);
		observationService = context.getBean(ObservationService.class);
		measurementService = context.getBean(MeasurementService.class);
	}

	public static OmopTransaction getInstance() {
		return omopTransaction;
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

	public Entry addResponseEntry(String status, String location) {
		Entry entryBundle = new Entry();
		UUID uuid = UUID.randomUUID();
		entryBundle.setFullUrl("urn:uuid:" + uuid.toString());
		EntryResponse responseBundle = new EntryResponse();
		responseBundle.setStatus(status);
		if (location != null)
			responseBundle.setLocation(location);
		entryBundle.setResponse(responseBundle);

		return entryBundle;
	}

	public List<Entry> createEntries(List<BaseResource> resources) throws FHIRException {
		List<Entry> responseEntries = new ArrayList<Entry>();
		Map<String, Long> patientMap = new HashMap<String, Long>();

		// do patient first.
		for (BaseResource resource : resources) {
			if (resource.getResourceType() == ResourceTypeEnum.PATIENT) {
				String originalId = resource.getId();
				Long fhirId = OmopPatient.getInstance().toDbase(ExtensionUtil.usCorePatientFromResource(resource),
						null);
				patientMap.put(originalId, fhirId);
				logger.debug("Adding patient info to patientMap " + originalId + "->" + fhirId);
				responseEntries.add(addResponseEntry("201 Created", "Patient/" + fhirId));
			}
		}

		// Now process the rest.
		for (BaseResource resource : resources) {
			if (resource.getResourceType() == ResourceTypeEnum.PATIENT) {
				// already done.
				continue;
			}

			if (resource.getResourceType() == ResourceTypeEnum.OBSERVATION) {
				Observation observation = (Observation) resource;
				ResourceReferenceDt subject = observation.getSubject();
				IdDt refIdType = linkToPatient(subject, patientMap);
				if (refIdType == null)
					continue;
				observation.setSubject(new ResourceReferenceDt(refIdType));

				logger.debug("Setting patient to Obs: "+observation.getSubject().getReference());
				Long fhirId = OmopObservation.getInstance().toDbase(observation, null);
				Entry newEntry;
				if (fhirId == null || fhirId == 0L) {
					newEntry = addResponseEntry("400 Bad Request", null);
					newEntry.setResource(observation);
				} else {
					newEntry = addResponseEntry("201 Created", "Observation/" + fhirId);
				}

				responseEntries.add(newEntry);
			}
		}

		return responseEntries;
	}
}
