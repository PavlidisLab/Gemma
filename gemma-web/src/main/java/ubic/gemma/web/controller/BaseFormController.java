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

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import ubic.gemma.Constants;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserService;
import ubic.gemma.util.MailEngine;
import ubic.gemma.web.util.MessageUtil;

/**
 * Implementation of <strong>SimpleFormController</strong> that contains convenience methods for subclasses. For
 * example, getting the current user and saving messages/errors. This class is intended to be a base class for all Form
 * controllers.
 * 
 * @author pavlidis
 * @author Matt Raible (original version)
 * @version $Id$
 * @spring.property name="messageUtil" ref="messageUtil"
 */
public abstract class BaseFormController extends SimpleFormController {
    protected static Log log = LogFactory.getLog( BaseFormController.class.getName() );
    private MessageUtil messageUtil;
    protected MailEngine mailEngine = null;
    protected SimpleMailMessage mailMessage = null;
    protected String templateName = null;

    protected UserService userService = null;

    /**
     * Define a cancel view to use.
     */
    protected String cancelView;

    /**
     * 
     *
     */
    public BaseFormController() {
        super();
    }

    /**
     * Convenience method to get the Configuration HashMap from the servlet context.
     * 
     * @return the user's populated form from the session
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getConfiguration() {
        Map<String, Object> config = ( Map<String, Object> ) getServletContext().getAttribute( Constants.CONFIG );

        // so unit tests don't puke when nothing's been set
        if ( config == null ) {
            return new HashMap<String, Object>();
        }
        return config;
    }

    /**
     * @param msgKey
     * @param locale
     * @return
     * @see ubic.gemma.web.util.MessageUtil#getText(java.lang.String, java.util.Locale)
     */
    public String getText( String msgKey, Locale locale ) {
        return this.messageUtil.getText( msgKey, locale );
    }

    public UserService getUserService() {
        return this.userService;
    }

    /**
     * @param request
     * @param msg
     * @see ubic.gemma.web.util.MessageUtil#saveMessage(javax.servlet.http.HttpServletRequest, java.lang.String)
     */
    public void saveMessage( HttpServletRequest request, String msg ) {
        this.messageUtil.saveMessage( request, msg );
    }

    /**
     * @param request
     * @param key
     * @param parameter
     * @param defaultMessage
     * @see ubic.gemma.web.util.MessageUtil#saveMessage(javax.servlet.http.HttpServletRequest, java.lang.String,
     *      java.lang.Object, java.lang.String)
     */
    public void saveMessage( HttpServletRequest request, String key, Object parameter, String defaultMessage ) {
        this.messageUtil.saveMessage( request, key, parameter, defaultMessage );
    }

    /**
     * @param request
     * @param key
     * @param parameters
     * @param defaultMessage
     * @see ubic.gemma.web.util.MessageUtil#saveMessage(javax.servlet.http.HttpServletRequest, java.lang.String,
     *      java.lang.Object[], java.lang.String)
     */
    public void saveMessage( HttpServletRequest request, String key, Object[] parameters, String defaultMessage ) {
        this.messageUtil.saveMessage( request, key, parameters, defaultMessage );
    }

    /**
     * @param request
     * @param key
     * @param defaultMessage
     * @see ubic.gemma.web.util.MessageUtil#saveMessage(javax.servlet.http.HttpServletRequest, java.lang.String,
     *      java.lang.String)
     */
    public void saveMessage( HttpServletRequest request, String key, String defaultMessage ) {
        this.messageUtil.saveMessage( request, key, defaultMessage );
    }

    /**
     * @param session
     * @param msg
     * @see ubic.gemma.web.util.MessageUtil#saveMessage(javax.servlet.http.HttpSession, java.lang.String)
     */
    public void saveMessage( HttpSession session, String msg ) {
        this.messageUtil.saveMessage( session, msg );
    }

    /**
     * @param cancelView the cancelView to set
     */
    public void setCancelView( String cancelView ) {
        this.cancelView = cancelView;
    }

    /**
     * @param mailEngine
     */
    public void setMailEngine( MailEngine mailEngine ) {
        this.mailEngine = mailEngine;
    }

    /**
     * @param message
     */
    public void setMailMessage( SimpleMailMessage message ) {
        this.mailMessage = message;
    }

    /**
     * @param messageUtil the messageUtil to set
     */
    public void setMessageUtil( MessageUtil messageUtil ) {
        this.messageUtil = messageUtil;
    }

    /**
     * @param templateName
     */
    public void setTemplateName( String templateName ) {
        this.templateName = templateName;
    }

    /**
     * @param userService
     */
    public void setUserService( UserService userService ) {
        this.userService = userService;
    }

    /**
     * Override this to control which cancelView is used. The default behavior is to go to the success view if there is
     * no cancel view defined; otherwise, get the cancel view.
     * 
     * @param request can be used to control which cancel view to use. (This is not used in the default implementation)
     * @return the name of the cancel view to use.
     */
    @SuppressWarnings("unused")
    protected String getCancelViewName( HttpServletRequest request ) {
        // Default to successView if cancelView is not defined
        if ( StringUtils.isBlank( cancelView ) ) {
            return getSuccessView();
        }
        return this.cancelView;
    }

    /**
     * Convenience method to get the user object from the session
     * 
     * @param request the current request
     * @return the user's populated object from the session
     */
    protected User getUser( HttpServletRequest request ) {
        return ( User ) request.getSession().getAttribute( Constants.USER_KEY );
    }

    /**
     * Set up a custom property editor for converting form inputs to real objects. Override this to add additional
     * custom editors (call super.initBinder() in your implemenation)
     */
    @Override
    @SuppressWarnings("unused")
    protected void initBinder( HttpServletRequest request, ServletRequestDataBinder binder ) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        binder.registerCustomEditor( Integer.class, null, new CustomNumberEditor( Integer.class, nf, true ) );
        binder.registerCustomEditor( Long.class, null, new CustomNumberEditor( Long.class, nf, true ) );
        binder.registerCustomEditor( byte[].class, new ByteArrayMultipartFileEditor() );
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
     * Override this to control which cancelView is used. The default behavior is to go to the success view if there is
     * no cancel view defined; otherwise, get the cancel view.
     * 
     * @param request can be used to control which cancel view to use. (This is not used in the default implementation)
     * @return the view to use.
     */
    protected ModelAndView getCancelView( HttpServletRequest request ) {
        return new ModelAndView( getCancelViewName( request ) );
    }

    /**
     * New errors are added if <tt>message</tt> is not empty (as per the definition of
     * {@link org.apache.commons.lang.StringUtils#isEmpty}. If empty, a new error will not be added, but existing
     * errors will still be processed.
     * 
     * @param request
     * @param response
     * @param command
     * @param errors
     * @param message - The error message to be displayed.
     * @return ModelAndView
     * @throws Exception
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
     * Convenience message to send messages to users, includes app URL as footer.
     * 
     * @param user
     * @param msg
     * @param url
     */
    protected void sendEmail( User user, String msg, String url ) {
        log.debug( "sending e-mail to user [" + user.getEmail() + "]..." );
        mailMessage.setTo( user.getFullName() + "<" + user.getEmail() + ">" );

        Map<String, Object> model = new HashMap<String, Object>();
        model.put( "user", user );
        model.put( "message", msg );
        model.put( "applicationURL", url );
        mailEngine.sendMessage( mailMessage, templateName, model );
    }

    /**
     * @return the messageUtil
     */
    public MessageUtil getMessageUtil() {
        return this.messageUtil;
    }

}