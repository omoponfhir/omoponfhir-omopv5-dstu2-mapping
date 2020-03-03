`import org.hl7.fhir.dstu3.model.IdType; becomes import ca.uhn.fhir.model.primitive.IdDt;`
`import org.hl7.fhir.dstu3.model.Patient; becomes import ca.uhn.fhir.model.dstu2.resource.Patient;`
`import org.hl7.fhir.dstu3.model.ConceptMap; becomes import ca.uhn.fhir.model.dstu2.resource.ConceptMap;`
`import org.hl7.fhir.dstu3.model.BooleanType; becomes import ca.uhn.fhir.model.primitive.BooleanDt;`
`import org.hl7.fhir.dstu3.model.Parameters; becomes import ca.uhn.fhir.model.dstu2.resource.Parameters;`
`import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent; becomes import ca.uhn.fhir.model.dstu2.resource.Parameters.Parameter;`
`import org.hl7.fhir.dstu3.model.CodeType; becomes import ca.uhn.fhir.model.primitive.CodeDt;`
`import org.hl7.fhir.dstu3.model.Coding; becomes import ca.uhn.fhir.model.dstu2.composite.CodingDt;`
`import org.hl7.fhir.dstu3.model.CodeableConcept; becomes import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;`
`import org.hl7.fhir.exceptions.FHIRException; becomes import edu.gatech.chai.omoponfhir.omopv5.stu3.utilities.FHIRException;`
`import org.hl7.fhir.dstu3.model.codesystems.AdministrativeGender; becomes import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;`
`import org.hl7.fhir.dstu3.model.Reference becomes import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt
;`
`import org.hl7.fhir.dstu3.model.Resource; becomes import ca.uhn.fhir.model.dstu2.resource.BaseResource;`
`import org.hl7.fhir.dstu3.model.DateTimeType; becomes import ca.uhn.fhir.model.primitive.DateTimeDt;`
`import org.hl7.fhir.dstu3.model.Type; becomes import ca.uhn.fhir.model.api.IDatatype;`
`import org.hl7.fhir.dstu3.model.codesystems.ConditionCategory; becomes import ca.uhn.fhir.model.dstu2.valueset.ConditionCategoryCodesEnum;`
`import org.hl7.fhir.dstu3.model.Device; becomes import ca.uhn.fhir.model.dstu2.resource.Device;`
`import org.hl7.fhir.dstu3.model.Period; becomes import ca.uhn.fhir.model.dstu2.composite.PeriodDt;`
`import org.hl7.fhir.dstu3.model.DeviceUseStatement; becomes import ca.uhn.fhir.model.dstu2.resource.DeviceUseStatement;`
`import org.hl7.fhir.dstu3.model.DocumentReference; becomes import ca.uhn.fhir.model.dstu2.resource.DocumentReference;`
`import org.hl7.fhir.dstu3.model.Encounter; becomes import ca.uhn.fhir.model.dstu2.resource.Encounter;`
`import org.hl7.fhir.dstu3.model.Identifier; becomes import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;`
`import org.hl7.fhir.dstu3.model.ResourceType; becomes import ca.uhn.fhir.model.dstu2.valueset.ResourceTypeEnum;`
`import org.hl7.fhir.dstu3.model.Enumerations.DocumentReferenceStatus; becomes import ca.uhn.fhir.model.dstu2.valueset.DocumentReferenceStatusEnum;`
`import org.hl7.fhir.dstu3.model.Attachment; becomes import ca.uhn.fhir.model.dstu2.composite.AttachmentDt;`
`import org.hl7.fhir.dstu3.model.DocumentReference.DocumentReferenceContentComponent; becomes import ca.uhn.fhir.model.dstu2.resource.DocumentReference.Content;`
`import org.hl7.fhir.dstu3.model.DocumentReference.DocumentReferenceContextComponent; becomes import ca.uhn.fhir.model.dstu2.resource.DocumentReference.Context;`
`import org.hl7.fhir.dstu3.model.Encounter; becomes import ca.uhn.fhir.model.dstu2.resource.Encounter;`
`import org.hl7.fhir.dstu3.model.Medication; becomes import ca.uhn.fhir.model.dstu2.resource.Medication;`
`import org.hl7.fhir.dstu3.model.Medication.MedicationIngredientComponent; becomes import ca.uhn.fhir.model.dstu2.resource.Medication.ProductIngredient;`
`import org.hl7.fhir.dstu3.model.Organization; becomes import ca.uhn.fhir.model.dstu2.resource.Organization;`
`import org.hl7.fhir.dstu3.model.Address; becomes import ca.uhn.fhir.model.dstu2.composite.AddressDt;`
`import org.hl7.fhir.dstu3.model.Address.AddressUse; becomes import ca.uhn.fhir.model.dstu2.valueset.AddressUseEnum;`
`import org.hl7.fhir.dstu3.model.Observation; becomes import ca.uhn.fhir.model.dstu2.resource.Observation;`
`import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent; becomes import ca.uhn.fhir.model.dstu2.resource.Bundle.Entry;`
`import org.hl7.fhir.dstu3.model.Bundle.BundleEntryResponseComponent; becomes import ca.uhn.fhir.model.dstu2.resource.Bundle.EntryResponse;`
`import org.hl7.fhir.dstu3.model.Bundle; becomes import ca.uhn.fhir.model.dstu2.resource.Bundle;`
`import org.hl7.fhir.dstu3.model.Bundle.HTTPVerb; becomes import ca.uhn.fhir.model.dstu2.valueset.HTTPVerbEnum;`
`import org.hl7.fhir.dstu3.model.StringType; becomes import ca.uhn.fhir.model.primitive.StringDt;`
`import org.hl7.fhir.dstu3.model.ContactPoint; becomes import ca.uhn.fhir.model.dstu2.composite.ContactPointDt;`
`import org.hl7.fhir.dstu3.model.ContactPoint.ContactPointSystem; becomes import ca.uhn.fhir.model.dstu2.valueset.ContactPointSystemEnum;`
`import org.hl7.fhir.dstu3.model.ContactPoint.ContactPointUse; becomes import ca.uhn.fhir.model.dstu2.valueset.ContactPointUseEnum;`
`import org.hl7.fhir.dstu3.model.Practitioner; becomes import ca.uhn.fhir.model.dstu2.resource.Practitioner;`
`import org.hl7.fhir.dstu3.model.Patient.PatientLinkComponent; becomes import ca.uhn.fhir.model.dstu2.resource.Patient.Link;`
`import org.hl7.fhir.dstu3.model.HumanName; becomes import ca.uhn.fhir.model.dstu2.composite.HumanNameDt;`
`import org.hl7.fhir.dstu3.model.codesystems.V3MaritalStatus; becomes import ca.uhn.fhir.model.dstu2.valueset.MaritalStatusCodesEnum;`
`import org.hl7.fhir.dstu3.model.codesystems.V3ActCode; becomes import edu.gatech.chai.omoponfhir.omopv5.stu3.utilities.V3ActCode;`
`import org.hl7.fhir.dstu3.model.codesystems.OrganizationType; becomes import edu.gatech.chai.omoponfhir.omopv5.stu3.utilities.OrganizationType;`
`import org.hl7.fhir.dstu3.model.codesystems.ObservationCategory; becomes import edu.gatech.chai.omoponfhir.omopv5.stu3.utilities.ObservationCategory;`
`import org.hl7.fhir.dstu3.model.codesystems.ConditionCategory; becomes import edu.gatech.chai.omoponfhir.omopv5.stu3.utilities.ConditionCategory;`
`import org.hl7.fhir.dstu3.model.Procedure; becomes import ca.uhn.fhir.model.dstu2.resource.Procedure;`
`import org.hl7.fhir.dstu3.model.Procedure.ProcedurePerformerComponent; becomes import ca.uhn.fhir.model.dstu2.resource.Procedure.Performer;`
` becomes import ca.uhn.fhir.model.dstu2.valueset.ProcedureStatusEnum;`
`import org.hl7.fhir.dstu3.model.Quantity; becomes import ca.uhn.fhir.model.dstu2.composite.QuantityDt;`
`import org.hl7.fhir.dstu3.model.SimpleQuantity; becomes import ca.uhn.fhir.model.dstu2.composite.SimpleQuantityDt;`
`import org.hl7.fhir.dstu3.model.Observation.ObservationStatus; become import ca.uhn.fhir.model.dstu2.valueset.ObservationStatusEnum;`
`import org.hl7.fhir.dstu3.model.Observation.ObservationReferenceRangeComponent; becomes import ca.uhn.fhir.model.dstu2.resource.Observation.ReferenceRange;`
`import org.hl7.fhir.dstu3.model.Observation.ObservationComponentComponent; becomes import ca.uhn.fhir.model.dstu2.resource.Observation.Component;`
`import org.hl7.fhir.dstu3.model.Duration; becomes
import ca.uhn.fhir.model.dstu2.composite.DurationDt;`
`import org.hl7.fhir.dstu3.model.MedicationRequest; becomes import ca.uhn.fhir.model.dstu2.resource.MedicationOrder;`
`import import org.hl7.fhir.dstu3.model.MedicationRequest.MedicationRequestDispenseRequestComponent; becomes import ca.uhn.fhir.model.dstu2.resource.MedicationOrder.DispenseRequest;`
`import org.hl7.fhir.dstu3.model.Dosage; becomes import ca.uhn.fhir.model.dstu2.resource.MedicationOrder.DosageInstruction;`
`import org.hl7.fhir.dstu3.model.Dosage; becomes import ca.uhn.fhir.model.dstu2.resource.MedicationStatement.Dosage;`
`import org.hl7.fhir.dstu3.model.Encounter.EncounterStatus; becomes import ca.uhn.fhir.model.dstu2.valueset.EncounterStateEnum;`
`import org.hl7.fhir.dstu3.model.Encounter.EncounterParticipantComponent; becomes import ca.uhn.fhir.model.dstu2.resource.Encounter.Participant;`
`import org.hl7.fhir.dstu3.model.MedicationStatement; becomes import ca.uhn.fhir.model.dstu2.resource.MedicationStatement;`
`import org.hl7.fhir.dstu3.model.Annotation; becomes import ca.uhn.fhir.model.dstu2.composite.AnnotationDt;`
`import org.hl7.fhir.dstu3.model.MedicationStatement.MedicationStatementStatus; becomes import ca.uhn.fhir.model.dstu2.valueset.MedicationStatementStatusEnum;`
`import org.hl7.fhir.dstu3.model.ValueSet.ConceptSetComponent; becomes import ca.uhn.fhir.model.dstu2.resource.ValueSet.CodeSystemConcept;`
`import org.hl7.fhir.dstu3.model.UriType; becomes import ca.uhn.fhir.model.primitive.UriDt;`
`import org.hl7.fhir.dstu3.model.Parameters; becomes import ca.uhn.fhir.model.dstu2.resource.Parameters;`

