package ubic.gemma.core.tasks.maintenance;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.job.AbstractTask;

@Component
@Scope("prototype")
public class ExpressionExperimentReportTaskImpl extends AbstractTask<ExpressionExperimentReportTaskCommand>
        implements ExpressionExperimentReportTask {

    private final Log log = LogFactory.getLog( ExpressionExperimentReportTask.class.getName() );

    @Autowired
    private ExpressionExperimentReportService expressionExperimentReportService;

    @Override
    public TaskResult call() {
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
