package ubic.gemma.tasks.analysis.expression;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.tasks.maintenance.ExpressionExperimentReportTaskCommand;

/**
 *
 *
 */
public class BatchInfoFetchTaskCommand extends ExpressionExperimentReportTaskCommand {
    public BatchInfoFetchTaskCommand( ExpressionExperiment expressionExperiment ) {
        super( expressionExperiment );
        this.setMaxRuntime( 15 );
    }

    @Override
    public Class getTaskClass() {
        return BatchInfoFetchTask.class;
    }
}
