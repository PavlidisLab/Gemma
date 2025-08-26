package ubic.gemma.web.controller.common.auditAndSecurity;

import gemma.gsec.authentication.UserDetailsImpl;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.mail.MailService;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.web.util.BaseWebTest;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class UserFormMultiActionControllerTest extends BaseWebTest {

    @Configuration
    @TestComponent
    static class CC extends BaseWebTestContextConfiguration {

        @Bean
        public UserFormMultiActionController userFormMultiActionController() {
            return new UserFormMultiActionController();
        }

        @Bean
        public UserManager userManager() {
            return mock();
        }

        @Bean
        public MailService mailService() {
            return mock();
        }
    }

    @Autowired
    private UserManager userManager;

    @After
    public void resetMocks() {
        reset( userManager );
    }

    @Test
    @WithMockUser("bob")
    public void testEditUserUpdateEmail() throws Exception {
        UserDetailsImpl details = new UserDetailsImpl( "1234", "bob", true, null, "bob@example.com", null, null );
        when( userManager.loadUserByUsername( "bob" ) ).thenReturn( details );
        perform( post( "/editUser.html" )
                .param( "email", "bob2@example.com" ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.success" ).value( true ) );
        verify( userManager ).loadUserByUsername( "bob" );
        verify( userManager ).updateUser( details );
        verifyNoMoreInteractions( userManager );
    }

    @Test
    @WithMockUser("bob")
    public void testEditUserUpdateEmailWhenEmailIsInvalid() throws Exception {
        UserDetailsImpl details = new UserDetailsImpl( "1234", "bob", true, null, "bob@example.com", null, null );
        when( userManager.loadUserByUsername( "bob" ) ).thenReturn( details );
        perform( post( "/editUser.html" )
                .param( "email", "invalid" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( jsonPath( "$.success" ).value( false ) )
                .andExpect( jsonPath( "$.message" ).value( "The email address does not look valid." ) );
        verify( userManager ).loadUserByUsername( "bob" );
        verifyNoMoreInteractions( userManager );
    }

    @Test
    @WithMockUser("bob")
    public void testEditUserUpdatePassword() throws Exception {
        perform( post( "/editUser.html" )
                .param( "password", "123456" )
                .param( "passwordConfirm", "123456" )
                .param( "oldPassword", "1234" ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.success" ).value( true ) );
        verify( userManager ).changePassword( "1234", "123456" );
        verifyNoMoreInteractions( userManager );
    }

    @Test
    @WithMockUser("bob")
    public void testEditUserUpdatePasswordWhenPasswordIsTooShort() throws Exception {
        perform( post( "/editUser.html" )
                .param( "password", "12345" )
                .param( "passwordConfirm", "12345" )
                .param( "oldPassword", "1234" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( jsonPath( "$.success" ).value( false ) )
                .andExpect( jsonPath( "$.message" ).value( "Password must be at least 6 characters long." ) );
        verifyNoInteractions( userManager );
    }

    @Test
    @WithMockUser("bob")
    public void testEditUserUpdatePasswordWhenConfirmationDoesNotMatch() throws Exception {
        perform( post( "/editUser.html" )
                .param( "password", "123456" )
                .param( "passwordConfirm", "123457" )
                .param( "oldPassword", "1234" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( jsonPath( "$.success" ).value( false ) )
                .andExpect( jsonPath( "$.message" ).value( "Password does not match the confirmation." ) );
        verifyNoInteractions( userManager );
    }
}