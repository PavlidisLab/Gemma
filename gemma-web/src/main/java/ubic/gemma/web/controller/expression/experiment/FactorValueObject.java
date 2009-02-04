/*
 * The Gemma project Copyright (c) 2009 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package ubic.gemma.web.controller.expression.experiment;

import java.io.Serializable;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * @author ?
 * @version $Id$
 * @deprecated ? There is already an object like this, see ExperimentalFactorValueObject
 */
@Deprecated
public class FactorValueObject implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 3378801249808036785L;
    private String factor;
    private long id;
    private String category;
    private String description;
    private Characteristic categoryCharacteritic;

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

    public FactorValueObject() {
        super();

    }

    public FactorValueObject( FactorValue fv ) {

        this.id = fv.getId();
        this.factor = "";

        if ( fv.getCharacteristics().size() > 0 ) {
            for ( Characteristic c : fv.getCharacteristics() ) {
                factor += c.getValue();
                // FIXME: with will always be the category and description of the last characteristic....
                category = c.getCategory();
                description = c.getDescription();
            }

        } else {
            factor += fv.getValue();
        }

    }

    public FactorValueObject( ExperimentalFactor ef ) {

        this.description = ef.getDescription();
        this.factor = ef.getName();
        this.id = ef.getId();

        Characteristic c = ef.getCategory();
        if ( c == null )
            this.category = "none";
        else if ( c instanceof VocabCharacteristic ) {
            VocabCharacteristic vc = ( VocabCharacteristic ) c;
            this.category = vc.getCategory();
        } else
            this.category = c.getCategory();
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

    /**
     * @return the categoryCharacteritic
     */
    public Characteristic getCategoryCharacteritic() {
        return categoryCharacteritic;
    }

    /**
     * @param categoryCharacteritic the categoryCharacteritic to set
     */
    public void setCategoryCharacteritic( Characteristic categoryCharacteritic ) {
        this.categoryCharacteritic = categoryCharacteritic;
    }

}
