package ubic.gemma.model.expression.bioAssayData;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.persistence.hibernate.CompressedStringListType;
import ubic.gemma.persistence.hibernate.IntArrayType;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.util.Arrays.binarySearch;

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
    List<String> cellIds;

    /**
     * Number of cells.
     * <p>
     * This should always be equal to the size of {@link #cellIds}.
     */
    Integer numberOfCells;

    /**
     * Cell types.
     * <p>
     * Use alongside {@link #cellTypesOffset} to determine the range applicable for the cell type.
     * <p>
     * The cell type {@code cellTypes[i]} applies to all the cells in the interval {@code [cellTypeOffset[i], cellTypeOffset[i+1][}.
     * To find the cell type of a given cell, use {@link #getCellType(int)}.
     */
    List<Characteristic> cellTypes;

    /**
     * Offsets of cell types.
     * <p>
     * This is stored in the database using {@link IntArrayType}.
     */
    int[] cellTypesOffset;

    /**
     * List of bioassays that each cell belongs to.
     * <p>
     * The {@link BioAssay} {@code bioAssays[i]} applies to all the cells in the interval {@code [bioAssaysOffset[i], bioAssaysOffset[i+1][}.
     * To find the bioassay type of a given cell, use {@link #getBioAssay(int)}.
     */
    List<BioAssay> bioAssays;

    /**
     * Offsets of the bioassays.
     * <p>
     * This always contain {@code bioAssays.size() + 1} elements.
     * <p>
     * This is stored in the database using {@link IntArrayType}.
     */
    int[] bioAssaysOffset;

    /**
     * Obtain the {@link BioAssay} for a given position.
     */
    public BioAssay getBioAssay( int index ) {
        return getArrayElement( bioAssays, bioAssaysOffset, index );
    }

    /**
     * Obtain the cell type of a given cell in the vector.
     */
    public Characteristic getCellType( int index ) {
        return getArrayElement( cellTypes, cellTypesOffset, index );
    }

    @Override
    public int hashCode() {
        if ( id != null ) {
            return Objects.hash( id );
        }
        // no need to hash numberOfCells, it's derived from cellIds's size
        return Objects.hash( cellIds, cellTypes, Arrays.hashCode( cellTypesOffset ), bioAssays, Arrays.hashCode( bioAssaysOffset ) );
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

    /**
     * Get an element of a sparse range array.
     */
    private <T> T getArrayElement( List<T> array, int[] offsets, int index ) {
        Assert.isTrue( index >= 0 && index < offsets.length, "Index out of range" );
        Assert.isTrue( array.size() == offsets.length, "Invalid size for bioAssaysOffset, it must contain N+1 indices." );
        int offset = binarySearch( offsets, index );
        if ( offset < 0 ) {
            return array.get( -offset - 1 );
        }
        return array.get( offset );
    }
}
