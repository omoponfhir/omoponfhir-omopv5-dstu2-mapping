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
import java.util.Set;

//import org.hl7.fhir.dstu3.model.CodeableConcept;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
//import org.hl7.fhir.dstu3.model.DeviceUseStatement;
import ca.uhn.fhir.model.dstu2.resource.DeviceUseStatement;
//import org.hl7.fhir.dstu3.model.IdType;
import ca.uhn.fhir.model.primitive.IdDt;
//import org.hl7.fhir.dstu3.model.OperationOutcome;
import ca.uhn.fhir.model.dstu2.resource.OperationOutcome;
//import org.hl7.fhir.dstu3.model.Patient;
import ca.uhn.fhir.model.dstu2.resource.Patient;
//import org.hl7.fhir.dstu3.model.Reference;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
//import org.hl7.fhir.dstu3.model.Resource;
import ca.uhn.fhir.model.dstu2.resource.BaseResource;
//import org.hl7.fhir.dstu3.model.ResourceType;
import ca.uhn.fhir.model.dstu2.valueset.ResourceTypeEnum;
//import org.hl7.fhir.dstu3.model.OperationOutcome.IssueSeverity;
import ca.uhn.fhir.model.dstu2.valueset.IssueSeverityEnum;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.mapping.OmopDeviceUseStatement;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.model.MyDeviceUseStatement;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.utilities.FHIRException;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;

public class DeviceUseStatementResourceProvider implements IResourceProvider {

	private WebApplicationContext myAppCtx;
	private OmopDeviceUseStatement myMapper;
	private String myDbType;
	private int preferredPageSize = 30;

	public DeviceUseStatementResourceProvider() {
		myAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();
		myDbType = myAppCtx.getServletContext().getInitParameter("backendDbType");
		if (myDbType.equalsIgnoreCase("omopv5") == true) {
			myMapper = new OmopDeviceUseStatement(myAppCtx);
		} else {
			myMapper = new OmopDeviceUseStatement(myAppCtx);
		}
		
		String pageSizeStr = myAppCtx.getServletContext().getInitParameter("preferredPageSize");
		if (pageSizeStr != null && pageSizeStr.isEmpty() == false) {
			int pageSize = Integer.parseInt(pageSizeStr);
			if (pageSize > 0) {
				preferredPageSize = pageSize;
			} 
		}
	}
	
	public static String getType() {
		return "DeviceUseStatement";
	}
	
	public OmopDeviceUseStatement getMyMapper() {
    	return myMapper;
    }
	
	private Integer getTotalSize(List<ParameterWrapper> paramList) {
		final Long totalSize;
		if (paramList.size() == 0) {
			totalSize = getMyMapper().getSize();
		} else {
			totalSize = getMyMapper().getSize(paramList);
		}
		
		return totalSize.intValue();
	}
	
	/***
	 * 
	 * @param theDeviceUseStatement
	 * @return
	 * 
	 * This creates a DeviceExposure entry. Since the table servers
	 * Devce and DeviceUseStatement, Create request should embed 
	 * Device FHIR information in the DeviceUseStatement resource.
	 */
	@Create()
	public MethodOutcome createDeviceUseStatement(@ResourceParam MyDeviceUseStatement theDeviceUseStatement) {
		validateResource(theDeviceUseStatement);
		
		// We need to check if this resource has device resource embedded.
		List<IResource> containeds = theDeviceUseStatement.getContained().getContainedResources();
		boolean deviceFound = false;
		for (IResource contained: containeds) {
			String resourceType = contained.getResourceName();
			if (resourceType == ResourceTypeEnum.DEVICE.toString()) {
				deviceFound = true;
				break;
			}
		}
		
		if (deviceFound == false) {
			errorProcessing("Device must be contained in the resource in order to create resource");
		}
		
		Long id=null;
		try {
			id = getMyMapper().toDbase(theDeviceUseStatement, null);
		} catch (FHIRException e) {
			e.printStackTrace();
		}		
		return new MethodOutcome(new IdDt(id));
	}
	
	@Delete()
	public void deleteDeviceUseStatement(@IdParam IdDt theId) {
		if (getMyMapper().removeByFhirId(theId) <= 0) {
			throw new ResourceNotFoundException(theId);
		}
	}

