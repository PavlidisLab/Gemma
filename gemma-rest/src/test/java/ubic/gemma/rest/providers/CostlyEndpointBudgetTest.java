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
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySources;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests for {@link CostlyEndpointBudget}; no Spring context, the property sources are
 * supplied directly.
 */
class CostlyEndpointBudgetTest {

    /**
     * Zero, so a refused acquire returns immediately instead of making every negative case wait.
     */
    private static final int NO_QUEUE = 0;

    private static CostlyEndpointBudget budget( Map<String, Object> properties ) {
        CostlyEndpointBudget budget = new CostlyEndpointBudget();
        MutablePropertySources sources = new MutablePropertySources();
        sources.addLast( new MapPropertySource( "test", properties ) );
        ReflectionTestUtils.setField( budget, "propertySources", ( PropertySources ) sources );
        budget.afterPropertiesSet();
        return budget;
    }

    private static CostlyEndpointBudget budget() {
        return budget( new HashMap<>() );
    }

    private static CostlyEndpointBudget budgetWith( String pool, int permits ) {
        Map<String, Object> properties = new HashMap<>();
        properties.put( "gemma.rest.budget." + pool + ".permits", String.valueOf( permits ) );
        return budget( properties );
    }

    @Test
    void poolsAreSizedFromTheDeclaredDefaults() {
        CostlyEndpointBudget budget = budget();
        int permits = CostlyEndpointBudget.POOLS.get( "search" );
        for ( int i = 0; i < permits; i++ ) {
            assertThat( budget.tryAcquire( "search", NO_QUEUE ) )
                    .withFailMessage( "permit %d of %d should have been available", i + 1, permits )
                    .isTrue();
        }
        assertThat( budget.tryAcquire( "search", NO_QUEUE ) ).isFalse();
    }

    @Test
    void aPropertyOverridesTheDeclaredDefault() {
        CostlyEndpointBudget budget = budgetWith( "search", 2 );
        assertThat( budget.tryAcquire( "search", NO_QUEUE ) ).isTrue();
        assertThat( budget.tryAcquire( "search", NO_QUEUE ) ).isTrue();
        assertThat( budget.tryAcquire( "search", NO_QUEUE ) ).isFalse();
    }

    @Test
    void poolsDoNotDrawOnEachOther() {
        CostlyEndpointBudget budget = budgetWith( "vectors", 1 );
        assertThat( budget.tryAcquire( "vectors", NO_QUEUE ) ).isTrue();
        assertThat( budget.tryAcquire( "vectors", NO_QUEUE ) ).isFalse();
        // A full matrix-download pool must not shut off search.
        assertThat( budget.tryAcquire( "search", NO_QUEUE ) ).isTrue();
    }

    @Test
    void releaseReturnsThePermit() {
        CostlyEndpointBudget budget = budgetWith( "vectors", 1 );
        assertThat( budget.tryAcquire( "vectors", NO_QUEUE ) ).isTrue();
        assertThat( budget.tryAcquire( "vectors", NO_QUEUE ) ).isFalse();
        budget.release( "vectors" );
        assertThat( budget.tryAcquire( "vectors", NO_QUEUE ) ).isTrue();
    }

    @Test
    void anUnknownPoolIsAProgrammingErrorNotARefusal() {
        assertThatThrownBy( () -> budget().tryAcquire( "no-such-pool", NO_QUEUE ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "no-such-pool" );
    }

    @Test
    void aPoolWithNoPermitsIsRejected() {
        Map<String, Object> properties = new HashMap<>();
        properties.put( "gemma.rest.budget.goterms.permits", "0" );
        assertThatThrownBy( () -> budget( properties ) )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "at least 1" );
    }

    @Test
    void releasingAPoolThatWasNeverAcquiredIsHarmless() {
        // The release path runs off a request property; if that property were ever set without a
        // matching acquire, silently widening the pool would be worse than doing nothing.
        CostlyEndpointBudget budget = budgetWith( "diffex", 1 );
        budget.release( "no-such-pool" );
        assertThat( budget.tryAcquire( "diffex", NO_QUEUE ) ).isTrue();
        assertThat( budget.tryAcquire( "diffex", NO_QUEUE ) ).isFalse();
    }
}
