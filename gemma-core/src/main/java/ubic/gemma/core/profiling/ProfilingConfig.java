package ubic.gemma.core.profiling;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Configuration
@Profile("profiling")
public class ProfilingConfig {

    @Autowired
    private BeanFactory beanFactory;

    @Bean
    public static BeanInitializationTimeMonitor beanInitializationTimeMonitor() {
        return new BeanInitializationTimeMonitor();
    }

    @Bean
    public ApplicationListener<ContextRefreshedEvent> reportInitializationTime( BeanInitializationTimeMonitor monitor ) {
        if ( !BeanInitializationTimeMonitor.log.isDebugEnabled() ) {
            BeanInitializationTimeMonitor.log.info( String.format( "Debug logging is not enabled for %s, you will only see partial profiling results.",
                    BeanInitializationTimeMonitor.class.getName() ) );
        }
        //noinspection Convert2Lambda
        return new ApplicationListener<ContextRefreshedEvent>() {
            @Override
            public void onApplicationEvent( ContextRefreshedEvent event ) {
                Map<String, Long> totalTimesMs = new HashMap<>();
                long totalTimeMs = monitor.getTotalTime( TimeUnit.MILLISECONDS );
                BeanInitializationTimeMonitor.log.info( String.format( "Spent %d ms instantiating and initializing beans.", totalTimeMs ) );
                for ( String beanName : monitor.getRecordedBeans() ) {
                    totalTimesMs.put( beanName, monitor.getTotalTime( beanName, TimeUnit.MILLISECONDS ) );
                }
                if ( BeanInitializationTimeMonitor.log.isDebugEnabled() ) {
                    String completeBreakdown = totalTimesMs.entrySet().stream()
                            .sorted( Map.Entry.comparingByValue( Comparator.reverseOrder() ) )
                            .map( entry -> ProfilingConfig.this.formatBeanInitializationTime( entry.getKey(), monitor, true ) )
                            .filter( Objects::nonNull )
                            .collect( Collectors.joining( "\n" ) );
                    BeanInitializationTimeMonitor.log.debug( "Complete breakdown of bean initialization time:\n\t" + completeBreakdown.replaceAll( "\n", "\n\t" ) );
                } else {
                    // just show the worst offenders
                    String worstOffenders = totalTimesMs.entrySet().stream()
                            .sorted( Map.Entry.comparingByValue( Comparator.reverseOrder() ) )
                            .limit( 10 )
                            .map( entry -> ProfilingConfig.this.formatBeanInitializationTime( entry.getKey(), monitor, false ) )
                            .filter( Objects::nonNull )
                            .collect( Collectors.joining( "\n" ) );
                    BeanInitializationTimeMonitor.log.info( "Top 10 worst offenders:\n\t" + worstOffenders.replaceAll( "\n", "\n\t" ) + "\n" + "Enable debug logs for " + BeanInitializationTimeMonitor.class.getName() + " for a complete breakdown." );
                }
                monitor.reset();
            }
        };
    }

    private String formatBeanInitializationTime( String beanName, BeanInitializationTimeMonitor monitor, boolean includeBeanDetails ) {
        long instantiationTimeMs = monitor.getInstantiationTime( beanName, TimeUnit.MILLISECONDS );
        long initializationTimeMs = monitor.getInitializationTime( beanName, TimeUnit.MILLISECONDS );
        if ( includeBeanDetails ) {
            String beanDetails;
            try {
                beanDetails = beanFactory.getBean( beanName ).toString();
            } catch ( NoSuchBeanDefinitionException e ) {
                return formatBeanInitializationTime( beanName, monitor, false );
            }
            return String.format( "%s:\n\tcreation: %d ms\n\tinitialization: %d ms\n\tdetails: %s", beanName,
                    instantiationTimeMs, initializationTimeMs, beanDetails.replaceAll( "\n", "\n\t" ) );
        } else {
            return String.format( "%s:\n\tcreation: %d ms\n\tinitialization: %d ms", beanName, instantiationTimeMs, initializationTimeMs );
        }
    }
}
