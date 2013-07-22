package ubic.gemma.tasks.analysis.expression;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.analysis.preprocess.batcheffects.BatchInfoPopulationService;
import ubic.gemma.job.TaskResult;
import ubic.gemma.tasks.AbstractTask;

/**
 * Task to try to get 'batch' information about an experiment. This usually involves downloading raw data files from the
 * provider.
 * @author paul
 * @version $Id$
 */
@Component
@Scope("prototype")
public class BatchInfoFetchTaskImpl extends AbstractTask<TaskResult, BatchInfoFetchTaskCommand> implements BatchInfoFetchTask {

    @Autowired private BatchInfoPopulationService batchInfoService;

    private Log log = LogFactory.getLog( BatchInfoFetchTask.class.getName() );

    @Override
    public TaskResult execute() {
        TaskResult result = new TaskResult( taskCommand, null );

        if ( taskCommand.doAll() ) {
            throw new UnsupportedOperationException(
                    "Doing all Batch fetches in task not implemented, sorry, you must configure one" );
        } else if ( taskCommand.getExpressionExperiment() != null ) {
            taskCommand.setMaxRuntime( 30 ); // time to download files etc.
            batchInfoService.fillBatchInformation( taskCommand.getExpressionExperiment(), true);
        } else {
            log.warn( "TaskCommand was not valid, nothing being done" );
        }

        return result;
    }
}
