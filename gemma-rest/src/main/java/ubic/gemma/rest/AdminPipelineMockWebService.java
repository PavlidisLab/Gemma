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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.core.pipeline.MockSchedulerControl;
import ubic.gemma.core.pipeline.Scenario;
import ubic.gemma.model.pipeline.PipelineJobEvent;
import ubic.gemma.persistence.service.pipeline.PipelineJobBatchService;
import ubic.gemma.rest.util.ResponseDataObject;

import java.util.Map;

import static ubic.gemma.rest.util.Responders.respond;

/**
 * Dev-only control surface for the scripted mock scheduler — lets a test or the
 * curation UI (UIB) set canned scenarios, step the deterministic clock, and inject
 * events without a real scheduler wired.
 *
 * <p>Registered unconditionally (Jersey package-scans {@code ubic.gemma.rest}, so a
 * {@code @Profile}-gated resource would still be exposed with null-injected fields).
 * Dev-only-ness comes from {@link MockSchedulerControl}: its only implementation is
 * {@code @Profile("scheduler-mock")}, so outside that profile {@link #control} is null
 * and every endpoint returns <b>404</b>. Endpoints require {@code GROUP_ADMIN}.</p>
 */
@Service
@Path("/admin/pipeline/_mock")
@Tag(name = "Admin/Pipeline/Mock", description = "Dev-only scripted-mock scheduler control — inactive unless the scheduler-mock profile is on")
public class AdminPipelineMockWebService {

    /** Null unless the {@code scheduler-mock} profile supplies the only impl. */
    @Autowired(required = false)
    private MockSchedulerControl control;

    @Autowired
    private PipelineJobBatchService pipelineJobBatchService;

    @POST
    @Path("/scenario")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Register a canned scenario for an experiment (or the fallback when experimentId is null)")
    public ResponseDataObject<Boolean> setScenario( SetScenarioRequest req ) {
        MockSchedulerControl c = requireControl();
        if ( req == null || req.scenario == null ) {
            throw new BadRequestException( "scenario is required" );
        }
        c.setScenario( req.experimentId, req.scenario );
        return respond( true );
    }

    @POST
    @Path("/advance")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Step the deterministic virtual clock forward, firing due scripted stages")
    public ResponseDataObject<Boolean> advance( AdvanceRequest req ) {
        MockSchedulerControl c = requireControl();
        if ( req == null || req.ms < 0 ) {
            throw new BadRequestException( "ms >= 0 is required" );
        }
        c.advance( req.ms );
        return respond( true );
    }

    @POST
    @Path("/emit")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Inject an arbitrary event for a job (bearer-free dev convenience over the internal callback)")
    public ResponseDataObject<PipelineJobEvent> emit( EmitRequest req ) {
        requireControl();
        if ( req == null || req.jobId == null || req.kind == null || req.kind.isBlank() ) {
            throw new BadRequestException( "jobId and kind are required" );
        }
        PipelineJobEvent event = pipelineJobBatchService.recordEvent( req.jobId, req.kind, req.payloadJson );
        return respond( event );
    }

    @GET
    @Path("/scenarios")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "List the currently-registered per-experiment scenarios")
    public ResponseDataObject<Map<Long, Scenario>> listScenarios() {
        return respond( requireControl().listScenarios() );
    }

    private MockSchedulerControl requireControl() {
        if ( control == null ) {
            throw new NotFoundException( "scripted mock scheduler is not active (scheduler-mock profile off)" );
        }
        return control;
    }

    // -----------------------------------------------------------------------
    // request bodies
    // -----------------------------------------------------------------------

    public static class SetScenarioRequest {
        public Long experimentId;
        public Scenario scenario;
    }

    public static class AdvanceRequest {
        public long ms;
    }

    public static class EmitRequest {
        public Long jobId;
        public String kind;
        public String payloadJson;
    }
}
