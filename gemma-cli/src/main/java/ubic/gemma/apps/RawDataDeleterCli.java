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
public class RawDataDeleterCli extends ExpressionExperimentVectorsManipulatingCli<RawExpressionDataVector> {

    @Autowired
    private DataDeleterService dataDeleterService;

    public RawDataDeleterCli() {
        super( RawExpressionDataVector.class );
        setDefaultToPreferredQuantitationType();
    }

    @Nullable
    @Override
    public String getCommandName() {
        return "deleteRawData";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Delete raw expression data";
    }

    @Override
    protected void processExpressionExperimentVectors( ExpressionExperiment ee, QuantitationType qt ) {
        dataDeleterService.deleteRawData( ee, qt );
        addSuccessObject( ee, qt, "Deleted raw data." );
    }
}
