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
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationServiceImpl;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.web.remote.JsonReaderResponse;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * RESTful interface for platforms.
 *
 * @author tesarst
 */
@Component
@Path("/platforms")
public class PlatformsWebService{

    private ArrayDesignService arrayDesignService;

    /**
     * Required by spring
     */
    public PlatformsWebService() {
    }

    /**
     * Constructor for service autowiring
     */
    @Autowired
    public PlatformsWebService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /* ********************************
     * API GET Methods
     * ********************************/

    /**
     * Fetch the platform annotation file for the indicated id. GO annotations are the "no parents" ones.
     *
     */
    @GET
    @Path("/{id}/annotations")
    @Produces(MediaType.APPLICATION_JSON)
    public String fetchAnnotationsById( @PathParam("id") final Long id, @Context HttpServletResponse servletResponse ) {
        ArrayDesign arrayDesign = arrayDesignService.load( id );
        if ( arrayDesign == null ) {
            ResponseBuilder builder = Response.status( Status.NOT_FOUND );
            builder.type( MediaType.TEXT_PLAIN );
            throw new WebApplicationException( builder.build() );
        }
        return fetchAnnotations( arrayDesign, servletResponse );
    }

    /**
     * Fetch a list of all the available platforms, limited to those which have annotation files available.
     *
     * @return JSON representing a collection of ArrayDesignValueObjects.
     */
    @GET
    @Path("/listAll")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonReaderResponse<ArrayDesignValueObject> listAll() {

        String fileType = ArrayDesignAnnotationService.NO_PARENTS_FILE_SUFFIX;
        Collection<ArrayDesignValueObject> vos = arrayDesignService.loadAllValueObjects();
        for ( ArrayDesignValueObject arrayDesign : vos ) {
            String fileBaseName = ArrayDesignAnnotationServiceImpl.mungeFileName( arrayDesign.getShortName() );
            String fileName = fileBaseName + fileType + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX;

            File f = new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + fileName );
            if ( f.exists() && f.canRead() ) {
                arrayDesign.setHasAnnotationFile( true );
            }
        }

        return new JsonReaderResponse<ArrayDesignValueObject>( new ArrayList<ArrayDesignValueObject>( vos ) );
    }

    /* ********************************
     * Private methods
     * ********************************/

    private String fetchAnnotations( ArrayDesign arrayDesign, HttpServletResponse servletResponse ) {

        String fileBaseName = arrayDesign.getShortName().replaceAll( Pattern.quote( "/" ), "_" );
        String fileName = fileBaseName + ArrayDesignAnnotationService.NO_PARENTS_FILE_SUFFIX
                + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX;

        File f = new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + fileName );

        if ( !f.canRead() ) {
            //log.error( "No annotation file for arraydesign " + arrayDesign.getShortName() + " found in " + fileName );
            ResponseBuilder builder = Response.status( Status.NOT_FOUND );
            builder.type( MediaType.TEXT_PLAIN );
            throw new WebApplicationException( builder.build() );
        }

        try (InputStream reader = new BufferedInputStream( new FileInputStream( f ) );
                OutputStream os = servletResponse.getOutputStream();) {

            byte[] buf = new byte[1024];
            int len;

            while ( ( len = reader.read( buf ) ) > 0 ) {
                os.write( buf, 0, len );
            }
            reader.close();

        } catch ( IOException e ) {
            ResponseBuilder builder = Response.status( Status.NOT_FOUND );
            builder.type( MediaType.TEXT_PLAIN );
            throw new WebApplicationException( builder.build() );
        }

        return arrayDesign.getShortName();
    }
}