	@Search()
	public IBundleProvider findDeviceUseStatementsByParams(
			@OptionalParam(name=DeviceUseStatement.SP_RES_ID) TokenParam theDeviceUseStatementId,
			@OptionalParam(name=DeviceUseStatement.SP_PATIENT, chainWhitelist={"", Patient.SP_NAME}) ReferenceParam thePatient,
			@OptionalParam(name=DeviceUseStatement.SP_SUBJECT, chainWhitelist={"", Patient.SP_NAME}) ReferenceParam theSubject,
			
			@IncludeParam(allow={"DeviceUseStatement:device"})
			final Set<Include> theIncludes

			) {
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper> ();

		if (theDeviceUseStatementId != null) {
			paramList.addAll(getMyMapper().mapParameter (DeviceUseStatement.SP_RES_ID, theDeviceUseStatementId, false));
		}

		// With OMOP, we only support subject to be patient.
		// If the subject has only ID part, we assume that is patient.
		if (theSubject != null) {
			if (theSubject.getResourceType() != null && 
					theSubject.getResourceType().equals(PatientResourceProvider.getType())) {
				thePatient = theSubject;
			} else {
				// If resource is null, we assume Patient.
				if (theSubject.getResourceType() == null) {
					thePatient = theSubject;
				} else {
					errorProcessing("subject search allows Only Patient Resource, but provided "+theSubject.getResourceType());
				}
			}
		}

		if (thePatient != null) {
			String patientChain = thePatient.getChain();
			if (patientChain != null) {
				if (Patient.SP_NAME.equals(patientChain)) {
					String thePatientName = thePatient.getValue();
					paramList.addAll(getMyMapper().mapParameter ("Patient:"+Patient.SP_NAME, thePatientName, false));
				} else if ("".equals(patientChain)) {
					paramList.addAll(getMyMapper().mapParameter ("Patient:"+Patient.SP_RES_ID, thePatient.getValue(), false));
				}
			} else {
				paramList.addAll(getMyMapper().mapParameter ("Patient:"+Patient.SP_RES_ID, thePatient.getIdPart(), false));
			}
		}

		MyBundleProvider myBundleProvider = new MyBundleProvider(paramList, theIncludes);
		myBundleProvider.setTotalSize(getTotalSize(paramList));
		myBundleProvider.setPreferredPageSize(preferredPageSize);
		
		return myBundleProvider;
	}
	
	@Read()
	public MyDeviceUseStatement readPatient(@IdParam IdDt theId) {
		MyDeviceUseStatement retval = (MyDeviceUseStatement) getMyMapper().toFHIR(theId);
		if (retval == null) {
			throw new ResourceNotFoundException(theId);
		}
			
		return retval;
	}
	
	@Update()
	public MethodOutcome updateDeviceUseStatement(@IdParam IdDt theId, @ResourceParam MyDeviceUseStatement theDeviceUseStatement) {
		validateResource(theDeviceUseStatement);

		Long fhirId = null;
		try {
			fhirId = getMyMapper().toDbase(theDeviceUseStatement, theId);
		} catch (FHIRException e) {
			e.printStackTrace();
		}
		if (fhirId == null) {
			throw new ResourceNotFoundException(theId);
		}
		
		return new MethodOutcome();
	}

	// TODO: Add more validation code here.
	private void validateResource(DeviceUseStatement theDeviceUseStatement) {
	}

	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return DeviceUseStatement.class;
	}
	
	private void errorProcessing(String msg) {
		OperationOutcome outcome = new OperationOutcome();
		CodeableConceptDt detailCode = new CodeableConceptDt();
		detailCode.setText(msg);
		outcome.addIssue().setSeverity(IssueSeverityEnum.FATAL).setDetails(detailCode);
		throw new UnprocessableEntityException(FhirContext.forDstu2(), outcome);
	}

	class MyBundleProvider extends OmopFhirBundleProvider implements IBundleProvider {
		Set<Include> theIncludes;

		public MyBundleProvider(List<ParameterWrapper> paramList, Set<Include> theIncludes) {
			super(paramList);
			setPreferredPageSize (preferredPageSize);
			this.theIncludes = theIncludes;
		}

		@Override
		public List<IBaseResource> getResources(int theFromIndex, int theToIndex) {
			List<IBaseResource> retv = new ArrayList<IBaseResource>();
			
			// _Include
			List<String> includes = new ArrayList<String>();
			if (theIncludes.contains(new Include("DeviceUseStatement:device"))) {
				includes.add("DeviceUseStatement:device");
			}

			if (paramList.size() == 0) {
				getMyMapper().searchWithoutParams(theFromIndex, theToIndex, retv, includes, null);
			} else {
				getMyMapper().searchWithParams(theFromIndex, theToIndex, paramList, retv, includes, null);
			}

			return retv;
		}

	}

}
