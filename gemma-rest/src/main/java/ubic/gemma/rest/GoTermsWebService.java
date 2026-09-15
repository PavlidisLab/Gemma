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
import java.util.LinkedHashSet;
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
    private org.springframework.cache.CacheManager cacheManager;

    /** Lazy-init handle to the GoTermGeneCountCache region; see EhcacheConfig. */
    private volatile org.springframework.cache.Cache geneCountCache;

    private org.springframework.cache.Cache geneCountCache() {
        org.springframework.cache.Cache c = geneCountCache;
        if ( c == null ) {
            c = cacheManager.getCache( "GoTermGeneCountCache" );
            geneCountCache = c;
        }
        return c;
    }

    @Autowired
    private OntologyService ontologyService;

    /**
     * GO ontology is autowired separately from the generic {@link OntologyService} list because
     * {@link ubic.gemma.core.ontology.providers.GeneOntologyServiceImpl} wraps basecode's GO
     * service and isn't picked up by the {@code List<OntologyService>} autowiring. Without this,
     * {@code ontologyService.getTerm("http://purl.obolibrary.org/obo/GO_0008152")} returns null
     * and propagate=true silently falls back to exact-term lookup.
     */
    @Autowired
    private ubic.gemma.core.ontology.providers.GeneOntologyService geneOntologyService;

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
            @QueryParam("propagate") @DefaultValue("false") boolean propagate,
            @Parameter(description = "When `propagate=true`, cap the breadth-first descendant walk at this many terms (including the root). Default 0 = unbounded (full subtree).")
            @QueryParam("maxTerms") @DefaultValue("0") int maxTerms
    ) {
        if ( termUri == null || termUri.trim().isEmpty() ) {
            throw new BadRequestException( "GO term URI is required." );
        }
        int offset = offsetArg.getValue();
        int limit = limitArg.getValue( 200 );
        if ( limit < 1 ) {
            throw new BadRequestException( "limit must be >= 1." );
        }
        if ( maxTerms < 0 ) {
            throw new BadRequestException( "maxTerms must be >= 0." );
        }

        Taxon taxon = taxonArg != null ? taxonArgService.getEntity( taxonArg ) : null;
        String canonicalUri = normalizeGoUri( termUri );

        Set<String> uris = expandUris( canonicalUri, propagate, maxTerms );
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

    /** Canonical GO URI prefix used by Gene2GOAssociation.ontologyEntry.valueUri rows. */
    private static final String GO_URI_PREFIX = "http://purl.obolibrary.org/obo/GO_";

    /**
     * Normalize a caller-supplied GO term identifier to the canonical full-URI form stored
     * on {@code Gene2GOAssociation.ontologyEntry.valueUri}. Accepts:
     * <ul>
     *   <li>The full URI ({@code http://purl.obolibrary.org/obo/GO_0001889}).</li>
     *   <li>The OBO-foundry short form ({@code GO_0001889}).</li>
     *   <li>The colon-separated short form ({@code GO:0001889}) — what most clients
     *       and {@code valueUri}-free callers send.</li>
     * </ul>
     * Tomcat's default {@code allowEncodedSlash=false} rejects {@code %2F} in path segments
     * so the full-URI form is rarely usable via the path parameter; the two short forms
     * are the common case. Returns the input unchanged if it doesn't match any known
     * pattern (lets the caller see the empty result rather than a synthetic 404).
     */
    private static String normalizeGoUri( String raw ) {
        if ( raw == null ) return null;
        String s = raw.trim();
        if ( s.startsWith( "http://" ) || s.startsWith( "https://" ) ) {
            return s;
        }
        if ( s.startsWith( "GO:" ) ) {
            return GO_URI_PREFIX + s.substring( 3 );
        }
        if ( s.startsWith( "GO_" ) ) {
            return GO_URI_PREFIX + s.substring( 3 );
        }
        return s;
    }

    /**
     * Expand {@code termUri} to the set of URIs to look up. When {@code propagate=false}
     * the set is just the supplied URI. When {@code true} and {@code maxTerms <= 0} we add
     * every {@code subClassOf} descendant (the original full-subtree behaviour). When
     * {@code maxTerms > 0} we run a bounded breadth-first walk through direct children and
     * stop as soon as {@code maxTerms} URIs (root included) have been collected — useful for
     * very broad parents like "metabolic process" where a full descendant walk produces
     * thousands of URIs that the caller doesn't actually need.
     * <p>
     * If the GO ontology isn't loaded, fall back to the single-URI case (logged) rather
     * than 503.
     */
    private Set<String> expandUris( String termUri, boolean propagate, int maxTerms ) {
        Set<String> uris = new LinkedHashSet<>();
        uris.add( termUri );
        if ( !propagate ) {
            return uris;
        }
        // Resolve via GeneOntologyService directly — see field-level comment for why the generic
        // ontologyService doesn't see GO. {@code getTerm(uri)} accepts both the full OBO URI and
        // GO:NNNNNNN / GO_NNNNNNN short forms, so normalizeGoUri is enough at the boundary.
        if ( !geneOntologyService.isOntologyLoaded() ) {
            log.warn( "GoTermsWebService: propagate=true requested but GO ontology not loaded; "
                    + "falling back to exact-term lookup for " + termUri );
            return uris;
        }
        OntologyTerm term = geneOntologyService.getTerm( termUri );
        if ( term == null ) {
            log.warn( "GoTermsWebService: propagate=true requested but " + termUri
                    + " not present in GO; falling back to exact-term lookup" );
            return uris;
        }
        if ( maxTerms <= 0 ) {
            // Full subtree. Walk via OntologyTerm.getChildren(false, false): transitive
            // subClassOf, no additional-property edges, no self.
            Collection<OntologyTerm> descendants = term.getChildren( false, false );
            for ( OntologyTerm d : descendants ) {
                if ( d.getUri() != null ) {
                    uris.add( d.getUri() );
                }
            }
        } else {
            // BFS via direct children. Bounded by maxTerms and by PROPAGATE_TIMEOUT_MS as a
            // safety net — basecode's getChildren is purely in-memory once GO is loaded, so
            // the timeout rarely fires, but keep it for the edge case of a still-warming index.
            long deadline = System.currentTimeMillis() + PROPAGATE_TIMEOUT_MS;
            List<OntologyTerm> frontier = new ArrayList<>();
            frontier.add( term );
            while ( !frontier.isEmpty() && uris.size() < maxTerms
                    && System.currentTimeMillis() < deadline ) {
                List<OntologyTerm> newFrontier = new ArrayList<>();
                for ( OntologyTerm f : frontier ) {
                    Collection<OntologyTerm> direct = f.getChildren( true, false );
                    for ( OntologyTerm c : direct ) {
                        if ( c.getUri() == null ) continue;
                        if ( uris.add( c.getUri() ) ) {
                            newFrontier.add( c );
                            if ( uris.size() >= maxTerms ) break;
                        }
                    }
                    if ( uris.size() >= maxTerms ) break;
                }
                frontier = newFrontier;
            }
        }
        log.debug( "GoTermsWebService: propagate from " + termUri + " expanded to " + uris.size() + " URIs"
                + ( maxTerms > 0 ? " (maxTerms=" + maxTerms + ")" : "" ) );
        return uris;
    }

    /**
     * Distinct-gene count for the given GO term, optionally including descendants. Cheap —
     * goes through {@code countByGOTermUris}, which runs {@code COUNT(DISTINCT gene.id)}
     * without materializing Gene rows. Use this in place of fetching the full gene list
     * just to display "N genes annotated to {term}" badges.
     */
    @GET
    @Path("/{termUri}/genes/count")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Count of genes annotated to a GO term (optionally including descendants)",
            description = "Companion to `/goTerms/{termUri}/genes` for callers that only want " +
                    "the count. `propagate=true` includes descendants. `maxTerms` (default 0) " +
                    "caps the BFS descendant walk; useful for very broad parent terms.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "Invalid termUri / taxon / maxTerms.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "503", description = "Ontology subtree expansion timed out (propagate=true only).",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
            })
    public ubic.gemma.rest.util.ResponseDataObject<GoTermGeneCountValueObject> countGenesByGoTerm(
            @PathParam("termUri") String termUri,
            @QueryParam("taxon") TaxonArg<?> taxonArg,
            @QueryParam("propagate") @DefaultValue("false") boolean propagate,
            @Parameter(description = "When `propagate=true`, cap the breadth-first descendant walk at this many terms (including the root). Default 0 = unbounded.")
            @QueryParam("maxTerms") @DefaultValue("0") int maxTerms
    ) {
        if ( termUri == null || termUri.trim().isEmpty() ) {
            throw new BadRequestException( "GO term URI is required." );
        }
        if ( maxTerms < 0 ) {
            throw new BadRequestException( "maxTerms must be >= 0." );
        }
        Taxon taxon = taxonArg != null ? taxonArgService.getEntity( taxonArg ) : null;
        String canonicalUri = normalizeGoUri( termUri );
        // Cache key: canonical URI + propagate + maxTerms + taxon id. SQL count is 1-5s on
        // broad subtrees; once computed the count is stable until GO is reloaded or
        // annotations are updated. Evict via POST /goTerms/cache/evict or by restarting.
        String cacheKey = canonicalUri + "|p=" + propagate + "|m=" + maxTerms
                + "|t=" + ( taxon != null ? taxon.getId() : "null" );
        org.springframework.cache.Cache c = geneCountCache();
        if ( c != null ) {
            org.springframework.cache.Cache.ValueWrapper hit = c.get( cacheKey );
            if ( hit != null && hit.get() instanceof GoTermGeneCountValueObject ) {
                return ubic.gemma.rest.util.Responders.respond( ( GoTermGeneCountValueObject ) hit.get() );
            }
        }
        long t0 = System.currentTimeMillis();
        Set<String> uris = expandUris( canonicalUri, propagate, maxTerms );
        long tExpand = System.currentTimeMillis() - t0;
        long t1 = System.currentTimeMillis();
        long count = gene2GOAssociationService.countByGOTermUris( uris, taxon );
        long tCount = System.currentTimeMillis() - t1;
        if ( tExpand + tCount > 200 ) {
            log.info( String.format(
                    "goTerms/genes/count uri=%s propagate=%s maxTerms=%d uris=%d count=%d expand=%dms count=%dms",
                    canonicalUri, propagate, maxTerms, uris.size(), count, tExpand, tCount ) );
        }
        GoTermGeneCountValueObject vo = new GoTermGeneCountValueObject( canonicalUri, count, uris.size(), propagate, maxTerms );
        if ( c != null ) {
            c.put( cacheKey, vo );
        }
        return ubic.gemma.rest.util.Responders.respond( vo );
    }

    // Bespoke /cache/evict was removed once GoTermGeneCountCache became a Spring-registered
    // cache region (see EhcacheConfig#APP_CACHES). The unified admin endpoint covers it:
    // DELETE /admin/caches/GoTermGeneCountCache (or DELETE /admin/caches for everything),
    // plus the GET /admin/caches stats view shows hit/miss counters.

    /**
     * Wire payload for {@code GET /goTerms/{termUri}/genes/count}. Fields:
     * <ul>
     *   <li>{@code termUri} — the URI whose count is reported.</li>
     *   <li>{@code geneCount} — number of distinct genes annotated, including the
     *       descendants traversed during expansion when {@code propagate=true}.</li>
     *   <li>{@code termsScanned} — how many GO URIs the count covers (root + descendants
     *       that were walked under the {@code maxTerms} cap).</li>
     *   <li>{@code propagate} / {@code maxTerms} — echoed back from the request so callers
     *       can detect "I asked for unbounded but got bounded" or vice versa.</li>
     * </ul>
     */
    @lombok.Value
    public static class GoTermGeneCountValueObject {
        String termUri;
        long geneCount;
        int termsScanned;
        boolean propagate;
        int maxTerms;
    }
}
