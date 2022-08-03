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
package ubic.gemma.model.expression.experiment;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.description.Characteristic;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * @author luke
 * @author keshav
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
public class ExperimentalFactorValueObject extends IdentifiableValueObject<ExperimentalFactor> implements Serializable {

    private static final long serialVersionUID = -2615804031123874251L;

    private String category;
    private String categoryUri;

    private String description;

    private String factorValues;

    private String name;
    private Integer numValues = 0;
    private String type = "categorical"; // continuous or categorical.
    private Collection<FactorValueValueObject> values;

    /**
     * Required when using the class as a spring bean.
     */
    public ExperimentalFactorValueObject() {
    }

    public ExperimentalFactorValueObject( Long id ) {
        super( id );
    }

    public ExperimentalFactorValueObject( ExperimentalFactor factor ) {
        super( factor.getId() );
        this.setName( factor.getName() );
        this.setDescription( factor.getDescription() );

        if ( factor.getCategory() != null ) {
            this.setCategory( factor.getCategory().getCategory() );
            this.setCategoryUri( factor.getCategory().getCategoryUri() );
        }

        /*
         * Note: this code copied from the ExperimentalDesignController.
         */
        Collection<FactorValueValueObject> vals = new HashSet<>();

        if ( factor.getType() != null ) {
            this.type = factor.getType().equals( FactorType.CATEGORICAL ) ? "categorical" : "continuous";
        } else {
            // Backwards compatibility: for old entries created prior to introduction of 'type' field in
            // ExperimentalFactor entity.
            // We have to take a guess.
            if ( factor.getFactorValues().isEmpty() ) {
                this.type = "categorical";
            } else {
                // Just use first factor value to make our guess.
                if ( factor.getFactorValues().iterator().next().getMeasurement() != null ) {
                    this.type = "continuous";
                } else {
                    this.type = "categorical";
                }
            }
        }

        if ( factor.getFactorValues() == null || factor.getFactorValues().isEmpty() ) {
            return;
        }

        Collection<FactorValue> fvs = factor.getFactorValues();
        StringBuilder factorValuesAsString = new StringBuilder( StringUtils.EMPTY );
        for ( FactorValue fv : fvs ) {
            String fvName = fv.toString();
            if ( StringUtils.isNotBlank( fvName ) ) {
                factorValuesAsString.append( fvName ).append( ", " );
            }
        }
        /* clean up the start and end of the string */
        factorValuesAsString = new StringBuilder(
                StringUtils.remove( factorValuesAsString.toString(), factor.getName() + ":" ) );
        factorValuesAsString = new StringBuilder( StringUtils.removeEnd( factorValuesAsString.toString(), ", " ) );

        this.setFactorValues( factorValuesAsString.toString() );

        this.numValues = factor.getFactorValues().size();
        Characteristic c = factor.getCategory();
        /*
         * NOTE this replaces code that previously made no sense. PP
         */
        for ( FactorValue value : factor.getFactorValues() ) {
            vals.add( new FactorValueValueObject( value, c ) );
        }

        this.setValues( vals );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        ExperimentalFactorValueObject other = ( ExperimentalFactorValueObject ) obj;
        return Objects.equals( id, other.id );
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

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public String getFactorValues() {
        return factorValues;
    }

    /**
     * @param factorValues Set a string which describes (in summary) the factor values
     */
    public void setFactorValues( String factorValues ) {
        this.factorValues = factorValues;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( int ) ( id ^ ( id >>> 32 ) );
        return result;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    /**
     * @return the numValues
     */
    public Integer getNumValues() {
        return this.numValues;
    }

    /**
     * @param numValues the numValues to set
     */
    public void setNumValues( Integer numValues ) {
        this.numValues = numValues;
    }

    public String getType() {
        return type;
    }

    public void setType( String type ) {
        this.type = type;
    }

    public Collection<FactorValueValueObject> getValues() {
        return values;
    }

    public void setValues( Collection<FactorValueValueObject> values ) {
        this.values = values;
    }
}
