/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseErrorObject;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;
import ubic.gemma.rest.util.args.TaxonArg;
import ubic.gemma.rest.util.args.TaxonArgService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * RESTful interface for reverse GO-term lookup: {@code GO term URI → genes annotated
 * with it}. Companion to {@code GET /genes/{id}/goTerms} (gene → its GO terms).
 * <p>
 * Drives the dataset Visualize tab's "pick genes by GO term" mode — visitor types a
 * GO term, the UI calls {@code /goTerms/{termUri}/genes} and renders the candidate
 * gene list for selection.
 * <p>
 * GO-term TYPEAHEAD is not a new endpoint; the existing
 * {@code GET /annotations/search?query=...&prefixes=GO_} already covers it (filters
 * the Lucene index to GO URIs, ranks by Lucene score).
 */
@Service
@Path("/goTerms")
@Slf4j
public class GoTermsWebService {

    /** Per-request bound on ontology subtree expansion when {@code propagate=true}. */
    private static final long PROPAGATE_TIMEOUT_MS = 15_000L;

    @Autowired
    private Gene2GOAssociationService gene2GOAssociationService;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private TaxonArgService taxonArgService;

    /**
     * Reverse lookup: genes annotated with the given GO term. {@code termUri} is the full
     * OBO-style URI ({@code http://purl.obolibrary.org/obo/GO_0006915}); URL-encoded into
     * the path segment.
     * <p>
     * When {@code propagate=true} (default {@code false}) the lookup also includes the
     * union of genes annotated to any {@code subClassOf} descendant of {@code termUri} —
     * the natural semantic for a parent term like "regulation of apoptotic process" where
     * the caller wants every gene under that umbrella. {@code propagate=true} requires the
     * GO ontology to be loaded in the {@link OntologyService}; if it isn't, the call falls
     * back to exact-term-only and logs a warning rather than 503-ing.
     */
    @GET
    @Path("/{termUri}/genes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Genes annotated to a GO term (reverse Gene2GO lookup)",
            description = "Path parameter is the URL-encoded GO URI (e.g. "
                    + "http%3A%2F%2Fpurl.obolibrary.org%2Fobo%2FGO_0006915 = "
                    + "GO_0006915 = apoptotic process). Optional `taxon` (common-name or "
                    + "ID) narrows the result to that taxon's gene set. `propagate=true` "
                    + "walks the GO subClassOf descendants of the supplied term and unions "
                    + "their gene sets — useful for upper-level terms like "
                    + "'regulation of apoptotic process'. `limit` caps at 200; "
                    + "the response carries `totalElements` so callers can show "
                    + "'N genes — refine the term or pick individually'.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "Invalid termUri / taxon / limit / offset.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "503", description = "Ontology subtree expansion timed out (propagate=true only).",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
            })
    public PaginatedResponseDataObject<GeneValueObject> getGenesByGoTerm(
            @PathParam("termUri") String termUri,
            @QueryParam("taxon") TaxonArg<?> taxonArg,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offsetArg,
            @Parameter(description = "Maximum genes per page; capped at 200.")
            @QueryParam("limit") @DefaultValue("100") LimitArg limitArg,
            @Parameter(description = "Walk GO subClassOf descendants of {termUri} and union the gene sets. Default false.")
            @QueryParam("propagate") @DefaultValue("false") boolean propagate
    ) {
        if ( termUri == null || termUri.trim().isEmpty() ) {
            throw new BadRequestException( "GO term URI is required." );
        }
        int offset = offsetArg.getValue();
        int limit = limitArg.getValue( 200 );
        if ( limit < 1 ) {
            throw new BadRequestException( "limit must be >= 1." );
        }

        Taxon taxon = taxonArg != null ? taxonArgService.getEntity( taxonArg ) : null;

        Set<String> uris = expandUris( termUri, propagate );
        Collection<Gene> genes = gene2GOAssociationService.findByGOTermUris( uris, taxon );

        // Sort genes by officialSymbol (case-insensitive); stable for paging.
        List<Gene> sorted = new ArrayList<>( genes );
        sorted.sort( Comparator.comparing(
                g -> g.getOfficialSymbol() != null ? g.getOfficialSymbol() : "",
                String.CASE_INSENSITIVE_ORDER ) );

        long totalElements = sorted.size();
        int from = Math.min( offset, sorted.size() );
        int to = Math.min( from + limit, sorted.size() );
        List<GeneValueObject> page = sorted.subList( from, to ).stream()
                .map( GeneValueObject::new )
                .collect( Collectors.toList() );

        return new PaginatedResponseDataObject<>(
                new ubic.gemma.persistence.util.Slice<>(
                        page, null, offset, limit, totalElements ),
                new String[] { "id" } );
    }

    /**
     * Expand {@code termUri} to the set of URIs to look up. When {@code propagate=false}
     * the set is just the supplied URI. When {@code true} we add every
     * {@code subClassOf} descendant. If the GO ontology isn't loaded, fall back to the
     * single-URI case (logged) rather than 503.
     */
    private Set<String> expandUris( String termUri, boolean propagate ) {
        Set<String> uris = new HashSet<>();
        uris.add( termUri );
        if ( !propagate ) {
            return uris;
        }
        try {
            OntologyTerm term = ontologyService.getTerm( termUri, PROPAGATE_TIMEOUT_MS, TimeUnit.MILLISECONDS );
            if ( term == null ) {
                log.warn( "GoTermsWebService: propagate=true requested but " + termUri
                        + " not loaded in OntologyService; falling back to exact-term lookup" );
                return uris;
            }
            Set<OntologyTerm> descendants = ontologyService.getChildren(
                    Collections.singleton( term ), false, false,
                    PROPAGATE_TIMEOUT_MS, TimeUnit.MILLISECONDS );
            for ( OntologyTerm d : descendants ) {
                if ( d.getUri() != null ) {
                    uris.add( d.getUri() );
                }
            }
            log.debug( "GoTermsWebService: propagate from " + termUri + " expanded to " + uris.size() + " URIs" );
            return uris;
        } catch ( TimeoutException e ) {
            throw new ServiceUnavailableException(
                    "GO subtree expansion timed out; retry without propagate=true or wait for the ontology to finish loading.",
                    DateUtils.addSeconds( new Date(), 30 ), e );
        }
    }
}
