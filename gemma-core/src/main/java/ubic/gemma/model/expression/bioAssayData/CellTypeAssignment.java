package ubic.gemma.model.expression.bioAssayData;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.common.description.Characteristic;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents the labelling of cell types.
 */
@Getter
@Setter
public class CellTypeAssignment extends Analysis {

    /**
     * A special indicator for {@link #cellTypeIndices} when the cell type is unknown.
     */
    public static final int UNKNOWN_CELL_TYPE = -1;

    /**
     * Indicate if this labelling is the preferred one.
     */
    private boolean preferred;

    /**
     * Cell types assignment to individual cells from the {@link #cellTypes} collections.
     * <p>
     * The value {@code -1} is used to indicate an unknown cell type.
     */
    private int[] cellTypeIndices;

    /**
     * List of cell types.
     */
    private List<Characteristic> cellTypes = new ArrayList<>();

    /**
     * Number of cell types.
     * <p>
     * This must always be equal to number of elements of {@link #cellTypes}.
     */
    private Integer numberOfCellTypes;

    /**
     * Obtain the type assignment of a given cell.
     *
     * @return the type assignment of a given cell, or null if the type was assigne to {@link #UNKNOWN_CELL_TYPE}.
     * @throws IndexOutOfBoundsException if the cell index is out of range or if the value is ousitde the range o
     */
    @Nullable
    public Characteristic getCellType( int cellIndex ) throws IndexOutOfBoundsException {
        int i = cellTypeIndices[cellIndex];
        if ( i == UNKNOWN_CELL_TYPE ) {
            return null;
        } else {
            return cellTypes.get( i );
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash( Arrays.hashCode( cellTypeIndices ), cellTypes );
    }

    @Override
    public boolean equals( Object object ) {
        return super.equals( object );
    }

    @Override
    public String toString() {
        return super.toString()
                + ( cellTypes != null ? " Cell Types=" + cellTypes.stream().map( Characteristic::getValue ).collect( Collectors.joining( ", " ) ) : "" );
    }
}
