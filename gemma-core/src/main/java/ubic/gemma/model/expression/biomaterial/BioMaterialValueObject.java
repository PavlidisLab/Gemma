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

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author lukem
 */
public class BioMaterialValueObject extends IdentifiableValueObject<BioMaterial> implements Serializable {

    public static final String CHARACTERISTIC_DELIMITER = "::::";
    private static final long serialVersionUID = -145137827948521045L;
    private String assayDescription;
    private String assayName;
    private Collection<BioAssayValueObject> bioAssays = new HashSet<>();
    private String characteristics;
    private String description;
    /**
     * Map of factor ids (factor232) to factor value (id or the actual value) for this biomaterial.
     */
    private Map<String, String> factorIdToFactorValueId;

    /**
     * Map of ids (factor232) to a representation of the factor (e.g., the name).
     */
    private Map<String, String> factors;
    private Collection<FactorValueValueObject> factorValueObjects = new HashSet<>();
    /**
     * Map of ids (fv133) to a representation of the value (for this biomaterial.)
     */
    private Map<String, String> factorValues;
    private String name;



    /**
     * Required when using the class as a spring bean.
     */
    public BioMaterialValueObject() {
    }

    public BioMaterialValueObject( Long id ) {
        super( id );
    }

    public BioMaterialValueObject( BioMaterial bm ) {
        super( bm.getId() );
        this.name = bm.getName();
        this.description = bm.getDescription();
        this.characteristics = getCharacteristicString( bm.getCharacteristics() );

        this.factors = new HashMap<>();
        this.factorValues = new HashMap<>();
        this.factorIdToFactorValueId = new HashMap<>();
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
        BioAssayValueObject baVo = new BioAssayValueObject( ba );
        this.bioAssays.add( baVo );
        this.assayName = ba.getName();
        this.assayDescription = ba.getDescription();
        this.assayName = ba.getName();
        this.assayDescription = ba.getDescription();
    }



    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        BioMaterialValueObject other = ( BioMaterialValueObject ) obj;

        if ( id == null ) {
            if ( other.id != null )
                return false;
        } else
            return id.equals( other.id ) && id.equals( other.id );

        if ( name == null ) {
            if ( other.name != null )
                return false;
        } else if ( !name.equals( other.name ) )
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );

        if ( id == null )
            result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
        return result;
    }



    public String getAssayDescription() {
        return assayDescription;
    }

    public void setAssayDescription( String assayDescription ) {
        this.assayDescription = assayDescription;
    }

    public String getAssayName() {
        return assayName;
    }

    public void setAssayName( String assayName ) {
        this.assayName = assayName;
    }

    public Collection<BioAssayValueObject> getBioAssays() {
        return bioAssays;
    }

    public void setBioAssays( Collection<BioAssayValueObject> bioAssays ) {
        this.bioAssays = bioAssays;
    }

    public String getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics( String characteristics ) {
        this.characteristics = characteristics;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public Map<String, String> getFactorIdToFactorValueId() {
        return factorIdToFactorValueId;
    }

    public void setFactorIdToFactorValueId( Map<String, String> factorIdToFactorValueId ) {
        this.factorIdToFactorValueId = factorIdToFactorValueId;
    }

    public Map<String, String> getFactors() {
        return factors;
    }

    public void setFactors( Map<String, String> factors ) {
        this.factors = factors;
    }

    public Collection<FactorValueValueObject> getFactorValueObjects() {
        return factorValueObjects;
    }

    public void setFactorValueObjects( Collection<FactorValueValueObject> factorValueObjects ) {
        this.factorValueObjects = factorValueObjects;
    }

    public Map<String, String> getFactorValues() {
        return factorValues;
    }

    public void setFactorValues( Map<String, String> factorValues ) {
        this.factorValues = factorValues;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }



    private String getCharacteristicString( Collection<Characteristic> characters ) {
        return StringUtils.join( characters, CHARACTERISTIC_DELIMITER );
    }

    private String getExperimentalFactorString( ExperimentalFactor factor ) {
        return factor.getName();
    }

    /**
     * Format the value as a string, either using the characteristic, value or measurement.
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
