package ubic.gemma.core.profiling;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import ubic.gemma.core.context.BeanFactoryUtils;

import java.beans.PropertyDescriptor;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Hook into the bean post-processing lifecycle and record bean initialization time.
 * @author poirigui
 */
public class BeanInitializationTimeMonitor implements BeanFactoryPostProcessor {

    static Log log = LogFactory.getLog( BeanInitializationTimeMonitor.class );

    private final Map<String, Long> instantiationTimesNs = new HashMap<>();
    private final Map<String, Long> initializationTimesNs = new HashMap<>();

    public Collection<String> getRecordedBeans() {
        Set<String> beans = new HashSet<>( instantiationTimesNs.keySet() );
        beans.addAll( initializationTimesNs.keySet() );
        return beans;
    }

    /**
     * Obtain the time spent instantiating a bean.
     */
    public long getInstantiationTime( String beanName, TimeUnit timeUnit ) {
        return timeUnit.convert( instantiationTimesNs.getOrDefault( beanName, 0L ), TimeUnit.NANOSECONDS );
    }

    /**
     * Obtain the time spent initializing a bean.
     */
    public long getInitializationTime( String beanName, TimeUnit timeUnit ) {
        return timeUnit.convert( initializationTimesNs.getOrDefault( beanName, 0L ), TimeUnit.NANOSECONDS );
    }

    /**
     * Obtain the total time spent instantiating and initializing a bean.
     */
    public Long getTotalTime( String beanName, TimeUnit timeUnit ) {
        return timeUnit.convert( initializationTimesNs.getOrDefault( beanName, 0L ) + instantiationTimesNs.getOrDefault( beanName, 0L ), TimeUnit.NANOSECONDS );
    }

    /**
     * Obtain the total time spent instantiating and initializing beans.
     */
    public long getTotalTime( TimeUnit timeUnit ) {
        return timeUnit.convert( instantiationTimesNs.values().stream().mapToLong( Long::longValue ).sum() + initializationTimesNs.values().stream().mapToLong( Long::longValue ).sum(), TimeUnit.NANOSECONDS );
    }

    /**
     * Reset recorded times.
     */
    public void reset() {
        instantiationTimesNs.clear();
        initializationTimesNs.clear();
    }

    @Override
    public void postProcessBeanFactory( ConfigurableListableBeanFactory beanFactory ) throws BeansException {
        BeanFactoryUtils.addBeanPostProcessor( beanFactory, 0, new PreMonitor() );
        beanFactory.addBeanPostProcessor( new PostMonitor() );
    }

    private class PreMonitor implements InstantiationAwareBeanPostProcessor {

        @Override
        public Object postProcessBeforeInstantiation( Class<?> beanClass, String beanName ) throws BeansException {
            instantiationTimesNs.put( beanName, System.nanoTime() );
            return null;
        }

        @Override
        public boolean postProcessAfterInstantiation( Object bean, String beanName ) throws BeansException {
            return true;
        }

        @Override
        public Object postProcessBeforeInitialization( Object bean, String beanName ) throws BeansException {
            initializationTimesNs.put( beanName, System.nanoTime() );
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization( Object bean, String beanName ) throws BeansException {
            return bean;
        }

        @Override
        public PropertyValues postProcessPropertyValues( PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName ) throws BeansException {
            return pvs;
        }
    }

    private class PostMonitor implements InstantiationAwareBeanPostProcessor {

        @Override
        public Object postProcessBeforeInstantiation( Class<?> beanClass, String beanName ) throws BeansException {
            return null;
        }

        @Override
        public boolean postProcessAfterInstantiation( Object bean, String beanName ) throws BeansException {
            Long preInstantiationTimeNs = instantiationTimesNs.get( beanName );
            if ( preInstantiationTimeNs == null ) {
                log.debug( "Did not record pre-instantiation time for bean " + beanName + "." );
                return true;
            }
            long instantiationTimeNs = System.nanoTime() - preInstantiationTimeNs;
            instantiationTimesNs.put( beanName, instantiationTimeNs );
            log.trace( String.format( "%s instantiation took %d ms", beanName, TimeUnit.NANOSECONDS.toMillis( instantiationTimeNs ) ) );
            return true;
        }

        @Override
        public Object postProcessBeforeInitialization( Object bean, String beanName ) throws BeansException {
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization( Object bean, String beanName ) throws BeansException {
            Long preInitializationTimeNs = initializationTimesNs.get( beanName );
            if ( preInitializationTimeNs == null ) {
                log.debug( "Did not record pre-initialization time for bean " + beanName + "." );
                return bean;
            }
            long initializationTimeNs = System.nanoTime() - preInitializationTimeNs;
            initializationTimesNs.put( beanName, initializationTimeNs );
            log.trace( String.format( "%s initialization took %d ms", beanName, TimeUnit.NANOSECONDS.toMillis( initializationTimeNs ) ) );
            return bean;
        }

        @Override
        public PropertyValues postProcessPropertyValues( PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName ) throws BeansException {
            return pvs;
        }
    }
}
