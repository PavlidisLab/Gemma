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

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import ubic.gemma.core.context.BeanNameGenerator;
import ubic.gemma.core.context.TestComponent;

/**
 * Phase 3 XML-&gt;Java migration: replaces {@code applicationContext-component-scan.xml}.
 * <p>
 * Defines the component-scan that pulls in every {@code @Component} / {@code @Service} /
 * {@code @Repository} / {@code @Controller} / {@code @Configuration} bean under
 * {@code ubic.gemma.core} and {@code ubic.gemma.persistence}. Beans annotated
 * {@link TestComponent} are excluded — those are picked up only by test contexts.
 * <p>
 * The {@code ubic.gemma.web} package is deliberately not scanned here; it is brought in by
 * {@code gemma-servlet.xml} in the {@code gemma-web} module.
 * <p>
 * Bean ids are produced by {@link BeanNameGenerator} (camel-case, strips trailing
 * "Impl") — matching the legacy XML's {@code name-generator} attribute exactly.
 * <p>
 * Bootstrapped from the thin {@code applicationContext-component-scan.xml} stub that
 * declares {@code <context:annotation-config/>} plus {@code <bean class="...ComponentScanConfig"/>}.
 * The stub is required because the XML wildcard loader
 * ({@code classpath*:ubic/gemma/applicationContext-*.xml}) needs at least one XML entry point;
 * once this {@code @Configuration} is registered, its {@code @ComponentScan} handles the rest.
 */
@Configuration
@ComponentScan(
        basePackages = { "ubic.gemma.core", "ubic.gemma.persistence" },
        nameGenerator = BeanNameGenerator.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ANNOTATION,
                classes = TestComponent.class ) )
public class ComponentScanConfig {
}
