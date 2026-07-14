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
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.pipeline.BatchRollup;
import ubic.gemma.model.pipeline.PipelineJobBatch;
import ubic.gemma.model.pipeline.PipelineJobEvent;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.pipeline.PipelineJobBatchService;
import ubic.gemma.persistence.service.pipeline.RetrySpec;
import ubic.gemma.rest.util.ResponseDataObject;

import static ubic.gemma.rest.util.Responders.respond;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Admin REST surface for curator-driven pipeline batch submissions.
 *
 * <p>All endpoints require {@code GROUP_ADMIN}; pipeline dispatch is a
 * privileged operation. See {@link PipelineJobBatchService} for semantics.</p>
 */
@Service
@Path("/admin/pipeline")
@Tag(name = "Admin/Pipeline", description = "Curator-driven pipeline batch submissions — admin only")
public class AdminPipelineWebService {

    private static final Log log = LogFactory.getLog( AdminPipelineWebService.class );

    @Autowired
    private PipelineJobBatchService pipelineJobBatchService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private UserManager userManager;

    /**
     * Comma-separated list of pipeline names this Gemma instance knows about.
     * Used to back the {@code /admin/pipeline/registry} dropdown.
     * Empty = no registry (UI falls back to free-text).
     */
    @Value("${gemma.pipeline.registry:}")
    private String pipelineRegistryCsv;

