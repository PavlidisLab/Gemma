package ubic.gemma.model.expression.bioAssayData;

import ubic.gemma.model.common.Describable;
import ubic.gemma.model.common.description.Characteristic;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Characteristics applicable to individual cells in a {@link SingleCellDimension}.
 * @author poirigui
 * @see CellTypeAssignment
 * @see GenericCellLevelCharacteristics
 */
public interface CellLevelCharacteristics extends Describable {

    Comparator<CellLevelCharacteristics> COMPARATOR = Comparator
            .comparing( CellLevelCharacteristics::getName, Comparator.nullsLast( Comparator.naturalOrder() ) )
            .thenComparing( clc -> !clc.getCharacteristics().isEmpty() ? clc.getCharacteristics().iterator().next() : null, Comparator.nullsLast( Comparator.naturalOrder() ) )
            .thenComparing( CellLevelCharacteristics::getId, Comparator.nullsLast( Comparator.naturalOrder() ) );

    /**
     * Indicator for an unknown characteristic.
     */
    int UNKNOWN_CHARACTERISTIC = -1;

    /**
     * {@inheritDoc}
     * <p>
     * The name is not mandatory, but if set it will appear in lieu of the category.
     */
    @Nullable
    @Override
    String getName();

    /**
     * List of characteristic.
     */
    List<Characteristic> getCharacteristics();

    /**
     * The number of characteristics in {@link #getCharacteristics()}.
     */
    int getNumberOfCharacteristics();

    /**
     * Each entry indicate which characteristic from {@link #getCharacteristics()} is applicable for a given cell.
     * <p>
     * {@link #UNKNOWN_CHARACTERISTIC} is used to indicate a missing value for a cell. In this case,
     * {@link #getCharacteristic(int)} returns {@code null}.
     * <p>
     * The size of this array is the number of cells, typically in a {@link SingleCellDimension}.
     */
    int[] getIndices();

    /**
     * Obtain the number of cells assigned with a characteristic.
     * <p>
     * This is equal to the number of entries in {@link #getIndices()} that are not {@link #UNKNOWN_CHARACTERISTIC}.
     * <p>
     * TOOD: switch to a regular {@code int} once existing CLCs have all been back-filled.
     */
    @Nullable
    Integer getNumberOfAssignedCells();

    /**
     * Obtain the characteristic assigned to a given cell.
     * @return the characteristic or {@code null} if the cell is assigned to {@link #UNKNOWN_CHARACTERISTIC}.
     * @throws IndexOutOfBoundsException if the cell index is out of bounds
     */
    @Nullable
    Characteristic getCharacteristic( int cellIndex ) throws IndexOutOfBoundsException;

    class Factory {

        public static CellLevelCharacteristics newInstance( @Nullable String name, @Nullable String description, List<Characteristic> characteristics, int[] indices ) {
            GenericCellLevelCharacteristics ret = new GenericCellLevelCharacteristics();
            ret.setName( name );
            ret.setDescription( description );
            ret.setCharacteristics( characteristics );
            ret.setNumberOfCharacteristics( characteristics.size() );
            ret.setIndices( indices );
            ret.setNumberOfAssignedCells( ( int ) Arrays.stream( indices ).filter( i -> i != UNKNOWN_CHARACTERISTIC ).count() );
            return ret;
        }
    }
}
