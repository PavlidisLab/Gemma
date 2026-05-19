/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.hibernate;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import ubic.gemma.persistence.retry.RetryLogger;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Renovations Phase 3: Java-config replacement for {@code applicationContext-hibernate.xml}.
 * <p>
 * Wires Gemma's Hibernate stack: the {@link SessionFactory} (via native Hibernate bootstrap through
 * {@link HibernateSessionFactoryBean}), the {@link HibernateTransactionManager}, and the retry
 * advisor that wraps {@code @Transactional} service methods to retry stale-state failures.
 * <p>
 * <b>Bean names are load-bearing.</b> Gemma DAOs, test base classes, and the security stack
 * autowire {@code sessionFactory} and {@code transactionManager} by name; the {@code @Bean(name=…)}
 * declarations below match the XML ids exactly.
 * <p>
 * <b>SessionFactory bootstrap.</b> We do NOT use Spring's {@code LocalSessionFactoryBean} —
 * Spring 6's hibernate5 LocalSessionFactoryBean calls {@code ReflectionManager.reset()}, which
 * was removed in Hibernate 6. The JPA-bootstrap alternative hard-codes
 * {@code hibernate.current_session_context_class=jpa} which breaks
 * {@code SessionFactory.getCurrentSession()} for our {@link HibernateTransactionManager}-bridged
 * DAOs. {@link HibernateSessionFactoryBean} is Gemma's native-Hibernate FactoryBean that side-steps
 * both problems; see its javadoc for the full Phase-2 history.
 * <p>
 * <b>{@code depends-on="createDatabaseInitializer"}.</b> Preserved from XML so that — in the test
 * profile — the empty {@code gemdtest} DB is created before Hibernate connects and runs
 * {@code hbm2ddl=create}. In prod/dev the {@code createDatabaseInitializer} bean is a dummy
 * {@code java.lang.Object} (see {@code applicationContext-dataSource.xml}) and the depends-on is
 * a no-op.
 * <p>
 * <b>{@code dataSource}.</b> Defined in {@code applicationContext-dataSource.xml}
 * (HikariDataSource) and injected here by name; we never redefine it.
 * <p>
 * <b>Transaction manager DataSource exposure.</b> {@code transactionManager.dataSource} is set so
 * that {@code JdbcTemplate} / {@code DataSourceUtils} operations against the same DataSource
 * participate in the current Hibernate-managed transaction. Required by the Phase-2 JDBC ACL stack
 * — ACL INSERT/UPDATE/SELECT must co-commit with the surrounding {@code @Transactional} business
 * call, otherwise {@code readAclById} right after {@code createAcl} can't see the just-inserted row.
 * <p>
 * <b>{@code @EnableTransactionManagement}.</b> Replaces {@code <tx:annotation-driven order="3"/>}.
 * The {@code order=3} from XML positioned annotation-driven tx advice after the retry advisor
 * (order=2) so that a stale-state retry attempt rolls back the failed tx and starts a fresh one.
 * Spring's default ordering for {@code @EnableTransactionManagement} is
 * {@code Ordered.LOWEST_PRECEDENCE}, which keeps tx advice outermost from method invocation — but
 * the retry advisor (order=2) is registered programmatically via {@code <aop:advisor>} below with
 * an explicit lower order, so it runs OUTSIDE the tx proxy and the relative ordering is preserved
 * (retry wraps tx, just as before).
 * <p>
 * <b>{@code @EnableCaching}.</b> Replaces {@code <cache:annotation-driven order="2"/>} so
 * {@code @Cacheable} annotations are honoured. The {@code cacheManager} alias is published from
 * {@link ubic.gemma.persistence.cache.EhcacheConfig} (via the existing {@code ehcache} →
 * {@code cacheManager} alias maintained in XML or programmatically); we don't redefine it here.
 * <p>
 * <b>Retry advisor.</b> The XML wired an {@code <aop:advisor>} pointing at
 * {@code ubic.gemma.persistence.util.Pointcuts.retryableOrTransactionalServiceMethod()} with a
 * {@code RetryOperationsInterceptor}. AspectJ autoproxy / pointcut-based advice is not naturally
 * expressible via {@code @Bean} alone; the simplest and most XML-faithful migration is to keep
 * exactly one small XML companion ({@code applicationContext-hibernate-retry.xml}) for the
 * {@code <aop:advisor>} declaration only, while the supporting interceptor + retry policy beans
 * move here. <b>However</b>, all the underlying beans are now {@code @Bean}-defined: a fresh aspect
 * configuration class could refer to them by id from a much smaller XML, or Spring's
 * {@code DefaultPointcutAdvisor} can be wired as a {@code @Bean} pointing at an
 * {@link org.springframework.aop.aspectj.AspectJExpressionPointcut}. We take the latter route
 * below so this migration eliminates the XML completely.
 */
