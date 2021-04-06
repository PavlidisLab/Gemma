package ubic.gemma.core.tasks.analysis.expression;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchInfoPopulationException;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchInfoPopulationService;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.tasks.AbstractTask;

/**
 * Task to try to get 'batch' information about an experiment. This usually involves downloading raw data files from the
 * provider.
 *
 * @author paul
 */
@Component
@Scope("prototype")
public class BatchInfoFetchTaskImpl extends AbstractTask<TaskResult, BatchInfoFetchTaskCommand>
        implements BatchInfoFetchTask {

    private final Log log = LogFactory.getLog( BatchInfoFetchTask.class.getName() );
    @Autowired
    private BatchInfoPopulationService batchInfoService;

    @Override
    public TaskResult execute() {
        TaskResult result = new TaskResult( taskCommand, null );

        if ( taskCommand.doAll() ) {
            throw new UnsupportedOperationException(
                    "Doing all Batch fetches in task not implemented, sorry, you must configure one" );
        } else if ( taskCommand.getExpressionExperiment() != null ) {
            taskCommand.setMaxRuntime( 30 ); // time to download files etc.
            try {
                batchInfoService.fillBatchInformation( taskCommand.getExpressionExperiment(), true );
            } catch ( BatchInfoPopulationException e ) {
                log.error( "Could not fill batch information for " + taskCommand.getExpressionExperiment() + ".", e );
            }
        } else {
            log.warn( "TaskCommand was not valid, nothing being done" );
        }

        return result;
    }
}
