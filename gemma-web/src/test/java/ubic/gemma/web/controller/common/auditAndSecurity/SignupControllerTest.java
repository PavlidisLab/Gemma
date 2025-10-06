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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.web.controller.common.auditAndSecurity.recaptcha.ReCaptcha;
import ubic.gemma.web.controller.common.auditAndSecurity.recaptcha.ReCaptchaResponse;
import ubic.gemma.web.util.BaseWebIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * This test replaces the recaptcha service used by {@link SignupController}, so it is annotated with {@link DirtiesContext}
 * to invalidate the context once all the tests have completed.
 *
 * @author Paul
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SignupControllerTest extends BaseWebIntegrationTest implements InitializingBean {

    @Autowired
    private SignupController suc;

    @Autowired
    private UserManager userManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ReCaptcha mockReCaptcha = mock( ReCaptcha.class );

    @Override
    public void afterPropertiesSet() {
        suc.setReCaptcha( mockReCaptcha );
    }

    @Before
    public void setUp() {
        when( mockReCaptcha.isPrivateKeySet() ).thenReturn( true );
    }

    @After
    public void tearDown() {
        reset( mockReCaptcha );
    }

    @Test
    public void testSignup() throws Exception {
        when( mockReCaptcha.validateRequest( any() ) )
                .thenReturn( new ReCaptchaResponse( true, "" ) );
        String uname = RandomStringUtils.insecure().nextAlphabetic( 10 );
        String password = RandomStringUtils.insecure().nextAlphabetic( 40 );
        String email = "foo@" + RandomStringUtils.insecure().nextAlphabetic( 10 ) + ".edu";
        perform( post( "/signup.html" )
                .param( "password", password )
                .param( "passwordConfirm", password )
                .param( "username", uname )
                .param( "email", email )
                .param( "emailConfirm", email ) )
                .andExpect( status().isOk() )
                .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
                .andExpect( jsonPath( "$.success" ).value( true ) );
        verify( mockReCaptcha ).isPrivateKeySet();
        verify( mockReCaptcha ).validateRequest( any() );
        assertThat( userManager.findByEmail( email ) )
                .satisfies( u -> {
                    assertThat( u.getUserName() ).isEqualTo( uname );
                    assertThat( u.getEmail() ).isEqualTo( email );
                    assertThat( u.getPassword() ).isEqualTo( passwordEncoder.encodePassword( password, uname ) );
                    assertThat( u.isEnabled() ).isFalse();
                } );
    }

    @Test
    public void testSignupWhenRecaptchaIsDisabled() throws Exception {
        when( mockReCaptcha.isPrivateKeySet() ).thenReturn( false );
        String uname = RandomStringUtils.insecure().nextAlphabetic( 10 );
        String password = RandomStringUtils.insecure().nextAlphabetic( 40 );
        String email = "foo@" + RandomStringUtils.insecure().nextAlphabetic( 10 ) + ".edu";
        perform( post( "/signup.html" )
                .param( "password", password )
                .param( "passwordConfirm", password )
                .param( "username", uname )
                .param( "email", email )
                .param( "emailConfirm", email ) )
                .andExpect( status().isOk() )
                .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
                .andExpect( jsonPath( "$.success" ).value( true ) );
        verify( mockReCaptcha ).isPrivateKeySet();
        verifyNoMoreInteractions( mockReCaptcha );
        assertThat( userManager.findByEmail( email ) )
                .satisfies( u -> {
                    assertThat( u.getUserName() ).isEqualTo( uname );
                    assertThat( u.getEmail() ).isEqualTo( email );
                    assertThat( u.getPassword() ).isEqualTo( passwordEncoder.encodePassword( password, uname ) );
                    assertThat( u.isEnabled() ).isFalse();
                } );
    }

    @Test
    public void testSignupWithRecaptchaIsInvalid() throws Exception {
        when( mockReCaptcha.validateRequest( any() ) )
                .thenReturn( new ReCaptchaResponse( false, "You are a bot, I knew it!" ) );
        String uname = RandomStringUtils.insecure().nextAlphabetic( 10 );
        String password = RandomStringUtils.insecure().nextAlphabetic( 40 );
        String email = "foo@" + RandomStringUtils.insecure().nextAlphabetic( 10 ) + ".edu";
        perform( post( "/signup.html" )
                .param( "password", password )
                .param( "passwordConfirm", password )
                .param( "username", uname )
                .param( "email", email )
                .param( "emailConfirm", email ) )
                .andExpect( status().isBadRequest() )
                .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
                .andExpect( jsonPath( "$.success" ).value( false ) );
        verify( mockReCaptcha ).isPrivateKeySet();
        verify( mockReCaptcha ).validateRequest( any() );
        assertThat( userManager.findByEmail( email ) ).isNull();
    }


    @Test
    public void testSignupWithPasswordDosentMatch() throws Exception {
        when( mockReCaptcha.validateRequest( any() ) )
                .thenReturn( new ReCaptchaResponse( false, "You are a bot, I knew it!" ) );
        String uname = RandomStringUtils.insecure().nextAlphabetic( 10 );
        String password = RandomStringUtils.insecure().nextAlphabetic( 40 );
        String email = "foo@" + RandomStringUtils.insecure().nextAlphabetic( 10 ) + ".edu";
        perform( post( "/signup.html" )
                .param( "password", password )
                .param( "passwordConfirm", password )
                .param( "username", uname )
                .param( "email", email )
                .param( "emailConfirm", email ) )
                .andExpect( status().isBadRequest() )
                .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
                .andExpect( jsonPath( "$.success" ).value( false ) );
        verify( mockReCaptcha ).isPrivateKeySet();
        verify( mockReCaptcha ).validateRequest( any() );
        assertThat( userManager.findByEmail( email ) ).isNull();
    }
}
