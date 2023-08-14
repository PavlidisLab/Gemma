package ubic.gemma.persistence.util;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.reset;

@ContextConfiguration
public class MailEngineTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    static class MailEngineTestContextConfiguration {

        @Bean
        public MailEngine mailEngine() {
            return new MailEngineImpl();
        }

        @Bean
        public MailSender mailSender() {
            return mock( MailSender.class );
        }

        @Bean
        public VelocityEngine velocityEngine() {
            Properties props = new Properties();
            props.setProperty( "resource.loaders", "class" );
            props.setProperty( "resource.loader.class.class", ClasspathResourceLoader.class.getName() );
            return new VelocityEngine( props );
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
        mailEngine.sendAdminMessage( "test", "test subject" );
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass( SimpleMailMessage.class );
        verify( mailSender ).send( captor.capture() );
        assertThat( captor.getValue() )
                .isNotNull().satisfies( m -> {
                    assertThat( m.getTo() ).containsExactly( Settings.getAdminEmailAddress() );
                    assertThat( m.getFrom() ).isEqualTo( Settings.getAdminEmailAddress() );
                    assertThat( m.getSubject() ).isEqualTo( "test subject" );
                    assertThat( m.getText() ).isEqualTo( "test" );
                } );
    }

    @Test
    public void testSendMessageWithVelocityTemplate() {
        Map<String, Object> vars = new HashMap<>();
        vars.put( "username", "foo" );
        vars.put( "siteurl", "http://example.com/" );
        vars.put( "confirmLink", "http://example.com/confirm?token=12ijdqwer9283" );
        mailEngine.sendMessage( new SimpleMailMessage(), "accountCreated.vm", vars );
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass( SimpleMailMessage.class );
        verify( mailSender ).send( captor.capture() );
        assertThat( captor.getValue() )
                .isNotNull();
        assertThat( captor.getValue().getText() )
                .contains( "foo" )
                .contains( "http://example.com/confirm?token=12ijdqwer9283" );
    }
}