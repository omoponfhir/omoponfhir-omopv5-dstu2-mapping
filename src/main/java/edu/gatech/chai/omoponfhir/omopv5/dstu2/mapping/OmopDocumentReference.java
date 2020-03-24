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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

//import org.hl7.fhir.dstu3.model.Attachment;
import ca.uhn.fhir.model.dstu2.composite.AttachmentDt;
//import org.hl7.fhir.dstu3.model.CodeableConcept;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
//import org.hl7.fhir.dstu3.model.Coding;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
//import org.hl7.fhir.dstu3.model.DocumentReference;
import ca.uhn.fhir.model.dstu2.resource.DocumentReference;
//import org.hl7.fhir.dstu3.model.DocumentReference.DocumentReferenceContentComponent;
import ca.uhn.fhir.model.dstu2.resource.DocumentReference.Content;
//import org.hl7.fhir.dstu3.model.DocumentReference.DocumentReferenceContextComponent;
import ca.uhn.fhir.model.dstu2.resource.DocumentReference.Context;
//import org.hl7.fhir.dstu3.model.Encounter;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
//import org.hl7.fhir.dstu3.model.Enumerations.DocumentReferenceStatus;
import ca.uhn.fhir.model.dstu2.valueset.DocumentReferenceStatusEnum;
//import org.hl7.fhir.dstu3.model.IdType;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.IdDt;
//import org.hl7.fhir.dstu3.model.Patient;
import ca.uhn.fhir.model.dstu2.resource.Patient;
//import org.hl7.fhir.dstu3.model.Reference;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;

import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.model.MyDocumentReference;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.DocumentReferenceResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.EncounterResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.PatientResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.PractitionerResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.utilities.CodeableConceptUtil;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.utilities.DateUtil;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.utilities.FHIRException;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.utilities.ThrowFHIRExceptions;
import edu.gatech.chai.omopv5.dba.service.ConceptService;
import edu.gatech.chai.omopv5.dba.service.FPersonService;
import edu.gatech.chai.omopv5.dba.service.NoteService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.dba.service.ProviderService;
import edu.gatech.chai.omopv5.dba.service.VisitOccurrenceService;
import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.model.entity.FPerson;
import edu.gatech.chai.omopv5.model.entity.Note;
import edu.gatech.chai.omopv5.model.entity.Provider;
import edu.gatech.chai.omopv5.model.entity.VisitOccurrence;

/***
 * DocumentReference
 * 
 * @author mc142
 * 
 *         maps to Note table
 *
 */
