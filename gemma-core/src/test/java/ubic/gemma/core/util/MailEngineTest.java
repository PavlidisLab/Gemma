package ubic.gemma.core.util;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class MailEngineTest extends BaseTest {

    @Configuration
    @TestComponent
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
        mailEngine.sendAdminMessage( "test subject", "test" );
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
    public void testSendMessageWithVelocityTemplate() {
        Map<String, Object> vars = new HashMap<>();
        vars.put( "username", "foo" );
        vars.put( "siteurl", "http://example.com/" );
        vars.put( "confirmLink", "http://example.com/confirm?token=12ijdqwer9283" );
        mailEngine.sendMessage( "test", "subject", "accountCreated.vm", vars );
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass( SimpleMailMessage.class );
        verify( mailSender ).send( captor.capture() );
        assertThat( captor.getValue() )
                .isNotNull();
        assertThat( captor.getValue().getText() )
                .contains( "foo" )
                .contains( "http://example.com/confirm?token=12ijdqwer9283" );
    }
}