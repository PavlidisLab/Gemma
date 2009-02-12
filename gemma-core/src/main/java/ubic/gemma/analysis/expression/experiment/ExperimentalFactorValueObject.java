/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.analysis.expression.experiment;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * @author luke
 * @author keshav
 * @version $Id$
 */
public class ExperimentalFactorValueObject {

    private long id;
    private String name;
    private String description;
    private String category;
    private String categoryUri;
    private String factorValues;  
    private int numValues = 0;

    /**
     * @return the numValues
     */
    public int getNumValues() {
        return this.numValues;
    }

    /**
     * @param numValues the numValues to set
     */
    public void setNumValues( int numValues ) {
        this.numValues = numValues;
    }

    private String type = "Categorical"; // continuous or categorical.

    public String getType() {
        return type;
    }

    public void setType( String type ) {
        this.type = type;
    }

    private Collection<FactorValueValueObject> values;

    public Collection<FactorValueValueObject> getValues() {
        return values;
    }

    public void setValues( Collection<FactorValueValueObject> values ) {
        this.values = values;
    }

    public ExperimentalFactorValueObject() {
    }

    public ExperimentalFactorValueObject( ExperimentalFactor factor ) {
        this.setId( factor.getId() );
        this.setName( factor.getName() );
        this.setDescription( factor.getDescription() );
        
        
        if (factor.getCategory() != null)
            this.setCategory( factor.getCategory().getCategory() );
        
        this.setCategoryUri( getCategoryUri( factor.getCategory() ) );

        /*
         * Note: this code copied from the ExperimentalDesignController.
         */
        Collection<FactorValueValueObject> vals = new HashSet<FactorValueValueObject>();

        if ( factor.getFactorValues() == null || factor.getFactorValues().isEmpty() ) {
            return;
        }

        // Normally not reached?
        this.numValues = factor.getFactorValues().size();

        for ( FactorValue value : factor.getFactorValues() ) {

            if ( value.getMeasurement() != null ) {
                this.type = "Continuous";
            } else {
                this.type = "Categorical";
            }

            Characteristic c = value.getExperimentalFactor().getCategory();
            if ( c == null ) {
                c = Characteristic.Factory.newInstance();
                if (value.getExperimentalFactor().getCategory() != null)
                    c.setValue( value.getExperimentalFactor().getCategory().getCategory() );
            }
            vals.add( new FactorValueValueObject( value, c ) );
        }

        this.setValues( vals );
    }

    private String getCategoryUri( Characteristic c ) {
        if ( c instanceof VocabCharacteristic ) {
            VocabCharacteristic vc = ( VocabCharacteristic ) c;
            return vc.getCategoryUri();
        }
        return null;

    }

    public long getId() {
        return id;
    }

    public void setId( long id ) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public String getCategory() { 
        return category;
    }

    public void setCategory( String category ) { 
        this.category = category;
    }

    public String getCategoryUri() {
        return categoryUri;
    }

    public void setCategoryUri( String categoryUri ) {
        this.categoryUri = categoryUri;
    }

    public String getFactorValues() {
        return factorValues;
    }

    /**
     * Set a string which describes (in summary) the factor values
     * 
     * @param factorValues
     */
    public void setFactorValues( String factorValues ) {
        this.factorValues = factorValues;
    }

}
