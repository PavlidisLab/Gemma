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

import org.apache.commons.io.IOUtils;
import org.hibernate.QueryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.services.rest.util.Responder;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.WebService;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;
import ubic.gemma.web.services.rest.util.args.IntArg;
import ubic.gemma.web.services.rest.util.args.PlatformArg;
import ubic.gemma.web.services.rest.util.args.SortArg;
import ubic.gemma.web.util.EntityNotFoundException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import java.io.*;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * RESTful interface for platforms.
 *
 * @author tesarst
 */
@Component
@Path("/platforms")
public class PlatformsWebService extends WebService {

    private static final String ERROR_MSG_PROP_NOT_FOUND = "Platforms do not contain the given sort property.";
    private static final String ERROR_MSG_PROP_NOT_FOUND_DETAIL = "Property of name '%s' not recognized.";
    private static final String ERROR_ANNOTATION_FILE_NOT_AVAILABLE = "Annotation file for platform %s does not exist or can not be accessed.";

    private ArrayDesignService arrayDesignService;
    private ExpressionExperimentService expressionExperimentService;

    /**
     * Required by spring
     */
    public PlatformsWebService() {
    }

    /**
     * Constructor for service autowiring
     */
    @Autowired
    public PlatformsWebService( ArrayDesignService arrayDesignService,
            ExpressionExperimentService expressionExperimentService ) {
        this.arrayDesignService = arrayDesignService;
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * Lists all platforms available in gemma.
     *
     * @param offset optional parameter (defaults to 0) skips the specified amount of platforms when retrieving them from the database.
     * @param limit  optional parameter (defaults to 20) limits the result to specified amount of platforms. Use 0 for no limit.
     * @param sort   optional parameter (defaults to +id) sets the ordering property and direction. Format is [+,-][property name].
     *               E.g. -accession will convert to descending ordering by the Accession property. Note that this will not necessarily
     *               sort the objects in the response, but rather tells the SQL query how to order the table before cropping it as
     *               specified in the offset and limit.
     * @return all platforms in the database, skipping the first [{@code offset}] of platforms, and limiting the amount in the result to
     * the value of the {@code limit} parameter.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject all( // Params:
            @QueryParam("offset") @DefaultValue("0") IntArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") IntArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg sort, // Optional, default +id
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        try {
            //FIXME currently not filtering out troubled
            return Responder.autoCode( arrayDesignService
                    .loadValueObjectsFilter( offset.getValue(), limit.getValue(), sort.getField(), sort.isAsc() ), sr );
        } catch ( QueryException e ) {
            WellComposedErrorBody error = new WellComposedErrorBody( Status.BAD_REQUEST, ERROR_MSG_PROP_NOT_FOUND );
            WellComposedErrorBody.addExceptionFields( error,
                    new EntityNotFoundException( String.format( ERROR_MSG_PROP_NOT_FOUND_DETAIL, sort.getField() ) ) );
            return Responder.code( error.getStatus(), error, sr );
        }
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
        //FIXME currently not filtering out troubled
        Object response = platformArg.getValueObject( arrayDesignService );
        return this.autoCodeResponse( platformArg, response, sr );
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
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        //FIXME currently not filtering out troubled
        Object response = platformArg.getExperiments( arrayDesignService, expressionExperimentService );
        return this.autoCodeResponse( platformArg, response, sr );
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
    public ResponseDataObject platformAnnotations( // Params:
            @PathParam("platformArg") PlatformArg<Object> platformArg, // Optional, default null
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        //FIXME currently not filtering out troubled
        ArrayDesign arrayDesign = platformArg.getPersistentObject( arrayDesignService );
        if ( arrayDesign == null )
            return this.autoCodeResponse( platformArg, null, sr );
        return outputAnnotationFile( arrayDesign, sr );
    }

    private ResponseDataObject outputAnnotationFile( ArrayDesign arrayDesign, HttpServletResponse sr ) {
        String fileName = arrayDesign.getShortName().replaceAll( Pattern.quote( "/" ), "_" )
                + ArrayDesignAnnotationService.NO_PARENTS_FILE_SUFFIX
                + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX;
        File file = new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + fileName );
        if ( !file.exists() ) {
            WellComposedErrorBody error = new WellComposedErrorBody( Status.NOT_FOUND,
                    String.format( ERROR_ANNOTATION_FILE_NOT_AVAILABLE, arrayDesign.getShortName() ) );
            return Responder.code( error.getStatus(), error, sr );
        }

        try (FileInputStream inputStream = new FileInputStream( file )) {
            InputStream fileStream = new FileInputStream( file );
            InputStream gzipStream = new GZIPInputStream( fileStream );
            Reader decoder = new InputStreamReader( gzipStream, "UTF-8" );
            BufferedReader buffered = new BufferedReader( decoder );
            String content = IOUtils.toString( buffered );
            return Responder.autoCode( content, sr );
        } catch ( IOException e ) {
            WellComposedErrorBody error = new WellComposedErrorBody( Status.NOT_FOUND,
                    String.format( ERROR_ANNOTATION_FILE_NOT_AVAILABLE, arrayDesign.getShortName() ) );
            WellComposedErrorBody.addExceptionFields( error, e );
            return Responder.code( error.getStatus(), error, sr );
        }
    }
}
