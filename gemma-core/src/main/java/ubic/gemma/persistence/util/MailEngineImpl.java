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
package ubic.gemma.persistence.util;

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

import java.util.Map;

/**
 * @author pavlidis
 * @author Matt Raible
 */
@Component
public class MailEngineImpl implements MailEngine {
    private static final Log log = LogFactory.getLog( MailEngineImpl.class );

    @Autowired
    private MailSender mailSender;

    @Autowired
    private VelocityEngine velocityEngine;

    /**
     * Sends a message to the gemma administrator as defined in the Gemma.properties file
     */
    @Override
    public void sendAdminMessage( String bodyText, String subject ) {

        if ( ( bodyText == null ) && ( subject == null ) ) {
            MailEngineImpl.log.warn( "Not sending empty email, both subject and body are null" );
            return;
        }

        MailEngineImpl.log.info( "Sending email notification to administrator regarding: " + subject );
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo( Settings.getAdminEmailAddress() );
        msg.setFrom( Settings.getAdminEmailAddress() );
        msg.setSubject( subject );
        msg.setText( bodyText );
        this.send( msg );
    }

    @Override
    public void send( SimpleMailMessage msg ) {
        try {
            mailSender.send( msg );
        } catch ( MailException ex ) {
            // log it and go on
            MailEngineImpl.log.error( ex.getMessage(), ex );
            MailEngineImpl.log.debug( ex, ex );
        }
    }

    @Override
    public void sendMessage( SimpleMailMessage msg, String templateName, Map<String, Object> model ) {
        String result = null;

        try {
            result = VelocityEngineUtils
                    .mergeTemplateIntoString( velocityEngine, templateName, RuntimeConstants.ENCODING_DEFAULT, model );
        } catch ( VelocityException e ) {
            e.printStackTrace();
        }

        msg.setText( result );
        this.send( msg );
    }
}