@Configuration
@EnableTransactionManagement(order = 3)
@EnableCaching(order = 2)
public class HibernateConfig {

    // ---------------------------------------------------------------------------------------------
    // Hibernate property placeholders (resolved from PropertySourcesConfiguration / hibernate.properties)
    // ---------------------------------------------------------------------------------------------

    @Value("${gemma.hibernate.hbm2ddl.auto}")
    private String hbm2ddlAuto;

    @Value("${gemma.hibernate.default_batch_fetch_size}")
    private String defaultBatchFetchSize;

    @Value("${gemma.hibernate.jdbc_batch_size}")
    private String jdbcBatchSize;

    @Value("${gemma.hibernate.show_sql}")
    private String showSql;

    @Value("${gemma.hibernate.format_sql}")
    private String formatSql;

    @Value("${gemma.transaction.maxretries}")
    private int maxRetries;

    /**
     * Filesystem root for Hibernate Search 7's Lucene indexes.
     * <p>
     * Resolved from {@code gemma.search.dir} (see {@code default.properties}); each
     * indexed entity becomes a sub-directory under this root. The directory is created
     * on demand at first index/query. See SEARCH_RECCE.md Section 3.5 — this is the
     * HS 7 successor to the pre-Phase-2 {@code hibernate.search.default.indexBase}
     * property.
     */
    @Value("${gemma.search.dir}")
    private String searchIndexBase;

    // ---------------------------------------------------------------------------------------------
    // SessionFactory
    // ---------------------------------------------------------------------------------------------

