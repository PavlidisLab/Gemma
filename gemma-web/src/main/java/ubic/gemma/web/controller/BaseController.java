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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.mail.SimpleMailMessage;

import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.MailEngineImpl;
import ubic.gemma.web.util.MessageUtil;

/**
 * Extend this to create a simple Single or MultiActionController; includes configuration for sending email and setting
 * messages in the session. Use the \@Controller and \@RequestMapping annotations to configure subclasses.
 * 
 * @author keshav
 * @version $Id$
 */
public abstract class BaseController {

    protected Log log = LogFactory.getLog( getClass().getName() );

    @Autowired
    protected MailEngineImpl mailEngine = null;

    @Autowired
    protected MessageSource messageSource;

    @Autowired
    private MessageUtil messageUtil;

    /**
     * @param mailEngine
     */
    public void setMailEngine( MailEngineImpl mailEngine ) {
        this.mailEngine = mailEngine;
    }

    public void setMessageSource( MessageSource messageSource ) {
        this.messageSource = messageSource;
    }

    /**
     * @param messageUtil the messageUtil to set
     */
    public void setMessageUtil( MessageUtil messageUtil ) {
        this.messageUtil = messageUtil;
    }

    /**
     * @param request
     * @param messageCode - if no message is found, this is used to form the message (instead of throwing an exception).
     * @param parameters
     */
    protected void addMessage( HttpServletRequest request, String messageCode, Object[] parameters ) {
        try {
            request.getSession().setAttribute( "messages",
                    messageSource.getMessage( messageCode, parameters, request.getLocale() ) );
        } catch ( NoSuchMessageException e ) {
            request.getSession().setAttribute( "messages", "??" + messageCode + "??" );
        }
    }

    /**
     * @return the messageUtil
     */
    protected MessageUtil getMessageUtil() {
        return this.messageUtil;
    }

    /**
     * @param msgKey
     * @param locale
     * @return
     * @see ubic.gemma.web.util.MessageUtilImpl#getText(java.lang.String, java.util.Locale)
     */
    protected String getText( String msgKey, Locale locale ) {
        return this.messageUtil.getText( msgKey, locale );
    }

    /**
     * @param msgKey
     * @param args
     * @param locale
     * @return
     */
    protected String getText( String msgKey, Object[] args, Locale locale ) {
        return this.messageUtil.getText( msgKey, args, locale );
    }

    /**
     * @param request
     * @param msg
     */
    protected void saveMessage( HttpServletRequest request, String msg ) {
        this.messageUtil.saveMessage( request, msg );
    }

    /**
     * @param request
     * @param key
     * @param parameter
     * @param defaultMessage
     */
    protected void saveMessage( HttpServletRequest request, String key, Object parameter, String defaultMessage ) {
        this.messageUtil.saveMessage( request, key, parameter, defaultMessage );
    }

    /**
     * @param name
     * @param subject
     * @param emailAddress
     * @param templateName
     * @param model
     */
    protected void sendEmail( String name, String emailAddress, String subject, String templateName,
            Map<String, Object> model ) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom( ConfigUtils.getAdminEmailAddress() );
        mailMessage.setSubject( subject );
        mailMessage.setTo( name + "<" + emailAddress + ">" );
        mailEngine.sendMessage( mailMessage, templateName, model );
    }

}
