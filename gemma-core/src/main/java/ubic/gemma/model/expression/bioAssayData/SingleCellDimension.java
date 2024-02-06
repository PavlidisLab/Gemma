package ubic.gemma.model.expression.bioAssayData;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.persistence.hibernate.CompressedStringListType;
import ubic.gemma.persistence.hibernate.IntArrayType;

import javax.annotation.Nullable;
import javax.persistence.Transient;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private List<String> cellIds;

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
    private Integer numberOfCells;

    /**
     * Cell types, or null if unknown.
     * <p>
     * Those are user-supplied cell type identifiers. Its size must be equal to that of {@link #cellIds}.
     * <p>
     * This is stored as a compressed, gzipped blob in the database. See {@link CompressedStringListType} for more details.
     */
    @Nullable
    private List<String> cellTypes;

    /**
     * Number of cell types.
     * <p>
     * This must always be equal to number of distinct elements of {@link #cellTypes}.
     */
    @Nullable
    private Integer numberOfCellTypes;

    /**
     * List of bioassays that each cell belongs to.
     * <p>
     * The {@link BioAssay} {@code bioAssays[i]} applies to all the cells in the interval {@code [bioAssaysOffset[i], bioAssaysOffset[i+1][}.
     * To find the bioassay type of a given cell, use {@link #getBioAssay(int)}.
     */
    private List<BioAssay> bioAssays;

    /**
     * Offsets of the bioassays.
     * <p>
     * This always contain {@code bioAssays.size()} elements.
     * <p>
     * This is stored in the database using {@link IntArrayType}.
     */
    private int[] bioAssaysOffset;

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
        if ( cellIdToIndex == null ) {
            cellIdToIndex = ListUtils.indexOfElements( cellIds );
        }
        Integer index = cellIdToIndex.get( cellId );
        if ( index == null ) {
            throw new IllegalArgumentException( "Cell ID not found: " + cellId );
        }
        return getBioAssay( index );
    }

    @Override
    public int hashCode() {
        if ( id != null ) {
            return Objects.hash( id );
        }
        // no need to hash numberOfCells, it's derived from cellIds's size
        return Objects.hash( cellIds, cellTypes, cellTypes, bioAssays, Arrays.hashCode( bioAssaysOffset ) );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( !( obj instanceof SingleCellDimension ) )
            return false;
        SingleCellDimension scd = ( SingleCellDimension ) obj;
        if ( id != null && scd.id != null ) {
            return Objects.equals( id, scd.id );
        }
        if ( id != null && ( ( SingleCellDimension ) obj ).id != null )
            return id.equals( ( ( SingleCellDimension ) obj ).id );
        return Objects.equals( cellTypes, scd.cellTypes )
                && Objects.equals( bioAssays, scd.bioAssays )
                && Objects.equals( cellIds, scd.cellIds );  // this is the most expensive to compare
    }
}
