package ubic.gemma.tasks.maintenance;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.job.TaskResult;
import ubic.gemma.tasks.AbstractTask;

@Component
@Scope("prototype")
public class ExpressionExperimentReportTaskImpl extends AbstractTask<TaskResult, ExpressionExperimentReportTaskCommand> implements ExpressionExperimentReportTask {

    private Log log = LogFactory.getLog( ExpressionExperimentReportTask.class.getName() );

    @Autowired
    private ExpressionExperimentReportService expressionExperimentReportService;

    @Override
    public TaskResult execute() {
        TaskResult result = new TaskResult( taskCommand, null );

        if ( taskCommand.doAll() ) {
            expressionExperimentReportService.generateSummaryObjects();
        } else if ( taskCommand.getExpressionExperiment() != null ) {
            expressionExperimentReportService.generateSummary( taskCommand.getExpressionExperiment().getId() );
        } else {
            log.warn( "TaskCommand was not valid, nothing being done" );
        }

        return result;
    }
}
