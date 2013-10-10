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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.analysis.service.ArrayDesignAnnotationServiceImpl;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.web.remote.JsonReaderResponse;

/**
 * RESTful services. Used by ErmineJ
 * 
 * @author paul
 * @version $Id$
 */
@Component
@Path("/arraydesign")
public class ArrayDesignWebService {

    private static Log log = LogFactory.getLog( ArrayDesignWebService.class );

    @Autowired
    private ArrayDesignService arrayDesignService = null;

    /**
     * Fetch the platform annotation file for the indicated shortName. GO annotations are the "no parents" ones.
     * 
     * @param shortName
     * @param servletResponse
     * @return
     * @deprecated because of problems with some shortNames (e.g., those containing '/'; see ermineJ bug 3480
     */
    @Deprecated
    @GET
    @Path("/fetchAnnotations/{shortName}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public String fetchAnnotations( @PathParam("shortName") final String shortName,
            @Context HttpServletResponse servletResponse ) {

        log.info( "Fetching annotation file for: " + shortName );

        ArrayDesign arrayDesign = arrayDesignService.findByShortName( shortName );
        if ( arrayDesign == null ) {
            log.error( "No array design with shortName=" + shortName + " found" );
            ResponseBuilder builder = Response.status( Status.NOT_FOUND );
            builder.type( MediaType.TEXT_PLAIN );
            throw new WebApplicationException( builder.build() );
        }
        return fetchAnnotations( arrayDesign, servletResponse );

    }

    /**
     * Fetch the platform annotation file for the indicated id. GO annotations are the "no parents" ones.
     * 
     * @param shortName
     * @param servletResponse
     * @return
     */
    @GET
    @Path("/fetchAnnotationsById/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public String fetchAnnotationsById( @PathParam("id") final Long id, @Context HttpServletResponse servletResponse ) {
        ArrayDesign arrayDesign = arrayDesignService.load( id );
        if ( arrayDesign == null ) {
            log.error( "No array design with id=" + id + " found" );
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

    /**
     * @param arrayDesign
     * @param servletResponse
     * @return
     */
    private String fetchAnnotations( ArrayDesign arrayDesign, HttpServletResponse servletResponse ) {

        String fileBaseName = arrayDesign.getShortName().replaceAll( Pattern.quote( "/" ), "_" );
        String fileName = fileBaseName + ArrayDesignAnnotationService.NO_PARENTS_FILE_SUFFIX
                + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX;

        File f = new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + fileName );

        if ( !f.canRead() ) {
            log.error( "No annotation file for arraydesign " + arrayDesign.getShortName() + " found in " + fileName );
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
