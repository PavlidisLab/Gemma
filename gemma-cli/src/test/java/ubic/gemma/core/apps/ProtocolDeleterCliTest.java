package ubic.gemma.core.apps;

import gemma.gsec.authentication.ManualAuthenticationService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.EntityLocator;
import ubic.gemma.core.util.test.BaseCliTest;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.persistence.service.common.protocol.ProtocolService;

import java.io.Console;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static ubic.gemma.core.util.test.Assertions.assertThat;

@ContextConfiguration
public class ProtocolDeleterCliTest extends BaseCliTest {

    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public ProtocolDeleterCli protocolDeleterCli() {
            return new ProtocolDeleterCli();
        }

        @Bean
        public ProtocolService protocolService() {
            return mock();
        }

        @Bean
        public EntityLocator entityLocator() {
            return mock();
        }

        @Bean
        public ManualAuthenticationService manualAuthenticationService() {
            return mock();
        }
    }

    @Autowired
    private ProtocolDeleterCli protocolDeleterCli;
    @Autowired
    private ProtocolService protocolService;
    @Autowired
    private EntityLocator entityLocator;

    @Test
    public void test() {
        Console console = mock( Console.class );
        when( console.readLine( any(), any() ) )
                .thenReturn( "YES" );
        Protocol protocol = new Protocol();
        when( entityLocator.locateProtocol( "foo" ) ).thenReturn( protocol );
        assertThat( protocolDeleterCli )
                .withConsole( console )
                .withArguments( "--protocol", "foo" )
                .succeeds();
        verify( protocolService ).remove( protocol );
    }
}