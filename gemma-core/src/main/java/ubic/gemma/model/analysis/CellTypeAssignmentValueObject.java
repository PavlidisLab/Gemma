package ubic.gemma.model.analysis;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author poirigui
 */
@Data
@EqualsAndHashCode(callSuper = true)
@CommonsLog
public class CellTypeAssignmentValueObject extends AnalysisValueObject<CellTypeAssignment> {

    /**
     * A set of cell types that are assigned to individual cells.
     */
    private Set<CharacteristicValueObject> cellTypes;

    /**
     * A list of IDs, one-per-cell, that refers to one of the cell type labels in {@link #cellTypes}.
     * <p>
     * {@code null} is used to indicate an unknown cell type.
     */
    private List<Long> cellTypeIds;

    /**
     * Indicate if this assignment is the preferred one.
     */
    private boolean isPreferred;

    public CellTypeAssignmentValueObject( CellTypeAssignment cellTypeAssignment ) {
        super( cellTypeAssignment );
        cellTypes = cellTypeAssignment.getCellTypes().stream()
                .map( CharacteristicValueObject::new )
                .collect( Collectors.toSet() );
        try {
            cellTypeIds = Arrays.stream( cellTypeAssignment.getCellTypeIndices() )
                    .mapToObj( i -> i != -1 ? requireNonNull( cellTypeAssignment.getCellTypes().get( i ).getId() ) : null )
                    .collect( Collectors.toList() );
        } catch ( IndexOutOfBoundsException e ) {
            // this may happen because getCellType() can fail if the data we have is incorrect, but we don't want to
            // break the VO serialization which would break the REST API.
            log.warn( "Cell type indices are invalid for " + cellTypeAssignment + "." );
        }
        isPreferred = cellTypeAssignment.isPreferred();
    }
}
