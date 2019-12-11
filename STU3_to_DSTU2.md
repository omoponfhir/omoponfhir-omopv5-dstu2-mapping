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
`//import org.hl7.fhir.dstu3.model.Resource; becomes import ca.uhn.fhir.model.dstu2.resource.BaseResource;`
` becomes import ca.uhn.fhir.model.primitive.DateTimeDt;`
` becomes import ca.uhn.fhir.model.api.IDatatype;`
`import org.hl7.fhir.dstu3.model.codesystems.ConditionCategory; becomes import ca.uhn.fhir.model.dstu2.valueset.ConditionCategoryCodesEnum;`
`//import org.hl7.fhir.dstu3.model.Device; becomes import ca.uhn.fhir.model.dstu2.resource.Device;`
`//import org.hl7.fhir.dstu3.model.Period; becomes import ca.uhn.fhir.model.dstu2.composite.PeriodDt;`
`//import org.hl7.fhir.dstu3.model.DeviceUseStatement; becomes import ca.uhn.fhir.model.dstu2.resource.DeviceUseStatement;`
`//import org.hl7.fhir.dstu3.model.DocumentReference; becomes import ca.uhn.fhir.model.dstu2.resource.DocumentReference;`
`//import org.hl7.fhir.dstu3.model.Encounter; becomes import ca.uhn.fhir.model.dstu2.resource.Encounter;`
`//import org.hl7.fhir.dstu3.model.Identifier; becomes import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;`
`//import org.hl7.fhir.dstu3.model.ResourceType; becomes import ca.uhn.fhir.model.dstu2.valueset.ResourceTypeEnum;`
`//import org.hl7.fhir.dstu3.model.Enumerations.DocumentReferenceStatus; becomes import ca.uhn.fhir.model.dstu2.valueset.DocumentReferenceStatusEnum;`
`//import org.hl7.fhir.dstu3.model.Attachment; becomes import ca.uhn.fhir.model.dstu2.composite.AttachmentDt;`
`//import org.hl7.fhir.dstu3.model.DocumentReference.DocumentReferenceContentComponent; becomes import ca.uhn.fhir.model.dstu2.resource.DocumentReference.Content;`
`//import org.hl7.fhir.dstu3.model.DocumentReference.DocumentReferenceContextComponent; becomes import ca.uhn.fhir.model.dstu2.resource.DocumentReference.Context;`
`//import org.hl7.fhir.dstu3.model.Encounter; becomes import ca.uhn.fhir.model.dstu2.resource.Encounter;`

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

In the condition class, there is no "Subject" type, but there is a "Patient" type. They are equivalent
	`.setSubject becomes .setPatient`
	`.getSubject becomes .getPatient`

In the condition class, there is no "Context" type, but there is an "Encounter" type. they are equivalent
	`.setContext becomes .setEncounter`
	`.getContext becomes .getEncounter`

This that were previously Date Type, need to be converted over to DateTimeDt