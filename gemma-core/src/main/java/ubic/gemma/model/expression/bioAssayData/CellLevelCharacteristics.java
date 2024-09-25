package ubic.gemma.model.expression.bioAssayData;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Characteristics applicable to individual cells in a {@link SingleCellDimension}.
 * @author poirigui
 */
public interface CellLevelCharacteristics extends Identifiable {

    /**
     * Indicator for an unknown characteristic.
     */
    int UNKNOWN_CHARACTERISTIC = -1;

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
     * List of characteristic.
     */
    List<Characteristic> getCharacteristics();

    @Nullable
    Characteristic getCharacteristic( int cellIndex );
}
