package ubic.gemma.apps;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;

import javax.annotation.Nullable;

/**
 * @author poirigui
 */
public class ListQuantitationTypesCli extends ExpressionExperimentVectorsManipulatingCli<DataVector> {

    @Autowired
    private QuantitationTypeService quantitationTypeService;

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
        Class<? extends DataVector> vectorType = quantitationTypeService.getDataVectorType( qt );
        if ( vectorType != null ) {
            getCliContext().getOutputStream().println( "\t\tVector Type: " + vectorType.getSimpleName() );
        }
        if ( StringUtils.isNotBlank( qt.getDescription() ) ) {
            if ( qt.getDescription().contains( "\n" ) ) {
                getCliContext().getOutputStream().println( "\t\tDescription:\n\t\t" + qt.getDescription().replaceAll( "\n", "\n\t\t" ) );
            } else {
                getCliContext().getOutputStream().println( "\t\tDescription: " + qt.getDescription() );
            }
        }
    }
}
