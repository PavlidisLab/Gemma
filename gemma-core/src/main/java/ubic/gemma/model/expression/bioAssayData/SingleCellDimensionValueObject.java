package ubic.gemma.model.expression.bioAssayData;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.analysis.CellTypeAssignmentValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

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
     */
    private List<String> cellIds;

    /**
     * A list of {@link ubic.gemma.model.expression.bioAssay.BioAssay} IDs that are applicable to the cells.
     */
    private List<Long> bioAssayIds;

    /**
     * The preferred cell type assignment.
     */
    @Nullable
    private CellTypeAssignmentValueObject cellTypeAssignment;

    /**
     * @param cellTypeAssignment a featured cell type assignment from {@link SingleCellDimension#getCellTypeAssignments()}
     */
    public SingleCellDimensionValueObject( SingleCellDimension singleCellDimension, @Nullable CellTypeAssignment cellTypeAssignment ) {
        super( singleCellDimension );
        this.cellIds = singleCellDimension.getCellIds();
        this.bioAssayIds = new ArrayList<>( singleCellDimension.getCellIds().size() );
        try {
            for ( int i = 0; i < singleCellDimension.getCellIds().size(); i++ ) {
                this.bioAssayIds.add( singleCellDimension.getBioAssay( i ).getId() );
            }
        } catch ( IllegalArgumentException | IndexOutOfBoundsException e ) {
            log.warn( "The bioassays sparse range array is invalid for " + singleCellDimension, e );
        }
        if ( cellTypeAssignment != null ) {
            this.cellTypeAssignment = new CellTypeAssignmentValueObject( cellTypeAssignment );
        }
    }
}
