/*
 * The gemma-web project
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
package ubic.gemma.web.controller.common.auditAndSecurity;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.testing.BaseSpringWebTest;

/**
 * Tortures the signup system by starting many threads and signing up many users, while at the same time creating a lot
 * of expression experiments.
 * 
 * @author Paul
 * @version $Id$
 */
public class SignupControllerTest extends BaseSpringWebTest {

    @Autowired
    private SignupController suc;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Before
    public void setup() {
        suc.setRecaptchaTester( new RecaptchaTester() {
            @Override
            public boolean validateCaptcha( HttpServletRequest request, String recatpchaPvtKey ) {
                return true;
            }
        } );
    }

    @Test
    public void testSignup() throws Exception {
        int numThreads = 10; // too high and we run out of connections, which is not what we're testing.
        final int numsignupsperthread = 20;
        final Random random = new Random();
        final AtomicInteger c = new AtomicInteger( 0 );
        final AtomicBoolean failed = new AtomicBoolean( false );
        Collection<Thread> threads = new HashSet<Thread>();
        for ( int i = 0; i < numThreads; i++ ) {

            Thread k = new Thread( new Runnable() {
                @Override
                public void run() {
                    try {
                        for ( int j = 0; j < numsignupsperthread; j++ ) {
                            MockHttpServletRequest req = null;
                            Thread.sleep( random.nextInt( 50 ) );
                            req = new MockHttpServletRequest( "POST", "/signup.html" );
                            String uname = RandomStringUtils.randomAlphabetic( 10 );
                            // log.info( "Signingup: " + uname + " (" + c.get() + ")" );

                            String password = RandomStringUtils.randomAlphabetic( 40 );
                            req.addParameter( "password", password );

                            req.addParameter( "passwordConfirm", password );
                            req.addParameter( "username", uname );
                            String email = "foo@" + RandomStringUtils.randomAlphabetic( 10 ) + ".edu";
                            req.addParameter( "email", email );

                            req.addParameter( "emailConfirm", email );
                            suc.signup( req, new MockHttpServletResponse() );

                            /*
                             * Extra torture.
                             */
                            ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
                            ee.setDescription( "From test" );
                            ee.setName( RandomStringUtils.randomAlphabetic( 20 ) );
                            ee.setShortName( RandomStringUtils.randomAlphabetic( 20 ) );
                            // log.info( "Making experiment" + ee.getName() );
                            ee = expressionExperimentService.create( ee );

                            c.incrementAndGet();

                        }
                    } catch ( Exception e ) {
                        failed.set( true );
                        log.error( "!!!!!!!!!!!!!!!!!!!!!! FAILED: " + e.getMessage() );
                        log.debug( e, e );
                        throw new RuntimeException( e );
                    }
                    log.debug( "Thread done." );
                }
            } );
            threads.add( k );

            k.start();
        }

        int waits = 0;
        int maxWaits = 20;
        int expectedEventCount = numThreads * numsignupsperthread;
        while ( c.get() < expectedEventCount && !failed.get() ) {
            Thread.sleep( 3000 );
            log.info( "Waiting ... C=" + +c.get() );
            if ( ++waits > maxWaits ) {
                for ( Thread t : threads ) {
                    if ( t.isAlive() ) t.interrupt();
                }
                fail( "Multithreaded failure: timed out." );
            }
        }

        log.debug( " &&&&& DONE &&&&&" );

        for ( Thread thread : threads ) {
            if ( thread.isAlive() ) thread.interrupt();
        }

        if ( failed.get() || c.get() != expectedEventCount ) {
            fail( "Multithreaded loading failure: check logs for failure to recover from deadlock?" );
        } else {
            log.info( "TORTURE TEST PASSED!" );
        }
    }
}
