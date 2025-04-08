package ubic.gemma.cli.util.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ubic.gemma.cli.authentication.CLIAuthenticationManager;
import ubic.gemma.core.context.TestComponent;

import static org.mockito.Mockito.mock;

/**
 * A base class with mocks for testing CLI commands that require authentication.
 */
@Configuration
@TestComponent
public class AuthenticatedCliTestConfig {

    @Bean
    public CLIAuthenticationManager cliAuthenticationManager() {
        return mock();
    }
}
