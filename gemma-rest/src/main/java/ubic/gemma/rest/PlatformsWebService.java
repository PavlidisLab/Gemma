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
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.lang.Nullable;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.core.analysis.sequence.BlatResult2Psl;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.BlatResultService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.search.SearchTimeoutException;
import ubic.gemma.core.search.ParseSearchException;
import ubic.gemma.core.search.SearchContext;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.annotations.GZIP;
import ubic.gemma.rest.util.CursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.FilteredAndCursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.FilteredAndPaginatedResponseDataObject;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.ResponseErrorObject;
import ubic.gemma.rest.util.args.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ubic.gemma.rest.util.Responders.paginate;
import static ubic.gemma.rest.util.Responders.paginateByCursor;
import static ubic.gemma.rest.util.Responders.respond;

/**
 * RESTful interface for platforms.
 *
 * @author tesarst
 */
@Service
@Path("/platforms")
@Slf4j
public class PlatformsWebService {

    private static final String ERROR_ANNOTATION_FILE_NOT_AVAILABLE = "The %s annotation file for platform %s does not exist or can not be accessed.";
    private static final String ERROR_ANNOTATION_FILE_CANNOT_BE_GENERATED = "Annotation file for platform %s is not on disk and this instance cannot generate it: %s";
    private static final String ERROR_NO_ALIGNMENTS = "Platform element %s on %s has no BLAT alignments that can be placed in the genome browser.";

    public static final String TEXT_TAB_SEPARATED_VALUES_UTF8 = "text/tab-separated-values; charset=UTF-8";
    public static final MediaType TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE = new MediaType( "text", "tab-separated-values", "UTF-8" );

    public static final String TEXT_PLAIN_UTF8 = "text/plain; charset=UTF-8";
    public static final MediaType TEXT_PLAIN_UTF8_TYPE = new MediaType( "text", "plain", "UTF-8" );

    @Autowired
    private GeneService geneService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private CompositeSequenceService compositeSequenceService;
    @Autowired
    private ArrayDesignAnnotationService annotationFileService;
    @Autowired
    private ArrayDesignReportService arrayDesignReportService;
    @Autowired
    private PlatformArgService arrayDesignArgService;
    @Autowired
    private CompositeSequenceArgService probeArgService;
    @Autowired
    private AccessDecisionManager accessDecisionManager;
    @Autowired
    private TicketsWebService ticketsWebService;
    @Autowired
    private BioSequenceService bioSequenceService;
    @Autowired
    private BlatResultService blatResultService;

