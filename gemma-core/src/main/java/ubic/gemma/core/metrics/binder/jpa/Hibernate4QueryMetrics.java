package ubic.gemma.core.metrics.binder.jpa;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.Statistics;

import java.util.concurrent.TimeUnit;

/**
 * Metrics for Hibernate 4 queries.
 *
 * @author poirigui
 * @see io.micrometer.core.instrument.binder.jpa.HibernateQueryMetrics
 */
public class Hibernate4QueryMetrics implements MeterBinder {

    private static final String SESSION_FACTORY_TAG_NAME = "entityManagerFactory";

    private final Iterable<Tag> tags;

    private final SessionFactory sessionFactory;

    /**
     * Create {@code HibernateQueryMetrics} and bind to the specified meter registry.
     * @param registry meter registry to use
     * @param sessionFactory session factory to use
     * @param sessionFactoryName session factory name as a tag value
     * @param tags additional tags
     */
    public static void monitor( MeterRegistry registry, SessionFactory sessionFactory, String sessionFactoryName,
            String... tags ) {
        monitor( registry, sessionFactory, sessionFactoryName, Tags.of( tags ) );
    }

    /**
     * Create {@code HibernateQueryMetrics} and bind to the specified meter registry.
     * @param registry meter registry to use
     * @param sessionFactory session factory to use
     * @param sessionFactoryName session factory name as a tag value
     * @param tags additional tags
     */
    public static void monitor( MeterRegistry registry, SessionFactory sessionFactory, String sessionFactoryName,
            Iterable<Tag> tags ) {
        new Hibernate4QueryMetrics( sessionFactory, sessionFactoryName, tags ).bindTo( registry );
    }

    /**
     * Create a {@code HibernateQueryMetrics}.
     * @param sessionFactory session factory to use
     * @param sessionFactoryName session factory name as a tag value
     * @param tags additional tags
     */
    public Hibernate4QueryMetrics( SessionFactory sessionFactory, String sessionFactoryName, Iterable<Tag> tags ) {
        this.tags = Tags.concat( tags, SESSION_FACTORY_TAG_NAME, sessionFactoryName );
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void bindTo( MeterRegistry meterRegistry ) {
        if ( sessionFactory instanceof SessionFactoryImplementor ) {
            EventListenerRegistry eventListenerRegistry = ( ( SessionFactoryImplementor ) sessionFactory )
                    .getServiceRegistry().getService( EventListenerRegistry.class );
            MetricsEventHandler metricsEventHandler = new MetricsEventHandler( meterRegistry );
            eventListenerRegistry.appendListeners( EventType.POST_LOAD, metricsEventHandler );
        }
    }

    class MetricsEventHandler implements PostLoadEventListener {

        private final MeterRegistry meterRegistry;

        MetricsEventHandler( MeterRegistry meterRegistry ) {
            this.meterRegistry = meterRegistry;
        }

        @Override
        public void onPostLoad( PostLoadEvent event ) {
            registerQueryMetric( event.getSession().getFactory().getStatistics() );
        }

        void registerQueryMetric( Statistics statistics ) {
            for ( String query : statistics.getQueries() ) {
                QueryStatistics queryStatistics = statistics.getQueryStatistics( query );

                FunctionCounter
                        .builder( "hibernate.query.cache.requests", queryStatistics, QueryStatistics::getCacheHitCount )
                        .tags( tags ).tags( "result", "hit", "query", query ).description( "Number of query cache hits" )
                        .register( meterRegistry );

                FunctionCounter
                        .builder( "hibernate.query.cache.requests", queryStatistics, QueryStatistics::getCacheMissCount )
                        .tags( tags ).tags( "result", "miss", "query", query ).description( "Number of query cache misses" )
                        .register( meterRegistry );

                FunctionCounter
                        .builder( "hibernate.query.cache.puts", queryStatistics, QueryStatistics::getCachePutCount )
                        .tags( tags ).tags( "query", query ).description( "Number of cache puts for a query" )
                        .register( meterRegistry );

                TimeGauge
                        .builder( "hibernate.query.execution.max", queryStatistics, TimeUnit.MILLISECONDS,
                                QueryStatistics::getExecutionMaxTime )
                        .tags( tags ).tags( "query", query ).description( "Query maximum execution time" )
                        .register( meterRegistry );

                TimeGauge
                        .builder( "hibernate.query.execution.min", queryStatistics, TimeUnit.MILLISECONDS,
                                QueryStatistics::getExecutionMinTime )
                        .tags( tags ).tags( "query", query ).description( "Query minimum execution time" )
                        .register( meterRegistry );

                FunctionCounter
                        .builder( "hibernate.query.execution.rows", queryStatistics,
                                QueryStatistics::getExecutionRowCount )
                        .tags( tags ).tags( "query", query ).description( "Number of rows processed for a query" )
                        .register( meterRegistry );
            }
        }

    }

}
