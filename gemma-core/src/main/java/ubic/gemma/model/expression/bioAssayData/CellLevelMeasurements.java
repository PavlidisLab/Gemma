package ubic.gemma.model.expression.bioAssayData;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.common.AbstractDescribable;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.measurement.MeasurementKind;
import ubic.gemma.model.common.measurement.MeasurementType;
import ubic.gemma.model.common.measurement.Unit;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;

import javax.annotation.Nullable;
import javax.persistence.Transient;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Objects;

import static ubic.gemma.persistence.util.ByteArrayUtils.*;

@Getter
@Setter
public class CellLevelMeasurements extends AbstractDescribable {

    public static final Comparator<CellLevelMeasurements> COMPARATOR = Comparator
            .comparing( CellLevelMeasurements::getName, Comparator.nullsLast( Comparator.naturalOrder() ) )
            .thenComparing( CellLevelMeasurements::getCategory, Comparator.nullsLast( Comparator.naturalOrder() ) )
            .thenComparing( CellLevelMeasurements::getId, Comparator.nullsLast( Comparator.naturalOrder() ) );

    private Characteristic category;

    private byte[] data;

    private MeasurementType type;

    @Nullable
    private MeasurementKind kindCV;

    @Nullable
    private String otherKind;

    private PrimitiveType representation;

    @Nullable
    private Unit unit;

    @Override
    @Nullable
    public String getName() {
        return super.getName();
    }

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
    public float[] getDataAsFloats() {
        ensureRepresentation( PrimitiveType.FLOAT );
        return byteArrayToFloats( data );
    }

    public void setDataAsFloats( float[] data ) {
        ensureRepresentation( PrimitiveType.FLOAT );
        setData( floatArrayToBytes( data ) );
    }

    @Transient
    public long[] getDataAsLongs() {
        ensureRepresentation( PrimitiveType.LONG );
        return byteArrayToLongs( data );
    }

    public void setDataAsLongs( long[] data ) {
        ensureRepresentation( PrimitiveType.LONG );
        setData( longArrayToBytes( data ) );
    }

    @Transient
    public int[] getDataAsInts() {
        ensureRepresentation( PrimitiveType.INT );
        return byteArrayToInts( data );
    }

    public void setDataAsInts( int[] data ) {
        ensureRepresentation( PrimitiveType.INT );
        setData( intArrayToBytes( data ) );
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
    public BitSet getDataAsBitSet() {
        ensureRepresentation( PrimitiveType.BITSET );
        return BitSet.valueOf( data );
    }

    public void setDataAsBitSet( BitSet data ) {
        ensureRepresentation( PrimitiveType.BITSET );
        setData( data.toByteArray() );
    }

    @Override
    public int hashCode() {
        return Objects.hash( super.hashCode(), category, representation, type, unit, otherKind, kindCV, unit );
    }

    @Override
    public boolean equals( Object object ) {
        if ( object == this ) {
            return true;
        }
        if ( !( object instanceof CellLevelMeasurements ) ) {
            return false;
        }
        CellLevelMeasurements other = ( CellLevelMeasurements ) object;
        if ( getId() != null && other.getId() != null )
            return getId().equals( other.getId() );
        return Objects.equals( getType(), other.getType() )
                && Objects.equals( getRepresentation(), other.getRepresentation() )
                && Objects.equals( getKindCV(), other.getKindCV() )
                && Objects.equals( getOtherKind(), other.getOtherKind() )
                && Objects.equals( getUnit(), other.getUnit() )
                && Arrays.equals( getData(), other.getData() );
    }

    @Override
    public String toString() {
        return super.toString()
                + ( category != null ? " Category=" + category : "" )
                + ( type != null ? " Type=" + type : "" )
                + ( representation != null ? " Representation=" + representation : "" )
                + ( unit != null ? " Unit=" + unit : "" );
    }

    private void ensureRepresentation( PrimitiveType primitiveType ) {
        if ( representation != primitiveType ) {
            throw new IllegalStateException( String.format( "This vector stores data of type %s, but %s was requested.",
                    representation, primitiveType ) );
        }
    }

    public static class Factory {
        public static CellLevelMeasurements newInstance( Category category ) {
            CellLevelMeasurements clm = new CellLevelMeasurements();
            clm.setCategory( Characteristic.Factory.newInstance( category ) );
            return clm;
        }
    }
}