    /**
     * Native Hibernate SessionFactory built from {@code hibernate.cfg.xml}.
     * <p>
     * {@code depends-on="createDatabaseInitializer"} is declared on the {@code @Bean} so that the
     * empty {@code gemdtest} DB is created before Hibernate connects (test profile). In prod/dev
     * the {@code createDatabaseInitializer} bean is a dummy {@code java.lang.Object} — the
     * dependency is a no-op.
     * <p>
     * The {@code MySQL57InnoDBDialect} is hard-coded (was a literal in the XML, not a placeholder).
     * Other Hibernate properties — hbm2ddl, batch sizes, sql logging — come from
     * {@code ${gemma.hibernate.*}} placeholders resolved against {@code Settings.properties} /
     * {@code hibernate.properties} by {@code PropertySourcesConfiguration}.
     */
    @Bean(name = "sessionFactory")
    @org.springframework.context.annotation.DependsOn("createDatabaseInitializer")
    public HibernateSessionFactoryBean sessionFactory( DataSource dataSource ) {
        HibernateSessionFactoryBean factory = new HibernateSessionFactoryBean();
        factory.setDataSource( dataSource );
        factory.setConfigLocation( new ClassPathResource( "hibernate.cfg.xml" ) );

        Properties props = new Properties();
        props.setProperty( "hibernate.hbm2ddl.auto", hbm2ddlAuto );
        props.setProperty( "hibernate.dialect", "ubic.gemma.persistence.hibernate.MySQL57InnoDBDialect" );
        // Bridge sessionFactory.getCurrentSession() to Spring's HibernateTransactionManager.
        props.setProperty( "hibernate.current_session_context_class",
                "org.springframework.orm.hibernate5.SpringSessionContext" );
        // L2 cache: JCache (Ehcache 3 jakarta). EhCache 2 was retired in Phase 2 Step 5b.
        props.setProperty( "hibernate.cache.region.factory_class", "jcache" );
        props.setProperty( "hibernate.javax.cache.provider", "org.ehcache.jsr107.EhcacheCachingProvider" );
        props.setProperty( "hibernate.javax.cache.missing_cache_strategy", "create" );
        props.setProperty( "hibernate.cache.use_query_cache", "true" );
        props.setProperty( "hibernate.cache.use_second_level_cache", "true" );
        // defaults for fetching/inserting
        props.setProperty( "hibernate.max_fetch_depth", "3" );
        props.setProperty( "hibernate.default_batch_fetch_size", defaultBatchFetchSize );
        props.setProperty( "hibernate.jdbc.batch_size", jdbcBatchSize );
        props.setProperty( "hibernate.jdbc.batch_versioned_data", "true" );
        props.setProperty( "hibernate.order_inserts", "true" );
        props.setProperty( "hibernate.order_updates", "true" );
        // used for micrometer
        props.setProperty( "hibernate.generate_statistics", "true" );
        // debugging options
        props.setProperty( "hibernate.show_sql", showSql );
        props.setProperty( "hibernate.format_sql", formatSql );

        // ------------------------------------------------------------------------------------------
        // Hibernate Search 7 bootstrap (Renovations Phase 3, search restoration Step 1 + 2).
        // Wires HS 7's Lucene-direct (local filesystem) backend. Property keys verified against
        // org.hibernate.search.engine.cfg.BackendSettings, LuceneBackendSettings,
        // LuceneIndexSettings, and HibernateOrmMapperSettings (HS 7.2.4). The HS 5
        // lucene_version / default.indexBase / indexing_strategy keys are GONE in HS 7 and
        // replaced by the hibernate.search.backend.* and hibernate.search.indexing.*
        // namespaces below. See SEARCH_RECCE.md Section 3.5.
        //
        // Step 2 update: with @Indexed entities now in place (the eight indexed roots:
        // ExpressionExperiment, Gene, ArrayDesign, CompositeSequence, BioSequence, GeneSet,
        // ExpressionExperimentSet, BibliographicReference -- plus the @IndexedEmbedded
        // contributor graph), we move from "bootstrap-only" to "schema-aware":
        //   - listeners.enabled stays FALSE: Gemma's pre-strip pattern was manual reindex via
        //     IndexerService / cron, not automatic on-write through Hibernate listeners.
        //     Step 3-4 will revisit if/when we want write-through indexing. Keeping it off
        //     means the SessionFactory boot validates the mapping (will fail-fast on bad
        //     annotations) but does NOT try to push live writes through Lucene, which keeps
        //     the test-DB write path identical to today.
        //   - plan.synchronization.strategy = write-sync: when an indexer DOES enqueue work
        //     (mass reindex in Step 4), have the indexing plan apply changes synchronously so
        //     a subsequent query sees them. Cheap on a single-node Lucene backend.
        //   - schema_management.strategy = create-or-update: now that we have real indexed
        //     entities, let HS 7 create the per-entity Lucene directories on first boot, and
        //     keep them in sync if we add a @KeywordField later. Step 6 will do the proper
        //     "drop the HS5 indexes and reindex from scratch" cutover; create-or-update is
        //     forgiving in the interim.
        // ------------------------------------------------------------------------------------------
        // Backend type is inferred when only one backend jar is on the classpath; pin it
        // explicitly so a future ES-backend artifact on classpath doesn't silently flip the
        // default.
        props.setProperty( "hibernate.search.backend.type", "lucene" );
        // Local filesystem directory storage (vs. heap / NIO mmap variants).
        props.setProperty( "hibernate.search.backend.directory.type", "local-filesystem" );
        // Index root directory (HS 7 successor to HS 5's hibernate.search.default.indexBase).
        props.setProperty( "hibernate.search.backend.directory.root", searchIndexBase );
        // See block comment above: listeners off, manual reindex pattern.
        props.setProperty( "hibernate.search.indexing.listeners.enabled", "false" );
        // Synchronize indexing-plan commits with the surrounding transaction so post-reindex
        // queries immediately see the new state.
        props.setProperty( "hibernate.search.indexing.plan.synchronization.strategy", "write-sync" );
        // Create per-entity Lucene index dirs on first boot, keep them in sync as the
        // mapping evolves. Production cutover (Step 6) will drop the legacy HS 5 directories
        // wholesale before this strategy ever sees them.
        props.setProperty( "hibernate.search.schema_management.strategy", "create-or-update" );

        factory.setHibernateProperties( props );
        return factory;
    }

    // ---------------------------------------------------------------------------------------------
    // Transaction manager
    // ---------------------------------------------------------------------------------------------

    /**
     * {@link HibernateTransactionManager} for Spring-managed {@code @Transactional} methods.
     * <p>
     * Both {@code sessionFactory} and {@code dataSource} are wired in. Setting {@code dataSource}
     * exposes the Hibernate session's JDBC connection via {@code DataSourceUtils} so that
     * {@code JdbcTemplate} operations against the same DataSource participate in the current
     * Hibernate-managed transaction. The Phase-2 JDBC ACL stack relies on this co-commit behaviour:
     * ACL INSERT/UPDATE/SELECT must commit with the surrounding business {@code @Transactional},
     * otherwise {@code readAclById} immediately after {@code createAcl} cannot see the just-inserted
     * row.
     */
    @Bean(name = "transactionManager")
    public HibernateTransactionManager transactionManager( SessionFactory sessionFactory, DataSource dataSource ) {
        HibernateTransactionManager tm = new HibernateTransactionManager();
        tm.setSessionFactory( sessionFactory );
        tm.setDataSource( dataSource );
        return tm;
    }

