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

import org.hl7.fhir.exceptions.FHIRException;

import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.ConditionResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.EncounterResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.PatientResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.PractitionerResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.utilities.CodeableConceptUtil;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.utilities.ConditionCategory;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.utilities.DateUtil;
import edu.gatech.chai.omopv5.dba.service.*;
import edu.gatech.chai.omopv5.model.entity.*;

import ca.uhn.fhir.model.dstu2.resource.*;
import ca.uhn.fhir.model.dstu2.composite.*;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.dstu2.valueset.ConditionCategoryCodesEnum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class OmopCondition extends BaseOmopResource<Condition, ConditionOccurrence, ConditionOccurrenceService> {

	private static final Logger logger = LoggerFactory.getLogger(OmopCondition.class);

	private static OmopCondition omopCondition = new OmopCondition();

	private ConditionOccurrenceService conditionOccurrenceService;
	private FPersonService fPersonService;
	private ProviderService providerService;
	private ConceptService conceptService;
	private VisitOccurrenceService visitOccurrenceService;

	public OmopCondition(WebApplicationContext context) {
		super(context, ConditionOccurrence.class, ConditionOccurrenceService.class,
				ConditionResourceProvider.getType());
		initialize(context);
	}

	public OmopCondition() {
		super(ContextLoader.getCurrentWebApplicationContext(), ConditionOccurrence.class,
				ConditionOccurrenceService.class, ConditionResourceProvider.getType());
		initialize(ContextLoader.getCurrentWebApplicationContext());
	}

	private void initialize(WebApplicationContext context) {
		// Get bean for other services that we need for mapping.
		if (context != null) {
			conditionOccurrenceService = context.getBean(ConditionOccurrenceService.class);
			fPersonService = context.getBean(FPersonService.class);
			providerService = context.getBean(ProviderService.class);
			conceptService = context.getBean(ConceptService.class);
			visitOccurrenceService = context.getBean(VisitOccurrenceService.class);
		} else {
			logger.error("context must be NOT null");
		}
		
		getSize();
	}

	public static OmopCondition getInstance() {
		return OmopCondition.omopCondition;
	}

	@Override
	public Condition constructFHIR(Long fhirId, ConditionOccurrence conditionOccurrence) {
		Condition condition = new Condition();
		condition.setId(new IdDt(fhirId));

		addPersonToCondition(conditionOccurrence, condition);
		addCodeToCondition(conditionOccurrence, condition);
		addStartAndEndDateToCondition(conditionOccurrence, condition);
		addTypeToCondition(conditionOccurrence, condition);
		addAsserterToCondition(conditionOccurrence, condition);
		addContextToCondition(conditionOccurrence, condition);

		// TODO: Need to map the following
		// ??Condition.abatement.abatementString, but we are using abatementDateTime for
		// the end date and Abatement[x] has a 0..1 cardinality.
		String stopReason = conditionOccurrence.getStopReason();
		// ??
		String sourceValue = conditionOccurrence.getConditionSourceValue();
		// ??
		Concept sourceConceptId = conditionOccurrence.getConditionSourceConcept();

		return condition;
	}

	@Override
	public Long toDbase(Condition fhirResource, IdDt fhirId) throws FHIRException {
		Long retval;
		Long omopId = null, fhirIdLong = null;

		if (fhirId != null) {
			fhirIdLong = fhirId.getIdPartAsLong();
			if (fhirIdLong == null) {
				logger.error("Failed to get Condition.id as Long Value");
				return null;
			}

			omopId = IdMapping.getOMOPfromFHIR(fhirIdLong, ConditionResourceProvider.getType());
		}

		ConditionOccurrence conditionOccurrence = constructOmop(omopId, fhirResource);

		// TODO: Do you need to call other services to update links resources.

		if (conditionOccurrence.getId() != null) {
			retval = conditionOccurrenceService.update(conditionOccurrence).getId();
		} else {
			retval = conditionOccurrenceService.create(conditionOccurrence).getId();
		}

		return IdMapping.getFHIRfromOMOP(retval, ConditionResourceProvider.getType());
	}

	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
		ParameterWrapper paramWrapper = new ParameterWrapper();
		if (or)
			paramWrapper.setUpperRelationship("or");
		else
			paramWrapper.setUpperRelationship("and");

		switch (parameter) {
//			DSTU2 doesn't support this search parameter
//			case Condition.SP_ABATEMENT_AGE:
//				// not supporting
//				break;
//			DSTU2 doesn't support this search parameter
//			case Condition.SP_ABATEMENT_BOOLEAN:
//				// not supporting
//				break;
//			case Condition.SP_ABATEMENT_DATE:
//				// Condition.abatementDate -> Omop ConditionOccurrence.conditionEndDate
//				putDateInParamWrapper(paramWrapper, value, "conditionEndDate");
//				mapList.add(paramWrapper);
//				break;
//			case Condition.SP_ABATEMENT_STRING:
//				// not supporting
//				break;
//			case Condition.SP_ASSERTED_DATE:
//				// Condition.assertedDate -> Omop ConditionOccurrence.conditionStartDate
//				putDateInParamWrapper(paramWrapper, value, "conditionStartDate");
//				mapList.add(paramWrapper);
//				break;
			case Condition.SP_ASSERTER:
				// Condition.asserter -> Omop Provider
				ReferenceParam patientReference = ((ReferenceParam) value);
				String patientId = String.valueOf(patientReference.getIdPartAsLong());

				paramWrapper.setParameterType("Long");
				paramWrapper.setParameters(Arrays.asList("provider.id"));
				paramWrapper.setOperators(Arrays.asList("="));
				paramWrapper.setValues(Arrays.asList(patientId));
				paramWrapper.setRelationship("or");
				mapList.add(paramWrapper);
				break;
			case Condition.SP_BODY_SITE:
				// not supporting
				break;
			case Condition.SP_CATEGORY:
				// Condition.category
				putConditionInParamWrapper(paramWrapper, value);
				mapList.add(paramWrapper);
				break;
//			DSTU2 doesn't support this search parameter
//			case Condition.SP_CLINICAL_STATUS:
//				break;
			case Condition.SP_CODE:
				String system = ((TokenParam) value).getSystem();
				String code = ((TokenParam) value).getValue();
//    			System.out.println("\n\n\n\n\nSystem:"+system+"\n\ncode:"+code+"\n\n\n\n\n");
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
					paramWrapper.setParameters(Arrays.asList("conditionConcept.conceptCode"));
					paramWrapper.setOperators(Arrays.asList("="));
					paramWrapper.setValues(Arrays.asList(code));
				} else if (!"None".equals(omopVocabulary) && (code == null || code.isEmpty())) {
					paramWrapper.setParameters(Arrays.asList("conditionConcept.vocabularyId"));
					paramWrapper.setOperators(Arrays.asList("="));
					paramWrapper.setValues(Arrays.asList(omopVocabulary));
				} else {
					paramWrapper.setParameters(Arrays.asList("conditionConcept.vocabularyId", "conditionConcept.conceptCode"));
					paramWrapper.setOperators(Arrays.asList("=", "="));
					paramWrapper.setValues(Arrays.asList(omopVocabulary, code));
				}
				paramWrapper.setRelationship("and");
				mapList.add(paramWrapper);

//                //Condition.code -> Omop Concept
//                putConditionInParamWrapper(paramWrapper, value);
//                mapList.add(paramWrapper);

				break;
//			DSTU2 doesn't support this search parameter
//			case Condition.SP_CONTEXT:
//				// Condition.context -> Omop VisitOccurrence
//				ReferenceParam visitReference = (ReferenceParam) value;
//				String visitId = String.valueOf(visitReference.getIdPartAsLong());
//				paramWrapper.setParameterType("Long");
//				paramWrapper.setParameters(Arrays.asList("visitOccurrence.id"));
//				paramWrapper.setOperators(Arrays.asList("="));
//				paramWrapper.setValues(Arrays.asList(visitId));
//				paramWrapper.setRelationship("or");
//				mapList.add(paramWrapper);
//				break;
			case Condition.SP_ENCOUNTER:
				// not supporting
				break;
			case Condition.SP_EVIDENCE:
				// not supporting
				break;
//			DSTU2 doesn't support this search parameter
//			case Condition.SP_EVIDENCE_DETAIL:
//				// not supporting
//				break;
			case Condition.SP_IDENTIFIER:
				// not supporting
				break;
//			DSTU2 doesn't support this search parameter
//			case Condition.SP_ONSET_AGE:
//				// not supporting
//				break;
//			case Condition.SP_ONSET_DATE:
//				// not supporting
//				break;
			case Condition.SP_ONSET_INFO:
				// not supporting
				break;
			case Condition.SP_PATIENT:
//				Subject doesn't exist in DSTU2, only Patient
//			case Condition.SP_SUBJECT:
				ReferenceParam subjectReference = ((ReferenceParam) value);
				Long fhirPatientId = subjectReference.getIdPartAsLong();
				Long omopPersonId = IdMapping.getOMOPfromFHIR(fhirPatientId, PatientResourceProvider.getType());

				String omopPersonIdString = String.valueOf(omopPersonId);

				paramWrapper.setParameterType("Long");
				paramWrapper.setParameters(Arrays.asList("fPerson.id"));
				paramWrapper.setOperators(Arrays.asList("="));
				paramWrapper.setValues(Arrays.asList(omopPersonIdString));
				paramWrapper.setRelationship("or");
				mapList.add(paramWrapper);
				break;
			case Condition.SP_RES_ID:
				String conditionId = ((TokenParam) value).getValue();
				paramWrapper.setParameterType("Long");
				paramWrapper.setParameters(Arrays.asList("id"));
				paramWrapper.setOperators(Arrays.asList("="));
				paramWrapper.setValues(Arrays.asList(conditionId));
				paramWrapper.setRelationship("or");
				mapList.add(paramWrapper);
				break;
			case Condition.SP_SEVERITY:
				// not supporting
				break;
			case Condition.SP_STAGE:
				// not supporting
				break;
//			DSTU2 doesn't support this search parameter
//			case Condition.SP_VERIFICATION_STATUS:
//				// not supporting
//				break;
			case Condition.SP_DATE_RECORDED:
				DateRangeParam dateRangeParam = ((DateRangeParam) value);
				DateUtil.constructParameterWrapper(dateRangeParam, "conditionStartDate", paramWrapper, mapList);		
				break;
			default:
				mapList = null;
		}

		return mapList;
	}

	/* ====================================================================== */
	/* PRIVATE METHODS */
	/* ====================================================================== */

	private void putConditionInParamWrapper(ParameterWrapper paramWrapper, Object value) {
		String system = ((TokenParam) value).getSystem();
		String code = ((TokenParam) value).getValue();

		paramWrapper.setParameterType("String");
		paramWrapper.setParameters(Arrays.asList("concept.vocabularyId", "concept.conceptCode"));
		paramWrapper.setParameters(Arrays.asList("like", "like"));
		paramWrapper.setValues(Arrays.asList(system, code));
		paramWrapper.setRelationship("and");
	}

	private void putDateInParamWrapper(ParameterWrapper paramWrapper, Object value, String omopTableColumn) {
		DateParam dateParam = (DateParam) value;
		ParamPrefixEnum apiOperator = dateParam.getPrefix();
		String sqlOperator = null;
		if (apiOperator.equals(ParamPrefixEnum.GREATERTHAN)) {
			sqlOperator = ">";
		} else if (apiOperator.equals(ParamPrefixEnum.GREATERTHAN_OR_EQUALS)) {
			sqlOperator = ">=";
		} else if (apiOperator.equals(ParamPrefixEnum.LESSTHAN)) {
			sqlOperator = "<";
		} else if (apiOperator.equals(ParamPrefixEnum.LESSTHAN_OR_EQUALS)) {
			sqlOperator = "<=";
		} else if (apiOperator.equals(ParamPrefixEnum.NOT_EQUAL)) {
			sqlOperator = "!=";
		} else {
			sqlOperator = "=";
		}
		Date effectiveDate = dateParam.getValue();

		paramWrapper.setParameterType("Date");
		paramWrapper.setParameters(Arrays.asList(omopTableColumn));
		paramWrapper.setOperators(Arrays.asList(sqlOperator));
		paramWrapper.setValues(Arrays.asList(String.valueOf(effectiveDate.getTime())));
		paramWrapper.setRelationship("or");
	}

	private CodeableConceptDt retrieveCodeableConcept(Concept concept) {
		CodeableConceptDt conditionCodeableConcept = null;
		try {
			conditionCodeableConcept = CodeableConceptUtil.createFromConcept(concept);
		} catch (FHIRException fe) {
			logger.error("Could not generate CodeableConcept from Concept.", fe);
		}
		return conditionCodeableConcept;
	}

	private void addPersonToCondition(ConditionOccurrence conditionOccurrence, Condition condition) {
		// Condition.subject
		FPerson fPerson = conditionOccurrence.getFPerson();
		// set the person
		ResourceReferenceDt subjectRef = new ResourceReferenceDt(new IdDt(PatientResourceProvider.getType(), fPerson.getId()));
		subjectRef.setDisplay(fPerson.getNameAsSingleString());
		condition.setPatient(subjectRef);
	}

	private void addCodeToCondition(ConditionOccurrence conditionOccurrence, Condition condition) {
		// Condition.code SNOMED-CT
		Concept conditionConcept = conditionOccurrence.getConditionConcept();
		if (conditionConcept != null) {
			CodeableConceptDt conditionCodeableConcept = retrieveCodeableConcept(conditionConcept);
			if (conditionCodeableConcept != null) {
				condition.setCode(conditionCodeableConcept);
			}
		}
	}

	private void addStartAndEndDateToCondition(ConditionOccurrence conditionOccurrence, Condition condition) {
		// Condition.onsetDateTime
		Date startDate = conditionOccurrence.getConditionStartDate();
		if (startDate != null) {
			DateTimeDt onsetDateTime = new DateTimeDt(startDate);
			condition.setOnset(onsetDateTime);
		}
		// Condition.abatementDateTime
		Date endDate = conditionOccurrence.getConditionEndDate();
		if (endDate != null) {
			DateTimeDt abatementDateTime = new DateTimeDt(endDate);
			condition.setAbatement(abatementDateTime);
		}
	}

	private void addTypeToCondition(ConditionOccurrence conditionOccurrence, Condition condition) {
		// Condition.category
		Concept typeConceptId = conditionOccurrence.getConditionTypeConcept();
		if (typeConceptId != null) {
//			String systemUri = ConditionCategoryCodesEnum.PROBLEMLISTITEM.getSystem();
			String systemUri = "http://hl7.org/fhir/condition-category";
			String code = null;

			String fhirCategoryCode = OmopConceptMapping.fhirForConditionTypeConcept(typeConceptId.getId());
			if (OmopConceptMapping.COND_NULL.fhirCode.equals(fhirCategoryCode)) {
				// We couldn't fine one. Default to problem-list
				code = ConditionCategory.PROBLEMLISTITEM.toCode(); // default
			} else {
				code = fhirCategoryCode;
			}

//			CodeableConcept typeCodeableConcept = retrieveCodeableConcept(typeConceptId);
			CodingDt typeCoding = new CodingDt();
			typeCoding.setSystem(systemUri);
			typeCoding.setCode(code);
			CodeableConceptDt typeCodeableConcept = new CodeableConceptDt();
			typeCodeableConcept.addCoding(typeCoding);
			condition.setCategory((ConditionCategoryCodesEnum.forCode(code)));
//			if (typeCodeableConcept != null) {
//				List<CodeableConcept> typeList = new ArrayList<CodeableConcept>();
//				typeList.add(typeCodeableConcept);
//				condition.setCategory(typeList);
//			}
		}
	}

	private void addAsserterToCondition(ConditionOccurrence conditionOccurrence, Condition condition) {
		// Condition.asserter
		Provider provider = conditionOccurrence.getProvider();
		if (provider != null) {
			ResourceReferenceDt providerRef = new ResourceReferenceDt(new IdDt(PractitionerResourceProvider.getType(), provider.getId()));
			providerRef.setDisplay(provider.getProviderName());
			condition.setAsserter(providerRef);
		}
	}

	private void addContextToCondition(ConditionOccurrence conditionOccurrence, Condition condition) {
		// Condition.context
		VisitOccurrence visitOccurrence = conditionOccurrence.getVisitOccurrence();
		if (visitOccurrence != null) {
			ResourceReferenceDt visitRef = new ResourceReferenceDt(
					new IdDt(EncounterResourceProvider.getType(), visitOccurrence.getId()));
			condition.setEncounter(visitRef);
		}
	}

	@Override
	public ConditionOccurrence constructOmop(Long omopId, Condition fhirResource) {
		// things to update Condition_Occurrence, Concept, FPerson, Provider,
		// VisitOccurrence
		ConditionOccurrence conditionOccurrence;
		FPerson fPerson;
		Provider provider;

		// check for an existing condition
		if (omopId != null) {
			conditionOccurrence = conditionOccurrenceService.findById(omopId);
		} else {
			conditionOccurrence = new ConditionOccurrence();
		}

		// get the Subject
		if (fhirResource.getPatient() != null) {
			Long subjectId = fhirResource.getPatient().getReferenceElement().getIdPartAsLong();
			Long subjectFhirId = IdMapping.getOMOPfromFHIR(subjectId, PatientResourceProvider.getType());
			fPerson = fPersonService.findById(subjectFhirId);
			if (fPerson == null) {
				try {
					throw new FHIRException("Could not get Person class.");
				} catch (FHIRException e) {
					e.printStackTrace();
				}
			}
			conditionOccurrence.setFPerson(fPerson);
		} else {
			// throw an error
			try {
				throw new FHIRException("FHIR Resource does not contain a Subject.");
			} catch (FHIRException e) {
				e.printStackTrace();
			}
		}

		// get the Provider
		if (fhirResource.getAsserter() != null && !fhirResource.getAsserter().isEmpty()) {
			Long providerId = fhirResource.getAsserter().getReferenceElement().getIdPartAsLong();
			Long providerOmopId = IdMapping.getOMOPfromFHIR(providerId, PractitionerResourceProvider.getType());
			provider = providerService.findById(providerOmopId);
			if (provider != null) {
				conditionOccurrence.setProvider(provider);
			}
		}

		// get the concept code
		CodeableConceptDt code = fhirResource.getCode();
		String valueSourceString = null;
		Concept concept = fhirCode2OmopConcept(conceptService, code, valueSourceString);
		conditionOccurrence.setConditionConcept(concept);

//		if (code != null) {
//			List<Coding> codes = code.getCoding();
//			Concept omopConcept;
//			// there is only one so get the first
//			try {
//				omopConcept = CodeableConceptUtil.getOmopConceptWithFhirConcept(conceptService, codes.get(0));
//				// set the concept
//				conditionOccurrence.setConceptId(omopConcept);
//			} catch (FHIRException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		} else {
//			// is there a generic condition concept to use?
//			try {
//				throw new FHIRException("FHIR Resource does not contain a Condition Code.");
//			} catch (FHIRException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}

		// get the start and end date. We are expecting both to be of type DateTimeType
		IDatatype onSet = fhirResource.getOnset();
		if (onSet != null && onSet instanceof DateTimeDt) {
			conditionOccurrence.setConditionStartDate(((DateTimeDt) fhirResource.getOnset()).getValueAsCalendar().getTime());
		} if (onSet != null && onSet instanceof PeriodDt) {
			PeriodDt period = (PeriodDt)onSet;
			Date start = period.getStart();
			Date end = period.getEnd();
			if (start != null) conditionOccurrence.setConditionStartDate(start);
			if (end != null) conditionOccurrence.setConditionEndDate(end);
		}

		if (fhirResource.getAbatement() != null && fhirResource.getAbatement() instanceof DateTimeDt) {
//			conditionOccurrence.setEndDate(((DateTimeDt) fhirResource.getAbatement()).toCalendar().getTime());
			conditionOccurrence.setConditionEndDate(((DateTimeDt) fhirResource.getAbatement()).getValueAsCalendar().getTime());
		} else {
			// leave alone, end date not required
		}

		// set the category
//		List<CodeableConceptDt> categories = fhirResource.getCategory();
		BoundCodeableConceptDt<ConditionCategoryCodesEnum> category= fhirResource.getCategory();
		Long typeConceptId = 0L;
//		for (CodeableConceptDt category : categories) {
			List<CodingDt> codings = category.getCoding();
			for (CodingDt coding : codings) {
				String fhirSystem = coding.getSystem();
				String fhirCode = coding.getCode();
				if (fhirSystem == null || fhirSystem.isEmpty() || fhirCode == null || fhirCode.isEmpty()) {
					continue;
				}
				try {
					typeConceptId = OmopConceptMapping.omopForConditionCategoryCode(fhirCode);
				} catch (FHIRException e) {
					e.printStackTrace();
				}
				if (typeConceptId > 0L)
					break;
			}
			if (typeConceptId > 0L)
//				break;
//		}

		concept = conceptService.findById(typeConceptId);
		conditionOccurrence.setConditionTypeConcept(concept);

		// set the context
		/* Set visit occurrence */
		ResourceReferenceDt contextReference = fhirResource.getEncounter();
		VisitOccurrence visitOccurrence = fhirContext2OmopVisitOccurrence(visitOccurrenceService, contextReference);
		if (visitOccurrence != null) {
			conditionOccurrence.setVisitOccurrence(visitOccurrence);
		}

		return conditionOccurrence;
	}
}