/*
 * The Gemma project
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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetValueObjectHelper;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.ResponseErrorObject;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static ubic.gemma.rest.util.Responders.paginate;
import static ubic.gemma.rest.util.Responders.respond;

/**
 * Experiment sets — named, ACL'd collections of datasets ("dataset groups" in the old DWR surface).
 * <p>
 * This is the REST home the parity audit recorded as missing
 * ({@code docs/audit/DWR_REST_GAP_AUDIT.md}: "Experiment-set (dataset group) CRUD + retrieval …
 * Zero REST home"). Everything here is a thin layer over {@link ExpressionExperimentSetService} and
 * {@link ExpressionExperimentSetValueObjectHelper}, which own the taxon rule, the ACL defaults and
 * the member validation.
 * <p>
 * 🛑 Not to be confused with {@code /groups}, which is USER groups. The two nouns collided in the
 * curation UI on 2026-08-29 and a set surface named "groups" would have kept them colliding.
 * <p>
 * <b>The taxon rule, since it decides what a set may hold.</b> A set MAY declare a taxon; when it
 * does, every member must match it — no mice in a rat set. When it does not, the set may span taxa.
 * A create whose members share a taxon gets it for free; one whose members disagree is stored
 * without a taxon rather than refused, because a curation cohort is a legitimate thing to want: the
 * gold reference set is 179 human, 254 mouse and 16 rat.
 *
 * @author gembro
 */
@Service
@Path("/experiment-sets")
@Tag(name = "Experiment Sets", description = "Named collections of datasets, with membership editing")
public class ExperimentSetsWebService {

    private final ExpressionExperimentSetService expressionExperimentSetService;
    private final ExpressionExperimentSetValueObjectHelper expressionExperimentSetValueObjectHelper;

    @Autowired
    public ExperimentSetsWebService( ExpressionExperimentSetService expressionExperimentSetService,
            ExpressionExperimentSetValueObjectHelper expressionExperimentSetValueObjectHelper ) {
        this.expressionExperimentSetService = expressionExperimentSetService;
        this.expressionExperimentSetValueObjectHelper = expressionExperimentSetValueObjectHelper;
    }

