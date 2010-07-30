package ubic.gemma.model;

import ubic.gemma.model.common.description.ExternalDatabase;

public class ExternalDatabaseValueObject {
    
	private String name;
    
    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public static ExternalDatabaseValueObject fromEntity(ExternalDatabase ed) {
    	ExternalDatabaseValueObject vo = new ExternalDatabaseValueObject();
    	if (ed.getName() != null) vo.setName(ed.getName());
    	return vo;
    }
    
}
