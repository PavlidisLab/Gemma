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

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.controller.common.auditAndSecurity.recaptcha.ReCaptcha;
import ubic.gemma.web.controller.common.auditAndSecurity.recaptcha.ReCaptchaResponse;
import ubic.gemma.web.util.BaseSpringWebTest;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tortures the signup system by starting many threads and signing up many users, while at the same time creating a lot
 * of expression experiments.
 *
 * @author Paul
 */
public class SignupControllerTest extends BaseSpringWebTest {

    @Autowired
    private SignupController suc;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    /* fixtures */
    private Collection<Future<?>> futures;

    @Before
    public void setUp() {
        ReCaptcha mockReCaptcha = Mockito.mock( ReCaptcha.class );
        when( mockReCaptcha.validateRequest( any( HttpServletRequest.class ) ) )
                .thenReturn( new ReCaptchaResponse( true, "" ) );
        suc.setRecaptchaTester( mockReCaptcha );
        futures = new HashSet<>();
    }

    @After
    public void tearDown() {
        for ( Future<?> future : futures ) {
            future.cancel( true );
        }
    }


    @SuppressWarnings("Duplicates") // Not in this project
    @Test
    @Category(SlowTest.class)
    public void testSignup() throws Exception {
        int numThreads = 10; // too high and we run out of connections, which is not what we're testing.
        final int numsignupsperthread = 20;
        int expectedEventCount = numThreads * numsignupsperthread;
        final Random random = new Random();
        final AtomicInteger c = new AtomicInteger( 0 );
        ExecutorService executor = Executors.newFixedThreadPool( numThreads );
        for ( int i = 0; i < expectedEventCount; i++ ) {
            futures.add( executor.submit( () -> {
                MockHttpServletRequest req;
                try {
                    Thread.sleep( random.nextInt( 50 ) );
                } catch ( InterruptedException e ) {
                    throw new RuntimeException( e );
                }
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
                try {
                    suc.signup( password, password, uname, email, email, req, new MockHttpServletResponse() );
                } catch ( Exception e ) {
                    throw new RuntimeException( e );
                }

                /*
                 * Extra torture.
                 */
                ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
                ee.setDescription( "From test" );
                ee.setName( RandomStringUtils.randomAlphabetic( 20 ) );
                ee.setShortName( RandomStringUtils.randomAlphabetic( 20 ) );
                // log.info( "Making experiment" + ee.getName() );
                expressionExperimentService.create( ee );

                c.incrementAndGet();
                log.debug( "Thread done." );
            } ) );
        }

        // 20 seconds
        long maxWaitNano = 20L * 1000L * 1000L * 1000L;
        long startTimeNano = System.nanoTime();
        for ( Future<?> f : futures ) {
            assertThat( f ).succeedsWithin( Math.max( maxWaitNano - ( System.nanoTime() - startTimeNano ), 0 ), TimeUnit.NANOSECONDS );
        }

        log.info( String.format( "Signup torture test took %d seconds", ( System.nanoTime() - startTimeNano ) / 1000 / 1000 / 1000 ) );

        assertEquals( expectedEventCount, c.get() );
    }
}
