package ubic.gemma.web.controller.expression.experiment;

import java.io.Serializable;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

public class FactorValueObject implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 3378801249808036785L;
    private String factor;
    private long id;
    private String category;
    private String description;

    /**
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * @param category the category to set
     */
    public void setCategory( String category ) {
        this.category = category;
    }

    /**
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId( long id ) {
        this.id = id;
    }

    public FactorValueObject( FactorValue fv ) {

        this.id = fv.getId();        
        this.factor = "";

        if ( fv.getCharacteristics().size() > 0 ) {
            for ( Characteristic c : fv.getCharacteristics() ) {
                factor += c.getValue();
            }

        } else {
            factor += fv.getValue();
        }

    }

    public FactorValueObject( ExperimentalFactor ef ) {

        this.description = ef.getDescription();
        this.factor = ef.getName();
        this.id = ef.getId();       
        
        Characteristic category = ef.getCategory();
        if (category == null)
           this.category =  "none";        
        else if ( category instanceof VocabCharacteristic ) {
            VocabCharacteristic vc = ( VocabCharacteristic ) category;
            this.category =  vc.getCategoryUri();
        } else
            this.category = category.getCategory();
    }

    public String getFactorValue() {

        return factor;
    }

    public void setFactorValue( String value ) {

        this.factor = value;
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

}
