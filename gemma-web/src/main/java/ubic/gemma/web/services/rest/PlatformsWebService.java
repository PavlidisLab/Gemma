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
import org.springframework.stereotype.Component;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.services.rest.util.*;
import ubic.gemma.web.services.rest.util.args.*;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.text.ParseException;
import java.util.regex.Pattern;

/**
 * RESTful interface for platforms.
 *
 * @author tesarst
 */
@Component
@Path("/platforms")
public class PlatformsWebService extends WebServiceWithFiltering {

    private static final String ERROR_ANNOTATION_FILE_NOT_AVAILABLE = "Annotation file for platform %s does not exist or can not be accessed.";

    private GeneService geneService;
    private ArrayDesignService arrayDesignService;
    private ExpressionExperimentService expressionExperimentService;
    private CompositeSequenceService compositeSequenceService;

    /**
     * Required by spring
     */
    public PlatformsWebService() {
    }

    /**
     * Constructor for service autowiring
     */
    @Autowired
    public PlatformsWebService( GeneService geneService, ArrayDesignService arrayDesignService,
            ExpressionExperimentService expressionExperimentService,
            CompositeSequenceService compositeSequenceService ) {
        this.geneService = geneService;
        this.arrayDesignService = arrayDesignService;
        this.expressionExperimentService = expressionExperimentService;
        this.compositeSequenceService = compositeSequenceService;
    }

    /**
     * @see WebServiceWithFiltering#all(FilterArg, IntArg, IntArg, SortArg, HttpServletResponse)
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject all( // Params:
            @QueryParam("filter") @DefaultValue("") PlatformFilterArg filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") IntArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") IntArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg sort, // Optional, default +id
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        // Uses this.loadVOsPreFilter(...)
        return super.all( filter, offset, limit, sort, sr );
    }

    /**
     * Retrieves single platform based on the given identifier.
     *
     * @param platformArg can either be the ArrayDesign ID or its short name (e.g. "Generic_yeast" or "GPL1355" ). Retrieval by ID
     *                    is more efficient. Only platforms that user has access to will be available.
     */
    @GET
    @Path("/{platformArg: [a-zA-Z0-9\\.]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject platform( // Params:
            @PathParam("platformArg") PlatformArg<Object> platformArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.autoCode( platformArg.getValueObject( arrayDesignService ), sr );
    }

    /**
     * Retrieves experiments in the given platform.
     *
     * @param platformArg can either be the ArrayDesign ID or its short name (e.g. "Generic_yeast" or "GPL1355" ). Retrieval by ID
     *                    is more efficient. Only platforms that user has access to will be available.
     */
    @GET
    @Path("/{platformArg: [a-zA-Z0-9\\.]+}/datasets")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject platformDatasets( // Params:
            @PathParam("platformArg") PlatformArg<Object> platformArg, // Required
            @QueryParam("offset") @DefaultValue("0") IntArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") IntArg limit, // Optional, default 20
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.autoCode( platformArg
                        .getExperiments( arrayDesignService, expressionExperimentService, limit.getValue(), offset.getValue() ),
                sr );
    }

    /**
     * Retrieves the composite sequences (elements) for the given platform.
     *
     * @param platformArg can either be the ArrayDesign ID or its short name (e.g. "Generic_yeast" or "GPL1355" ). Retrieval by ID
     *                    is more efficient. Only platforms that user has access to will be available.
     * @param offset      optional parameter (defaults to 0) skips the specified amount of datasets when retrieving them from the database.
     * @param limit       optional parameter (defaults to 20) limits the result to specified amount of datasets. Use 0 for no limit.
     */
    @GET
    @Path("/{platformArg: [a-zA-Z0-9\\.]+}/elements")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject platformElements( // Params:
            @PathParam("platformArg") PlatformArg<Object> platformArg, // Required
            @QueryParam("offset") @DefaultValue("0") IntArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") IntArg limit, // Optional, default 20
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.autoCode( platformArg
                .getElements( arrayDesignService, compositeSequenceService, limit.getValue(), offset.getValue() ), sr );
    }

    /**
     * Retrieves the genes on the given platform element
     *
     * @param platformArg can either be the ArrayDesign ID or its short name (e.g. "Generic_yeast" or "GPL1355" ). Retrieval by ID
     *                    is more efficient. Only platforms that user has access to will be available.
     * @param probeArg    the name of the platform element for which the genes should be retrieved.
     * @param offset      optional parameter (defaults to 0) skips the specified amount of datasets when retrieving them from the database.
     * @param limit       optional parameter (defaults to 20) limits the result to specified amount of datasets. Use 0 for no limit.
     */
    @GET
    @Path("/{platformArg: [a-zA-Z0-9\\.]+}/elements/{probeArg: [a-zA-Z0-9_%2F\\.-]+}/genes")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject platformElementGenes( // Params:
            @PathParam("platformArg") PlatformArg<Object> platformArg, // Required
            @PathParam("probeArg") CompositeSequenceArg probeArg, // Required
            @QueryParam("offset") @DefaultValue("0") IntArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") IntArg limit, // Optional, default 20
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        probeArg.setPlatform( platformArg.getPersistentObject( arrayDesignService ) );
        return Responder.autoCode( geneService.loadValueObjects( compositeSequenceService
                .getGenes( probeArg.getPersistentObject( compositeSequenceService ), offset.getValue(),
                        limit.getValue() ) ), sr );
    }

    /**
     * Retrieves the annotation file for the given platform.
     *
     * @param platformArg can either be the ArrayDesign ID or its short name (e.g. "Generic_yeast" or "GPL1355" ). Retrieval by ID
     *                    is more efficient. Only platforms that user has access to will be available.
     * @return the content of the annotation file of the given platform.
     */
    @GET
    @Path("/{platformArg: [a-zA-Z0-9\\.]+}/annotations")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response platformAnnotations( // Params:
            @PathParam("platformArg") PlatformArg<Object> platformArg, // Optional, default null
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return outputAnnotationFile( platformArg.getPersistentObject( arrayDesignService ) );
    }

    @Override
    protected ResponseDataObject loadVOsPreFilter( FilterArg filter, IntArg offset, IntArg limit, SortArg sort,
            HttpServletResponse sr ) throws ParseException {
        return Responder.autoCode( arrayDesignService
                .loadValueObjectsPreFilter( offset.getValue(), limit.getValue(), sort.getField(), sort.isAsc(),
                        filter.getObjectFilters() ), sr );
    }

    /**
     * Creates a response with the annotation file for given array design
     *
     * @param arrayDesign the platform to fetch and output the annotation file for.
     * @return a Response object containing the annotation file.
     */
    private Response outputAnnotationFile( ArrayDesign arrayDesign ) {
        String fileName = arrayDesign.getShortName().replaceAll( Pattern.quote( "/" ), "_" )
                + ArrayDesignAnnotationService.NO_PARENTS_FILE_SUFFIX
                + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX;
        File file = new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + fileName );
        if ( !file.exists() ) {
            WellComposedErrorBody errorBody = new WellComposedErrorBody( Status.NOT_FOUND,
                    String.format( ERROR_ANNOTATION_FILE_NOT_AVAILABLE, arrayDesign.getShortName() ) );
            throw new GemmaApiException( errorBody );
        }

        return Response.ok( file ).header( "Content-Disposition", "attachment; filename=" + file.getName() ).build();
    }

}
