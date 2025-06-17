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
package ubic.gemma.web.controller;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import ubic.gemma.web.util.MessageUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.NumberFormat;

/**
 * Implementation of <strong>SimpleFormController</strong> that contains convenience methods for subclasses. For
 * example, getting the current user and saving messages/errors. This class is intended to be a base class for all Form
 * controllers.
 * @deprecated {@link SimpleFormController} is deprecated, use annotations-based GET/POST mapping instead.
 *
 * @author pavlidis (originally based on Appfuse code)
 */
@Deprecated
public abstract class BaseFormController extends SimpleFormController {
    protected static final Log log = LogFactory.getLog( BaseFormController.class.getName() );

    @Autowired
    protected MessageUtil messageUtil;

    /**
     * Override this to control which cancelView is used. The default behavior is to go to the success view if there is
     * no cancel view defined; otherwise, get the cancel view.
     *
     * @param request can be used to control which cancel view to use. (This is not used in the default implementation)
     * @return the view to use.
     */
    protected ModelAndView getCancelView( HttpServletRequest request ) {
        return new ModelAndView( "/home.html" );
    }

    /**
     * Set up a custom property editor for converting form inputs to real objects. Override this to add additional
     * custom editors (call super.initBinder() in your implementation)
     */
    @InitBinder
    protected void initBinder( WebDataBinder binder ) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        binder.registerCustomEditor( Integer.class, null, new CustomNumberEditor( Integer.class, nf, true ) );
        binder.registerCustomEditor( Long.class, null, new CustomNumberEditor( Long.class, nf, true ) );
        binder.registerCustomEditor( byte[].class, new ByteArrayMultipartFileEditor() );
    }

    /**
     * Convenience method to get the user object from the session
     *
     * @param request the current request
     * @return the user's populated object from the session
     */

    protected ModelAndView processErrors( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors, String message ) throws Exception {
        if ( !StringUtils.isEmpty( message ) ) {
            log.error( message );
            if ( command == null ) {
                errors.addError( new ObjectError( "nullCommand", null, null, message ) );
            } else {
                errors.addError( new ObjectError( command.toString(), null, null, message ) );
            }
        }

        return this.processFormSubmission( request, response, command, errors );
    }

    /**
     * Default behavior for FormControllers - redirect to the successView when the cancel button has been pressed.
     */
    @Override
    protected ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {
        if ( request.getParameter( "cancel" ) != null ) {
            messageUtil.saveMessage( "errors.cancel", "Cancelled" );
            return getCancelView( request );
        }

        return super.processFormSubmission( request, response, command, errors );
    }
}