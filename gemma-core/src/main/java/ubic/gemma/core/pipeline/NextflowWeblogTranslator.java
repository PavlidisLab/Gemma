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
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Value;
import org.springframework.lang.Nullable;

import java.util.Optional;

/**
 * Translates one Nextflow {@code -with-weblog} message into Gemma's internal event vocabulary
 * ({@code stage}/{@code progress}/{@code completed}/{@code error}) so it can be handed to
 * {@link ubic.gemma.persistence.service.pipeline.PipelineJobBatchService#recordEvent}.
 *
 * <p>Pure, stateless, and scheduler-agnostic at the persistence boundary: the REST ingest parses the
 * raw weblog body, calls {@link #translate(String)}, and forwards any produced event to the service.
 * Under the one-run-per-EE model (R11) one Nextflow run maps to exactly one {@code PipelineJob}, so no
 * study-tag routing is needed — every message for a run belongs to that run's job.</p>
 *
 * <p>Payload shape pinned against Nextflow <b>24.10.3</b> (see
 * {@code docs/pipeline-compute/NEXTFLOW_DISPATCH_RESOLUTIONS.md} §"Weblog payload shape"). Key
 * decisions:</p>
 * <ul>
 *   <li><b>Terminal disposition comes from the workflow-level {@code completed} event's
 *       {@code metadata.workflow.success}</b> — never from the (empty) {@code error} event, never
 *       inferred from a per-task {@code FAILED} trace (a task can fail and be retried; only the run's
 *       {@code completed} is authoritative).</li>
 *   <li>The failure message prefers {@code errorReport} (always populated on failure), falling back to
 *       {@code errorMessage}.</li>
 *   <li>No {@code failureClass} is emitted — Nextflow's weblog doesn't classify failures, so
 *       {@code recordEvent} defaults it to {@code UNKNOWN} (handoff D9: the pipeline is the source of
 *       truth for the class; unclassified ⇒ UNKNOWN).</li>
 *   <li>{@code started}, {@code process_submitted}, and the bare {@code error} event are ignored
 *       (return {@link Optional#empty()}) — noise or premature; the job's live state is driven by the
 *       process/terminal events. Unknown event names are ignored (forward-compatible).</li>
 * </ul>
 */
public class NextflowWeblogTranslator {

    // Gemma internal event kinds (mirrors the literals in PipelineJobBatchServiceImpl.recordEvent).
    private static final String KIND_STAGE = "stage";
    private static final String KIND_PROGRESS = "progress";
    private static final String KIND_COMPLETED = "completed";
    private static final String KIND_ERROR = "error";

    private final ObjectMapper objectMapper;

    /** Uses a vanilla mapper — translation only parses raw JSON and emits compact JSON, no custom (de)serializers. */
    public NextflowWeblogTranslator() {
        this( new ObjectMapper() );
    }

    public NextflowWeblogTranslator( ObjectMapper objectMapper ) {
        this.objectMapper = objectMapper;
    }

    /**
     * Translate a raw Nextflow weblog message body.
     *
     * @param weblogJson the POST body Nextflow sent
     * @return the Gemma event to record, or empty if this message maps to nothing (ignored event)
     * @throws IllegalArgumentException if the body is not parseable JSON with an {@code event} field
     */
    public Optional<TranslatedEvent> translate( String weblogJson ) {
        JsonNode root;
        try {
            root = objectMapper.readTree( weblogJson );
        } catch ( Exception e ) {
            throw new IllegalArgumentException( "unparseable weblog body: " + e.getMessage(), e );
        }
        if ( root == null || !root.hasNonNull( "event" ) ) {
            throw new IllegalArgumentException( "weblog body has no 'event' field" );
        }
        String event = root.get( "event" ).asText();
        String runName = text( root, "runName" );
        switch ( event ) {
            case "completed":
                return Optional.of( translateCompleted( root, runName ) );
            case "process_started":
                return Optional.of( translateProcessStarted( root.path( "trace" ) ) );
            case "process_completed":
                return Optional.of( translateProcessCompleted( root.path( "trace" ) ) );
            // started / process_submitted / error (empty) / anything unknown → ignored.
            default:
                return Optional.empty();
        }
    }

