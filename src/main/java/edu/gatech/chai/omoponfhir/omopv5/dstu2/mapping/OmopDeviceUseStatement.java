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
import java.util.Arrays;
import java.util.Date;
import java.util.List;

//import org.hl7.fhir.dstu3.model.CodeableConcept;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.*;
import ca.uhn.fhir.model.dstu2.resource.Device;
import ca.uhn.fhir.model.dstu2.resource.DeviceUseStatement;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.resource.BaseResource;
import ca.uhn.fhir.model.dstu2.valueset.ResourceTypeEnum;

import org.hl7.fhir.instance.model.api.IIdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.hl7.fhir.exceptions.FHIRException;

import ca.uhn.fhir.rest.param.TokenParam;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.model.MyDevice;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.model.MyDeviceUseStatement;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.DeviceUseStatementResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.PatientResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.PractitionerResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.utilities.CodeableConceptUtil;
import edu.gatech.chai.omopv5.dba.service.ConceptService;
import edu.gatech.chai.omopv5.dba.service.DeviceExposureService;
import edu.gatech.chai.omopv5.dba.service.FPersonService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.dba.service.ProviderService;
import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.model.entity.DeviceExposure;
import edu.gatech.chai.omopv5.model.entity.FPerson;
import edu.gatech.chai.omopv5.model.entity.Provider;

