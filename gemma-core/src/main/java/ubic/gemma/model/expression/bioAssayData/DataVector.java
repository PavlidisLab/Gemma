/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.expression.bioAssayData;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * An abstract class representing a one-dimensional vector of data about some aspect of an {@link ExpressionExperiment}.
 * @see DesignElementDataVector
 */
@Getter
@Setter
public abstract class DataVector implements Identifiable, Serializable {

    private static final long serialVersionUID = -5823802521832643417L;

    private Long id;
    private ExpressionExperiment expressionExperiment;
    private QuantitationType quantitationType;
    private byte[] data;

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        if ( id != null ) {
            return Objects.hashCode( id );
        }
        return Objects.hash( expressionExperiment, quantitationType, Arrays.hashCode( data ) );
    }

    /**
     * Returns <code>true</code> if the argument is an DataVector instance and all identifiers for this entity equal the
     * identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof DataVector ) ) {
            return false;
        }
        final DataVector that = ( DataVector ) object;
        if ( this.id != null && that.id != null ) {
            return Objects.equals( id, that.id );
        }
        return Objects.equals( expressionExperiment, that.expressionExperiment )
                && Objects.equals( quantitationType, that.quantitationType )
                && Arrays.equals( data, that.data );
    }
}