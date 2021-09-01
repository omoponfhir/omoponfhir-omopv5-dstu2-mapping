package edu.gatech.chai.omoponfhir.omopv5.dstu2.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.exceptions.FHIRException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.model.dstu2.composite.AnnotationDt;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.composite.SimpleQuantityDt;
import ca.uhn.fhir.model.dstu2.resource.Immunization;
import ca.uhn.fhir.model.dstu2.resource.MedicationOrder;
import ca.uhn.fhir.model.dstu2.valueset.MedicationAdministrationStatusEnum;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.EncounterResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.ImmunizationResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.PatientResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.PractitionerResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.utilities.CodeableConceptUtil;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.utilities.DateUtil;
import edu.gatech.chai.omopv5.dba.service.ConceptService;
import edu.gatech.chai.omopv5.dba.service.DrugExposureService;
import edu.gatech.chai.omopv5.dba.service.FImmunizationViewService;
import edu.gatech.chai.omopv5.dba.service.FPersonService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.dba.service.ProviderService;
import edu.gatech.chai.omopv5.dba.service.VisitOccurrenceService;
import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.model.entity.DrugExposure;
import edu.gatech.chai.omopv5.model.entity.FImmunizationView;
import edu.gatech.chai.omopv5.model.entity.FPerson;
import edu.gatech.chai.omopv5.model.entity.Provider;
import edu.gatech.chai.omopv5.model.entity.VisitOccurrence;

/**
* <h1>OMOP to FHIR Immunization Mapping Class</h1>
* This class maps FHIR Immunication resource from/to OMOP CDM.
* <p>
* <b>Note:</b> Giving proper comments in your program makes it more
* user friendly and it is assumed as a high quality code.
*
* @author  Myung Choi
*/
public class OmopImmunization extends BaseOmopResource<Immunization, FImmunizationView, FImmunizationViewService> {

	final static Logger logger = LoggerFactory.getLogger(OmopImmunization.class);

	private VisitOccurrenceService visitOccurrenceService;
	private DrugExposureService drugExposureService;
	private ConceptService conceptService;
	private ProviderService providerService;
	private FPersonService fPersonService;

	private final static Long SELF_REPORTED_CONCEPTID = 44787730L;
	private final static Long PHYSICIAN_ADMINISTERED_PROCEDURE = 38000179L;
	
	private static String _columns = "distinct d.id as id,"
			+ " d.stopReason as stopReason,"
			+ " d.drugExposureStartDate as drugExposureStartDate,"
			+ " d.drugExposureStartDatetime as drugExposureStartDateTime,"
			+ " d.drugExposureEndDate as drugExposureEndDate,"
			+ " d.drugExposureEndDatetime as drugExposureEndDateTime,"
			+ " d.drugConcept as drugConceptId,"
			+ " c.vocabularyId as vaccineVocabularyId,"
			+ " c.conceptCode as vaccineConceptCode,"
			+ " c.conceptName as vaccineConceptName,"
			+ " d.person as persionId,"
			+ " fp.familyName as familyName,"
			+ " fp.given1Name as given1Name,"
			+ " fp.given2Name as given2Name,"
			+ " fp.prefixName as prefixName,"
			+ " fp.suffixName as suffixName,"
			+ " d.provider as providerId,"
			+ " pr.providerName as providerName,"
			+ " d.visitOccurrence as visitOccurrenceId,"
			+ " d.lotNumber as lotName,"
			+ " d.routeConcept_id as routeConceptId,"
			+ " r.vocabularyId as routeVocabularyId,"
			+ " r.conceptCode as routeConceptCode,"
			+ " r.conceptName as routeConceptName,"
			+ " d.quantity as quantity,"
			+ " d.sig as sig";
	
