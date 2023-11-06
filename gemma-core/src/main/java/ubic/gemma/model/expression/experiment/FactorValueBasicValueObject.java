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
import org.hibernate.Hibernate;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.common.measurement.MeasurementValueObject;

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

    /**
     * The ID of the experimental factor this factor value belongs to.
     */
    private Long experimentalFactorId;

    /**
     * The experiment factor category.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private CharacteristicValueObject experimentalFactorCategory;

    /**
     * Indicate if this factor value is a measurement.
     * <p>
     * Note that measurements can have characteristics as optional annotations.
     * @deprecated simply check if {@link #getMeasurement()} is non-null instead
     */
    @Deprecated
    private boolean isMeasurement;

    /**
     * The measurement associated with this factor value.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MeasurementValueObject measurement;

    /**
     * The characteristics associated with this factor value.
     */
    private List<CharacteristicValueObject> characteristics;

    /**
     * The statements associated with this factor value.
     */
    private List<StatementValueObject> statements;

    /**
     * @deprecated use either {@link #characteristics} or {@link #measurement}
     */
    @Deprecated
    private String value;

    /**
     * Human-readable summary of the factor value.
     */
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

        if ( Hibernate.isInitialized( fv.getExperimentalFactor() ) ) {
            if ( fv.getExperimentalFactor().getCategory() != null ) {
                this.experimentalFactorCategory = new CharacteristicValueObject( fv.getExperimentalFactor().getCategory() );
            }
        }

        if ( fv.getMeasurement() != null ) {
            this.measurement = new MeasurementValueObject( fv.getMeasurement() );
            this.isMeasurement = true;
        }

        this.characteristics = fv.getCharacteristics().stream()
                .sorted()
                .map( CharacteristicValueObject::new )
                .collect( Collectors.toList() );

        this.statements = fv.getCharacteristics().stream()
                .sorted()
                .map( StatementValueObject::new )
                .collect( Collectors.toList() );

        this.value = fv.getValue();
        this.summary = FactorValueUtils.getSummaryString( fv );
    }

    @Override
    public String toString() {
        return "FactorValueValueObject [factor=" + summary + ", value=" + value + "]";
    }
}
