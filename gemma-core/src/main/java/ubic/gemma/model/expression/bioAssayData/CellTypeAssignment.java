package ubic.gemma.model.expression.bioAssayData;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.annotations.MayBeUninitialized;
import ubic.gemma.model.common.DescribableUtils;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.util.ModelUtils;
import ubic.gemma.persistence.hibernate.ByteArrayType;

import org.springframework.lang.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a cell type assignment where cells from a given dataset are assigned cell types.
 *
 * @author poirigui
 * @see SingleCellDimension
 */
@Getter
@Setter
@Entity
@DiscriminatorValue("CellTypeAssignment")
public class CellTypeAssignment extends Analysis implements CellLevelCharacteristics {

    public static final Comparator<CellTypeAssignment> COMPARATOR = Comparator
            .comparing( CellTypeAssignment::getName, DescribableUtils.NAME_COMPARATOR )
            .thenComparing( clc -> clc.getProtocol() != null ? clc.getProtocol().getName() : null, Comparator.nullsLast( Comparator.naturalOrder() ) )
            .thenComparing( CellTypeAssignment::getId, Comparator.nullsLast( Comparator.naturalOrder() ) );

    /**
     * A special indicator for {@link #cellTypeIndices} when the cell type is unknown.
     */
    public static final int UNKNOWN_CELL_TYPE = UNKNOWN_CHARACTERISTIC;

    /**
     * Indicate if this assignment is the preferred one.
     * <p>
     * There can only be one preferred cell type assignment for a given {@link SingleCellDimension}.
     */
    // hbm carried not-null="false" because of other Analysis subclasses sharing the table; preserve that.
    @Column(name = "IS_PREFERRED", columnDefinition = "TINYINT")
    private boolean preferred;

    /**
     * Cell types assignment to individual cells from the {@link #cellTypes} collections.
     * <p>
     * The value {@code -1} is used to indicate an unknown cell type.
     */
    @Type(value = ByteArrayType.class, parameters = @Parameter(name = "arrayType", value = "int"))
    @Column(name = "CELL_TYPE_INDICES", columnDefinition = "LONGBLOB")
    private int[] cellTypeIndices;

    @Nullable
    @Column(name = "NUMBER_OF_ASSIGNED_CELLS", columnDefinition = "INTEGER")
    private Integer numberOfAssignedCells;

    /**
     * List of cell types.
     */
    // The cell types live in the CHARACTERISTIC table; spell out the FK + ordering column.
    @MayBeUninitialized
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "CELL_TYPE_ASSIGNMENT_FK", columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "CHARACTERISTIC_CELL_TYPE_ASSIGNMENT_FKC"))
    @OrderColumn(name = "CELL_TYPE_ASSIGNMENT_ORDERING")
    @Immutable
    private List<Characteristic> cellTypes = new ArrayList<>();

    /**
     * Number of cell types.
     * <p>
     * This must always be equal to number of elements of {@link #cellTypes}.
     */
    @Column(name = "NUMBER_OF_CELL_TYPES", columnDefinition = "INTEGER")
    private int numberOfCellTypes;

    /**
     * Obtain the type assignment of a given cell.
     *
     * @return the type assignment of a given cell, or null if the type was assigne to {@link #UNKNOWN_CELL_TYPE}.
     * @throws IndexOutOfBoundsException if the cell index is out of bounds
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

    /**
     * @deprecated Use {@link #getCellTypes()} instead.
     */
    @Override
    @Deprecated
    public List<Characteristic> getCharacteristics() {
        return getCellTypes();
    }

    /**
     * @deprecated Use {@link #getNumberOfCellTypes()} instead.
     */
    @Override
    @Deprecated
    public int getNumberOfCharacteristics() {
        return getNumberOfCellTypes();
    }

    /**
     * @deprecated Use {@link #getCellTypeIndices()} instead.
     */
    @Override
    @Deprecated
    public int[] getIndices() {
        return getCellTypeIndices();
    }

    /**
     * Use {@link #getCellType(int)} instead.
     */
    @Nullable
    @Override
    public Characteristic getCharacteristic( int cellIndex ) {
        return getCellType( cellIndex );
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof CellTypeAssignment ) )
            return false;
        CellTypeAssignment that = ( CellTypeAssignment ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        }
        return DescribableUtils.equalsByName( this, that )
                // cellTypes might be uninitialized, ignore it when comparing
                && ModelUtils.equals( cellTypes, that.cellTypes ) == ModelUtils.EqualityOutcome.EQUAL
                && Arrays.equals( cellTypeIndices, that.cellTypeIndices );
    }

    @Override
    public String toString() {
        return super.toString()
                + ( cellTypes != null && ModelUtils.isInitialized( cellTypes ) ? " Cell Types=" + cellTypes.stream().map( Characteristic::getValue ).collect( Collectors.joining( ", " ) ) : "" )
                + ( " Number of Cell Types=" + numberOfCellTypes )
                + ( numberOfAssignedCells != null ? " Number of Assigned Cells=" + numberOfAssignedCells : "" )
                + ( preferred ? " [Preferred]" : "" );
    }

    public static class Factory {

        public static CellTypeAssignment newInstance( String name, List<Characteristic> characteristics, int[] indices ) {
            CellTypeAssignment cta = new CellTypeAssignment();
            cta.setName( name );
            cta.setCellTypes( characteristics );
            cta.setNumberOfCellTypes( characteristics.size() );
            cta.setCellTypeIndices( indices );
            cta.setNumberOfAssignedCells( ( int ) Arrays.stream( indices ).filter( i -> i != UNKNOWN_CELL_TYPE ).count() );
            return cta;
        }
    }
}
