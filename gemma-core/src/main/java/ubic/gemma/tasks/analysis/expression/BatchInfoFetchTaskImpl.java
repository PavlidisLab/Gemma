package ubic.gemma.tasks.analysis.expression;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.analysis.preprocess.batcheffects.BatchInfoPopulationService;
import ubic.gemma.job.TaskResult;

/**
 * Task to try to get 'batch' information about an experiment. This usually involves downloading raw data files from the
 * provider.
 * @author paul
 * @version $Id$
 */
@Component
@Scope("prototype")
public class BatchInfoFetchTaskImpl implements BatchInfoFetchTask {

    @Autowired private BatchInfoPopulationService batchInfoService;

    private BatchInfoFetchTaskCommand command;

    private Log log = LogFactory.getLog( BatchInfoFetchTask.class.getName() );

    @Override
    public void setCommand( BatchInfoFetchTaskCommand command ) {
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
            throw new UnsupportedOperationException(
                    "Doing all Batch fetches in task not implemented, sorry, you must configure one" );
        } else if ( command.getExpressionExperiment() != null ) {
            command.setMaxRuntime( 30 ); // time to download files etc.
            batchInfoService.fillBatchInformation(command.getExpressionExperiment(), true);
        } else {
            log.warn( "TaskCommand was not valid, nothing being done" );
        }

        return result;
    }

}
