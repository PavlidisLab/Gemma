/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.web.controller.common.auditAndSecurity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.util.upload.FileUploadUtil;

/**
 * Controller class to upload Files. This demonstrates the technique, but isn't all that useful as is.
 * 
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 * @author keshav
 * @version $Id$
 * @spring.bean id="fileUploadController"
 * @spring.property name="commandName" value="fileUpload"
 * @spring.property name="commandClass" value="ubic.gemma.web.controller.common.auditAndSecurity.FileUpload"
 * @spring.property name="validator" ref="fileUploadValidator"
 * @spring.property name="formView" value="uploadForm"
 * @spring.property name="successView" value="uploadDisplay"
 */
public class FileUploadController extends BaseFormController {

    private static Log log = LogFactory.getLog( FileUploadController.class.getName() );

    /**
     * 
     */
    @Override
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        // When this is reached, the file has already been uploaded.

        FileUpload fileUpload = ( FileUpload ) command;

        // validate a file was entered
        if ( fileUpload.getFile().length == 0 ) {
            Object[] args = new Object[] { getText( "uploadForm.file", request.getLocale() ) };
            errors.rejectValue( "file", "errors.required", args, "File" );
            response.getWriter().write( "{success : false, error : 'File is required'}" );
            return null;
        }
        File copiedFile = null;
        try {
            copiedFile = FileUploadUtil.copyUploadedFile( request, fileUpload, "file" );
            log.info( "Uploaded file!" );
        } catch ( Exception e ) {
            response.getWriter().write( "{success : false, error: '" + e.getMessage() + "' }" );
            return null;
        }

        if ( copiedFile == null ) {
            response.getWriter().write( "{success : false, error : 'unknown problem getting file' }" );
            return null;
        }

        response.getWriter().write( "{success : true, localFile : '" + copiedFile.getAbsolutePath() + "'}" );
        response.getWriter().flush();
        return null;
    }

    /**
     * Ajax. DWR can handle this.
     * 
     * @throws IOException
     * @throws FileNotFoundException
     */
    public String upload( InputStream is ) throws FileNotFoundException, IOException {
        File copiedFile = FileUploadUtil.copyUploadedInputStream( is );
        log.info( "Uploaded file!" );
        return copiedFile.getAbsolutePath();
    }

    /**
     * 
     */
    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {
        if ( request.getParameter( "cancel" ) != null ) {
            return new ModelAndView( new RedirectView( "mainMenu.html" ) );
        }

        return super.processFormSubmission( request, response, command, errors );
    }
}
