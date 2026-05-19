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
package ubic.gemma.rest.util;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import ubic.gemma.core.context.BeanNameGenerator;
import ubic.gemma.core.context.TestComponent;

/**
 * Component-scan configuration for the gemma-rest module, replacing the XML
 * {@code <context:component-scan>} block previously declared in
 * {@code applicationContext-component-scan.xml}.
 * <p>
 * Scans {@code ubic.gemma.rest} for stereotype-annotated beans (@Component, @Service, @Repository,
 * @Controller, @Configuration, etc.) using Gemma's custom {@link BeanNameGenerator} (which strips
 * the "Impl" suffix from bean names) and excludes classes annotated with {@link TestComponent}
 * so the production context never picks up test-only beans.
 * <p>
 * Wired in {@code applicationContext-component-scan.xml} as a {@code <bean>} so the existing
 * wildcard XML loader ({@code classpath*:ubic/gemma/applicationContext-*.xml} — see
 * {@code SpringContextUtils} and {@code web.xml}) still discovers it without any classpath-order
 * changes.
 */
@Configuration
@ComponentScan(
        basePackages = "ubic.gemma.rest",
        nameGenerator = BeanNameGenerator.class,
        excludeFilters = @Filter(type = FilterType.ANNOTATION, classes = TestComponent.class)
)
public class RestComponentScanConfig {
}
