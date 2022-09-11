package ubic.gemma.core.util;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Hook into the bean post-processing lifecycle and report the worst offenders.
 * @author poirigui
 */
@CommonsLog
@Component
public class BeanInitializationTimeMonitor implements BeanPostProcessor, Ordered, ApplicationListener<ContextRefreshedEvent> {

    /**
     * Total amount of time that has to elapse while initializing the context for emitting a warning.
     */
    private static final int TOTAL_TIME_WARN_THRESHOLD = 5000;

    private final Map<String, StopWatch> stopWatches = new HashMap<>();

    @Autowired
    private BeanFactory beanFactory;

    @Override
    public Object postProcessBeforeInitialization( Object bean, String beanName ) throws BeansException {
        if ( stopWatches.containsKey( beanName ) ) {
            // from a previous context refresh
            stopWatches.get( beanName ).reset();
            stopWatches.get( beanName ).start();
        } else {
            stopWatches.put( beanName, StopWatch.createStarted() );
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization( Object bean, String beanName ) throws BeansException {
        StopWatch sw = stopWatches.get( beanName );
        if ( sw == null || sw.isStopped() ) {
            // sometimes, it does not get pre-processed, or it gets called twice...
            return bean;
        } else {
            sw.stop();
        }
        return bean;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void onApplicationEvent( ContextRefreshedEvent contextRefreshedEvent ) {
        long totalTime = stopWatches.values().stream().mapToLong( StopWatch::getTime ).sum();
        String worstOffenders =
                stopWatches.entrySet().stream()
                        .sorted( Comparator.comparing( e -> e.getValue().getTime(), Comparator.reverseOrder() ) )
                        .limit( 10 )
                        .map( entry -> formatBeanInitializationTime( entry.getKey(), entry.getValue(), false ) )
                        .filter( Objects::nonNull )
                        .collect( Collectors.joining( "\n\t" ) );
        if ( totalTime >= TOTAL_TIME_WARN_THRESHOLD ) {
            log.warn( String.format( "Spent %d ms initializing beans. Here are the worst offenders:\n\n\t%s.\n\nEnable debug logs for %s for a complete breakdown.",
                    totalTime, worstOffenders, BeanInitializationTimeMonitor.class.getName() ) );
        }
        if ( log.isDebugEnabled() ) {
            String completeBreakdown = stopWatches.entrySet().stream()
                    .sorted( Comparator.comparing( e -> e.getValue().getTime(), Comparator.reverseOrder() ) )
                    .map( entry -> formatBeanInitializationTime( entry.getKey(), entry.getValue(), true ) )
                    .filter( Objects::nonNull )
                    .collect( Collectors.joining( ", " ) );
            log.debug( "Complete breakdown of bean initialization time: " + completeBreakdown );
        }
    }

    private String formatBeanInitializationTime( String beanName, StopWatch stopWatch, boolean includeBean ) {
        try {
            return beanName + ": " + stopWatch.getTime( TimeUnit.MILLISECONDS ) + " ms" + ( includeBean ? " [" + beanFactory.getBean( beanName ) + "]" : "" );
        } catch ( NoSuchBeanDefinitionException e ) {
            // FIXME: for some reason, we have a ubic.gemma.persistence.retry.RetryPolicy#1c025cb bean defined (see https://github.com/PavlidisLab/Gemma/issues/345)
            return null; // euh??!
        }
    }
}
