/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.core.analysis.service.ExpressionAnalysisResultSetFileService;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.core.util.math.distribution.Histogram;
import ubic.gemma.model.analysis.AnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.Baseline;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultSetValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.annotations.GZIP;
import ubic.gemma.rest.util.CursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.FilteredAndCursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.FilteredAndPaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.ResponseErrorObject;
import ubic.gemma.rest.util.SortValueObject;
import ubic.gemma.rest.util.args.*;

import org.springframework.lang.Nullable;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

import static ubic.gemma.rest.util.Responders.sendfile;

import static ubic.gemma.rest.util.MediaTypeUtils.negotiate;
import static ubic.gemma.rest.util.MediaTypeUtils.withQuality;
import static ubic.gemma.rest.util.Responders.paginate;
import static ubic.gemma.rest.util.Responders.respond;

/**
 * Endpoint for {@link ubic.gemma.model.analysis.AnalysisResultSet}
 */
@Service
@Path("/resultSets")
@Slf4j
public class AnalysisResultSetsWebService {

    public static final String TEXT_TAB_SEPARATED_VALUES_UTF8_Q9 = "text/tab-separated-values; charset=UTF-8; q=0.9";
    /**
     * Same media type without the quality parameter, for the {@link GZIP} media-type restriction — that is matched
     * against the response's {@code Content-Type}, which carries no {@code q}.
     */
    private static final String TEXT_TAB_SEPARATED_VALUES_UTF8 = "text/tab-separated-values; charset=UTF-8";
    private static final MediaType TEXT_TAB_SEPARATED_VALUES_Q9_TYPE = withQuality( new MediaType( "text", "tab-separated-values", "UTF-8" ), 0.9 );

    @Autowired
    private ExpressionAnalysisResultSetService expressionAnalysisResultSetService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionAnalysisResultSetFileService expressionAnalysisResultSetFileService;

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @Autowired
    private ExpressionAnalysisResultSetArgService expressionAnalysisResultSetArgService;

    @Autowired
    private DatasetArgService datasetArgService;
    @Autowired
    private DatabaseEntryArgService databaseEntryArgService;

