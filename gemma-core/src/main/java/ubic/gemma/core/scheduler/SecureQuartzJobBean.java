package ubic.gemma.core.scheduler;

import lombok.Setter;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.util.Assert;

/**
 * A secure Quartz job bean that executes with a given security context.
 * @author poirigui
 */
@Setter
public abstract class SecureQuartzJobBean extends QuartzJobBean {

    private SecurityContext securityContext;

    @Override
    protected final void executeInternal( JobExecutionContext context ) throws JobExecutionException {
        Assert.notNull( securityContext, "A security context is not set." );
        try {
            DelegatingSecurityContextCallable.create( () -> {
                executeAs( context );
                return null;
            }, securityContext ).call();
        } catch ( JobExecutionException | RuntimeException e ) {
            throw e;
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    protected abstract void executeAs( JobExecutionContext context ) throws JobExecutionException;
}

