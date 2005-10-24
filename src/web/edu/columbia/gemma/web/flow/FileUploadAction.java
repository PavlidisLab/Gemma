/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.web.flow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.DataBinder;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;
import org.springframework.webflow.Event;
import org.springframework.webflow.RequestContext;
import org.springframework.webflow.action.FormObjectAccessor;
import org.springframework.webflow.execution.servlet.ServletEvent;

import edu.columbia.gemma.web.Constants;
import edu.columbia.gemma.web.controller.common.auditAndSecurity.FileUpload;

/**
 * A flow version of the Spring MVC-based FileUploadController. Code taken partly from example.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @spring.bean name="uploadFileAction"
 * @author keshav
 * @author pavlidis
 * @version $Id$
 */
public class FileUploadAction extends AbstractFlowFormAction {

    private static Log log = LogFactory.getLog( FileUploadAction.class.getName() );

    public FileUploadAction() {
        setFormObjectName( "file" );
        setFormObjectClass( FileUpload.class );
    }

    /**
     * 
     */
    @SuppressWarnings("unused")
    @Override
    protected void initBinder( RequestContext context, DataBinder binder ) {
        /*
         * to actually be able to convert a multipart object to a byte[] we have to register a custom editor (in this
         * case the ByteArrayMultipartFileEditor)
         */
        binder.registerCustomEditor( byte[].class, new ByteArrayMultipartFileEditor() );
        /* now Spring knows how to handle multipart objects and convert them */
    }

    /**
     * @param context
     * @return Event
     * @throws Exception
     */
    @Override
    public Event doExecute( RequestContext context ) throws Exception {
        assert context != null;

        CommonsMultipartFile file = ( CommonsMultipartFile ) context.getSourceEvent().getAttribute( "file" );
        log.debug( file );

        String filename = ( String ) context.getSourceEvent().getAttribute( "name" );

        /* validate the file that was entered */
        if ( file.getBytes().length == 0 ) {
            // Errors errors = getFormObjectAccessor( context ).getFormErrors();
            // FIXME - errors are not getting displayed.
            Errors errors = this.getFormErrors( context );
            log.debug(errors);
            errors.reject( "file", "Must enter a file." );
            return error();
        }

        /* the directory to upload to.. */
        String contextPath = ( ( ServletEvent ) context.getSourceEvent() ).getRequest().getContextPath();

        String user = ( ( ServletEvent ) context.getSourceEvent() ).getRequest().getRemoteUser();

        String uploadDir = contextPath + "/resources/" + user + "/";
        log.warn( "Upload  path is " + uploadDir );

        /* Create the directory if it doesn't exist */
        File dirPath = new File( uploadDir );

        if ( !dirPath.exists() ) {
            boolean success = dirPath.mkdirs();
            if ( !success ) {
                return error( new IOException( "Could not make output directory " + dirPath ) );
            }
        }

        /* retrieve the file data */
        InputStream stream = file.getInputStream();

        try {
            /* write the file to the file specified */
            OutputStream bos = new FileOutputStream( uploadDir + file.getOriginalFilename() );
            int bytesRead = 0;
            byte[] buffer = new byte[8192];

            while ( ( bytesRead = stream.read( buffer, 0, 8192 ) ) != -1 ) {
                bos.write( buffer, 0, bytesRead );
            }

            bos.close();

            /* close the stream */
            stream.close();
        } catch ( IOException e ) {
            return error( e );
        }

        /* place the data in flow scope to be used by the next view state (uploadDisplay.jsp) */
        context.getFlowScope().setAttribute( "friendlyName", filename );
        context.getFlowScope().setAttribute( "fileName", file.getOriginalFilename() );
        context.getFlowScope().setAttribute( "contentType", file.getContentType() );
        context.getFlowScope().setAttribute( "size", file.getSize() + " bytes" );
        context.getFlowScope().setAttribute( "location",
                dirPath.getAbsolutePath() + Constants.FILE_SEP + file.getOriginalFilename() );

        String link = uploadDir;

        context.getFlowScope().setAttribute( "link", link + file.getOriginalFilename() );

        log.warn( "Uploaded file!" );
        addMessage( context, "display.title", new Object[] {} );
        return success();
    }

}
