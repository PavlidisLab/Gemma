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
package ubic.gemma.model.expression.biomaterial;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;

/**
 * @version $Id$
 * @author lukem
 */
public class BioMaterialValueObject implements Serializable {

    private String assayDescription;
    private String assayName;
    private BioAssayValueObject bioAssay;
    private String characteristics;

    private String description;

    /*
     * Map of factor ids (factor232) to factor value (id or the actual value) for this biomaterial.
     */
    private Map<String, String> factorIdToFactorValueId;

    /*
     * Map of ids (factor232) to a representation of the factor (e.g., the name).
     */
    private Map<String, String> factors;

    private Collection<FactorValueValueObject> factorValueObjects = new HashSet<FactorValueValueObject>();
    /*
     * Map of ids (fv133) to a representation of the value (for this biomaterial.)
     */
    private Map<String, String> factorValues;

    private Long id;

    private String name;

    public BioMaterialValueObject() {
    }

    public BioMaterialValueObject( BioMaterial bm ) {
        this.id = bm.getId();
        this.name = bm.getName();
        this.description = bm.getDescription();
        this.characteristics = getCharacteristicString( bm.getCharacteristics() );

        this.factors = new HashMap<String, String>();
        this.factorValues = new HashMap<String, String>();
        this.factorIdToFactorValueId = new HashMap<String, String>();
        for ( FactorValue fv : bm.getFactorValues() ) {
            this.factorValueObjects.add( new FactorValueValueObject( fv ) );
            ExperimentalFactor factor = fv.getExperimentalFactor();
            String factorId = String.format( "factor%d", factor.getId() );
            String factorValueId = String.format( "fv%d", fv.getId() );
            this.factors.put( factorId, getExperimentalFactorString( factor ) );
            this.factorValues.put( factorValueId, getFactorValueString( fv ) );

            if ( fv.getMeasurement() == null ) {
                this.factorIdToFactorValueId.put( factorId, factorValueId );
            } else {
                /*
                 * use the actual value, not the factorvalue id.
                 */
                this.factorIdToFactorValueId.put( factorId, factorValues.get( factorValueId ) );
            }
        }
    }

    public BioMaterialValueObject( BioMaterial bm, BioAssay ba ) {
        this( bm );
        this.bioAssay = new BioAssayValueObject( ba );
        this.assayName = ba.getName();
        this.assayDescription = ba.getDescription();
        this.assayName = ba.getName();
        this.assayDescription = ba.getDescription();
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        BioMaterialValueObject other = ( BioMaterialValueObject ) obj;

        if ( id == null ) {
            if ( other.id != null ) return false;
        } else if ( !id.equals( other.id ) )
            return false;
        else
            return id.equals( other.id );

        if ( name == null ) {
            if ( other.name != null ) return false;
        } else if ( !name.equals( other.name ) ) return false;
        return true;
    }

    public String getAssayDescription() {
        return assayDescription;
    }

    public String getAssayName() {
        return assayName;
    }

    public BioAssayValueObject getBioAssay() {
        return bioAssay;
    }

    public String getCharacteristics() {
        return characteristics;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getFactorIdToFactorValueId() {
        return factorIdToFactorValueId;
    }

    public Map<String, String> getFactors() {
        return factors;
    }

    public Collection<FactorValueValueObject> getFactorValueObjects() {
        return factorValueObjects;
    }

    public Map<String, String> getFactorValues() {
        return factorValues;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );

        if ( id == null ) result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
        return result;
    }

    public void setAssayDescription( String assayDescription ) {
        this.assayDescription = assayDescription;
    }

    public void setAssayName( String assayName ) {
        this.assayName = assayName;
    }

    public void setBioAssay( BioAssayValueObject bioAssay ) {
        this.bioAssay = bioAssay;
    }

    public void setCharacteristics( String characteristics ) {
        this.characteristics = characteristics;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public void setFactorIdToFactorValueId( Map<String, String> factorIdToFactorValueId ) {
        this.factorIdToFactorValueId = factorIdToFactorValueId;
    }

    public void setFactors( Map<String, String> factors ) {
        this.factors = factors;
    }

    public void setFactorValueObjects( Collection<FactorValueValueObject> factorValueObjects ) {
        this.factorValueObjects = factorValueObjects;
    }

    public void setFactorValues( Map<String, String> factorValues ) {
        this.factorValues = factorValues;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setName( String name ) {
        this.name = name;
    }

    /**
     * @param characters
     * @return
     */
    private String getCharacteristicString( Collection<Characteristic> characters ) {
        return StringUtils.join( characters, "," );
    }

    /**
     * @param factor
     * @return
     */
    private String getExperimentalFactorString( ExperimentalFactor factor ) {
        return factor.getName();
    }

    /**
     * Format the value as a string, either using the characteristic, value or measurement.
     * 
     * @param value
     * @return
     */
    private String getFactorValueString( FactorValue value ) {
        if ( !value.getCharacteristics().isEmpty() ) {
            return getCharacteristicString( value.getCharacteristics() );
        } else if ( value.getMeasurement() != null ) {
            return value.getMeasurement().getValue();
        } else {
            return value.getValue();
        }
    }

}
