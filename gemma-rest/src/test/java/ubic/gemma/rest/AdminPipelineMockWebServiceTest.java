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
import ubic.gemma.core.pipeline.MockSchedulerControl;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.pipeline.PipelineJobEvent;
import ubic.gemma.persistence.service.pipeline.PipelineJobBatchService;
import ubic.gemma.rest.analytics.AnalyticsProvider;
import ubic.gemma.rest.util.BaseJerseyTest5;
import ubic.gemma.rest.util.JacksonConfig;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.concurrent.Future;

import static org.apache.commons.lang3.concurrent.ConcurrentUtils.constantFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ubic.gemma.rest.util.Assertions.assertThat;

/**
 * Jersey REST IT for the dev-only {@code /admin/pipeline/_mock} surface (control
 * present). Verifies routing, request-body binding, and delegation to
 * {@link MockSchedulerControl} / {@link PipelineJobBatchService}. The disabled-profile
 * 404 path is covered by {@link AdminPipelineMockWebServiceDisabledTest}.
 */
@ContextConfiguration
@TestExecutionListeners({ WithSecurityContextTestExecutionListener.class })
public class AdminPipelineMockWebServiceTest extends BaseJerseyTest5 {

    @Configuration
    @TestComponent
    @Import(JacksonConfig.class)
    public static class AdminPipelineMockWebServiceContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer placeholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer( "gemma.hosturl=http://localhost:8080" );
        }

        @Bean
        public MockSchedulerControl mockSchedulerControl() {
            return mock( MockSchedulerControl.class );
        }

        @Bean
        public PipelineJobBatchService pipelineJobBatchService() {
            return mock( PipelineJobBatchService.class );
        }

        @Bean
        public AdminPipelineMockWebService adminPipelineMockWebService() {
            return new AdminPipelineMockWebService();
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
    private MockSchedulerControl control;

    @Autowired
    private PipelineJobBatchService pipelineJobBatchService;

    @AfterEach
    public void resetMocks() {
        reset( control, pipelineJobBatchService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void advance_stepsTheClock() {
        assertThat( target( "/admin/pipeline/_mock/advance" ).request()
                .post( Entity.json( "{\"ms\":1000}" ) ) )
                .hasStatus( Response.Status.OK );
        verify( control ).advance( 1000L );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void advance_negativeIsBadRequest() {
        assertThat( target( "/admin/pipeline/_mock/advance" ).request()
                .post( Entity.json( "{\"ms\":-1}" ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void setScenario_delegatesToControl() {
        String body = "{\"experimentId\":null,\"scenario\":{\"outcome\":\"SUCCEED\",\"transport\":\"PUSH\","
                + "\"stages\":[{\"afterMs\":0,\"kind\":\"completed\",\"payloadJson\":\"{}\"}]}}";
        assertThat( target( "/admin/pipeline/_mock/scenario" ).request()
                .post( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );
        verify( control ).setScenario( isNull(), any() );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void emit_recordsEvent() {
        when( pipelineJobBatchService.recordEvent( eq( 5L ), eq( "stage" ), any() ) )
                .thenReturn( new PipelineJobEvent() );
        assertThat( target( "/admin/pipeline/_mock/emit" ).request()
                .post( Entity.json( "{\"jobId\":5,\"kind\":\"stage\",\"payloadJson\":\"{}\"}" ) ) )
                .hasStatus( Response.Status.OK );
        verify( pipelineJobBatchService ).recordEvent( eq( 5L ), eq( "stage" ), any() );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void listScenarios_returnsOk() {
        when( control.listScenarios() ).thenReturn( Collections.emptyMap() );
        assertThat( target( "/admin/pipeline/_mock/scenarios" ).request().get() )
                .hasStatus( Response.Status.OK );
    }
}
