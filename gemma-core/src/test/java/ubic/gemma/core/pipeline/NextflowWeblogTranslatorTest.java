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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.pipeline.NextflowWeblogTranslator.TranslatedEvent;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test for {@link NextflowWeblogTranslator}, asserting against <b>real captured</b>
 * {@code -with-weblog} payloads (Nextflow 24.10.3) under {@code data/pipeline/weblog/}. Fixtures were
 * captured by pointing {@code -with-weblog} at a throwaway sink and running tiny pipelines (a clean
 * success, a runtime {@code exit 3}, and a script/wiring error) — see
 * {@code docs/pipeline-compute/NEXTFLOW_DISPATCH_RESOLUTIONS.md} §"Weblog payload shape".
 */
class NextflowWeblogTranslatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private NextflowWeblogTranslator translator;

    @BeforeEach
    void setUp() {
        translator = new NextflowWeblogTranslator( objectMapper );
    }

    private String fixture( String name ) throws IOException {
        try ( InputStream is = getClass().getResourceAsStream( "/data/pipeline/weblog/" + name ) ) {
            assertThat( is ).as( "fixture %s on classpath", name ).isNotNull();
            return new String( is.readAllBytes(), StandardCharsets.UTF_8 );
        }
    }

    private JsonNode payloadOf( TranslatedEvent e ) throws IOException {
        return objectMapper.readTree( e.getPayloadJson() );
    }

    @Test
    void completedSuccess_mapsToCompleted() throws IOException {
        TranslatedEvent e = translator.translate( fixture( "completed_success.json" ) ).orElseThrow();
        assertThat( e.getKind() ).isEqualTo( "completed" );
        JsonNode p = payloadOf( e );
        assertThat( p.path( "event" ).asText() ).isEqualTo( "completed" );
        assertThat( p.has( "message" ) ).isFalse();           // no failure detail on success
        assertThat( p.has( "durationMs" ) ).isTrue();
    }

    @Test
    void completedRuntimeFailure_mapsToError_withExitStatusAndMessage() throws IOException {
        TranslatedEvent e = translator.translate( fixture( "completed_failure.json" ) ).orElseThrow();
        assertThat( e.getKind() ).isEqualTo( "error" );
        JsonNode p = payloadOf( e );
        assertThat( p.path( "exitStatus" ).asInt() ).isEqualTo( 3 );
        // errorReport is preferred over errorMessage; the report names the failing process.
        assertThat( p.path( "message" ).asText() ).contains( "RUN (beta)" );
        // No failureClass emitted → recordEvent defaults it to UNKNOWN (handoff D9).
        assertThat( p.has( "failureClass" ) ).isFalse();
    }

    @Test
    void completedScriptError_mapsToError_nullExitStatusFallsBackToReport() throws IOException {
        // Script/wiring error: exitStatus is null and errorMessage is null in the source — only
        // errorReport is populated. Exercises both the null-exitStatus and the message fallback.
        TranslatedEvent e = translator.translate( fixture( "completed_script_error.json" ) ).orElseThrow();
        assertThat( e.getKind() ).isEqualTo( "error" );
        JsonNode p = payloadOf( e );
        assertThat( p.has( "exitStatus" ) ).isFalse();        // null in source → omitted, no crash
        assertThat( p.path( "message" ).asText() ).isNotBlank();
    }

    @Test
    void processStarted_mapsToStage_withProcessAndTag() throws IOException {
        TranslatedEvent e = translator.translate( fixture( "process_started.json" ) ).orElseThrow();
        assertThat( e.getKind() ).isEqualTo( "stage" );
        JsonNode p = payloadOf( e );
        assertThat( p.path( "process" ).asText() ).isNotBlank();
        assertThat( p.has( "tag" ) ).isTrue();
        assertThat( p.has( "taskId" ) ).isTrue();
    }

    @Test
    void processCompletedOk_mapsToProgress() throws IOException {
        TranslatedEvent e = translator.translate( fixture( "process_completed_ok.json" ) ).orElseThrow();
        assertThat( e.getKind() ).isEqualTo( "progress" );
        JsonNode p = payloadOf( e );
        assertThat( p.path( "status" ).asText() ).isEqualTo( "COMPLETED" );
        assertThat( p.path( "exit" ).asInt() ).isEqualTo( 0 );
    }

    /**
     * The key correctness guard: a per-task {@code FAILED} is a {@code progress} snapshot, NOT a job
     * {@code error}. Job failure is decided solely by the workflow-level {@code completed} event —
     * because Nextflow may retry a failed task.
     */
    @Test
    void processCompletedFailed_mapsToProgress_notError() throws IOException {
        TranslatedEvent e = translator.translate( fixture( "process_completed_failed.json" ) ).orElseThrow();
        assertThat( e.getKind() ).isEqualTo( "progress" );
        JsonNode p = payloadOf( e );
        assertThat( p.path( "status" ).asText() ).isEqualTo( "FAILED" );
        assertThat( p.path( "exit" ).asInt() ).isEqualTo( 3 );
    }

    @Test
    void startedEvent_isIgnored() throws IOException {
        assertThat( translator.translate( fixture( "started.json" ) ) ).isEmpty();
    }

    @Test
    void bareErrorEvent_isIgnored() throws IOException {
        // The `error` event carries no detail — the job's failure comes from `completed`.
        assertThat( translator.translate( fixture( "error.json" ) ) ).isEmpty();
    }

    @Test
    void unknownEvent_isIgnored() {
        assertThat( translator.translate( "{\"event\":\"process_submitted\",\"runName\":\"x\"}" ) ).isEmpty();
        assertThat( translator.translate( "{\"event\":\"some_future_event\"}" ) ).isEmpty();
    }

    @Test
    void malformedOrEventless_throws() {
        assertThatThrownBy( () -> translator.translate( "not json" ) )
                .isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> translator.translate( "{\"runName\":\"x\"}" ) )
                .isInstanceOf( IllegalArgumentException.class );
    }
}
