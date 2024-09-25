package ubic.gemma.model.expression.bioAssayData;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.common.description.Characteristic;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Generic cell-level characteristics.
 * <p>
 * For cell types, use {@link CellTypeAssignment} instead.
 * @author poirigui
 */
@Getter
@Setter
public class GenericCellLevelCharacteristics extends AbstractIdentifiable implements CellLevelCharacteristics {

    private int[] indices;

    private List<Characteristic> characteristics;

    @Nullable
    @Override
    public Characteristic getCharacteristic( int cellIndex ) {
        int i = indices[cellIndex];
        if ( i == UNKNOWN_CHARACTERISTIC ) {
            return null;
        } else {
            return characteristics.get( i );
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash( Arrays.hashCode( indices ), characteristics );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof GenericCellLevelCharacteristics ) )
            return false;
        GenericCellLevelCharacteristics that = ( GenericCellLevelCharacteristics ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        }
        return Objects.equals( characteristics, that.characteristics )
                && Arrays.equals( indices, that.indices );
    }
}
