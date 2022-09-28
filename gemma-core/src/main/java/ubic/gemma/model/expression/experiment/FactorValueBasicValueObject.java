/*
 * The Gemma project Copyright (c) 2009 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package ubic.gemma.model.expression.experiment;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.measurement.MeasurementValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicBasicValueObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;

/**
 * Each factorvalue can be associated with multiple characteristics (or with a measurement).
 *
 * @author tesarst
 */
@SuppressWarnings("unused") // Used in json serialization
@Data
@EqualsAndHashCode(of = { "characteristics" }, callSuper = true)
public class FactorValueBasicValueObject extends IdentifiableValueObject<FactorValue> implements Serializable {

    private static final long serialVersionUID = 3378801249808036785L;

    private boolean isBaseline;
    private Collection<CharacteristicBasicValueObject> characteristics;
    private CharacteristicBasicValueObject experimentalFactorCategory;
    private MeasurementValueObject measurement;
    private String value;
    private String summary;
    private Long experimentalFactorId;

    /**
     * Required when using the class as a spring bean.
     */
    public FactorValueBasicValueObject() {
    }

    public FactorValueBasicValueObject( Long id ) {
        super( id );
    }

    public FactorValueBasicValueObject( FactorValue fv ) {
        super( fv.getId() );
        this.summary = getSummaryString( fv );
        this.experimentalFactorId = fv.getExperimentalFactor().getId();
        this.isBaseline = fv.getIsBaseline() != null ? fv.getIsBaseline() : false;

        if ( fv.getMeasurement() != null ) {
            this.measurement = new MeasurementValueObject( fv.getMeasurement() );
        }
        this.value = fv.getValue();

        if ( fv.getCharacteristics() != null ) {
            this.characteristics = new ArrayList<>( fv.getCharacteristics().size() );
            for ( Characteristic c : fv.getCharacteristics() ) {
                this.characteristics.add( new CharacteristicBasicValueObject( c ) );
            }
        }

        if ( fv.getExperimentalFactor().getCategory() != null ) {
            this.experimentalFactorCategory = new CharacteristicBasicValueObject( fv.getExperimentalFactor().getCategory() );
        }
    }

    @Override
    public String toString() {
        return "FactorValueValueObject [factor=" + summary + ", value=" + value + "]";
    }

    // causes a conflict with getMeasurement...
//    public Boolean isMeasurement() {
//        return this.measurement != null;
//    }

    static String getSummaryString( FactorValue fv ) {
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
}
