package ubic.gemma.core.apps;

import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.loader.expression.DataDeleterService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;

/**
 * @author poirigui
 */
public class SingleCellDataDeleterCli extends ExpressionExperimentVectorsManipulatingCli<SingleCellExpressionDataVector> {

    @Autowired
    private DataDeleterService dataDeleterService;

    public SingleCellDataDeleterCli() {
        super( SingleCellExpressionDataVector.class );
    }

    @Nullable
    @Override
    public String getCommandName() {
        return "deleteSingleCellData";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Delete single cell data and any related data files";
    }

    @Override
    protected void processExpressionExperimentVectors( ExpressionExperiment ee, QuantitationType qt ) {
        dataDeleterService.deleteSingleCellData( ee, qt );
    }
}
