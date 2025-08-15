package ubic.gemma.core.mail;

import org.apache.velocity.exception.VelocityException;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class MailEngineImplTest extends BaseTest {

    @Configuration
    @TestComponent
    @Import({ VelocityConfig.class })
    static class MailEngineTestContextConfiguration {

        @Bean
        public static PropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer(
                    "gemma.admin.email=gemma@chibi.msl.ubc.ca",
                    "gemma.noreply.email=noreply@gemma.pavlab.msl.ubc.ca",
                    "gemma.support.email=pavlab-support@msl.ubc.ca" );
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
    private MailEngine mailEngine;

    @Autowired
    private MailSender mailSender;

    @After
    public void tearDown() {
        reset( mailSender );
    }


    @Test
    public void test() {
        mailEngine.sendMessageToAdmin( "test subject", "test" );
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass( SimpleMailMessage.class );
        verify( mailSender ).send( captor.capture() );
        assertThat( captor.getValue() )
                .isNotNull().satisfies( m -> {
                    assertThat( m.getTo() ).containsExactly( "gemma@chibi.msl.ubc.ca" );
                    assertThat( m.getFrom() ).isEqualTo( "noreply@gemma.pavlab.msl.ubc.ca" );
                    assertThat( m.getSubject() ).isEqualTo( "test subject" );
                    assertThat( m.getText() ).isEqualTo( "test" );
                } );
    }

    @Test
    public void testSendWithUnresolvableVariables() {
        Map<String, Object> vars = new HashMap<>();
        vars.put( "username", "foo" );
        assertThatThrownBy( () -> mailEngine.sendMessage( "test", "subject", "passwordReset", vars ) )
                .isInstanceOf( MailPreparationException.class )
                .hasCauseInstanceOf( VelocityException.class );
    }

    @Test
    public void testSendWithNullReference() {
        Map<String, Object> vars = new HashMap<>();
        vars.put( "message", "test" );
        vars.put( "username", "foo" );
        vars.put( "password", "1234" );
        vars.put( "confirmLink", null );
        assertThatThrownBy( () -> mailEngine.sendMessage( "test", "subject", "passwordReset", vars ) )
                .isInstanceOf( MailPreparationException.class )
                .hasCauseInstanceOf( VelocityException.class );
    }

    @Test
    public void testSendWithUnresolvableTemplate() {
        Map<String, Object> vars = new HashMap<>();
        vars.put( "username", "foo" );
        vars.put( "password", "1234" );
        vars.put( "confirmLink", "http://example.com/confirm?token=12ijdqwer9283" );
        assertThatThrownBy( () -> mailEngine.sendMessage( "test", "subject", "passwordReset2", vars ) )
                .isInstanceOf( MailPreparationException.class )
                .hasCauseInstanceOf( VelocityException.class );
    }
}