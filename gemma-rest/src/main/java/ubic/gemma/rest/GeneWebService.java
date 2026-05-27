/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.core.analysis.sequence.ArrayDesignMapResultService;
import ubic.gemma.core.analysis.sequence.CompositeSequenceMapValueObject;
import ubic.gemma.core.search.SearchContext;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.search.SearchTimeoutException;
import ubic.gemma.core.search.ParseSearchException;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.BioAssaySetValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneOntologyTermValueObject;
import ubic.gemma.model.genome.PhysicalLocationValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.rest.util.CursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.ResponseErrorObject;
import ubic.gemma.rest.util.args.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ubic.gemma.rest.util.Responders.paginate;
import static ubic.gemma.rest.util.Responders.paginateByCursor;
import static ubic.gemma.rest.util.Responders.respond;

/**
 * RESTful interface for genes.
 * Does not have an 'all' endpoint (no use-cases).
 * Most methods also have a taxon-specific counterpart in the {@link TaxaWebService} (useful when using the 'official
 * symbol' identifier, as this class will just return a random taxon homologue).
 *
 * @author tesarst
 */
@Service
@Path("/genes")
public class GeneWebService {

    @Autowired
    private GeneService geneService;
    @Autowired
    private GeneArgService geneArgService;
    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;
    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;
    @Autowired
    private CompositeSequenceService compositeSequenceService;
    @Autowired
    private ArrayDesignMapResultService arrayDesignMapResultService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private TaxonArgService taxonArgService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve all genes",
            description = "Supports two pagination modes. Legacy mode: pass `offset` (and `limit`); response includes `offset` and `totalElements`. "
                    + "Cursor mode (recommended for deep pagination and consistency under writes): pass an opaque `cursor` token from a previous response's `nextCursor` / `prevCursor` field. "
                    + "`offset` and `cursor` are mutually exclusive — passing both yields a 400. "
                    + "In cursor mode `totalElements` is `null` by default (no count query per request).",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(oneOf = {
                                    PaginatedResponseDataObject.class,
                                    CursorPaginatedResponseDataObject.class
                            }))),
            })
    public Object getGenes(
            @QueryParam("offset") @DefaultValue("0") OffsetArg offsetArg,
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg,
            @Parameter(description = "Opaque keyset-pagination cursor token; mutually exclusive with `offset`.") @QueryParam("cursor") CursorArg cursorArg
    ) {
        if ( cursorArg != null ) {
            // mutual exclusion: an explicit, user-supplied offset is incompatible with cursor mode.
            // The default offset=0 from @DefaultValue is not considered "user-supplied" — clients
            // commonly leave offset unset when switching to cursor mode, and that should not 400.
            CursorPage<GeneValueObject> page = geneArgService.getGenesByCursor( cursorArg.getValue(), limitArg.getValue() );
            geneService.populateAssociatedExperimentCount( page );
            return paginateByCursor( page, new String[] { "id" } );
        }
        Slice<GeneValueObject> slice = geneArgService.getGenes( offsetArg.getValue(), limitArg.getValue() );
        geneService.populateAssociatedExperimentCount( slice );
        return paginate( slice, new String[] { "id" } );
    }

    /**
     * Retrieves all genes matching the identifier.
     *
     * @param genes a list of gene identifiers, separated by commas (','). Identifiers can be one of
     *              NCBI ID, Ensembl ID or official symbol. NCBI ID is the most efficient (and
     *              guaranteed to be unique) identifier. Official symbol returns a gene homologue on a random taxon.
     *              <p>
     *              Do not combine different identifiers in one query.
     *              </p>
     */
    /**
     * Free-text typeahead for genes. Shim over {@link SearchService} so the
     * curation-UI can keep calling {@code GET /genes/search?query=...} instead
     * of the canonical {@code GET /search?query=...&resultTypes=...Gene}.
     * <p>
     * Path is declared before {@link #getGenesByIds(GeneArrayArg)} (which owns
     * {@code GET /genes/{genes}}) so JAX-RS resolves the literal {@code "search"}
     * segment before falling through to the template variable.
     *
     * @param query     non-empty free-text query (symbol, alias, NCBI id, …).
     * @param taxonArg  optional — when supplied, results are scoped to that taxon.
     * @param limit     1..{@value #SEARCH_MAX_LIMIT}; default 20.
     */
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Free-text gene search (typeahead)",
            description = "Delegates to the search service with `resultTypes=Gene`. "
                    + "Returns gene value-objects ordered by search score. Hard-cap on `limit` is "
                    + SEARCH_MAX_LIMIT_STR + "; default is " + SEARCH_DEFAULT_LIMIT_STR + ".",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "Empty / invalid query, or `limit` out of range.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "503", description = "The search timed out.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public ResponseDataObject<List<GeneValueObject>> searchGenes(
            @QueryParam("query") String query,
            @QueryParam("taxon") TaxonArg<?> taxonArg,
            @QueryParam("limit") @DefaultValue(SEARCH_DEFAULT_LIMIT_STR) int limit
    ) {
        if ( query == null || query.trim().isEmpty() ) {
            throw new BadRequestException( "Search query cannot be empty." );
        }
        if ( limit < 1 || limit > SEARCH_MAX_LIMIT ) {
            throw new BadRequestException( "'limit' must be between 1 and " + SEARCH_MAX_LIMIT
                    + " (got " + limit + ")." );
        }
        ubic.gemma.model.genome.Taxon taxon = taxonArg != null ? taxonArgService.getEntity( taxonArg ) : null;
        SearchSettings settings = SearchSettings.builder()
                .query( query.trim() )
                .taxonConstraint( taxon )
                .resultTypes( Collections.singleton( Gene.class ) )
                .maxResults( limit )
                .fillResults( true )
                .build();
        List<SearchResult<?>> raw;
        try {
            raw = searchService.search( settings, new SearchContext( null, null ) ).toList();
        } catch ( ParseSearchException e ) {
            throw new BadRequestException( "Invalid search query: " + e.getQuery(), e );
        } catch ( SearchTimeoutException e ) {
            throw new ServiceUnavailableException( e.getMessage(), 30L, e.getCause() );
        } catch ( SearchException e ) {
            throw new InternalServerErrorException( e );
        }
        List<GeneValueObject> vos = new ArrayList<>( raw.size() );
        // Endpoint-level taxon filter: SearchService aggregates from multiple sources and not all
        // honour SearchSettings.taxonConstraint (HibernateSearchSource and the GO source historically
        // bypassed it). Backstop here so /genes/search never leaks cross-taxa hits.
        Long wantTaxonId = taxon != null ? taxon.getId() : null;
        for ( SearchResult<?> sr : raw ) {
            Object o = sr.getResultObject();
            GeneValueObject vo;
            if ( o instanceof GeneValueObject ) {
                vo = ( GeneValueObject ) o;
            } else if ( o instanceof Gene ) {
                vo = new GeneValueObject( ( Gene ) o );
            } else {
                // SearchResults with null resultObject (see #417) are dropped silently.
                continue;
            }
            if ( wantTaxonId != null
                    && ( vo.getTaxon() == null || !wantTaxonId.equals( vo.getTaxon().getId() ) ) ) {
                continue;
            }
            vos.add( vo );
        }
        return respond( vos );
    }

    /** Default {@code limit} for {@link #searchGenes}; sized for typeahead. */
    static final int SEARCH_DEFAULT_LIMIT = 20;
    private static final String SEARCH_DEFAULT_LIMIT_STR = "20";
    /** Upper bound on {@code limit}; requests above this are 400. */
    static final int SEARCH_MAX_LIMIT = 50;
    private static final String SEARCH_MAX_LIMIT_STR = "50";

    @GET
    @Path("/{genes}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve genes matching gene identifiers")
    public ResponseDataObject<List<GeneValueObject>> getGenesByIds( // Params:
            @PathParam("genes") GeneArrayArg genes // Required
    ) {
        SortArg<Gene> sort = SortArg.valueOf( "+id" );
        Filters filters = Filters.empty();
        filters.and( geneArgService.getFilters( genes ) );
        Slice<GeneValueObject> slice = geneService.loadValueObjects( filters, geneArgService.getSort( sort ), 0, -1 );
        geneService.populateAssociatedExperimentCount( slice );
        return respond( slice );
    }

    /**
     * Retrieves the physical location of the given gene.
     *
     * @param geneArg can either be the NCBI ID, Ensembl ID or official symbol. NCBI ID is most efficient (and
     *                guaranteed to be unique). Official symbol returns a gene homologue on a random taxon.
     */
    @GET
    @Path("/{gene}/locations")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the physical locations of a given gene")
    public ResponseDataObject<List<PhysicalLocationValueObject>> getGeneLocations( // Params:
            @PathParam("gene") GeneArg<?> geneArg // Required
    ) {
        return respond( geneArgService.getGeneLocation( geneArg ) );
    }

    /**
     * Retrieves the probes (composite sequences) with this gene.
     *
     * @param geneArg can either be the NCBI ID, Ensembl ID or official symbol. NCBI ID is most efficient (and
     *                guaranteed to be unique). Official symbol returns a gene homologue on a random taxon.
     */
    @GET
    @Path("/{gene}/probes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the probes associated to a genes across all platforms",
            description = "Supports two pagination modes. Legacy mode: pass `offset` (and `limit`); response includes `offset` and `totalElements`. "
                    + "Cursor mode (recommended for deep pagination and consistency under writes — a single gene can map to many probes across multi-platform inventories): "
                    + "pass an opaque `cursor` token from a previous response's `nextCursor` / `prevCursor` field. "
                    + "`offset` and `cursor` are mutually exclusive — passing a non-null `cursor` selects cursor mode. "
                    + "In cursor mode the result is always sorted by ascending `cs.id` (cursor mode forces a single-component id sort pending the indexed-column audit in phase B); "
                    + "the path-derived `{gene}` constraint is preserved; `totalElements` is `null` by default (no count query per request). "
                    + "Pass `summary=true` to receive an enriched per-row VO with the gene-list this probe maps to and the BLAT-hit count (replaces the legacy `getGeneCsSummaries` DWR call); "
                    + "the page shape is unchanged but each element is a `CompositeSequenceSummaryValueObject` instead of the thin `CompositeSequenceValueObject`.",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(oneOf = {
                                    PaginatedResponseDataObject.class,
                                    CursorPaginatedResponseDataObject.class
                            }))),
            })
    public Object getGeneProbes( // Params:
            @PathParam("gene") GeneArg<?> geneArg, // Required
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @Parameter(description = "Opaque keyset-pagination cursor token; mutually exclusive with `offset`.") @QueryParam("cursor") CursorArg cursorArg,
            @Parameter(description = "When true, each element is enriched with the gene-list this probe maps to and the BLAT-hit count (the legacy `getGeneCsSummaries` shape).")
            @QueryParam("summary") @DefaultValue("false") boolean summary
    ) {
        if ( cursorArg != null ) {
            // Mutual-exclusion: a non-null cursor selects cursor mode. The default offset=0 is
            // not considered user-supplied (parallels GET /platforms/{platform}/elements/{probe}/genes step 1l).
            // In cursor mode we currently force a +id sort (GeneArgService.getGeneProbesByCursor)
            // — the DAO restricts cursors to single-component id sorts until the index audit lands.
            // The path-derived gene.id constraint is preserved by the DAO query (the keyset HQL walks
            // the same gene→probe join structure as the offset variant, scoped to the resolved Gene).
            CursorPage<CompositeSequenceValueObject> page = geneArgService.getGeneProbesByCursor( geneArg, cursorArg.getValue(), limit.getValue() );
            if ( summary ) {
                Map<Long, CompositeSequenceMapValueObject> enrichment = loadProbeSummaries( page );
                CursorPage<CompositeSequenceSummaryValueObject> enriched = page.map( probe -> toSummaryVo( probe, enrichment.get( probe.getId() ) ) );
                return paginateByCursor( enriched, new String[] { "id" } );
            }
            return paginateByCursor( page, new String[] { "id" } );
        }
        Slice<CompositeSequenceValueObject> slice = geneArgService.getGeneProbes( geneArg, offset.getValue(), limit.getValue() );
        if ( summary ) {
            Map<Long, CompositeSequenceMapValueObject> enrichment = loadProbeSummaries( slice );
            Slice<CompositeSequenceSummaryValueObject> enriched = slice.map( probe -> toSummaryVo( probe, enrichment.get( probe.getId() ) ) );
            return paginate( enriched, new String[] { "id" } );
        }
        return paginate( slice, new String[] { "id" } );
    }

    /**
     * Build the per-probe enrichment map for the given page of probe VOs &mdash; one
     * {@code getRawSummary} hit over the page's probe IDs, fanned into a
     * {@link CompositeSequenceMapValueObject} per probe via the existing
     * {@link ArrayDesignMapResultService} aggregator. Returned map is keyed by
     * {@code compositeSequence.id} for O(1) lookup during slice mapping; probes with no
     * sequence-analysis rows (no BLAT hits, no gene-product mappings) are absent from
     * the map and surface as a summary VO with an empty gene list and {@code numBlatHits=null}.
     */
    private Map<Long, CompositeSequenceMapValueObject> loadProbeSummaries( List<CompositeSequenceValueObject> probes ) {
        if ( probes.isEmpty() ) {
            return Collections.emptyMap();
        }
        List<Long> ids = probes.stream().map( CompositeSequenceValueObject::getId ).collect( Collectors.toList() );
        Collection<CompositeSequence> entities = compositeSequenceService.load( ids );
        Collection<Object[]> rawSummaries = compositeSequenceService.getRawSummary( entities );
        if ( rawSummaries == null || rawSummaries.isEmpty() ) {
            return Collections.emptyMap();
        }
        Collection<CompositeSequenceMapValueObject> summaries = arrayDesignMapResultService.getSummaryMapValueObjects( rawSummaries );
        Map<Long, CompositeSequenceMapValueObject> byId = new HashMap<>( summaries.size() );
        for ( CompositeSequenceMapValueObject s : summaries ) {
            if ( s.getCompositeSequenceId() != null ) {
                byId.put( Long.parseLong( s.getCompositeSequenceId() ), s );
            }
        }
        return byId;
    }

    private static CompositeSequenceSummaryValueObject toSummaryVo( CompositeSequenceValueObject probe, @org.springframework.lang.Nullable CompositeSequenceMapValueObject mapVo ) {
        List<GeneValueObject> genes;
        Integer numBlatHits;
        if ( mapVo != null ) {
            genes = new ArrayList<>( mapVo.getGenes().values() );
            numBlatHits = mapVo.getNumBlatHits();
        } else {
            genes = Collections.emptyList();
            numBlatHits = null;
        }
        return new CompositeSequenceSummaryValueObject( probe, genes, genes.size(), numBlatHits );
    }

    /**
     * Refresh gene-to-probe associations.
     */
    @GET
    @Path("/probes/refresh")
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Refresh gene-to-probe associations.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    // FIXME: this is broken, see https://github.com/swagger-api/swagger-core/issues/4693
                    @ApiResponse(responseCode = "204")
            })
    public Response refreshGenesProbes() {
        tableMaintenanceUtil.evictGene2CsQueryCache();
        return Response.noContent().build();
    }

    /**
     * Retrieves the GO terms of the given gene.
     *
     * @param geneArg can either be the NCBI ID, Ensembl ID or official symbol. NCBI ID is most efficient (and
     *                guaranteed to be unique). Official symbol returns a gene homologue on a random taxon.
     */
    @GET
    @Path("/{gene}/goTerms")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the GO terms associated to a gene")
    public ResponseDataObject<List<GeneOntologyTermValueObject>> getGeneGoTerms( // Params:
            @PathParam("gene") GeneArg<?> geneArg // Required
    ) {
        return respond( geneArgService.getGeneGoTerms( geneArg ) );
    }

    /**
     * Retrieves a fully-populated overview of the given gene, suitable for rendering
     * the gene-page header in gemma-curation-ui. Replaces the legacy
     * {@code GeneController.loadGeneDetails(Long)} DWR call.
     *
     * <p>The returned VO carries: aliases, multifunctionality rank, composite-sequence
     * count, platform count, gene-set memberships, homologues, GO-term count, and the
     * associated-experiment count (filled in by {@code populateAssociatedExperimentCount}).</p>
     *
     * @param geneArg can either be the NCBI ID, Ensembl ID or official symbol. NCBI ID is most efficient (and
     *                guaranteed to be unique). Official symbol returns a gene homologue on a random taxon.
     */
    @GET
    @Path("/{gene}/overview")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve a fully-populated overview of a gene",
            description = "Returns the gene VO populated with aliases, multifunctionality rank, composite-sequence count, platform count, gene-set memberships, homologues, GO-term count, and associated-experiment count. Replaces the legacy `loadGeneDetails` DWR call used by the gemma-web gene page.",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class))),
                    @ApiResponse(responseCode = "404", description = "Gene not found",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public ResponseDataObject<GeneValueObject> getGeneOverview( // Params:
            @PathParam("gene") GeneArg<?> geneArg // Required
    ) {
        Gene gene = geneArgService.getEntity( geneArg );
        GeneValueObject gvo = geneService.loadFullyPopulatedValueObject( gene.getId() );
        if ( gvo == null ) {
            // getEntity already throws 404 on missing, but guard against a race where the
            // gene is removed between the resolve and the fat-loader call.
            throw new NotFoundException( "No gene found with id=" + gene.getId() );
        }
        gvo.setNumGoTerms( geneService.findGOTerms( gene.getId() ).size() );
        return respond( gvo );
    }

    /**
     * Retrieves the homologues of the given gene. Single-purpose subset of
     * {@link #getGeneOverview} for callers that only need the homologue list.
     *
     * @param geneArg can either be the NCBI ID, Ensembl ID or official symbol. NCBI ID is most efficient (and
     *                guaranteed to be unique). Official symbol returns a gene homologue on a random taxon.
     */
    @GET
    @Path("/{gene}/homologues")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the homologues of a gene",
            description = "Returns the gene's homologues across all taxa (via the homologene service). The legacy gemma-web gene page surfaces this in the Overview tab.",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class))),
                    @ApiResponse(responseCode = "404", description = "Gene not found",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public ResponseDataObject<Collection<GeneValueObject>> getGeneHomologues( // Params:
            @PathParam("gene") GeneArg<?> geneArg // Required
    ) {
        Gene gene = geneArgService.getEntity( geneArg );
        GeneValueObject gvo = geneService.loadFullyPopulatedValueObject( gene.getId() );
        if ( gvo == null ) {
            throw new NotFoundException( "No gene found with id=" + gene.getId() );
        }
        Collection<GeneValueObject> homologues = gvo.getHomologues();
        return respond( homologues != null ? homologues : Collections.<GeneValueObject>emptyList() );
    }

    /**
     * Retrieves the differential expression results for the given gene across all experiments
     * the caller has access to (ACL-filtered downstream).
     *
     * <p>Wraps {@link DifferentialExpressionResultService#findByGene(Gene, boolean, boolean, double, int)}
     * with {@code useGene2Cs=true} and {@code keepNonSpecificProbes=false} — matches the
     * convention used by the dataset-scoped DEA endpoint in {@code DatasetsWebService}.</p>
     *
     * <p>The cold-cache latency on this path (~4s for high-traffic genes like TP53) is mitigated by
     * {@code DiffExGeneWarmupService} which periodically re-runs the underlying call for a seed gene
     * list. See {@code PERF_PROBE_REPORT_ROUND3.md} §C1.</p>
     *
     * @param geneArg can either be the NCBI ID, Ensembl ID or official symbol. NCBI ID is most efficient (and
     *                guaranteed to be unique). Official symbol returns a gene homologue on a random taxon.
     * @param threshold optional q/p-value threshold. Defaults to 1.0 (no filtering).
     * @param limit optional cap on results returned per experiment grouping. -1 means no cap.
     */
    @GET
    @Path("/{gene}/differentialExpression")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve differential expression results for a gene across all accessible experiments",
            description = "Returns a flat list of per-experiment groupings, each with the experiment VO and the list of probe-level DEA results for the given gene. "
                    + "Results are scoped to experiments the caller has read access to (ACL-filtered). "
                    + "Cold-cache latency is mitigated by a scheduled warm-up of a seed gene list (`gemma.diffex.warmup.*`).",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class))),
                    @ApiResponse(responseCode = "404", description = "Gene not found",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public ResponseDataObject<List<GeneDifferentialExpressionGroupValueObject>> getGeneDifferentialExpression( // Params:
            @PathParam("gene") GeneArg<?> geneArg, // Required
            @Parameter(description = "Maximum threshold on the corrected P-value to retain a result (inclusive). Default 1.0 returns all.",
                    schema = @Schema(minimum = "0.0", maximum = "1.0"))
            @QueryParam("threshold") @DefaultValue("1.0") double threshold,
            @Parameter(description = "Cap on results returned per experiment grouping; -1 (default) means no cap.")
            @QueryParam("limit") @DefaultValue("-1") int limit
    ) {
        if ( threshold < 0 || threshold > 1 ) {
            throw new BadRequestException( "The threshold must be in the [0, 1] interval." );
        }
        Gene gene = geneArgService.getEntity( geneArg );
        Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> grouped =
                differentialExpressionResultService.findByGene( gene, true, false, threshold, limit );
        List<GeneDifferentialExpressionGroupValueObject> payload = new ArrayList<>( grouped.size() );
        for ( Map.Entry<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> e : grouped.entrySet() ) {
            payload.add( new GeneDifferentialExpressionGroupValueObject( e.getKey(), e.getValue() ) );
        }
        return respond( payload );
    }

    /**
     * One experiment's DEA results for the requested gene. The outer list returned by
     * {@link #getGeneDifferentialExpression} is a flat sequence of these — a JSON-friendly
     * rendering of the {@code Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>>}
     * the underlying service hands back (maps don't serialize cleanly when the key is a complex VO).
     */
    @Data
    public static class GeneDifferentialExpressionGroupValueObject {
        private final BioAssaySetValueObject experiment;
        private final List<DifferentialExpressionValueObject> results;
    }

    /**
     * Enriched per-probe row returned by {@link #getGeneProbes} when {@code summary=true}.
     * Replaces the legacy DWR {@code CompositeSequenceController.getGeneCsSummaries} shape:
     * for each probe (composite sequence) on the page, carries the thin probe VO plus the
     * list of genes this probe maps to and the distinct-BLAT-hit count.
     * <p>
     * {@code numGenes} duplicates {@code genes.size()} as a UI convenience (avoids forcing
     * the client to count when only the cardinality matters). {@code numBlatHits} is the
     * count of distinct sequence-similarity hits (chrom + target-start + target-end + target-starts
     * + query-sequence), aggregated by {@code ArrayDesignMapResultService}; null when the probe
     * has no sequence-analysis rows.
     */
    @Data
    public static class CompositeSequenceSummaryValueObject implements Serializable {
        private static final long serialVersionUID = 1L;
        private final CompositeSequenceValueObject probe;
        private final List<GeneValueObject> genes;
        private final int numGenes;
        @org.springframework.lang.Nullable
        private final Integer numBlatHits;
    }
}