    /**
     * Written into the PSL track's provenance comment so a track pasted into UCSC says which Gemma
     * produced it.
     */
    @Value("${gemma.hosturl}")
    private String hostUrl;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve all platforms",
            description = "Supports two pagination modes. Legacy mode: pass `offset` (and `limit`); response includes `offset` and `totalElements`. "
                    + "Cursor mode (recommended for deep pagination and consistency under writes): pass an opaque `cursor` token from a previous response's `nextCursor` / `prevCursor` field. "
                    + "`offset` and `cursor` are mutually exclusive — passing a non-null `cursor` selects cursor mode. "
                    + "In cursor mode the result is always sorted by ascending `id` (the user `sort` arg is currently ignored, pending the indexed-column audit in phase B); `totalElements` is `null` by default (no count query per request).",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(oneOf = {
                                    PaginatedResponseDataObject.class,
                                    FilteredAndCursorPaginatedResponseDataObject.class
                            }))),
            })
    public Object getPlatforms( // Params:
            @QueryParam("filter") @DefaultValue("") FilterArg<ArrayDesign> filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg<ArrayDesign> sort, // Optional, default +id
            @Parameter(description = "Opaque keyset-pagination cursor token; mutually exclusive with `offset`.") @QueryParam("cursor") CursorArg cursorArg,
            @Parameter(description = "Opt-in: populate `numberOfGenes` (distinct genes the platform's elements map to) and `numberOfMappedElements` (elements with at least one gene mapping). Off by default — the counts aggregate the whole gene-to-element mapping for each platform on the page, which is wasted work for callers that do not render them.") @QueryParam("withGeneCounts") @DefaultValue("false") boolean withGeneCounts
    ) {
        Filters filters = arrayDesignArgService.getFilters( filter );
        if ( cursorArg != null ) {
            // Mutual-exclusion: a non-null cursor selects cursor mode. The default offset=0 is
            // not considered user-supplied (parallels GET /genes step 1b). In cursor mode we
            // currently force a +id sort (PlatformArgService.getPlatformsByCursor) — the DAO
            // restricts cursors to single-component id sorts until the index audit lands.
            CursorPage<ArrayDesignValueObject> page = arrayDesignArgService.getPlatformsByCursor(
                    filters, cursorArg.getValue(), limit.getValue() );
            if ( withGeneCounts ) {
                hydrateGeneCounts( page );
            }
            return new FilteredAndCursorPaginatedResponseDataObject<>( page, filters, new String[] { "id" } );
        }
        FilteredAndPaginatedResponseDataObject<ArrayDesignValueObject> response =
                paginate( arrayDesignService::loadValueObjects, filters, new String[] { "id" },
                        arrayDesignArgService.getSort( sort ), offset.getValue(), limit.getValue() );
        if ( withGeneCounts ) {
            hydrateGeneCounts( response.getData() );
        }
        return response;
    }

    @GET
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Count platforms matching the provided filter")
    public ResponseDataObject<Long> getNumberOfPlatforms(
            @QueryParam("filter") @DefaultValue("") FilterArg<ArrayDesign> filter ) {
        return respond( arrayDesignService.count( arrayDesignArgService.getFilters( filter ) ) );
    }

    /**
     * Retrieves all datasets matching the given identifiers.
     *
     * @param platformsArg a list of identifiers, separated by commas (','). Identifiers can either be the
     *                    ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                    is more efficient.
     *                    <p>
     *                    Only datasets that user has access to will be available.
     *                    </p>
     *                    <p>
     *                    Do not combine different identifiers in one query.
     *                    </p>
     */
    @GET
    @Path("/{platform}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve all platforms matching a set of platform identifiers",
            description = "Supports two pagination modes. Legacy mode: pass `offset` (and `limit`); response includes `offset` and `totalElements`. "
                    + "Cursor mode (recommended for deep pagination and consistency under writes): pass an opaque `cursor` token from a previous response's `nextCursor` / `prevCursor` field. "
                    + "`offset` and `cursor` are mutually exclusive — passing a non-null `cursor` selects cursor mode. "
                    + "In cursor mode the result is always sorted by ascending `id` (the user `sort` arg is currently ignored, pending the indexed-column audit in phase B); "
                    + "the path-derived platform-identifier predicate is preserved on top of the user-supplied `?filter=`; `totalElements` is `null` by default (no count query per request).",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(oneOf = {
                                    FilteredAndPaginatedResponseDataObject.class,
                                    FilteredAndCursorPaginatedResponseDataObject.class
                            }))),
            })
    public Object getPlatformsByIds( // Params:
            @PathParam("platform") PlatformArrayArg platformsArg, // Optional
            @QueryParam("filter") @DefaultValue("") FilterArg<ArrayDesign> filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg<ArrayDesign> sort, // Optional, default +id
            @Parameter(description = "Opaque keyset-pagination cursor token; mutually exclusive with `offset`.") @QueryParam("cursor") CursorArg cursorArg,
            @Parameter(description = "Opt-in: populate `numberOfGenes` (distinct genes the platform's elements map to) and `numberOfMappedElements` (elements with at least one gene mapping).") @QueryParam("withGeneCounts") @DefaultValue("false") boolean withGeneCounts
    ) {
        Filters filters = arrayDesignArgService.getFilters( filter )
                .and( arrayDesignArgService.getFilters( platformsArg ) );
        if ( cursorArg != null ) {
            // Mutual-exclusion: a non-null cursor selects cursor mode. The default offset=0 is
            // not considered user-supplied (parallels GET /platforms step 1c). In cursor mode we
            // currently force a +id sort (PlatformArgService.getPlatformsByCursor) — the DAO
            // restricts cursors to single-component id sorts until the index audit lands.
            // The path-derived id-set predicate is preserved by composing it into `filters`
            // before the DAO call, so the cursor-mode result is restricted to the same set of
            // platform identifiers that the offset-mode result would be.
            CursorPage<ArrayDesignValueObject> page = arrayDesignArgService.getPlatformsByCursor(
                    filters, cursorArg.getValue(), limit.getValue() );
            if ( withGeneCounts ) {
                hydrateGeneCounts( page );
            }
            return new FilteredAndCursorPaginatedResponseDataObject<>( page, filters, new String[] { "id" } );
        }
        FilteredAndPaginatedResponseDataObject<ArrayDesignValueObject> response =
                paginate( arrayDesignService::loadValueObjects, filters, new String[] { "id" },
                        arrayDesignArgService.getSort( sort ), offset.getValue(), limit.getValue() );
        if ( withGeneCounts ) {
            hydrateGeneCounts( response.getData() );
        }
        return response;
    }

    @GET
    @Path("/blacklisted")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Retrieve all blacklisted platforms", hidden = true,
            description = "Supports two pagination modes. Legacy mode: pass `offset` (and `limit`); response includes `offset` and `totalElements`. "
                    + "Cursor mode (recommended for deep pagination and consistency under writes): pass an opaque `cursor` token from a previous response's `nextCursor` / `prevCursor` field. "
                    + "`offset` and `cursor` are mutually exclusive — passing a non-null `cursor` selects cursor mode. "
                    + "In cursor mode the result is always sorted by ascending `id` (the user `sort` arg is currently ignored, pending the indexed-column audit in phase B); the blacklist short-name/accession predicate is preserved on top of the user-supplied `?filter=`; `totalElements` is `null` by default (no count query per request).",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(oneOf = {
                                    FilteredAndPaginatedResponseDataObject.class,
                                    FilteredAndCursorPaginatedResponseDataObject.class
                            }))),
            })
    public Object getBlacklistedPlatforms(
            @QueryParam("filter") @DefaultValue("") FilterArg<ArrayDesign> filter,
            @QueryParam("sort") @DefaultValue("+id") SortArg<ArrayDesign> sort,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset,
            @QueryParam("limit") @DefaultValue("20") LimitArg limit,
            @Parameter(description = "Opaque keyset-pagination cursor token; mutually exclusive with `offset`.") @QueryParam("cursor") CursorArg cursorArg
    ) {
        Filters filters = arrayDesignArgService.getFilters( filter );
        if ( cursorArg != null ) {
            // Mutual-exclusion: a non-null cursor selects cursor mode. The default offset=0 is
            // not considered user-supplied (parallels GET /platforms step 1c). In cursor mode we
            // currently force a +id sort (PlatformArgService.getBlacklistedPlatformsByCursor) —
            // the DAO restricts cursors to single-component id sorts until the index audit lands.
            // The blacklist short-name/accession predicate is composed inside the DAO so the
            // blacklist scope is enforced identically in both modes.
            CursorPage<ArrayDesignValueObject> page = arrayDesignArgService.getBlacklistedPlatformsByCursor(
                    filters, cursorArg.getValue(), limit.getValue() );
            return new FilteredAndCursorPaginatedResponseDataObject<>( page, filters, new String[] { "id" } );
        }
        return paginate( arrayDesignService::loadBlacklistedValueObjects, filters,
                new String[] { "id" }, arrayDesignArgService.getSort( sort ), offset.getValue(), limit.getValue() );
    }

    /**
     * Retrieves experiments in the given platform.
     *
     * @param platformArg can either be the ArrayDesign ID or its short name (e.g. "GPL1355" ). Retrieval by ID
     *                    is more efficient. Only platforms that user has access to will be available.
     * @param offset      optional parameter (defaults to 0) skips the specified amount of datasets when retrieving them
     *                    from the database.
     * @param limit       optional parameter (defaults to 20) limits the result to specified amount of datasets. Use 0
     */
    @GET
    @Path("/{platform}/datasets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve all experiments using a given platform",
            description = "Supports two pagination modes. Legacy mode: pass `offset` (and `limit`); response includes `offset` and `totalElements`. "
                    + "Cursor mode (recommended for deep pagination and consistency under writes): pass an opaque `cursor` token from a previous response's `nextCursor` / `prevCursor` field. "
                    + "`offset` and `cursor` are mutually exclusive — passing a non-null `cursor` selects cursor mode. "
                    + "In cursor mode the result is always sorted by ascending `id` (legacy offset mode keys off `bioAssays.arrayDesignUsed.id`; cursor mode forces a single-component id sort pending the indexed-column audit in phase B); the `bioAssays.arrayDesignUsed.id = ?` constraint is preserved; `totalElements` is `null` by default (no count query per request).",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(oneOf = {
                                    PaginatedResponseDataObject.class,
                                    CursorPaginatedResponseDataObject.class
                            }))),
            })
    public Object getPlatformDatasets( // Params:
            @PathParam("platform") PlatformArg<?> platformArg, // Required
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @Parameter(description = "Opaque keyset-pagination cursor token; mutually exclusive with `offset`.") @QueryParam("cursor") CursorArg cursorArg
    ) {
        if ( cursorArg != null ) {
            // Mutual-exclusion: a non-null cursor selects cursor mode. The default offset=0 is
            // not considered user-supplied (parallels GET /platforms/{platform}/elements step 1e).
            // In cursor mode we currently force a +id sort (PlatformArgService.getExperimentsByCursor)
            // — the DAO restricts cursors to single-component id sorts until the index audit lands.
            // The path-derived bioAssays.arrayDesignUsed.id filter is composed into the Filters
            // inside getExperimentsByCursor so the platform scope is enforced identically in both
            // modes.
            CursorPage<ExpressionExperimentValueObject> page = arrayDesignArgService.getExperimentsByCursor(
                    platformArg, cursorArg.getValue(), limit.getValue() );
            return paginateByCursor( page, new String[] { "id" } );
        }
        return paginate( arrayDesignArgService.getExperiments( platformArg, limit.getValue(), offset.getValue() ), new String[] { "id" } );
    }

    /**
     * Retrieves the composite sequences (elements) for the given platform.
     *
     * @param platformArg can either be the ArrayDesign ID or its short name (e.g. "GPL1355" ). Retrieval by ID
     *                    is more efficient. Only platforms that user has access to will be available.
     * @param offset      optional parameter (defaults to 0) skips the specified amount of datasets when retrieving them
     *                    from the database.
     * @param limit       optional parameter (defaults to 20) limits the result to specified amount of datasets. Use 0
     */
    @GET
    @Path("/{platform}/elements")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the probes for a given platform",
            description = "Supports two pagination modes. Legacy mode: pass `offset` (and `limit`); response includes `offset` and `totalElements`. "
                    + "Cursor mode (recommended for deep pagination and consistency under writes): pass an opaque `cursor` token from a previous response's `nextCursor` / `prevCursor` field. "
                    + "`offset` and `cursor` are mutually exclusive — passing a non-null `cursor` selects cursor mode. "
                    + "In cursor mode the result is always sorted by ascending `id` (legacy offset mode uses the DAO default order; cursor mode forces a single-component id sort pending the indexed-column audit in phase B); the `arrayDesign.id = ?` constraint is preserved; `totalElements` is `null` by default (no count query per request).",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(oneOf = {
                                    PaginatedResponseDataObject.class,
                                    CursorPaginatedResponseDataObject.class
                            }))),
            })
    public Object getPlatformElements( // Params:
            @PathParam("platform") PlatformArg<?> platformArg, // Required
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @Parameter(description = "Opaque keyset-pagination cursor token; mutually exclusive with `offset`.") @QueryParam("cursor") CursorArg cursorArg,
            @Parameter(description = "Opt-in: populate `sequence` (raw probe sequence string) and `sequenceLength` on each element. Off by default to keep the listing response small — sequences are 25-300bp per probe and would inflate a 22k-element page by ~1 MB.") @QueryParam("withSequence") @DefaultValue("false") boolean withSequence,
            @Parameter(description = "Opt-in: populate `genes` (compact `{id, officialSymbol, ncbiId}` per mapped gene) on each element. Off by default; costs one extra batch query per page. An element that maps to no gene gets `[]`, so an empty list is distinguishable from the field not being requested.") @QueryParam("withGenes") @DefaultValue("false") boolean withGenes,
            @Parameter(description = "Restrict to the elements mapping to this gene. Free text, resolved through gene search — an official symbol, an alias/synonym (`p53` finds TP53), or an NCBI id all work. Scoped to the platform's own taxon, so no taxon argument is needed. A query matching no gene returns an empty page.") @QueryParam("gene") QueryArg geneQuery,
            @QueryParam("filter") @DefaultValue("") FilterArg<CompositeSequence> filter // Optional, default no restriction
    ) {
        // Resolve ?gene= before touching pagination: the resolution is a search, and a query that
        // matches no gene on this platform's taxon has to yield an empty page rather than silently
        // degrading to the unfiltered listing.
        Collection<Long> geneIds = geneQuery != null
                ? resolveGeneIdsForPlatform( platformArg, geneQuery )
                : null;
        Filters userFilters = probeArgService.getFilters( filter );
        if ( cursorArg != null ) {
            // Mutual-exclusion: a non-null cursor selects cursor mode. The default offset=0 is
            // not considered user-supplied (parallels GET /platforms step 1c). In cursor mode we
            // currently force a +id sort (PlatformArgService.getElementsByCursor) — the DAO
            // restricts cursors to single-component id sorts until the index audit lands.
            // The path-derived arrayDesign.id filter is composed into the Filters inside
            // getElementsByCursor so the platform scope is enforced identically in both modes.
            CursorPage<CompositeSequenceValueObject> page = arrayDesignArgService.getElementsByCursor(
                    platformArg, userFilters, geneIds, cursorArg.getValue(), limit.getValue(), withSequence, withGenes );
            return new FilteredAndCursorPaginatedResponseDataObject<>( page, userFilters, new String[] { "id" } );
        }
        return paginate( arrayDesignArgService.getElements( platformArg, userFilters, geneIds, limit.getValue(), offset.getValue(), withSequence, withGenes ), userFilters, new String[] { "id" } );
    }

    /**
     * Populate {@code numberOfGenes} + {@code numberOfMappedElements} on a page of platform VOs.
     * <p>
     * These counts are NOT computed live. Measured against production, counting distinct genes and
     * mapped elements for the single largest platform takes ~1.7&nbsp;s warm (21,288 genes over
     * 114,159 elements) — past the point where a per-request computation is acceptable, and a page
     * of twenty platforms multiplies it. The counts also change only when a platform's sequence or
     * gene mappings are recomputed, which is why {@code ArrayDesignReportService} keeps them in a
     * disk report: written by the pipeline whenever mappings change, and by a monthly scheduled job
     * ({@code SchedulerConfig.arrayDesignReportTrigger}).
     * <p>
     * Two sources, in order:
     * <ol>
     * <li>Gene-list platforms (how RNA-seq data is represented — the "elements" ARE genes) need no
     * report at all: the element count IS both counts, and that is one indexed count on
     * {@code COMPOSITE_SEQUENCE}.</li>
     * <li>Microarray platforms come from the disk report. Absent report means the counts stay null
     * — deliberately, rather than falling back to the slow query. Null here means "not computed
     * yet", which the report timestamp lets a client distinguish from a real zero.</li>
     * </ol>
     */
    private void hydrateGeneCounts( Iterable<ArrayDesignValueObject> vos ) {
        List<ArrayDesignValueObject> pending = new ArrayList<>();
        for ( ArrayDesignValueObject vo : vos ) {
            if ( vo.getId() == null ) {
                continue;
            }
            if ( isGeneList( vo ) ) {
                // Elements are genes one-for-one, so a single indexed count answers both, with no
                // dependence on a report having been generated.
                ArrayDesign ad = arrayDesignService.load( vo.getId() );
                if ( ad != null ) {
                    long elements = arrayDesignService.countCompositeSequences( ad );
                    vo.setNumberOfGenes( elements );
                    vo.setNumberOfMappedElements( elements );
                    continue;
                }
            }
            pending.add( vo );
        }
        if ( pending.isEmpty() ) {
            return;
        }
        // fillInValueObjects reads one serialized report per platform and leaves the VO untouched
        // when none exists, so platforms without a report keep null counts.
        arrayDesignReportService.fillInValueObjects( pending );
        for ( ArrayDesignValueObject vo : pending ) {
            vo.setNumberOfGenes( parseReportCount( vo.getNumGenes() ) );
            vo.setNumberOfMappedElements( parseReportCount( vo.getNumProbesToGenes() ) );
            vo.setGeneCountsLastUpdated( vo.getDateCached() );
        }
    }

    /**
     * True for the platform kinds whose elements are genes rather than probes — {@code GENELIST} is
     * what processed RNA-seq lands on, and {@code SEQUENCING} is its upstream sibling.
     */
    private static boolean isGeneList( ArrayDesignValueObject vo ) {
        String tt = vo.getTechnologyType();
        return TechnologyType.GENELIST.name().equals( tt ) || TechnologyType.SEQUENCING.name().equals( tt );
    }

    /**
     * The report stores its counts as strings. A malformed or absent entry yields null rather than
     * a zero, so "no report" never renders as "this platform maps to no genes".
     */
    @Nullable
    private static Long parseReportCount( @Nullable String value ) {
        if ( value == null ) {
            return null;
        }
        try {
            return Long.parseLong( value.trim() );
        } catch ( NumberFormatException e ) {
            return null;
        }
    }

    /**
     * Resolve a free-text {@code ?gene=} query to the gene ids an element listing should be
     * restricted to, scoped to the taxon of {@code platformArg}.
     * <p>
     * Deliberately goes through {@link SearchService} rather than
     * {@code GeneService.findByOfficialSymbol}: the visitor typing into the platform page's element
     * box types what they know a gene as, which is frequently an alias or an older symbol
     * ({@code p53} for TP53, {@code Cx43} for GJA1) that an exact-symbol lookup misses entirely.
     * The platform fixes the taxon, so no taxon argument is needed and cross-species orthologs are
     * excluded up front.
     * <p>
     * Returns the top-ranked gene plus any that tie it on both score and match kind — a genuine
     * ambiguity, e.g. one alias carried by two genes. Deliberately NOT the whole ranked list: the
     * page is ordered by element id, not by gene relevance, so admitting weaker matches would
     * scatter their probes through the results with nothing to indicate they ranked lower.
     * Pair with {@code withGenes=true} to show which gene each returned element matched.
     */
    private Collection<Long> resolveGeneIdsForPlatform( PlatformArg<?> platformArg, QueryArg geneQuery ) {
        ArrayDesign platform = arrayDesignArgService.getEntity( platformArg );
        String query = geneQuery.getValue().trim();
        SearchSettings settings = SearchSettings.builder()
                .query( query )
                .taxonConstraint( platform.getPrimaryTaxon() )
                .resultTypes( Collections.singleton( Gene.class ) )
                // Wide candidate window, then rank, then cut — same reasoning as
                // GeneWebService.searchGenes: cutting before the taxon filter and the re-rank would
                // let an arbitrary ortholog decide the answer.
                .maxResults( GENE_SEARCH_CANDIDATE_LIMIT )
                .fillResults( true )
                .build();
        List<SearchResult<?>> raw;
        try {
            raw = new ArrayList<>( searchService.search( settings, new SearchContext( null, null ) ).toList() );
        } catch ( ParseSearchException e ) {
            throw new BadRequestException( "Invalid gene query: " + e.getQuery(), e );
        } catch ( SearchTimeoutException e ) {
            throw new ServiceUnavailableException( e.getMessage(), 30L, e.getCause() );
        } catch ( SearchException e ) {
            throw new InternalServerErrorException( e );
        }
        raw.sort( GeneWebService.searchRankingComparator( query.toLowerCase( Locale.ROOT ) ) );
        Long wantTaxonId = platform.getPrimaryTaxon() != null ? platform.getPrimaryTaxon().getId() : null;
        List<Long> ids = new ArrayList<>();
        Double topScore = null;
        Object topMatchKind = null;
        for ( SearchResult<?> sr : raw ) {
            Object o = sr.getResultObject();
            Long geneId;
            Long taxonId;
            if ( o instanceof GeneValueObject ) {
                GeneValueObject vo = ( GeneValueObject ) o;
                geneId = vo.getId();
                taxonId = vo.getTaxon() != null ? vo.getTaxon().getId() : null;
            } else if ( o instanceof Gene ) {
                Gene g = ( Gene ) o;
                geneId = g.getId();
                taxonId = g.getTaxon() != null ? g.getTaxon().getId() : null;
            } else {
                // SearchResults with a null resultObject carry no identity to filter or key on.
                continue;
            }
            if ( geneId == null ) {
                continue;
            }
            // Backstop the taxon constraint here too: SearchService aggregates across sources and
            // not all of them honour SearchSettings.taxonConstraint (see GeneWebService.searchGenes).
            if ( wantTaxonId != null && !wantTaxonId.equals( taxonId ) ) {
                continue;
            }
            if ( topScore == null ) {
                topScore = sr.getScore();
                topMatchKind = sr.getMatchKind();
            } else if ( sr.getScore() != topScore.doubleValue()
                    || !java.util.Objects.equals( topMatchKind, sr.getMatchKind() ) ) {
                break;
            }
            ids.add( geneId );
        }
        return ids;
    }

    /**
     * Candidate window requested from the search service when resolving {@code ?gene=}, before the
     * rank and the taxon backstop narrow it to the winning gene. Matches
     * {@code GeneWebService.SEARCH_CANDIDATE_LIMIT} — only ids and scores are materialized at this
     * width.
     */
    private static final int GENE_SEARCH_CANDIDATE_LIMIT = 500;

    /**
     * Retrieves composite sequences (elements) of the given platform.
     *
     * @param platformArg can either be the ArrayDesign ID or its short name (e.g. "GPL1355" ). Retrieval by ID
     *                    is more efficient. Only platforms that user has access to will be available.
     * @param probesArg   a list of identifiers, separated by commas (','). Identifiers can either be the
     *                    CompositeSequence ID or its name (e.g. AFFX_Rat_beta-actin_M_at).
     *                    <p>
     *                    Only elements on platforms that user has access to will be available.
     *                    </p>
     *                    <p>
     *                    Do not combine different identifiers in one query.
     *                    </p>
     */
    @GET
    @Path("/{platform}/elements/{probes}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the selected probes for a given platform",
            description = "Supports two pagination modes. Legacy mode: pass `offset` (and `limit`); response includes `offset` and `totalElements`. "
                    + "Cursor mode (recommended for deep pagination and consistency under writes): pass an opaque `cursor` token from a previous response's `nextCursor` / `prevCursor` field. "
                    + "`offset` and `cursor` are mutually exclusive — passing a non-null `cursor` selects cursor mode. "
                    + "In cursor mode the result is always sorted by ascending `id` (cursor mode forces a single-component id sort pending the indexed-column audit in phase B); the path-derived `arrayDesign.id = ?` constraint and the `{probes}` id/name set restriction are both preserved; `totalElements` is `null` by default (no count query per request).",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(oneOf = {
                                    FilteredAndPaginatedResponseDataObject.class,
                                    FilteredAndCursorPaginatedResponseDataObject.class
                            }))),
            })
    public Object getPlatformElement( // Params:
            @PathParam("platform") PlatformArg<?> platformArg, // Required
            @PathParam("probes") CompositeSequenceArrayArg probesArg, // Required
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @Parameter(description = "Opaque keyset-pagination cursor token; mutually exclusive with `offset`.") @QueryParam("cursor") CursorArg cursorArg,
            @Parameter(description = "Opt-in: populate `sequence` and `sequenceLength` on each element. Useful when looking up a small probe set explicitly — for a curator inspecting a single probe row, the sequence is a one-row fetch.") @QueryParam("withSequence") @DefaultValue("false") boolean withSequence,
            @Parameter(description = "Opt-in: populate `genes` (compact `{id, officialSymbol, ncbiId}` per mapped gene) on each element. An element that maps to no gene gets `[]`.") @QueryParam("withGenes") @DefaultValue("false") boolean withGenes
    ) {
        if ( cursorArg != null ) {
            // Mutual-exclusion: a non-null cursor selects cursor mode. The default offset=0 is
            // not considered user-supplied (parallels GET /platforms/{platform}/elements step 1e).
            // In cursor mode we currently force a +id sort (PlatformArgService.getElementsByCursor)
            // — the DAO restricts cursors to single-component id sorts until the index audit lands.
            // The path-derived arrayDesign.id filter AND the {probes} id/name set restriction are
            // composed into the Filters inside getElementsByCursor (via CompositeSequenceArrayArg
            // .getPlatformFilter()) so the scope is enforced identically in both modes.
            CursorPage<CompositeSequenceValueObject> page = arrayDesignArgService.getElementsByCursor(
                    platformArg, probesArg, cursorArg.getValue(), limit.getValue(), withSequence, withGenes );
            // Filters are computed inside getElementsByCursor; re-compute here purely for the
            // echoed `filter` field on the response wrapper (matches the offset variant).
            Filters filters = arrayDesignArgService.getElementFilters( platformArg, probesArg );
            return new FilteredAndCursorPaginatedResponseDataObject<>( page, filters, new String[] { "id" } );
        }
        // Use the Filtered* paginate overload so the response surface keeps the echoed `filter`
        // field (matching the cursor-mode FilteredAndCursorPaginatedResponseDataObject).
        Filters filters = arrayDesignArgService.getElementFilters( platformArg, probesArg );
        return paginate( arrayDesignArgService.getElements( platformArg, probesArg, limit.getValue(), offset.getValue(), withSequence, withGenes ), filters, new String[] { "id" } );
    }

    /**
     * Retrieves the genes on the given platform element.
     *
     * @param platformArg can either be the ArrayDesign ID or its short name (e.g. "GPL1355" ). Retrieval by ID
     *                    is more efficient. Only platforms that user has access to will be available.
     * @param probeArg    the platform element for which the genes should be retrieved, by ID or by name —
     *                    the ID is the addressing form; a name works when it is well-formed for a path segment.
     *                    🛑 A name containing a forward slash cannot be addressed this way at all — the reverse
     *                    proxy 404s the encoded form and Tomcat 400s it, so it never reaches the application, and
     *                    lifting that is a deployment change nobody wants on a public proxy. Every other character
     *                    is the client's to percent-encode. Resolve such a name to its ID through the query string,
     *                    where a slash is legal:
     *                    {@code GET /platforms/{platform}/elements?filter=name = AFFX-HUMISGF3A/M97935_MA_at}.
     * @param offset      optional parameter (defaults to 0) skips the specified amount of datasets when retrieving them
     *                    from the database.
     * @param limit       optional parameter (defaults to 20) limits the result to specified amount of datasets. Use 0
     *                    for no limit.
     */
    @GET
    @Path("/{platform}/elements/{probe}/genes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the genes associated to a probe in a given platform",
            description = "Supports two pagination modes. Legacy mode: pass `offset` (and `limit`); response includes `offset` and `totalElements`. "
                    + "Cursor mode (recommended for deep pagination and consistency under writes — a single probe can map to many genes on multi-mapping arrays): "
                    + "pass an opaque `cursor` token from a previous response's `nextCursor` / `prevCursor` field. "
                    + "`offset` and `cursor` are mutually exclusive — passing a non-null `cursor` selects cursor mode. "
                    + "In cursor mode the result is always sorted by ascending `gene.id` (cursor mode forces a single-component id sort pending the indexed-column audit in phase B); "
                    + "the path-derived `{platform}` and `{probe}` constraints are preserved; `totalElements` is `null` by default (no count query per request).",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(oneOf = {
                                    FilteredAndPaginatedResponseDataObject.class,
                                    FilteredAndCursorPaginatedResponseDataObject.class
                            }))),
            })
    public Object getPlatformElementGenes( // Params:
            @PathParam("platform") PlatformArg<?> platformArg, // Required
            @PathParam("probe") CompositeSequenceArg<?> probeArg, // Required
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @Parameter(description = "Opaque keyset-pagination cursor token; mutually exclusive with `offset`.") @QueryParam("cursor") CursorArg cursorArg
    ) {
        if ( cursorArg != null ) {
            // Mutual-exclusion: a non-null cursor selects cursor mode. The default offset=0 is
            // not considered user-supplied (parallels GET /platforms/{platform}/elements step 1e).
            // In cursor mode we currently force a +id sort (CompositeSequenceArgService.getGenesByCursor)
            // — the DAO restricts cursors to single-component id sorts until the index audit lands.
            // The path-derived arrayDesign.id and {probe} constraints are preserved by the DAO query
            // (the keyset HQL walks the same join structure as the offset variant, scoped to the resolved
            // CompositeSequence).
            ArrayDesign platform = arrayDesignArgService.getEntity( platformArg );
            CursorPage<Gene> page = probeArgService.getGenesByCursor( probeArg, platform, cursorArg.getValue(), limit.getValue() );
            // FIXME: deal with potential null return value of loadValueObject (matches the offset variant)
            CursorPage<GeneValueObject> voPage = page.map( geneService::loadValueObject );
            return new FilteredAndCursorPaginatedResponseDataObject<>( voPage, probeArgService.getFilters( probeArg ), new String[] { "id" } );
        }
        // FIXME: deal with potential null return value of loadValueObject
        return paginate( compositeSequenceService
                .getGenes( probeArgService.getEntityWithPlatform( probeArg, arrayDesignArgService.getEntity( platformArg ) ), offset.getValue(),
                        limit.getValue(), true )
                .map( geneService::loadValueObject ), probeArgService.getFilters( probeArg ), new String[] { "id" } );
    }

    /**
     * Retrieves the per-probe gene-mapping summary (BLAT alignments + biological-sequence
     * metadata + supported genes) for a single probe on a given platform. Replaces the
     * legacy {@code CompositeSequenceController.getGeneMappingSummary} DWR call used by
     * the gemma-web gene-page Elements drill-down.
     *
     * @param platformArg can either be the ArrayDesign ID or its short name (e.g. "GPL1355" ). Retrieval by ID
     *                    is more efficient. Only platforms that user has access to will be available.
     * @param probeArg    the name or ID of the platform element for which the mapping summary should be retrieved.
     *                    the ID is the addressing form; a name works when it is well-formed for a path segment.
     *                    🛑 A name containing a forward slash cannot be addressed this way at all — the reverse
     *                    proxy 404s the encoded form and Tomcat 400s it, so it never reaches the application, and
     *                    lifting that is a deployment change nobody wants on a public proxy. Every other character
     *                    is the client's to percent-encode. Resolve such a name to its ID through the query string,
     *                    where a slash is legal:
     *                    {@code GET /platforms/{platform}/elements?filter=name = AFFX-HUMISGF3A/M97935_MA_at}.
     */
    @GET
    @Path("/{platform}/elements/{probe}/mappingSummary")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the gene-mapping summary for a probe",
            description = "Returns the probe value object with `geneMappingSummaries` populated: one entry per distinct BLAT alignment, carrying the alignment scores, the biological sequence metadata, and the genes supported by that alignment. Replaces the legacy `getGeneMappingSummary` DWR call.",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class))),
                    @ApiResponse(responseCode = "404", description = "Probe not found on the given platform",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public ResponseDataObject<CompositeSequenceValueObject> getPlatformElementMappingSummary( // Params:
            @PathParam("platform") PlatformArg<?> platformArg, // Required
            @PathParam("probe") CompositeSequenceArg<?> probeArg // Required
    ) {
        ArrayDesign platform = arrayDesignArgService.getEntity( platformArg );
        ubic.gemma.model.expression.designElement.CompositeSequence cs =
                probeArgService.getEntityWithPlatform( probeArg, platform );
        return respond( compositeSequenceService.loadValueObjectWithGeneMappingSummary( cs ) );
    }

    /**
     * Retrieves the BLAT alignments of a single probe as a UCSC Genome Browser custom track, in
     * PSL format.
     * <p>
     * Replaces the legacy gemma-web {@code BlatResultTrackController} ({@code blatTrack.html?id=}),
     * which served one alignment at a time and was meant to be fetched BY UCSC, via
     * {@code hgTracks?hgt.customText=<gemma url>}. That round trip is not available to us: the
     * deployment does not answer bots, so UCSC's fetcher cannot retrieve the URL. The 2.0 browser
     * therefore reads this text itself and submits the CONTENT to UCSC (POST {@code hgct_customText}
     * to {@code hgCustom}) rather than handing UCSC a link back to us.
     * <p>
     * The track is keyed on the probe rather than on a BLAT result id on purpose. The
     * {@code mappingSummary} response carries {@code blatResult.id} values that are not all real
     * {@code BlatResult} rows -- the {@code AnnotationAssociation} branch of
     * {@code getGeneMappingSummary} synthesizes a value object holding the BIOSEQUENCE id -- so a
     * client-supplied id cannot be trusted to address an alignment. Deriving the alignments here
     * from the probe's biological characteristic avoids that ambiguity entirely.
     *
     * @param platformArg can either be the ArrayDesign ID or its short name (e.g. "GPL1355" ). Retrieval by ID
     *                    is more efficient. Only platforms that user has access to will be available.
     * @param probeArg    the name or ID of the platform element whose alignments should be rendered.
     *                    the ID is the addressing form; a name works when it is well-formed for a path segment.
     *                    🛑 A name containing a forward slash cannot be addressed this way at all — the reverse
     *                    proxy 404s the encoded form and Tomcat 400s it, so it never reaches the application, and
     *                    lifting that is a deployment change nobody wants on a public proxy. Every other character
     *                    is the client's to percent-encode. Resolve such a name to its ID through the query string,
     *                    where a slash is legal:
     *                    {@code GET /platforms/{platform}/elements?filter=name = AFFX-HUMISGF3A/M97935_MA_at}.
     * @param download    when true, serve with an attachment disposition so a browser saves it as a
     *                    {@code .psl} file instead of rendering it inline.
     */
    @GET
    @Path("/{platform}/elements/{probe}/pslTrack")
    @Produces(TEXT_PLAIN_UTF8)
    @Operation(summary = "Retrieve the BLAT alignments of a probe as a UCSC custom track",
            description = "Returns a UCSC Genome Browser custom track in PSL format covering every BLAT alignment of the probe: a `browser position` line framing the best-scoring alignment, a `track` line, and one PSL data line per alignment. Intended to be POSTed to UCSC's `hgCustom` as `hgct_customText` by the client.",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(mediaType = TEXT_PLAIN_UTF8, schema = @Schema(type = "string"))),
                    @ApiResponse(responseCode = "404", description = "Probe not found on the given platform, or it has no BLAT alignments that can be placed in the genome browser",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public Response getPlatformElementPslTrack( // Params:
            @PathParam("platform") PlatformArg<?> platformArg, // Required
            @PathParam("probe") CompositeSequenceArg<?> probeArg, // Required
            @Parameter(hidden = true) @QueryParam("download") @DefaultValue("false") Boolean download
    ) {
        ArrayDesign platform = arrayDesignArgService.getEntity( platformArg );
        CompositeSequence cs = probeArgService.getEntityWithPlatform( probeArg, platform );

        BioSequence bioSequence = bioSequenceService.findByCompositeSequence( cs );
        if ( bioSequence == null ) {
            throw new NotFoundException( String.format( ERROR_NO_ALIGNMENTS, cs.getName(), platform.getShortName() ) );
        }

        Collection<BlatResult> alignments = blatResultService.thaw( blatResultService.findByBioSequence( bioSequence ) );
        // an alignment with no target chromosome cannot be placed in the browser; dropping those here
        // keeps one unplaceable alignment from failing the whole track.
        List<BlatResult> placeable = alignments.stream()
                .filter( br -> br.getTargetChromosome() != null && br.getTargetChromosome().getName() != null )
                .collect( Collectors.toList() );
        if ( placeable.isEmpty() ) {
            throw new NotFoundException( String.format( ERROR_NO_ALIGNMENTS, cs.getName(), platform.getShortName() ) );
        }
        if ( placeable.size() < alignments.size() ) {
            log.warn( "Dropped " + ( alignments.size() - placeable.size() ) + " of " + alignments.size()
                    + " BLAT alignments of " + cs.getName() + " from its PSL track: no target chromosome." );
        }

        String track = BlatResult2Psl.blatResults2PslTrack( placeable, hostUrl, cs.getName() );
        String fileName = cs.getName().replaceAll( Pattern.quote( "/" ), "_" ) + ".psl";
        Response.ResponseBuilder builder = Response.ok( track )
                .type( download ? MediaType.APPLICATION_OCTET_STREAM_TYPE : TEXT_PLAIN_UTF8_TYPE );
        if ( download ) {
            builder = builder.header( "Content-Disposition", "attachment; filename=\"" + fileName + "\"" );
        }
        return builder.build();
    }

    /**
     * Which of the three annotation-file flavours written by
     * {@link ArrayDesignAnnotationService#create} to serve. The constant names are the accepted
     * query values, and each carries the file-name suffix that identifies its file on disk.
     * <p>
     * Only {@link #standard} was reachable over HTTP before; the other two were generated on every
     * GO-enabled run and then never served.
     */
    public enum AnnotationFileType {
        /**
         * Every GO term, including the parents implied by the term's ancestry.
         */
        standard( ArrayDesignAnnotationService.STANDARD_FILE_SUFFIX ),
        /**
         * Biological-process terms only.
         */
        bioProcess( ArrayDesignAnnotationService.BIO_PROCESS_FILE_SUFFIX ),
        /**
         * Directly-assigned GO terms only, without the implied parents.
         */
        noParents( ArrayDesignAnnotationService.NO_PARENTS_FILE_SUFFIX );

        private final String suffix;

        AnnotationFileType( String suffix ) {
            this.suffix = suffix;
        }

        /**
         * @return the suffix inserted between the munged platform short name and
         * {@link ArrayDesignAnnotationService#ANNOTATION_FILE_SUFFIX}.
         */
        public String getSuffix() {
            return suffix;
        }
    }

    /**
     * Resolve the {@code type} query parameter, case-insensitively.
     * <p>
     * An unknown value is a 400 naming the accepted ones. Falling back to the standard file instead
     * would hand the caller a different platform's-worth of annotations than they asked for, with
     * nothing in the response to say so.
     */
    private static AnnotationFileType parseAnnotationFileType( String raw ) {
        for ( AnnotationFileType t : AnnotationFileType.values() ) {
            if ( t.name().equalsIgnoreCase( raw.trim() ) ) {
                return t;
            }
        }
        throw new BadRequestException( "Unknown annotation file type '" + raw
                + "'. Expected one of: standard, bioProcess, noParents." );
    }

    /**
     * Retrieves the annotation file for the given platform.
     *
     * @param platformArg can either be the ArrayDesign ID or its short name (e.g. "GPL1355" ). Retrieval by ID
     *                    is more efficient. Only platforms that user has access to will be available.
     * @param typeArg     which annotation-file flavour to serve, see {@link AnnotationFileType}; defaults to
     *                    {@code standard}, which is what this endpoint served unconditionally before.
     * @return the content of the annotation file of the given platform.
     */
    @GZIP(mediaTypes = TEXT_TAB_SEPARATED_VALUES_UTF8, alreadyCompressed = true)
    @GET
    @Path("/{platform}/annotations")
    @Produces("text/tab-separated-values; charset=UTF-8")
    @Operation(summary = "Retrieve the annotations of a given platform",
            description = "The following columns are available: ElementName, GeneSymbols, GOTerms, GemmaIDs, NCBIids. "
                    + "Older files might still use ProbeName instead of ElementName. "
                    + "The `type` parameter selects which of the three generated flavours to serve: `standard` "
                    + "(default) has every GO term including the parents implied by the term's ancestry, "
                    + "`bioProcess` keeps only biological-process terms, and `noParents` keeps only the directly "
                    + "assigned terms. The non-standard flavours only exist for platforms whose annotations were "
                    + "generated with GO loaded; requesting one that is absent regenerates all three.",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(type = "string"),
                                    examples = { @ExampleObject("classpath:/restapidocs/examples/platform-annotations.tsv") })),
                    @ApiResponse(responseCode = "400", description = "The annotation file type is not a recognised value.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public Response getPlatformAnnotations( // Params:
            @PathParam("platform") PlatformArg<?> platformArg,// Optional, default null
            @Parameter(description = "Which annotation file to serve.", schema = @Schema(implementation = AnnotationFileType.class))
            @QueryParam("type") @DefaultValue("standard") String typeArg,
            @Parameter(hidden = true) @QueryParam("download") @DefaultValue("false") Boolean download,
            @Parameter(hidden = true) @QueryParam("force") @DefaultValue("false") Boolean force
    ) {
        if ( force ) {
            checkIsAdmin();
        }
        AnnotationFileType type = parseAnnotationFileType( typeArg );
        ArrayDesign arrayDesign = arrayDesignArgService.getEntity( platformArg );
        String fileName = arrayDesign.getShortName().replaceAll( Pattern.quote( "/" ), "_" )
                + type.getSuffix()
                + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX;
        java.nio.file.Path file = annotationFileService.getAnnotDataDir().resolve( fileName );
        if ( force || !Files.exists( file ) ) {
            try {
                // generate it. This will cause a delay, and potentially a time-out, but better than a 404
                // To speed things up, we don't delete other files
                // One create() writes all three flavours (the bioProcess / noParents pair is only
                // written when GO is on, which it is here), so a miss on any type is served by this call.
                annotationFileService.create( arrayDesign, true, false ); // include GO by default.
            } catch ( IOException e ) {
                log.error( "Failed to generate annotation files for " + arrayDesign, e );
                throw new NotFoundException( String.format( ERROR_ANNOTATION_FILE_NOT_AVAILABLE, type, arrayDesign.getShortName() ) );
            } catch ( IllegalStateException e ) {
                // create() refuses to run unless GO is loaded, so an instance started with
                // load.geneOntology=false can never satisfy a cache miss here. Report that rather
                // than letting the raw message escape as a 500.
                log.error( "Cannot generate annotation file for " + arrayDesign + ": " + e.getMessage() );
                throw new ServiceUnavailableException(
                        String.format( ERROR_ANNOTATION_FILE_CANNOT_BE_GENERATED, arrayDesign.getShortName(), e.getMessage() ),
                        30L, e );
            }
            // create() returns quietly for platforms with no gene mappings, leaving nothing on disk
            if ( !Files.exists( file ) ) {
                throw new NotFoundException( String.format( ERROR_ANNOTATION_FILE_NOT_AVAILABLE, type, arrayDesign.getShortName() ) );
            }
        }
        return Response.ok( file )
                .type( download ? MediaType.APPLICATION_OCTET_STREAM_TYPE : TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE )
                .header( "Content-Disposition", "attachment; filename=\"" + ( download ? file.getFileName().toString() : FilenameUtils.removeExtension( file.getFileName().toString() ) ) + "\"" )
                .build();
    }

    /**
     * Retrieves the open curation tickets for a given platform.
     * <p>
     * Step 1s of {@code CURSOR_PAGINATION_STEP1_PLAN.md} adds an opt-in
     * cursor-mode branch parallel to step 1p (the
     * {@code /datasets/{dataset}/tickets} endpoint). The legacy mode is
     * preserved byte-for-byte for callers that do not supply {@code ?cursor=}.
     */
    @GET
    @Path("/{platform}/tickets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the open curation tickets for a platform",
            description = "Legacy mode (no `cursor` parameter): returns the full unpaginated open-ticket list "
                    + "in the existing shape (no count query, full result set). "
                    + "Cursor mode (recommended for platforms accumulating long curation histories): "
                    + "pass an opaque `cursor` token from a previous response's `nextCursor` / `prevCursor` "
                    + "field along with a `limit`. In cursor mode the result is always sorted by ascending `id` "
                    + "(cursor mode forces a single-component id sort pending the indexed-column audit in phase B); "
                    + "the path-derived `targetType = ARRAY_DESIGN, targetId = {platform}` constraint and "
                    + "the open-state restriction (OPEN/IN_PROGRESS) are preserved; `totalElements` is `null` by "
                    + "default (no count query per request).",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(oneOf = {
                                    ResponseDataObject.class,
                                    CursorPaginatedResponseDataObject.class
                            })))
            })
    public Object getPlatformTickets(
            @PathParam("platform") PlatformArg<?> platformArg,
            @Parameter(description = "Opaque keyset-pagination cursor token.")
            @QueryParam("cursor") CursorArg cursorArg,
            @Parameter(description = "Page size for cursor mode (ignored when no `cursor` is supplied).")
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg
    ) {
        ArrayDesign ad = arrayDesignArgService.getEntity( platformArg );
        if ( cursorArg != null ) {
            CursorPage<TicketValueObject> page = ticketsWebService.openTicketsForArrayDesignByCursor(
                    ad.getId(), cursorArg.getValue(), limitArg.getValue() );
            return paginateByCursor( page, new String[] { "id" } );
        }
        return respond( ticketsWebService.openTicketsForArrayDesign( ad.getId() ) );
    }

    /**
     * TODO (Phase 3 cleanup leftover): cannot fold into a method-level
     * {@code @PreAuthorize("hasAuthority('GROUP_ADMIN')")} on
     * {@link #getPlatformAnnotations} because the admin requirement is gated on
     * {@code force == true} — the endpoint must remain callable by non-admins when
     * {@code force} is false. Refactoring would require splitting the endpoint into admin-only
     * and public variants, which inflates the public REST surface. Leaving the conditional
     * manual check in place is the least-bad option until the {@code force} parameter is itself
     * reconsidered.
     */
    private void checkIsAdmin() {
        accessDecisionManager.decide( SecurityContextHolder.getContext().getAuthentication(), null, Collections.singletonList( new SecurityConfig( "GROUP_ADMIN" ) ) );
    }
}
