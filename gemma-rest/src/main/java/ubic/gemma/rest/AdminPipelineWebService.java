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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.pipeline.PipelineJobBatch;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.pipeline.PipelineJobBatchService;
import ubic.gemma.rest.util.ResponseDataObject;

import static ubic.gemma.rest.util.Responders.respond;

import java.util.ArrayList;
import java.util.Collection;
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
        PipelineJobBatch batch = pipelineJobBatchService.submit( req.pipeline, ees, curator, req.paramsJson, req.note );
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

    // -----------------------------------------------------------------------
    // request body
    // -----------------------------------------------------------------------

    public static class SubmitBatchRequest {
        public String pipeline;
        public List<Long> experimentIds = new ArrayList<>();
        public String paramsJson;
        public String note;
    }
}
