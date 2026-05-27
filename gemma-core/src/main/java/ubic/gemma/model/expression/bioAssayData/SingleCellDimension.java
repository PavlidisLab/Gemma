package ubic.gemma.model.expression.bioAssayData;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import ubic.gemma.core.datastructure.sparse.SparseListUtils;
import ubic.gemma.model.annotations.MayBeUninitialized;
import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.util.ModelUtils;
import ubic.gemma.persistence.hibernate.ByteArrayType;
import ubic.gemma.persistence.hibernate.CompressedStringListType;

import java.util.*;

import static ubic.gemma.core.datastructure.sparse.SparseListUtils.getSparseRangeArrayElement;

/**
 * Represents a single-cell dimension, holding shared information for a set of {@link SingleCellExpressionDataVector}.
 *
 * @author poirigui
 * @see SingleCellExpressionDataVector
 */
@Entity
@Table(name = "SINGLE_CELL_DIMENSION")
@Immutable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Getter
@Setter
public class SingleCellDimension extends AbstractIdentifiable implements Identifiable {

    /**
     * Cell identifiers.
     * <p>
     * Those are user-supplied cell identifiers. Each cell from a given {@link BioAssay} must be assigned a unique id.
     * <p>
     * This is stored as a compressed, gzipped blob in the database. See {@link CompressedStringListType} for more details.
     */
    // FIXME: the delimiter is not a real newline, but a backslash followed by a n, we would need to rewrite all
    //        the cell IDs we have to fix this, see https://github.com/PavlidisLab/Gemma/issues/1365
    @MayBeUninitialized(hasSize = true)
    @Type(value = CompressedStringListType.class, parameters = @Parameter(name = "delimiter", value = "\\n"))
    @Column(name = "CELL_IDS", nullable = false, columnDefinition = "LONGBLOB")
    private List<String> cellIds = new ArrayList<>();

    /**
     * Number of cell IDs.
     * <p>
     * This is *not* the number of cells
     * <p>
     * This must always be equal to the size of {@link #cellIds}.
     */
    // TODO: rename to NUMBER_OF_CELL_IDS
    @Column(name = "NUMBER_OF_CELLS", nullable = false, columnDefinition = "INTEGER")
    private int numberOfCellIds = 0;

