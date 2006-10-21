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
package ubic.gemma.testing;

import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Class to extend for tests of controllers et al that need a spring context. Provides convenience methods for dealing
 * with mock requests and responses. Also provides a safe port to send email on for testing (for example, using
 * dumbster)
 * 
 * @author pavlidis
 * @version $Id$
 * @see com.dumbster.smtp.SimpleSmtpServer
 */
public abstract class BaseSpringWebTest extends BaseSpringContextTest {

    /**
     * 
     */
    protected static final int MAIL_PORT = 2525;

    /**
     * Convenience methods to make tests simpler
     */
    public MockHttpServletRequest newPost( String url ) {
        return new MockHttpServletRequest( "POST", url );
    }

    public MockHttpServletRequest newGet( String url ) {
        return new MockHttpServletRequest( "GET", url );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseTransactionalSpringContextTest#onSetUpInTransaction()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        // change the port on the mailSender so it doesn't conflict with an
        // existing SMTP server on localhost
        JavaMailSenderImpl mailSender = ( JavaMailSenderImpl ) getBean( "mailSender" );
        assert mailSender != null;
        mailSender.setPort( MAIL_PORT );
        mailSender.setHost( "localhost" );
    }
}
