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

import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.PeriodDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.resource.Practitioner;
import ca.uhn.fhir.model.dstu2.resource.Procedure;
import ca.uhn.fhir.model.dstu2.resource.Procedure.Performer;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.dstu2.valueset.ProcedureStatusEnum;

import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.hl7.fhir.exceptions.FHIRException;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;

import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.EncounterResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.PatientResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.PractitionerResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.ProcedureResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.utilities.CodeableConceptUtil;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.utilities.DateUtil;
import edu.gatech.chai.omopv5.dba.service.ConceptService;
import edu.gatech.chai.omopv5.dba.service.FPersonService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.dba.service.ProcedureOccurrenceService;
import edu.gatech.chai.omopv5.dba.service.ProviderService;
import edu.gatech.chai.omopv5.dba.service.VisitOccurrenceService;
import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.model.entity.FPerson;
import edu.gatech.chai.omopv5.model.entity.ProcedureOccurrence;
import edu.gatech.chai.omopv5.model.entity.Provider;
import edu.gatech.chai.omopv5.model.entity.VisitOccurrence;

public class OmopProcedure extends BaseOmopResource<Procedure, ProcedureOccurrence, ProcedureOccurrenceService>
		implements IResourceMapping<Procedure, ProcedureOccurrence> {

	private static OmopProcedure omopProcedure = new OmopProcedure();
	private ConceptService conceptService;
	private FPersonService fPersonService;
	private VisitOccurrenceService visitOccurrenceService;
	private ProviderService providerService;

	private static long OMOP_PROCEDURE_TYPE_DEFAULT = 44786630L;
	
	public OmopProcedure(WebApplicationContext context) {
		super(context, ProcedureOccurrence.class, ProcedureOccurrenceService.class, ProcedureResourceProvider.getType());
		initialize(context);
	}

	public OmopProcedure() {
		super(ContextLoaderListener.getCurrentWebApplicationContext(), ProcedureOccurrence.class, ProcedureOccurrenceService.class, ProcedureResourceProvider.getType());
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}
	
	private void initialize(WebApplicationContext context) {
		conceptService = context.getBean(ConceptService.class);
		fPersonService = context.getBean(FPersonService.class);
		visitOccurrenceService = context.getBean(VisitOccurrenceService.class);
		providerService = context.getBean(ProviderService.class);
		
		getSize();
	}
	
	public static OmopProcedure getInstance() {
		return OmopProcedure.omopProcedure;
	}
	
	@Override
	public Long toDbase(Procedure fhirResource, IdDt fhirId) throws FHIRException {
		Long omopId = null;
		if (fhirId != null) {
			// Update
			omopId = IdMapping.getOMOPfromFHIR(fhirId.getIdPartAsLong(), ProcedureResourceProvider.getType());
		}
		
		ProcedureOccurrence procedureOccurrence = constructOmop(omopId, fhirResource);

		Long OmopRecordId = null;
		if (omopId == null) {
			OmopRecordId = getMyOmopService().create(procedureOccurrence).getId();
		} else {
			OmopRecordId = getMyOmopService().update(procedureOccurrence).getId();
		}
		
		return IdMapping.getFHIRfromOMOP(OmopRecordId, ProcedureResourceProvider.getType());
	}

	@Override
	public Procedure constructResource(Long fhirId, ProcedureOccurrence entity, List<String> includes) {
		Procedure procedure = constructFHIR(fhirId,entity); 
		Long omopId = entity.getId();
		
		if (!includes.isEmpty()) {
			if (includes.contains("Procedure:patient")) {
//				if (procedure.hasSubject()) {
				if (!procedure.getSubject().isEmpty()) {
					Long patientFhirId = procedure.getSubject().getReferenceElement().getIdPartAsLong();
					Patient patient = OmopPatient.getInstance().constructFHIR(patientFhirId, entity.getFPerson());
					procedure.getSubject().setResource(patient);
				}
			}
			
			if (includes.contains("Procedure:performer")) {
				Boolean tempBool= false;
				List<Performer> tempList = procedure.getPerformer();
				for (Performer item : tempList) {
					if (!item.isEmpty())
						tempBool = true;
				}
//				if (procedure.hasPerformer()) {
				if (tempBool) {
					List<Performer> performers = procedure.getPerformer();
					for (Performer performer: performers) {
						if (!performer.isEmpty()) {
							Long practitionerFhirId = performer.getActor().getReferenceElement().getIdPartAsLong();
							Practitioner practitioner = OmopPractitioner.getInstance().constructFHIR(practitionerFhirId, entity.getProvider());
							performer.getActor().setResource(practitioner);
						}
					}
				}
			}

			if (includes.contains("Procedure:context")) {
//				if (procedure.hasContext()) {
				if (procedure.getEncounter().isEmpty()) {
					Long encounterFhirId = procedure.getEncounter().getReferenceElement().getIdPartAsLong();
					Encounter encounter = OmopEncounter.getInstance().constructFHIR(encounterFhirId, entity.getVisitOccurrence());
					procedure.getEncounter().setResource(encounter);
				}
			}

		}
		
		return procedure;
	}

	@Override
	public Procedure constructFHIR(Long fhirId, ProcedureOccurrence entity) {
		Procedure procedure = new Procedure(); //Assuming default active state
		procedure.setId(new IdDt(fhirId));

		// Set subject 
		ResourceReferenceDt patientReference = new ResourceReferenceDt(new IdDt(PatientResourceProvider.getType(), entity.getFPerson().getId()));
		patientReference.setDisplay(entity.getFPerson().getNameAsSingleString());
		procedure.setSubject(patientReference);
		
		// TODO: We put completed as a default. Revisit this
		procedure.setStatus(ProcedureStatusEnum.COMPLETED);
		
		// Procedure code concept mapping
		Concept procedureConcept = entity.getProcedureConcept();
		CodeableConceptDt procedureCodeableConcept = null;
		try {
			procedureCodeableConcept = CodeableConceptUtil.getCodeableConceptFromOmopConcept(procedureConcept);
		} catch (FHIRException e) {
			e.printStackTrace();
		}

		if (procedureCodeableConcept != null) {
			procedure.setCode(procedureCodeableConcept);
		}

		// Procedure category mapping 
//		Concept procedureTypeConcept = entity.getProcedureTypeConcept();
//		CodeableConcept procedureTypeCodeableConcept = null;
//		try {
//			procedureTypeCodeableConcept = CodeableConceptUtil.getCodeableConceptFromOmopConcept(procedureTypeConcept);
//		} catch (FHIRException e) {
//			e.printStackTrace();
//		}
//
//		if (procedureTypeCodeableConcept != null) {
//			procedure.setCategory(procedureTypeCodeableConcept);
//		}
		
		// Context mapping
		VisitOccurrence visitOccurrence = entity.getVisitOccurrence();
		if (visitOccurrence != null) {
			ResourceReferenceDt contextReference = new ResourceReferenceDt(new IdDt(EncounterResourceProvider.getType(), visitOccurrence.getId()));
			procedure.setEncounter(contextReference);
		}
		
		// Performer mapping
		Provider provider = entity.getProvider();
		if (provider != null && provider.getId() != 0L) {
			Performer performer = new Performer();
			
			// actor mapping
			Long providerFhirId = IdMapping.getFHIRfromOMOP(provider.getId(), PractitionerResourceProvider.getType());
			ResourceReferenceDt actorReference = new ResourceReferenceDt(new IdDt(PractitionerResourceProvider.getType(), providerFhirId));
			performer.setActor(actorReference);
			
			// role mapping
			Concept providerSpecialtyConcept = provider.getSpecialtyConcept();
			if (providerSpecialtyConcept != null && providerSpecialtyConcept.getId() != 0L) {
				CodeableConceptDt performerRoleCodeableConcept = null;
				try {
					performerRoleCodeableConcept = CodeableConceptUtil.getCodeableConceptFromOmopConcept(providerSpecialtyConcept);
				} catch (FHIRException e) {
					e.printStackTrace();
				}
	
				if (performerRoleCodeableConcept != null) {
					performer.setRole(performerRoleCodeableConcept);
				}
			}
			procedure.addPerformer(performer);
		}
		
		// Location mapping
		// TODO: Add location after Location mapping is done.
		
		// Performed DateTime mapping
		DateTimeDt date = new DateTimeDt(entity.getProcedureDate());
		procedure.setPerformed(date);
		
		return procedure;
	}

	@Override
	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
		ParameterWrapper paramWrapper = new ParameterWrapper();
        if (or) paramWrapper.setUpperRelationship("or");
        else paramWrapper.setUpperRelationship("and");

		switch (parameter) {
		case Procedure.SP_RES_ID:
			String procedureId = ((TokenParam) value).getValue();
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(procedureId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Procedure.SP_CODE:
			String system = ((TokenParam) value).getSystem();
			String code = ((TokenParam) value).getValue();
			
			if ((system == null || system.isEmpty()) && (code == null || code.isEmpty()))
				break;
			
			String omopVocabulary = "None";
			if (system != null && !system.isEmpty()) {
				try {
					omopVocabulary = OmopCodeableConceptMapping.omopVocabularyforFhirUri(system);
				} catch (FHIRException e) {
					e.printStackTrace();
				}
			} 

			paramWrapper.setParameterType("String");
			if ("None".equals(omopVocabulary) && code != null && !code.isEmpty()) {
				paramWrapper.setParameters(Arrays.asList("procedureConcept.conceptCode"));
				paramWrapper.setOperators(Arrays.asList("="));
				paramWrapper.setValues(Arrays.asList(code));
			} else if (!"None".equals(omopVocabulary) && (code == null || code.isEmpty())) {
				paramWrapper.setParameters(Arrays.asList("procedureConcept.vocabularyId"));
				paramWrapper.setOperators(Arrays.asList("="));
				paramWrapper.setValues(Arrays.asList(omopVocabulary));				
			} else {
				paramWrapper.setParameters(Arrays.asList("procedureConcept.vocabularyId", "procedureConcept.conceptCode"));
				paramWrapper.setOperators(Arrays.asList("=","="));
				paramWrapper.setValues(Arrays.asList(omopVocabulary, code));
			}
			paramWrapper.setRelationship("and");
			mapList.add(paramWrapper);
			break;
//		case Procedure.SP_CONTEXT:
		case Procedure.SP_ENCOUNTER:
			Long fhirEncounterId = ((ReferenceParam) value).getIdPartAsLong();
			Long omopVisitOccurrenceId = IdMapping.getOMOPfromFHIR(fhirEncounterId, EncounterResourceProvider.getType());
//			String resourceName = ((ReferenceParam) value).getResourceType();
			
			// We support Encounter so the resource type should be Encounter.
			if (omopVisitOccurrenceId != null) {
				paramWrapper.setParameterType("Long");
				paramWrapper.setParameters(Arrays.asList("visitOccurrence.id"));
				paramWrapper.setOperators(Arrays.asList("="));
				paramWrapper.setValues(Arrays.asList(String.valueOf(omopVisitOccurrenceId)));
				paramWrapper.setRelationship("or");
				mapList.add(paramWrapper);
			}
			break;
		case Procedure.SP_DATE:
			// We got Date Range here. 
			DateRangeParam dateRangeParam = ((DateRangeParam) value);
			
			// There are 3 possible cases. 
			// case 1 is range.
			// case 2 is lower bound and no upper bound
			// case 3 is upper bound and no lower bound
			
//			if (dateRangeParam.getUpperBound() == null) {
//				// case 2
//				Date lowerDate = dateRangeParam.getLowerBoundAsInstant();
//				
//			}
//			ParamPrefixEnum apiOperator = dateParam.getPrefix();
//			String sqlOperator = null;
//			if (apiOperator.equals(ParamPrefixEnum.GREATERTHAN)) {
//				sqlOperator = ">";
//			} else if (apiOperator.equals(ParamPrefixEnum.GREATERTHAN_OR_EQUALS)) {
//				sqlOperator = ">=";
//			} else if (apiOperator.equals(ParamPrefixEnum.LESSTHAN)) {
//				sqlOperator = "<";
//			} else if (apiOperator.equals(ParamPrefixEnum.LESSTHAN_OR_EQUALS)) {
//				sqlOperator = "<=";
//			} else if (apiOperator.equals(ParamPrefixEnum.NOT_EQUAL)) {
//				sqlOperator = "!=";
//			} else {
//				sqlOperator = "=";
//			}
//			Date date = dateParam.getValue();
//			
//			paramWrapper.setParameterType("Date");
//			paramWrapper.setParameters(Arrays.asList("procedureDate"));
//			paramWrapper.setOperators(Arrays.asList(sqlOperator));
//			paramWrapper.setValues(Arrays.asList(String.valueOf(date.getTime())));
//			paramWrapper.setRelationship("or");
			DateUtil.constructParameterWrapper(dateRangeParam, "procedureDate", paramWrapper, mapList);
			break;
		case Procedure.SP_SUBJECT:
		case Procedure.SP_PATIENT:
			ReferenceParam patientReference = ((ReferenceParam) value);
			Long fhirPatientId = patientReference.getIdPartAsLong();
			Long omopPersonId = IdMapping.getOMOPfromFHIR(fhirPatientId, PatientResourceProvider.getType());

			String omopPersonIdString = String.valueOf(omopPersonId);
			
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("fPerson.id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(omopPersonIdString));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Procedure.SP_PERFORMER:
			// We only support provider (Practitioner).
			Long fhirPractitionerId = ((ReferenceParam) value).getIdPartAsLong();
			Long omopProviderId = IdMapping.getOMOPfromFHIR(fhirPractitionerId, PractitionerResourceProvider.getType());
//			String performerResourceName = ((ReferenceParam) value).getResourceType();
			
			// We support Encounter so the resource type should be Encounter.
			if (omopProviderId != null) {
				paramWrapper.setParameterType("Long");
				paramWrapper.setParameters(Arrays.asList("provider.id"));
				paramWrapper.setOperators(Arrays.asList("="));
				paramWrapper.setValues(Arrays.asList(String.valueOf(omopProviderId)));
				paramWrapper.setRelationship("or");
				mapList.add(paramWrapper);
			}
			break;
		default:
			mapList = null;
		}
		
		return mapList;
	}

	@Override
	public ProcedureOccurrence constructOmop(Long omopId, Procedure fhirResource) {
		ProcedureOccurrence procedureOccurrence = null;
		if (omopId == null) {
			// Create
			procedureOccurrence = new ProcedureOccurrence();
		} else {
			procedureOccurrence = getMyOmopService().findById(omopId);
			
			if (procedureOccurrence == null) {
				try {
					throw new FHIRException(fhirResource.getId() + " does not exist");
				} catch (FHIRException e) {
					e.printStackTrace();
				}
			}
		}

		// Procedure type concept mapping.
//		CodeableConcept categoryCodeableConcept = fhirResource.getCategory();
//		Concept procedureTypeConcept = null;
//		if (!categoryCodeableConcept.isEmpty()) {
//			List<Coding> codings = categoryCodeableConcept.getCoding();
//			for (Coding coding: codings) {
//				procedureTypeConcept = CodeableConceptUtil.getOmopConceptWithFhirConcept(conceptService, coding);
//				if (procedureTypeConcept != null) break;
//			}
//		}		
//		
//		if (procedureTypeConcept != null) {
//			procedureOccurrence.setProcedureTypeConcept(procedureTypeConcept);
//		}
		// Procedure type concept is not mappable. But, this is required.
		// Hardcode to 44786630L (Primary Procedure)
		procedureOccurrence.setProcedureTypeConcept(new Concept(OMOP_PROCEDURE_TYPE_DEFAULT));
		
		// Procedure concept mapping
		CodeableConceptDt codeCodeableConcept = fhirResource.getCode();
		Concept procedureConcept = null;
		if (!codeCodeableConcept.isEmpty()) {
			List<CodingDt> codings = codeCodeableConcept.getCoding();
			for (CodingDt coding: codings) {
				try {
					procedureConcept = CodeableConceptUtil.getOmopConceptWithFhirConcept(conceptService, coding);
					if (procedureConcept != null) break;
				} catch (FHIRException e) {
					e.printStackTrace();
				}
			}
		}
		
		if (procedureConcept != null) {
			procedureOccurrence.setProcedureConcept(procedureConcept);
		}
		
		// Person mapping
		try {
			ResourceReferenceDt patientReference = fhirResource.getSubject();
		if (patientReference.getReferenceElement().getResourceType().equals(PatientResourceProvider.getType())) {
			Long patientFhirId = patientReference.getReferenceElement().getIdPartAsLong();
			Long omopFPersonId = IdMapping.getOMOPfromFHIR(patientFhirId, PatientResourceProvider.getType());
			if (omopFPersonId == null) {
				throw new FHIRException("Unable to get OMOP person ID from FHIR patient ID");
			} 
			
			FPerson fPerson = fPersonService.findById(omopFPersonId);
			if (fPerson != null) {
				procedureOccurrence.setFPerson(fPerson);
			} else {
				throw new FHIRException("Unable to find the person from OMOP database");
			}
		} else {
			throw new FHIRException("Subject must be Patient");
		}

		// Visit Occurrence mapping
//		ResourceReferenceDt encounterReference = fhirResource.getContext();
    	ResourceReferenceDt encounterReference = fhirResource.getEncounter();
		if (encounterReference.getReferenceElement().getResourceType().equals(EncounterResourceProvider.getType())) {
			Long encounterFhirId = encounterReference.getReferenceElement().getIdPartAsLong();
			Long omopVisitOccurrenceId = IdMapping.getOMOPfromFHIR(encounterFhirId, EncounterResourceProvider.getType());
			if (omopVisitOccurrenceId == null) {
				throw new FHIRException("Unable to get OMOP Visit Occurrence ID from FHIR encounter ID");
			}
			
			VisitOccurrence visitOccurrence = visitOccurrenceService.findById(omopVisitOccurrenceId);
			if (visitOccurrence != null) {
				procedureOccurrence.setVisitOccurrence(visitOccurrence);
			} else {
				throw new FHIRException("Unable to find the visit occurrence from OMOP database");
			}
		} else {
			throw new FHIRException("Context must be Encounter");
		}

		} catch (FHIRException e) {
			e.printStackTrace();
		}

		// Provider mapping
		List<Performer> performers = fhirResource.getPerformer();
		for (Performer performer: performers) {
			if (performer.getActor().getReferenceElement().getResourceType().equals(PractitionerResourceProvider.getType())) {
				Long performerFhirId = performer.getActor().getReferenceElement().getIdPartAsLong();
				Long omopProviderId = IdMapping.getOMOPfromFHIR(performerFhirId, PractitionerResourceProvider.getType());
				if (omopProviderId == null) continue;
				Provider provider = providerService.findById(omopProviderId);
				if (provider == null || provider.getId() == 0L) continue;
				
				// specialty mapping
				CodeableConceptDt roleCodeableConcept = performer.getRole();
				Concept specialtyConcept = null;
				if (!roleCodeableConcept.isEmpty()) {
					List<CodingDt> codings = roleCodeableConcept.getCoding();
					for (CodingDt coding: codings) {
						if (!coding.isEmpty()) {
							try {
								specialtyConcept = CodeableConceptUtil.getOmopConceptWithFhirConcept(conceptService, coding);
							} catch (FHIRException e) {
								e.printStackTrace();
							}
							if (specialtyConcept != null) {
								if (provider.getSpecialtyConcept() == null || provider.getSpecialtyConcept().getId() == 0L) {
									// We have specialty information but provider table does not have this.
									// We can populate.
									provider.setSpecialtyConcept(specialtyConcept);
									providerService.update(provider);
									break;
								}
							}
						}
					}
				}
				
				procedureOccurrence.setProvider(provider);
				break;
			}
		}
		
		// Procedure Date mapping. Use start date for Period.
		try {
			IDatatype performedType = fhirResource.getPerformed();
		if (!performedType.isEmpty()) {
			Date performedDate = null;
			if (performedType instanceof DateTimeDt) {
				// PerformedDateTime
				performedDate= ((DateTimeDt) performedType).getValue();
//				performedDate = performedType.castToDateTime(performedType).getValue();
			} else {
				// PerformedPeriod
//				performedDate = performedType.castToPeriod(performedType).getStart();
				performedDate= ((PeriodDt) performedType).getStart();
			}
			
			if (performedDate != null)
				procedureOccurrence.setProcedureDate(performedDate);
		}
		} catch (FHIRException e) {
			e.printStackTrace();
		}
		
		return procedureOccurrence;
	}

}
