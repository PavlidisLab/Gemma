package ubic.gemma.core.config;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.core.metrics.GenericMeterRegistryConfigurer;
import ubic.gemma.core.metrics.MeterRegistryJCacheConfigurer;

import javax.cache.CacheManager;
import javax.sql.DataSource;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Smoke test for the {@code metrics} Spring profile.
 * <p>
 * Phase 2 (commit {@code ab94b884a4}) deleted {@code MeterRegistryEhcacheConfigurer} during the
 * Ehcache 2 -> Ehcache 3 / jakarta migration, which left the {@code metrics} profile broken: any
 * attempt to start it would fail at compile time on a dead reference. Phase 3 introduced
 * {@link MeterRegistryJCacheConfigurer} (Micrometer's {@code JCacheMetrics} binder over the new
 * JSR-107 cache manager) and re-wired {@link MetricsConfig} to use it.
 * <p>
 * This test boots {@link MetricsConfig} under the {@code metrics} profile with all heavy
 * dependencies (SessionFactory, HikariDataSource, TaskExecutor, TaskRunningService, JCache
 * CacheManager) mocked, and verifies that every {@code @Bean} instantiates cleanly. It does
 * not exercise the binders against a real datasource / Hibernate / JCache provider — that
 * happens inside the integration-test context — but it does guarantee that the profile loads,
 * which is the regression we care about here.
 */
public class MetricsConfigTest {

    @Configuration
    @TestComponent
    static class MetricsConfigTestDeps {

        @Bean
        public SessionFactory sessionFactory() {
            SessionFactory sf = mock( SessionFactory.class );
            Statistics s = mock( Statistics.class );
            // Stats disabled -> Hibernate4Metrics / Hibernate4QueryMetrics bind no counters.
            when( s.isStatisticsEnabled() ).thenReturn( Boolean.FALSE );
            when( sf.getStatistics() ).thenReturn( s );
            return sf;
        }

        @Bean
        public DataSource dataSource() {
            // HikariCPMetrics requires a HikariDataSource; setMetricRegistry on an
            // un-initialized pool is safe (pool boots on first getConnection()).
            return new HikariDataSource();
        }

        @Bean( destroyMethod = "shutdown" )
        public TaskExecutor taskExecutor() {
            // GenericTaskExecutorMetrics requires a ThreadPoolTaskExecutor (or a
            // DelegatingTaskExecutor wrapping one) and reads getThreadPoolExecutor() from it,
            // so the executor must be initialized — a Mockito mock or bare instance would
            // either fail the instanceof guard or NPE on the inner ThreadPoolExecutor.
            ThreadPoolTaskExecutor tpe = new ThreadPoolTaskExecutor();
            tpe.setCorePoolSize( 1 );
            tpe.setMaxPoolSize( 1 );
            tpe.setThreadNamePrefix( "metricsConfigTest-" );
            tpe.initialize();
            return tpe;
        }

        @Bean
        public TaskRunningService taskRunningService() {
            // TaskRunningService is itself a MeterBinder; its bindTo() is invoked by
            // GenericMeterRegistryConfigurer. A bare mock is fine — bindTo() is a no-op on it.
            return mock( TaskRunningService.class );
        }

        @Bean
        public CacheManager jCacheCacheManager() {
            CacheManager cm = mock( CacheManager.class );
            // No caches registered -> MeterRegistryJCacheConfigurer.configure() iterates nothing.
            when( cm.getCacheNames() ).thenReturn( Collections.emptyList() );
            return cm;
        }
    }

    @Test
    public void metricsProfileBeansInstantiateCleanly() {
        try ( AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext() ) {
            ctx.getEnvironment().setActiveProfiles( "metrics" );
            ctx.register( MetricsConfigTestDeps.class, MetricsConfig.class );
            ctx.refresh();

            assertThat( ctx.getBean( MeterRegistry.class ) ).isNotNull();
            assertThat( ctx.getBean( GenericMeterRegistryConfigurer.class ) ).isNotNull();
            assertThat( ctx.getBean( TimedAspect.class ) ).isNotNull();
            assertThat( ctx.getBean( MeterRegistryJCacheConfigurer.class ) ).isNotNull();
        }
    }

    @Test
    public void metricsProfileInactiveByDefault() {
        // Without the metrics profile, none of the @Profile("metrics") beans exist. This
        // protects against accidental always-on activation (the profile is opt-in).
        try ( AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext() ) {
            ctx.register( MetricsConfigTestDeps.class, MetricsConfig.class );
            ctx.refresh();
            assertThat( ctx.getBeansOfType( MeterRegistry.class ) ).isEmpty();
            assertThat( ctx.getBeansOfType( TimedAspect.class ) ).isEmpty();
            assertThat( ctx.getBeansOfType( MeterRegistryJCacheConfigurer.class ) ).isEmpty();
        }
    }
}
