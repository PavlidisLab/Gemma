package ubic.gemma.model.expression.bioAssayData;

import ubic.gemma.model.common.Describable;
import ubic.gemma.model.common.DescribableUtils;
import ubic.gemma.model.common.description.Characteristic;

import org.springframework.lang.Nullable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Characteristics applicable to individual cells in a {@link SingleCellDimension}.
 * <p>
 * 🛑 <b>Despite the name, this is a grouping of cells, not a value per cell.</b> It holds a short list of labels
 * ({@link #getCharacteristics()}, stored once each in {@code CHARACTERISTIC}) and, for every cell, the position of
 * its label in that list ({@link #getIndices()}, 4 bytes per cell in one blob). A cell type assignment, a QC flag
 * ({@code mito_outlier = true/false}) or a cluster membership fits; the storage stays proportional to the number of
 * labels however many cells there are.
 * <p>
 * A continuous per-cell measurement (a QC metric, a PC score) does not fit: every distinct value becomes its own
 * label, so the label list grows to one entry per cell. One AnnData load did exactly that: GSE244451's unnamed
 * float columns wrote 3,369,548 {@code CHARACTERISTIC} rows, 34% of the table, before the dataset was deleted in
 * 2026-09. Nothing in this model refuses such a list, so check the number of distinct values before creating one.
 *
 * @author poirigui
 * @see CellTypeAssignment
 * @see GenericCellLevelCharacteristics
 */
public interface CellLevelCharacteristics extends Describable {

    Comparator<CellLevelCharacteristics> COMPARATOR = Comparator
            .comparing( CellLevelCharacteristics::getName, DescribableUtils.NAME_COMPARATOR )
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
     * The labels cells are grouped under, one entry per distinct label and not one per cell.
     * <p>
     * A cell refers to one of these through {@link #getIndices()}.
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
     *
     * @return the characteristic or {@code null} if the cell is assigned to {@link #UNKNOWN_CHARACTERISTIC}.
     * @throws IndexOutOfBoundsException if the cell index is out of bounds
     */
    @Nullable
    Characteristic getCharacteristic( int cellIndex ) throws IndexOutOfBoundsException;

    class Factory {

        /**
         * @param characteristics the distinct labels; see the class documentation before passing one per cell
         * @param indices         for each cell, the position of its label in {@code characteristics}, or
         *                        {@link CellLevelCharacteristics#UNKNOWN_CHARACTERISTIC}
         */
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
