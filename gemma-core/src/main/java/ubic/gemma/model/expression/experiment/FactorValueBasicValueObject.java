/*
 * The Gemma project Copyright (c) 2009 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package ubic.gemma.model.expression.experiment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicBasicValueObject;
import ubic.gemma.model.common.measurement.MeasurementValueObject;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Each factorvalue can be associated with multiple characteristics (or with a measurement).
 *
 * @author tesarst
 */
@SuppressWarnings("unused") // Used in json serialization
@Data
@EqualsAndHashCode(of = { "characteristics" }, callSuper = true)
public class FactorValueBasicValueObject extends IdentifiableValueObject<FactorValue> {

    private static final long serialVersionUID = 3378801249808036785L;

    private Long experimentalFactorId;
    private CharacteristicBasicValueObject experimentalFactorCategory;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MeasurementValueObject measurement;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<StatementValueObject> characteristics;
    private boolean isMeasurement;

    /**
     * @deprecated use either {@link #characteristics} or {@link #measurement}
     */
    @Deprecated
    private String value;
    /**
     * @deprecated define your own logic for summarizing a factor value
     */
    @Deprecated
    private String summary;

    /**
     * Required when using the class as a spring bean.
     */
    public FactorValueBasicValueObject() {
        super();
    }

    public FactorValueBasicValueObject( Long id ) {
        super( id );
    }

    public FactorValueBasicValueObject( FactorValue fv ) {
        super( fv );
        this.experimentalFactorId = fv.getExperimentalFactor().getId();

        if ( fv.getExperimentalFactor().getCategory() != null ) {
            this.experimentalFactorCategory = new CharacteristicBasicValueObject( fv.getExperimentalFactor().getCategory() );
        }

        if ( fv.getMeasurement() != null ) {
            this.measurement = new MeasurementValueObject( fv.getMeasurement() );
            this.isMeasurement = true;
        }

        this.characteristics = fv.getCharacteristics().stream()
                .sorted()
                .map( StatementValueObject::new )
                .collect( Collectors.toList() );

        this.value = fv.getValue();
        this.summary = getSummaryString( fv );
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
        if ( !fv.getCharacteristics().isEmpty() ) {
            for ( Iterator<Statement> iter = fv.getCharacteristics().iterator(); iter.hasNext(); ) {
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
