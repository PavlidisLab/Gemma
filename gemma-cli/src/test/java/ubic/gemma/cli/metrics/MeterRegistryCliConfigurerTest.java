package ubic.gemma.cli.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.cli.authentication.CLIAuthenticationAware;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration
public class MeterRegistryCliConfigurerTest extends BaseTest {

    @Configuration
    @TestComponent
    static class MeterRegistryCliConfigurerTestContextConfiguration {

        @Bean
        public SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        public MeterRegistryCliConfigurer meterRegistryCliConfigurer( MeterRegistry meterRegistry ) {
            return new MeterRegistryCliConfigurer( meterRegistry );
        }
    }

    @Autowired
    private SimpleMeterRegistry meterRegistry;

    @Autowired
    private List<CLIAuthenticationAware> cliAuthenticationAware;

    @Test
    public void test() {
        meterRegistry.counter( "test" ).increment();
        assertThat( meterRegistry.getMetersAsString() )
                .contains( "environment='cli'" )
                .doesNotContain( "user='joe'" );

        // fake a successful authentication event
        Authentication auth = mock( Authentication.class );
        UserDetails userDetails = mock( UserDetails.class );
        when( userDetails.getUsername() ).thenReturn( "joe" );
        when( auth.getPrincipal() ).thenReturn( userDetails );
        cliAuthenticationAware.forEach( bean -> bean.setAuthentication( auth ) );

        meterRegistry.counter( "test" ).increment();
        assertThat( meterRegistry.getMetersAsString() )
                .contains( "environment='cli'" )
                .contains( "user='joe'" );
    }

}