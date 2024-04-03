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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.mail.SimpleMailMessage;
import ubic.gemma.persistence.util.MailEngine;
import ubic.gemma.persistence.util.Settings;
import ubic.gemma.web.util.MessageUtil;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;

/**
 * Extend this to create a simple Single or MultiActionController; includes configuration for sending email and setting
 * messages in the session. Use the \@Controller and \@RequestMapping annotations to configure subclasses.
 *
 * @author keshav
 */
public abstract class BaseController {

    protected Log log = LogFactory.getLog( getClass().getName() );

    @Autowired
    private MailEngine mailEngine = null;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private MessageUtil messageUtil;

    @Autowired
    private ServletContext servletContext;

    public void setMailEngine( MailEngine mailEngine ) {
        this.mailEngine = mailEngine;
    }

    public void setMessageSource( MessageSource messageSource ) {
        this.messageSource = messageSource;
    }

    /**
     * @param messageCode - if no message is found, this is used to form the message (instead of throwing an exception).
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
     * @param messageUtil the messageUtil to set
     */
    public void setMessageUtil( MessageUtil messageUtil ) {
        this.messageUtil = messageUtil;
    }

    /**
     * @see ubic.gemma.web.util.MessageUtilImpl#getText(java.lang.String, java.util.Locale)
     */
    protected String getText( String msgKey, Locale locale ) {
        return this.messageUtil.getText( msgKey, locale );
    }

    protected String getText( String msgKey, Object[] args, Locale locale ) {
        return this.messageUtil.getText( msgKey, args, locale );
    }

    protected void saveMessage( HttpServletRequest request, String msg ) {
        this.messageUtil.saveMessage( request, msg );
    }

    protected void saveMessage( HttpServletRequest request, String key, Object parameter, String defaultMessage ) {
        this.messageUtil.saveMessage( request, key, parameter, defaultMessage );
    }

    protected void sendConfirmationEmail( HttpServletRequest request, String token, String username, String email,
            Map<String, Object> model, String templateName ) {
        try {
            model.put( "username", username );
            model.put( "confirmLink",
                    Settings.getHostUrl() + servletContext.getContextPath() + "/confirmRegistration.html?key=" + token + "&username=" + username );

            mailEngine.sendMessage( username + "<" + email + ">", getText( "signup.email.subject", request.getLocale() ), templateName, model );

        } catch ( Exception e ) {
            log.error( "Couldn't send email to " + email, e );
        }
    }

}
