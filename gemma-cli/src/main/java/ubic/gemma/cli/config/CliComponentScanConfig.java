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
package ubic.gemma.cli.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;
import ubic.gemma.cli.util.CLI;
import ubic.gemma.cli.util.PrototypeScopeResolver;
import ubic.gemma.core.context.BeanNameGenerator;
import ubic.gemma.core.context.LazyInitByDefaultPostProcessor;
import ubic.gemma.core.context.TestComponent;

/**
 * Component-scan configuration for the gemma-cli module, replacing the XML
 * {@code <context:component-scan>} blocks previously declared in
 * {@code applicationContext-component-scan.xml}.
 * <p>
 * Three scans, matching the legacy XML exactly:
 * <ol>
 *     <li>{@code ubic.gemma.cli} — standard stereotype scan with the {@link TestComponent}
 *         exclude filter. Picks up CLI-specific {@code @Component}/{@code @Service}/etc. beans.</li>
 *     <li>{@code ubic.gemma.apps} — restricted scan: default filters off, include filter on
 *         the {@link CLI} interface (assignable type), with {@link PrototypeScopeResolver}
 *         so each CLI tool is created per-invocation rather than as a singleton. This is
 *         how the in-tree CLI tools get registered.</li>
 *     <li>{@code ubic.gemma.contrib.apps} — identical to (2), for out-of-tree CLI tools
 *         dropped into the contrib classpath.</li>
 * </ol>
 * All three use Gemma's custom {@link BeanNameGenerator} (camel-case, strips trailing
 * "Impl") to match the legacy XML's {@code name-generator} attribute.
 * <p>
 * Also re-declares the {@link LazyInitByDefaultPostProcessor} bean that the XML defined
 * as a bare {@code <bean class="..."/>}; the post-processor must be returned from a
 * {@code static} {@code @Bean} method so the container instantiates it before the
 * @Configuration class is fully populated (it's a {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor}).
 * <p>
 * The whole configuration is gated on the {@code cli} profile, matching the legacy XML's
 * {@code profile="cli"} attribute, so the CLI scans don't fire in web/REST contexts.
 * <p>
 * Wired in {@code applicationContext-component-scan.xml} as a {@code <bean>} so the
 * existing wildcard XML loader ({@code classpath*:ubic/gemma/applicationContext-*.xml} —
 * see {@code SpringContextUtils} and the various CLI bootstrap paths) still discovers it
 * without classpath-order surgery.
 */
@Configuration
@Profile("cli")
@ComponentScans({
        // CLI-specific components
        @ComponentScan(
                basePackages = "ubic.gemma.cli",
                nameGenerator = BeanNameGenerator.class,
                excludeFilters = @Filter(type = FilterType.ANNOTATION, classes = TestComponent.class)
        ),
        // Standard CLI tools (assignable to CLI, instantiated per-invocation)
        @ComponentScan(
                basePackages = "ubic.gemma.apps",
                nameGenerator = BeanNameGenerator.class,
                useDefaultFilters = false,
                scopeResolver = PrototypeScopeResolver.class,
                includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CLI.class),
                excludeFilters = @Filter(type = FilterType.ANNOTATION, classes = TestComponent.class)
        ),
        // CLI tools from contrib packages
        @ComponentScan(
                basePackages = "ubic.gemma.contrib.apps",
                nameGenerator = BeanNameGenerator.class,
                useDefaultFilters = false,
                scopeResolver = PrototypeScopeResolver.class,
                includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CLI.class),
                excludeFilters = @Filter(type = FilterType.ANNOTATION, classes = TestComponent.class)
        )
})
public class CliComponentScanConfig {

    /**
     * Re-creates the bare {@code <bean class="ubic.gemma.core.context.LazyInitByDefaultPostProcessor"/>}
     * from the legacy XML. Must be {@code static} because the bean is a
     * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor} and Spring
     * needs to instantiate it before the enclosing @Configuration is processed.
     */
    @Bean
    public static LazyInitByDefaultPostProcessor lazyInitByDefaultPostProcessor() {
        return new LazyInitByDefaultPostProcessor();
    }
}
