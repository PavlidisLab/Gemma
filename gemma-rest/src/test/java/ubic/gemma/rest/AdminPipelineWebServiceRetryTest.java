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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.pipeline.BatchRollup;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.pipeline.PipelineJobBatchService;
import ubic.gemma.persistence.service.pipeline.RetrySpec;
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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ubic.gemma.rest.util.Assertions.assertThat;

/**
 * Jersey IT for the task-3 mop-up routes on {@code /admin/pipeline} — verifies routing, RetrySpec
 * body binding, and that the {@link BatchRollup} shape is returned. Service is mocked
 * (TasksWebServiceTest pattern); the retry semantics themselves are covered by
 * {@code PipelineJobRetryMockIT} against the DB.
 */
@ContextConfiguration
@TestExecutionListeners({ WithSecurityContextTestExecutionListener.class })
public class AdminPipelineWebServiceRetryTest extends BaseJerseyTest5 {

    @Configuration
    @TestComponent
    @Import(JacksonConfig.class)
    public static class AdminPipelineWebServiceRetryContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer placeholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer( "gemma.hosturl=http://localhost:8080" );
        }

        @Bean
        public PipelineJobBatchService pipelineJobBatchService() {
            return mock( PipelineJobBatchService.class );
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock( ExpressionExperimentService.class );
        }

        @Bean
        public UserManager userManager() {
            return mock( UserManager.class );
        }

        @Bean
        public AdminPipelineWebService adminPipelineWebService() {
            return new AdminPipelineWebService();
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

    private static BatchRollup rollup( int total, int failed ) {
        BatchRollup r = new BatchRollup();
        r.total = total;
        r.failed = failed;
        r.needsAttention = failed > 0;
        return r;
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void rollup_returnsRollupShape() {
        when( pipelineJobBatchService.computeRollup( 5L ) ).thenReturn( rollup( 12, 2 ) );
        assertThat( target( "/admin/pipeline/batches/5/rollup" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.total", 12 )
                .hasFieldOrPropertyWithValue( "data.needs_attention", true );
        verify( pipelineJobBatchService ).computeRollup( 5L );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void retryFailed_bindsBodyAndReturnsRollup() {
        when( pipelineJobBatchService.retryFailed( eq( 5L ), any( RetrySpec.class ) ) ).thenReturn( rollup( 12, 0 ) );
        assertThat( target( "/admin/pipeline/batches/5/retry-failed" ).request()
                .post( Entity.json( "{\"onlyRetryable\":true}" ) ) )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.total", 12 );
        verify( pipelineJobBatchService ).retryFailed( eq( 5L ), any( RetrySpec.class ) );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void retryJob_bindsBodyAndReturnsRollup() {
        when( pipelineJobBatchService.retryJob( eq( 7L ), any( RetrySpec.class ) ) ).thenReturn( rollup( 12, 0 ) );
        assertThat( target( "/admin/pipeline/batches/5/jobs/7/retry" ).request()
                .post( Entity.json( "{\"onlyRetryable\":false}" ) ) )
                .hasStatus( Response.Status.OK );
        verify( pipelineJobBatchService ).retryJob( eq( 7L ), any( RetrySpec.class ) );
    }
}