    @GET
    @Path("/registry")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "List pipeline names known to this Gemma instance")
    public ResponseDataObject<List<String>> pipelineRegistry() {
        if ( pipelineRegistryCsv == null || pipelineRegistryCsv.isBlank() ) {
            return respond( Collections.emptyList() );
        }
        List<String> names = new ArrayList<>();
        for ( String name : pipelineRegistryCsv.split( "," ) ) {
            String trimmed = name.trim();
            if ( !trimmed.isEmpty() ) {
                names.add( trimmed );
            }
        }
        return respond( names );
    }

    @POST
    @Path("/batches")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Submit a new pipeline batch")
    public ResponseDataObject<PipelineJobBatch> submitBatch( SubmitBatchRequest req ) {
        if ( req == null || req.pipeline == null || req.pipeline.isBlank() ) {
            throw new BadRequestException( "pipeline is required" );
        }
        if ( req.experimentIds == null || req.experimentIds.isEmpty() ) {
            throw new BadRequestException( "experimentIds is required" );
        }
        User curator = userManager.getCurrentUser();
        if ( curator == null ) {
            throw new BadRequestException( "no authenticated user resolved" );
        }
        Collection<ExpressionExperiment> ees = expressionExperimentService.load( req.experimentIds );
        if ( ees.size() != req.experimentIds.size() ) {
            log.warn( String.format( "submitBatch: %d/%d experiment ids resolved (some not found or not accessible)",
                    ees.size(), req.experimentIds.size() ) );
        }
        if ( ees.isEmpty() ) {
            throw new BadRequestException( "no resolvable experiments in experimentIds" );
        }
        PipelineJobBatch batch = pipelineJobBatchService.submit( req.pipeline, ees, curator,
                req.paramsJson, req.note, req.maxConcurrent );
        return respond( batch );
    }

    @GET
    @Path("/batches")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "List batches submitted by the current curator")
    public ResponseDataObject<List<PipelineJobBatch>> listMyBatches(
            @QueryParam("state") PipelineJobBatch.BatchState state,
            @QueryParam("limit") Integer limit ) {
        User curator = userManager.getCurrentUser();
        if ( curator == null ) {
            throw new BadRequestException( "no authenticated user resolved" );
        }
        List<PipelineJobBatch> batches = pipelineJobBatchService.findByOwner( curator.getId(), state,
                limit != null ? limit : 50 );
        return respond( batches );
    }

    @GET
    @Path("/batches/{batchId}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Retrieve one batch with rollup state")
    public ResponseDataObject<PipelineJobBatch> getBatch( @PathParam("batchId") Long batchId ) {
        PipelineJobBatch batch = pipelineJobBatchService.get( batchId );
        if ( batch == null ) throw new NotFoundException( "no batch " + batchId );
        return respond( batch );
    }

    @POST
    @Path("/batches/{batchId}/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Cancel every non-terminal job in the batch")
    public ResponseDataObject<PipelineJobBatch> cancelBatch( @PathParam("batchId") Long batchId ) {
        pipelineJobBatchService.cancelBatch( batchId );
        PipelineJobBatch batch = pipelineJobBatchService.get( batchId );
        return respond( batch );
    }

    @GET
    @Path("/batches/{batchId}/jobs/{jobId}/events")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "List progress events for one job")
    public ResponseDataObject<List<PipelineJobEvent>> jobEvents(
            @PathParam("batchId") Long batchId,
            @PathParam("jobId") Long jobId,
            @QueryParam("sinceMillis") Long sinceMillis,
            @QueryParam("limit") Integer limit ) {
        // batchId is a path-readability hint only; events are scoped by jobId.
        Date since = sinceMillis != null ? new Date( sinceMillis ) : null;
        List<PipelineJobEvent> events = pipelineJobBatchService.findEvents( jobId, since, limit != null ? limit : 200 );
        return respond( events );
    }

    @POST
    @Path("/batches/{batchId}/jobs/{jobId}/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Cancel one job mid-batch")
    public ResponseDataObject<Boolean> cancelJob( @PathParam("batchId") Long batchId,
            @PathParam("jobId") Long jobId ) {
        // batchId is a path-readability hint only; the service operates on jobId.
        pipelineJobBatchService.cancelJob( jobId );
        return respond( true );
    }

    @GET
    @Path("/batches/{batchId}/rollup")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Derived disposition of the batch over its current attempts")
    public ResponseDataObject<BatchRollup> batchRollup( @PathParam("batchId") Long batchId ) {
        return respond( pipelineJobBatchService.computeRollup( batchId ) );
    }

    @POST
    @Path("/batches/{batchId}/retry-failed")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Mop-up: retry the batch's failed current attempts; returns the fresh rollup")
    public ResponseDataObject<BatchRollup> retryFailed( @PathParam("batchId") Long batchId,
            RetrySpec spec ) {
        return respond( pipelineJobBatchService.retryFailed( batchId, spec != null ? spec : new RetrySpec() ) );
    }

    @POST
    @Path("/batches/{batchId}/jobs/{jobId}/retry")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Retry a single terminal job (mints a new attempt); returns the batch rollup")
    public ResponseDataObject<BatchRollup> retryJob( @PathParam("batchId") Long batchId,
            @PathParam("jobId") Long jobId, RetrySpec spec ) {
        // batchId is a path-readability hint only; the service operates on jobId.
        return respond( pipelineJobBatchService.retryJob( jobId, spec != null ? spec : new RetrySpec() ) );
    }

    @POST
    @Path("/batches/{batchId}/hold")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Pause dispatch for the batch (in-flight jobs keep running)")
    public ResponseDataObject<PipelineJobBatch> holdBatch( @PathParam("batchId") Long batchId ) {
        pipelineJobBatchService.holdBatch( batchId );
        return respond( pipelineJobBatchService.get( batchId ) );
    }

    @POST
    @Path("/batches/{batchId}/resume")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Clear the hold and top up within the concurrency budget")
    public ResponseDataObject<PipelineJobBatch> resumeBatch( @PathParam("batchId") Long batchId ) {
        pipelineJobBatchService.resumeBatch( batchId );
        return respond( pipelineJobBatchService.get( batchId ) );
    }

    @PATCH
    @Path("/batches/{batchId}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Update a batch's dispatch config (maxConcurrent, note)")
    public ResponseDataObject<PipelineJobBatch> updateBatch( @PathParam("batchId") Long batchId,
            UpdateBatchRequest req ) {
        if ( req == null ) {
            req = new UpdateBatchRequest();
        }
        return respond( pipelineJobBatchService.updateBatch( batchId, req.maxConcurrent, req.note ) );
    }

    // -----------------------------------------------------------------------
    // request body
    // -----------------------------------------------------------------------

    public static class SubmitBatchRequest {
        public String pipeline;
        public List<Long> experimentIds = new ArrayList<>();
        public String paramsJson;
        public String note;
        public Integer maxConcurrent;
    }

    public static class UpdateBatchRequest {
        public Integer maxConcurrent;
        public String note;
    }
}
