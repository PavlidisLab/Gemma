package edu.columbia.gemma.web.controller.common.auditAndSecurity;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.view.RedirectView;

import edu.columbia.gemma.common.auditAndSecurity.User;
import edu.columbia.gemma.common.auditAndSecurity.UserService;
import edu.columbia.gemma.util.RequestUtil;
import edu.columbia.gemma.web.util.MailEngine;

/**
 * Simple class to retrieve and send a password hint to users by email.
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @spring.bean id="passwordHintController" name="/passwordHint.html"
 * @spring.property name="userService" ref="userService"
 * @spring.property name="mailEngine" ref="mailEngine"
 * @spring.property name="message" ref="mailMessage"
 * @spring.property name="messageSource" ref="messageSource"
 */
public class PasswordHintController implements Controller {
    private transient final Log log = LogFactory.getLog( PasswordHintController.class );
    private MessageSource messageSource = null;
    private UserService userService = null;
    protected MailEngine mailEngine = null;
    protected SimpleMailMessage message = null;

    @SuppressWarnings("unused")
    public ModelAndView handleRequest( HttpServletRequest request, HttpServletResponse response ) throws Exception {
        if ( log.isDebugEnabled() ) {
            log.debug( "entering 'handleRequest' method..." );
        }
        String username = request.getParameter( "username" );
        MessageSourceAccessor text = new MessageSourceAccessor( messageSource, request.getLocale() );

        // ensure that the username has been sent
        if ( username == null ) {
            log.warn( "Username not specified, notifying user that it's a required field." );

            request.setAttribute( "error", text.getMessage( "errors.required", new Object[] { text
                    .getMessage( "user.username" ) } ) );

            return new ModelAndView( "login" );
        }

        if ( log.isDebugEnabled() ) {
            log.debug( "Processing Password Hint..." );
        }

        // look up the user's information
        try {
            User user = userService.getUser( username );

            StringBuffer msg = new StringBuffer();
            msg.append( "Your password hint is: " + user.getPasswordHint() );
            msg.append( "\n\nLogin at: " + RequestUtil.getAppURL( request ) );

            message.setTo( user.getEmail() );
            String subject = text.getMessage( "webapp.prefix" ) + text.getMessage( "user.passwordHint" );
            message.setSubject( subject );
            message.setText( msg.toString() );
            mailEngine.send( message );

            saveMessage( request, text.getMessage( "login.passwordHint.sent",
                    new Object[] { username, user.getEmail() } ) );
        } catch ( Exception e ) {
            saveError( request, text.getMessage( "login.passwordHint.error", new Object[] { username } ) );
        }

        return new ModelAndView( new RedirectView( request.getContextPath() ) );
    }

    @SuppressWarnings("unchecked")
    public void saveError( HttpServletRequest request, String error ) {
        List<Object> errors = ( List<Object> ) request.getSession().getAttribute( "errors" );
        if ( errors == null ) {
            errors = new ArrayList<Object>();
        }
        errors.add( error );
        request.getSession().setAttribute( "errors", errors );
    }

    // this method is also in BaseForm Controller
    @SuppressWarnings("unchecked")
    public void saveMessage( HttpServletRequest request, String msg ) {
        List<Object> messages = ( List<Object> ) request.getSession().getAttribute( "messages" );
        if ( messages == null ) {
            messages = new ArrayList<Object>();
        }
        messages.add( msg );
        request.getSession().setAttribute( "messages", messages );
    }

    public void setMailEngine( MailEngine mailEngine ) {
        this.mailEngine = mailEngine;
    }

    public void setMessage( SimpleMailMessage message ) {
        this.message = message;
    }

    public void setMessageSource( MessageSource messageSource ) {
        this.messageSource = messageSource;
    }

    public void setUserService( UserService userService ) {
        this.userService = userService;
    }
}
