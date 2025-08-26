package ubic.gemma.apps;

import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.loader.expression.DataDeleterService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;

/**
 * @author poirigui
 */
public class SingleCellDataAggregateDeleterCli extends ExpressionExperimentVectorsManipulatingCli<RawExpressionDataVector> {

    @Autowired
    private DataDeleterService dataDeleterService;

    public SingleCellDataAggregateDeleterCli() {
        super( RawExpressionDataVector.class );
        setSingleExperimentMode();
        setQuantitationTypeIdentifierRequired();
    }

    @Nullable
    @Override
    public String getCommandName() {
        return "deleteSingleCellDataAggregate";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Delete a single-cell data aggregate";
    }

    @Override
    protected void processExpressionExperimentVectors( ExpressionExperiment ee, QuantitationType qt ) {
        dataDeleterService.deleteSingleCellDataAggregate( ee, qt );
        addSuccessObject( ee, qt, "Deleted single-cell data aggregate." );
    }
}
