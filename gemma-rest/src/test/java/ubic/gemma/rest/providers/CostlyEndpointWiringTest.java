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
package ubic.gemma.rest.providers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the wiring that the rest of the mechanism assumes.
 * <p>
 * {@link CostlyEndpointFilter} tolerates a missing {@link CostlyEndpointBudget} bean so that the
 * mocked Spring contexts in this module keep working, which means a wiring mistake would not fail any
 * other test — the budgets would just quietly stop bounding anything. This test is what would catch
 * that: it asks Spring's own scanner the question the WAR asks at boot.
 */
class CostlyEndpointWiringTest {

    /**
     * Scans the package {@code <context:component-scan base-package="ubic.gemma.rest"/>} covers, asking
     * only about one type.
     * <p>
     * The default filters are off deliberately: left on, they match every {@code @Component} under
     * {@code ubic.gemma.rest} and the answer says nothing about the type asked for. A candidate here is
     * therefore a class Spring's scanner accepts <em>and</em> that is assignable to {@code type}.
     */
    private static Set<BeanDefinition> scanFor( Class<?> type ) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider( false );
        scanner.addIncludeFilter( new AssignableTypeFilter( type ) );
        return scanner.findCandidateComponents( "ubic.gemma.rest" );
    }

    @Test
    void theBudgetIsPickedUpByTheComponentScanTheWarRuns() {
        assertThat( scanFor( CostlyEndpointBudget.class ) )
                .withFailMessage( "CostlyEndpointBudget is not a scannable component, so @Costly routes "
                        + "would run unbounded in the deployed WAR" )
                .hasSize( 1 );
    }

    @Test
    void bothHalvesOfTheMechanismArePickedUpToo() {
        // The acquire and the release have to be registered together: one without the other either
        // bounds nothing or never gives a permit back.
        assertThat( scanFor( CostlyEndpointFilter.class ) ).hasSize( 1 );
        assertThat( scanFor( CostlyEndpointApplicationEventListener.class ) ).hasSize( 1 );
    }
}
