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
package edu.gatech.chai.omoponfhir.omopv5.dstu2.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//import org.hl7.fhir.dstu3.model.BooleanType;
import ca.uhn.fhir.model.primitive.BooleanDt;
//import org.hl7.fhir.dstu3.model.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
//import org.hl7.fhir.dstu3.model.CodeableConcept;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
//import org.hl7.fhir.dstu3.model.Coding;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
//import org.hl7.fhir.dstu3.model.Enumerations.MessageEvent;
import ca.uhn.fhir.model.dstu2.valueset.MessageEventEnum;
//import org.hl7.fhir.dstu3.model.MessageHeader;
import ca.uhn.fhir.model.dstu2.resource.MessageHeader;
//import org.hl7.fhir.dstu3.model.OperationOutcome;
import ca.uhn.fhir.model.dstu2.resource.OperationOutcome;
//import org.hl7.fhir.dstu3.model.MessageHeader.MessageHeaderResponseComponent;
import ca.uhn.fhir.model.dstu2.resource.MessageHeader.Response;
//import org.hl7.fhir.dstu3.model.MessageHeader.ResponseType;
import ca.uhn.fhir.model.dstu2.valueset.ResponseTypeEnum;
//import org.hl7.fhir.dstu3.model.OperationOutcome.IssueSeverity;
import ca.uhn.fhir.model.dstu2.valueset.IssueSeverityEnum;
//import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import ca.uhn.fhir.model.dstu2.resource.Bundle.Entry;
//import org.hl7.fhir.dstu3.model.Bundle.BundleType;
import ca.uhn.fhir.model.dstu2.valueset.BundleTypeEnum;
//import org.hl7.fhir.dstu3.model.Resource;
import ca.uhn.fhir.model.dstu2.resource.BaseResource;
//import org.hl7.fhir.dstu3.model.ResourceType;
import ca.uhn.fhir.model.dstu2.valueset.ResourceTypeEnum;
//import org.hl7.fhir.dstu3.model.UriType;
import ca.uhn.fhir.model.primitive.UriDt;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.mapping.OmopServerOperations;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.utilities.CodeableConceptUtil;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.utilities.FHIRException;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.utilities.ThrowFHIRExceptions;

public class ServerOperations {
	private OmopServerOperations myMapper;
	
	public ServerOperations() {
		myMapper = new OmopServerOperations();
	}
	
	@Operation(name="$process-message")
	public Bundle processMessageOperation(
			@OperationParam(name="content") Bundle theContent,
			@OperationParam(name="async") BooleanDt theAsync,
			@OperationParam(name="response-url") UriDt theUri
			) {
		Bundle retVal = new Bundle();
		MessageHeader messageHeader = null;
		List<BaseResource> resources = new ArrayList<BaseResource>();
		
		if (theContent.getType() == BundleTypeEnum.MESSAGE.toString()) {
			List<Entry> entries = theContent.getEntry();
			// Evaluate the first entry, which must be MessageHeader
//			BundleEntryComponent entry1 = theContent.getEntryFirstRep();
//			Resource resource = entry1.getResource();
			if (entries != null && entries.size() > 0 && 
					entries.get(0).getResource() != null &&
//					entries.get(0).getResource().getResourceType() == ResourceTypeEnum.MESSAGEHEADER) {
					entries.get(0).getResource().getResourceName() == ResourceTypeEnum.MESSAGEHEADER.toString()) {
				messageHeader = (MessageHeader) entries.get(0).getResource();
				// We handle observation-type.
				// TODO: Add other types later.
				CodingDt event = messageHeader.getEvent();
				CodingDt obsprovided = new CodingDt(MessageEventEnum.OBSERVATION_PROVIDE.getSystem(), MessageEventEnum.OBSERVATION_PROVIDE.getCode());
				obsprovided.setDisplay("Provide a simple observation or update a previously provided simple observation.");
				if (CodeableConceptUtil.compareCodings(event, obsprovided) == 0) {
					// This is lab report. they are all to be added to the server.
					for (int i=1; i<entries.size(); i++) {
						resources.add((BaseResource) entries.get(i).getResource());
					}
				} else {
					ThrowFHIRExceptions.unprocessableEntityException(
							"We currently support only observation-provided Message event");
				}
			}
		} else {
			ThrowFHIRExceptions.unprocessableEntityException(
					"The bundle must be a MESSAGE type");
		}
		Response messageHeaderResponse = new Response();
		messageHeaderResponse.setId(messageHeader.getId());

		List<Entry> resultEntries = null;
		try {
			resultEntries = myMapper.createEntries(resources);
			messageHeaderResponse.setCode(ResponseTypeEnum.OK);
		} catch (FHIRException e) {
			e.printStackTrace();
			messageHeaderResponse.setCode(ResponseTypeEnum.OK);
			OperationOutcome outcome = new OperationOutcome();
			CodeableConceptDt detailCode = new CodeableConceptDt();
			detailCode.setText(e.getMessage());
			outcome.addIssue().setSeverity(IssueSeverityEnum.ERROR).setDetails(detailCode);
//			messageHeaderResponse.setDetailsTarget(outcome);
//	We don't have this in DSTU2
		}
		
		messageHeader.setResponse(messageHeaderResponse);
		Entry responseMessageEntry = new Entry();
		UUID uuid = UUID.randomUUID();
		responseMessageEntry.setFullUrl("urn:uuid:"+uuid.toString());
		responseMessageEntry.setResource(messageHeader);
		
		if (resultEntries == null) resultEntries = new ArrayList<Entry>();
		
		resultEntries.add(0, responseMessageEntry);
		retVal.setEntry(resultEntries);
		
		return retVal;
	}
}
