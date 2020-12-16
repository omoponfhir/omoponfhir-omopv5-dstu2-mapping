package edu.gatech.chai.omoponfhir.omopv5.dstu2.utilities;

import org.hl7.fhir.exceptions.FHIRException;

public enum ConditionCategory {

    /**
     * An item on a problem list which can be managed over time and can be expressed by a practitioner (e.g. physician, nurse), patient, or related person.
     */
    PROBLEMLISTITEM,
    /**
     * A point in time diagnosis (e.g. from a physician or nurse) in context of an encounter.
     */
    ENCOUNTERDIAGNOSIS,
    /**
     * added to help the parsers
     */
    NULL;
    public static ConditionCategory fromCode(String codeString) throws FHIRException {
        if (codeString == null || "".equals(codeString))
            return null;
        if ("problem-list-item".equals(codeString))
            return PROBLEMLISTITEM;
        if ("encounter-diagnosis".equals(codeString))
            return ENCOUNTERDIAGNOSIS;
        throw new FHIRException("Unknown ConditionCategory code '"+codeString+"'");
    }
    public String toCode() {
        switch (this) {
            case PROBLEMLISTITEM: return "problem-list-item";
            case ENCOUNTERDIAGNOSIS: return "encounter-diagnosis";
            default: return "?";
        }
    }
    public String getSystem() {
        return "http://hl7.org/fhir/condition-category";
    }
    public String getDefinition() {
        switch (this) {
            case PROBLEMLISTITEM: return "An item on a problem list which can be managed over time and can be expressed by a practitioner (e.g. physician, nurse), patient, or related person.";
            case ENCOUNTERDIAGNOSIS: return "A point in time diagnosis (e.g. from a physician or nurse) in context of an encounter.";
            default: return "?";
        }
    }
    public String getDisplay() {
        switch (this) {
            case PROBLEMLISTITEM: return "Problem List Item";
            case ENCOUNTERDIAGNOSIS: return "Encounter Diagnosis";
            default: return "?";
        }
    }


}

