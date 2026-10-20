/*
 * The Gemma project Copyright (c) 2009 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package ubic.gemma.model.expression.experiment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Each factorvalue can be associated with multiple characteristics (or with a measurement).
 *
 * @author tesarst
 */
@SuppressWarnings("unused") // Used in json serialization
@Data
@EqualsAndHashCode(callSuper = true)
public class FactorValueBasicValueObject extends AbstractFactorValueValueObject {

    private static final long serialVersionUID = 3378801249808036785L;

    /**
     * @deprecated use either {@link #getCharacteristics()} or {@link #getMeasurementObject()}
     */
    @Deprecated
    @Schema(description = "Use `summary` if you need a human-readable representation of this factor value or lookup the `characteristics` bag.", deprecated = true)
    private String value;

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
        super( fv, true );
        this.value = fv.getValue();
    }

    @Override
    public String toString() {
        return "FactorValueValueObject [factor=" + getSummary() + ", value=" + value + "]";
    }
}
