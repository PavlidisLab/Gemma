package ubic.gemma.tasks.analysis.expression;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.analysis.preprocess.svd.SVDService;
import ubic.gemma.job.TaskResult;

/**
 * @author paul
 * @version $Id$
 */
@Component
@Scope("prototype")
public class SvdTaskImpl implements SvdTask {

    private Log log = LogFactory.getLog( SvdTask.class.getName() );

    @Autowired private SVDService svdService;

    private SvdTaskCommand command;

    @Override
    public void setCommand( SvdTaskCommand command ) {
        this.command = command;
    }

    @Override
    public TaskResult execute() {

        TaskResult result = new TaskResult( command, null );

        if ( command.getExpressionExperiment() != null ) {
            svdService.svd( command.getExpressionExperiment().getId() );
        } else {
            log.warn( "TaskCommand was not valid, nothing being done" );
        }

        return result;
    }

}
