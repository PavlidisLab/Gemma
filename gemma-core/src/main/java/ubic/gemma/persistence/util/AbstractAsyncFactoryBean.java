package ubic.gemma.persistence.util;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Base implementation of {@link AsyncFactoryBean} so that subclasses only need to implement the relevant parts of the
 * {@link FactoryBean} interface.
 * <p>
 * The implementation initializes beans using a {@link ExecutorService} which can be configured via
 * {@link #AbstractAsyncFactoryBean(ExecutorService)}. The default executor is single-threaded, which is suitable for
 * singleton beans.
 * <p>
 * Never inject {@link T} directly as this will cause a synchronous initialization of the bean. Instead, inject either a
 * {@link org.springframework.beans.factory.BeanFactory} and call {@link org.springframework.beans.factory.BeanFactory#getBean(Class)}
 * when needed or inject {@link AsyncFactoryBean} and call {@link AsyncFactoryBean#getObjectAsync()}.
 * <p>
 * This implementation handles destruction of the factory when the context is closed by dispatching a
 * {@link Future#cancel(boolean)} on any pending bean creation.
 * @param <T> type of bean that this factory provides
 * @author poirigui
 */
public abstract class AbstractAsyncFactoryBean<T> implements AsyncFactoryBean<T>, DisposableBean {

    /**
     * Executor used to initialize beans.
     */
    private final ExecutorService executor;

    /**
     * Singleton if {@link #isSingleton()} is true.
     */
    private Future<T> singletonBean;

    /**
     * Pending futures, but might also contain completed ones.
     * <p>
     * Completed futures are removed before adding new ones.
     */
    private final List<Future<T>> pendingBeans = new ArrayList<>();

    protected AbstractAsyncFactoryBean() {
        this( Executors.newSingleThreadExecutor() );
    }

    protected AbstractAsyncFactoryBean( ExecutorService executor ) {
        this.executor = executor;
    }

    @Override
    public synchronized final Future<T> getObjectAsync() {
        if ( isSingleton() && singletonBean != null ) {
            return singletonBean;
        }
        Future<T> future = executor.submit( this::getObject );
        if ( isSingleton() ) {
            singletonBean = future;
        }
        pendingBeans.removeIf( Future::isDone );
        pendingBeans.add( future );
        return future;
    }

    @Override
    public final boolean isInitialized() {
        return isSingleton() && singletonBean != null;
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void destroy() {
        for ( Future<T> f : pendingBeans ) {
            f.cancel( true );
        }
    }
}
