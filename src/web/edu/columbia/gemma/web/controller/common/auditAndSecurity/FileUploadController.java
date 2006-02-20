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
package edu.columbia.gemma.web.controller.common.auditAndSecurity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import edu.columbia.gemma.web.Constants;
import edu.columbia.gemma.web.controller.BaseFormController;
import edu.columbia.gemma.web.util.upload.CommonsMultipartFile;

/**
 * Controller class to upload Files. This demonstrates the technique, but isn't all that useful as is.
 * 
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 * @author keshav
 * @version $Id$
 * @spring.bean id="fileUploadController" name="/selectFile.html /uploadFile.html"
 * @spring.property name="commandName" value="fileUpload"
 * @spring.property name="commandClass" value="edu.columbia.gemma.web.controller.common.auditAndSecurity.FileUpload"
 * @spring.property name="validator" ref="genericBeanValidator"
 * @spring.property name="formView" value="uploadForm"
 * @spring.property name="successView" value="uploadDisplay"
 */
public class FileUploadController extends BaseFormController {

    private static Log log = LogFactory.getLog( FileUploadController.class.getName() );

    /**
     * 
     */
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {
        FileUpload fileUpload = ( FileUpload ) command;

        // validate a file was entered
        if ( fileUpload.getFile().length == 0 ) {
            Object[] args = new Object[] { getText( "uploadForm.file", request.getLocale() ) };
            errors.rejectValue( "file", "errors.required", args, "File" );

            return showForm( request, response, errors );
        }

        MultipartHttpServletRequest multipartRequest = ( MultipartHttpServletRequest ) request;
        CommonsMultipartFile file = ( CommonsMultipartFile ) multipartRequest.getFile( "file" );

        // the directory to upload to
        String uploadDir = getServletContext().getRealPath( "/resources" ) + "/" + request.getRemoteUser() + "/";

        // Create the directory if it doesn't exist
        File dirPath = new File( uploadDir );

        if ( !dirPath.exists() ) {
            dirPath.mkdirs();
        }

        // retrieve the file data
        InputStream stream = file.getInputStream();

        // write the file to the file specified
        OutputStream bos = new FileOutputStream( uploadDir + file.getOriginalFilename() );
        int bytesRead = 0;
        byte[] buffer = new byte[8192];

        while ( ( bytesRead = stream.read( buffer, 0, 8192 ) ) != -1 ) {
            bos.write( buffer, 0, bytesRead );
        }

        bos.close();

        // close the stream
        stream.close();

        // place the data into the request for retrieval on next page
        request.setAttribute( "friendlyName", fileUpload.getName() );
        request.setAttribute( "fileName", file.getOriginalFilename() );
        request.setAttribute( "contentType", file.getContentType() );
        request.setAttribute( "size", file.getSize() + " bytes" );
        request.setAttribute( "location", dirPath.getAbsolutePath() + Constants.FILE_SEP + file.getOriginalFilename() );

        String link = request.getContextPath() + "/resources" + "/" + request.getRemoteUser() + "/";

        request.setAttribute( "link", link + file.getOriginalFilename() );

        log.info( "Uploaded file!" );

        return new ModelAndView( getSuccessView() );
    }

    /**
     * 
     */
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {
        if ( request.getParameter( "cancel" ) != null ) {
            return new ModelAndView( new RedirectView( "mainMenu.html" ) );
        }

        return super.processFormSubmission( request, response, command, errors );
    }
}
