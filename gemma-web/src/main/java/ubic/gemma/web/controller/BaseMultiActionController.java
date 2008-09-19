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

import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.NoSuchMessageException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserService;
import ubic.gemma.util.MailEngine;
import ubic.gemma.web.util.MessageUtil;

/**
 * Extend this to create a MultiActionController.
 * 
 * @author keshav
 * @version $Id$
 * @spring.property name="messageUtil" ref="messageUtil"
 */
public abstract class BaseMultiActionController extends MultiActionController {

    protected Log log = LogFactory.getLog( getClass().getName() );
    protected MailEngine mailEngine = null;
    protected SimpleMailMessage mailMessage = null;
    protected String templateName = null;
    protected UserService userService = null;

    private MessageUtil messageUtil;

    /**
     * @param request
     * @param messageCode - if no message is found, this is used to form the message (instead of throwing an exception).
     * @param parameters
     */
    protected void addMessage( HttpServletRequest request, String messageCode, Object[] parameters ) {
        try {
            request.getSession().setAttribute( "messages",
                    getMessageSourceAccessor().getMessage( messageCode, parameters ) );
        } catch ( NoSuchMessageException e ) {
            request.getSession().setAttribute( "messages", "??" + messageCode + "??" );
        }
    }

    protected void saveMessage( HttpServletRequest request, String msg ) {
        this.messageUtil.saveMessage( request, msg );
    }

    protected void saveMessage( HttpServletRequest request, String key, Object parameter, String defaultMessage ) {
        this.messageUtil.saveMessage( request, key, parameter, defaultMessage );
    }

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
     * @param templateName
     */
    public void setTemplateName( String templateName ) {
        this.templateName = templateName;
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

    /**
     * @param user
     * @param templateName
     * @param model
     */
    protected void sendEmail( User user, String templateName, Map model ) {
        if ( StringUtils.isBlank( user.getEmail() ) ) {
            log.warn( "Could not send email to " + user + ", no email address" );
        }
        mailMessage.setTo( user.getFullName() + "<" + user.getEmail() + ">" );
        mailEngine.sendMessage( mailMessage, templateName, model );
    }

    public void setUserService( UserService userService ) {
        this.userService = userService;
    }
}
