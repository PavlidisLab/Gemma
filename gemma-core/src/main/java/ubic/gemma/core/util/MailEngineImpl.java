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
package ubic.gemma.core.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;
import org.springframework.ui.velocity.VelocityEngineUtils;

import java.util.Arrays;
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

    @Value("${gemma.noreply.email}")
    private String noreplyEmailAddress;

    @Value("${gemma.admin.email}")
    private String adminEmailAddress;

    @Value("${gemma.support.email}")
    private String supportEmailAddress;

    @Override
    public String getAdminEmailAddress() {
        return adminEmailAddress;
    }

    /**
     * Sends a message to the gemma administrator as defined in the Gemma.properties file
     */
    @Override
    public void sendAdminMessage( String subject, String bodyText ) {
        if ( StringUtils.isBlank( adminEmailAddress ) ) {
            MailEngineImpl.log.warn( "Not sending email, no admin email is configured." );
            return;
        }
        MailEngineImpl.log.info( "Sending email notification to administrator regarding: " + subject );
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom( noreplyEmailAddress );
        msg.setTo( adminEmailAddress );
        // no need to set the reply to support, it's meant for a Gemma admin
        msg.setSubject( subject );
        msg.setText( bodyText );
        send( msg );
        MailEngineImpl.log.info( "Email notification sent to " + Arrays.toString( msg.getTo() ) );
    }

    @Override
    public void sendMessage( String to, String subject, String body ) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo( to );
        msg.setFrom( noreplyEmailAddress );
        msg.setReplyTo( supportEmailAddress );
        msg.setSubject( subject );
        msg.setText( body );
        send( msg );
    }

    @Override
    public void sendMessage( String to, String subject, String templateName, Map<String, Object> model ) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo( to );
        msg.setFrom( noreplyEmailAddress );
        msg.setReplyTo( supportEmailAddress );
        msg.setSubject( subject );
        try {
            msg.setText( VelocityEngineUtils.mergeTemplateIntoString( velocityEngine, templateName,
                    RuntimeConstants.ENCODING_DEFAULT, model ) );
        } catch ( VelocityException e ) {
            MailEngineImpl.log.error( e.getMessage(), e );
            return;
        }
        send( msg );
    }

    private void send( SimpleMailMessage msg ) {
        if ( StringUtils.isBlank( msg.getSubject() ) || StringUtils.isBlank( msg.getText() ) ) {
            MailEngineImpl.log.warn( "Not sending empty email, both subject and body are blank" );
            return;
        }
        try {
            mailSender.send( msg );
        } catch ( MailException ex ) {
            // log it and go on
            MailEngineImpl.log.error( ex.getMessage(), ex );
        }
    }
}
