package ubic.gemma.core.security.concurrent;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.SchedulingTaskExecutor;
import ubic.gemma.core.util.concurrent.DelegatingSchedulingTaskExecutor;

public class DelegatingSecurityContextSchedulingTaskExecutor extends org.springframework.security.scheduling.DelegatingSecurityContextSchedulingTaskExecutor implements DelegatingSchedulingTaskExecutor, ApplicationListener<ContextClosedEvent> {

    private final SchedulingTaskExecutor delegate;

    public DelegatingSecurityContextSchedulingTaskExecutor( SchedulingTaskExecutor delegate ) {
        super( delegate );
        this.delegate = delegate;
    }

    @Override
    public SchedulingTaskExecutor getDelegate() {
        return delegate;
    }

    /**
     * Spring's pool-backed executors ({@link org.springframework.scheduling.concurrent.ExecutorConfigurationSupport},
     * the base of {@code ThreadPoolTaskExecutor}/{@code ThreadPoolTaskScheduler}) shut their thread pool down on
     * {@link ContextClosedEvent} by implementing {@link ApplicationListener}. Wrapping such a bean hid that interface,
     * which both dropped the context-close shutdown propagation and made the event multicaster throw
     * {@code BeanNotOfRequiredTypeException} at shutdown — the bean was registered as a listener by its pre-wrapping
     * type but resolved to this wrapper, which was not an {@code ApplicationListener}. Re-expose the contract and
     * forward the event so both are restored; a no-op when the delegate is not itself a listener.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void onApplicationEvent( ContextClosedEvent event ) {
        if ( delegate instanceof ApplicationListener ) {
            ( ( ApplicationListener<ContextClosedEvent> ) delegate ).onApplicationEvent( event );
        }
    }
}
