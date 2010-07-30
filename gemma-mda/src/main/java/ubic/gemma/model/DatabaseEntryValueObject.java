package ubic.gemma.model;

import ubic.gemma.model.common.description.DatabaseEntry;

public class DatabaseEntryValueObject {	
	
	private String accession;
	private ExternalDatabaseValueObject externalDatabase;
		
	public String getAccession() {
		return accession;
	}
	public void setAccession(String accession) {
		this.accession = accession;
	}
	public ExternalDatabaseValueObject getExternalDatabase() {
		return externalDatabase;
	}
	public void setExternalDatabase(ExternalDatabaseValueObject externalDatabase) {
		this.externalDatabase = externalDatabase;
	}	
	
	public static DatabaseEntryValueObject fromEntity( DatabaseEntry de) {
		if (de == null) return null;
		DatabaseEntryValueObject vo = new DatabaseEntryValueObject();
		vo.setAccession(de.getAccession());
		vo.setExternalDatabase( ExternalDatabaseValueObject.fromEntity(de.getExternalDatabase()) );
		return vo;
	}
}
