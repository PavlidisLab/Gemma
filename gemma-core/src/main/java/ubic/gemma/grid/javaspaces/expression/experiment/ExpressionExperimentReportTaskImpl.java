package ubic.gemma.grid.javaspaces.expression.experiment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.grid.javaspaces.BaseSpacesTask;
import ubic.gemma.grid.javaspaces.TaskCommand;
import ubic.gemma.grid.javaspaces.TaskResult;
import ubic.gemma.util.progress.TaskRunningService;
import ubic.gemma.util.progress.grid.javaspaces.SpacesProgressAppender;

/**
 * Handles delegation of report generation (to the space, or run locally)
 * 
 * @author klc
 * @version $Id$
 */

public class ExpressionExperimentReportTaskImpl extends BaseSpacesTask implements ExpressionExperimentReportTask {

    private Log log = LogFactory.getLog( ExpressionExperimentReportTaskImpl.class.getName() );

    private String taskId = null;
    private ExpressionExperimentReportService expressionExperimentReportService;

    public void setExpressionExperimentReportService(
            ExpressionExperimentReportService expressionExperimentReportService ) {
        this.expressionExperimentReportService = expressionExperimentReportService;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        this.taskId = TaskRunningService.generateTaskId();
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.grid.javaspaces.expression.experiment.ExpressionExperimentReportTask#execute()
     */
    public TaskResult execute( TaskCommand spacesCommand ) {

        SpacesProgressAppender spacesProgressAppender = super.initProgressAppender( this.getClass() );

        ExpressionExperimentReportTaskCommand eertc = ( ExpressionExperimentReportTaskCommand ) spacesCommand;

        TaskResult result = new TaskResult();
        result.setTaskID( super.taskId );

        if ( eertc.doAll() ) {
            expressionExperimentReportService.generateSummaryObjects();
        } else if ( eertc.getExpressionExperiment() != null ) {
            expressionExperimentReportService.generateSummaryObject( eertc.getExpressionExperiment().getId() );
        } else {
            log.warn( "TaskCommand was not valid, nothing being done" );
        }

        log.info( "Task execution complete ... returning result for task with id " + result.getTaskID() );

        super.tidyProgress( spacesProgressAppender );

        return result;
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.grid.javaspaces.SpacesTask#getTaskId()
     */
    @Override
    public String getTaskId() {
        return taskId;
    }

}
