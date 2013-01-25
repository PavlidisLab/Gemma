package ubic.gemma.tasks.analysis.expression;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.tasks.maintenance.ExpressionExperimentReportTaskCommand;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 10/01/13
 * Time: 10:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class BatchInfoFetchTaskCommand extends ExpressionExperimentReportTaskCommand {
    public BatchInfoFetchTaskCommand( ExpressionExperiment expressionExperiment ) {
        super( expressionExperiment );
    }

    @Override
    public Class getTaskClass() {
        return BatchInfoFetchTask.class;
    }
}
