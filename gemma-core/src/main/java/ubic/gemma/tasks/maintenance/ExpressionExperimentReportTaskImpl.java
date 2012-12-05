package ubic.gemma.tasks.maintenance;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.job.TaskMethod;
import ubic.gemma.job.TaskResult;

/**
 * Handles delegation of report generation (to the space, or run locally)
 * 
 * @author klc
 * @version $Id$
 */

@Component
public class ExpressionExperimentReportTaskImpl implements ExpressionExperimentReportTask {

    private Log log = LogFactory.getLog( ExpressionExperimentReportTaskImpl.class.getName() );

    @Autowired
    private ExpressionExperimentReportService expressionExperimentReportService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.grid.javaspaces.task.expression.experiment.ExpressionExperimentReportTask#execute()
     */
    @Override
    @TaskMethod
    public TaskResult execute( ExpressionExperimentReportTaskCommand command ) {

        TaskResult result = new TaskResult( command, null );

        if ( command.doAll() ) {
            expressionExperimentReportService.generateSummaryObjects();
        } else if ( command.getExpressionExperiment() != null ) {
            expressionExperimentReportService.generateSummary( command.getExpressionExperiment().getId() );

        } else {
            log.warn( "TaskCommand was not valid, nothing being done" );
        }

        return result;
    }

}