	private static String _from = "DrugExposure d join Concept c on d.drugConcept = c.id"
			+ " join ConceptRelationship cr on d.drugConcept = cr.id.conceptId2"
			+ " join Concept c2 on cr.id.conceptId1 = c2.id"
			+ " join Person p on d.fPerson = p.id"
			+ " join FPerson fp on d.fPerson = fp.id"
			+ " left join Provider pr on d.provider = pr.id"
			+ " left join VisitOccurrence v on d.visitOccurrence = v.id"
			+ " left join Concept r on d.routeConcept = r.id";
	
	private String _where = "c2.vocabularyId = 'CVX'";
	
	public OmopImmunization(WebApplicationContext context) {
		super(context, FImmunizationView.class, FImmunizationViewService.class, ImmunizationResourceProvider.getType());
		initialize(context);
	}

	private void initialize(WebApplicationContext context) {
		visitOccurrenceService = context.getBean(VisitOccurrenceService.class);
		conceptService = context.getBean(ConceptService.class);
		drugExposureService = context.getBean(DrugExposureService.class);
		providerService = context.getBean(ProviderService.class);
		fPersonService = context.getBean(FPersonService.class);

//		String sizeSql = "select count(distinct d) from " + _from + " where " + _where;
//		getSize(sizeSql, null);
		getSize();
	}

	@Override
	public Long toDbase(Immunization fhirResource, IdDt fhirId) throws FHIRException {
		Long omopId = null;
		DrugExposure drugExposure = null;
		if (fhirId != null) {
			omopId = fhirId.getIdPartAsLong();
		}

		drugExposure = constructDrugExposure(omopId, fhirResource);

		Long retOmopId = null;
		if (omopId == null) {
			retOmopId = drugExposureService.create(drugExposure).getId();
		} else {
			retOmopId = drugExposureService.update(drugExposure).getId();
		}
		return retOmopId;
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
		case MedicationOrder.SP_RES_ID:
			String immunizationId = ((TokenParam) value).getValue();
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(immunizationId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
			
		case Immunization.SP_VACCINE_CODE:
			String system = ((TokenParam) value).getSystem();
			String code = ((TokenParam) value).getValue();
//			System.out.println("\n\n\n\n\nSystem:"+system+"\n\ncode:"+code+"\n\n\n\n\n");
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
				paramWrapper.setParameters(Arrays.asList("immunizationConcept.conceptCode"));
				paramWrapper.setOperators(Arrays.asList("="));
				paramWrapper.setValues(Arrays.asList(code));
			} else if (!"None".equals(omopVocabulary) && (code == null || code.isEmpty())) {
				paramWrapper.setParameters(Arrays.asList("immunizationConcept.vocabularyId"));
				paramWrapper.setOperators(Arrays.asList("="));
				paramWrapper.setValues(Arrays.asList(omopVocabulary));
			} else {
				paramWrapper.setParameters(Arrays.asList("immunizationConcept.vocabularyId", "immunizationConcept.conceptCode"));
				paramWrapper.setOperators(Arrays.asList("=", "="));
				paramWrapper.setValues(Arrays.asList(omopVocabulary, code));
			}
			paramWrapper.setRelationship("and");
			mapList.add(paramWrapper);

			break;

		case Immunization.SP_DATE:
			DateRangeParam dateRangeParam = ((DateRangeParam) value);
			DateUtil.constructParameterWrapper(dateRangeParam, "immunizationDate", paramWrapper, mapList);
			break;
			
		case Immunization.SP_PATIENT:
			ReferenceParam patientReference = ((ReferenceParam) value);
			Long fhirPatientId = patientReference.getIdPartAsLong();
			String omopPersonIdString = String.valueOf(fhirPatientId);
			
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("fPerson.id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(omopPersonIdString));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);			
			break;
			
		default:
			mapList = null;
		}

		return mapList;
	}
	
	public String constructSearchSql(String whereStatement) {
		String searchSql = "select distinct d from " + _from + " where " + _where;
		if (whereStatement != null && !whereStatement.isEmpty()) {
			searchSql += " and " + whereStatement;
		}
		
		return searchSql;
	}
	
