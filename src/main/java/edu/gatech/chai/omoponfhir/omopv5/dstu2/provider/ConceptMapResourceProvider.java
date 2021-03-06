package edu.gatech.chai.omoponfhir.omopv5.dstu2.provider;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Base64;
import java.util.List;

//import org.hl7.fhir.dstu3.model.BooleanType;
import ca.uhn.fhir.model.primitive.BooleanDt;
//import org.hl7.fhir.dstu3.model.CodeType;
import ca.uhn.fhir.model.primitive.CodeDt;
import ca.uhn.fhir.model.primitive.IdDt;
//import org.hl7.fhir.dstu3.model.CodeableConcept;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
//import org.hl7.fhir.dstu3.model.Coding;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
//import org.hl7.fhir.dstu3.model.ConceptMap;
import ca.uhn.fhir.model.dstu2.resource.ConceptMap;
//import org.hl7.fhir.dstu3.model.Parameters;
import ca.uhn.fhir.model.dstu2.resource.Parameters;
//import org.hl7.fhir.dstu3.model.StringType;
import ca.uhn.fhir.model.primitive.StringDt;
//import org.hl7.fhir.dstu3.model.UriType;
import ca.uhn.fhir.model.primitive.UriDt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.IResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.mapping.OmopConceptMap;

public class ConceptMapResourceProvider implements IResourceProvider {
	private static final Logger logger = LoggerFactory.getLogger(ConceptMapResourceProvider.class);

	private WebApplicationContext myAppCtx;
	private String myDbType;
	private OmopConceptMap myMapper;
	private int preferredPageSize = 30;
	private FhirContext fhirContext;

	public ConceptMapResourceProvider() {
		myAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();
		myDbType = myAppCtx.getServletContext().getInitParameter("backendDbType");
		if (myDbType.equalsIgnoreCase("omopv5") == true) {
			myMapper = new OmopConceptMap(myAppCtx);
		} else {
			myMapper = new OmopConceptMap(myAppCtx);
		}

		String pageSizeStr = myAppCtx.getServletContext().getInitParameter("preferredPageSize");
		if (pageSizeStr != null && pageSizeStr.isEmpty() == false) {
			int pageSize = Integer.parseInt(pageSizeStr);
			if (pageSize > 0) {
				preferredPageSize = pageSize;
			}
		}
		
	}

	@Override
	public Class<ConceptMap> getResourceType() {
		return ConceptMap.class;
	}

	public static String getType() {
		return "ConceptMap";
	}

	public void setFhirContext(FhirContext fhirContext) {
		this.fhirContext = fhirContext;
	}

	@Read()
	public ConceptMap readConceptMap(@IdParam IdDt theId) {
		return null;
	}

