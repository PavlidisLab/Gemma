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

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.nio.*;
import java.util.Arrays;
import java.util.Objects;

import static ubic.gemma.persistence.util.ByteArrayUtils.*;

/**
 * An abstract class representing a one-dimensional vector of data about some aspect of an {@link ExpressionExperiment}.
 * @see DesignElementDataVector
 */
@Getter
@Setter
@MappedSuperclass
public abstract class DataVector extends AbstractIdentifiable {

    // EE is LAZY everywhere (bulk hbm: lazy="proxy"; single-cell hbm: lazy="proxy"); foreign-key
    // name + not-null override happens at the entity level via @AssociationOverride.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EXPRESSION_EXPERIMENT_FK", nullable = false, columnDefinition = "BIGINT")
    private ExpressionExperiment expressionExperiment;

    /**
     * Owning {@link QuantitationType}. Fetch mode varies by concrete subclass — bulk vectors keep
     * the QT LAZY to avoid an N+1 on bulk loads, single-cell vectors fetch eagerly to match the
     * original hbm. Declared as an abstract accessor here so each subclass can configure its own
     * {@code @ManyToOne} fetch policy (which @MappedSuperclass + @AssociationOverride cannot do).
     */
    public abstract QuantitationType getQuantitationType();

    public abstract void setQuantitationType( QuantitationType quantitationType );

    @Lob
    @Column(name = "DATA", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] data;

    /**
     * Obtain the data as a generic {@link Buffer}.
     */
    @Transient
    public Buffer getDataAsBuffer() {
        switch ( getQuantitationType().getRepresentation() ) {
            case FLOAT:
                return getDataAsFloatBuffer();
            case DOUBLE:
                return getDataAsDoubleBuffer();
            case INT:
                return getDataAsIntBuffer();
            case LONG:
                return getDataAsLongBuffer();
            default:
                throw new UnsupportedOperationException( "Cannot create a buffer for data stored in "
                        + getQuantitationType().getRepresentation() + "." );
        }
    }

    @Transient
    public float[] getDataAsFloats() {
        ensureRepresentation( PrimitiveType.FLOAT );
        return byteArrayToFloats( data );
    }

    public void setDataAsFloats( float[] data ) {
        ensureRepresentation( PrimitiveType.FLOAT );
        setData( floatArrayToBytes( data ) );
    }

    public FloatBuffer getDataAsFloatBuffer() {
        ensureRepresentation( PrimitiveType.FLOAT );
        return ByteBuffer.wrap( data ).asFloatBuffer();
    }

    @Transient
    public double[] getDataAsDoubles() {
        ensureRepresentation( PrimitiveType.DOUBLE );
        return byteArrayToDoubles( data );
    }

    /**
     * Obtain the data as a {@link DoubleBuffer}.
     * <p>
     * The underlying data is not copied, so this is the most efficient way to perform arbitrary access or slice parts
     * of the vector.
     */
    public DoubleBuffer getDataAsDoubleBuffer() {
        ensureRepresentation( PrimitiveType.DOUBLE );
        return ByteBuffer.wrap( data ).asDoubleBuffer();
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

    public void setDataAsChars( char[] data ) {
        ensureRepresentation( PrimitiveType.CHAR );
        setData( charArrayToBytes( data ) );
    }

    @Transient
    public int[] getDataAsInts() {
        ensureRepresentation( PrimitiveType.INT );
        return byteArrayToInts( data );
    }

    @Transient
    public IntBuffer getDataAsIntBuffer() {
        ensureRepresentation( PrimitiveType.INT );
        return ByteBuffer.wrap( data ).asIntBuffer();
    }

    public void setDataAsInts( int[] data ) {
        ensureRepresentation( PrimitiveType.INT );
        setData( intArrayToBytes( data ) );
    }

    @Transient
    public long[] getDataAsLongs() {
        ensureRepresentation( PrimitiveType.LONG );
        return byteArrayToLongs( data );
    }

    @Transient
    public LongBuffer getDataAsLongBuffer() {
        ensureRepresentation( PrimitiveType.LONG );
        return ByteBuffer.wrap( data ).asLongBuffer();
    }

    public void setDataAsLongs( long[] data ) {
        ensureRepresentation( PrimitiveType.LONG );
        setData( longArrayToBytes( data ) );
    }

    @Transient
    public String[] getDataAsStrings() {
        ensureRepresentation( PrimitiveType.STRING );
        return byteArrayToStrings( data );
    }

    public void setDataAsStrings( String[] data ) {
        ensureRepresentation( PrimitiveType.STRING );
        setData( stringsToByteArray( data ) );
    }

    @Transient
    public String[] getDataAsTabbedStrings() {
        ensureRepresentation( PrimitiveType.STRING );
        return byteArrayToTabbedStrings( data );
    }

    public void setDataAsTabbedStrings( String[] data ) {
        ensureRepresentation( PrimitiveType.STRING );
        setData( stringsToTabbedBytes( data ) );
    }

    @Transient
    public Object[] getDataAsObjects() {
        return byteArrayToObjects( data, getQuantitationType().getRepresentation().getJavaClass() );
    }

    public void setDataAsObjects( Object[] data ) {
        if ( data.length > 0 ) {
            ensureRepresentation( data[0].getClass() );
            setData( objectArrayToBytes( data ) );
        } else {
            setData( new byte[0] );
        }
    }

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        // also, we cannot hash the ID because it is assigned on creation
        // hashing the data is wasteful because subclasses will have a design element to distinguish distinct vectors
        return Objects.hash( expressionExperiment, getQuantitationType() );
    }

    private void ensureRepresentation( PrimitiveType primitiveType ) {
        if ( getQuantitationType().getRepresentation() != primitiveType ) {
            throw new IllegalStateException( String.format( "This vector stores data of type %s, but %s was requested.",
                    getQuantitationType().getRepresentation(), primitiveType ) );
        }
    }

    private void ensureRepresentation( Class<?> clazz ) {
        if ( !clazz.equals( getQuantitationType().getRepresentation().getJavaClass() ) ) {
            // try to find a primitive type that matches the requested class
            String requestedType = Arrays.stream( PrimitiveType.values() )
                    .filter( p -> clazz.equals( p.getJavaClass() ) )
                    .map( PrimitiveType::toString )
                    .findFirst()
                    .orElse( clazz.getName() );
            throw new IllegalStateException( String.format( "This vector stores data of type %s, but %s was requested.",
                    getQuantitationType().getRepresentation(), requestedType ) );
        }
    }
}