`IdType becomes IdDt`
`CodeableConcept becomes CodeableConceptDt`
`Resource becomes BaseResource`
`Coding becomes CodingDt`
`DateTimeType becomes DateTimeDt`
`Period becomes PeriodDt`
`Type becomes IDatatype`
`Reference becomes ResourceReferenceDt`
`DocumentReferenceContentComponent becomes Content`
`DocumentReferenceContextComponent becomes Context`
`Identifier becomes IdentifierDt`
`Address becomes AddressDt`
`AddressUse becomes AddressUseEnum`
`BundleEntryComponent becomes Entry`
`BundleEntryResponseComponent becomes EntryResponse`
`ResourceType.Patient becomes ResourceTypeEnum.PATIENT`
`ResourceType.Observation becomes ResourceTypeEnum.OBSERVATION`
`PatientLinkComponent becomes Link`
`HumanName becomes HumanNameDt`
`ContactPoint becomes ContactPointDt`
`V3MaritalStatus.fromCode becomes MaritalStatusCodesEnum.forCode`
`AdministrativeGender becomes AdministrativeGenderEnum`
`StringType becomes StringDt`
`ProcedurePerformerComponent becomes Performer`
`Procedure.ProcedureStatus becomes ProcedureStatusEnum`
`ObservationComponentComponent becomes Component`
`ObservationStatus becomes ObservationStatusEnum`
`ObservationReferenceRangeComponent becomes ReferenceRange`
`MedicationRequestDispenseRequestComponent becomes DispenseRequest`
`MedicationIngredientComponent becomes ProductIngredient`
`toCalendar becomes getValueAsCalendar`
`EncounterStatus becomes EncounterStateEnum`
`EncounterParticipantComponent becomes Participant`
`Annotation becomes AnnotationDt`

