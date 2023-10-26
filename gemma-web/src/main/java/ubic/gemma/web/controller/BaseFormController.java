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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.persistence.util.MailEngine;
import ubic.gemma.web.util.MessageUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

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
    private MessageUtil messageUtil;

    @Autowired
    private MailEngine mailEngine;

    /**
     * @return the messageUtil
     */
    public MessageUtil getMessageUtil() {
        return this.messageUtil;
    }

    /**
     * @param messageUtil the messageUtil to set
     */
    public void setMessageUtil( MessageUtil messageUtil ) {
        this.messageUtil = messageUtil;
    }

    /**
     * @see ubic.gemma.web.util.MessageUtilImpl#getText(java.lang.String, java.util.Locale)
     */
    public String getText( String msgKey, Locale locale ) {
        return this.messageUtil.getText( msgKey, locale );
    }

    /**
     * @see ubic.gemma.web.util.MessageUtilImpl#saveMessage(javax.servlet.http.HttpServletRequest, java.lang.String)
     */
    public void saveMessage( HttpServletRequest request, String msg ) {
        this.messageUtil.saveMessage( request, msg );
    }

    /**
     * @see ubic.gemma.web.util.MessageUtilImpl#saveMessage(javax.servlet.http.HttpServletRequest, java.lang.String, java.lang.Object, java.lang.String)
     */
    public void saveMessage( HttpServletRequest request, String key, Object parameter, String defaultMessage ) {
        this.messageUtil.saveMessage( request, key, parameter, defaultMessage );
    }

    /**
     * @see ubic.gemma.web.util.MessageUtilImpl#saveMessage(javax.servlet.http.HttpServletRequest, java.lang.String, java.lang.Object[], java.lang.String)
     */
    public void saveMessage( HttpServletRequest request, String key, Object[] parameters, String defaultMessage ) {
        this.messageUtil.saveMessage( request, key, parameters, defaultMessage );
    }

    /**
     * @see ubic.gemma.web.util.MessageUtilImpl#saveMessage(javax.servlet.http.HttpServletRequest, java.lang.String, java.lang.String)
     */
    public void saveMessage( HttpServletRequest request, String key, String defaultMessage ) {
        this.messageUtil.saveMessage( request, key, defaultMessage );
    }

    /**
     * @see ubic.gemma.web.util.MessageUtilImpl#saveMessage(javax.servlet.http.HttpSession, java.lang.String)
     */
    public void saveMessage( HttpSession session, String msg ) {
        this.messageUtil.saveMessage( session, msg );
    }

    public void setMailEngine( MailEngine mailEngine ) {
        this.mailEngine = mailEngine;
    }

    /**
     * Override this to control which cancelView is used. The default behavior is to go to the success view if there is
     * no cancel view defined; otherwise, get the cancel view.
     *
     * @param request can be used to control which cancel view to use. (This is not used in the default implementation)
     * @return the view to use.
     */
    protected ModelAndView getCancelView( HttpServletRequest request ) {
        return new ModelAndView( WebConstants.HOME_PAGE );
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
            messageUtil.saveMessage( request, "errors.cancel", "Cancelled" );
            return getCancelView( request );
        }

        return super.processFormSubmission( request, response, command, errors );
    }

    /**
     * Convenience message to send messages to users
     */
    protected void sendEmail( User user, String msg ) {
        if ( StringUtils.isBlank( user.getEmail() ) ) {
            log.warn( "Could not send email to " + user + ", no email address" );
        }
        log.debug( "sending e-mail to user [" + user.getEmail() + "]..." );
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo( user.getFullName() + "<" + user.getEmail() + ">" );

        mailEngine.send( message );
    }

    /**
     * Convenience message to send messages to users
     */
    protected void sendEmail( User user, String templateName, Map<String, Object> model ) {
        if ( StringUtils.isBlank( user.getEmail() ) ) {
            log.warn( "Could not send email to " + user + ", no email address" );
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo( user.getFullName() + "<" + user.getEmail() + ">" );
        mailEngine.sendMessage( message, templateName, model );
    }

}