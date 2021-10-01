/*
 * The gemma project
 *
 * Copyright (c) 2013 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.util.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessagePreparator;

import javax.mail.internet.MimeMessage;

/**
 * Mock mail sender for testing.
 *
 * @author Paul
 */
public class DummyMailSender extends JavaMailSenderImpl {

    private static final Log log = LogFactory.getLog( DummyMailSender.class );

    @Override
    public void send( SimpleMailMessage simpleMessage ) throws MailException {
        DummyMailSender.log.debug( "Sent" );
    }

    @Override
    public void send( SimpleMailMessage[] simpleMessages ) throws MailException {
        DummyMailSender.log.debug( "Sent" );
    }

    @Override
    public void send( MimeMessage mimeMessage ) throws MailException {
        DummyMailSender.log.debug( "Sent" );
    }

    @Override
    public void send( MimeMessage[] mimeMessages ) throws MailException {
        DummyMailSender.log.debug( "Sent" );
    }

    @Override
    public void send( final MimeMessagePreparator mimeMessagePreparator ) throws MailException {
        DummyMailSender.log.debug( "Sent" );
    }

    @Override
    public void send( MimeMessagePreparator[] mimeMessagePreparators ) throws MailException {
        DummyMailSender.log.debug( "Sent" );
    }
}