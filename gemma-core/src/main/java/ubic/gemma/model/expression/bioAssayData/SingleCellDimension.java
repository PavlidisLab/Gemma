package ubic.gemma.model.expression.bioAssayData;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.persistence.hibernate.ByteArrayType;
import ubic.gemma.persistence.hibernate.CompressedStringListType;

import java.util.*;

import static ubic.gemma.core.util.ListUtils.getSparseRangeArrayElement;

/**
 * Represents a single-cell dimension, holding shared information for a set of {@link SingleCellExpressionDataVector}.
 *
 * @author poirigui
 * @see SingleCellExpressionDataVector
 */
@Getter
@Setter
public class SingleCellDimension implements Identifiable {

    private Long id;

    /**
     * Cell identifiers.
     * <p>
     * Those are user-supplied cell identifiers. Each cell from a given {@link BioAssay} must be assigned a unique id.
     * <p>
     * This is stored as a compressed, gzipped blob in the database. See {@link CompressedStringListType} for more details.
     */
    private List<String> cellIds = new ArrayList<>();

    /**
     * Number of cells.
     * <p>
     * This must always be equal to the size of {@link #cellIds}.
     */
    private int numberOfCells = 0;

    /**
     * Set of cell types assignment to individual cells. This is empty if no cell types have been assigned and should
     * always contain a preferred labelling as per {@link CellTypeAssignment#isPreferred()} if non-empty.
     */
    private Set<CellTypeAssignment> cellTypeAssignments = new HashSet<>();

    /**
     * List of {@link BioAssay}s applicable to the cells.
     * <p>
     * The {@link BioAssay} in {@code bioAssays[sampleIndex]} applies to all the cells in the interval {@code [bioAssaysOffset[sampleIndex], bioAssaysOffset[sampleIndex+1][}.
     * To find the bioassay of a given cell, use {@link #getBioAssay(int)}.
     */
    private List<BioAssay> bioAssays = new ArrayList<>();

    /**
     * Offsets of the bioassays.
     * <p>
     * This must always contain {@code bioAssays.size()} elements.
     * <p>
     * This is stored in the database using {@link ByteArrayType}.
     */
    private int[] bioAssaysOffset = new int[0];

    /**
     * Obtain the {@link BioAssay} for a given cell position.
     *
     * @param cellIndex the cell position in {@link #cellIds}
     * @throws IllegalArgumentException  if the sparse range array is invalid as per {@link ubic.gemma.core.util.ListUtils#getSparseRangeArrayElement(List, int[], int, int)}
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public BioAssay getBioAssay( int cellIndex ) throws IndexOutOfBoundsException {
        return getSparseRangeArrayElement( bioAssays, bioAssaysOffset, cellIds.size(), cellIndex );
    }

    /**
     * Obtain a list of cell IDs for the given sample.
     *
     * @param sampleIndex the sample position in {@link #bioAssays}
     */
    public List<String> getCellIdsBySample( int sampleIndex ) {
        return Collections.unmodifiableList( cellIds.subList( bioAssaysOffset[sampleIndex], bioAssaysOffset[sampleIndex] + getNumberOfCellsBySample( sampleIndex ) ) );
    }

    /**
     * Obtain the number for cells for the given sample.
     * <p>
     * This is more efficient than looking up the size of {@link #getCellIdsBySample(int)}.
     *
     * @param sampleIndex the sample position in {@link #bioAssays}
     */
    public int getNumberOfCellsBySample( int sampleIndex ) {
        if ( sampleIndex == bioAssays.size() - 1 ) {
            return cellIds.size() - bioAssaysOffset[sampleIndex];
        } else {
            return bioAssaysOffset[sampleIndex + 1] - bioAssaysOffset[sampleIndex];
        }
    }

    @Override
    public int hashCode() {
        if ( id != null ) {
            return Objects.hash( id );
        }
        // no need to hash numberOfCells, it's derived from cellIds's size
        return Objects.hash( cellIds, bioAssays, Arrays.hashCode( bioAssaysOffset ) );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( !( obj instanceof SingleCellDimension ) )
            return false;
        SingleCellDimension scd = ( SingleCellDimension ) obj;
        if ( id != null && scd.id != null )
            return id.equals( scd.id );
        return Objects.equals( bioAssays, scd.bioAssays )
                && Arrays.equals( bioAssaysOffset, scd.bioAssaysOffset )
                && Objects.equals( cellIds, scd.cellIds );  // this is the most expensive to compare
    }

    @Override
    public String toString() {
        return String.format( "SingleCellDimension %s", id != null ? "Id=" + id : "" );
    }
}