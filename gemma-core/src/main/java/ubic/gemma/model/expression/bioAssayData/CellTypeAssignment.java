package ubic.gemma.model.expression.bioAssayData;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.annotations.MayBeUninitialized;
import ubic.gemma.model.common.description.Characteristic;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a cell type assignment where cells from a given dataset are assigned cell types.
 * @author poirigui
 * @see SingleCellDimension
 */
@Getter
@Setter
public class CellTypeAssignment extends Analysis implements CellLevelCharacteristics {

    public static final Comparator<CellTypeAssignment> COMPARATOR = Comparator
            .comparing( CellTypeAssignment::getName, Comparator.nullsLast( Comparator.naturalOrder() ) )
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
    private boolean preferred;

    /**
     * Cell types assignment to individual cells from the {@link #cellTypes} collections.
     * <p>
     * The value {@code -1} is used to indicate an unknown cell type.
     */
    private int[] cellTypeIndices;

    @Nullable
    private Integer numberOfAssignedCells;

    /**
     * List of cell types.
     */
    @MayBeUninitialized
    private List<Characteristic> cellTypes = new ArrayList<>();

    /**
     * Number of cell types.
     * <p>
     * This must always be equal to number of elements of {@link #cellTypes}.
     */
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
    public List<Characteristic> getCharacteristics() {
        return getCellTypes();
    }

    /**
     * @deprecated Use {@link #getNumberOfCellTypes()} instead.
     */
    @Override
    public int getNumberOfCharacteristics() {
        return getNumberOfCellTypes();
    }

    /**
     * @deprecated Use {@link #getCellTypeIndices()} instead.
     */
    @Override
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
        return Objects.hash( super.hashCode(), Arrays.hashCode( cellTypeIndices ), cellTypes );
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
        return Objects.equals( getName(), that.getName() )
                && Objects.equals( cellTypes, that.cellTypes )
                && Arrays.equals( cellTypeIndices, that.cellTypeIndices );
    }

    @Override
    public String toString() {
        return super.toString()
                + ( cellTypes != null ? " Cell Types=" + cellTypes.stream().map( Characteristic::getValue ).collect( Collectors.joining( ", " ) ) : "" )
                + ( " Number of Cell Types=" + numberOfCellTypes )
                + ( numberOfAssignedCells != null ? " Number of Assigned Cells=" + numberOfAssignedCells : null )
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
