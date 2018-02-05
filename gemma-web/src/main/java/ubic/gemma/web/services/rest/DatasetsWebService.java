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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.analysis.preprocess.svd.SVDValueObject;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.GeeqService;
import ubic.gemma.web.services.rest.util.*;
import ubic.gemma.web.services.rest.util.args.*;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * RESTful interface for datasets.
 *
 * @author tesarst
 */
@Service
@Path("/datasets")
public class DatasetsWebService extends
        WebServiceWithFiltering<ExpressionExperiment, ExpressionExperimentValueObject, ExpressionExperimentService> {

    private static final String ERROR_DATA_FILE_NOT_AVAILABLE = "Data file for experiment %s can not be created.";
    private static final String ERROR_DESIGN_FILE_NOT_AVAILABLE = "Design file for experiment %s can not be created.";

    private ExpressionExperimentService expressionExperimentService;
    private ExpressionDataFileService expressionDataFileService;
    private ArrayDesignService arrayDesignService;
    private BioAssayService bioAssayService;
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;
    private GeneService geneService;
    private SVDService svdService;
    private GeeqService geeqService;
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

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
            GeneService geneService, SVDService svdService, GeeqService geeqService,
            DifferentialExpressionAnalysisService differentialExpressionAnalysisService ) {
        super( expressionExperimentService );
        this.expressionExperimentService = expressionExperimentService;
        this.expressionDataFileService = expressionDataFileService;
        this.arrayDesignService = arrayDesignService;
        this.bioAssayService = bioAssayService;
        this.processedExpressionDataVectorService = processedExpressionDataVectorService;
        this.geneService = geneService;
        this.svdService = svdService;
        this.geeqService = geeqService;
        this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
    }

    /**
     * @see WebServiceWithFiltering#all(FilterArg, IntArg, IntArg, SortArg, HttpServletResponse)
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject all( // Params:
            @QueryParam("filter") @DefaultValue("") DatasetFilterArg filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") IntArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") IntArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg sort, // Optional, default +id
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return super.all( filter, offset, limit, sort, sr );
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
     * @see WebServiceWithFiltering#some(ArrayEntityArg, FilterArg, IntArg, IntArg, SortArg, HttpServletResponse)
     */
    @GET
    @Path("/{datasetsArg: [^/]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject datasets( // Params:
            @PathParam("datasetsArg") ArrayDatasetArg datasetsArg, // Optional
            @QueryParam("filter") @DefaultValue("") DatasetFilterArg filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") IntArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") IntArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg sort, // Optional, default +id
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return super.some( datasetsArg, filter, offset, limit, sort, sr );
    }

    /**
     * Retrieves platforms for the given dataset.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{datasetArg: [a-zA-Z0-9\\.-]+}/platforms")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject datasetPlatforms( // Params:
            @PathParam("datasetArg") DatasetArg<Object> datasetArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.autoCode( datasetArg.getPlatforms( expressionExperimentService, arrayDesignService ), sr );
    }

    @GET
    @Path("/geeq")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @PreAuthorize( "hasRole('GROUP_ADMIN')" ) // TODO remove, left here for reference before it is actually used somewhere.
    public ResponseDataObject datasetGeeq( // Params:
            @QueryParam("start") IntArg startArg, // Required
            @QueryParam("stop") IntArg stopArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        HashMap<Long, Exception> problems = new HashMap<>();
        long max = 11000L;
        long start = startArg.getValue();
        long stop = stopArg.getValue();
        String msg = "Success, no problems";
        int ran = 0;

        for ( long i = start; i < stop; i++ ) {
            if ( i > max ) {
                msg = "Success, max ID reached before getting to stop arg: " + max;
                break;
            }
            try {
                ExpressionExperiment ee = expressionExperimentService.load( i );
                if ( ee != null ) {
                    geeqService.calculateScore( i );
                    ran++;
                }
            } catch ( Exception e ) {
                System.out.println( i + " failed: " + e.getMessage() );
                problems.put( i, e );
            }
        }

        for ( Long id : problems.keySet() ) {
            System.out.println( id + " GEEQ FAILED: " + problems.get( id ).getMessage() );
        }

        return Responder.autoCode( problems.size() > 0 ? problems : msg + " Ran ees: " + ran, sr );
    }

    /**
     * Retrieves the samples for the given dataset.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{datasetArg: [a-zA-Z0-9\\.-]+}/samples")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject datasetSamples( // Params:
            @PathParam("datasetArg") DatasetArg<Object> datasetArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        ExpressionExperiment ee = datasetArg.getPersistentObject( expressionExperimentService );
        expressionExperimentService.getBioAssayDimensions( ee );
        return Responder.autoCode( datasetArg.getSamples( expressionExperimentService, bioAssayService ), sr );
    }

    /**
     * Retrieves the differential analysis results for the given dataset.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{datasetArg: [a-zA-Z0-9\\.-]+}/analyses/differential")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject datasetDiffAnalysis( // Params:
            @PathParam("datasetArg") DatasetArg<Object> datasetArg, // Required
            @QueryParam("offset") @DefaultValue("0") IntArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") IntArg limit, // Optional, default 20
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.autoCode(
                this.getDiffExVos( datasetArg.getPersistentObject( expressionExperimentService ).getId(),
                        offset.getValue(), limit.getValue() ), sr );
    }

    /**
     * Retrieves the annotations for the given dataset.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{datasetArg: [a-zA-Z0-9\\.-]+}/annotations")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject datasetAnnotations( // Params:
            @PathParam("datasetArg") DatasetArg<Object> datasetArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.autoCode( datasetArg.getAnnotations( expressionExperimentService ), sr );
    }

    /**
     * Retrieves the data for the given dataset.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     * @param filterData return filtered the expression data.
     */
    @GET
    @Path("/{datasetArg: [a-zA-Z0-9\\.-]+}/data")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response datasetData( // Params:
            @PathParam("datasetArg") DatasetArg<Object> datasetArg, // Required
            @QueryParam("filter") @DefaultValue("false") BoolArg filterData, // Optional, default false
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        ExpressionExperiment ee = datasetArg.getPersistentObject( expressionExperimentService );
        return outputDataFile( ee, filterData.getValue() );
    }

    /**
     * Retrieves the design for the given dataset.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{datasetArg: [a-zA-Z0-9\\.-]+}/design")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response datasetDesign( // Params:
            @PathParam("datasetArg") DatasetArg<Object> datasetArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        ExpressionExperiment ee = datasetArg.getPersistentObject( expressionExperimentService );
        return outputDesignFile( ee );
    }

    /**
     * Retrieves the design for the given dataset.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{datasetArg: [a-zA-Z0-9\\.-]+}/svd")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject datasetSVD( // Params:
            @PathParam("datasetArg") DatasetArg<Object> datasetArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        SVDValueObject svd = svdService.getSvd( datasetArg.getPersistentObject( expressionExperimentService ).getId() );
        return Responder.autoCode( svd == null ?
                null :
                new SimpleSVDValueObject( svd.getBioMaterialIds(), svd.getVariances(), svd.getvMatrix() ), sr );
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
     *                        guaranteed to be unique) identifier. Official symbol will return a random homologue. Use one
     *                        of the IDs to specify the correct taxon - if the gene taxon does not match the taxon of the
     *                        given datasets, expression levels for that gene will be missing from the response.
     *                        <p>
     *                        You can combine various identifiers in one query, but an invalid identifier will cause the
     *                        call to yield an error.
     *                        </p>
     * @param keepNonSpecific whether to keep elements that are mapped to multiple genes.
     * @param consolidate     whether genes with multiple elements should consolidate the information. The options are:
     *                        <ul>
     *                        <li>pickmax: only return the vector that has the highest expression (mean over all its bioAssays)</li>
     *                        <li>pickvar: only return the vector with highest variance of expression across its bioAssays</li>
     *                        <li>average: create a new vector that will average the bioAssay values from all vectors</li>
     *                        </ul>
     */
    @GET
    @Path("/{datasets: [^/]+}/expressions/genes/{genes: [^/]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject datasetExpressions( // Params:
            @PathParam("datasets") ArrayDatasetArg datasets, // Required
            @PathParam("genes") ArrayGeneArg genes, // Required
            @QueryParam("keepNonSpecific") @DefaultValue("false") BoolArg keepNonSpecific, // Optional, default false
            @QueryParam("consolidate") @DefaultValue("") ExpLevelConsolidationArg consolidate,
            // Optional, default false
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.autoCode( processedExpressionDataVectorService
                .getExpressionLevels( datasets.getPersistentObjects( expressionExperimentService ),
                        genes.getPersistentObjects( geneService ), keepNonSpecific.getValue(),
                        consolidate == null ? null : consolidate.getValue() ), sr );
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
     *                        <li>pickmax: only return the vector that has the highest expression (mean over all its bioAssays)</li>
     *                        <li>pickvar: only return the vector with highest variance of expression across its bioAssays</li>
     *                        <li>average: create a new vector that will average the bioAssay values from all vectors</li>
     *                        </ul>
     */
    @GET
    @Path("/{datasets: [^/]+}/expressions/pca")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject datasetExpressionsPca( // Params:
            @PathParam("datasets") ArrayDatasetArg datasets, // Required
            @QueryParam("component") IntArg component, // Required, default 1
            @QueryParam("limit") @DefaultValue("100") IntArg limit, // Optional, default 100
            @QueryParam("keepNonSpecific") @DefaultValue("false") BoolArg keepNonSpecific, // Optional, default false
            @QueryParam("consolidate") @DefaultValue("") ExpLevelConsolidationArg consolidate,
            // Optional, default false
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        checkReqArg( component, "component" );
        return Responder.autoCode( processedExpressionDataVectorService
                .getExpressionLevelsPca( datasets.getPersistentObjects( expressionExperimentService ), limit.getValue(),
                        component.getValue(), keepNonSpecific.getValue(),
                        consolidate == null ? null : consolidate.getValue() ), sr );
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
     * @param threshold       the threshold that the differential expression has to meet to be included in the response.
     * @param limit           maximum amount of returned gene-probe expression level pairs.
     * @param keepNonSpecific whether to keep elements that are mapped to multiple genes.
     * @param consolidate     whether genes with multiple elements should consolidate the information. The options are:
     *                        <ul>
     *                        <li>pickmax: only return the vector that has the highest expression (mean over all its bioAssays)</li>
     *                        <li>pickvar: only return the vector with highest variance of expression across its bioAssays</li>
     *                        <li>average: create a new vector that will average the bioAssay values from all vectors</li>
     *                        </ul>
     */
    @GET
    @Path("/{datasets: [^/]+}/expressions/differential")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject datasetExpressionsDiffEx( // Params:
            @PathParam("datasets") ArrayDatasetArg datasets, // Required
            @QueryParam("diffExSet") LongArg diffExSet, // Required
            @QueryParam("threshold") @DefaultValue("100.0") DoubleArg threshold, // Optional, default 100.0
            @QueryParam("limit") @DefaultValue("100") IntArg limit, // Optional, default 100
            @QueryParam("keepNonSpecific") @DefaultValue("false") BoolArg keepNonSpecific, // Optional, default false
            @QueryParam("consolidate") @DefaultValue("") ExpLevelConsolidationArg consolidate,
            // Optional, default false
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        checkReqArg( diffExSet, "diffExSet" );
        return Responder.autoCode( processedExpressionDataVectorService
                .getExpressionLevelsDiffEx( datasets.getPersistentObjects( expressionExperimentService ),
                        diffExSet.getValue(), threshold.getValue(), limit.getValue(), keepNonSpecific.getValue(),
                        consolidate == null ? null : consolidate.getValue() ), sr );
    }

    private Response outputDataFile( ExpressionExperiment ee, boolean filter ) {
        ee = expressionExperimentService.thawLite( ee );
        File file = expressionDataFileService.writeOrLocateDataFile( ee, false, filter );
        return outputFile( file, ERROR_DATA_FILE_NOT_AVAILABLE, ee.getShortName() );
    }

    private Response outputDesignFile( ExpressionExperiment ee ) {
        ee = expressionExperimentService.thawLite( ee );
        File file = expressionDataFileService.writeOrLocateDesignFile( ee, false );
        return outputFile( file, ERROR_DESIGN_FILE_NOT_AVAILABLE, ee.getShortName() );
    }

    private Response outputFile( File file, String error, String shortName ) {
        try {
            if ( file == null || !file.exists() ) {
                WellComposedErrorBody errorBody = new WellComposedErrorBody( Response.Status.NOT_FOUND,
                        String.format( error, shortName ) );
                throw new GemmaApiException( errorBody );
            }

            return Response.ok( Files.readAllBytes( file.toPath() ) )
                    .header( "Content-Disposition", "attachment; filename=" + file.getName() ).build();
        } catch ( IOException e ) {
            WellComposedErrorBody errorBody = new WellComposedErrorBody( Response.Status.NOT_FOUND,
                    String.format( error, shortName ) );
            errorBody.addErrorsField( "error", e.getMessage() );

            throw new GemmaApiException( errorBody );
        }
    }

    private Collection getDiffExVos( Long eeId, int offset, int limit ) {
        Map<ExpressionExperimentDetailsValueObject, Collection<DifferentialExpressionAnalysisValueObject>> map = differentialExpressionAnalysisService
                .getAnalysesByExperiment( Collections.singleton( eeId ), offset, limit );
        if ( map == null || map.size() < 1 ) {
            return Collections.EMPTY_LIST;
        }
        return map.get( map.keySet().iterator().next() );
    }

    @SuppressWarnings("unused") // Used for json serialization
    private class SimpleSVDValueObject {
        /**
         * Order same as the rows of the v matrix.
         */
        private Long[] bioMaterialIds;

        /**
         * An array of values representing the fraction of the variance each component accounts for
         */
        private Double[] variances;
        private DoubleMatrix<Long, Integer> vMatrix;

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
