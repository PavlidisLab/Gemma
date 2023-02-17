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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.analysis.preprocess.filter.NoRowsLeftAfterFilteringException;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.analysis.preprocess.svd.SVDValueObject;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationFetchingEvent;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.ExperimentExpressionLevelsValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.annotations.GZIP;
import ubic.gemma.rest.util.*;
import ubic.gemma.rest.util.args.*;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RESTful interface for datasets.
 *
 * @author tesarst
 */
@Service
@Path("/datasets")
@CommonsLog
public class DatasetsWebService {

    private static final String ERROR_DATA_FILE_NOT_AVAILABLE = "Data file for experiment %s can not be created.";
    private static final String ERROR_DESIGN_FILE_NOT_AVAILABLE = "Design file for experiment %s can not be created.";

    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private ExpressionDataFileService expressionDataFileService;
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private BioAssayService bioAssayService;
    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;
    @Autowired
    private SVDService svdService;
    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    @Autowired
    private AuditEventService auditEventService;
    @Autowired
    private OutlierDetectionService outlierDetectionService;
    @Autowired
    private QuantitationTypeService quantitationTypeService;
    @Autowired
    private DatasetArgService datasetArgService;
    @Autowired
    private GeneArgService geneArgService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve all datasets")
    public FilteringAndPaginatedResponseDataObject<ExpressionExperimentValueObject> getDatasets( // Params:
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg<ExpressionExperiment> sort // Optional, default +id
    ) {
        Filters filters = datasetArgService.getFilters( filter );
        return Responder.paginate( expressionExperimentService::loadValueObjects, filters, new String[] { "id" },
                datasetArgService.getSort( sort ), offset.getValue(), limit.getValue() );
    }

    @GET
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Count datasets matching the provided filters")
    public ResponseDataObject<Long> getNumberOfDatasets(
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter ) {
        return Responder.respond( expressionExperimentService.count( datasetArgService.getFilters( filter ) ) );
    }

    public interface UsageStatistics {
        Long getNumberOfExpressionExperiments();
    }