	public String constructSizeSql(String whereStatement) {
		String searchSql = "select count(distinct d) from " + _from + " where " + _where;
		if (whereStatement != null && !whereStatement.isEmpty()) {
			searchSql += " and " + whereStatement;
		}
		
		return searchSql;
	}

	public String mapParameter(String parameter, Object value, Map<String, String> parameterSet) {
		String whereStatement = "";
		
		switch (parameter) {
		case Immunization.SP_RES_ID:
			String immunizationId = ((TokenParam) value).getValue();
			whereStatement = "d.id = :drugExposureId";
			parameterSet.put("drugExposureId", "Long," + immunizationId);
			break;
			
		case Immunization.SP_VACCINE_CODE:
			List<TokenParam> codes = ((TokenOrListParam) value).getValuesAsQueryTokens();

			int i = 0;
			for (TokenParam code : codes) {
				String systemValue = code.getSystem();
				String codeValue = code.getValue();

				if ((systemValue == null || systemValue.isEmpty()) && (codeValue == null || codeValue.isEmpty())) {
					// This is not searchable. So, skip this. 
					continue;
				}

				String omopVocabulary = "None";
				if (systemValue != null && !systemValue.isEmpty()) {
					// Find OMOP vocabulary_id for this system. If not found,
					// put empty so that we can search it by code only (if provided).
					try {
						omopVocabulary = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(systemValue);
					} catch (FHIRException e) {
						e.printStackTrace();
						systemValue = "";
					}
				}

				if (systemValue != null && !systemValue.isEmpty() && codeValue != null && !codeValue.isEmpty()) {
					String vId = "c.vocabularyId = :" + "vaccineCodeSystem" + i;
					String cCode = "c.conceptCode = :" + "vaccineCodeCode" + i;
					String statement = "(" + vId + " and " + cCode + ")";
					
					whereStatement = (whereStatement == null || whereStatement.isEmpty()) ? statement : whereStatement + " or " + statement;
					parameterSet.put("vaccineCodeSystem"+i, "String," + omopVocabulary);
					parameterSet.put("vaccineCodeCode"+i, "String," + codeValue);					
				} else if ((systemValue == null || systemValue.isEmpty()) && codeValue != null && !codeValue.isEmpty()) {
					String statement = "c.conceptCode = :" + "vaccineCodeCode" + i;
					whereStatement = (whereStatement == null || whereStatement.isEmpty()) ? statement : whereStatement + " or " + statement;
					parameterSet.put("vaccineCodeCode"+i, "String," + codeValue);					
				} else if ((codeValue == null || codeValue.isEmpty()) && systemValue != null && !systemValue.isEmpty()) {
					String statement = "c.vocabularyId = :" + "vaccineCodeSystem" + i;
					whereStatement = (whereStatement == null || whereStatement.isEmpty()) ? statement : whereStatement + " or " + statement;
					parameterSet.put("vaccineCodeSystem"+i, "String," + omopVocabulary);					
				} else {
					continue; // no system or code
				}

				i++;
			}
			break;

		case Immunization.SP_DATE:
			DateRangeParam dateRangeParam = ((DateRangeParam) value);
			
			DateParam lowerDateParam = dateRangeParam.getLowerBound();
			DateParam upperDateParam = dateRangeParam.getUpperBound();
			if (lowerDateParam != null && upperDateParam != null) {
				// case 1
				String lowerSqlOperator = DateUtil.getSqlOperator(lowerDateParam.getPrefix());
				String upperSqlOperator = DateUtil.getSqlOperator(upperDateParam.getPrefix());
				
				whereStatement = "d.drugExposureStartDate " + lowerSqlOperator + " :drugExposureStartDate and " + "d.drugExposureEndDate " + upperSqlOperator + " :drugExposureEndDate";
				parameterSet.put("drugExposureStartDate", "Date," + String.valueOf(lowerDateParam.getValue().getTime()));
				parameterSet.put("drugExposureEndDate", "Date," + String.valueOf(upperDateParam.getValue().getTime()));
			} else if (lowerDateParam != null && upperDateParam == null) {
				String lowerSqlOperator = DateUtil.getSqlOperator(lowerDateParam.getPrefix());
				
				whereStatement = "d.drugExposureStartDate " + lowerSqlOperator + " :drugExposureStartDate";
				parameterSet.put("drugExposureStartDate", "Date," + String.valueOf(lowerDateParam.getValue().getTime()));
			} else {
				String upperSqlOperator = DateUtil.getSqlOperator(upperDateParam.getPrefix());
				
				whereStatement = "d.drugExposureEndDate " + upperSqlOperator + " :drugExposureEndDate";
				parameterSet.put("drugExposureEndDate", "Date," + String.valueOf(upperDateParam.getValue().getTime()));
			}
			break;
			
		case Immunization.SP_PATIENT:
			ReferenceParam patientReference = ((ReferenceParam) value);
			Long fhirPatientId = patientReference.getIdPartAsLong();
			String omopPersonIdString = String.valueOf(fhirPatientId);
			
			whereStatement = "p.id = :patient";
			parameterSet.put("patient", "Long," + omopPersonIdString);
			
			break;
			
		default:
			
		}
		
		
		return whereStatement;
	}

