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
import org.springframework.stereotype.Service;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
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
import java.text.ParseException;

/**
 * RESTful interface for datasets.
 *
 * @author tesarst
 */
@Service
@Path("/datasets")
public class DatasetsWebService extends WebServiceWithFiltering {

    private static final String ERROR_DATA_FILE_NOT_AVAILABLE = "Data file for experiment %s can not be created.";
    private static final String ERROR_DESIGN_FILE_NOT_AVAILABLE = "Design file for experiment %s can not be created.";

    private ExpressionExperimentService expressionExperimentService;
    private ExpressionDataFileService expressionDataFileService;
    private ArrayDesignService arrayDesignService;
    private BioAssayService bioAssayService;

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
            BioAssayService bioAssayService ) {
        this.expressionExperimentService = expressionExperimentService;
        this.expressionDataFileService = expressionDataFileService;
        this.arrayDesignService = arrayDesignService;
        this.bioAssayService = bioAssayService;
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
        // Uses this.loadVOsPreFilter(...)
        return super.all( filter, offset, limit, sort, sr );
    }

    /**
     * Retrieves single dataset based on the given identifier.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{datasetArg: [a-zA-Z0-9\\.]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject dataset( // Params:
            @PathParam("datasetArg") DatasetArg<Object> datasetArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        Object response = datasetArg.getValueObject( expressionExperimentService );
        return this.autoCodeResponse( datasetArg, response, sr );
    }

    /**
     * Retrieves platforms for the given experiment.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{datasetArg: [a-zA-Z0-9\\.]+}/platforms")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject datasetPlatforms( // Params:
            @PathParam("datasetArg") DatasetArg<Object> datasetArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        Object response = datasetArg.getPlatforms( expressionExperimentService, arrayDesignService );
        return this.autoCodeResponse( datasetArg, response, sr );
    }

    /**
     * Retrieves the samples for given experiment.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{datasetArg: [a-zA-Z0-9\\.]+}/samples")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject datasetSamples( // Params:
            @PathParam("datasetArg") DatasetArg<Object> datasetArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        Object response = datasetArg.getSamples( expressionExperimentService, bioAssayService );
        return this.autoCodeResponse( datasetArg, response, sr );
    }

    /**
     * Retrieves the annotations for given experiment.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{datasetArg: [a-zA-Z0-9\\.]+}/annotations")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject datasetAnnotations( // Params:
            @PathParam("datasetArg") DatasetArg<Object> datasetArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        Object response = datasetArg.getAnnotations( expressionExperimentService );
        return this.autoCodeResponse( datasetArg, response, sr );
    }

    /**
     * Retrieves the data for given experiment.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     * @param filterData filters the expression data so that they do not contain samples under a minimum threshold.
     */
    @GET
    @Path("/{datasetArg: [a-zA-Z0-9\\.]+}/data")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response datasetData( // Params:
            @PathParam("datasetArg") DatasetArg<Object> datasetArg, // Required
            @QueryParam("filter") @DefaultValue("false") BoolArg filterData, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        ExpressionExperiment ee = datasetArg.getPersistentObject( expressionExperimentService );
        if ( ee == null ) {
            WellComposedErrorBody errorBody = new WellComposedErrorBody( Response.Status.NOT_FOUND,
                    datasetArg.getNullCause() );
            throw new GemmaApiException( errorBody );
        }
        return outputDataFile( ee, filterData.getValue() );
    }

    /**
     * Retrieves the design for given experiment.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{datasetArg: [a-zA-Z0-9\\.]+}/design")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response datasetDesign( // Params:
            @PathParam("datasetArg") DatasetArg<Object> datasetArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        ExpressionExperiment ee = datasetArg.getPersistentObject( expressionExperimentService );
        if ( ee == null ) {
            WellComposedErrorBody errorBody = new WellComposedErrorBody( Response.Status.NOT_FOUND,
                    datasetArg.getNullCause() );
            throw new GemmaApiException( errorBody );
        }
        return outputDesignFile( ee );
    }

    @Override
    protected ResponseDataObject loadVOsPreFilter( FilterArg filter, IntArg offset, IntArg limit, SortArg sort,
            HttpServletResponse sr ) throws ParseException {
        return Responder.autoCode( expressionExperimentService
                .loadValueObjectsPreFilter( offset.getValue(), limit.getValue(), sort.getField(), sort.isAsc(),
                        filter.getObjectFilters() ), sr );
    }

    private Response outputDataFile( ExpressionExperiment ee, boolean filter ) {
        ee = expressionExperimentService.thawLite( ee );
        File file = expressionDataFileService.writeTemporaryDataFile( ee, filter );
        return outputFile( file, ERROR_DATA_FILE_NOT_AVAILABLE, ee.getShortName() );
    }

    private Response outputDesignFile( ExpressionExperiment ee ) {
        ee = expressionExperimentService.thawLite( ee );
        File file = expressionDataFileService.writeTemporaryDesignFile( ee );
        return outputFile( file, ERROR_DESIGN_FILE_NOT_AVAILABLE, ee.getShortName() );
    }

    private Response outputFile( File file, String error, String shortName ) {
        try {
            if ( file == null || !file.exists() ) {
                WellComposedErrorBody errorBody = new WellComposedErrorBody( Response.Status.NOT_FOUND,
                        String.format( error, shortName ) );
                throw new GemmaApiException( errorBody );
            }

            Response response = Response.ok( Files.readAllBytes( file.toPath() ) )
                    .header( "Content-Disposition", "attachment; filename=" + file.getName() ).build();

            deleteFile( file );

            return response;
        } catch ( IOException e ) {
            WellComposedErrorBody errorBody = new WellComposedErrorBody( Response.Status.NOT_FOUND,
                    String.format( error, shortName ) );
            errorBody.addErrorsField( "error", e.getMessage() );

            deleteFile( file );

            throw new GemmaApiException( errorBody );
        }
    }

    private void deleteFile( File file ) {
        if ( file.canWrite() && file.delete() ) {
            log.info( "Deleted: " + file );
        } else {
            log.warn( "Could not delete file: " + file );
        }
    }

}