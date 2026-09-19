/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.rest.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Edge cases of {@link OpenApiFactory#stripQuality(String)} the generated spec does not happen to
 * exercise. The spec only ever shows one shape of the problem, so the ones it does not reach —
 * a bare type, a lone quality parameter, an uppercase spelling — are pinned here.
 */
class OpenApiFactoryTest {

    @Test
    void aMediaTypeWithNoParametersIsUnchanged() {
        assertThat( OpenApiFactory.stripQuality( "application/json" ) ).isEqualTo( "application/json" );
    }

    @Test
    void charsetSurvivesAndTheQualityGoes() {
        // the shape swagger-core emits from @Produces(TEXT_TAB_SEPARATED_VALUES_UTF8 + ";qs=0.9")
        assertThat( OpenApiFactory.stripQuality( "text/tab-separated-values; charset=UTF-8; q=0.9" ) )
                .isEqualTo( "text/tab-separated-values; charset=UTF-8" );
    }

    @Test
    void spacingIsNormalizedSoTheTwoSpellingsCollapseToOneKey() {
        // the same response carried "; q=0.9" and ";q=0.9" on different status codes
        assertThat( OpenApiFactory.stripQuality( "text/tab-separated-values; charset=UTF-8;q=0.9" ) )
                .isEqualTo( OpenApiFactory.stripQuality( "text/tab-separated-values; charset=UTF-8; q=0.9" ) );
    }

    @Test
    void aLoneQualityParameterLeavesTheBareType() {
        assertThat( OpenApiFactory.stripQuality( "text/plain;q=0.5" ) ).isEqualTo( "text/plain" );
        assertThat( OpenApiFactory.stripQuality( "text/plain;qs=0.5" ) ).isEqualTo( "text/plain" );
    }

    @Test
    void theParameterNameIsMatchedWithoutRegardToCase() {
        assertThat( OpenApiFactory.stripQuality( "text/plain; Q=0.5" ) ).isEqualTo( "text/plain" );
        assertThat( OpenApiFactory.stripQuality( "text/plain; QS=0.5" ) ).isEqualTo( "text/plain" );
    }

    @Test
    void aParameterMerelyStartingWithQIsKept() {
        // 'quality' is not 'q'; a prefix match here would silently drop a real parameter
        assertThat( OpenApiFactory.stripQuality( "text/plain; quality=high" ) )
                .isEqualTo( "text/plain; quality=high" );
    }

    @Test
    void theVersionParameterOnThePrometheusScrapeIsKept() {
        assertThat( OpenApiFactory.stripQuality( "text/plain; version=0.0.4; charset=utf-8" ) )
                .isEqualTo( "text/plain; version=0.0.4; charset=utf-8" );
    }
}
