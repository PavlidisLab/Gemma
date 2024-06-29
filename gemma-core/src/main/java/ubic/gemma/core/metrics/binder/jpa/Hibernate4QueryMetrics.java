package ubic.gemma.core.metrics.binder.jpa;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.hibernate.SessionFactory;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.Statistics;
import ubic.gemma.core.lang.Nullable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Query metrics for Hibernate 4.
 * @see io.micrometer.core.instrument.binder.jpa.HibernateQueryMetrics
 * @author poirigui
 */
public class Hibernate4QueryMetrics implements MeterBinder, AutoCloseable {

    private static final String SESSION_FACTORY_TAG_NAME = "entityManagerFactory";

    private final Iterable<Tag> tags;

    @Nullable
    private final Statistics statistics;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    /**
     * Create a {@code HibernateQueryMetrics}.
     * @param sessionFactory session factory to use
     * @param sessionFactoryName session factory name as a tag value
     * @param tags additional tags
     */
    public Hibernate4QueryMetrics( SessionFactory sessionFactory, String sessionFactoryName, Iterable<Tag> tags ) {
        this.tags = Tags.concat( tags, SESSION_FACTORY_TAG_NAME, sessionFactoryName );
        Statistics statistics = sessionFactory.getStatistics();
        this.statistics = statistics.isStatisticsEnabled() ? statistics : null;
    }

    @Override
    public void bindTo( MeterRegistry meterRegistry ) {
        if ( statistics == null ) {
            return;
        }
        executor.scheduleAtFixedRate( () -> {
            registerQueryMetric( statistics, meterRegistry );
        }, 0, 1000, TimeUnit.MILLISECONDS );
    }

    private void registerQueryMetric( Statistics statistics, MeterRegistry meterRegistry ) {
        for ( String query : statistics.getQueries() ) {
            QueryStatistics queryStatistics = statistics.getQueryStatistics( query );

            FunctionCounter
                    .builder( "hibernate.query.cache.requests", queryStatistics, QueryStatistics::getCacheHitCount )
                    .tags( tags )
                    .tags( "result", "hit", "query", query )
                    .description( "Number of query cache hits" )
                    .register( meterRegistry );

            FunctionCounter
                    .builder( "hibernate.query.cache.requests", queryStatistics, QueryStatistics::getCacheMissCount )
                    .tags( tags )
                    .tags( "result", "miss", "query", query )
                    .description( "Number of query cache misses" )
                    .register( meterRegistry );

            FunctionCounter
                    .builder( "hibernate.query.cache.puts", queryStatistics, QueryStatistics::getCachePutCount )
                    .tags( tags )
                    .tags( "query", query )
                    .description( "Number of cache puts for a query" )
                    .register( meterRegistry );

            TimeGauge
                    .builder( "hibernate.query.execution.max", queryStatistics, TimeUnit.MILLISECONDS,
                            QueryStatistics::getExecutionMaxTime )
                    .tags( tags )
                    .tags( "query", query )
                    .description( "Query maximum execution time" )
                    .register( meterRegistry );

            TimeGauge
                    .builder( "hibernate.query.execution.min", queryStatistics, TimeUnit.MILLISECONDS,
                            QueryStatistics::getExecutionMinTime )
                    .tags( tags )
                    .tags( "query", query )
                    .description( "Query minimum execution time" )
                    .register( meterRegistry );

            FunctionCounter
                    .builder( "hibernate.query.execution.rows", queryStatistics, QueryStatistics::getExecutionRowCount )
                    .tags( tags )
                    .tags( "query", query )
                    .description( "Number of rows processed for a query" )
                    .register( meterRegistry );
        }
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
