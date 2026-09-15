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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import ubic.gemma.core.metrics.binder.VirtualThreadExecutorMetrics;
import ubic.gemma.persistence.util.EntityUrlBuilder;

/**
 * Phase 3 XML-&gt;Java migration: replaces the default-profile beans from
 * {@code applicationContext-serviceBeans.xml} — {@code entityUrlBuilder}, {@code taskExecutor},
 * and {@code expressionDataFileTaskExecutor}.
 * <p>
 * Bean ids are preserved exactly because other code (XML wiring elsewhere, {@code @Qualifier}
 * references, and {@code applicationContext.getBean("taskExecutor")} call sites) resolves them
 * by name.
 * <p>
 * The {@code metrics} profile beans from the original XML are migrated separately in
 * {@link MetricsConfig}. The dead {@code MeterRegistryEhcacheConfigurer} bean (whose backing
 * class was removed in the Phase 2 EhCache retirement) is intentionally not carried over —
 * the {@code metrics} profile is opt-in and was never being instantiated anyway.
 */
@Configuration
public class ServiceBeansConfig {

    /**
     * Marked {@link Primary} because gsec's XML also defines an {@code entityUrlBuilder} bean
     * (with a different host-url source); the legacy XML used {@code primary="true"} so that
     * Gemma's host-url-configured builder wins over gsec's at injection sites that ask for the
     * type without qualifier.
     */
    @Bean
    @Primary
    public EntityUrlBuilder entityUrlBuilder( @Value("${gemma.hosturl}") String hostUrl ) {
        return new EntityUrlBuilder( hostUrl );
    }

    /**
     * Local short-lived task executor. Marked {@link Primary} so that injection points asking
     * for {@code TaskExecutor} (without a qualifier) get this one rather than
     * {@code expressionDataFileTaskExecutor}.
     */
    @Bean(name = "taskExecutor")
    @Primary
    public ThreadPoolTaskExecutor taskExecutor( @Value("${gemma.localTasks.corePoolSize}") int corePoolSize ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix( "gemma-local-tasks-thread-" );
        executor.setCorePoolSize( corePoolSize );
        return executor;
    }

    /**
     * Executor for generating data files (CEL / count matrices / TSVs).
     * <p>
     * JDK 21 migration: backed by a virtual-thread-per-task executor wrapped through Spring's
     * {@link ConcurrentTaskExecutor} so it still implements
     * {@link AsyncTaskExecutor} (callers {@code execute}/{@code submit} via that interface).
     * <p>
     * Behavioural change vs. the previous {@link ThreadPoolTaskExecutor}: submission never blocks
     * and never throws {@code TaskRejectedException}, because there is no bounded queue and no
     * fixed worker pool. Concurrency is bounded only by the number of in-flight VTs the JVM is
     * willing to mount. The {@code gemma.expressionDataFileTasks.corePoolSize} and
     * {@code .queueCapacity} properties no longer constrain this executor. The two consumers
     * (DifferentialExpressionAnalyzerServiceImpl, ExpressionDataFileServiceImpl) both treat
     * {@code TaskRejectedException} as a soft failure ("archive will be rebuilt on next read"),
     * so dropping queue-based backpressure is safe.
     */
    @Bean(name = "expressionDataFileTaskExecutor")
    public AsyncTaskExecutor expressionDataFileTaskExecutor( VirtualThreadExecutorMetrics expressionDataFileTaskExecutorMetrics ) {
        return new ConcurrentTaskExecutor( expressionDataFileTaskExecutorMetrics.wrap(
                ubic.gemma.core.util.concurrent.Executors.newVirtualThreadPerTaskExecutorIfAvailable() ) );
    }

    /**
     * Micrometer binder for the {@link #expressionDataFileTaskExecutor} VT executor. Picked up by
     * {@code MetricsConfig#genericMeterRegistryConfigurer} when the {@code metrics} profile is active.
     * Defined as its own bean so the wrap-with-metrics path is exercised regardless of profile
     * (a no-op {@code bindTo} when no registry is calling it).
     */
    @Bean
    public VirtualThreadExecutorMetrics expressionDataFileTaskExecutorMetrics() {
        return new VirtualThreadExecutorMetrics( "expressionDataFileTaskExecutor" );
    }
}
