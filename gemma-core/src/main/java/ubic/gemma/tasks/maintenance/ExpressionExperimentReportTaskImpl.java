package ubic.gemma.tasks.maintenance;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.job.TaskResult;

@Component
@Scope("prototype")
public class ExpressionExperimentReportTaskImpl implements ExpressionExperimentReportTask {

    private Log log = LogFactory.getLog( ExpressionExperimentReportTask.class.getName() );

    @Autowired private ExpressionExperimentReportService expressionExperimentReportService;

    private ExpressionExperimentReportTaskCommand command;

    @Override
    public void setCommand(ExpressionExperimentReportTaskCommand command) {
        this.command = command;
    }

    /*
             * (non-Javadoc)
             *
             * @see ubic.gemma.grid.javaspaces.task.expression.experiment.ExpressionExperimentReportTask#execute()
             */
    @Override
    public TaskResult execute() {

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
