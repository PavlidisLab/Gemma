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
package ubic.gemma.web.services.rest;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.analysis.preprocess.svd.SVDValueObject;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationFetchingEvent;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.ExperimentExpressionLevelsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.web.services.rest.util.ArgUtils;
import ubic.gemma.web.services.rest.util.PaginatedResponseDataObject;
import ubic.gemma.web.services.rest.util.Responder;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.args.*;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.*;

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

    private ExpressionExperimentService service;
    private ExpressionExperimentService expressionExperimentService;
    private ExpressionDataFileService expressionDataFileService;
    private ArrayDesignService arrayDesignService;
    private BioAssayService bioAssayService;
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;
    private GeneService geneService;
    private SVDService svdService;
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    private AuditEventService auditEventService;
    private OutlierDetectionService outlierDetectionService;

    /**
     * Required by spring
     */
    public DatasetsWebService() {
    }

    /**
     * Constructor for service autowiring
     */
    @Autowired
    public DatasetsWebService( ExpressionExperimentService expressionExperimentService,
            ExpressionDataFileService expressionDataFileService, ArrayDesignService arrayDesignService,
            BioAssayService bioAssayService, ProcessedExpressionDataVectorService processedExpressionDataVectorService,
            GeneService geneService, SVDService svdService,
            DifferentialExpressionAnalysisService differentialExpressionAnalysisService, AuditEventService auditEventService,
            OutlierDetectionService outlierDetectionService ) {
        this.service = expressionExperimentService;
        this.expressionExperimentService = expressionExperimentService;
        this.expressionDataFileService = expressionDataFileService;
        this.arrayDesignService = arrayDesignService;
        this.bioAssayService = bioAssayService;
        this.processedExpressionDataVectorService = processedExpressionDataVectorService;
        this.geneService = geneService;
        this.svdService = svdService;
        this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
        this.auditEventService = auditEventService;
        this.outlierDetectionService = outlierDetectionService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve all datasets")
    public PaginatedResponseDataObject<ExpressionExperimentValueObject> all( // Params:
            @QueryParam("filter") @DefaultValue("") FilterArg filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg sort, // Optional, default +id
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.paginate( service.loadValueObjectsPreFilter( filter.getObjectFilters( expressionExperimentService ), sort.getSort( expressionExperimentService ), offset.getValue(), limit.getValue() ) );
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
    public PaginatedResponseDataObject<ExpressionExperimentValueObject> datasets( // Params:
            @PathParam("dataset") DatasetArrayArg datasetsArg, // Optional
            @QueryParam("filter") @DefaultValue("") FilterArg filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg sort, // Optional, default +id
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        Filters filters = filter.getObjectFilters( expressionExperimentService );
        if ( filters == null ) {
            filters = new Filters();
        }
        filters.add( datasetsArg.getObjectFilters( service ) );
        return Responder.paginate( service.loadValueObjectsPreFilter( filters, sort.getSort( expressionExperimentService ), offset.getValue(), limit.getValue() ) );
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
    @Operation(summary = "Retrieve the platform of a dataset")
    //    @PreAuthorize( "hasRole('GROUP_ADMIN')" )
    public ResponseDataObject<List<ArrayDesignValueObject>> datasetPlatforms( // Params:
            @PathParam("dataset") DatasetArg<Object> datasetArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
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
    @Operation(summary = "Retrieve the samples of a dataset")
    public ResponseDataObject<List<BioAssayValueObject>> datasetSamples( // Params:
            @PathParam("dataset") DatasetArg<Object> datasetArg, // Required
            @QueryParam("factorValues") FactorValueArrayArg factorValues,
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
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
    @Path("/{datasetArg}/analyses/differential")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the main differential analysis of a dataset")
    public ResponseDataObject<List<DifferentialExpressionAnalysisValueObject>> datasetDiffAnalysis( // Params:
            @PathParam("datasetArg") DatasetArg<Object> datasetArg, // Required
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.respond(
                this.getDiffExVos( datasetArg.getEntity( expressionExperimentService ).getId(),
                        offset.getValue(), limit.getValue() )
        );
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
    @Operation(summary = "Retrieve the annotations analysis of a dataset")
    public ResponseDataObject<Set<AnnotationValueObject>> datasetAnnotations( // Params:
            @PathParam("dataset") DatasetArg<Object> datasetArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.respond( datasetArg.getAnnotations( expressionExperimentService ) );
    }

    /**
     * Retrieves the data for the given dataset.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     * @param filterData return filtered the expression data.
     */
    @GET
    @Path("/{dataset}/data")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the expression data of a dataset")
    public Response datasetData( // Params:
            @PathParam("dataset") DatasetArg<Object> datasetArg, // Required
            @QueryParam("filter") @DefaultValue("false") BoolArg filterData, // Optional, default false
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        ExpressionExperiment ee = datasetArg.getEntity( expressionExperimentService );
        return this.outputDataFile( ee, filterData.getValue() );
    }

    /**
     * Retrieves the design for the given dataset.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{dataset}/design")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the design of a dataset")
    public Response datasetDesign( // Params:
            @PathParam("dataset") DatasetArg<Object> datasetArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        ExpressionExperiment ee = datasetArg.getEntity( expressionExperimentService );
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
    @Operation(summary = "Indicate of a dataset has batch information")
    public ResponseDataObject<Boolean> datasetHasBatch( // Params:
            @PathParam("dataset") DatasetArg<Object> datasetArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        ExpressionExperiment ee = datasetArg.getEntity( expressionExperimentService );
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
    @Operation(summary = "Retrieve the singular value decomposition (SVD) of a dataset expression data")
    public ResponseDataObject<SimpleSVDValueObject> datasetSVD( // Params:
            @PathParam("dataset") DatasetArg<Object> datasetArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        SVDValueObject svd = svdService.getSvd( datasetArg.getEntity( expressionExperimentService ).getId() );
        return Responder.respond( svd == null ? null : new SimpleSVDValueObject( svd.getBioMaterialIds(), svd.getVariances(), svd.getvMatrix() )
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
    public ResponseDataObject<List<ExperimentExpressionLevelsValueObject>> datasetExpressions( // Params:
            @PathParam("datasets") DatasetArrayArg datasets, // Required
            @PathParam("genes") GeneArrayArg genes, // Required
            @QueryParam("keepNonSpecific") @DefaultValue("false") BoolArg keepNonSpecific, // Optional, default false
            @QueryParam("consolidate") @DefaultValue("") ExpLevelConsolidationArg consolidate,
            // Optional, default false
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.respond( processedExpressionDataVectorService
                .getExpressionLevels( datasets.getEntities( expressionExperimentService ),
                        genes.getEntities( geneService ), keepNonSpecific.getValue(),
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
    public ResponseDataObject<List<ExperimentExpressionLevelsValueObject>> datasetExpressionsPca( // Params:
            @PathParam("datasets") DatasetArrayArg datasets, // Required
            @QueryParam("component") @DefaultValue("1") IntArg component, // Required, default 1
            @QueryParam("limit") @DefaultValue("100") LimitArg limit, // Optional, default 100
            @QueryParam("keepNonSpecific") @DefaultValue("false") BoolArg keepNonSpecific, // Optional, default false
            @QueryParam("consolidate") @DefaultValue("") ExpLevelConsolidationArg consolidate, // Optional, default false
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        ArgUtils.requiredArg( component, "component" );
        return Responder.respond( processedExpressionDataVectorService
                .getExpressionLevelsPca( datasets.getEntities( expressionExperimentService ), limit.getValueNoMaximum(),
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
    public ResponseDataObject<List<ExperimentExpressionLevelsValueObject>> datasetExpressionsDiffEx( // Params:
            @PathParam("datasets") DatasetArrayArg datasets, // Required
            @QueryParam("diffExSet") LongArg diffExSet, // Required
            @QueryParam("threshold") @DefaultValue("1.0") DoubleArg threshold, // Optional, default 1.0
            @QueryParam("limit") @DefaultValue("100") LimitArg limit, // Optional, default 100
            @QueryParam("keepNonSpecific") @DefaultValue("false") BoolArg keepNonSpecific, // Optional, default false
            @QueryParam("consolidate") @DefaultValue("") ExpLevelConsolidationArg consolidate,
            // Optional, default false
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        ArgUtils.requiredArg( diffExSet, "diffExSet" );
        return Responder.respond( processedExpressionDataVectorService
                .getExpressionLevelsDiffEx( datasets.getEntities( expressionExperimentService ),
                        diffExSet.getValue(), threshold.getValue(), limit.getValueNoMaximum(), keepNonSpecific.getValue(),
                        consolidate == null ? null : consolidate.getValue() )
        );
    }

    private Response outputDataFile( ExpressionExperiment ee, boolean filter ) {
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
        return Response.ok( file )
                .header( "Content-Disposition", "attachment; filename=" + file.getName() )
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
    private static class SimpleSVDValueObject {
        /**
         * Order same as the rows of the v matrix.
         */
        private final Long[] bioMaterialIds;

        /**
         * An array of values representing the fraction of the variance each component accounts for
         */
        private final Double[] variances;
        private final DoubleMatrix<Long, Integer> vMatrix;

        SimpleSVDValueObject( Long[] bioMaterialIds, Double[] variances, DoubleMatrix<Long, Integer> vMatrix ) {
            this.bioMaterialIds = bioMaterialIds;
            this.variances = variances;
            this.vMatrix = vMatrix;
        }

        public Long[] getBioMaterialIds() {
            return bioMaterialIds;
        }

        public Double[] getVariances() {
            return variances;
        }

        public DoubleMatrix<Long, Integer> getvMatrix() {
            return vMatrix;
        }
    }

}
