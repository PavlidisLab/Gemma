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
package edu.columbia.gemma.web.controller.flow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.DataBinder;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;
import org.springframework.webflow.Event;
import org.springframework.webflow.RequestContext;
import org.springframework.webflow.action.FormAction;
import org.springframework.webflow.execution.servlet.ServletEvent;

import edu.columbia.gemma.web.controller.common.auditAndSecurity.FileUpload;

/**
 * A flow version of the Spring MVC-based FileUploadController. Code taken partly from example.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class FileUploadAction extends FormAction {

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
        // to actually be able to convert a multipart object to a byte[]
        // we have to register a custom editor (in this case the
        // ByteArrayMultipartFileEditor)
        binder.registerCustomEditor( byte[].class, new ByteArrayMultipartFileEditor() );
        // now Spring knows how to handle multipart objects and convert them
    }

    /**
     * @param context
     * @return
     * @throws Exception
     */
    @Override
    public Event doExecute( RequestContext context ) throws Exception {
        assert context != null;

        FileUpload fub = ( FileUpload ) context.getSourceEvent().getAttribute( "file" ); //  org.springframework.web.multipart.commons.CommonsMultipartFile
        if ( fub == null ) {
            return error( new IOException( "FileUpload parameter was null" ) );
        }
        byte[] fileData = fub.getFile();

        if ( fileData == null || fileData.length == 0 ) {
            return error( new IOException( "File data was null or empty" ) );
        }

        // the directory to upload to.. .I don't know what this does.
        String uploadDir = ( ( ServletEvent ) context.getSourceEvent() ).getRequest().getContextPath();
        String user = ( ( ServletEvent ) context.getSourceEvent() ).getRequest().getRemoteUser();

        String uploadedFile = uploadDir + "/resources/" + user + "/";
        log.warn( "Upload  path is " + uploadedFile );

        // Create the directory if it doesn't exist
        File dirPath = new File( uploadDir );

        if ( !dirPath.exists() ) {
            boolean success = dirPath.mkdirs();
            if ( !success ) {
                return error( new IOException( "Could not make output directory " + dirPath ) );
            }
        }

        try {
            OutputStream bos = new FileOutputStream( uploadedFile );
            bos.write( fileData, 0, fileData.length );
            bos.close();
        } catch ( IOException e ) {
            return error( e );
        }
        context.getFlowScope().setAttribute( "readFile", uploadedFile );
        return success();
    }

}
