package ubic.gemma.core.util;

import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.mail.*;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.common.auditAndSecurity.User;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class MailServiceTest extends BaseTest {

    @Configuration
    @TestComponent
    @Import(VelocityConfig.class)
    static class MailEngineTestContextConfiguration {

        @Bean
        public static PropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer(
                    "gemma.hosturl=http://localhost:8080",
                    "gemma.admin.email=gemma@chibi.msl.ubc.ca",
                    "gemma.noreply.email=noreply@gemma.pavlab.msl.ubc.ca",
                    "gemma.support.email=pavlab-support@msl.ubc.ca" );
        }

        @Bean
        public MailService mailService() {
            return new MailServiceImpl();
        }

        @Bean
        public MailEngine mailEngine() {
            return new MailEngineImpl();
        }

        @Bean
        public MailSender mailSender() {
            return mock( MailSender.class );
        }
    }

    @Autowired
    private MailService mailService;

    @Autowired
    private MailSender mailSender;

    @After
    public void tearDown() {
        reset( mailSender );
    }

    @Test
    public void testSendAccountCreatedEmail() {
        mailService.sendSignupConfirmationEmail( "foo@example.com", "foo", "12ijdqwer9283", Locale.getDefault() );
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass( SimpleMailMessage.class );
        verify( mailSender ).send( captor.capture() );
        assertThat( captor.getValue() )
                .isNotNull();
        assertThat( captor.getValue().getText() )
                .contains( "foo" )
                .contains( "http://localhost:8080/confirmRegistration.html?key=12ijdqwer9283&username=foo" );
    }

    @Test
    public void testSendPasswordResetEmail() {
        mailService.sendResetConfirmationEmail( "foo@example.com", "foo", "1234", "12ijdqwer9283", Locale.getDefault() );
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass( SimpleMailMessage.class );
        verify( mailSender ).send( captor.capture() );
        assertThat( captor.getValue() )
                .isNotNull();
        assertThat( captor.getValue().getText() )
                .contains( "foo" )
                .contains( "1234" )
                .contains( "http://localhost:8080/confirmRegistration.html?key=12ijdqwer9283&username=foo" );
    }

    @Test
    public void testUserAddeddToGroup() {
        User user = User.Factory.newInstance( "foo" );
        user.setEmail( "foo@example.com" );
        String group = "testGroup";
        User admin = User.Factory.newInstance( "bar" );
        admin.setEmail( "admin@example.com" );
        mailService.sendAddUserToGroupEmail( user, group, admin );
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass( SimpleMailMessage.class );
        verify( mailSender ).send( captor.capture() );
        assertThat( captor.getValue() )
                .isNotNull();
        assertThat( captor.getValue().getText() )
                .contains( "foo" )
                .contains( "bar" )
                .contains( "http://localhost:8080/manageGroups.html" );
    }

    @Test
    public void testUserRemovedFromGroup() {
        User user = User.Factory.newInstance( "foo" );
        user.setEmail( "foo@example.com" );
        String group = "testGroup";
        User admin = User.Factory.newInstance( "bar" );
        admin.setEmail( "admin@example.com" );
        mailService.sendRemoveUserFromGroupEmail( user, group, admin );
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass( SimpleMailMessage.class );
        verify( mailSender ).send( captor.capture() );
        assertThat( captor.getValue() )
                .isNotNull();
        assertThat( captor.getValue().getText() )
                .contains( "foo" )
                .contains( "bar" )
                .contains( "http://localhost:8080/manageGroups.html" );
    }
}