/*
 * The Gemma project Copyright (c) 2009 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package ubic.gemma.model.expression.experiment;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;

import java.io.Serializable;
import java.util.Iterator;

/**
 * Each factorvalue can be associated with multiple characteristics (or with a measurement). However, for flattening out
 * the objects for client display, there is only one characteristic associated here.
 * Note: this used to be called FactorValueObject and now replaces the old FactorValueValueObject. Confusing!
 *
 * @author Paul
 */
public class FactorValueValueObject implements Serializable {

    private static final long serialVersionUID = 3378801249808036785L;

    private String category;
    private String categoryUri;
    private String description;
    private String factor;
    private String value;
    private String valueUri;
    /**
     * It could be the id of the measurement if there is no characteristic.
     */
    private Long charId;
    private Long factorId;
    private Long id;
    private Boolean isBaseline = false;
    private boolean measurement = false;

    public FactorValueValueObject() {
        super();
    }

    /*
     * FIXME this constructor is messed up. We should not be using the Factor, this is for FactorValues!
     */
    public FactorValueValueObject( ExperimentalFactor ef ) {

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

    public FactorValueValueObject( FactorValue fv ) {
        super();
        if ( fv.getCharacteristics().size() == 1 ) {
            init( fv, fv.getCharacteristics().iterator().next() );
        } else if ( fv.getCharacteristics().size() > 1 ) {
            /*
             * Inadequate! Want to capture them all.
             */
            init( fv, fv.getCharacteristics().iterator().next() );
        } else {
            init( fv, null );
        }
    }

    /**
     * @param c - specific characteristic we're focusing on (yes, this is confusing). This is necessary if the
     *          FactorValue has multiple characteristics. DO NOT pass in the ExperimentalFactor category, this just
     *          confuses things. FIXME this makes no sense and we _do_ pass in the EF category in several places.
     *          If c is null, the plain "value" is used.
     */
    public FactorValueValueObject( FactorValue value, Characteristic c ) {
        super();
        init( value, c );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        FactorValueValueObject other = ( FactorValueValueObject ) obj;
        if ( charId == null ) {
            if ( other.charId != null )
                return false;
        } else if ( !charId.equals( other.charId ) )
            return false;

        if ( id == null ) {
            if ( other.id != null )
                return false;
        } else
            return id.equals( other.id ) && id.equals( other.id );

        if ( value == null ) {
            if ( other.value != null )
                return false;
        } else if ( !value.equals( other.value ) )
            return false;
        return true;
    }

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

    public String getCategoryUri() {
        return categoryUri;
    }

    public void setCategoryUri( String categoryUri ) {
        this.categoryUri = categoryUri;
    }

    public Long getCharId() {
        return charId;
    }

    public void setCharId( Long charId ) {
        this.charId = charId;
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

    public Long getFactorId() {
        return factorId;
    }

    public void setFactorId( Long factorId ) {
        this.factorId = factorId;
    }

    public String getFactorValue() {

        return factor;
    }

    public void setFactorValue( String value ) {

        this.factor = value;
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

    public Boolean getIsBaseline() {
        return isBaseline;
    }

    public void setIsBaseline( Boolean isBaseline ) {
        this.isBaseline = isBaseline;
    }

    public String getValue() {
        return value;
    }

    public void setValue( String value ) {
        this.value = value;
    }

    public String getValueUri() {
        return valueUri;
    }

    public void setValueUri( String valueUri ) {
        this.valueUri = valueUri;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );

        if ( id == null ) {
            result = prime * result + ( ( charId == null ) ? 0 : charId.hashCode() );

            result = prime * result + ( ( value == null ) ? 0 : value.hashCode() );
        }
        return result;
    }

    public boolean isMeasurement() {
        return measurement;
    }

    public void setMeasurement( boolean measurement ) {
        this.measurement = measurement;
    }

    @Override
    public String toString() {
        return "FactorValueValueObject [factor=" + factor + ", value=" + value + "]";
    }

    private String getSummaryString( FactorValue fv ) {
        StringBuilder buf = new StringBuilder();
        if ( fv.getCharacteristics().size() > 0 ) {
            for ( Iterator<Characteristic> iter = fv.getCharacteristics().iterator(); iter.hasNext(); ) {
                Characteristic c = iter.next();
                buf.append( c.getValue() == null ? "[Unassigned]" : c.getValue() );
                if ( iter.hasNext() )
                    buf.append( ", " );
            }
        } else if ( fv.getMeasurement() != null ) {
            buf.append( fv.getMeasurement().getValue() );
        } else if ( StringUtils.isNotBlank( fv.getValue() ) ) {
            buf.append( fv.getValue() );
        } else {
            buf.append( "?" );
        }
        return buf.toString();
    }

    private void init( FactorValue val, Characteristic c ) {
        this.setId( val.getId() );
        this.setFactorValue( getSummaryString( val ) );
        this.setFactorId( val.getExperimentalFactor().getId() );
        this.isBaseline = val.getIsBaseline() != null ? val.getIsBaseline() : this.isBaseline;

        if ( val.getMeasurement() != null ) {
            this.setMeasurement( true );
            this.value = val.getMeasurement().getValue();
            this.setCharId( val.getMeasurement().getId() );
        } else if ( c != null && c.getId() != null ) {
            this.setCharId( c.getId() );
        } else {
            this.value = val.getValue();
        }

        if ( c != null ) {
            this.setCategory( c.getCategory() );
            this.setValue( c.getValue() ); // clobbers if we set it already
            if ( c instanceof VocabCharacteristic ) {
                VocabCharacteristic vc = ( VocabCharacteristic ) c;
                this.setCategoryUri( vc.getCategoryUri() );
                this.setValueUri( vc.getValueUri() );
            }
        }

        /*
         * Make sure we fill in the Category for this.
         */
        Characteristic factorCategory = val.getExperimentalFactor().getCategory();
        if ( this.getCategory() == null && factorCategory != null ) {
            this.setCategory( factorCategory.getCategory() );

            if ( factorCategory instanceof VocabCharacteristic ) {
                VocabCharacteristic vc = ( VocabCharacteristic ) factorCategory;
                this.setCategoryUri( vc.getCategoryUri() );
            }

        }
    }

}