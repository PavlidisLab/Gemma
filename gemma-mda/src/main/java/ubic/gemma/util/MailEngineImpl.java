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
package ubic.gemma.util;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;
import org.springframework.ui.velocity.VelocityEngineUtils;


/**
 * @author pavlidis
 * @author Matt Raible
 * @version $Id$
 */
@Component
public class MailEngineImpl implements MailEngine {
    protected static final Log log = LogFactory.getLog( MailEngineImpl.class );

    @Autowired
    private MailSender mailSender;

    @Autowired
    private VelocityEngine velocityEngine;

    /**
     * @param msg
     * @param templateName
     * @param model
     */
    @Override
    public void sendMessage( SimpleMailMessage msg, String templateName, Map<String, Object> model ) {
        String result = null;

        try {
            result = VelocityEngineUtils.mergeTemplateIntoString( velocityEngine, templateName,
                    RuntimeConstants.ENCODING_DEFAULT, model );
        } catch ( VelocityException e ) {
            e.printStackTrace();
        }

        msg.setText( result );
        send( msg );
    }

    /**
     * @param msg
     */
    @Override
    public void send( SimpleMailMessage msg ) {
        try {
            mailSender.send( msg );
        } catch ( MailException ex ) {
            // log it and go on
            log.error( ex.getMessage() );
            log.debug( ex, ex );
        }
    }

    /**
     * Sends a message to the gemma administrator as defined in the Gemma.properties file
     * 
     * @param bodyText
     * @param subject
     */
    @Override
    public void sendAdminMessage( String bodyText, String subject ) {

        if ( ( bodyText == null ) && ( subject == null ) ) {
            log.warn( "Not sending empty email, both subject and body are null" );
            return;
        }

        log.info( "Sending email notification to administrator regarding: " + subject );
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo( Settings.getAdminEmailAddress() );
        msg.setFrom( Settings.getAdminEmailAddress() );
        msg.setSubject( subject );
        msg.setText( bodyText );
        this.send( msg );
    }
}
