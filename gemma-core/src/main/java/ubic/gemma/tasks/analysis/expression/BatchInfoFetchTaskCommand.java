package ubic.gemma.tasks.analysis.expression;

import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.tasks.Task;
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
    public Class<? extends Task<TaskResult, ? extends TaskCommand>>  getTaskClass() {
        return BatchInfoFetchTask.class;
    }
}