    // ---------------------------------------------------------------------------------------------
    // Retry advisor (replaces <aop:config><aop:advisor pointcut-ref="retryable" advice-ref="retryAdvice"/>)
    // ---------------------------------------------------------------------------------------------

    /**
     * Retry policy: max-attempts + a fixed map of retryable Hibernate exceptions (stale state).
     * {@code traverseCauses=true} so wrapped exceptions still match.
     */
    @Bean
    public SimpleRetryPolicy retryPolicy() {
        Map<Class<? extends Throwable>, Boolean> retryable = new HashMap<>();
        retryable.put( org.hibernate.StaleObjectStateException.class, true );
        retryable.put( org.hibernate.StaleStateException.class, true );
        return new SimpleRetryPolicy( maxRetries, retryable, true );
    }

    /**
     * Retry advice that wraps service methods matching the
     * {@code Pointcuts.retryableOrTransactionalServiceMethod()} pointcut. Composed of an
     * exponential back-off and the {@link #retryPolicy()} above, plus the {@link RetryLogger}
     * listener for visibility.
     */
    @Bean
    public RetryOperationsInterceptor retryAdvice( SimpleRetryPolicy retryPolicy, RetryLogger retryLogger ) {
        RetryTemplate template = new RetryTemplate();
        template.setBackOffPolicy( new ExponentialBackOffPolicy() );
        template.setRetryPolicy( retryPolicy );
        template.registerListener( retryLogger );

        RetryOperationsInterceptor interceptor = new RetryOperationsInterceptor();
        interceptor.setRetryOperations( template );
        return interceptor;
    }

    /**
     * Bind the retry advice to the pointcut
     * {@code ubic.gemma.persistence.util.Pointcuts.retryableOrTransactionalServiceMethod()} with
     * {@code order=2} — outside the {@code @EnableTransactionManagement} advisor (order=3) — so a
     * stale-state failure rolls back the inner transaction before the retry advisor restarts the
     * call in a fresh tx. Equivalent to the XML {@code <aop:advisor pointcut-ref="retryable"
     * advice-ref="retryAdvice" order="2"/>}.
     */
    @Bean
    public org.springframework.aop.support.DefaultPointcutAdvisor retryAdvisor( RetryOperationsInterceptor retryAdvice ) {
        org.springframework.aop.aspectj.AspectJExpressionPointcut pc =
                new org.springframework.aop.aspectj.AspectJExpressionPointcut();
        pc.setExpression( "ubic.gemma.persistence.util.Pointcuts.retryableOrTransactionalServiceMethod()" );
        org.springframework.aop.support.DefaultPointcutAdvisor advisor =
                new org.springframework.aop.support.DefaultPointcutAdvisor( pc, retryAdvice );
        advisor.setOrder( 2 );
        return advisor;
    }

    // ---------------------------------------------------------------------------------------------
    // cacheManager alias (replaces <alias name="ehcache" alias="cacheManager"/>)
    // ---------------------------------------------------------------------------------------------

    /**
     * Register an alias {@code cacheManager} → {@code ehcache} so that Spring's
     * {@code @EnableCaching} infrastructure (which looks up a bean named {@code cacheManager} by
     * default) and any legacy injection point on the name {@code cacheManager} resolve to the
     * {@link org.springframework.cache.CacheManager} published by
     * {@link ubic.gemma.persistence.cache.EhcacheConfig} under the id {@code ehcache}.
     * <p>
     * Equivalent to the XML {@code <alias name="ehcache" alias="cacheManager"/>}. Implemented as a
     * static {@link BeanFactoryPostProcessor} so the alias is registered before any
     * {@code @EnableCaching} infrastructure tries to resolve it; the {@code static} qualifier on
     * the {@code @Bean} method is the documented Spring idiom for BFPPs declared inside a
     * {@code @Configuration} class.
     */
    @Bean
    public static BeanFactoryPostProcessor cacheManagerAliasRegistrar() {
        return beanFactory -> {
            if ( beanFactory instanceof DefaultListableBeanFactory ) {
                DefaultListableBeanFactory dlbf = ( DefaultListableBeanFactory ) beanFactory;
                if ( !dlbf.containsBean( "cacheManager" ) ) {
                    dlbf.registerAlias( "ehcache", "cacheManager" );
                }
            }
        };
    }
}
