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
import java.util.List;

//import org.hl7.fhir.dstu3.model.CodeableConcept;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
//import org.hl7.fhir.dstu3.model.Coding;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
//import org.hl7.fhir.dstu3.model.IdType;
import ca.uhn.fhir.model.primitive.IdDt;
//import org.hl7.fhir.dstu3.model.Identifier;
import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;
//import org.hl7.fhir.dstu3.model.Organization;
import ca.uhn.fhir.model.dstu2.resource.Organization;
//import org.hl7.fhir.dstu3.model.Reference;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;

import org.hl7.fhir.instance.model.api.IIdType;
//import org.hl7.fhir.dstu3.model.Address;
import ca.uhn.fhir.model.dstu2.composite.AddressDt;
//import org.hl7.fhir.dstu3.model.Address.AddressUse;
import ca.uhn.fhir.model.dstu2.valueset.AddressUseEnum;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.provider.OrganizationResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.utilities.AddressUtil;
import edu.gatech.chai.omoponfhir.omopv5.dstu2.utilities.FHIRException;
import edu.gatech.chai.omopv5.dba.service.CareSiteService;
import edu.gatech.chai.omopv5.dba.service.LocationService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.dba.service.VocabularyService;
import edu.gatech.chai.omopv5.model.entity.CareSite;
import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.model.entity.Location;

public class OmopOrganization extends BaseOmopResource<Organization, CareSite, CareSiteService> implements IResourceMapping<Organization, CareSite> {
	
	private static OmopOrganization omopOrganization = new OmopOrganization();
	private LocationService locationService;
	private VocabularyService vocabularyService;

	public OmopOrganization(WebApplicationContext context) {
		super(context, CareSite.class, CareSiteService.class, OrganizationResourceProvider.getType());
		
	}

	public OmopOrganization() {
		super(ContextLoaderListener.getCurrentWebApplicationContext(), CareSite.class, CareSiteService.class, OrganizationResourceProvider.getType());
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}

	private void initialize(WebApplicationContext context) {		
		// Get bean for other service(s) for mapping.
		locationService = context.getBean(LocationService.class);
		vocabularyService = context.getBean(VocabularyService.class);
		
		getSize();
	}
	
	public static OmopOrganization getInstance() {
		return omopOrganization;
	}
	
	@Override
	public Organization constructFHIR(Long fhirId, CareSite careSite) {
		Organization organization = new Organization();

		organization.setId(new IdDt(fhirId));

		if (careSite.getCareSiteName() != null && careSite.getCareSiteName() != "") {
			organization.setName(careSite.getCareSiteName());
		}

		if (careSite.getPlaceOfServiceConcept() != null) {
			String codeString = careSite.getPlaceOfServiceConcept().getConceptCode();
//			String systemUriString = careSite.getPlaceOfServiceConcept().getVocabulary().getVocabularyReference();
			String systemUriString = vocabularyService.findById(careSite.getPlaceOfServiceConcept().getVocabularyId()).getVocabularyReference();
			String displayString = careSite.getPlaceOfServiceConcept().getConceptName();
			CodingDt tempcoding = new CodingDt(systemUriString,codeString);
			tempcoding.setDisplay(displayString);
			CodeableConceptDt typeCodeableConcept = new CodeableConceptDt()
					.addCoding(tempcoding);
			organization.setType(typeCodeableConcept);
//			organization.addType(typeCodeableConcept);
		}

		if (careSite.getLocation() != null) {
			// WARNING check if mapping for lines are correct
			organization.addAddress().setUse(AddressUseEnum.HOME).addLine(careSite.getLocation().getAddress1())
					.addLine(careSite.getLocation().getAddress2())
					.setCity(careSite.getLocation().getCity()).setPostalCode(careSite.getLocation().getZipCode())
					.setState(careSite.getLocation().getState());
			// .setPeriod(period);
		}

		return organization;
	}

	@Override
	public Long toDbase(Organization organization, IdDt fhirId) throws FHIRException {
		// If fhirId is null, then it's CREATE.
		// If fhirId is not null, then it's UPDATE.

		Long omopId = null;
		if (fhirId != null) {
			omopId = IdMapping.getOMOPfromFHIR(fhirId.getIdPartAsLong(), OrganizationResourceProvider.getType());
		} else {
			// See if we have this already. If so, we throw error.
			// Get the identifier to store the source information.
			// If we found a matching one, replace this with the careSite.
			List<IdentifierDt> identifiers = organization.getIdentifier();
			CareSite existingCareSite = null;
			String careSiteSourceValue = null;
			for (IdentifierDt identifier: identifiers) {
				if (identifier.getValue().isEmpty() == false) {
					careSiteSourceValue = identifier.getValue();
					
					existingCareSite = getMyOmopService().searchByColumnString("careSiteSourceValue", careSiteSourceValue).get(0);
					if (existingCareSite != null) {
						omopId = existingCareSite.getId();
						break;
					}
				}
			}
		}

		CareSite careSite = constructOmop(omopId, organization);
		
		Long omopRecordId = null;
		if (careSite.getId() != null) {
			omopRecordId = getMyOmopService().update(careSite).getId();	
		} else {
			omopRecordId = getMyOmopService().create(careSite).getId();
		}
		
		Long fhirRecordId = IdMapping.getFHIRfromOMOP(omopRecordId, OrganizationResourceProvider.getType());
		return fhirRecordId;
	}

//	@Override
//	public Long getSize() {
//		return myOmopService.getSize(CareSite.class);
//	}
//	
//	public Long getSize(Map<String, List<ParameterWrapper>> map) {
//		return myOmopService.getSize(CareSite.class, map);
//	}
	