    /**
     * Retrieve all {@link AnalysisResultSet} matching a set of criteria.
     *
     * @param datasets        filter result sets that belong to any of the provided dataset identifiers, or null to ignore
     * @param databaseEntries filter by associated datasets with given external identifiers, or null to ignore
     */
    @GZIP
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve all result sets matching the provided criteria",
            description = "Supports two pagination modes. Legacy mode: pass `offset` (and `limit`); response includes `offset` and `totalElements`. "
                    + "Cursor mode (recommended for deep pagination and consistency under writes): pass an opaque `cursor` token from a previous response's `nextCursor` / `prevCursor` field. "
                    + "`offset` and `cursor` are mutually exclusive — passing a non-null `cursor` selects cursor mode. "
                    + "In cursor mode the result is always sorted by ascending `id` (the user's `?sort=` is ignored); the dataset / databaseEntry / filter constraints are preserved; `totalElements` is `null` by default (no count query per request).",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(oneOf = {
                                    FilteredAndPaginatedResponseDataObject.class,
                                    FilteredAndCursorPaginatedResponseDataObject.class
                            }))),
            })
    public Object getResultSets(
            @Parameter(schema = @Schema(implementation = DatasetArrayArg.class), explode = Explode.FALSE) @QueryParam("datasets") DatasetArrayArg datasets,
            @Parameter(schema = @Schema(implementation = DatabaseEntryArrayArg.class), explode = Explode.FALSE) @QueryParam("databaseEntries") DatabaseEntryArrayArg databaseEntries,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionAnalysisResultSet> filters,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset,
            @QueryParam("limit") @DefaultValue("20") LimitArg limit,
            @QueryParam("sort") @DefaultValue("+id") SortArg<ExpressionAnalysisResultSet> sort,
            @Parameter(description = "Opaque keyset-pagination cursor token; mutually exclusive with `offset`.") @QueryParam("cursor") CursorArg cursorArg ) {
        Collection<BioAssaySet> bas = null;
        if ( datasets != null ) {
            Collection<ExpressionExperiment> ees = new ArrayList<>( datasetArgService.getEntities( datasets ) );
            bas = new ArrayList<>( ees );
            // expand with all subsets — single batched fetch keyed by source experiment, replaces the per-EE N+1
            Map<ExpressionExperiment, Collection<ExpressionExperimentSubSet>> subSetsByEE =
                    expressionExperimentService.getSubSetsWithBioAssays( ees );
            for ( Collection<ExpressionExperimentSubSet> subSets : subSetsByEE.values() ) {
                bas.addAll( subSets );
            }
        }
        Collection<DatabaseEntry> des = null;
        if ( databaseEntries != null ) {
            des = databaseEntryArgService.getEntities( databaseEntries );
        }
        Filters filters2 = expressionAnalysisResultSetArgService.getFilters( filters );
        if ( cursorArg != null ) {
            // Mutual-exclusion: a non-null cursor selects cursor mode (parallels GET /platforms step 1c
            // and step 1h). In cursor mode we currently force a +id sort (DAO enforced) — the user's
            // ?sort= arg is intentionally not consulted. The dataset / databaseEntry / filter scope
            // is preserved identically across modes.
            CursorPage<DifferentialExpressionAnalysisResultSetValueObject> page =
                    expressionAnalysisResultSetArgService.getResultSetsByCursor( bas, des, filters2, cursorArg.getValue(), limit.getValue() );
            return new FilteredAndCursorPaginatedResponseDataObject<>( page, filters2, new String[] { "id" } );
        }
        return paginate( expressionAnalysisResultSetService.findByBioAssaySetInAndDatabaseEntryInLimit(
                        bas, des, filters2, offset.getValue(), limit.getValue(), expressionAnalysisResultSetArgService.getSort( sort ) ),
                filters2, new String[] { "id" } );
    }

    @GET
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Count result sets matching the provided filter")
    public ResponseDataObject<Long> getNumberOfResultSets(
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionAnalysisResultSet> filter ) {
        return respond( expressionAnalysisResultSetService.count( expressionAnalysisResultSetArgService.getFilters( filter ) ) );
    }

    /**
     * Retrieve a {@link AnalysisResultSet} given its identifier.
     */
    // Two representations, two compression strategies — hence the repeated annotation.
    //
    // JSON is generated in-band, so Jersey's GZipEncoder compresses it on the fly (header set BEFORE the encoder
    // runs, which is what triggers it).
    //
    // TSV is served straight off a pre-gzipped cache file through sendfile(), which hands the file to Tomcat's
    // connector and never writes to the entity stream the encoder would have wrapped. Compressing on the fly is
    // therefore impossible on that path: declaring plain @GZIP there stamped Content-Encoding: gzip onto a raw
    // plain-text body, and clients that honour the header failed to inflate it (curl --compressed exited 61).
    // alreadyCompressed = true appends the header AFTER the encoder instead, so nothing tries to wrap the stream
    // and the already-gzipped bytes go out as-is.
    @GZIP(mediaTypes = MediaType.APPLICATION_JSON)
    @GZIP(mediaTypes = TEXT_TAB_SEPARATED_VALUES_UTF8, alreadyCompressed = true)
    @GET
    @Path("/{resultSet}")
    @Produces({ MediaType.APPLICATION_JSON, TEXT_TAB_SEPARATED_VALUES_UTF8_Q9 })
    @Operation(summary = "Retrieve a single analysis result set by its identifier",
            description = "A slice or results can be retrieved by specifying the `offset` and `limit` parameters. "
                    + "This is only applicable to the JSON representation. "
                    + "The TSV output exposes the following columns: id, probe_id, probe_name, gene_(id|name|ncbi_id|official_symbol|official_name), pvalue, corrected_pvalue, rank, contrast_{fvId}_(coefficient|log2fc|tstat|pvalue). "
                    + "For interaction terms, `{fvId}` is structured as `{id1}_{id2}`. "
                    + "For continuous factors, `{fvId}` is empty and a single `_` delimiter is used.",
            responses = {
                    @ApiResponse(responseCode = "200", content = {
                            @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = PaginatedResultsResponseDataObjectDifferentialExpressionAnalysisResultSetValueObject.class)),
                            @Content(mediaType = TEXT_TAB_SEPARATED_VALUES_UTF8_Q9,
                                    schema = @Schema(type = "string"),
                                    examples = { @ExampleObject("classpath:/restapidocs/examples/result-set.tsv") })
                    }),
                    @ApiResponse(responseCode = "404", description = "The analysis result set could not be found.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Object getResultSet(
            @PathParam("resultSet") ExpressionAnalysisResultSetArg analysisResultSet,
            @QueryParam("threshold") Double threshold,
            @QueryParam("offset") OffsetArg offsetArg,
            @QueryParam("limit") LimitArg limitArg,
            @Parameter(description = "Include complete factor values in contrasts instead of only populating `factorValueId` and `secondFactorValueId`. In 2.9.0, this will default to false.", schema = @Schema(defaultValue = "true")) @QueryParam("includeFactorValuesInContrasts") Boolean includeFactorValuesInContrasts,
            @Parameter(description = "Include complete taxon in genes instead of only populating `taxonId`. When this is set to true, a `taxa` collection will be included in `DifferentialExpressionAnalysisResultSetValueObject`. In 2.9.0, this will default to false.", schema = @Schema(defaultValue = "true")) @QueryParam("includeTaxonInGenes") Boolean includeTaxonInGenes,
            @Parameter(hidden = true) @QueryParam("excludeResults") @DefaultValue("false") Boolean excludeResults,
            @Context HttpHeaders headers ) {
        MediaType acceptedMediaType = negotiate( headers, MediaType.APPLICATION_JSON_TYPE, TEXT_TAB_SEPARATED_VALUES_Q9_TYPE );
        if ( acceptedMediaType.equals( MediaType.APPLICATION_JSON_TYPE ) ) {
            // TODO: those should default to false in 2.9.0, see https://github.com/PavlidisLab/Gemma/issues/1198
            if ( includeFactorValuesInContrasts == null ) {
                includeFactorValuesInContrasts = true;
            }
            if ( includeTaxonInGenes == null ) {
                includeTaxonInGenes = true;
            }
            if ( offsetArg != null || limitArg != null || threshold != null ) {
                if ( excludeResults ) {
                    throw new BadRequestException( "The excludeResults parameter cannot be used with offset/limit or threshold parameters." );
                }
                int offset = 0, limit = LimitArg.MAXIMUM;
                if ( offsetArg != null ) {
                    offset = offsetArg.getValue();
                }
                if ( limitArg != null ) {
                    limit = limitArg.getValue();
                }
                if ( threshold != null ) {
                    if ( threshold < 0.0 || threshold > 1.0 ) {
                        throw new BadRequestException( "The threshold must be between 0 and 1." );
                    }
                    return getResultSetAsJson( analysisResultSet, includeFactorValuesInContrasts, includeTaxonInGenes, threshold, offset, limit );
                } else {
                    return getResultSetAsJson( analysisResultSet, includeFactorValuesInContrasts, includeTaxonInGenes, offset, limit );
                }
            } else {
                return getResultSetAsJson( analysisResultSet, includeFactorValuesInContrasts, includeTaxonInGenes, excludeResults );
            }
        } else {
            if ( offsetArg != null || limitArg != null ) {
                throw new BadRequestException( "The offset/limit parameters cannot be used with the TSV representation." );
            }
            if ( includeFactorValuesInContrasts != null ) {
                throw new BadRequestException( "The includeFactorValuesInContrasts parameter cannot be used with the TSV representation." );
            }
            if ( includeTaxonInGenes != null ) {
                throw new BadRequestException( "The includeTaxonInGenes parameter cannot be used with the TSV representation." );
            }
            if ( excludeResults ) {
                throw new BadRequestException( "The excludeResults parameter cannot be used with the TSV representation." );
            }
            if ( threshold != null ) {
                throw new BadRequestException( "The threshold parameter cannot be used with the TSV representation." );
            }
            return getResultSetAsTsvCached( analysisResultSet );
        }
    }

    /**
     * Histogram-binned p-values for a differential-expression result set.
     * <p>
     * Served straight out of the stored {@code PVALUE_DISTRIBUTION} row hanging off the result set
     * ({@code ANALYSIS_RESULT_SET.PVALUE_DISTRIBUTION_FK}); nothing is aggregated per request.
     * {@code DifferentialExpressionAnalyzerServiceImpl#addPvalueDistribution} writes it when the
     * analysis is run: 100 fixed-width bins over {@code [0, 1]} of {@code result.getPvalue()}, i.e.
     * the RAW p-values. Measured on production 2026-08-31: {@code PVALUE_DISTRIBUTION} is 0.1 GB and
     * every one of the 56,616 result sets has a row, whereas the
     * {@code DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT} table the previous implementation grouped over
     * is 120.5 GB / ~1,574,396,269 rows.
     * <p>
     * Because the stored bins are fixed, {@code bins} can only merge whole stored bins — it has to
     * divide the stored bin count exactly. Splitting a stored count across two output bins would be
     * inventing data, so a non-divisor is a 400 rather than an approximation.
     */
    @GET
    @Path("/{resultSet}/pvalueDistribution")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Binned p-value distribution for a differential-expression result set",
            description = "Returns the p-value histogram stored with the result set, optionally down-binned by "
                    + "merging adjacent stored bins. The histogram is written when the differential-expression "
                    + "analysis is run — 100 fixed-width bins over [0, 1] of the **raw** p-values — and is served "
                    + "verbatim; nothing is computed per request. "
                    + "`bins` must therefore divide the stored bin count (100) exactly: 1, 2, 4, 5, 10, 20, 25, 50 "
                    + "or 100. Any other value is rejected with a 400, because splitting a stored count across two "
                    + "output bins would be inventing data. "
                    + "Bin i covers (i/bins, (i+1)/bins]; the first bin also includes 0.0. Note this is the "
                    + "closed-on-the-right convention of the stored histogram, not the half-open one. "
                    + "`column` accepts only `raw`: no corrected-p-value histogram is stored anywhere, and building "
                    + "one would mean scanning the full results table, so `column=corrected` is rejected with a 400 "
                    + "instead of silently answering with the raw distribution.",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PvalueDistributionResponseDataObject.class))),
                    @ApiResponse(responseCode = "204", description = "The stored histogram is empty — it has no bins, or every bin is zero (the analysis produced no non-null p-values)."),
                    @ApiResponse(responseCode = "400", description = "`bins` does not divide the stored bin count, or `column` is not 'raw'.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The analysis result set could not be found, or it has no stored p-value distribution.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response getPvalueDistribution(
            @PathParam("resultSet") ExpressionAnalysisResultSetArg analysisResultSet,
            @Parameter(description = "Number of bins. Must divide the stored bin count (100) exactly: 1, 2, 4, 5, 10, 20, 25, 50 or 100.",
                    schema = @Schema(defaultValue = "20", minimum = "1", maximum = "100"))
            @QueryParam("bins") @DefaultValue("20") int bins,
            @Parameter(description = "Which p-value column to serve. Only the raw distribution is stored; 'corrected' is rejected with a 400.",
                    schema = @Schema(defaultValue = "raw", allowableValues = { "raw" }))
            @QueryParam("column") @DefaultValue("raw") String column ) {
        if ( "corrected".equals( column ) ) {
            throw new BadRequestException( "Only the raw p-value distribution is stored; there is no corrected one to serve, "
                    + "and computing it would mean scanning the whole results table. Pass column=raw or omit the parameter." );
        }
        if ( !"raw".equals( column ) ) {
            throw new BadRequestException( "The 'column' parameter must be 'raw' (got '" + column + "')." );
        }
        if ( bins < 1 ) {
            throw new BadRequestException( "The 'bins' parameter must be at least 1 (got " + bins + ")." );
        }
        // resolves through the AbstractEntityArgService.getEntity path which 404s if missing and
        // enforces ACL on the loaded result set.
        ExpressionAnalysisResultSet ears = expressionAnalysisResultSetArgService.getEntity( analysisResultSet );
        Histogram stored = expressionAnalysisResultSetService.loadPvalueDistribution( ears );
        if ( stored == null ) {
            // Distinct from 204: there is no PVALUE_DISTRIBUTION row at all. Zero result sets are in
            // this state on production, so a 404 here is a data-integrity signal, not a normal answer.
            throw new NotFoundException( "ExpressionAnalysisResultSet " + ears.getId() + " has no stored p-value distribution." );
        }
        double[] storedCounts = stored.getArray();
        if ( storedCounts.length == 0 ) {
            return Response.noContent().build();
        }
        if ( storedCounts.length % bins != 0 ) {
            throw new BadRequestException( "The 'bins' parameter must divide the stored bin count (" + storedCounts.length
                    + ") exactly, so that output bins are whole numbers of stored bins; accepted values are "
                    + divisorsOf( storedCounts.length ) + " (got " + bins + ")." );
        }
        int storedBinsPerOutputBin = storedCounts.length / bins;
        long[] counts = new long[bins];
        long total = 0;
        for ( int i = 0; i < storedCounts.length; i++ ) {
            // bin counts are whole numbers (Histogram increments by 1); round rather than truncate
            // so a stored value that drifted to 4.999999 is not silently read as 4.
            long c = Math.round( storedCounts[i] );
            counts[i / storedBinsPerOutputBin] += c;
            total += c;
        }
        if ( total == 0 ) {
            return Response.noContent().build();
        }
        return Response.ok( respond( new PvalueDistributionValueObject( ears.getId(), column, counts ) ) ).build();
    }

    /**
     * Ascending divisors of {@code n}, for the {@code bins} rejection message.
     */
    private static String divisorsOf( int n ) {
        StringBuilder sb = new StringBuilder();
        for ( int d = 1; d <= n; d++ ) {
            if ( n % d == 0 ) {
                if ( sb.length() > 0 ) {
                    sb.append( ", " );
                }
                sb.append( d );
            }
        }
        return sb.toString();
    }

    /**
     * Disk-cache + sendfile path for the TSV representation of a result set.
     * <p>
     * Result sets are immutable post-creation; the cached gzipped TSV under
     * {@code <dataDir>/resultSets/resultSet_<id>.tsv.gz} stays valid forever. Its bytes are what goes on the wire —
     * see the {@code @GZIP} declarations on {@link #getResultSet} for why it is cached pre-compressed. The
     * cold-build path materializes the
     * full result set (50k results + thawed probe + contrasts + factor values + result-to-genes map = hundreds of
     * batched DB round-trips) once on first request; subsequent requests skip the DB entirely.
     */
    private Response getResultSetAsTsvCached( ExpressionAnalysisResultSetArg analysisResultSet ) {
        Long id = analysisResultSet.getValue();
        try ( LockedPath p = expressionDataFileService.writeOrLocateDifferentialExpressionResultSetTsvFile( id, false ) ) {
            return sendfile( p.getPath() )
                    .type( new MediaType( "text", "tab-separated-values", "UTF-8" ) )
                    .header( "Content-Disposition", "attachment; filename=\"resultSet_" + id + ".tsv\"" )
                    .build();
        } catch ( java.util.NoSuchElementException e ) {
            throw new NotFoundException( "Could not find ExpressionAnalysisResultSet for " + analysisResultSet + "." );
        } catch ( RejectedExecutionException e ) {
            log.warn( "expressionDataFileTaskExecutor queue is full; streaming result-set " + id + " in-band.", e );
            return Response.ok( getResultSetAsTsv( analysisResultSet ) )
                    .type( new MediaType( "text", "tab-separated-values", "UTF-8" ) )
                    .build();
        } catch ( IOException e ) {
            log.error( "Failed to materialize cached result-set TSV for " + id + ", falling back to stream.", e );
            return Response.ok( getResultSetAsTsv( analysisResultSet ) )
                    .type( new MediaType( "text", "tab-separated-values", "UTF-8" ) )
                    .build();
        }
    }

    private ResponseDataObject<DifferentialExpressionAnalysisResultSetValueObject> getResultSetAsJson( ExpressionAnalysisResultSetArg analysisResultSet, boolean includeFactorValuesInContrasts, boolean includeTaxonInGenes, boolean excludeResults ) {
        if ( excludeResults ) {
            ExpressionAnalysisResultSet ears = expressionAnalysisResultSetArgService.getEntity( analysisResultSet );
            return respond( expressionAnalysisResultSetService.loadValueObject( ears ) );
        } else {
            ExpressionAnalysisResultSet ears = expressionAnalysisResultSetArgService.getEntityWithContrastsAndResults( analysisResultSet );
            if ( ears == null ) {
                throw new NotFoundException( "Could not find ExpressionAnalysisResultSet for " + analysisResultSet + "." );
            }
            return respond( expressionAnalysisResultSetService.loadValueObjectWithResults( ears, includeFactorValuesInContrasts, false, includeTaxonInGenes ) );
        }
    }

    private PaginatedResultsResponseDataObjectDifferentialExpressionAnalysisResultSetValueObject getResultSetAsJson( ExpressionAnalysisResultSetArg analysisResultSet, boolean includeFactorValuesInContrasts, boolean includeTaxonInGenes, int offset, int limit ) {
        ExpressionAnalysisResultSet ears = expressionAnalysisResultSetArgService.getEntityWithContrastsAndResults( analysisResultSet, offset, limit );
        if ( ears == null ) {
            throw new NotFoundException( "Could not find ExpressionAnalysisResultSet for " + analysisResultSet + "." );
        }
        long totalElements = expressionAnalysisResultSetService.countResults( ears );
        return paginateResults( expressionAnalysisResultSetService.loadValueObjectWithResults( ears, includeFactorValuesInContrasts, true, includeTaxonInGenes ), null, offset, limit, totalElements );
    }

    private PaginatedResultsResponseDataObjectDifferentialExpressionAnalysisResultSetValueObject getResultSetAsJson( ExpressionAnalysisResultSetArg analysisResultSet, boolean includeFactorValuesInContrasts, boolean includeTaxonInGenes, double threshold, int offset, int limit ) {
        ExpressionAnalysisResultSet ears = expressionAnalysisResultSetArgService.getEntityWithContrastsAndResults( analysisResultSet, threshold, offset, limit );
        if ( ears == null ) {
            throw new NotFoundException( "Could not find ExpressionAnalysisResultSet for " + analysisResultSet + "." );
        }
        long totalElements = expressionAnalysisResultSetService.countResults( ears, threshold );
        return paginateResults( expressionAnalysisResultSetService.loadValueObjectWithResults( ears, includeFactorValuesInContrasts, true, includeTaxonInGenes ), threshold, offset, limit, totalElements );
    }

    private StreamingOutput getResultSetAsTsv( ExpressionAnalysisResultSetArg analysisResultSet ) {
        final ExpressionAnalysisResultSet ears = expressionAnalysisResultSetArgService.getEntityWithContrastsAndResults( analysisResultSet );
        if ( ears == null ) {
            throw new NotFoundException( "Could not find ExpressionAnalysisResultSet for " + analysisResultSet + "." );
        }
        final Map<Long, Set<Gene>> resultId2Genes = expressionAnalysisResultSetService.loadResultIdToGenesMap( ears );
        Baseline baseline = expressionAnalysisResultSetService.getBaseline( ears );
        return outputStream -> {
            try ( Writer writer = new OutputStreamWriter( outputStream, StandardCharsets.UTF_8 ) ) {
                expressionAnalysisResultSetFileService.writeTsv( ears, baseline, resultId2Genes, writer );
            }
        };
    }

    private PaginatedResultsResponseDataObjectDifferentialExpressionAnalysisResultSetValueObject paginateResults( DifferentialExpressionAnalysisResultSetValueObject resultSet, @Nullable Double threshold, int offset, int limit, long totalElements ) {
        return new PaginatedResultsResponseDataObjectDifferentialExpressionAnalysisResultSetValueObject( resultSet, threshold, offset, limit, totalElements );
    }

    /**
     * Concrete {@link ResponseDataObject} type for the pvalueDistribution endpoint so Swagger has a
     * non-generic schema to reference.
     */
    public static class PvalueDistributionResponseDataObject extends ResponseDataObject<PvalueDistributionValueObject> {
        public PvalueDistributionResponseDataObject( PvalueDistributionValueObject payload ) {
            super( payload );
        }
    }

    /**
     * Similar to {@link ubic.gemma.rest.util.PaginatedResponseDataObject}, but the {@code data.results} is paginated
     * instead of {@code data}
     */
    @Getter
    public static class PaginatedResultsResponseDataObjectDifferentialExpressionAnalysisResultSetValueObject extends ResponseDataObject<DifferentialExpressionAnalysisResultSetValueObject> {

        private final String filter;
        private final SortValueObject sort;
        private final String[] groupBy;
        private final Integer offset;
        private final Integer limit;
        private final Long totalElements;

        public PaginatedResultsResponseDataObjectDifferentialExpressionAnalysisResultSetValueObject( DifferentialExpressionAnalysisResultSetValueObject resultSet, @Nullable Double threshold, int offset, int limit, long totalElements ) {
            super( resultSet );
            this.filter = threshold != null ? "results.correctedPvalue <= " + threshold : "";
            this.sort = new SortValueObject( Sort.by( null, "correctedPvalue", Sort.Direction.ASC, Sort.NullMode.LAST, "results.correctedPvalue" ) );
            this.groupBy = new String[] { "results.id" };
            this.offset = offset;
            this.limit = limit;
            this.totalElements = totalElements;
        }
    }
}