public class OmopDeviceUseStatement extends BaseOmopResource<MyDeviceUseStatement, DeviceExposure, DeviceExposureService>
		implements IResourceMapping<MyDeviceUseStatement, DeviceExposure> {
	
	private static final Logger logger = LoggerFactory.getLogger(OmopDeviceUseStatement.class);
	private static OmopDeviceUseStatement omopDeviceUseStatement = new OmopDeviceUseStatement();

	private ConceptService conceptService;
	private FPersonService fPersonService;
	private ProviderService providerService;

	public OmopDeviceUseStatement(WebApplicationContext context) {
		super(context, DeviceExposure.class, DeviceExposureService.class, DeviceUseStatementResourceProvider.getType());
		initialize(context);
	}
	
	public OmopDeviceUseStatement() {
		super(ContextLoaderListener.getCurrentWebApplicationContext(), DeviceExposure.class, DeviceExposureService.class, DeviceUseStatementResourceProvider.getType());
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}
	
	private void initialize(WebApplicationContext context) {
		conceptService = context.getBean(ConceptService.class);
		fPersonService = context.getBean(FPersonService.class);
		providerService = context.getBean(ProviderService.class);
		
		getSize();
	}
	
	public static OmopDeviceUseStatement getInstance() {
		return OmopDeviceUseStatement.omopDeviceUseStatement;
	}

	@Override
	public MyDeviceUseStatement constructResource(Long fhirId, DeviceExposure entity, List<String> includes) {
		MyDeviceUseStatement deviceUseStatement = constructFHIR(fhirId, entity);
		
		if (!includes.isEmpty()) {
			if (includes.contains("DeviceUseStatement:device")) {
				if (!deviceUseStatement.getDevice().isEmpty()) {
//				if (deviceUseStatement.hasDevice()) {
					ResourceReferenceDt deviceReference = deviceUseStatement.getDevice();
					IIdType deviceReferenceId = deviceReference.getReferenceElement();
					Long deviceReferenceFhirId = deviceReferenceId.getIdPartAsLong();
					MyDevice device = OmopDevice.getInstance().constructFHIR(deviceReferenceFhirId, entity);
					deviceReference.setResource(device);
				}
			}
		}
		
		return deviceUseStatement;
	}

	@Override
	public MyDeviceUseStatement constructFHIR(Long fhirId, DeviceExposure entity) {
		MyDeviceUseStatement myDeviceUseStatement = new MyDeviceUseStatement();
		myDeviceUseStatement.setId(new IdDt(DeviceUseStatementResourceProvider.getType(), fhirId));
		
		// In OMOPonFHIR, both Device and DeviceUseStatement are coming from the same
		// DeviceExposure table. Thus, Device._id = DeviceUseStatment._id
		// As we use the same device_exposure for both Device and DeviceUseStatement,
		// it would be easier for user to have a direct access to the device.
		// So, we contain the device rather than reference it.
		MyDevice myDevice = OmopDevice.getInstance().constructFHIR(fhirId, entity);
//		myDeviceUseStatement.addContained(myDevice);
		List<IResource> tempList = myDeviceUseStatement.getContained().getContainedResources();
		tempList.add(myDevice);
		ContainedDt tempContained = new ContainedDt();
		tempContained.setContainedResources(tempList);
		myDeviceUseStatement.setContained(tempContained);

		// Set the Id as a local id.
		myDeviceUseStatement.setDevice(new ResourceReferenceDt("#"+String.valueOf(fhirId)));
		
//		myDeviceUseStatement.setDevice(new Reference(new IdType(DeviceResourceProvider.getType(), fhirId)));
		
		// set subject, which is a patient.
		ResourceReferenceDt patientReference = new ResourceReferenceDt(new IdDt(PatientResourceProvider.getType(), entity.getFPerson().getId()));
		String singleName = entity.getFPerson().getNameAsSingleString();
		if (singleName != null && !singleName.isEmpty()) {
			patientReference.setDisplay(singleName);
		}
		myDeviceUseStatement.setSubject(patientReference);
		
		// set when this device is used.
		PeriodDt whenUsedPeriod = new PeriodDt();
		DateTimeDt startDate = new DateTimeDt(entity.getDeviceExposureStartDate());
		whenUsedPeriod.setStart(startDate);

		DateTimeDt endDate = new DateTimeDt(entity.getDeviceExposureEndDate());
		if (endDate != null) {
			whenUsedPeriod.setEnd(endDate);
		}
		
		myDeviceUseStatement.setWhenUsed(whenUsedPeriod);
		
//		// set source with Practitioner.
//		Provider provider = entity.getProvider();
//		if (provider != null) {
//			Long providerOmopId = provider.getId();
//			Long practitionerFhirId = IdMapping.getFHIRfromOMOP(providerOmopId, PractitionerResourceProvider.getType());
//			myDeviceUseStatement.setSource(new ResourceReferenceDt(new IdDt(PractitionerResourceProvider.getType(),practitionerFhirId)));
//		}
//		DSTU2 doesn't have sources; just take this out

		return myDeviceUseStatement;
	}
	
	@Override
	public Long toDbase(MyDeviceUseStatement fhirResource, IdDt fhirId) throws FHIRException {
		Long omopId = null;
		if (fhirId != null) {
			// Search for this ID.
			omopId = IdMapping.getOMOPfromFHIR(fhirId.getIdPartAsLong(), DeviceUseStatementResourceProvider.getType());
		}
		
		DeviceExposure deviceExposure = constructOmop(omopId, fhirResource);
		
		Long omopRecordId = null;
		if (deviceExposure.getId() != null) {
			omopRecordId = getMyOmopService().update(deviceExposure).getId();
		} else {
			omopRecordId = getMyOmopService().create(deviceExposure).getId();
		}
		return IdMapping.getFHIRfromOMOP(omopRecordId, DeviceUseStatementResourceProvider.getType());
	}

	@Override
	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
		ParameterWrapper paramWrapper = new ParameterWrapper();
		if (or)
			paramWrapper.setUpperRelationship("or");
		else
			paramWrapper.setUpperRelationship("and");
		
		switch (parameter) {
		case DeviceUseStatement.SP_RES_ID:
			String deviceUseStatementId = ((TokenParam) value).getValue();
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(deviceUseStatementId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case "Patient:" + Patient.SP_RES_ID:
			addParamlistForPatientIDName(parameter, (String)value, paramWrapper, mapList);
//			String pId = (String) value;
//			paramWrapper.setParameterType("Long");
//			paramWrapper.setParameters(Arrays.asList("fPerson.id"));
//			paramWrapper.setOperators(Arrays.asList("="));
//			paramWrapper.setValues(Arrays.asList(pId));
//			paramWrapper.setRelationship("or");
//			mapList.add(paramWrapper);
			break;
		case "Patient:" + Patient.SP_NAME:
			addParamlistForPatientIDName(parameter, (String)value, paramWrapper, mapList);
//			String patientName = (String) value;
//			paramWrapper.setParameterType("String");
//			paramWrapper.setParameters(Arrays.asList("fPerson.familyName", "fPerson.givenName1", "fPerson.givenName2",
//					"fPerson.prefixName", "fPerson.suffixName"));
//			paramWrapper.setOperators(Arrays.asList("like", "like", "like", "like", "like"));
//			paramWrapper.setValues(Arrays.asList("%" + patientName + "%"));
//			paramWrapper.setRelationship("or");
//			mapList.add(paramWrapper);
			break;
		default:
			mapList = null;
		}

		return mapList;
	}

	@Override
	public DeviceExposure constructOmop(Long omopId, MyDeviceUseStatement deviceUseStatement) {
		DeviceExposure deviceExposure = null;
		Device device = null;
		
		if (omopId != null) {
			deviceExposure = getMyOmopService().findById(omopId);
			if (deviceExposure == null) {
				try {
					throw new FHIRException(deviceUseStatement.getId() + " does not exist");
				} catch (FHIRException e) {
					e.printStackTrace();
				}
				
				return null;
			}
		} else {
			deviceExposure = new DeviceExposure();
		}

		// the deviceExposure contain both device and deviceUseStatement.
		// The provider should have been validated the resource if it contains device if create.
		// If device is not contained, then it means this is update.
		ResourceReferenceDt deviceReference = deviceUseStatement.getDevice();
		
		// reference should be pointing to contained or should have same id as deviceUseStatement.
		IIdType idType = deviceReference.getReferenceElement();
		if (idType.isLocal()) {
			// Check contained section.
//			List<BaseResource> containeds = deviceUseStatement.getContained();
			List<IResource> containeds = deviceUseStatement.getContained().getContainedResources();
			for (IResource contained: containeds) {
//				if (contained.getResourceType()==ResourceTypeEnum.DEVICE &&
				if (contained instanceof Device &&
						contained.getId().equals(idType.getIdPart())) {
					device = (Device) contained;
				}
			}
		} else {
			String deviceId = idType.getIdPart();
			if (omopId != null && !deviceId.equals(String.valueOf(omopId))) {
				// Error... device Id must be same as deviceUseStatement.
				try {
					throw new FHIRException("DeviceUseStatement.device: Device/"+deviceId+" must be Device/" + deviceUseStatement.getId());
				} catch (FHIRException e) {
					e.printStackTrace();
				}
				
				return null;
			}
		}

		ResourceReferenceDt subject = deviceUseStatement.getSubject();
		IIdType subjectReference = subject.getReferenceElement();
		if (!subjectReference.getResourceType().equals(PatientResourceProvider.getType())) {
			try {
				throw new FHIRException("DeviceUseStatement.subject must be Patient");
			} catch (FHIRException e) {
				e.printStackTrace();
			}
			
			return null;
		}
		
		Long patientId = subjectReference.getIdPartAsLong();
		Long omopPersonId = IdMapping.getOMOPfromFHIR(patientId, PatientResourceProvider.getType());
		FPerson fPerson = fPersonService.findById(omopPersonId);
		if (fPerson == null) {
			try {
				throw new FHIRException("DeviceUseStatement.subject(Patient) does not exist");
			} catch (FHIRException e) {
				e.printStackTrace();
			}
			
			return null;
		}
		
		deviceExposure.setFPerson(fPerson);
		
		// start and end datetime.
		PeriodDt periodUsed = deviceUseStatement.getWhenUsed();
		if (periodUsed != null && !periodUsed.isEmpty()) {
			Date startDate = periodUsed.getStart();
			if (startDate != null) {
				deviceExposure.setDeviceExposureStartDate(startDate);
			}
			
			Date endDate = periodUsed.getEnd();
			if (endDate != null) {
				deviceExposure.setDeviceExposureEndDate(endDate);
			}
		}
		
		// source(Practitioner)
//		ResourceReferenceDt practitionerSource = deviceUseStatement.getSource();
//		if (practitionerSource != null && !practitionerSource.isEmpty()) {
//			IIdType practitionerReference = practitionerSource.getReferenceElement();
//			Long practitionerId = practitionerReference.getIdPartAsLong();
//			Long omopProviderId = IdMapping.getOMOPfromFHIR(practitionerId, PractitionerResourceProvider.getType());
//			Provider provider = providerService.findById(omopProviderId);
//			if (provider == null) {
//				try {
//					throw new FHIRException("DeviceUseStatement.source(Practitioner) does not exist");
//				} catch (FHIRException e) {
//					e.printStackTrace();
//				}
//
//				return null;
//			}
//
//			deviceExposure.setProvider(provider);
//		}
//		we don't have sources; so just wipe this out?
		
		// check Device parameters.
		if (device != null) {
			// set device type
			CodeableConceptDt deviceType = device.getType();
			if (deviceType != null && !deviceType.isEmpty()) {
				CodingDt deviceTypeCoding = deviceType.getCodingFirstRep();
				try {
					Concept concept = CodeableConceptUtil.getOmopConceptWithFhirConcept(conceptService, deviceTypeCoding);
					if (concept != null) {
						deviceExposure.setDeviceConcept(concept);
						if (concept.getId() != 0L) {
							deviceExposure.setDeviceSourceConcept(concept);
						} else {
							deviceExposure.setDeviceSourceValue(deviceTypeCoding.getSystem()+":"+deviceTypeCoding.getCode());
						}
					}
				} catch (FHIRException e) {
					e.printStackTrace();
				}
			}
			
			// set device UDI
//			DeviceUdiComponent udi = device.getUdi();
//			if (udi != null && !udi.isEmpty()) {
//				String deviceIdentifier = udi.getDeviceIdentifier();
//				if (deviceIdentifier != null && !deviceIdentifier.isEmpty()) {
//					deviceExposure.setUniqueDeviceId(deviceIdentifier);
//				}
//			}
//			in DSTU2, UDI is returned as a string
			String udi = device.getUdi();
			if (udi != null && !udi.isEmpty()) {
				deviceExposure.setUniqueDeviceId(udi);
			}
		}
		
		// set default value that cannot be null
		Concept deviceTypeConcept = new Concept();
		deviceTypeConcept.setId(44818707L);
		deviceExposure.setDeviceTypeConcept(deviceTypeConcept);
		
		return deviceExposure;
	}
}
