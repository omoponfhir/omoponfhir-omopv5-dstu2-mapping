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
package edu.gatech.chai.omoponfhir.omopv5.dstu2.utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import org.hl7.fhir.dstu3.model.CodeableConcept;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
//import org.hl7.fhir.dstu3.model.Coding;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.mapping.BaseOmopResource;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.mapping.OmopCodeableConceptMapping;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.utilities.FHIRException;
import edu.gatech.chai.omoponfhir.local.dao.FhirOmopVocabularyMapImpl;
import edu.gatech.chai.omopv5.dba.service.ConceptService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.model.entity.Concept;

public class CodeableConceptUtil {
	public static void addCodingFromOmopConcept(CodeableConceptDt codeableConcept, Concept concept) throws FHIRException {
		String fhirUri = OmopCodeableConceptMapping.fhirUriforOmopVocabulary(concept.getVocabularyId());

		CodingDt coding = new CodingDt();
		coding.setSystem(fhirUri);
		coding.setCode(concept.getConceptCode());
		coding.setDisplay(concept.getConceptName());
		
		codeableConcept.addCoding(coding);
	}
	
	public static CodingDt getCodingFromOmopConcept(Concept concept, FhirOmopVocabularyMapImpl fhirOmopVocabularyMap) throws FHIRException {
		String fhirUri = fhirOmopVocabularyMap.getFhirSystemNameFromOmopVocabulary(concept.getVocabularyId());

		CodingDt coding = new CodingDt();
		coding.setSystem(fhirUri);
		coding.setCode(concept.getConceptCode());
		coding.setDisplay(concept.getConceptName());

		return coding;
	}
	
	public static CodeableConceptDt getCodeableConceptFromOmopConcept(Concept concept, FhirOmopVocabularyMapImpl fhirOmopVocabularyMap) throws FHIRException {
		CodeableConceptDt codeableConcept = new CodeableConceptDt();
		CodingDt coding = getCodingFromOmopConcept(concept, fhirOmopVocabularyMap);
		codeableConcept.addCoding(coding);

		return codeableConcept;
	}

	public static CodeableConceptDt getCodeableConceptFromOmopConcept(Concept concept) throws FHIRException {
		CodeableConceptDt codeableConcept = new CodeableConceptDt();
		addCodingFromOmopConcept (codeableConcept, concept);		
		return codeableConcept;
	}
	
	public static Concept getOmopConceptWithOmopVacabIdAndCode(ConceptService conceptService, String omopVocabularyId, String code) {
		if (omopVocabularyId == null) return null;
		
		ParameterWrapper param = new ParameterWrapper(
				"String",
				Arrays.asList("vocabulary", "conceptCode"),
				Arrays.asList("=", "="),
				Arrays.asList(omopVocabularyId, code),
				"and"
				);
		
		List<ParameterWrapper> params = new ArrayList<ParameterWrapper>();
		params.add(param);

		List<Concept> conceptIds = conceptService.searchWithParams(0, 0, params, null);
		if (conceptIds.isEmpty()) {
			return null;
		}
		
		// We should have only one entry... so... 
		return conceptIds.get(0);
	}
	
	public static Concept getOmopConceptWithFhirConcept(ConceptService conceptService, CodingDt fhirCoding) throws FHIRException {
		String system = fhirCoding.getSystem();
		String code = fhirCoding.getCode();
		
		String omopVocabularyId = OmopCodeableConceptMapping.omopVocabularyforFhirUri(system);
		return getOmopConceptWithOmopVacabIdAndCode(conceptService, omopVocabularyId, code);
	}
	
	public static Concept searchConcept(ConceptService conceptService, CodeableConceptDt codeableConcept) throws FHIRException {
		List<CodingDt> codings = codeableConcept.getCoding();
		for (CodingDt coding : codings) {
			// get OMOP Vocabulary from mapping.
			Concept ret = getOmopConceptWithFhirConcept(conceptService, coding);
			if (ret != null) return ret;
		}
		return null;
	}

	/**
	 * Creates a {@link CodeableConceptDt} from a {@link Concept}
	 * @param concept the {@link Concept} to use to generate the {@link CodeableConceptDt}
	 * @return a {@link CodeableConceptDt} generated from the passed in {@link Concept}
	 * @throws FHIRException if the {@link Concept} vocabulary cannot be mapped by the {@link OmopCodeableConceptMapping} fhirUriforOmopVocabularyi method.
     */
	public static CodeableConceptDt createFromConcept(Concept concept) throws FHIRException{
		String conceptVocab = concept.getVocabularyId();
		String conceptFhirUri = OmopCodeableConceptMapping.fhirUriforOmopVocabulary(conceptVocab);
		String conceptCode = concept.getConceptCode();
		String conceptName = concept.getConceptName();

		CodingDt conceptCoding = new CodingDt();
		conceptCoding.setSystem(conceptFhirUri);
		conceptCoding.setCode(conceptCode);
		conceptCoding.setDisplay(conceptName);

		CodeableConceptDt codeableConcept = new CodeableConceptDt();
		codeableConcept.addCoding(conceptCoding);
		return codeableConcept;
	}
	
	/**
	 * 
	 * @param coding1
	 * @param coding2
	 * @return 
	 *   1 if only code matches,
	 *   0 if both system and code match,
	 *   -1 if none matches.
	 */
	public static int compareCodings(CodingDt coding1, CodingDt coding2) {
		boolean isSystemMatch = false;
		boolean isCodeMatch = false;
//      below is the refrence implementation for hasSystem and hasCode from dstu3
//		public boolean hasSystem() {return this.system != null && !this.system.isEmpty();}
//		public boolean hasCode() {return this.code != null && !this.code.isEmpty();}

		if ((coding1.getSystem() != null && !coding1.getSystem().isEmpty()) && (coding2.getSystem() != null && !coding2.getSystem().isEmpty())){
//		if (coding1.hasSystem() && coding1.hasSystem()) {
			if (coding1.getSystem().equals(coding2.getSystem())) {
				isSystemMatch = true;
			}
		}
		if ((coding1.getCode() != null && !coding1.getCode().isEmpty()) && (coding2.getCode() != null && !coding2.getCode().isEmpty())){
//		if (coding1.hasCode() && coding2.hasCode()) {
			if (coding1.getCode().equals(coding2.getCode())) {
				isCodeMatch = true;
			}
		}
		
		if (isSystemMatch && isCodeMatch) return 0;
		if (isCodeMatch) return 1;
		return -1;
	}

}