    /**
     * List of {@link BioAssay}s applicable to the cells.
     * <p>
     * The {@link BioAssay} in {@code bioAssays[sampleIndex]} applies to all the cells in the interval
     * {@code [bioAssaysOffset[sampleIndex], bioAssaysOffset[sampleIndex+1][} except for the last sample which owns the
     * remaining cells.
     * <p>
     * To find the {@link BioAssay} of a given cell, use {@link #getBioAssay(int)}.
     */
    @MayBeUninitialized
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "BIO_ASSAYS2SINGLE_CELL_DIMENSIONS",
            joinColumns = @JoinColumn(name = "SINGLE_CELL_DIMENSIONS_FK", columnDefinition = "BIGINT"),
            inverseJoinColumns = @JoinColumn(name = "BIO_ASSAYS_FK", columnDefinition = "BIGINT"),
            foreignKey = @ForeignKey(name = "SINGLE_CELL_DIMENSIONS_FKC"),
            inverseForeignKey = @ForeignKey(name = "BIO_ASSAYS_SC_FKC"))
    @OrderColumn(name = "ORDERING")
    @Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
    private List<BioAssay> bioAssays = new ArrayList<>();

    /**
     * Offsets of the {@link BioAssay} in {@link #cellIds}.
     * <p>
     * This must always contain {@code bioAssays.size()} elements.
     * <p>
     * This is stored in the database using {@link ByteArrayType}.
     * <p>
     * This may be set to {@code null} to keep the model lightweight.
     */
    @Type(value = ByteArrayType.class, parameters = @Parameter(name = "arrayType", value = "int"))
    @Column(name = "BIO_ASSAYS_OFFSET", nullable = false, columnDefinition = "LONGBLOB")
    private int[] bioAssaysOffset = new int[0];

    /**
     * Set of cell types assignment to individual cells.
     * <p>
     * This is empty if no cell types have been assigned and should always contain a preferred assignment as per
     * {@link CellTypeAssignment#isPreferred()} if non-empty.
     */
    @MayBeUninitialized
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "SINGLE_CELL_DIMENSION_FK", columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "SINGLE_CELL_DIMENSION_FKC"))
    private Set<CellTypeAssignment> cellTypeAssignments = new HashSet<>();

    /**
     * Set of cell-level characteristics.
     * <p>
     * Cell types have a special treatment and should be added to {@link #cellTypeAssignments}.
     */
    @MayBeUninitialized
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "SINGLE_CELL_DIMENSION_FK", columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "CELL_LEVEL_CHARACTERISTICS_SINGLE_SINGLE_CELL_DIMENSION_FKC"))
    private Set<CellLevelCharacteristics> cellLevelCharacteristics = new HashSet<>();

    /**
     * Obtain the {@link BioAssay} for a given cell position.
     *
     * @param cellIndex the cell position in {@link #cellIds}
     * @throws IllegalArgumentException  if the sparse range array is invalid as per {@link SparseListUtils#getSparseRangeArrayElement(List, int[], int, int)}
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public BioAssay getBioAssay( int cellIndex ) throws IndexOutOfBoundsException {
        if ( cellIndex < 0 || cellIndex > numberOfCellIds ) {
            throw new IndexOutOfBoundsException( "Cell index must be in the range [0, " + numberOfCellIds + "[." );
        }
        return getSparseRangeArrayElement( bioAssays, bioAssaysOffset, numberOfCellIds, cellIndex );
    }

    /**
     * Obtain a list of cell IDs for the given sample.
     *
     * @param sampleIndex the sample position in {@link #bioAssays}
     */
    public List<String> getCellIdsBySample( int sampleIndex ) {
        if ( sampleIndex < 0 || sampleIndex >= bioAssays.size() ) {
            throw new IndexOutOfBoundsException( "Sample index must be in range [0, " + bioAssays.size() + "[." );
        }
        return Collections.unmodifiableList( cellIds.subList( bioAssaysOffset[sampleIndex], bioAssaysOffset[sampleIndex] + getNumberOfCellIdsBySample( sampleIndex ) ) );
    }

    /**
     * Obtain the number for cells for the given sample.
     * <p>
     * This is more efficient than looking up the size of {@link #getCellIdsBySample(int)}.
     *
     * @param sampleIndex the sample position in {@link #bioAssays}
     */
    public int getNumberOfCellIdsBySample( int sampleIndex ) {
        if ( sampleIndex < 0 || sampleIndex >= bioAssays.size() ) {
            throw new IndexOutOfBoundsException( "Sample index must be in range [0, " + bioAssays.size() + "[." );
        }
        if ( sampleIndex == bioAssays.size() - 1 ) {
            return numberOfCellIds - bioAssaysOffset[sampleIndex];
        } else {
            return bioAssaysOffset[sampleIndex + 1] - bioAssaysOffset[sampleIndex];
        }
    }

    @Override
    public int hashCode() {
        // bioAssays may be uninitialized, so it's not safe to include in the hashcode
        return Objects.hash( numberOfCellIds, Arrays.hashCode( bioAssaysOffset ) );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( !( obj instanceof SingleCellDimension ) )
            return false;
        SingleCellDimension scd = ( SingleCellDimension ) obj;
        if ( getId() != null && scd.getId() != null )
            return getId().equals( scd.getId() );
        // bioAssays might be uninitialized, ignore it when comparing
        return ModelUtils.equals( bioAssays, scd.bioAssays ) == ModelUtils.EqualityOutcome.EQUAL
                && Arrays.equals( bioAssaysOffset, scd.bioAssaysOffset )
                && Objects.equals( cellIds, scd.cellIds );  // this is the most expensive to compare
    }

    @Override
    public String toString() {
        return super.toString()
                + ( bioAssays != null ? " Number of Assays=" + bioAssays.size() : "" )
                + " Number of Cell IDs=" + ( cellIds != null ? cellIds.size() : numberOfCellIds );
    }
}