	@Override
	public Organization constructResource(Long fhirId, CareSite entity, List<String> includes) {
		Organization myOrganization = constructFHIR(fhirId, entity);
		
		if (!includes.isEmpty()) {
			if (includes.contains("Organization:partof")) {
				ResourceReferenceDt partOfOrganization = myOrganization.getPartOf();
				if (partOfOrganization != null && partOfOrganization.isEmpty() == false) {
					IIdType partOfOrgId = partOfOrganization.getReferenceElement();
					Long partOfOrgFhirId = partOfOrgId.getIdPartAsLong();
					Long omopId = IdMapping.getOMOPfromFHIR(partOfOrgFhirId, OrganizationResourceProvider.getType());
					CareSite partOfCareSite = getMyOmopService().findById(omopId);
					Organization partOfOrgResource = constructFHIR(partOfOrgFhirId, partOfCareSite);
					
					partOfOrganization.setResource(partOfOrgResource);
				}
			}
		}

		return myOrganization;
	}

	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
		ParameterWrapper paramWrapper = new ParameterWrapper();
        if (or) paramWrapper.setUpperRelationship("or");
        else paramWrapper.setUpperRelationship("and");

		switch (parameter) {
		case Organization.SP_RES_ID:
			String orgnizationId = ((TokenParam) value).getValue();
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(orgnizationId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Organization.SP_NAME:
			// This is family name, which is string. use like.
			String familyString = ((StringParam) value).getValue();
			paramWrapper.setParameterType("String");
			paramWrapper.setParameters(Arrays.asList("careSiteName"));
			paramWrapper.setOperators(Arrays.asList("like"));
			paramWrapper.setValues(Arrays.asList(familyString));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		default:
			mapList = null;
		}

		return mapList;
	}

	@Override
	public CareSite constructOmop(Long omopId, Organization myOrganization) {
		String careSiteSourceValue = null;
		Location location = null;
		
		CareSite careSite = null;
		if (omopId != null) {
			careSite  = getMyOmopService().findById(omopId);
			if (careSite == null) {
				try {
					throw new FHIRException(myOrganization.getId() + " does not exist");
				} catch (FHIRException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			location = careSite.getLocation();
		} else {
			careSite = new CareSite();
		}

		IdentifierDt identifier = myOrganization.getIdentifierFirstRep();
		if (!identifier.getValue().isEmpty()) {
			careSiteSourceValue = identifier.getValue();
			careSite.setCareSiteSourceValue(careSiteSourceValue);
		}
		
		Location existingLocation = AddressUtil.searchAndUpdate(locationService, myOrganization.getAddressFirstRep(), location);
		if (existingLocation != null) {
			careSite.setLocation(existingLocation);
		}

		// Organization.name to CareSiteName
		careSite.setCareSiteName(myOrganization.getName());

		// Organzation.type to Place of Service Concept
//		in DSTU2, this only returns 1, not a list
//		List<CodeableConceptDt> orgTypes = myOrganization.getType();
//		for (CodeableConceptDt orgType: orgTypes) {
//			List<CodingDt> typeCodings = orgType.getCoding();
//			if (typeCodings.size() > 0) {
//				String typeCode = typeCodings.get(0).getCode();
//				Long placeOfServiceId;
//				try {
//					placeOfServiceId = OmopConceptMapping.omopForOrganizationTypeCode(typeCode);
//					Concept placeOfServiceConcept = new Concept();
//					placeOfServiceConcept.setId(placeOfServiceId);
//					careSite.setPlaceOfServiceConcept(placeOfServiceConcept);
//				} catch (FHIRException e) {
//					e.printStackTrace();
//				}
//			}
//		}
		CodeableConceptDt orgType = myOrganization.getType();
		List<CodingDt> typeCodings = orgType.getCoding();
		if (typeCodings.size() > 0) {
			String typeCode = typeCodings.get(0).getCode();
			Long placeOfServiceId;
			try {
				placeOfServiceId = OmopConceptMapping.omopForOrganizationTypeCode(typeCode);
				Concept placeOfServiceConcept = new Concept();
				placeOfServiceConcept.setId(placeOfServiceId);
				careSite.setPlaceOfServiceConcept(placeOfServiceConcept);
			} catch (FHIRException e) {
				e.printStackTrace();
			}
		}

		// Address to Location ID
		List<AddressDt> addresses = myOrganization.getAddress();
		for (AddressDt address: addresses) {
			// We can only store one address.
			Location retLocation = AddressUtil.searchAndUpdate(locationService, address, careSite.getLocation());
			if (retLocation != null) {
				careSite.setLocation(retLocation);
				break;
			}
		}

		return careSite;
	}
}