    /** Workflow terminal — the authoritative success/failure signal. */
    private TranslatedEvent translateCompleted( JsonNode root, @Nullable String runName ) {
        JsonNode wf = root.path( "metadata" ).path( "workflow" );
        boolean success = wf.path( "success" ).asBoolean( false );
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put( "source", "nextflow" );
        payload.put( "event", "completed" );
        if ( runName != null ) {
            payload.put( "runName", runName );
        }
        if ( wf.hasNonNull( "duration" ) ) {
            payload.put( "durationMs", wf.get( "duration" ).asLong() );
        }
        if ( success ) {
            return new TranslatedEvent( KIND_COMPLETED, write( payload ) );
        }
        // Failure: exitStatus may be null; message prefers errorReport, then errorMessage.
        if ( wf.hasNonNull( "exitStatus" ) ) {
            payload.put( "exitStatus", wf.get( "exitStatus" ).asInt() );
        }
        String message = firstNonBlank( text( wf, "errorReport" ), text( wf, "errorMessage" ) );
        if ( message != null ) {
            payload.put( "message", message );
        }
        return new TranslatedEvent( KIND_ERROR, write( payload ) );
    }

    /** A task began running → a durable stage marker (also flips QUEUED→RUNNING in recordEvent). */
    private TranslatedEvent translateProcessStarted( JsonNode trace ) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put( "source", "nextflow" );
        payload.put( "event", "process_started" );
        putIfPresent( payload, "process", trace, "process" );
        putIfPresent( payload, "tag", trace, "tag" );
        putIfPresent( payload, "name", trace, "name" );
        if ( trace.hasNonNull( "task_id" ) ) {
            payload.put( "taskId", trace.get( "task_id" ).asLong() );
        }
        return new TranslatedEvent( KIND_STAGE, write( payload ) );
    }

    /**
     * A task finished → a snapshot-only progress update (recordEvent does NOT persist {@code progress}
     * as a row, so per-task completions keep the live snapshot fresh without spamming the timeline).
     * A per-task {@code FAILED} is deliberately NOT a job failure here — that waits for the workflow
     * {@code completed} event (the task may be retried).
     */
    private TranslatedEvent translateProcessCompleted( JsonNode trace ) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put( "source", "nextflow" );
        payload.put( "event", "process_completed" );
        putIfPresent( payload, "process", trace, "process" );
        putIfPresent( payload, "tag", trace, "tag" );
        putIfPresent( payload, "status", trace, "status" );
        if ( trace.hasNonNull( "exit" ) ) {
            payload.put( "exit", trace.get( "exit" ).asInt() );
        }
        return new TranslatedEvent( KIND_PROGRESS, write( payload ) );
    }

    // ---- helpers -----------------------------------------------------------

    @Nullable
    private static String text( JsonNode node, String field ) {
        JsonNode v = node.get( field );
        return v != null && v.isTextual() ? v.asText() : null;
    }

    private static void putIfPresent( ObjectNode payload, String outKey, JsonNode src, String field ) {
        String v = text( src, field );
        if ( v != null ) {
            payload.put( outKey, v );
        }
    }

    @Nullable
    private static String firstNonBlank( @Nullable String a, @Nullable String b ) {
        if ( a != null && !a.isBlank() ) {
            return a;
        }
        return b != null && !b.isBlank() ? b : null;
    }

    private String write( ObjectNode node ) {
        try {
            return objectMapper.writeValueAsString( node );
        } catch ( Exception e ) {
            // ObjectNode serialization can't realistically fail; surface loudly if it ever does.
            throw new IllegalStateException( "failed to serialize translated weblog payload", e );
        }
    }

    /** The Gemma-side event a weblog message maps to: a {@code kind} and its JSON payload. */
    @Value
    public static class TranslatedEvent {
        String kind;
        String payloadJson;
    }
}
