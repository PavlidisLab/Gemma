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
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.web.controller.common.auditAndSecurity.recaptcha.ReCaptcha;
import ubic.gemma.web.controller.common.auditAndSecurity.recaptcha.ReCaptchaResponse;
import ubic.gemma.web.util.BaseSpringWebTest;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tortures the signup system by starting many threads and signing up many users, while at the same time creating a lot
 * of expression experiments.
 * <p>
 * This test replaces the recaptcha service used by {@link SignupController}, so it is annotated with {@link DirtiesContext}
 * to invalidate the context once all the tests have completed.
 *
 * @author Paul
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SignupControllerTest extends BaseSpringWebTest {

    @Autowired
    private SignupController suc;

    @Mock
    private ReCaptcha mockReCaptcha;

    /* fixtures */
    private Collection<Future<?>> futures;

    @Before
    public void setUp() {
        when( mockReCaptcha.isPrivateKeySet() ).thenReturn( true );
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

    @Test
    @Category(SlowTest.class)
    public void testSignup() {
        int numThreads = 10; // too high and we run out of connections, which is not what we're testing.
        final int numsignupsperthread = 20;
        int expectedEventCount = numThreads * numsignupsperthread;
        final AtomicInteger c = new AtomicInteger( 0 );
        ExecutorService executor = Executors.newFixedThreadPool( numThreads );
        for ( int i = 0; i < expectedEventCount; i++ ) {
            futures.add( executor.submit( () -> {
                try {
                    String uname = RandomStringUtils.randomAlphabetic( 10 );
                    String password = RandomStringUtils.randomAlphabetic( 40 );
                    String email = "foo@" + RandomStringUtils.randomAlphabetic( 10 ) + ".edu";
                    mvc.perform( post( "/signup.html" )
                                    .param( "password", password )
                                    .param( "passwordConfirm", password )
                                    .param( "username", uname )
                                    .param( "email", email )
                                    .param( "emailConfirm", email ) )
                            .andExpect( status().isOk() )
                            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
                            .andExpect( jsonPath( "$.success" ).value( true ) );
                } catch ( Exception e ) {
                    throw new RuntimeException( e );
                }
                c.incrementAndGet();
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
        verify( this.mockReCaptcha, times( expectedEventCount ) )
                .isPrivateKeySet();
        verify( this.mockReCaptcha, times( expectedEventCount ) )
                .validateRequest( any() );
    }
}
