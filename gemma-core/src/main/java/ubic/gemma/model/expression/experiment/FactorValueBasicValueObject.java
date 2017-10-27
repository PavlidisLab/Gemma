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
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicBasicValueObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Each factorvalue can be associated with multiple characteristics (or with a measurement).
 *
 * @author tesarst
 */
@SuppressWarnings("unused") // Used in json serialization
public class FactorValueBasicValueObject extends IdentifiableValueObject<FactorValue> implements Serializable {

    private static final long serialVersionUID = 3378801249808036785L;

    private Boolean isBaseline;
    private Collection<CharacteristicBasicValueObject> characteristics;
    private CharacteristicBasicValueObject experimentalFactorCategory;
    private Measurement measurement;
    private String fvValue;
    private String fvSummary;
    private Long experimentalFactorId;

    /**
     * Required when using the class as a spring bean.
     */
    public FactorValueBasicValueObject() {
    }

    public FactorValueBasicValueObject( Long id ) {
        super( id );
    }

    public FactorValueBasicValueObject( FactorValue fv ) { // Used to have an extra argument: Characteristic c;
        super( fv.getId() );
        this.fvSummary = FactorValueValueObject.getSummaryString( fv );
        this.experimentalFactorId = fv.getExperimentalFactor().getId();
        this.isBaseline = fv.getIsBaseline() != null ? fv.getIsBaseline() : false;

        this.measurement = fv.getMeasurement();
        this.fvValue = fv.getValue();

        if ( fv.getCharacteristics() != null ) {
            this.characteristics = new ArrayList<>( fv.getCharacteristics().size() );
            for ( Characteristic c : fv.getCharacteristics() ) {
                this.characteristics.add( new CharacteristicBasicValueObject( c ) );
            }
        }

        this.experimentalFactorCategory = new CharacteristicBasicValueObject(
                fv.getExperimentalFactor().getCategory() );

    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        FactorValueBasicValueObject other = ( FactorValueBasicValueObject ) obj;

        if ( characteristics != null && other.characteristics != null ) {
            return characteristics.equals( other.characteristics ) && id.equals( other.id );
        }
        return characteristics == null && other.characteristics == null && id.equals( other.id );
    }

    @Override
    public String toString() {
        return "FactorValueValueObject [factor=" + fvSummary + ", value=" + fvValue + "]";
    }

    public Boolean getBaseline() {
        return isBaseline;
    }

    public Collection<CharacteristicBasicValueObject> getCharacteristics() {
        return characteristics;
    }

    public Measurement getMeasurement() {
        return measurement;
    }

    public String getFvValue() {
        return fvValue;
    }

    public String getFvSummary() {
        return fvSummary;
    }

    public Long getExperimentalFactorId() {
        return experimentalFactorId;
    }

    public CharacteristicBasicValueObject getExperimentalFactorCategory() {
        return experimentalFactorCategory;
    }
}
