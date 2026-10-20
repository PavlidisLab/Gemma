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
package ubic.gemma.cli.pipeline;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * Reports CLI progress to the Gemma pipeline framework when this process was
 * launched as a child of a pipeline job. Three environment variables wire the
 * contract:
 *
 * <ul>
 *   <li>{@code GEMMA_JOB_ID} — Gemma-side job id (the {@code PipelineJob.id});
 *       enables reporting when present, no-op when absent.</li>
 *   <li>{@code GEMMA_PIPELINE_CALLBACK_URL} — base URL of gemma-rest's
 *       internal callback endpoint
 *       (e.g. {@code https://gemma.example.com/rest/v2/internal/pipeline}).</li>
 *   <li>{@code GEMMA_PIPELINE_CALLBACK_TOKEN} — shared bearer secret for the
 *       internal endpoint. Resolve from a secrets store before launching the
 *       CLI; never bake into a script.</li>
 * </ul>
 *
 * <p>When {@code GEMMA_JOB_ID} is unset the reporter is a no-op singleton
 * ({@link #NOOP}) — standalone CLI invocations remain unaffected.</p>
 *
 * <p>Failures to reach the callback endpoint are logged at WARN but never
 * propagate — the pipeline framework's reconciler poll loop will eventually
 * close gaps from missed pushes, so a transient network failure shouldn't
 * fail the underlying CLI work.</p>
 */
public final class PipelineJobReporter {

    private static final Log log = LogFactory.getLog( PipelineJobReporter.class );

    private static final Duration TIMEOUT = Duration.ofSeconds( 10 );

    public static final PipelineJobReporter NOOP = new PipelineJobReporter( null, null, null );

    private final String jobId;
    private final String callbackBaseUrl;
    private final String token;
    private final HttpClient http;

    private PipelineJobReporter( String jobId, String callbackBaseUrl, String token ) {
        this.jobId = jobId;
        this.callbackBaseUrl = callbackBaseUrl;
        this.token = token;
        this.http = ( jobId != null )
                ? HttpClient.newBuilder().connectTimeout( TIMEOUT ).build()
                : null;
    }

    /**
     * Resolve a reporter from the process environment. Returns {@link #NOOP}
     * when {@code GEMMA_JOB_ID} is absent — standalone CLI invocations get a
     * no-op reporter without any conditionals at call sites.
     */
    public static PipelineJobReporter fromEnv() {
        String jobId = System.getenv( "GEMMA_JOB_ID" );
        if ( jobId == null || jobId.isBlank() ) {
            return NOOP;
        }
        String url = System.getenv( "GEMMA_PIPELINE_CALLBACK_URL" );
        String token = System.getenv( "GEMMA_PIPELINE_CALLBACK_TOKEN" );
        if ( url == null || url.isBlank() || token == null || token.isBlank() ) {
            log.warn( "GEMMA_JOB_ID is set but GEMMA_PIPELINE_CALLBACK_URL or _TOKEN is missing; reporter inactive" );
            return NOOP;
        }
        return new PipelineJobReporter( jobId, url, token );
    }

    public boolean isActive() {
        return jobId != null;
    }

    /**
     * Report a stage boundary (e.g. {@code "loading-vectors"}, {@code "running-anova"}).
     * Service-side this transitions QUEUED → RUNNING on first arrival.
     * Non-terminal: fire-and-forget.
     */
    public void stage( String phase ) {
        post( "stage", Map.of( "phase", phase ), false );
    }

    /**
     * Report a progress sample. {@code percent} may be {@code null} when the
     * CLI knows step-level progress but not percent-complete.
     * Non-terminal: fire-and-forget.
     */
    public void progress( Integer percent, String message ) {
        Map<String, Object> body = ( percent != null )
                ? Map.of( "percent", percent, "message", message != null ? message : "" )
                : Map.of( "message", message != null ? message : "" );
        post( "progress", body, false );
    }

    /**
     * Terminal success. Sync-wait so the event reaches the server before the
     * JVM exits. Caller should not emit any further events after this.
     */
    public void completed() {
        post( "completed", Map.of(), true );
    }

    /**
     * Terminal failure. Sync-wait so the event reaches the server before the
     * JVM exits. Caller should not emit any further events after this.
     */
    public void error( Throwable t ) {
        post( "error", Map.of(
                "message", t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName(),
                "class", t.getClass().getName() ), true );
    }

    /**
     * Terminal cancelled. Sync-wait so the event reaches the server before
     * the JVM exits. Use when the CLI catches an interrupt + wants to report
     * cooperative shutdown.
     */
    public void killed( String reason ) {
        post( "killed", Map.of( "reason", reason != null ? reason : "" ), true );
    }

    private void post( String kind, Map<String, ?> payload, boolean terminal ) {
        if ( !isActive() ) {
            return;
        }
        // Non-terminal events: fire-and-forget. A slow or unreachable
        // gemma-rest must NOT block the CLI — sync send() would add 10s of
        // latency to every stage report; for a 100-step pipeline that's 16+
        // minutes of pure callback waste.
        //
        // Terminal events (completed | error | killed): sync-wait so the
        // event reaches the server before the JVM exits. Without this an
        // async request would be cancelled mid-flight when System.exit()
        // hits. The reconciler poll loop closes any remaining push gap.
        String body = String.format( "{\"kind\":%s,\"payloadJson\":%s}",
                jsonString( kind ), jsonString( payloadAsJson( payload ) ) );
        HttpRequest req = HttpRequest.newBuilder()
                .uri( URI.create( callbackBaseUrl.replaceAll( "/+$", "" ) + "/jobs/" + jobId + "/events" ) )
                .header( "Authorization", "Bearer " + token )
                .header( "Content-Type", "application/json" )
                .timeout( TIMEOUT )
                .POST( HttpRequest.BodyPublishers.ofString( body, StandardCharsets.UTF_8 ) )
                .build();
        if ( terminal ) {
            try {
                HttpResponse<String> resp = http.send( req, HttpResponse.BodyHandlers.ofString() );
                if ( resp.statusCode() / 100 != 2 ) {
                    log.warn( String.format( "pipeline callback non-2xx: %d for kind=%s (body=%s)",
                            resp.statusCode(), kind, resp.body() ) );
                }
            } catch ( IOException | InterruptedException e ) {
                log.warn( "pipeline callback failed for kind=" + kind + ": " + e.getMessage() );
                if ( e instanceof InterruptedException ) {
                    Thread.currentThread().interrupt();
                }
            }
        } else {
            http.sendAsync( req, HttpResponse.BodyHandlers.ofString() )
                    .thenAccept( resp -> {
                        if ( resp.statusCode() / 100 != 2 ) {
                            log.warn( String.format( "pipeline callback non-2xx: %d for kind=%s (body=%s)",
                                    resp.statusCode(), kind, resp.body() ) );
                        }
                    } )
                    .exceptionally( e -> {
                        log.warn( "pipeline callback failed for kind=" + kind + ": " + e.getMessage() );
                        return null;
                    } );
        }
    }

    private static String payloadAsJson( Map<String, ?> payload ) {
        if ( payload.isEmpty() ) return "{}";
        StringBuilder sb = new StringBuilder( "{" );
        boolean first = true;
        for ( Map.Entry<String, ?> e : payload.entrySet() ) {
            if ( !first ) sb.append( "," );
            first = false;
            sb.append( jsonString( e.getKey() ) ).append( ":" );
            Object v = e.getValue();
            if ( v instanceof Number ) {
                sb.append( v );
            } else {
                sb.append( jsonString( String.valueOf( v ) ) );
            }
        }
        sb.append( "}" );
        return sb.toString();
    }

    private static String jsonString( String s ) {
        if ( s == null ) return "null";
        StringBuilder sb = new StringBuilder( s.length() + 2 ).append( '"' );
        for ( int i = 0; i < s.length(); i++ ) {
            char c = s.charAt( i );
            switch ( c ) {
                case '"': sb.append( "\\\"" ); break;
                case '\\': sb.append( "\\\\" ); break;
                case '\n': sb.append( "\\n" ); break;
                case '\r': sb.append( "\\r" ); break;
                case '\t': sb.append( "\\t" ); break;
                default:
                    if ( c < 0x20 ) {
                        sb.append( String.format( "\\u%04x", ( int ) c ) );
                    } else {
                        sb.append( c );
                    }
            }
        }
        sb.append( '"' );
        return sb.toString();
    }
}
