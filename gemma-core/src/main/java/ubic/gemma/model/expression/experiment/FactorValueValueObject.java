/*
 * The Gemma project Copyright (c) 2009 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package ubic.gemma.model.expression.experiment;

import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.description.Characteristic;

import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * Each factorvalue can be associated with multiple characteristics (or with a measurement). However, for flattening out
 * the objects for client display, there is only one characteristic associated here.
 * Note: this used to be called FactorValueObject and now replaces the old FactorValueValueObject. Confusing!
 *
 * @author Paul
 * @deprecated aim towards using the FactorValueBasicValueObject. This one is confusing. Once usage of this
 *             type has been completely phased out, revise the BioMaterialValueObject and relevant DAOs and Services.
 */
@Deprecated
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
public class FactorValueValueObject extends IdentifiableValueObject<FactorValue> implements Serializable {

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
    private Boolean isBaseline = false;
    private boolean measurement = false;

    /**
     * Required when using the class as a spring bean.
     */
    public FactorValueValueObject() {
    }

    public FactorValueValueObject( Long id ) {
        super( id );
    }

    public FactorValueValueObject( FactorValue fv ) {
        super( fv.getId() );
        if ( fv.getCharacteristics().size() == 1 ) {
            this.init( fv, fv.getCharacteristics().iterator().next() );
        } else if ( fv.getCharacteristics().size() > 1 ) {
            /*
             * Inadequate! Want to capture them all - use FactorValueBasicValueObject!
             */
            this.init( fv, fv.getCharacteristics().iterator().next() );
        } else {
            this.init( fv, null );
        }
    }

    /**
     * @param      c     - specific characteristic we're focusing on (yes, this is confusing). This is necessary if the
     *                   FactorValue has multiple characteristics. DO NOT pass in the ExperimentalFactor category, this
     *                   just
     *                   confuses things.
     *                   If c is null, the plain "value" is used.
     * @param      value value
     * @deprecated see class deprecated note
     */
    public FactorValueValueObject( FactorValue value, @Nullable Characteristic c ) {
        super( value.getId() );
        this.init( value, c );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
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
            return other.value == null;
        } else
            return value.equals( other.value );
    }

    @Override
    public String toString() {
        return "FactorValueValueObject [factor=" + factor + ", value=" + value + "]";
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

    private void init( FactorValue val, @Nullable Characteristic c ) {
        this.setFactorValue( FactorValueBasicValueObject.getSummaryString( val ) );
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

            this.setCategoryUri( c.getCategoryUri() );
            this.setValueUri( c.getValueUri() );

        }

        /*
         * Make sure we fill in the Category for this.
         */
        Characteristic factorCategory = val.getExperimentalFactor().getCategory();
        if ( this.getCategory() == null && factorCategory != null ) {
            this.setCategory( factorCategory.getCategory() );
            this.setCategoryUri( factorCategory.getCategoryUri() );

        }
    }

}
