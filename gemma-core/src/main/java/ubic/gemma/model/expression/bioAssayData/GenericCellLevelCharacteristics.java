package ubic.gemma.model.expression.bioAssayData;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.annotations.MayBeUninitialized;
import ubic.gemma.model.common.AbstractDescribable;
import ubic.gemma.model.common.description.Characteristic;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Generic cell-level characteristics.
 * <p>
 * For cell types, use {@link CellTypeAssignment} instead.
 * <p>
 * This is not meant to be used directly, prefer {@link CellLevelCharacteristics.Factory#newInstance} for creating
 * cell-level characteristics or {@link CellTypeAssignment} for cell types.
 * @author poirigui
 */
@Getter
@Setter
public class GenericCellLevelCharacteristics extends AbstractDescribable implements CellLevelCharacteristics {

    @MayBeUninitialized
    private List<Characteristic> characteristics;

    private int numberOfCharacteristics;

    private int[] indices;

    @Nullable
    private Integer numberOfAssignedCells;

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
        return Objects.hash( characteristics, Arrays.hashCode( indices ) );
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
        return Objects.equals( getName(), that.getName() )
                && Objects.equals( characteristics, that.characteristics )
                && Arrays.equals( indices, that.indices );
    }

    @Override
    public String toString() {
        return super.toString()
                + ( characteristics != null ? " Characteristics=" + characteristics.stream().map( Characteristic::getValue ).collect( Collectors.joining( ", " ) ) : "" )
                + ( " Number of characteristics=" + numberOfCharacteristics )
                + ( numberOfAssignedCells != null ? " Number of assigned cells=" + numberOfAssignedCells : "" );
    }
}
