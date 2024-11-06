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
import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.persistence.Transient;
import java.util.Objects;

import static ubic.gemma.persistence.util.ByteArrayUtils.*;

/**
 * An abstract class representing a one-dimensional vector of data about some aspect of an {@link ExpressionExperiment}.
 * @see DesignElementDataVector
 */
@Getter
@Setter
public abstract class DataVector extends AbstractIdentifiable {

    private ExpressionExperiment expressionExperiment;
    private QuantitationType quantitationType;
    private byte[] data;

    @Transient
    public double[] getDataAsDoubles() {
        ensureRepresentation( PrimitiveType.DOUBLE );
        return byteArrayToDoubles( data );
    }

    public void setDataAsDoubles( double[] data ) {
        ensureRepresentation( PrimitiveType.DOUBLE );
        setData( doubleArrayToBytes( data ) );
    }

    @Transient
    public boolean[] getDataAsBooleans() {
        ensureRepresentation( PrimitiveType.BOOLEAN );
        return byteArrayToBooleans( data );
    }

    public void setDataAsBooleans( boolean[] data ) {
        ensureRepresentation( PrimitiveType.BOOLEAN );
        setData( booleanArrayToBytes( data ) );
    }

    @Transient
    public char[] getDataAsChars() {
        ensureRepresentation( PrimitiveType.CHAR );
        return byteArrayToChars( data );
    }

    @Transient
    public int[] getDataAsInts() {
        ensureRepresentation( PrimitiveType.INT );
        return byteArrayToInts( data );
    }

    @Transient
    public long[] getDataAsLongs() {
        ensureRepresentation( PrimitiveType.LONG );
        return byteArrayToLongs( data );
    }

    @Transient
    public String[] getDataAsStrings() {
        ensureRepresentation( PrimitiveType.STRING );
        return byteArrayToStrings( data );
    }

    @Transient
    public String getDataAsString() {
        ensureRepresentation( PrimitiveType.STRING );
        return byteArrayToAsciiString( data );
    }

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        // also, we cannot hash the ID because it is assigned on creation
        // hashing the data is wasteful because subclasses will have a design element to distinguish distinct vectors
        return Objects.hash( expressionExperiment, quantitationType );
    }

    private void ensureRepresentation( PrimitiveType primitiveType ) {
        if ( quantitationType.getRepresentation() != primitiveType ) {
            throw new IllegalStateException( String.format( "This vector stores data of type %s, but %s was requested.",
                    quantitationType.getRepresentation(), primitiveType ) );
        }
    }
}