```
getAuthorFirstRep method doesn't exist, but you can impplement it via the following (by example)

//		ResourceReferenceDt authorReference = fhirResource.getAuthorFirstRep();
		if (fhirResource.getAuthor().isEmpty()) {
			fhirResource.addAuthor();
		}
		ResourceReferenceDt authorReference = fhirResource.getAuthor().get(0);
```

In the Condiditon class:
	there is no "Subject" type, but there is a "Patient" type. They are equivalent
		`.setSubject becomes .setPatient`
		`.getSubject becomes .getPatient`
	there is no "Context" type, but there is an "Encounter" type. they are equivalent
		`.setContext becomes .setEncounter`
		`.getContext becomes .getEncounter`

This that were previously Date Type, need to be converted over to DateTimeDt

any Coding instance that used the 3 parameter constructor needs changed to the 2 constructor, and adding the display
	new Coding(systemUriString, codeString, displayString) 
	VS 
	CodingDt tempcoding = new CodingDt(systemUriString,codeString);
	tempcoding.setDisplay(displayString)

EntryResponse type doesn't have a constructor that takes in the status
	EntryResponse responseComponent = new EntryResponse(new StringType("201 Created"));
	VS
	EntryResponse responseComponent = new EntryResponse();
	responseComponent.setStatus(new StringDt("201 Created"));

