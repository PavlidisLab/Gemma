package ubic.gemma.model.expression.bioAssayData;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Characteristics applicable to individual cells in a {@link SingleCellDimension}.
 * @author poirigui
 * @see CellTypeAssignment
 * @see GenericCellLevelCharacteristics
 */
public interface CellLevelCharacteristics extends Identifiable {

    /**
     * Indicator for an unknown characteristic.
     */
    int UNKNOWN_CHARACTERISTIC = -1;

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

    @Nullable
    Characteristic getCharacteristic( int cellIndex );

    class Factory {

        public static CellLevelCharacteristics newInstance( List<Characteristic> characteristics, int[] indices ) {
            GenericCellLevelCharacteristics ret = new GenericCellLevelCharacteristics();
            ret.setCharacteristics( characteristics );
            ret.setNumberOfCharacteristics( characteristics.size() );
            ret.setIndices( indices );
            return ret;
        }
    }
}
