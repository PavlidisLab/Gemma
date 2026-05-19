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
package ubic.gemma.core.config;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.logging.Log4j2Metrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.jmx.JmxMeterRegistry;
import io.micrometer.jmx.JmxConfig;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.core.metrics.GenericMeterRegistryConfigurer;
import ubic.gemma.core.metrics.binder.GenericTaskExecutorMetrics;
import ubic.gemma.core.metrics.binder.database.HikariCPMetrics;
import ubic.gemma.core.metrics.binder.jpa.Hibernate4Metrics;
import ubic.gemma.core.metrics.binder.jpa.Hibernate4QueryMetrics;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Phase 3 XML-&gt;Java migration: replaces the {@code <beans profile="metrics">} block from
 * {@code applicationContext-serviceBeans.xml}. Activated only when the {@code metrics} Spring
 * profile is set; otherwise none of these beans are instantiated.
 * <p>
 * The legacy {@code MeterRegistryEhcacheConfigurer} (Ehcache 2.x API) was deleted in Phase 2
 * along with the rest of the Ehcache 2 stack, which left the {@code metrics} profile broken
 * until Phase 3. It is replaced here by {@link #meterRegistryJCacheConfigurer} which binds
 * per-cache JSR-107 metrics from the new Ehcache 3 / JCache backend.
 */
@Configuration
@Profile("metrics")
@EnableAspectJAutoProxy
public class MetricsConfig {

    @Bean
    public MeterRegistry meterRegistry() {
        // XML used <constructor-arg value="DEFAULT"/> + <constructor-arg value="SYSTEM"/> which
        // selected the JmxMeterRegistry.JmxConfig static field + Clock.SYSTEM enum value. The
        // typed JmxMeterRegistry constructor in micrometer-registry-jmx takes (JmxConfig, Clock);
        // pass the same DEFAULT / SYSTEM equivalents here.
        return new JmxMeterRegistry( JmxConfig.DEFAULT, io.micrometer.core.instrument.Clock.SYSTEM );
    }

    /**
     * Aggregate all the {@link MeterBinder}s the legacy XML registered (basic JVM metrics,
     * logging metrics, two Hibernate session-factory binders, one Hikari pool binder, one
     * task-executor binder) plus the {@link TaskRunningService} bean (which itself implements
     * {@link MeterBinder}). Order preserved from the XML.
     */
    @Bean
    public GenericMeterRegistryConfigurer genericMeterRegistryConfigurer(
            MeterRegistry meterRegistry,
            SessionFactory sessionFactory,
            DataSource dataSource,
            TaskExecutor taskExecutor,
            TaskRunningService taskRunningService ) {
        List<MeterBinder> binders = new ArrayList<>();
        // basic JVM metrics
        binders.add( new ClassLoaderMetrics() );
        binders.add( new JvmMemoryMetrics() );
        binders.add( new ProcessorMetrics() );
        binders.add( new JvmThreadMetrics() );
        // logging metrics
        binders.add( new Log4j2Metrics() );
        // database metrics
        binders.add( new Hibernate4Metrics( sessionFactory, "sessionFactory", Collections.emptyList() ) );
        binders.add( new Hibernate4QueryMetrics( sessionFactory, "sessionFactory", Collections.emptyList() ) );
        binders.add( new HikariCPMetrics( (HikariDataSource) dataSource ) );
        // task-executor metrics
        GenericTaskExecutorMetrics localTasksMetrics = new GenericTaskExecutorMetrics( taskExecutor );
        localTasksMetrics.setPoolName( "gemmaLocalTasks" );
        binders.add( localTasksMetrics );
        // job-submission service is itself a MeterBinder
        binders.add( taskRunningService );
        return new GenericMeterRegistryConfigurer( meterRegistry, binders );
    }

    /**
     * Enables {@code @Timed}-annotated method timing. The XML had a sibling
     * {@code <aop:aspectj-autoproxy/>}; replaced here by the class-level
     * {@link EnableAspectJAutoProxy} annotation.
     */
    @Bean
    public TimedAspect timedAspect( MeterRegistry meterRegistry ) {
        return new TimedAspect( meterRegistry );
    }

    /**
     * Per-cache JSR-107 metrics (Ehcache 3 / JCache backend, see {@code EhcacheConfig}).
     * Replaces the {@code MeterRegistryJCacheConfigurer} XML bean previously declared in
     * applicationContext-serviceBeans.xml.
     */
    @Bean
    public ubic.gemma.core.metrics.MeterRegistryJCacheConfigurer meterRegistryJCacheConfigurer(
            MeterRegistry meterRegistry,
            javax.cache.CacheManager jCacheCacheManager ) {
        return new ubic.gemma.core.metrics.MeterRegistryJCacheConfigurer( meterRegistry, jCacheCacheManager );
    }
}
