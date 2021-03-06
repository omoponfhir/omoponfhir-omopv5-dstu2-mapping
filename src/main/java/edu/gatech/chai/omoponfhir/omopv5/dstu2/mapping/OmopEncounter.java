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

import ca.uhn.fhir.model.dstu2.resource.Encounter;

import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.composite.PeriodDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Encounter.Participant;
import ca.uhn.fhir.model.dstu2.valueset.EncounterStateEnum;
import ca.uhn.fhir.model.dstu2.valueset.EncounterClassEnum;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.ConditionResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.EncounterResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.OrganizationResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.PatientResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.PractitionerResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.utilities.DateUtil;

import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.hl7.fhir.exceptions.FHIRException;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.TokenParam;
import edu.gatech.chai.omopv5.dba.service.CareSiteService;
import edu.gatech.chai.omopv5.dba.service.ConditionOccurrenceService;
import edu.gatech.chai.omopv5.dba.service.FPersonService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.dba.service.ProviderService;
import edu.gatech.chai.omopv5.dba.service.VisitOccurrenceService;
import edu.gatech.chai.omopv5.model.entity.CareSite;
import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.model.entity.ConditionOccurrence;
import edu.gatech.chai.omopv5.model.entity.FPerson;
import edu.gatech.chai.omopv5.model.entity.Provider;
import edu.gatech.chai.omopv5.model.entity.VisitOccurrence;

