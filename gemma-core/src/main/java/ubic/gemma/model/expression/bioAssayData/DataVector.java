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

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.Serializable;

/**
 * An abstract class representing a one-dimensional vector of data about some aspect of an {@link ExpressionExperiment}.
 * @see DesignElementDataVector
 */
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
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
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
        return this.id != null && that.getId() != null && this.id.equals( that.getId() );
    }

    @Override
    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public ExpressionExperiment getExpressionExperiment() {
        return expressionExperiment;
    }

    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    public QuantitationType getQuantitationType() {
        return this.quantitationType;
    }

    public void setQuantitationType( QuantitationType quantitationType ) {
        this.quantitationType = quantitationType;
    }

    public byte[] getData() {
        return this.data;
    }

    public void setData( byte[] data ) {
        this.data = data;
    }
}