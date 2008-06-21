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
package ubic.gemma.web.controller.expression.experiment;

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
        this.setCategory( getCategoryString( factor.getCategory() ) );
        this.setCategoryUri( getCategoryUri( factor.getCategory() ) );

        /*
         * Note: this code copied from the ExperimentalDesignController.
         */
        Collection<FactorValueValueObject> vals = new HashSet<FactorValueValueObject>();
        for ( FactorValue value : factor.getFactorValues() ) {
            Characteristic category = value.getExperimentalFactor().getCategory();
            if ( category == null ) {
                category = Characteristic.Factory.newInstance();
                category.setValue( value.getExperimentalFactor().getName() );
            }
            vals.add( new FactorValueValueObject( value, category ) );
        }

        this.setValues( vals );
    }

    private String getCategoryString( Characteristic category ) {
        if ( category == null ) return "no category";
        StringBuffer buf = new StringBuffer();
        if ( category.getCategory() != null ) {
            buf.append( category.getCategory() );
            if ( category.getValue() != null && !category.getValue().equals( category.getCategory() ) ) {
                buf.append( " / " );
                buf.append( category.getValue() );
            }
        } else if ( category.getValue() != null ) {
            buf.append( category.getValue() );
        }
        return buf.toString();
    }

    private String getCategoryUri( Characteristic category ) {
        if ( category instanceof VocabCharacteristic ) {
            VocabCharacteristic vc = ( VocabCharacteristic ) category;
            return vc.getValueUri() == null ? vc.getCategoryUri() : vc.getValueUri();
        } else {
            return null;
        }
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

    public void setFactorValues( String factorValues ) {
        this.factorValues = factorValues;
    }

}
