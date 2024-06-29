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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.core.analysis.service.ExpressionAnalysisResultSetFileService;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.analysis.AnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.Baseline;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultSetValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.annotations.GZIP;
import ubic.gemma.rest.util.*;
import ubic.gemma.rest.util.args.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static ubic.gemma.rest.util.MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8;
import static ubic.gemma.rest.util.MediaTypeUtils.negotiate;
import static ubic.gemma.rest.util.Responders.paginate;
import static ubic.gemma.rest.util.Responders.respond;

/**
 * Endpoint for {@link ubic.gemma.model.analysis.AnalysisResultSet}
 */
@Service
@Path("/resultSets")
public class AnalysisResultSetsWebService {

    private static final String TEXT_TAB_SEPARATED_VALUES_Q9 = TEXT_TAB_SEPARATED_VALUES_UTF8 + "; q=0.9";

    private static final MediaType TEXT_TAB_SEPARATED_VALUES_Q9_TYPE = MediaTypeUtils.withQuality( MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE, 0.9 );

    @Autowired
    private ExpressionAnalysisResultSetService expressionAnalysisResultSetService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionAnalysisResultSetFileService expressionAnalysisResultSetFileService;

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
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve all result sets matching the provided criteria")
    public FilteredAndPaginatedResponseDataObject<DifferentialExpressionAnalysisResultSetValueObject> getResultSets(
            @Parameter(schema = @Schema(implementation = DatasetArrayArg.class), explode = Explode.FALSE) @QueryParam("datasets") DatasetArrayArg datasets,
            @Parameter(schema = @Schema(implementation = DatabaseEntryArrayArg.class), explode = Explode.FALSE) @QueryParam("databaseEntries") DatabaseEntryArrayArg databaseEntries,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionAnalysisResultSet> filters,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset,
            @QueryParam("limit") @DefaultValue("20") LimitArg limit,
            @QueryParam("sort") @DefaultValue("+id") SortArg<ExpressionAnalysisResultSet> sort ) {
        Collection<BioAssaySet> bas = null;
        if ( datasets != null ) {
            Collection<ExpressionExperiment> ees = new ArrayList<>( datasetArgService.getEntities( datasets ) );
            bas = new ArrayList<>( ees );
            // expand with all subsets
            for ( ExpressionExperiment ee : ees ) {
                bas.addAll( expressionExperimentService.getSubSets( ee ) );
            }
        }
        Collection<DatabaseEntry> des = null;
        if ( databaseEntries != null ) {
            des = databaseEntryArgService.getEntities( databaseEntries );
        }
        Filters filters2 = expressionAnalysisResultSetArgService.getFilters( filters );
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
    @GZIP
    @GET
    @Path("/{resultSet}")
    @Produces({ MediaType.APPLICATION_JSON, TEXT_TAB_SEPARATED_VALUES_Q9 })
    @Operation(summary = "Retrieve a single analysis result set by its identifier",
            description = "A slice or results can be retrieved by specifying the `offset` and `limit` parameters. "
                    + "This is only applicable to the JSON representation. "
                    + "The TSV output exposes the following columns: id, probe_id, probe_name, gene_(id|name|ncbi_id|official_symbol|official_name), pvalue, corrected_pvalue, rank, contrast_{fvId}_(coefficient|log2fc|tstat|pvalue). "
                    + "For interaction terms, `{fvId}` is structured as `{id1}_{id2}`. "
                    + "For continuous factors, `{fvId}` is empty and a single `_` delimiter is used.",
            responses = {
                    @ApiResponse(content = {
                            @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = PaginatedResultsResponseDataObjectDifferentialExpressionAnalysisResultSetValueObject.class)),
                            @Content(mediaType = TEXT_TAB_SEPARATED_VALUES_UTF8, /* no need to expose the q-value */
                                    schema = @Schema(type = "string", format = "binary"),
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
            return getResultSetAsTsv( analysisResultSet );
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
        final Map<Long, List<Gene>> resultId2Genes = expressionAnalysisResultSetService.loadResultIdToGenesMap( ears );
        Baseline baseline = expressionAnalysisResultSetService.getBaseline( ears );
        return outputStream -> {
            try ( OutputStreamWriter writer = new OutputStreamWriter( outputStream ) ) {
                expressionAnalysisResultSetFileService.writeTsv( ears, baseline, resultId2Genes, writer );
            }
        };
    }

    private PaginatedResultsResponseDataObjectDifferentialExpressionAnalysisResultSetValueObject paginateResults( DifferentialExpressionAnalysisResultSetValueObject resultSet, @Nullable Double threshold, int offset, int limit, long totalElements ) {
        return new PaginatedResultsResponseDataObjectDifferentialExpressionAnalysisResultSetValueObject( resultSet, threshold, offset, limit, totalElements );
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
            this.sort = new SortValueObject( Sort.by( null, "correctedPvalue", Sort.Direction.ASC, "results.correctedPvalue" ) );
            this.groupBy = new String[] { "results.id" };
            this.offset = offset;
            this.limit = limit;
            this.totalElements = totalElements;
        }
    }
}
