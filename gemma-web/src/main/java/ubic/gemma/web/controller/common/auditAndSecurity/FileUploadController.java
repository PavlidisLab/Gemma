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
 * @spring.bean id="fileUploadController" name="/selectFile.html /uploadFile.html"
 * @spring.property name="commandName" value="fileUpload"
 * @spring.property name="commandClass" value="ubic.gemma.web.controller.common.auditAndSecurity.FileUpload"
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

        FileUploadUtil.uploadFile( request, fileUpload, "file" );
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
