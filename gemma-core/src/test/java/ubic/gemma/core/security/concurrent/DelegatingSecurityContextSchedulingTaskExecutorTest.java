package ubic.gemma.core.security.concurrent;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Guards the shutdown-event round-trip for the scheduling wrapper. A pool-backed executor
 * ({@link ThreadPoolTaskExecutor}) shuts down on {@link ContextClosedEvent} via {@link ApplicationListener};
 * the wrapper must re-expose that contract (so the event multicaster can resolve it at close without a
 * {@code BeanNotOfRequiredTypeException}) and forward the event to the delegate.
 */
public class DelegatingSecurityContextSchedulingTaskExecutorTest {

    @Test
    public void testWrapperIsApplicationListenerAndForwardsContextClosedEvent() {
        ThreadPoolTaskExecutor delegate = new ThreadPoolTaskExecutor();
        delegate.initialize();
        ThreadPoolTaskExecutor spy = spy( delegate );

        DelegatingSecurityContextSchedulingTaskExecutor wrapper =
                new DelegatingSecurityContextSchedulingTaskExecutor( spy );

        // The multicaster resolves listener beans via ApplicationListener.class — the wrapper must satisfy it.
        assertThat( wrapper ).isInstanceOf( ApplicationListener.class );

        ContextClosedEvent event = new ContextClosedEvent( mock( ApplicationContext.class ) );
        wrapper.onApplicationEvent( event );

        // ThreadPoolTaskExecutor handles pool shutdown through its own ApplicationListener callback.
        verify( spy ).onApplicationEvent( event );

        delegate.shutdown();
    }
}
