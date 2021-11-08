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
package ubic.gemma.core.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

/**
 * Mock mail sender for testing.
 *
 * @author Paul
 */
public class DummyMailSender implements MailSender {

    private static final Log log = LogFactory.getLog( DummyMailSender.class );

    @Override
    public void send( SimpleMailMessage simpleMessage ) throws MailException {
        DummyMailSender.log.debug( "Sent" );
    }

    @Override
    public void send( SimpleMailMessage[] simpleMessages ) throws MailException {
        DummyMailSender.log.debug( "Sent" );
    }
}