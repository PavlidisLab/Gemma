package ubic.gemma.model.expression.bioAssayData;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.measurement.MeasurementKind;
import ubic.gemma.model.common.measurement.MeasurementType;
import ubic.gemma.model.common.measurement.Unit;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;

import static ubic.gemma.persistence.util.ByteArrayUtils.booleanArrayToBytes;
import static ubic.gemma.persistence.util.ByteArrayUtils.byteArrayToBooleans;

@Getter
@Setter
public class CellLevelMeasurements extends AbstractIdentifiable {

    private Characteristic category;

    private byte[] data;

    private MeasurementType type;

    private MeasurementKind kindCV;

    private String otherKind;

    private PrimitiveType representation;

    private Unit unit;

    public boolean[] getDataAsBooleans() {
        ensureRepresentation( PrimitiveType.BOOLEAN );
        return byteArrayToBooleans( data );
    }

    public void setDataAsBooleans( boolean[] data ) {
        ensureRepresentation( PrimitiveType.BOOLEAN );
        setData( booleanArrayToBytes( data ) );
    }

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
        return Objects.hash( category, representation, type, unit, otherKind, kindCV, unit );
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
