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
package ubic.gemma.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.pipeline.BatchScenario;
import ubic.gemma.core.pipeline.Scenario;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the canonical scenario fixtures under {@code pipeline-scenarios/} — the shared
 * Gemma↔UIB contract. Parsing them here means a change to the wire shape breaks a Gemma
 * test rather than silently drifting from what UIB expects.
 */
class PipelineScenarioFixturesTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private <T> T load( String name, Class<T> type ) throws Exception {
        try ( InputStream is = getClass().getResourceAsStream( "/pipeline-scenarios/" + name ) ) {
            assertThat( is ).as( "fixture %s exists", name ).isNotNull();
            return mapper.readValue( is, type );
        }
    }

    @Test
    void succeedFast() throws Exception {
        Scenario s = load( "SUCCEED_FAST.json", Scenario.class );
        assertThat( s.outcome ).isEqualTo( Scenario.Outcome.SUCCEED );
        assertThat( s.transport ).isEqualTo( Scenario.Transport.PUSH );
        assertThat( s.stages ).extracting( st -> st.kind ).containsExactly( "stage", "progress", "completed" );
    }

    @Test
    void allTransient() throws Exception {
        Scenario s = load( "ALL_TRANSIENT.json", Scenario.class );
        assertThat( s.outcome ).isEqualTo( Scenario.Outcome.FAIL );
        assertThat( s.failureClass ).isEqualTo( Scenario.FailureClass.TRANSIENT );
        assertThat( s.stages.get( s.stages.size() - 1 ).kind ).isEqualTo( "error" );
        assertThat( s.stages.get( s.stages.size() - 1 ).payloadJson ).contains( "TRANSIENT" );
    }

    @Test
    void permanentReject() throws Exception {
        Scenario s = load( "PERMANENT_REJECT.json", Scenario.class );
        assertThat( s.outcome ).isEqualTo( Scenario.Outcome.FAIL );
        assertThat( s.failureClass ).isEqualTo( Scenario.FailureClass.PERMANENT );
    }

    @Test
    void stallThenReconcile() throws Exception {
        Scenario s = load( "STALL_THEN_RECONCILE.json", Scenario.class );
        assertThat( s.outcome ).isEqualTo( Scenario.Outcome.STALL );
        assertThat( s.transport ).isEqualTo( Scenario.Transport.POLL );
        // A stall never scripts a terminal stage — poll() stays RUNNING forever.
        assertThat( s.stages ).noneSatisfy( st ->
                assertThat( st.kind ).isIn( "completed", "error", "killed" ) );
    }

    @Test
    void partialBatch() throws Exception {
        BatchScenario b = load( "PARTIAL_BATCH.json", BatchScenario.class );
        assertThat( b.failEveryNth ).isEqualTo( 3 );
        assertThat( b.fail.outcome ).isEqualTo( Scenario.Outcome.FAIL );
        assertThat( b.succeed.outcome ).isEqualTo( Scenario.Outcome.SUCCEED );
        // Every 3rd ordinal fails: 0,1 succeed; 2 fails.
        assertThat( b.scenarioForOrdinal( 0 ) ).isSameAs( b.succeed );
        assertThat( b.scenarioForOrdinal( 2 ) ).isSameAs( b.fail );
    }
}