    /* ================================ READ ================================ */

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List experiment sets",
            description = "Paginated. `mine=true` restricts to the sets the caller owns; otherwise the "
                    + "list is every set the caller can read, which for an anonymous caller means the "
                    + "public ones. `query` matches the name case-insensitively. `includeMembers=true` "
                    + "populates `expressionExperimentIds`, which is the expensive part — a set can "
                    + "hold thousands of datasets — so it is off by default.",
            responses = @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()))
    public PaginatedResponseDataObject<ExpressionExperimentSetValueObject> getExperimentSets(
            @Parameter(description = "Only the sets owned by the caller.")
            @QueryParam("mine") @DefaultValue("false") boolean mine,
            @Parameter(description = "Case-insensitive substring of the set name.")
            @QueryParam("query") @Nullable String query,
            @Parameter(description = "Populate the member dataset ids.")
            @QueryParam("includeMembers") @DefaultValue("false") boolean includeMembers,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offsetArg,
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg
    ) {
        List<ExpressionExperimentSetValueObject> all = new ArrayList<>( mine
                ? expressionExperimentSetService.loadMySetValueObjects( includeMembers )
                : expressionExperimentSetService.loadAllExperimentSetValueObjects( includeMembers ) );
        if ( query != null && !query.isEmpty() ) {
            String needle = query.toLowerCase();
            all = all.stream()
                    .filter( s -> s.getName() != null && s.getName().toLowerCase().contains( needle ) )
                    .collect( Collectors.toList() );
        }
        all.sort( Comparator.comparing( ExpressionExperimentSetValueObject::getName,
                Comparator.nullsLast( String.CASE_INSENSITIVE_ORDER ) ) );
        int offset = offsetArg.getValue();
        int limit = limitArg.getValue();
        long total = all.size();
        List<ExpressionExperimentSetValueObject> page = all.subList( Math.min( offset, all.size() ),
                Math.min( offset + limit, all.size() ) );
        return paginate( new Slice<>( page, null, offset, limit, total ), new String[] { "id" } );
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve one experiment set",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "No such set, or the caller cannot read it.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<ExpressionExperimentSetValueObject> getExperimentSet(
            @PathParam("id") Long id,
            @Parameter(description = "Populate the member dataset ids.")
            @QueryParam("includeMembers") @DefaultValue("true") boolean includeMembers
    ) {
        return respond( expressionExperimentSetService.loadValueObjectById( id, includeMembers ) );
    }

    @GET
    @Path("/{id}/datasets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List the datasets in an experiment set",
            description = "The members as dataset value objects rather than bare ids.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "No such set, or the caller cannot read it.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<List<ExpressionExperimentDetailsValueObject>> getExperimentSetDatasets(
            @PathParam("id") Long id
    ) {
        requireSet( id );
        Collection<ExpressionExperimentDetailsValueObject> members =
                expressionExperimentSetService.getExperimentValueObjectsInSet( id );
        return respond( new ArrayList<>( members ) );
    }

    /* =============================== WRITE ================================ */

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create an experiment set",
            description = "The set is PRIVATE unless `isPublic` is true. The taxon is derived from the "
                    + "members when they agree and left unset when they do not, which is what allows a "
                    + "set to span taxa; supply `taxonId` to declare the constraint explicitly, and "
                    + "every member must then match it.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created.", content = @Content()),
                    @ApiResponse(responseCode = "400", description = "No name, or a member does not match a declared taxon.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response createExperimentSet( ExperimentSetRequest req ) {
        if ( req == null || req.name == null || req.name.trim().isEmpty() ) {
            throw new BadRequestException( "name is required." );
        }
        ExpressionExperimentSetValueObject vo = new ExpressionExperimentSetValueObject();
        vo.setName( req.name.trim() );
        vo.setDescription( req.description != null ? req.description : "" );
        vo.setIsPublic( Boolean.TRUE.equals( req.isPublic ) );
        vo.setTaxonId( req.taxonId );
        if ( req.datasetIds != null ) {
            vo.getExpressionExperimentIds().addAll( req.datasetIds );
        }
        Long id = expressionExperimentSetValueObjectHelper.create( vo ).getId();
        return Response.status( Response.Status.CREATED )
                .entity( respond( expressionExperimentSetService.loadValueObjectById( id, true ) ) )
                .build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Rename an experiment set, or change its description",
            description = "Membership is not touched here; use `PUT /experiment-sets/{id}/datasets`.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "No such set, or the caller cannot read it.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<ExpressionExperimentSetValueObject> updateExperimentSet(
            @PathParam("id") Long id, ExperimentSetRequest req
    ) {
        ExpressionExperimentSetValueObject vo = requireSet( id );
        if ( req == null || ( req.name == null && req.description == null ) ) {
            throw new BadRequestException( "Provide a name, a description, or both." );
        }
        if ( req.name != null ) {
            if ( req.name.trim().isEmpty() ) {
                throw new BadRequestException( "name must not be blank." );
            }
            vo.setName( req.name.trim() );
        }
        if ( req.description != null ) {
            vo.setDescription( req.description );
        }
        return respond( expressionExperimentSetValueObjectHelper.updateNameAndDescription( vo, true ) );
    }

    @PUT
    @Path("/{id}/datasets")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Replace the membership of an experiment set",
            description = "The body's `datasetIds` become the set's members exactly — this is a replace, "
                    + "not an add. Sending an empty list empties the set.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "A member does not match the set's declared taxon.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "No such set, or the caller cannot read it.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<ExpressionExperimentSetValueObject> updateExperimentSetMembers(
            @PathParam("id") Long id, ExperimentSetMembersRequest req
    ) {
        requireSet( id );
        if ( req == null || req.datasetIds == null ) {
            throw new BadRequestException( "datasetIds is required; send an empty list to empty the set." );
        }
        expressionExperimentSetValueObjectHelper.updateMembers( id, req.datasetIds );
        return respond( expressionExperimentSetService.loadValueObjectById( id, true ) );
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete an experiment set",
            description = "The datasets themselves are untouched; only the set is removed.",
            responses = {
                    // no content block on a 204: the OpenAPI contract test requires it to be
                    // absent, not empty, and an empty @Content() serializes as application/json
                    // with a null schema
                    @ApiResponse(responseCode = "204", description = "Deleted."),
                    @ApiResponse(responseCode = "404", description = "No such set, or the caller cannot read it.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response deleteExperimentSet( @PathParam("id") Long id ) {
        expressionExperimentSetValueObjectHelper.delete( requireSet( id ) );
        return Response.noContent().build();
    }

    /**
     * Load a set or 404. A set the caller cannot read is indistinguishable from one that does not
     * exist, which is the same answer the rest of the API gives for a securable it may not see.
     */
    private ExpressionExperimentSetValueObject requireSet( Long id ) {
        ExpressionExperimentSetValueObject vo = expressionExperimentSetService.loadValueObjectById( id, false );
        if ( vo == null ) {
            throw new NotFoundException( "No experiment set with id " + id + "." );
        }
        return vo;
    }

    /**
     * Create / rename body. Every field is optional on update; {@code name} is required on create.
     */
    public static class ExperimentSetRequest {
        public String name;
        public String description;
        public Boolean isPublic;
        /** Declare the taxon constraint explicitly; omit to derive it from the members. */
        public Long taxonId;
        public List<Long> datasetIds;
    }

    /** Membership replacement body. */
    public static class ExperimentSetMembersRequest {
        public List<Long> datasetIds;
    }
}