	/**
	 * $translate operation for concept translation.
	 * 
	 */
	@Operation(name = "$translate", idempotent = true)
	public Parameters translateOperation(RequestDetails theRequestDetails,
			@OperationParam(name = "code") CodeDt theCode, @OperationParam(name = "system") UriDt theSystem,
			@OperationParam(name = "version") StringDt theVersion, @OperationParam(name = "source") UriDt theSource,
			@OperationParam(name = "coding") CodingDt theCoding,
			@OperationParam(name = "codeableConcept") CodeableConceptDt theCodeableConcept,
			@OperationParam(name = "target") UriDt theTarget,
			@OperationParam(name = "targetsystem") UriDt theTargetSystem,
			@OperationParam(name = "reverse") BooleanDt theReverse) {

		Parameters retVal = new Parameters();

		String mappingTerminologyUrl = System.getenv("MAPPING_TERMINOLOGY_URL");
		if (mappingTerminologyUrl != null && !mappingTerminologyUrl.isEmpty()) {
			String mappingRequestUrl = theRequestDetails.getCompleteUrl();
			if (mappingRequestUrl != null && !mappingRequestUrl.isEmpty() && !"none".equalsIgnoreCase(mappingRequestUrl)) {
				logger.debug("$translate: RequestDetails - " + theRequestDetails.getCompleteUrl());

				if (!mappingTerminologyUrl.endsWith("/")) {
					mappingTerminologyUrl = mappingTerminologyUrl.concat("/");
				}

				int urlTranslateIndex = mappingRequestUrl.indexOf("$translate");
				if (urlTranslateIndex >= 0) {
					String remoteMappingTerminologyUrlEncoded = mappingTerminologyUrl
							+ mappingRequestUrl.substring(urlTranslateIndex);
					
					String remoteMappingTerminologyUrl = remoteMappingTerminologyUrlEncoded;
					try {
						remoteMappingTerminologyUrl = URLDecoder.decode(remoteMappingTerminologyUrlEncoded, "UTF-8");
						RestTemplate restTemplate = new RestTemplate();

						String authTypeEnv = System.getenv("AUTH_TYPE");
						ResponseEntity<String> response;
						HttpEntity<String> entity = null;
						if (authTypeEnv != null && !authTypeEnv.isEmpty() && !"none".equalsIgnoreCase(authTypeEnv)) {
							String prefix = authTypeEnv.substring(0, 6);
							if ("basic ".equalsIgnoreCase(prefix)) {
								String rawString = authTypeEnv.substring(6);
								String base64encoded = Base64.getEncoder().encodeToString(rawString.getBytes());

								HttpHeaders headers = new HttpHeaders();
								headers.set("Authorization", "Basic " + base64encoded);
								entity = new HttpEntity<String>(headers);
							}
						}
						
						if (entity == null) {
							response = restTemplate.getForEntity(remoteMappingTerminologyUrl, String.class);
						} else {
							response = restTemplate.exchange(remoteMappingTerminologyUrl, HttpMethod.GET, entity, String.class);
						}
						
						if (response.getStatusCode().equals(HttpStatus.OK)) {
							String result = response.getBody();
							IParser fhirJsonParser = fhirContext.newJsonParser();
							Parameters parameters = fhirJsonParser.parseResource(Parameters.class, result);
							if (parameters != null && !parameters.isEmpty()) {
								logger.debug("$translate: responding parameters from external server, " + remoteMappingTerminologyUrl);
								return parameters;
							} else {
								logger.debug("$translate: empty parameter received from " + remoteMappingTerminologyUrl + ". Trying local");
							}
						}
					} catch (RestClientException | UnsupportedEncodingException e) {
						// We have an error.
						logger.error("$translate: Error on connecting to Remote ConceptMap server at "
								+ remoteMappingTerminologyUrl);

						// We may want not to return empty as we can try our internal server...
					}
				}
			}
		}

		String targetUri;
		String targetSystem;
		if (theTarget == null || theTarget.isEmpty()) {
			targetUri = "";
		} else {
			targetUri = theTarget.getValueAsString();
		}
		if (theTargetSystem == null || theTargetSystem.isEmpty()) {
			targetSystem = "";
		} else {
			targetSystem = theTargetSystem.getValueAsString();
		}

		if (theCodeableConcept != null && !theCodeableConcept.isEmpty()) {
			// If codeableconcept exists, use this.
			// If we have multiple codings, run them until we have a matching
			// translation.
			List<CodingDt> codings = theCodeableConcept.getCoding();
			for (CodingDt coding : codings) {
				String code = coding.getCode();
				String system = coding.getSystem();

				retVal = myMapper.translateConcept(code, system, targetUri, targetSystem);
				if (retVal != null && !retVal.isEmpty()) {
					return retVal;
				}
			}
		}

		if (theCoding != null && !theCoding.isEmpty()) {
			// if coding is provided, use this.
			String code = theCoding.getCode();
			String system = theCoding.getSystem();

			retVal = myMapper.translateConcept(code, system, targetUri, targetSystem);
			if (retVal != null && !retVal.isEmpty()) {
				return retVal;
			}
		}

		if (theCode != null && !theCode.isEmpty() && theSystem != null && !theSystem.isEmpty()) {
			String code = theCode.getValueAsString();
			String system = theSystem.getValueAsString();
			retVal = myMapper.translateConcept(code, system, targetUri, targetSystem);
			return retVal;
		}

		return retVal;
	}
}