	public String constructOrderParams(SortSpec theSort) {
		if (theSort == null) return null;

		String direction;

		if (theSort.getOrder() != null) direction = theSort.getOrder().toString();
		else direction = "ASC";

		String orderParam = new String();

		if (theSort.getParamName().equals(Immunization.SP_VACCINE_CODE)) {
			orderParam = "d.drugConcept.conceptCode " + direction;
		} else if (theSort.getParamName().equals(Immunization.SP_DATE)) {
			orderParam = "d.drugExposureStartDate " + direction;
		} else if (theSort.getParamName().equals(Immunization.SP_PATIENT)) {
			orderParam = "d.person.id " + direction;
		} else {
			orderParam = "d.id " + direction;
		}

		String orderParams = orderParam;

		if (theSort.getChain() != null) {
			orderParams = orderParams.concat(","+constructOrderParams(theSort.getChain()));
		}

		return orderParams;
	}
	
	@Override
	public Immunization constructFHIR(Long fhirId, FImmunizationView entity) {
		Immunization immunization = new Immunization();
		immunization.setId(new IdDt(fhirId));
		
		// Set some static default values
		immunization.setWasNotGiven(false);
		
		// Set patient
		ResourceReferenceDt patientReference = new ResourceReferenceDt(new IdDt(PatientResourceProvider.getType(), entity.getFPerson().getId()));
		patientReference.setDisplay(entity.getFPerson().getNameAsSingleString());
		immunization.setPatient(patientReference);
		
		// status - set to stopped if we have stop reason. Otherwise, we just set it to
		if (entity.getImmunizationStatus() != null && !entity.getImmunizationStatus().isEmpty()) {
			immunization.setStatus(MedicationAdministrationStatusEnum.STOPPED.getCode());
		} else {
			immunization.setStatus(MedicationAdministrationStatusEnum.COMPLETED.getCode());
		}

		// date
		immunization.setDate(new DateTimeDt(entity.getImmunizationDate()));
		
		// vaccine code
		CodeableConceptDt vaccineCodeable = CodeableConceptUtil.getCodeableConceptFromOmopConcept(entity.getImmunizationConcept(), getFhirOmopVocabularyMap());
		immunization.setVaccineCode(vaccineCodeable);
		
		// performer
		Provider provider = entity.getProvider();
		if (provider != null) {
			ResourceReferenceDt performerReference = new ResourceReferenceDt(new IdDt(PractitionerResourceProvider.getType(), entity.getProvider().getId()));
//			performerReference.setDisplay(entity.getProvider().getProviderName());
			immunization.setPerformer(performerReference);
		}
		
		// encounter
		VisitOccurrence visitOccurrence = entity.getVisitOccurrence();
		if (visitOccurrence != null) {
			ResourceReferenceDt encounterReference = new ResourceReferenceDt(new IdDt(EncounterResourceProvider.getType(), entity.getVisitOccurrence().getId()));
			immunization.setEncounter(encounterReference);
		}
		
		// lot number
		String lotNumber = entity.getLotNumber();
		if (lotNumber != null && !lotNumber.isEmpty()) {
			immunization.setLotNumber(lotNumber);
		}
		
		// route
		Concept routeConcept = entity.getRouteConcept();
		if (routeConcept != null) {
			CodeableConceptDt routeCodeable = CodeableConceptUtil.getCodeableConceptFromOmopConcept(routeConcept, getFhirOmopVocabularyMap());
			immunization.setRoute(routeCodeable);
		}
		
		// quantity
		Double quantity = entity.getQuantity();
		if (quantity != null && !quantity.isInfinite() && !quantity.isNaN()) {
			immunization.setDoseQuantity(new SimpleQuantityDt(quantity));
		}
		
		// sig
		String note = entity.getImmunizationNote();
		if (note != null && !note.isEmpty()) {
			List<AnnotationDt> theValue = new ArrayList<AnnotationDt> ();
			AnnotationDt annotation = new AnnotationDt();
			annotation.setText(note);
			immunization.setNote(theValue);
		}
		
		// reported if self reported
		Concept drugTypeConcept = entity.getImmunizationTypeConcept();
		if (drugTypeConcept.getId() == OmopImmunization.SELF_REPORTED_CONCEPTID) {
			immunization.setReported(true);
		} else {
			immunization.setReported(false);
		}

		return immunization;
	}

