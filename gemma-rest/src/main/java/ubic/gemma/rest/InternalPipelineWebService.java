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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ubic.gemma.model.pipeline.PipelineJobEvent;
import ubic.gemma.persistence.service.pipeline.PipelineJobBatchService;
import ubic.gemma.rest.util.ResponseDataObject;

import static ubic.gemma.rest.util.Responders.respond;

/**
 * Service-to-service push callback for scheduler-side pipelines reporting
 * progress / terminal state. Distinct from the admin REST surface — auth is
 * a shared bearer secret ({@code gemma.pipeline.callback.token}), not curator
 * session.
 *
 * <p>The scheduler-side pipeline calls
 * {@code POST /internal/pipeline/jobs/{jobId}/events} with
 * {@code Authorization: Bearer <secret>} on every progress / stage /
 * completion / error event. The endpoint persists the event and updates the
 * job state machine (see {@link PipelineJobBatchService#recordEvent}).</p>
 *
 * <p>STUB AUTH: the bearer token validation is a TODO pending an operational
 * decision on whether to share secrets via Gemma.properties, Vault, or
 * mTLS. The handler currently rejects with 401 if no token is configured
 * and accepts any matching token otherwise.</p>
 */
@Service
@Path("/internal/pipeline")
@Tag(name = "Internal/Pipeline", description = "Scheduler push callbacks — service-to-service only")
public class InternalPipelineWebService {

    private static final Log log = LogFactory.getLog( InternalPipelineWebService.class );

    @Autowired
    private PipelineJobBatchService pipelineJobBatchService;

    @Value("${gemma.pipeline.callback.token:}")
    private String expectedToken;

    @POST
    @Path("/jobs/{jobId}/events")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Append one event from the scheduler-side pipeline")
    public ResponseDataObject<PipelineJobEvent> postEvent(
            @PathParam("jobId") Long jobId,
            @HeaderParam("Authorization") String authHeader,
            PostEventRequest req ) {
        verifyToken( authHeader );
        if ( req == null || req.kind == null || req.kind.isBlank() ) {
            throw new BadRequestException( "kind is required" );
        }
        PipelineJobEvent event = pipelineJobBatchService.recordEvent( jobId, req.kind, req.payloadJson );
        return respond( event );
    }

    private void verifyToken( String authHeader ) {
        if ( expectedToken == null || expectedToken.isBlank() ) {
            // Fail-closed: no token configured means no scheduler is wired,
            // so we reject all push callbacks. Operator must set
            // gemma.pipeline.callback.token to enable.
            log.warn( "InternalPipelineWebService: no gemma.pipeline.callback.token configured; rejecting" );
            throw new NotAuthorizedException( "pipeline callbacks are not enabled" );
        }
        if ( authHeader == null || !authHeader.startsWith( "Bearer " ) ) {
            throw new NotAuthorizedException( "missing or malformed Authorization header" );
        }
        String supplied = authHeader.substring( "Bearer ".length() ).trim();
        if ( !expectedToken.equals( supplied ) ) {
            throw new NotAuthorizedException( "invalid token" );
        }
    }

    public static class PostEventRequest {
        public String kind;
        public String payloadJson;
    }
}
