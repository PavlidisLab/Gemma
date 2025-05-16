package ubic.gemma.apps;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.cli.authentication.CLIAuthenticationManager;
import ubic.gemma.cli.util.test.BaseCliTest;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.persistence.service.common.protocol.ProtocolService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

@ContextConfiguration
public class ProtocolAdderCliTest extends BaseCliTest {

    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public ProtocolAdderCli protocolAdderCli() {
            return new ProtocolAdderCli();
        }

        @Bean
        public ProtocolService protocolService() {
            return mock();
        }

        @Bean
        public CLIAuthenticationManager cliAuthenticationManager() {
            return mock();
        }
    }

    @Autowired
    private ProtocolAdderCli protocolAdderCli;

    @Autowired
    private ProtocolService protocolService;

    @After
    public void resetMocks() {
        reset( protocolService );
    }

    @Test
    @WithMockUser
    public void test() {
        when( protocolService.create( any( Protocol.class ) ) ).thenAnswer( a -> a.getArgument( 0 ) );
        assertThat( protocolAdderCli )
                .withArguments( "--name", "foo" )
                .succeeds();
        verify( protocolService ).findByName( "foo" );
        verify( protocolService ).create( any( Protocol.class ) );
    }

    @Test
    @WithMockUser
    public void testWhenProtocolWithNameAlreadyExists() {
        when( protocolService.findByName( "foo" ) ).thenReturn( new Protocol() );
        assertThat( protocolAdderCli )
                .withArguments( "--name", "foo" )
                .failsWith( 2 );
        verify( protocolService ).findByName( "foo" );
        verifyNoMoreInteractions( protocolService );
    }
}