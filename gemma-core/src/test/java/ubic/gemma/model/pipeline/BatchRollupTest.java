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
package ubic.gemma.model.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the {@link BatchRollup} wire contract shared with the curation UI (UIB): the JSON keys must
 * stay snake_case (§1.3). The rollup <em>math</em> is exercised end-to-end against the DB in
 * {@code PipelineJobRetryMockIT}; here we only lock the serialized shape.
 */
class BatchRollupTest {

    @Test
    void serializesToSnakeCaseWireKeys() throws Exception {
        BatchRollup r = new BatchRollup();
        r.total = 12;
        r.done = 10;
        r.failed = 2;
        r.failedRetryable = 1;
        r.failedPermanent = 1;
        r.needsAttention = true;
        r.terminal = false;

        String json = new ObjectMapper().writeValueAsString( r );

        assertThat( json )
                .contains( "\"failed_retryable\":1" )
                .contains( "\"failed_permanent\":1" )
                .contains( "\"needs_attention\":true" )
                .contains( "\"terminal\":false" )
                .contains( "\"total\":12" )
                // camelCase must NOT leak onto the wire
                .doesNotContain( "failedRetryable" )
                .doesNotContain( "needsAttention" );
    }
}
