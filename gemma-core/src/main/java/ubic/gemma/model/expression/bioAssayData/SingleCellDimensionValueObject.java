package ubic.gemma.model.expression.bioAssayData;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.model.util.ModelUtils;
import ubic.gemma.model.analysis.CellTypeAssignmentValueObject;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.util.UninitializedList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Value object for a single-cell dimension.
 * <p>
 * {@link BioAssay}s are unpacked into a list of IDs. This is suitable because this object is displayed in the context
 * of an {@link ExpressionExperimentValueObject} and its associated {@link BioAssayValueObject}.
 *
 * @author poirigui
 */
@Data
@EqualsAndHashCode(callSuper = true)
@CommonsLog
public class SingleCellDimensionValueObject extends IdentifiableValueObject<SingleCellDimension> {

    /**
     * Cell identifiers.
     * <p>
     * This may be null if cell IDs are explicitly omitted (i.e. {@link ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao#getPreferredSingleCellDimensionWithoutCellIds(ExpressionExperiment)}),
     * in which case it will not be serialized in JSON.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> cellIds;

    /**
     * Number of cells.
     * <p>
     * This is always equal to the length of {@link #cellIds}.
     */
    private int numberOfCells;

    /**
     * A list of {@link ubic.gemma.model.expression.bioAssay.BioAssay} IDs that are applicable to the cells.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Long> bioAssayIds;

    /**
     * All the cell type assignments.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Set<CellTypeAssignmentValueObject> cellTypeAssignments;

    /**
     * All the other cell-level characteristics.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Set<CellLevelCharacteristicsValueObject> cellLevelCharacteristics;

    public SingleCellDimensionValueObject( SingleCellDimension singleCellDimension, boolean excludeBioAssayIds, boolean excludeCellTypeIds, boolean excludeCharacteristicIds ) {
        super( singleCellDimension );
        if ( singleCellDimension.getCellIds() instanceof UninitializedList ) {
            this.cellIds = null;
        } else {
            this.cellIds = singleCellDimension.getCellIds();
        }
        this.numberOfCells = singleCellDimension.getNumberOfCells();
        if ( !excludeBioAssayIds ) {
            this.bioAssayIds = new ArrayList<>( singleCellDimension.getNumberOfCells() );
            try {
                for ( int i = 0; i < singleCellDimension.getNumberOfCells(); i++ ) {
                    this.bioAssayIds.add( requireNonNull( singleCellDimension.getBioAssay( i ).getId() ) );
                }
            } catch ( IllegalArgumentException | IndexOutOfBoundsException e ) {
                log.warn( "The bioassays sparse range array is invalid for " + singleCellDimension, e );
            }
        }
        if ( ModelUtils.isInitialized( singleCellDimension.getCellTypeAssignments() ) ) {
            this.cellTypeAssignments = singleCellDimension.getCellTypeAssignments().stream()
                    .map( cellTypeAssignment -> new CellTypeAssignmentValueObject( cellTypeAssignment, excludeCellTypeIds ) )
                    .collect( Collectors.toSet() );
        }
        if ( ModelUtils.isInitialized( singleCellDimension.getCellLevelCharacteristics() ) ) {
            this.cellLevelCharacteristics = singleCellDimension.getCellLevelCharacteristics().stream()
                    .map( ( CellLevelCharacteristics clc ) -> new CellLevelCharacteristicsValueObject( clc, excludeCharacteristicIds ) )
                    .collect( Collectors.toSet() );
        }
    }
}
