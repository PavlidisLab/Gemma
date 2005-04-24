package edu.columbia.gemma.web.util;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.ui.velocity.VelocityEngineUtils;

/**
 * From Appfuse.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class MailEngine {
    protected static final Log log = LogFactory.getLog( MailEngine.class );
    private MailSender mailSender;
    private VelocityEngine velocityEngine;

    /**
     * @param mailSender
     */
    public void setMailSender( MailSender mailSender ) {
        this.mailSender = mailSender;
    }

    /**
     * @param velocityEngine
     */
    public void setVelocityEngine( VelocityEngine velocityEngine ) {
        this.velocityEngine = velocityEngine;
    }

    /**
     * @param msg
     * @param templateName
     * @param model
     */
    public void sendMessage( SimpleMailMessage msg, String templateName, Map model ) {
        String result = null;

        try {
            result = VelocityEngineUtils.mergeTemplateIntoString( velocityEngine, templateName, model );
        } catch ( VelocityException e ) {
            e.printStackTrace();
        }

        msg.setText( result );
        send( msg );
    }

    /**
     * @param msg
     */
    public void send( SimpleMailMessage msg ) {
        try {
            mailSender.send( msg );
        } catch ( MailException ex ) {
            // log it and go on
            log.error( ex.getMessage() );
        }
    }
}
