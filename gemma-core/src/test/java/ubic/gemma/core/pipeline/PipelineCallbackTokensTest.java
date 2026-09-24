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
package ubic.gemma.core.pipeline;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PipelineCallbackTokensTest {

    @Test
    void tokenIsStablePerJobAndDiffersAcrossJobsAndSecrets() {
        String t7 = PipelineCallbackTokens.forJob( "s3cret", 7L );
        assertThat( t7 ).matches( "[0-9a-f]{64}" );
        assertThat( PipelineCallbackTokens.forJob( "s3cret", 7L ) ).isEqualTo( t7 );
        assertThat( PipelineCallbackTokens.forJob( "s3cret", 8L ) ).isNotEqualTo( t7 );
        assertThat( PipelineCallbackTokens.forJob( "other", 7L ) ).isNotEqualTo( t7 );
    }

    @Test
    void matchesOnlyTheJobItWasMintedFor() {
        String t7 = PipelineCallbackTokens.forJob( "s3cret", 7L );
        assertThat( PipelineCallbackTokens.matches( "s3cret", 7L, t7 ) ).isTrue();
        assertThat( PipelineCallbackTokens.matches( "s3cret", 8L, t7 ) ).isFalse();
        assertThat( PipelineCallbackTokens.matches( "s3cret", 7L, "s3cret" ) ).isFalse();
        assertThat( PipelineCallbackTokens.matches( "s3cret", 7L, null ) ).isFalse();
    }

    @Test
    void unconfiguredSecretRejectsEverythingAndMintsNothing() {
        assertThat( PipelineCallbackTokens.matches( "", 7L, "anything" ) ).isFalse();
        assertThat( PipelineCallbackTokens.matches( null, 7L, "anything" ) ).isFalse();
        assertThatThrownBy( () -> PipelineCallbackTokens.forJob( " ", 7L ) )
                .isInstanceOf( IllegalArgumentException.class );
    }
}
