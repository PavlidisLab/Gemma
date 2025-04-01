package ubic.gemma.apps;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;

/**
 * @author poirigui
 */
public class ListQuantitationTypesCli extends ExpressionExperimentVectorsManipulatingCli<DataVector> {

    public ListQuantitationTypesCli() {
        super( DataVector.class );
        setUseReferencesIfPossible();
        setDefaultToAll();
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        getCliContext().getOutputStream().println( formatExperiment( expressionExperiment ) );
        super.processExpressionExperiment( expressionExperiment );
    }

    @Override
    protected void processExpressionExperimentVectors( ExpressionExperiment ee, QuantitationType qt ) {
        getCliContext().getOutputStream().println( "\t" + qt );
    }

    @Nullable
    @Override
    public String getCommandName() {
        return "listQuantitationTypes";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "List the available quantitation types for an experiment.";
    }
}
