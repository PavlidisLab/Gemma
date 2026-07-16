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

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.pipeline.PipelineJobEvent;
import ubic.gemma.persistence.service.pipeline.PipelineJobBatchService;
import ubic.gemma.rest.analytics.AnalyticsProvider;
import ubic.gemma.rest.util.BaseJerseyTest5;
import ubic.gemma.rest.util.JacksonConfig;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.concurrent.Future;

import static org.apache.commons.lang3.concurrent.ConcurrentUtils.constantFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ubic.gemma.rest.util.Assertions.assertThat;

/**
 * Jersey REST IT for the Nextflow {@code -with-weblog} ingest
 * ({@code POST /internal/pipeline/jobs/{id}/weblog}). Verifies bearer-token auth, that the raw weblog
 * body is translated and forwarded to {@link PipelineJobBatchService#recordEvent} with the right kind,
 * that ignored messages acknowledge with 204 and record nothing, and that malformed bodies are 400.
 * Payload-level translation coverage lives in {@code NextflowWeblogTranslatorTest} (gemma-core).
 */
@ContextConfiguration
public class InternalPipelineWebServiceTest extends BaseJerseyTest5 {

    private static final String TOKEN = "test-secret";

    @Configuration
    @TestComponent
    @Import(JacksonConfig.class)
    public static class InternalPipelineWebServiceContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer placeholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer(
                    "gemma.hosturl=http://localhost:8080",
                    "gemma.pipeline.callback.token=" + TOKEN );
        }

        @Bean
        public PipelineJobBatchService pipelineJobBatchService() {
            return mock( PipelineJobBatchService.class );
        }

        @Bean
        public InternalPipelineWebService internalPipelineWebService() {
            return new InternalPipelineWebService();
        }

        @Bean
        public AnalyticsProvider analyticsProvider() {
            return mock();
        }

        @Bean
        public AccessDecisionManager accessDecisionManager() {
            return mock();
        }

        @Bean
        public Future<OpenAPI> openApi() {
            return constantFuture( mock() );
        }

        @Bean
        public BuildInfo buildInfo() {
            return mock();
        }
    }

    @Autowired
    private PipelineJobBatchService pipelineJobBatchService;

    @AfterEach
    public void resetMocks() {
        reset( pipelineJobBatchService );
    }

    private Response postWeblog( long jobId, String bearer, String body ) {
        return target( "/internal/pipeline/jobs/" + jobId + "/weblog" ).request()
                .header( "Authorization", bearer )
                .post( Entity.json( body ) );
    }

    @Test
    public void completedSuccess_recordsCompletedEvent() {
        when( pipelineJobBatchService.recordEvent( eq( 7L ), eq( "completed" ), any() ) )
                .thenReturn( new PipelineJobEvent() );
        String body = "{\"event\":\"completed\",\"runName\":\"x\","
                + "\"metadata\":{\"workflow\":{\"success\":true,\"duration\":123}}}";
        assertThat( postWeblog( 7L, "Bearer " + TOKEN, body ) ).hasStatus( Response.Status.OK );
        verify( pipelineJobBatchService ).recordEvent( eq( 7L ), eq( "completed" ), any() );
    }

    @Test
    public void failedTask_forwardsProgressNotError() {
        // A per-task FAILED must reach recordEvent as `progress`, never `error` (job failure is
        // decided by the workflow `completed` event) — the correctness guard, verified end-to-end.
        when( pipelineJobBatchService.recordEvent( eq( 7L ), eq( "progress" ), any() ) )
                .thenReturn( new PipelineJobEvent() );
        String body = "{\"event\":\"process_completed\",\"trace\":{\"process\":\"RUN\",\"tag\":\"beta\","
                + "\"status\":\"FAILED\",\"exit\":3}}";
        assertThat( postWeblog( 7L, "Bearer " + TOKEN, body ) ).hasStatus( Response.Status.OK );
        verify( pipelineJobBatchService ).recordEvent( eq( 7L ), eq( "progress" ), any() );
        verify( pipelineJobBatchService, never() ).recordEvent( eq( 7L ), eq( "error" ), any() );
    }

    @Test
    public void ignoredEvent_acknowledgesWith204AndRecordsNothing() {
        assertThat( postWeblog( 7L, "Bearer " + TOKEN, "{\"event\":\"started\",\"metadata\":{}}" ) )
                .hasStatus( Response.Status.NO_CONTENT );
        verify( pipelineJobBatchService, never() ).recordEvent( any(), any(), any() );
    }

    @Test
    public void malformedBody_isBadRequest() {
        assertThat( postWeblog( 7L, "Bearer " + TOKEN, "not json" ) )
                .hasStatus( Response.Status.BAD_REQUEST );
    }

    @Test
    public void invalidToken_isUnauthorized() {
        assertThat( postWeblog( 7L, "Bearer wrong", "{\"event\":\"started\"}" ) )
                .hasStatus( Response.Status.UNAUTHORIZED );
        verify( pipelineJobBatchService, never() ).recordEvent( any(), any(), any() );
    }
}