    @GET
    @Path("/platforms")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve usage statistics of platforms among datasets matching the provided filter",
            description = "Usage statistics are aggregated across experiment tags, samples and factor values mentioned in the experimental design.")
    public LimitedResponseDataObject<ArrayDesignWithUsageStatisticsValueObject> getDatasetsPlatformsUsageStatistics( @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter, @QueryParam("limit") @DefaultValue("50") LimitArg limit ) {
        Filters filters = datasetArgService.getFilters( filter );
        Integer l = limit.getValue( 50 );
        List<ArrayDesignWithUsageStatisticsValueObject> results = expressionExperimentService.getArrayDesignUsedOrOriginalPlatformUsageFrequency( filters, true, limit.getValue( 50 ) )
                .entrySet()
                .stream().map( e -> new ArrayDesignWithUsageStatisticsValueObject( e.getKey(), e.getValue() ) )
                .sorted( Comparator.comparing( UsageStatistics::getNumberOfExpressionExperiments, Comparator.reverseOrder() ) )
                .collect( Collectors.toList() );
        return Responder.limit( results, filters, new String[] { "id" }, Sort.by( null, "numberOfExpressionExperiments", Sort.Direction.DESC, "numberOfExpressionExperiments" ), l );
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    @JsonIgnoreProperties({ "expressionExperimentCount", "numberOfSwitchedExpressionExperiments" })
    public static class ArrayDesignWithUsageStatisticsValueObject extends ArrayDesignValueObject implements UsageStatistics {

        public ArrayDesignWithUsageStatisticsValueObject( ArrayDesign arrayDesign, Long numberOfExpressionExperiments ) {
            super( arrayDesign );
            setExpressionExperimentCount( numberOfExpressionExperiments );
        }
    }

    @GET
    @GZIP
    @Path("/annotations")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve usage statistics of annotations among datasets matching the provided filter",
            description = "Usage statistics are aggregated across experiment tags, samples and factor values mentioned in the experimental design.")
    public LimitedResponseDataObject<AnnotationWithUsageStatisticsValueObject> getDatasetsAnnotationsUsageStatistics(
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter, @QueryParam("limit") LimitArg limit ) {
        Filters filters = datasetArgService.getFilters( filter );
        // at 200, the least frequent term covers about 50 datasets
        int l = limit != null ? limit.getValueNoMaximum() : -1;
        List<AnnotationWithUsageStatisticsValueObject> results = expressionExperimentService.getAnnotationsUsageFrequency( filters, l )
                .stream().map( e -> new AnnotationWithUsageStatisticsValueObject( e.getCharacteristic(), e.getNumberOfExpressionExperiments(), getParentTerms( e.getTerm() ) ) )
                .sorted( Comparator.comparing( UsageStatistics::getNumberOfExpressionExperiments, Comparator.reverseOrder() ) )
                .collect( Collectors.toList() );
        return Responder.limit( results, filters, new String[] { "classUri", "className", "termUri", "termName" },
                Sort.by( null, "numberOfExpressionExperiments", Sort.Direction.DESC, "numberOfExpressionExperiments" ), l );
    }

    @Nullable
    private static Set<OntologyTermValueObject> getParentTerms( @Nullable OntologyTerm c ) {
        if ( c != null ) {
            return c.getParents( true ).stream()
                    .map( t -> toTermVo( t, new HashSet<>() ) )
                    .collect( Collectors.toSet() );
        } else {
            return null;
        }
    }

    /**
     * The
     * @param ontologyTerm
     * @param visited
     * @return
     */
    private static OntologyTermValueObject toTermVo( OntologyTerm ontologyTerm, Set<OntologyTerm> visited ) {
        Set<OntologyTermValueObject> parentVos;
        if ( visited.contains( ontologyTerm ) ) {
            // TODO: maybe add a note?
            parentVos = null;
        } else {
            visited.add( ontologyTerm );
            parentVos = ontologyTerm.getParents( true ).stream()
                    .map( t -> toTermVo( t, new HashSet<>( visited ) ) )
                    .collect( Collectors.toSet() );
        }
        return new OntologyTermValueObject( ontologyTerm, parentVos );
    }

    @Value
    @EqualsAndHashCode(of = { "uri" })
    public static class OntologyTermValueObject {

        String uri;
        String name;
        /**
         * Empty i
         */
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Set<OntologyTermValueObject> parentTerms;

        public OntologyTermValueObject( OntologyTerm ontologyTerm, Set<OntologyTermValueObject> parentTerms ) {
            this.uri = ontologyTerm.getUri();
            this.name = ontologyTerm.getTerm();
            this.parentTerms = parentTerms;
        }
    }

    /**
     * This is an aggregated entity across value URI and value, thus the {@code id} and {@code objectClass} are omitted.
     */
    @Value
    @EqualsAndHashCode(callSuper = true)
    @JsonIgnoreProperties(value = { "id", "objectClass" })
    public static class AnnotationWithUsageStatisticsValueObject extends AnnotationValueObject implements UsageStatistics {

        /**
         * Number of times the characteristic is mentioned among matching datasets.
         */
        Long numberOfExpressionExperiments;

        /**
         * URIs of parent terms.
         */
        @Nullable
        Set<OntologyTermValueObject> parentTerms;

        public AnnotationWithUsageStatisticsValueObject( Characteristic c, Long numberOfExpressionExperiments, @Nullable Set<OntologyTermValueObject> parentTerms ) {
            super( c );
            this.numberOfExpressionExperiments = numberOfExpressionExperiments;
            this.parentTerms = parentTerms;
        }
    }

    /**
     * Retrieves all datasets matching the given identifiers.
     *
     * @param datasetsArg a list of identifiers, separated by commas (','). Identifiers can either be the
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
    @Path("/{dataset}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve datasets by their identifiers")
    public FilteringAndPaginatedResponseDataObject<ExpressionExperimentValueObject> getDatasetsByIds( // Params:
            @PathParam("dataset") DatasetArrayArg datasetsArg, // Optional
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg<ExpressionExperiment> sort // Optional, default +id
    ) {
        Filters filters = datasetArgService.getFilters( filter ).and( datasetArgService.getFilters( datasetsArg ) );
        return Responder.paginate( expressionExperimentService::loadValueObjects, filters, new String[] { "id" },
                datasetArgService.getSort( sort ), offset.getValue(), limit.getValue() );
    }

    /**
     * Browse blacklisted datasets.
     */
    @GET
    @Path("/blacklisted")
    @Produces(MediaType.APPLICATION_JSON)
    public FilteringAndPaginatedResponseDataObject<ExpressionExperimentValueObject> getBlacklistedDatasets(
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filterArg,
            @QueryParam("sort") @DefaultValue("+id") SortArg<ExpressionExperiment> sortArg,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset,
            @QueryParam("limit") @DefaultValue("20") LimitArg limit ) {
        return Responder.paginate( expressionExperimentService::loadBlacklistedValueObjects,
                datasetArgService.getFilters( filterArg ), new String[] { "id" }, datasetArgService.getSort( sortArg ),
                offset.getValue(), limit.getValue() );
    }

    /**
     * Retrieves platforms for the given dataset.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{dataset}/platforms")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the platforms of a dataset", responses = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(ref = "ResponseDataObjectListArrayDesignValueObject"))),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<List<ArrayDesignValueObject>> getDatasetPlatforms( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg // Required
    ) {
        return Responder.respond( datasetArg.getPlatforms( expressionExperimentService, arrayDesignService ) );
    }

    /**
     * Retrieves the samples for the given dataset.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{dataset}/samples")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the samples of a dataset", responses = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(ref = "ResponseDataObjectListBioAssayValueObject"))),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<List<BioAssayValueObject>> getDatasetSamples( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg // Required
    ) {
        return Responder.respond( datasetArg.getSamples( expressionExperimentService, bioAssayService, outlierDetectionService ) );
    }

    /**
     * Retrieves the differential analysis results for the given dataset.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{dataset}/analyses/differential")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve annotations and surface level stats for a dataset's differential analyses", responses = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(ref = "ResponseDataObjectListDifferentialExpressionAnalysisValueObject"))),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<List<DifferentialExpressionAnalysisValueObject>> getDatasetDifferentialExpressionAnalyses( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg, // Required
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit // Optional, default 20
    ) {
        return Responder.respond(
                this.getDiffExVos( datasetArgService.getEntity( datasetArg ).getId(),
                        offset.getValue(), limit.getValue() )
        );
    }

    /**
     * Retrieves the result sets of all the differential expression analyses of a dataset.
     * <p>
     * This is actually performing a 302 Found redirection to point the HTTP client to the corresponding result sets
     * endpoint.
     *
     * @see AnalysisResultSetsWebService#getResultSets(DatasetArrayArg, DatabaseEntryArrayArg, FilterArg, OffsetArg, LimitArg, SortArg)
     */
    @GET
    @Path("/{dataset}/analyses/differential/resultSets")
    @Operation(summary = "Retrieve the result sets of all differential analyses of a dataset", responses = {
            @ApiResponse(responseCode = "302", description = "If the dataset is found, a redirection to the corresponding getResultSets operation."),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.", content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response getDatasetDifferentialExpressionAnalysesResultSets(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Context HttpServletRequest request ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        UriComponentsBuilder uriComponents;
        // this is only for testing because Jersey in-memory container lacks a servlet context
        if ( request != null ) {
            uriComponents = ServletUriComponentsBuilder.fromServletMapping( request )
                    .scheme( null ).host( null );
        } else {
            uriComponents = UriComponentsBuilder.newInstance();
        }
        URI resultSetUri = uriComponents
                .path( "/resultSets" )
                .queryParam( "datasets", "{datasetId}" )
                .buildAndExpand( ee.getId() ).toUri();
        return Response.status( Response.Status.FOUND )
                .location( resultSetUri )
                .build();
    }

    /**
     * Retrieves the annotations for the given dataset.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{dataset}/annotations")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the annotations of a dataset", responses = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(ref = "ResponseDataObjectSetAnnotationValueObject"))),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<Set<AnnotationValueObject>> getDatasetAnnotations( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg // Required
    ) {
        return Responder.respond( datasetArg.getAnnotations( expressionExperimentService ) );
    }

    /**
     * Retrieve all available quantitation types for a dataset.
     */
    @GET
    @Path("/{dataset}/quantitationTypes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve quantitation types of a dataset")
    public ResponseDataObject<Set<QuantitationTypeValueObject>> getDatasetQuantitationTypes( @PathParam("dataset") DatasetArg<?> datasetArg ) {
        return Responder.respond( datasetArg.getQuantitationTypes( expressionExperimentService ) );
    }

    /**
     * Retrieves the data for the given dataset.
     * <p>
     * The returned TSV format contains the following columns:
     *
     * <ul>
     *     <li>Probe</li>
     *     <li>Sequence</li>
     *     <li>GeneSymbol (optional)</li>
     *     <li>GeneName (optional)</li>
     *     <li>GemmaId (optional)</li>
     *     <li>NCBIid (optional)</li>
     * </ul>
     *
     * followed by one column per sample.
     * <p>
     * <b>Note:</b> Additional gene information is only available if the corresponding platform's annotations has been dumped
     * on-disk.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     * @param filterData return filtered the expression data.
     */
    @GET
    @Path("/{dataset}/data")
    @Produces(MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8)
    @Operation(summary = "Retrieve processed expression data of a dataset",
            description = "This endpoint is deprecated and getDatasetProcessedExpression() should be used instead.",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8,
                            schema = @Schema(type = "string", format = "binary"))),
                    @ApiResponse(responseCode = "204", description = "The dataset expression matrix is empty."),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) }, deprecated = true)
    public Response getDatasetExpression( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg, // Required
            @QueryParam("filter") @DefaultValue("false") BoolArg filterData // Optional, default false
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        try {
            return this.outputDataFile( ee, filterData.getValue() );
        } catch ( NoRowsLeftAfterFilteringException e ) {
            return Response.noContent().build();
        } catch ( FilteringException e ) {
            throw new InternalServerErrorException( String.format( "Filtering of dataset %s failed.", ee.getShortName() ), e );
        }
    }

    /**
     * Retrieve processed expression data.
     * <p>
     * The payload is transparently compressed via a <code>Content-Encoding</code> header and streamed to avoid dumping
     * the whole payload in memory.
     */
    @GZIP
    @GET
    @Path("/{dataset}/data/processed")
    @Produces(MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8)
    @Operation(summary = "Retrieve processed expression data of a dataset", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8,
                    schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "404", description = "Either the dataset or the quantitation type do not exist.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response getDatasetProcessedExpression( @PathParam("dataset") DatasetArg<?> datasetArg,
            @QueryParam("quantitationType") QuantitationTypeArg<?> quantitationTypeArg ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        QuantitationType qt;
        if ( quantitationTypeArg != null ) {
            qt = quantitationTypeArg.getEntityForExpressionExperimentAndDataVectorType( ee, ProcessedExpressionDataVector.class, quantitationTypeService );
        } else {
            qt = expressionExperimentService.getPreferredQuantitationTypeForDataVectorType( ee, ProcessedExpressionDataVector.class );
            if ( qt == null ) {
                throw new NotFoundException( String.format( "No preferred quantitation type could be for found processed expression data data of %s.", ee ) );
            }
        }
        StreamingOutput stream = ( output ) -> expressionDataFileService.writeProcessedExpressionData( ee, qt, new OutputStreamWriter( output ) );
        return Response.ok( stream )
                .header( "Content-Disposition", String.format( "attachment; filename=%d_%s_expmat.unfilt.data.txt", ee.getId(), ee.getShortName() ) )
                .build();
    }

    /**
     * Retrieve raw expression data.
     * <p>
     * The payload is transparently compressed via a <code>Content-Encoding</code> header and streamed to avoid dumping
     * the whole payload in memory.
     */
    @GZIP
    @GET
    @Path("/{dataset}/data/raw")
    @Produces(MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8)
    @Operation(summary = "Retrieve raw expression data of a dataset", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8,
                    schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "404", description = "Either the dataset or the quantitation type do not exist.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response getDatasetRawExpression( @PathParam("dataset") DatasetArg<?> datasetArg,
            @QueryParam("quantitationType") QuantitationTypeArg<?> quantitationTypeArg ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        QuantitationType qt;
        if ( quantitationTypeArg != null ) {
            qt = quantitationTypeArg.getEntityForExpressionExperimentAndDataVectorType( ee, RawExpressionDataVector.class, quantitationTypeService );
        } else {
            qt = expressionExperimentService.getPreferredQuantitationTypeForDataVectorType( ee, RawExpressionDataVector.class );
            if ( qt == null ) {
                throw new NotFoundException( String.format( "No preferred quantitation type could be found for raw expression data data of %s.", ee ) );
            }
        }
        StreamingOutput stream = ( output ) -> expressionDataFileService.writeRawExpressionData( ee, qt, new OutputStreamWriter( output ) );
        return Response.ok( stream )
                .header( "Content-Disposition", String.format( "attachment; filename=%d_%s_expmat.unfilt.raw.data.txt", ee.getId(), ee.getShortName() ) )
                .build();
    }

    /**
     * Retrieves the design for the given dataset.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{dataset}/design")
    @Produces(MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8)
    @Operation(summary = "Retrieve the design of a dataset", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8,
                    schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response getDatasetDesign( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg // Required
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        return this.outputDesignFile( ee );
    }

    /**
     * Returns true if the experiment has had batch information successfully filled in. This will be true even if there
     * is only one batch. It does not reflect the presence or absence of a batch effect.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{dataset}/hasbatch")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Indicate of a dataset has batch information", hidden = true)
    public ResponseDataObject<Boolean> getDatasetHasBatch( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg // Required
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        return Responder.respond( this.auditEventService.hasEvent( ee, BatchInformationFetchingEvent.class ) );
    }

    /**
     * Retrieves the design for the given dataset.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{dataset}/svd")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the singular value decomposition (SVD) of a dataset expression data", responses = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(ref = "ResponseDataObjectSimpleSVDValueObject"))),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<SimpleSVDValueObject> getDatasetSvd( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg // Required
    ) {
        SVDValueObject svd = svdService.getSvd( datasetArgService.getEntity( datasetArg ).getId() );
        return Responder.respond( svd == null ? null : new SimpleSVDValueObject( Arrays.asList( svd.getBioMaterialIds() ), svd.getVariances(), svd.getvMatrix().getRawMatrix() )
        );
    }

    /**
     * Retrieves the expression levels of given genes on given datasets.
     *
     * @param datasets        a list of dataset identifiers separated by commas (','). The identifiers can either be the
     *                        ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                        is more efficient. Only datasets that user has access to will be available.
     *                        <p>
     *                        You can combine various identifiers in one query, but an invalid identifier will cause the
     *                        call to yield an error.
     *                        </p>
     * @param genes           a list of gene identifiers, separated by commas (','). Identifiers can be one of
     *                        NCBI ID, Ensembl ID or official symbol. NCBI ID is the most efficient (and
     *                        guaranteed to be unique) identifier. Official symbol will return a random homologue. Use
     *                        one
     *                        of the IDs to specify the correct taxon - if the gene taxon does not match the taxon of
     *                        the
     *                        given datasets, expression levels for that gene will be missing from the response.
     *                        <p>
     *                        You can combine various identifiers in one query, but an invalid identifier will cause the
     *                        call to yield an error.
     *                        </p>
     * @param keepNonSpecific whether to keep elements that are mapped to multiple genes.
     * @param consolidate     whether genes with multiple elements should consolidate the information. The options are:
     *                        <ul>
     *                        <li>pickmax: only return the vector that has the highest expression (mean over all its
     *                        bioAssays)</li>
     *                        <li>pickvar: only return the vector with highest variance of expression across its
     *                        bioAssays</li>
     *                        <li>average: create a new vector that will average the bioAssay values from all
     *                        vectors</li>
     *                        </ul>
     */
    @GET
    @Path("/{datasets}/expressions/genes/{genes: [^/]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the expression data matrix of a set of datasets and genes")
    public ResponseDataObject<List<ExperimentExpressionLevelsValueObject>> getDatasetExpressionForGenes( // Params:
            @PathParam("datasets") DatasetArrayArg datasets, // Required
            @PathParam("genes") GeneArrayArg genes, // Required
            @QueryParam("keepNonSpecific") @DefaultValue("false") BoolArg keepNonSpecific, // Optional, default false
            @QueryParam("consolidate") ExpLevelConsolidationArg consolidate // Optional, default everything is returned
    ) {
        return Responder.respond( processedExpressionDataVectorService
                .getExpressionLevels( datasetArgService.getEntities( datasets ),
                        geneArgService.getEntities( genes ), keepNonSpecific.getValue(),
                        consolidate == null ? null : consolidate.getValue() )
        );
    }

    /**
     * Retrieves the expression levels of genes highly expressed in the given component on given datasets.
     *
     * @param datasets        a list of dataset identifiers separated by commas (','). The identifiers can either be the
     *                        ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                        is more efficient. Only datasets that user has access to will be available.
     *                        <p>
     *                        You can combine various identifiers in one query, but an invalid identifier will cause the
     *                        call to yield an error.
     *                        </p>
     * @param limit           maximum amount of returned gene-probe expression level pairs.
     * @param component       the pca component to limit the results to.
     * @param keepNonSpecific whether to keep elements that are mapped to multiple genes.
     * @param consolidate     whether genes with multiple elements should consolidate the information. The options are:
     *                        <ul>
     *                        <li>pickmax: only return the vector that has the highest expression (mean over all its
     *                        bioAssays)</li>
     *                        <li>pickvar: only return the vector with highest variance of expression across its
     *                        bioAssays</li>
     *                        <li>average: create a new vector that will average the bioAssay values from all
     *                        vectors</li>
     *                        </ul>
     */
    @GET
    @Path("/{datasets}/expressions/pca")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the principal components (PCA) of a set of datasets")
    public ResponseDataObject<List<ExperimentExpressionLevelsValueObject>> getDatasetExpressionPca( // Params:
            @PathParam("datasets") DatasetArrayArg datasets, // Required
            @QueryParam("component") @DefaultValue("1") IntArg component, // Required, default 1
            @QueryParam("limit") @DefaultValue("100") LimitArg limit, // Optional, default 100
            @QueryParam("keepNonSpecific") @DefaultValue("false") BoolArg keepNonSpecific, // Optional, default false
            @QueryParam("consolidate") ExpLevelConsolidationArg consolidate // Optional, default everything is returned
    ) {
        ArgUtils.requiredArg( component, "component" );
        return Responder.respond( processedExpressionDataVectorService
                .getExpressionLevelsPca( datasetArgService.getEntities( datasets ), limit.getValueNoMaximum(),
                        component.getValue(), keepNonSpecific.getValue(),
                        consolidate == null ? null : consolidate.getValue() )
        );
    }

    /**
     * Retrieves the expression levels of genes highly expressed in the given component on given datasets.
     *
     * @param datasets        a list of dataset identifiers separated by commas (','). The identifiers can either be the
     *                        ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                        is more efficient. Only datasets that user has access to will be available.
     *                        <p>
     *                        You can combine various identifiers in one query, but an invalid identifier will cause the
     *                        call to yield an error.
     *                        </p>
     * @param diffExSet       the ID of the differential expression set to retrieve the data from.
     * @param threshold       the FDR threshold that the differential expression has to meet to be included in the response.
     * @param limit           maximum amount of returned gene-probe expression level pairs.
     * @param keepNonSpecific whether to keep elements that are mapped to multiple genes.
     * @param consolidate     whether genes with multiple elements should consolidate the information. The options are:
     *                        <ul>
     *                        <li>pickmax: only return the vector that has the highest expression (mean over all its
     *                        bioAssays)</li>
     *                        <li>pickvar: only return the vector with highest variance of expression across its
     *                        bioAssays</li>
     *                        <li>average: create a new vector that will average the bioAssay values from all
     *                        vectors</li>
     *                        </ul>
     */
    @GET
    @Path("/{datasets}/expressions/differential")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the expression levels of a set of datasets subject to a threshold on their differential expressions")
    public ResponseDataObject<List<ExperimentExpressionLevelsValueObject>> getDatasetDifferentialExpression( // Params:
            @PathParam("datasets") DatasetArrayArg datasets, // Required
            @QueryParam("diffExSet") LongArg diffExSet, // Required
            @QueryParam("threshold") @DefaultValue("1.0") DoubleArg threshold, // Optional, default 1.0
            @QueryParam("limit") @DefaultValue("100") LimitArg limit, // Optional, default 100
            @QueryParam("keepNonSpecific") @DefaultValue("false") BoolArg keepNonSpecific, // Optional, default false
            @QueryParam("consolidate") ExpLevelConsolidationArg consolidate // Optional, default everything is returned
    ) {
        ArgUtils.requiredArg( diffExSet, "diffExSet" );
        return Responder.respond( processedExpressionDataVectorService
                .getExpressionLevelsDiffEx( datasetArgService.getEntities( datasets ),
                        diffExSet.getValue(), threshold.getValue(), limit.getValueNoMaximum(), keepNonSpecific.getValue(),
                        consolidate == null ? null : consolidate.getValue() )
        );
    }

    private Response outputDataFile( ExpressionExperiment ee, boolean filter ) throws FilteringException {
        ee = expressionExperimentService.thawLite( ee );
        File file = expressionDataFileService.writeOrLocateDataFile( ee, false, filter );
        return this.outputFile( file, DatasetsWebService.ERROR_DATA_FILE_NOT_AVAILABLE, ee.getShortName() );
    }

    private Response outputDesignFile( ExpressionExperiment ee ) {
        ee = expressionExperimentService.thawLite( ee );
        File file = expressionDataFileService.writeOrLocateDesignFile( ee, false );
        return this.outputFile( file, DatasetsWebService.ERROR_DESIGN_FILE_NOT_AVAILABLE, ee.getShortName() );
    }

    private Response outputFile( File file, String error, String shortName ) {
        if ( file == null || !file.exists() ) {
            throw new NotFoundException( String.format( error, shortName ) );
        }
        // we remove the .gz extension because we use HTTP Content-Encoding
        return Response.ok( file )
                .header( "Content-Encoding", "gzip" )
                .header( "Content-Disposition", "attachment; filename=" + FilenameUtils.removeExtension( file.getName() ) )
                .build();
    }

    private List<DifferentialExpressionAnalysisValueObject> getDiffExVos( Long eeId, int offset, int limit ) {
        Map<ExpressionExperimentDetailsValueObject, List<DifferentialExpressionAnalysisValueObject>> map = differentialExpressionAnalysisService
                .getAnalysesByExperiment( Collections.singleton( eeId ), offset, limit );
        if ( map == null || map.size() < 1 ) {
            return Collections.emptyList();
        }
        return map.get( map.keySet().iterator().next() );
    }

    @SuppressWarnings("unused") // Used for json serialization
    @Value
    private static class SimpleSVDValueObject {
        /**
         * Order same as the rows of the v matrix.
         */
        List<Long> bioMaterialIds;

        /**
         * An array of values representing the fraction of the variance each component accounts for
         */
        double[] variances;
        double[][] vMatrix;
    }
}
