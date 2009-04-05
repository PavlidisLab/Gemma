/*
 * The Gemma project Copyright (c) 2009 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package ubic.gemma.analysis.expression.experiment;

import java.io.Serializable;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * Each factorvalue can be associated with multiple characteristics (or with a measurement). However, for flattening out
 * the objects for client display, there is only one characteristic associated here.
 * <p>
 * Note: this used to be called FactorValueObject and now replaces the old FactorValueValueObject. Confusing!
 * 
 * @author ?, paul updated
 * @version $Id$
 */
public class FactorValueValueObject implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 3378801249808036785L;
    private String factor;
    private Long id;

    private String description;

    private String category;
    private String categoryUri;
    private String value;
    private String valueUri;
    private boolean measurement = false;

    /**
     * It could be the id of the measurement if there is no characteristic.
     */
    private Long charId;

    public Long getCharId() {
        return charId;
    }

    public void setCharId( Long charId ) {
        this.charId = charId;
    }

    public String getValueUri() {
        return valueUri;
    }

    public void setValueUri( String valueUri ) {
        this.valueUri = valueUri;
    }

    public String getValue() {
        return value;
    }

    public void setValue( String value ) {
        this.value = value;
    }

    public String getCategoryUri() {
        return categoryUri;
    }

    public void setCategoryUri( String categoryUri ) {
        this.categoryUri = categoryUri;
    }

    public boolean isMeasurement() {
        return measurement;
    }

    public void setMeasurement( boolean measurement ) {
        this.measurement = measurement;
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

    public FactorValueValueObject() {
        super();

    }

    /**
     * @param value
     * @param c - specific characteristic we're focusing on (yes, this is confusing). Note that if the FV has no
     *        characteristics, you can pass in the ExperimentalFactor's characteristic instead.
     */
    public FactorValueValueObject( FactorValue value, Characteristic c ) {

        this.setId( value.getId() );
        this.setFactorValue( getSummaryString( value ) );

        if ( value.getMeasurement() != null ) {
            this.setMeasurement( true );
            this.value = value.getMeasurement().getValue();
            this.setCharId( value.getMeasurement().getId() );
        } else if ( c != null && c.getId() != null ) {
            this.setCharId( c.getId() );
        } else {
            this.value = value.getValue();
        }

        if ( c != null ) {
            this.setCategory( c.getCategory() );
            this.setValue( c.getValue() );
            if ( c instanceof VocabCharacteristic ) {
                VocabCharacteristic vc = ( VocabCharacteristic ) c;
                this.setCategoryUri( vc.getCategoryUri() );
                this.setValueUri( vc.getValueUri() );
            }
        }
    }

    /**
     * @param fv
     */
    public FactorValueValueObject( FactorValue fv ) {
        this( fv, fv.getExperimentalFactor().getCategory() );
    }

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
     * @param fv
     * @return
     */
    private String getSummaryString( FactorValue fv ) {
        StringBuffer buf = new StringBuffer();
        if ( fv.getCharacteristics().size() > 0 ) {
            for ( Iterator<Characteristic> iter = fv.getCharacteristics().iterator(); iter.hasNext(); ) {
                Characteristic c = iter.next();
                buf.append( c.getValue() == null ? "[Unassigned]" : c.getValue() );
                if ( iter.hasNext() ) buf.append( ", " );
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

}
