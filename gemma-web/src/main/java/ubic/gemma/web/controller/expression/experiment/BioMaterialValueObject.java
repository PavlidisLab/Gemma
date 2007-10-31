package ubic.gemma.web.controller.expression.experiment;

import java.io.Serializable;

import ubic.gemma.model.expression.biomaterial.BioMaterial;

public class BioMaterialValueObject implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 2160220506752909053L;

    
    private Long id;
    private String description;
    private String factorValue;
    private String name;
    private String bioAssayDescription;
    private String bioAssayName;
    
    
    
    public BioMaterialValueObject(){
        super();               
    }
    
    
    /**
     * @param bm
     * Given a BioMaterial sets the id, description, and name.  Doesn't not set the factor value.
     */
    public BioMaterialValueObject(BioMaterial bm){
        
        this();
        this.id = bm.getId();
        this.description = bm.getDescription();
        this.name = bm.getName();
        
    }
    @Override
    public int hashCode(){    	
    	return this.id.hashCode();    	
    }
    
    @Override
    public boolean equals(Object bmvo){    	    
    	return this.hashCode() == bmvo.hashCode();
    }
    
    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    /**
     * @param description the description to set
     */
    public void setDescription( String description ) {
        this.description = description;
    }
    /**
     * @return the factorValue
     */
    public String getFactorValue() {
        return factorValue;
    }
    /**
     * @param factorValue the factorValue to set
     */
    public void setFactorValue( String factorValue ) {
        this.factorValue = factorValue;
    }
    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }
    /**
     * @param id the id to set
     */
    public void setId( Long id ) {
        this.id = id;
    }
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    /**
     * @param name the name to set
     */
    public void setName( String name ) {
        this.name = name;
    }


	public String getBioAssayDescription() {
		return bioAssayDescription;
	}


	public void setBioAssayDescription(String bioAssayDescription) {
		this.bioAssayDescription = bioAssayDescription;
	}


	public String getBioAssayName() {
		return bioAssayName;
	}


	public void setBioAssayName(String bioAssayName) {
		this.bioAssayName = bioAssayName;
	}
    
    
    
    
}
