package ubic.gemma.core.context;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.DisposableBean;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Base implementation of {@link AsyncFactoryBean}.
 * <p>
 * For singleton beans, this implementation holds the singleton using a {@link Future}. Thus, the {@link #createObject()}
 * does not need to actually use a singleton pattern as it is guaranteed to be called only once when {@link #isSingleton()}
 * is true.
 * <p>
 * The implementation initializes beans using a {@link ExecutorService} which can be configured via
 * {@link #AbstractAsyncFactoryBean(ExecutorService)}. The default executor is single-threaded, which is suitable for
 * singleton beans.
 * <p>
 * This implementation handles destruction of the factory when the context is closed by dispatching a
 * {@link Future#cancel(boolean)} on any pending bean creation.
 * <p>
 * The {@link #getObject()} is thread-safe and will appropriately handle multiple threads attempting to create a
 * singleton bean or coordinate multiple pending beans.
 * @param <T> type of bean that this factory provides
 * @author poirigui
 */
@CommonsLog
public abstract class AbstractAsyncFactoryBean<T> implements AsyncFactoryBean<T>, DisposableBean {

    /**
     * Executor used to initialize beans.
     */
    private final ExecutorService executor;

    /**
     * Whether to shut down the {@link #executor} when this bean is disposed.
     */
    private boolean shutdownExecutorOnDispose = false;

    /**
     * Singleton if {@link #isSingleton()} is true.
     */
    private volatile Future<T> singletonBean;

    /**
     * Pending futures, but might also contain completed ones.
     * <p>
     * Completed futures are removed before adding new ones.
     */
    private final Queue<Future<T>> pendingBeans = new ConcurrentLinkedQueue<>();

    protected AbstractAsyncFactoryBean() {
        this( Executors.newSingleThreadExecutor() );
        this.shutdownExecutorOnDispose = true;
    }

    protected AbstractAsyncFactoryBean( ExecutorService executor ) {
        this.executor = executor;
    }

    @Override
    public final Future<T> getObject() {
        Future<T> future;
        if ( isSingleton() ) {
            if ( singletonBean != null ) {
                return singletonBean;
            }
            synchronized ( this ) {
                singletonBean = future = executor.submit( this::createObject );
            }
        } else {
            future = executor.submit( this::createObject );
        }
        pendingBeans.removeIf( Future::isDone );
        pendingBeans.add( future );
        return future;
    }

    @Override
    public final Class<?> getObjectType() {
        return Future.class;
    }

    /**
     * Create a new bean as per {@link #getObject()}.
     * <p>
     * The implementation of a singleton bean does not need to use the singleton pattern as this implementation already
     * guarantees it.
     */
    protected abstract T createObject() throws Exception;

    @Override
    public final boolean isInitialized() {
        return isSingleton() && singletonBean != null;
    }

    @Override
    public final void destroy() {
        if ( shutdownExecutorOnDispose ) {
            executor.shutdown();
        }
        pendingBeans.removeIf( Future::isDone );
        if ( !pendingBeans.isEmpty() ) {
            log.info( String.format( "There are pending beans creation in %s, they will be cancelled.", getClass().getName() ) );
            for ( Future<T> f : pendingBeans ) {
                f.cancel( true );
            }
            // after cancel, the futures are guaranteed to be done, so we can clear then right away
            pendingBeans.clear();
        }
    }
}
