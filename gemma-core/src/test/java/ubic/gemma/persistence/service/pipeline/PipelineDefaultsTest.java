/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.pipeline;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link PipelineDefaults} — the per-pipeline dispatch defaults (O8).
 */
class PipelineDefaultsTest {

    @Test
    void scAnnotation_defaultsTo25() {
        assertThat( new PipelineDefaults( 25 ).maxConcurrentFor( PipelineDefaults.SC_ANNOTATION ) ).isEqualTo( 25 );
    }

    @Test
    void override_isRespected() {
        assertThat( new PipelineDefaults( 50 ).maxConcurrentFor( PipelineDefaults.SC_ANNOTATION ) ).isEqualTo( 50 );
    }

    @Test
    void unknownOrNullPipeline_hasNoDefault() {
        PipelineDefaults d = new PipelineDefaults( 25 );
        assertThat( d.maxConcurrentFor( "test-pipeline" ) ).isNull();   // ⇒ unlimited, unchanged
        assertThat( d.maxConcurrentFor( null ) ).isNull();
    }
}