	public DrugExposure constructDrugExposure(Long omopId, Immunization fhirResource) {
		DrugExposure drugExposure = null;
		if (omopId != null) {
			// Update
			drugExposure = drugExposureService.findById(omopId);
			if (drugExposure == null) {
				throw new FHIRException(fhirResource.getId() + " does not exist");
			}
		} else {
			// Create
			List<IdentifierDt> identifiers = fhirResource.getIdentifier();
			for (IdentifierDt identifier : identifiers) {
				if (identifier.isEmpty())
					continue;
				String identifierValue = identifier.getValue();
				List<DrugExposure> results = drugExposureService.searchByColumnString("drugSourceValue",
						identifierValue);
				if (results.size() > 0) {
					drugExposure = results.get(0);
					omopId = drugExposure.getId();
					break;
				}
			}

			if (drugExposure == null) {
				drugExposure = new DrugExposure();
				// Add the source column.
				IdentifierDt identifier = fhirResource.getIdentifierFirstRep();
				if (!identifier.isEmpty()) {
					drugExposure.setDrugSourceValue(identifier.getValue());
				}
			}
		}

		// Set patient.
		ResourceReferenceDt patientReference = fhirResource.getPatient();
		if (patientReference == null)
			throw new FHIRException("Patient must exist.");

		Long omopFPersonId = patientReference.getReferenceElement().getIdPartAsLong();

		FPerson fPerson = fPersonService.findById(omopFPersonId);
		if (fPerson == null)
			throw new FHIRException("Patient/" + omopFPersonId + " is not valid");

		drugExposure.setFPerson(fPerson);

		// Find drug type concept from reported
		boolean reported = fhirResource.getReported();
		if (reported) {
			drugExposure.setDrugTypeConcept(new Concept(OmopImmunization.SELF_REPORTED_CONCEPTID));
		} else {
			drugExposure.setDrugTypeConcept(new Concept(OmopImmunization.PHYSICIAN_ADMINISTERED_PROCEDURE));
		}
		
		// status
		String status = fhirResource.getStatus();
		if (status != null && status.equals(MedicationAdministrationStatusEnum.STOPPED.getCode())) {
			drugExposure.setStopReason(MedicationAdministrationStatusEnum.STOPPED.getCode());
		}
		
		// vaccine code to drug concept		
		CodeableConceptDt vaccineCode = fhirResource.getVaccineCode();
		if (vaccineCode.isEmpty()) {
			throw new FHIRException("vaccineCode cannot be empty");
		}

		Concept drugConcept = null;
		for (CodingDt vaccineCodeCoding : vaccineCode.getCoding()) {
			drugConcept = CodeableConceptUtil.getOmopConceptWithFhirConcept(conceptService, vaccineCodeCoding);
			if (drugConcept != null) break;
		}
		
		if (drugConcept == null) drugConcept = new Concept(0L);
		drugExposure.setDrugConcept(drugConcept);
		drugExposure.setDrugSourceValue(vaccineCode.getCodingFirstRep().getSystem()+":"+vaccineCode.getCodingFirstRep().getCode());
		
		// date. we only have one date.
		Date startDate = fhirResource.getDate();
		if (startDate == null) {
			throw new FHIRException("date cannot be null");
		}
		
		drugExposure.setDrugExposureStartDate(startDate);
		drugExposure.setDrugExposureStartDateTime(startDate);
		drugExposure.setDrugExposureEndDate(startDate);
		
		// performer
		ResourceReferenceDt performerReference = fhirResource.getPerformer();
		if (!performerReference.isEmpty()) {
			Long performerId = performerReference.getReferenceElement().getIdPartAsLong();
			Provider provider = providerService.findById(performerId);
			if (provider == null) {
				throw new FHIRException("performer (" + performerId + ") does not exist");
			}
			
			drugExposure.setProvider(new Provider(performerId));
		}
		
		// encounter
		ResourceReferenceDt encounterReference = fhirResource.getEncounter();
		if (!encounterReference.isEmpty()) {
			Long encounterId = encounterReference.getReferenceElement().getIdPartAsLong();
			VisitOccurrence visitOccurrence = visitOccurrenceService.findById(encounterId);
			if (visitOccurrence == null) {
				throw new FHIRException("encounter (" + encounterId + ") does not exist");
			}
			
			drugExposure.setVisitOccurrence(visitOccurrence);
		}
		
		// lotNumber
		String lotNumber = fhirResource.getLotNumber();
		if (lotNumber != null && !lotNumber.isEmpty()) {
			drugExposure.setLotNumber(lotNumber);
		}
		
		// route
		CodeableConceptDt routeCode = fhirResource.getRoute();
		if (!routeCode.isEmpty()) {
			Concept routeConcept = null;
			for (CodingDt routeCodeCoding : routeCode.getCoding()) {
				routeConcept = CodeableConceptUtil.getOmopConceptWithFhirConcept(conceptService, routeCodeCoding);
				if (routeConcept != null) break;
			}
			
			if (routeConcept == null) {
				drugExposure.setRouteConcept(new Concept (0L));
			}
			
			drugExposure.setRouteSourceValue(routeCode.getCodingFirstRep().getSystem()+":"+routeCode.getCodingFirstRep().getCode()+":"+routeCode.getCodingFirstRep().getDisplay());
		}
		
		// doseQuantity
		SimpleQuantityDt doseQuantity = fhirResource.getDoseQuantity();
		if (!doseQuantity.isEmpty()) {
			drugExposure.setQuantity(doseQuantity.getValue().doubleValue());
		}
		
		// note
		List<AnnotationDt> note = fhirResource.getNote();
		String sigText = "";
		for (AnnotationDt noteAnnotation : note) {
			sigText += noteAnnotation.getText() + " ";
		}
		
		if (!sigText.isEmpty() ) {
			drugExposure.setSig(sigText.trim());
		}
		
		return drugExposure;	
	}

	@Override
	public FImmunizationView constructOmop(Long omopId, Immunization fhirResource) {
		// this is a view. So, it's read-only
		return null;
	}

}