package ubic.gemma.web.controller.expression.experiment;

import java.io.Serializable;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.FactorValue;

public class FactorValueObject implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 3378801249808036785L;
    private String factorValue;
    private long id;

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

        this.factorValue = "";

        if ( fv.getCharacteristics().size() > 0 ) {
            for ( Characteristic c : fv.getCharacteristics() ) {
                factorValue += c.getValue();
            }

        } else {
            factorValue += fv.getValue();
        }

    }
    
    public String getFactorValue(){
        
        return factorValue;
    }

}
