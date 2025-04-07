package ubic.gemma.apps;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
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

    @Override
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        getCliContext().getOutputStream().println( formatExperiment( expressionExperiment ) );
        super.processExpressionExperiment( expressionExperiment );
        getCliContext().getOutputStream().println();
    }

    @Override
    protected void processExpressionExperimentVectors( ExpressionExperiment ee, QuantitationType qt ) {
        getCliContext().getOutputStream().println( "\t" + qt );
        BioAssayDimension dimension = eeService.getBioAssayDimension( ee, qt );
        if ( dimension != null ) {
            getCliContext().getOutputStream().println( "\t\t" + dimension );
        }
    }
}