public class OmopEncounter extends BaseOmopResource<Encounter, VisitOccurrence, VisitOccurrenceService>
		implements IResourceMapping<Encounter, VisitOccurrence> {

	private static OmopEncounter omopEncounter = new OmopEncounter();
	private FPersonService fPersonService;
	private CareSiteService careSiteService;
	private ProviderService providerService;
	private ConditionOccurrenceService conditionOccurrenceService;

	public OmopEncounter() {
		super(ContextLoaderListener.getCurrentWebApplicationContext(), VisitOccurrence.class,
				VisitOccurrenceService.class, EncounterResourceProvider.getType());
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}

	public OmopEncounter(WebApplicationContext context) {
		super(context, VisitOccurrence.class, VisitOccurrenceService.class, EncounterResourceProvider.getType());
		initialize(context);
	}

	private void initialize(WebApplicationContext context) {
		// Get bean for other services that we need for mapping.
		fPersonService = context.getBean(FPersonService.class);
		careSiteService = context.getBean(CareSiteService.class);
		providerService = context.getBean(ProviderService.class);
		conditionOccurrenceService = context.getBean(ConditionOccurrenceService.class);

		getSize();
	}

	public static OmopEncounter getInstance() {
		return OmopEncounter.omopEncounter;
	}

	@Override
	public Encounter constructFHIR(Long fhirId, VisitOccurrence visitOccurrence) {
		Encounter encounter = new Encounter();
		encounter.setId(new IdDt(fhirId));

		if (visitOccurrence.getVisitConcept() != null) {
			String visitString = visitOccurrence.getVisitConcept().getConceptName().toLowerCase();
//			CodingDt coding = new CodingDt();
//			if (visitString.contains("inpatient")) {
//				coding.setSystem(V3ActCode.IMP.getSystem());
//				coding.setCode(V3ActCode.IMP.toCode());
//				coding.setDisplay(V3ActCode.IMP.getDisplay());
//			} else if (visitString.toLowerCase().contains("outpatient")) {
//				coding.setSystem(V3ActCode.AMB.getSystem());
//				coding.setCode(V3ActCode.AMB.toCode());
//				coding.setDisplay(V3ActCode.AMB.getDisplay());
//			} else if (visitString.toLowerCase().contains("ambulatory")
//					|| visitString.toLowerCase().contains("office")) {
//				coding.setSystem(V3ActCode.AMB.getSystem());
//				coding.setCode(V3ActCode.AMB.toCode());
//				coding.setDisplay(V3ActCode.AMB.getDisplay());
//			} else if (visitString.toLowerCase().contains("home")) {
//				coding.setSystem(V3ActCode.HH.getSystem());
//				coding.setCode(V3ActCode.HH.toCode());
//				coding.setDisplay(V3ActCode.HH.getDisplay());
//			} else if (visitString.toLowerCase().contains("emergency")) {
//				coding.setSystem(V3ActCode.EMER.getSystem());
//				coding.setCode(V3ActCode.EMER.toCode());
//				coding.setDisplay(V3ActCode.EMER.getDisplay());
//			} else if (visitString.toLowerCase().contains("field")) {
//				coding.setSystem(V3ActCode.FLD.getSystem());
//				coding.setCode(V3ActCode.FLD.toCode());
//				coding.setDisplay(V3ActCode.FLD.getDisplay());
//			} else if (visitString.toLowerCase().contains("daytime")) {
//				coding.setSystem(V3ActCode.SS.getSystem());
//				coding.setCode(V3ActCode.SS.toCode());
//				coding.setDisplay(V3ActCode.SS.getDisplay());
//			} else if (visitString.toLowerCase().contains("virtual")) {
//				coding.setSystem(V3ActCode.VR.getSystem());
//				coding.setCode(V3ActCode.VR.toCode());
//				coding.setDisplay(V3ActCode.VR.getDisplay());
//			} else {
//				coding = null;
//			}
			String encounterClassName = "OTHER";
			if (visitString.contains("inpatient")) {
				encounterClassName = "INPATIENT";
			} else if (visitString.toLowerCase().contains("outpatient")) {
				encounterClassName = "OUTPATIENT";
			} else if (visitString.toLowerCase().contains("ambulatory")
					|| visitString.toLowerCase().contains("office")) {
				encounterClassName = "AMBULATORY";
			} else if (visitString.toLowerCase().contains("home")) {
				encounterClassName = "HOME";
			} else if (visitString.toLowerCase().contains("emergency")) {
				encounterClassName = "EMERGENCY";
			} else if (visitString.toLowerCase().contains("field")) {
				encounterClassName = "FIELD";
			} else if (visitString.toLowerCase().contains("daytime")) {
				encounterClassName = "DAYTIME";
			} else if (visitString.toLowerCase().contains("virtual")) {
				encounterClassName = "VIRTUAL";
			}

			if (encounterClassName != null)
//				encounter.setClass_(coding);
				encounter.setClassElement(EncounterClassEnum.valueOf(encounterClassName));

		}

		encounter.setStatus(EncounterStateEnum.FINISHED);

		// set Patient Reference
		ResourceReferenceDt patientReference = new ResourceReferenceDt(
				new IdDt(PatientResourceProvider.getType(), visitOccurrence.getFPerson().getId()));
		patientReference.setDisplay(visitOccurrence.getFPerson().getNameAsSingleString());
		encounter.setPatient(patientReference);

		// set Period
		PeriodDt visitPeriod = new PeriodDt();
//		DateFormat dateOnlyFormat = new SimpleDateFormat("yyyy/MM/dd");
//		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
//		try {
		// For start Date
//			String timeString = "00:00:00";
//			if (visitOccurrence.getStartTime() != null && !visitOccurrence.getStartTime().isEmpty()) {
//				timeString = visitOccurrence.getStartTime();
//			}
		Date visitStartDate = visitOccurrence.getVisitStartDate();
		Date visitStartDateTime = visitOccurrence.getVisitStartDateTime();
		DateTimeDt tempDate = null;
		if (visitStartDate != null) {
			if (visitStartDateTime != null) {
				tempDate = new DateTimeDt(visitStartDateTime);
			} else {
				tempDate = new DateTimeDt(visitStartDate);
			}
		}
//			String dateTimeString = dateOnlyFormat.format(visitOccurrence.getStartDate()) + " " + timeString;
//			Date DateTime = dateFormat.parse(dateTimeString);
//			DateTimeDt tempDate = new DateTimeDt(DateTime);
//			visitPeriod.setStart(DateTime);
		visitPeriod.setStart(tempDate);

		// For end Date
		Date visitEndDate = visitOccurrence.getVisitStartDate();
		Date visitEndDateTime = visitOccurrence.getVisitStartDateTime();
		DateTimeDt tempDate2 = null;
		if (visitEndDate != null) {
			if (visitEndDateTime != null) {
				tempDate2 = new DateTimeDt(visitEndDateTime);
			} else {
				tempDate2 = new DateTimeDt(visitEndDate);
			}
		}

		// timeString = "00:00:00";
//			if (visitOccurrence.getEndTime() != null && !visitOccurrence.getEndTime().isEmpty()) {
//				timeString = visitOccurrence.getEndTime();
//			}
//			dateTimeString = dateOnlyFormat.format(visitOccurrence.getEndDate()) + " " + timeString;
//			DateTime = dateFormat.parse(dateTimeString);
//			DateTimeDt tempDate2 = new DateTimeDt(DateTime);
//			visitPeriod.setEnd(DateTime);
		visitPeriod.setEnd(tempDate2);

//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		encounter.setPeriod(visitPeriod);

		if (visitOccurrence.getCareSite() != null) {
			ResourceReferenceDt serviceProviderReference = new ResourceReferenceDt(
					new IdDt(OrganizationResourceProvider.getType(), IdMapping.getFHIRfromOMOP(
							visitOccurrence.getCareSite().getId(), OrganizationResourceProvider.getType())));
			serviceProviderReference.setDisplay(visitOccurrence.getCareSite().getCareSiteName());
			encounter.setServiceProvider(serviceProviderReference);
		}

		if (visitOccurrence.getProvider() != null) {
			ResourceReferenceDt individualReference = new ResourceReferenceDt(
					new IdDt(PractitionerResourceProvider.getType(), IdMapping.getFHIRfromOMOP(
							visitOccurrence.getProvider().getId(), PractitionerResourceProvider.getType())));
			individualReference.setDisplay(visitOccurrence.getProvider().getProviderName());
			Participant participate = new Participant();
			participate.setIndividual(individualReference);
			encounter.addParticipant(participate);
		}

		// set condition if available.
//		ParameterWrapper param = new ParameterWrapper();
//		param.setParameterType("Long");
//		param.setParameters(Arrays.asList("visitOccurrence.id"));
//		param.setOperators(Arrays.asList("="));
//		param.setValues(Arrays.asList(String.valueOf(visitOccurrence.getId())));
//		List<ParameterWrapper> params = Arrays.asList(param);
//		List<ConditionOccurrence> conditions = conditionOccurrenceService.searchWithParams(0, 0, params, null);
		List<ConditionOccurrence> conditions = conditionOccurrenceService.searchByColumnString("visitOccurrence.id",
				visitOccurrence.getId());
		for (ConditionOccurrence condition : conditions) {
			ResourceReferenceDt conditionReference = new ResourceReferenceDt(
					new IdDt(ConditionResourceProvider.getType(), condition.getId()));
//			DiagnosisComponent diagnosisComponent = new DiagnosisComponent();
//			diagnosisComponent.setCondition(conditionReference);
//			encounter.addDiagnosis(diagnosisComponent);
			List<ResourceReferenceDt> tempList = encounter.getIndication();
			tempList.add(conditionReference);
			encounter.setIndication(tempList);
		}

		return encounter;
	}

	@Override
	public Long toDbase(Encounter fhirResource, IdDt fhirId) throws FHIRException {
		Long retval;
		Long omopId = null;
		if (fhirId != null) {
			omopId = IdMapping.getOMOPfromFHIR(fhirId.getIdPartAsLong(), getMyFhirResourceType());
		}

		VisitOccurrence visitOccurrence = constructOmop(omopId, fhirResource);

		if (visitOccurrence.getId() != null) {
			retval = getMyOmopService().update(visitOccurrence).getId();
		} else {
			retval = getMyOmopService().create(visitOccurrence).getId();
		}

		return IdMapping.getFHIRfromOMOP(retval, getMyFhirResourceType());
	}

	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
		ParameterWrapper paramWrapper = new ParameterWrapper();
		if (or)
			paramWrapper.setUpperRelationship("or");
		else
			paramWrapper.setUpperRelationship("and");

		switch (parameter) {
		case Encounter.SP_RES_ID:
			String encounterId = ((TokenParam) value).getValue();
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(encounterId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
//		case Encounter.SP_DIAGNOSIS:
		// TODO: handle diagnosis. This is condition id. Add join capability to
		// parameter wrapper.
//			break;
		case Encounter.SP_DATE:
			DateRangeParam dateRangeParam = ((DateRangeParam) value);
			paramWrapper.setUpperRelationship("or"); // or these two maps
			DateUtil.constructParameterWrapper(dateRangeParam, "visitStartDate", paramWrapper, mapList);
			ParameterWrapper paramWrapper1 = new ParameterWrapper();
			paramWrapper1.setUpperRelationship("or");
			DateUtil.constructParameterWrapper(dateRangeParam, "visitEndDate", paramWrapper1, mapList);
			break;
		case "Patient:" + Patient.SP_RES_ID:
			addParamlistForPatientIDName(parameter, (String) value, paramWrapper, mapList);
			break;
		case "Patient:" + Patient.SP_NAME:
			addParamlistForPatientIDName(parameter, (String) value, paramWrapper, mapList);
			break;
		default:
			mapList = null;
		}

		return mapList;

	}

	@Override
	public VisitOccurrence constructOmop(Long omopId, Encounter encounter) {
		FPerson fPerson;
		VisitOccurrence visitOccurrence = null;

		if (omopId != null) {
			visitOccurrence = getMyOmopService().findById(omopId);
		}
		if (visitOccurrence == null) {
			visitOccurrence = new VisitOccurrence();
		}

		ResourceReferenceDt patientReference = encounter.getPatient();
		if (patientReference == null || patientReference.isEmpty())
			return null; // We have to have a patient

		// get the Subject
		if (encounter.getPatient() != null) {
			Long subjectId = encounter.getPatient().getReferenceElement().getIdPartAsLong();
			Long subjectFhirId = IdMapping.getOMOPfromFHIR(subjectId, PatientResourceProvider.getType());
			fPerson = fPersonService.findById(subjectFhirId);
			visitOccurrence.setFPerson(fPerson);
		} else {
			// throw an error
			try {
				throw new FHIRException("FHIR Resource does not contain a Subject.");
			} catch (FHIRException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// We are writing to the database. Keep the source so we know where it
		// is coming from
		if (encounter.getId() != null && !encounter.getId().isEmpty()) {
			// See if we already have this in the source field. If so,
			// then we want update not create
			VisitOccurrence origVisit = getMyOmopService().findById(omopId);
			if (origVisit == null)
				visitOccurrence.setVisitSourceValue(encounter.getId().getValueAsString());
			else
				visitOccurrence.setId(origVisit.getId());
		}

		/* Set Period */
//		SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
		PeriodDt tempPeriod = encounter.getPeriod();
		if (tempPeriod != null) {
			Date tempDate = tempPeriod.getStart();
			if (tempDate != null) {
				visitOccurrence.setVisitStartDate(tempDate);
				visitOccurrence.setVisitStartDateTime(tempDate);
			} else {
				visitOccurrence.setVisitStartDate(new Date(0));
			}

			tempDate = tempPeriod.getEnd();
			if (tempDate != null) {
				visitOccurrence.setVisitEndDate(tempDate);
				visitOccurrence.setVisitEndDateTime(tempDate);
			} else {
				visitOccurrence.setVisitEndDate(new Date(0));
			}
		}

		/*
		 * Set Class - IP: Inpatient Visit - OP: Outpient Visit - ER: Emergency Room
		 * Visit - LTCP: Long Term Care Visit -
		 */
//		CodingDt classCoding = encounter.getClass_();
//		CodingDt classCoding = new CodingDt("http://hl7.org/fhir/v3/ActCode",encounter.getClassElement());
//		String code = classCoding.getCode();
		String code = encounter.getClassElement();

		Long omopConceptCode;
		try {
			omopConceptCode = (OmopConceptMapping.omopForEncounterClassCode(code));
			Concept visitConcept = new Concept();
			visitConcept.setId(omopConceptCode);
			visitOccurrence.setVisitConcept(visitConcept);
		} catch (FHIRException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/* Set Visit Type - we hardcode this */
		Concept visitTypeConcept = new Concept();
		visitTypeConcept.setId(44818518L); // This is Visit derived from EHR
		visitOccurrence.setVisitTypeConcept(visitTypeConcept);

		/* Set provider, which is practitioner in FHIR */
		Participant participant = encounter.getParticipantFirstRep();
		if (participant != null && !participant.isEmpty()) {
			ResourceReferenceDt individualRef = participant.getIndividual();
			if (individualRef != null) {
				if (individualRef.getReferenceElement().getResourceType()
						.equals(PractitionerResourceProvider.getType())) {
					Long providerId = IdMapping.getOMOPfromFHIR(individualRef.getReferenceElement().getIdPartAsLong(),
							PractitionerResourceProvider.getType());
					Provider provider = providerService.findById(providerId);
					if (provider != null) {
						visitOccurrence.setProvider(provider);
					}
				}
			}
		}

		// Set care site, which is organization in FHIR
		ResourceReferenceDt serviceProvider = encounter.getServiceProvider();
		if (serviceProvider != null && !serviceProvider.isEmpty()) {
			// service provider is Organization in Omop on FHIR.
			if (serviceProvider.getReferenceElement().getResourceType()
					.equals(OrganizationResourceProvider.getType())) {
				Long careSiteId = IdMapping.getOMOPfromFHIR(serviceProvider.getReferenceElement().getIdPartAsLong(),
						OrganizationResourceProvider.getType());
				CareSite careSite = careSiteService.findById(careSiteId);
				if (careSite != null) {
					visitOccurrence.setCareSite(careSite);
				}
			}
		}
		// NOTE: diagnosis.condition
		// This contains what condition is pointing to this encounter.
		// This is a link, which the conditionOccurrence should already have in OMOP.
		// So, we do not import this information here.

		// TODO: How do we handle Location Resource. This is different from
		// Location table in OMOP v5.
		// List<ca.uhn.fhir.model.dstu2.resource.Encounter.Location> locations =
		// encounter.getLocation();
		// if (locations.size() > 0) {
		// ca.uhn.fhir.model.dstu2.resource.Encounter.Location location =
		// locations.get(0);
		// ResourceReferenceDt locationResourceRef = location.getLocation();
		// if (locationResourceRef != null) {
		// Location locationResource = (Location)
		// locationResourceRef.getResource();
		// AddressDt address = locationResource.getAddress();
		// if (address != null) {
		// edu.gatech.i3l.fhir.dstu2.entities.Location myLocation =
		// edu.gatech.i3l.fhir.dstu2.entities.Location.searchAndUpdate(address,
		// null);
		// this.set
		// }
		// }
		// }

		return visitOccurrence;
	}
}
