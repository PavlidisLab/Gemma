/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.rest.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;
import ubic.gemma.core.util.BuildInfo;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure-Mockito unit tests for {@link RestAuthEntryPoint}.
 * <p>
 * Asserts the JSON shape, content-type, and status code returned when Spring Security
 * triggers the entry point on a failed authentication. No Spring context, sub-second per test.
 */
@ExtendWith(MockitoExtension.class)
class RestAuthEntryPointTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private AuthenticationException authException;

    private ObjectMapper objectMapper;
    private BuildInfo buildInfo;
    private StringWriter writer;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        buildInfo = new BuildInfo( "1.0.0-TEST", "2026-01-01T00:00:00Z", "deadbeef" );
        writer = new StringWriter();
        when( response.getWriter() ).thenReturn( new PrintWriter( writer ) );
    }

    @Test
    void commence_writesJsonWith401Status_whenOpenApiReady() throws Exception {
        OpenAPI api = new OpenAPI().info( new Info().title( "Gemma RESTful API" ).version( "2.0" ) );
        Future<OpenAPI> openApi = CompletableFuture.completedFuture( api );
        RestAuthEntryPoint entryPoint = new RestAuthEntryPoint( objectMapper, openApi, buildInfo );

        entryPoint.commence( request, response, authException );

        verify( response ).setContentType( "application/json" );
        verify( response ).setStatus( HttpServletResponse.SC_UNAUTHORIZED );
        verify( response ).addHeader( "WWW-Authenticate", "xBasic realm=Gemma RESTful API" );
        verify( response ).flushBuffer();

        JsonNode body = objectMapper.readTree( writer.toString() );
        assertThat( body.get( "apiVersion" ).asText() ).isEqualTo( "2.0" );
        assertThat( body.get( "buildInfo" ) ).isNotNull();
        assertThat( body.get( "buildInfo" ).get( "version" ).asText() ).isEqualTo( "1.0.0-TEST" );
        assertThat( body.get( "error" ).get( "code" ).asInt() ).isEqualTo( 401 );
        assertThat( body.get( "error" ).get( "message" ).asText() )
                .isEqualTo( "Provided authentication credentials are invalid." );
    }

    @Test
    void commence_omitsVersion_whenOpenApiStillLoading() throws Exception {
        Future<OpenAPI> openApi = new CompletableFuture<>(); // never completes
        RestAuthEntryPoint entryPoint = new RestAuthEntryPoint( objectMapper, openApi, buildInfo );

        entryPoint.commence( request, response, authException );

        verify( response ).setStatus( HttpServletResponse.SC_UNAUTHORIZED );
        verify( response ).addHeader( "WWW-Authenticate", "xBasic realm=Gemma RESTful API" );

        JsonNode body = objectMapper.readTree( writer.toString() );
        assertThat( body.get( "apiVersion" ).isNull() ).isTrue();
        assertThat( body.get( "error" ).get( "code" ).asInt() ).isEqualTo( 401 );
    }

    @Test
    void commence_omitsVersion_whenInfoSectionMissing() throws Exception {
        OpenAPI api = new OpenAPI(); // no info
        Future<OpenAPI> openApi = CompletableFuture.completedFuture( api );
        RestAuthEntryPoint entryPoint = new RestAuthEntryPoint( objectMapper, openApi, buildInfo );

        entryPoint.commence( request, response, authException );

        verify( response ).setStatus( HttpServletResponse.SC_UNAUTHORIZED );
        verify( response ).addHeader( "WWW-Authenticate", "xBasic realm=Gemma RESTful API" );

        JsonNode body = objectMapper.readTree( writer.toString() );
        assertThat( body.get( "apiVersion" ).isNull() ).isTrue();
    }

    @Test
    void commence_returns200_whenXmlHttpRequest() throws Exception {
        // RestAuthEntryPoint deliberately swaps 401 → 200 for XHR requests so the browser
        // doesn't trigger the default Basic-auth popup. The error body still carries code=401.
        when( request.getHeader( "X-Requested-With" ) ).thenReturn( "XMLHttpRequest" );
        OpenAPI api = new OpenAPI().info( new Info().title( "Gemma RESTful API" ).version( "2.0" ) );
        Future<OpenAPI> openApi = CompletableFuture.completedFuture( api );
        RestAuthEntryPoint entryPoint = new RestAuthEntryPoint( objectMapper, openApi, buildInfo );

        entryPoint.commence( request, response, authException );

        verify( response ).setStatus( HttpServletResponse.SC_OK );

        JsonNode body = objectMapper.readTree( writer.toString() );
        assertThat( body.get( "error" ).get( "code" ).asInt() ).isEqualTo( 401 );
    }
}