public class OmopDocumentReference extends BaseOmopResource<DocumentReference, Note, NoteService>
		implements IResourceMapping<DocumentReference, Note> {

	private static OmopDocumentReference omopDocumentReference = new OmopDocumentReference();
	private ConceptService conceptService;
	private FPersonService fPersonService;
	private ProviderService providerService;
	private VisitOccurrenceService visitOccurrenceService;

	public OmopDocumentReference() {
		super(ContextLoaderListener.getCurrentWebApplicationContext(), Note.class, NoteService.class,
				DocumentReferenceResourceProvider.getType());
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}

	public OmopDocumentReference(WebApplicationContext context) {
		super(context, Note.class, NoteService.class, DocumentReferenceResourceProvider.getType());
		initialize(context);
	}

	private void initialize(WebApplicationContext context) {
		conceptService = context.getBean(ConceptService.class);
		fPersonService = context.getBean(FPersonService.class);
		providerService = context.getBean(ProviderService.class);
		visitOccurrenceService = context.getBean(VisitOccurrenceService.class);
		
		getSize();
	}

	public static OmopDocumentReference getInstance() {
		return OmopDocumentReference.omopDocumentReference;
	}

	@Override
	public Long toDbase(DocumentReference fhirResource, IdDt fhirId) throws FHIRException {
		Long omopId = null;
		if (fhirId != null) {
			// Update
			omopId = IdMapping.getOMOPfromFHIR(fhirId.getIdPartAsLong(), DocumentReferenceResourceProvider.getType());
		}
		
		Note note = constructOmop(omopId, fhirResource);
		
		Long OmopRecordId = null;
		if (omopId == null) {
			OmopRecordId = getMyOmopService().create(note).getId();
		} else {
			OmopRecordId = getMyOmopService().update(note).getId();
		}
		
		return IdMapping.getFHIRfromOMOP(OmopRecordId, DocumentReferenceResourceProvider.getType());
	}

	@Override
	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
		ParameterWrapper paramWrapper = new ParameterWrapper();
        if (or) paramWrapper.setUpperRelationship("or");
        else paramWrapper.setUpperRelationship("and");

		switch (parameter) {
		case DocumentReference.SP_RES_ID:
			String documentReferenceId = ((TokenParam) value).getValue();
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(documentReferenceId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case DocumentReference.SP_ENCOUNTER:
			Long fhirId = ((ReferenceParam) value).getIdPartAsLong();
			Long omopVisitOccurrenceId = IdMapping.getOMOPfromFHIR(fhirId, DocumentReferenceResourceProvider.getType());
			
			if (omopVisitOccurrenceId != null) {
				paramWrapper.setParameterType("Long");
				paramWrapper.setParameters(Arrays.asList("visitOccurrence.id"));
				paramWrapper.setOperators(Arrays.asList("="));
				paramWrapper.setValues(Arrays.asList(String.valueOf(omopVisitOccurrenceId)));
				paramWrapper.setRelationship("or");
				mapList.add(paramWrapper);
			}
			break;
		case DocumentReference.SP_CREATED:
		case DocumentReference.SP_INDEXED:
			Date date = ((DateParam) value).getValue();
			ParamPrefixEnum prefix = ((DateParam) value).getPrefix();
			String inequality = "=";
			if (prefix.equals(ParamPrefixEnum.EQUAL)) inequality = "=";
			else if (prefix.equals(ParamPrefixEnum.LESSTHAN)) inequality = "<";
			else if (prefix.equals(ParamPrefixEnum.LESSTHAN_OR_EQUALS)) inequality = "<=";
			else if (prefix.equals(ParamPrefixEnum.GREATERTHAN)) inequality = ">";
			else if (prefix.equals(ParamPrefixEnum.GREATERTHAN_OR_EQUALS)) inequality = ">=";
			else if (prefix.equals(ParamPrefixEnum.NOT_EQUAL)) inequality = "!=";

			// get Date.
			SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
			String time = timeFormat.format(date);

			// get only date part.
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Date dateWithoutTime = null;
			try {
				dateWithoutTime = sdf.parse(sdf.format(date));
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				break;
			}

			System.out.println("TIME VALUE:"+String.valueOf(dateWithoutTime.getTime()));
			paramWrapper.setParameterType("Date");
			paramWrapper.setParameters(Arrays.asList("date"));
			paramWrapper.setOperators(Arrays.asList(inequality));
			paramWrapper.setValues(Arrays.asList(String.valueOf(dateWithoutTime.getTime())));
			paramWrapper.setRelationship("and");
			mapList.add(paramWrapper);
			
			// Time
			ParameterWrapper paramWrapper_time = new ParameterWrapper();
			paramWrapper_time.setParameterType("String");
			paramWrapper_time.setParameters(Arrays.asList("time"));
			paramWrapper_time.setOperators(Arrays.asList(inequality));
			paramWrapper_time.setValues(Arrays.asList(time));
			paramWrapper_time.setRelationship("and");
			mapList.add(paramWrapper_time);
			break;
		case DocumentReference.SP_TYPE:
			String system = ((TokenParam) value).getSystem();
			String code = ((TokenParam) value).getValue();
			
			if ((system == null || system.isEmpty()) && (code == null || code.isEmpty()))
				break;
			
			String omopVocabulary = "None";
			if (system != null && !system.isEmpty()) {
				try {
					omopVocabulary = OmopCodeableConceptMapping.omopVocabularyforFhirUri(system);
					// DocumentReference is mapped from Note table, which uses Note Type concept code.
					// If this is LOINC, we can try to convert this to Note Type.
					if ("LOINC".equals(omopVocabulary)) {
						// Get concept id.
						Concept loincConcept = CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService, omopVocabulary, code);
						Long omopConceptId = OmopNoteTypeMapping.getOmopConceptIdFor(loincConcept.getId());
						if (!omopConceptId.equals(0L)) {
							// We found the mapping. Use this to compare with concept id.
							paramWrapper.setParameterType("Long");
							paramWrapper.setParameters(Arrays.asList("typeConcept.id"));
							paramWrapper.setOperators(Arrays.asList("="));
							paramWrapper.setValues(Arrays.asList(String.valueOf(omopConceptId)));
							paramWrapper.setRelationship("and");
							mapList.add(paramWrapper);
							break;
						}
					}
				} catch (FHIRException e) {
					e.printStackTrace();
				}
			} 
			
			paramWrapper.setParameterType("String");
			if ("None".equals(omopVocabulary) && code != null && !code.isEmpty()) {
				paramWrapper.setParameters(Arrays.asList("typeConcept.conceptCode"));
				paramWrapper.setOperators(Arrays.asList("="));
				paramWrapper.setValues(Arrays.asList(code));
			} else if (!"None".equals(omopVocabulary) && (code == null || code.isEmpty())) {
				paramWrapper.setParameters(Arrays.asList("typeConcept.vocabulary"));
				paramWrapper.setOperators(Arrays.asList("="));
				paramWrapper.setValues(Arrays.asList(omopVocabulary));				
			} else {
				paramWrapper.setParameters(Arrays.asList("typeConcept.vocabulary", "typeConcept.conceptCode"));
				paramWrapper.setOperators(Arrays.asList("=","="));
				paramWrapper.setValues(Arrays.asList(omopVocabulary, code));
			}
			paramWrapper.setRelationship("and");
			mapList.add(paramWrapper);
			break;
		case "Patient:" + Patient.SP_RES_ID:
			addParamlistForPatientIDName(parameter, (String)value, paramWrapper, mapList);
			break;
		case "Patient:" + Patient.SP_NAME:
			addParamlistForPatientIDName(parameter, (String)value, paramWrapper, mapList);
			break;
		default:
			mapList = null;
		}
		
        return mapList;
	}

	@Override
	public Note constructOmop(Long omopId, DocumentReference fhirResource) {
		Note note = null;
		if (omopId == null) {
			// Create
			note = new Note();
		} else {
			// Update
			note = getMyOmopService().findById(omopId);
			if (note == null) {
				try {
					throw new FHIRException(fhirResource.getId() + " does not exist");
				} catch (FHIRException e) {
					e.printStackTrace();
				}
			}
		}
		
		// get type
		CodeableConceptDt typeCodeableConcept = fhirResource.getType();
		CodingDt loincCoding = null;
		Concept typeOmopConcept = null;
		Concept typeFhirConcept = null;
		if (typeCodeableConcept != null & !typeCodeableConcept.isEmpty()) {
			for (CodingDt coding: typeCodeableConcept.getCoding()) {
				try {
					typeFhirConcept = CodeableConceptUtil.getOmopConceptWithFhirConcept(conceptService, coding);
				} catch (FHIRException e) {
					typeFhirConcept = null;
					e.printStackTrace();
				}
				if ("http://loinc.org".equals(coding.getSystem()) ||
						"urn:oid:2.16.840.1.113883.6.1".equals(coding.getSystem())) {
					loincCoding = coding;
					break;
				}
			}
			
			if (typeFhirConcept == null) {
				ThrowFHIRExceptions.unprocessableEntityException("The type codeableconcept is not recognized");
			}
			
			if (loincCoding != null) {
				// We found loinc coding. See if we can convert to Note Type concept.
				Long typeOmopConceptId = OmopNoteTypeMapping.getOmopConceptIdFor(typeFhirConcept.getId());
				typeOmopConcept = conceptService.findById(typeOmopConceptId);
			}
			
			if (typeOmopConcept == null) {
				// We couldn't get Note Type concept. Just use the fhirConcept as is.
				typeOmopConcept = typeFhirConcept;
			}
			
			note.setType(typeOmopConcept);
		} else {
			ThrowFHIRExceptions.unprocessableEntityException("The type codeableconcept cannot be null");
		}
		
		// Get patient
		ResourceReferenceDt subject = fhirResource.getSubject();
		if (subject == null || subject.isEmpty()) {
			ThrowFHIRExceptions.unprocessableEntityException("Subject(Patient) must be provided");
		}
		
		if (subject.getReferenceElement().getResourceType().equals(PatientResourceProvider.getType())) {
			// get patient ID.
			Long patientId = subject.getReferenceElement().getIdPartAsLong();
			Long omopPersonId = IdMapping.getOMOPfromFHIR(patientId, PatientResourceProvider.getType());
			FPerson fPerson = fPersonService.findById(omopPersonId);
			if (fPerson == null) {
				ThrowFHIRExceptions.unprocessableEntityException("Patient does not exist");
			}
			
			note.setFPerson(fPerson);
		} else {
			ThrowFHIRExceptions.unprocessableEntityException("Only Patient is supported for subject");
		}
		
		// get indexed.
		Date indexedDate = fhirResource.getIndexed();
		if (indexedDate != null) {
			note.setDate(indexedDate);
			
			SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
			note.setTime(timeFormat.format(indexedDate));
		}
		
		// get author.
//		ResourceReferenceDt authorReference = fhirResource.getAuthorFirstRep();
		if (fhirResource.getAuthor().isEmpty()) {
			fhirResource.addAuthor();
		}
		ResourceReferenceDt authorReference = fhirResource.getAuthor().get(0);
		if (authorReference != null && !authorReference.isEmpty()) {
			if (authorReference.getReferenceElement().getResourceType().equals(PractitionerResourceProvider.getType())) {
				Long practitionerId = authorReference.getReferenceElement().getIdPartAsLong();
				Long omopProviderId = IdMapping.getOMOPfromFHIR(practitionerId, PractitionerResourceProvider.getType());
				Provider provider = providerService.findById(omopProviderId);
				if (provider != null) {
					note.setProvider(provider);
				} else {
					ThrowFHIRExceptions.unprocessableEntityException("Author practitioner does not exist");
				}
			} else {
				ThrowFHIRExceptions.unprocessableEntityException("Only practitioner is supported for author");
			}
		}
		
		// get encounter.
		Context context = fhirResource.getContext();
		if (context != null && !context.isEmpty()) {
			ResourceReferenceDt encounterReference = context.getEncounter();
			if (encounterReference != null && !encounterReference.isEmpty()) {
				Long encounterId = encounterReference.getReferenceElement().getIdPartAsLong();
				Long omopVisitOccurrenceId = IdMapping.getOMOPfromFHIR(encounterId, EncounterResourceProvider.getType());
				VisitOccurrence visitOccurrence = visitOccurrenceService.findById(omopVisitOccurrenceId);
				if (visitOccurrence != null) {
					note.setVisitOccurrence(visitOccurrence);
				} else {
					ThrowFHIRExceptions.unprocessableEntityException("context.encounter does not exist");
				}
			}
		}
		
		// get content.
		String note_text = new String();
		List<Content> contents = fhirResource.getContent();
		for (Content content: contents) {
			AttachmentDt attachment = content.getAttachment();
			if (attachment == null || attachment.isEmpty()) {
				ThrowFHIRExceptions.unprocessableEntityException("content.attachment cannot be empty");
			}
			
			String contentType = attachment.getContentType();
			if (contentType != null && !contentType.isEmpty()) {
				if (!"text/plain".equals(contentType)) {
					ThrowFHIRExceptions.unprocessableEntityException("content.attachment must be text/plain");
				}
				
				byte[] data = attachment.getData();
				if (data == null) {
					data = attachment.getHash();
				}
				
				if (data == null) {
					ThrowFHIRExceptions.unprocessableEntityException("content.attachment.data or hash must exist");
				}
				
				// get text.
				String data_text = new String(data);
				note_text = note_text.concat(data_text);
			} else {
				ThrowFHIRExceptions.unprocessableEntityException("content.attachment.contentType must be specified as text/plain");
			}
		}
		
		if (note_text.isEmpty()) {
			ThrowFHIRExceptions.unprocessableEntityException("content.attachment.data and hash data seems to be empty");
		}
		
		note.setNoteText(note_text);
		
		return note;
	}
	
	@Override
	public DocumentReference constructResource(Long fhirId, Note entity, List<String> includes) {
		DocumentReference documentReference = constructFHIR(fhirId, entity);
		
		if (!includes.isEmpty()) {
			if (includes.contains("DocumentReference:patient") || includes.contains("DocumentReference:subject")) {
//				if (documentReference.hasSubject()) {
				if (!documentReference.getSubject().isEmpty()) {
					Long patientFhirId = documentReference.getSubject().getReferenceElement().getIdPartAsLong();
					Patient patient = OmopPatient.getInstance().constructFHIR(patientFhirId, entity.getFPerson());
					documentReference.getSubject().setResource(patient);
				}
			}
			if (includes.contains("DocumentReference:encounter")) {
//				if (documentReference.hasContext()) {
				if (documentReference.getContent().isEmpty()) {
					Context documentContext = documentReference.getContext();
//					if (documentContext.hasEncounter()) {
					if (documentContext.getEncounter().isEmpty()) {
						Long encounterFhirId = documentContext.getEncounter().getReferenceElement().getIdPartAsLong();
						Encounter encounter = OmopEncounter.getInstance().constructFHIR(encounterFhirId, entity.getVisitOccurrence());
						documentContext.getEncounter().setResource(encounter);
					}
				}
			}
		}
		
		return documentReference;
	}

	@Override
	public DocumentReference constructFHIR(Long fhirId, Note entity) {
		MyDocumentReference documentReference = new MyDocumentReference();

		documentReference.setId(new IdDt(fhirId));

		// status: hard code to current.
		documentReference.setStatus(DocumentReferenceStatusEnum.CURRENT);

		// type: map OMOP's Note Type concept to LOINC code if possible.
		Concept omopTypeConcept = entity.getType();
		CodeableConceptDt typeCodeableConcept = null;
		if ("Note Type".equals(omopTypeConcept.getVocabulary())) {
			Long loincConceptId = OmopNoteTypeMapping.getLoincConceptIdFor(omopTypeConcept.getId());
			System.out.println("origin:"+omopTypeConcept.getId()+" loinc:"+loincConceptId);
			try {
				if (loincConceptId != 0L) {
					// We found lonic code for this. Find this concept and create FHIR codeable
					// concept.
					Concept loincConcept = conceptService.findById(loincConceptId);
					typeCodeableConcept = CodeableConceptUtil.getCodeableConceptFromOmopConcept(loincConcept);
				}
			} catch (FHIRException e) {
				e.printStackTrace();
				typeCodeableConcept = null;
			}
		}
		
		if (typeCodeableConcept == null) {
			try {
				typeCodeableConcept = CodeableConceptUtil.getCodeableConceptFromOmopConcept(omopTypeConcept);
			} catch (FHIRException e) {
				e.printStackTrace();
				typeCodeableConcept = null;
			}
		}
		
		// If type CodeableConcept is still null,
		if (typeCodeableConcept == null) return null;

		// Set the type now.
		documentReference.setType(typeCodeableConcept);
		
		// Set Subject
		FPerson fPerson = entity.getFPerson();
		ResourceReferenceDt patientReference = new ResourceReferenceDt(new IdDt(PatientResourceProvider.getType(), IdMapping.getFHIRfromOMOP(fPerson.getId(), PatientResourceProvider.getType())));
		patientReference.setDisplay(fPerson.getNameAsSingleString());
		documentReference.setSubject(patientReference);
		
		// Set created time
		Date createdDate = entity.getDate();
		String createdTime = entity.getTime();
		DateTimeDt createdDateTime = null;
		if (createdDate != null) {
			if (createdTime != null)
//				createdDateTime = DateUtil.constructDateTime(createdDate, createdTime);
				createdDateTime = new DateTimeDt(DateUtil.constructDateTime(createdDate, createdTime));
			else
//				createdDateTime = DateUtil.constructDateTime(createdDate, null);
				createdDateTime = new DateTimeDt(DateUtil.constructDateTime(createdDate, null));
		}
		
		if (createdDateTime != null) {
			documentReference.setCreated(createdDateTime);
//			documentReference.setIndexed(createdDateTime);
			documentReference.setIndexedWithMillisPrecision(createdDate);
		}
		
		// Set author 
		Provider provider = entity.getProvider();
		if (provider != null) {
			ResourceReferenceDt practitionerReference = new ResourceReferenceDt (new IdDt(PractitionerResourceProvider.getType(), IdMapping.getFHIRfromOMOP(provider.getId(), PractitionerResourceProvider.getType())));
			practitionerReference.setDisplay(provider.getProviderName());
//			documentReference.addAuthor(practitionerReference);
			List<ResourceReferenceDt> tempList = documentReference.getAuthor();
			tempList.add(practitionerReference);
			documentReference.setAuthor(tempList);
		}
		
		// Set content now.
		String noteText = entity.getNoteText();
		if (noteText != null && !noteText.isEmpty()) {
			AttachmentDt attachment = new AttachmentDt();
			attachment.setContentType("text/plain");
			attachment.setLanguage("en-US");
			
			// Convert data to base64
			attachment.setData(noteText.getBytes());
			
//			Content documentReferenceContentComponent = new Content(attachment);
			Content documentReferenceContentComponent = new Content();
			documentReferenceContentComponent.setAttachment(attachment);
			documentReference.addContent(documentReferenceContentComponent);
		}
		
		// Set context if visitOccurrence exists.
		VisitOccurrence visitOccurrence = entity.getVisitOccurrence();
		if (visitOccurrence != null) {
			ResourceReferenceDt encounterReference = new ResourceReferenceDt(new IdDt(EncounterResourceProvider.getType(), IdMapping.getFHIRfromOMOP(visitOccurrence.getId(), EncounterResourceProvider.getType())));
			Context documentReferenceContextComponent = new Context();
			documentReferenceContextComponent.setEncounter(encounterReference);
			documentReferenceContextComponent.setSourcePatientInfo(patientReference);
			
			documentReference.setContext(documentReferenceContextComponent);
		}
		
		return documentReference;
	}
}