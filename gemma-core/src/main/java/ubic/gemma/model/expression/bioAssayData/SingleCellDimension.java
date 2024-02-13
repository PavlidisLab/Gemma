package ubic.gemma.model.expression.bioAssayData;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.persistence.hibernate.ByteArrayType;
import ubic.gemma.persistence.hibernate.CompressedStringListType;

import javax.annotation.Nullable;
import javax.persistence.Transient;
import java.util.*;

import static ubic.gemma.core.util.ListUtils.getSparseRangeArrayElement;

@Getter
@Setter
public class SingleCellDimension implements Identifiable {

    private Long id;

    /**
     * Cell identifiers.
     * <p>
     * Those are user-supplied cell identifiers. Each cell must be assigned a unique id.
     * <p>
     * This is stored as a compressed, gzipped blob in the database. See {@link CompressedStringListType} for more details.
     */
    private List<String> cellIds = new ArrayList<>();

    /**
     * An internal collection for mapping cell IDs to their position in {@link #cellIds}.
     */
    @Nullable
    @Transient
    private Map<String, Integer> cellIdToIndex;

    /**
     * Number of cells.
     * <p>
     * This should always be equal to the size of {@link #cellIds}.
     */
    private int numberOfCells = 0;

    /**
     * Set of cell types assignment to individual cells. This is empty if no cell types have been assigned and should
     * always contain a preferred labelling as per {@link CellTypeLabelling#preferred} if non-empty.
     */
    private Set<CellTypeLabelling> cellTypeLabellings = new HashSet<>();

    /**
     * List of bioassays that each cell belongs to.
     * <p>
     * The {@link BioAssay} {@code bioAssays[i]} applies to all the cells in the interval {@code [bioAssaysOffset[i], bioAssaysOffset[i+1][}.
     * To find the bioassay type of a given cell, use {@link #getBioAssay(int)}.
     */
    private List<BioAssay> bioAssays = new ArrayList<>();

    /**
     * Offsets of the bioassays.
     * <p>
     * This always contain {@code bioAssays.size()} elements.
     * <p>
     * This is stored in the database using {@link ByteArrayType}.
     */
    private int[] bioAssaysOffset = new int[0];

    public void setCellIds( List<String> cellIds ) {
        this.cellIds = cellIds;
        // invalidate index cache
        this.cellIdToIndex = null;
    }

    /**
     * Obtain the {@link BioAssay} for a given position.
     */
    public BioAssay getBioAssay( int index ) {
        return getSparseRangeArrayElement( bioAssays, bioAssaysOffset, cellIds.size(), index );
    }

    /**
     * Obtain the {@link BioAssay} for a given cell ID.
     */
    public BioAssay getBioAssayByCellId( String cellId ) {
        return getBioAssay( getCellIndex( cellId ) );
    }

    private int getCellIndex( String cellId ) {
        if ( cellIdToIndex == null ) {
            cellIdToIndex = ListUtils.indexOfElements( cellIds );
        }
        Integer index = cellIdToIndex.get( cellId );
        if ( index == null ) {
            throw new IllegalArgumentException( "Cell ID not found: " + cellId );
        }
        return index;
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
