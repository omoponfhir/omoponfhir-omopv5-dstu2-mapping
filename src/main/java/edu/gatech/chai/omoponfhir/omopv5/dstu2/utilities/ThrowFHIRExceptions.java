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

//import org.hl7.fhir.dstu3.model.CodeableConcept;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
//import org.hl7.fhir.dstu3.model.OperationOutcome;
import ca.uhn.fhir.model.dstu2.resource.OperationOutcome;
//import org.hl7.fhir.dstu3.model.OperationOutcome.IssueSeverity;
import ca.uhn.fhir.model.dstu2.valueset.IssueSeverityEnum;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class ThrowFHIRExceptions {

	public static UnprocessableEntityException unprocessableEntityException(String message) {
		OperationOutcome outcome = new OperationOutcome();
		CodeableConceptDt detailCode = new CodeableConceptDt();
		detailCode.setText(message);
		outcome.addIssue().setSeverity(IssueSeverityEnum.FATAL).setDetails(detailCode);
		throw new UnprocessableEntityException(FhirContext.forDstu3(), outcome);
	}
	
	public static InternalErrorException internalErrorException(String message) {
		throw new InternalErrorException(message);
	}
}