DSTU2 doesn't have a unified version of https://www.hl7.org/fhir/v3/ActCode/cs.html

DSTU2 uses careProvider, STU3 uses generalPracticioner

for AdministrativeGenderEnum
	`AdministrativeGender.toCode becomes AdministrativeGenderEnum.forCode or AdministrativeGenderEnum.getCode `
	`AdministrativeGender.fromCode becomes AdministrativeGenderEnum.forCode`
	this might have issues with capitalization. 

DSTU2 doesn't have addContained in BaseResource, so instead you must convert it to be compatible with setcontained ex:

`myDeviceUseStatement.addContained(myDevice);` becomes 
```	
List<IResource> tempList = myDeviceUseStatement.getContained().getContainedResources();
tempList.add(myDevice);
ContainedDt tempContained = new ContainedDt();
tempContained.setContainedResources(tempList);
myDeviceUseStatement.setContained(tempContained);	
```
	This was necessary in OmopMedicationRequest as well

In the Procdure class, there is no "Context" type, but there is an "Encounter" type. they are equivalent
	`.setContext becomes .setEncounter`
	`.getContext becomes .getEncounter`


In the Observation Class, the "comment" field used to be "comments"
	`.getComment();` becomes `getComments();`
	there is no "Context" type, but there is an "Encounter" type. they are equivalent
		`.setContext becomes .setEncounter`
		`.getContext becomes .getEncounter`
	The category field can only hold 1 at a time in DSTU2, not multiple like in STU3
		`.addCategory becomes .setCategory`
	there is no addPerformer method
		`observation.addPerformer(performerRef);` becomes
		```
		List<ResourceReferenceDt> tempList= observation.getPerformer();
		tempList.add(performerRef);
		observation.setPerformer(tempList);
		```
	getAppliesTo doesn't exist as far as far as ranges are concerned.
		the only thing that could potentially apply is the "meaning"

In MedicationRequest, in DSTU2, it was all called MedicationOrder instead.  
	Subject doesn't exist as a type, instead we have Patient

In the Encoutner Class, the "reference" field is slightly changed
	`.setReferenceElement becomes .setReference`

In the DocumentReference class:
	add author doesn't work the way we think it does. Instead, we should set and get as in the Observation class with addPerformer
	```
//			documentReference.addAuthor(practitionerReference);
			List<ResourceReferenceDt> tempList = documentReference.getAuthor();
			tempList.add(practitionerReference);
			documentReference.setAuthor(tempList);
	```
	Content doesn't take anything for the constructor, you have to add an attachment after the fact
	```
//			Content documentReferenceContentComponent = new Content(attachment);
			Content documentReferenceContentComponent = new Content();
			documentReferenceContentComponent.setAttachment(attachment);
	```

in the Enocunter class:
	there is no "Subject" type, but there is a "Patient" type. They are equivalent
		`.setSubject becomes .setPatient`
		`.getSubject becomes .getPatient`
	Diagnosis aren't in existance, but an equivalent is "Indication"
		```
			DiagnosisComponent diagnosisComponent = new DiagnosisComponent();
			diagnosisComponent.setCondition(conditionReference);
			encounter.addDiagnosis(diagnosisComponent);
			List<ResourceReferenceDt> tempList=encounter.getIndication();
			tempList.add(conditionReference);
			encounter.setIndication(tempList);
		```

in the OmopPatient class:
	Identifier types aren't of the CodeableConceptDt type; instead they are IdentifierTypeCodesEnum

in the MedicationStatement class:
	there is no "Subject" type, but there is a "Patient" type. They are equivalent
		`.setSubject becomes .setPatient`
		`.getSubject becomes .getPatient`
	there is no "Context" type, but there is an "Encounter" type. they are equivalent
		`.setContext becomes .setEncounter`
		`.getContext becomes .getEncounter`
	there is no setDose Method, but instead setQuantity
		`.setDose becomes .setQuantity`
	BasedOn and PartOf don't exist in this class

useful sites for completing the work. 
https://hapifhir.io/hapi-fhir/apidocs/hapi-fhir-structures-dstu2/
https://hapifhir.io/hapi-fhir/apidocs/hapi-fhir-structures-dstu3/
https://hapifhir.io/hapi-fhir/apidocs/hapi-fhir-base/
http://hl7.org/fhir/DSTU2/resourcelist.html
http://hl7.org/fhir/STU3/resourcelist.html