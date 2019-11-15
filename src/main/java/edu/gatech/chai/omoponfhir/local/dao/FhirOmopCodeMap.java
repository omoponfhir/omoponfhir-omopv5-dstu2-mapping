package edu.gatech.chai.omoponfhir.local.dao;

import java.sql.Connection;
import java.util.List;

import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import edu.gatech.chai.omoponfhir.local.model.FhirOmopCodeMapEntry;

public interface FhirOmopCodeMap {
	public Connection connect();
	
	public int save(FhirOmopCodeMapEntry codeMapEntry);
	public void update(FhirOmopCodeMapEntry codeMapEntry);
	public void delete(Long omopConcept);
	public List<FhirOmopCodeMapEntry> get();
	public Long getOmopCodeFromFhirCoding(CodingDt fhirCoding);
	public CodingDt getFhirCodingFromOmopConcept(Long omopConcept);
	public CodingDt getFhirCodingFromOmopSourceString(String omopSourceString);
}
