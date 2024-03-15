package ubic.gemma.web.scheduler;

import lombok.Setter;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.util.Assert;

/**
 *
 */
@Setter
public abstract class SecureQuartzJobBean extends QuartzJobBean {

    private SecureInvoker secureInvoker;

    @Override
    protected final void executeInternal( JobExecutionContext context ) throws JobExecutionException {
        Assert.notNull( secureInvoker, "The secureInvoker bean is not set." );
        try {
            secureInvoker.invoke( () -> {
                executeAsAgent( context );
                return null;
            } );
        } catch ( Exception e ) {
            throw new JobExecutionException( e );
        }
    }

    protected abstract void executeAsAgent( JobExecutionContext context ) throws JobExecutionException;
}

