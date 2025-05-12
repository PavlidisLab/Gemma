package ubic.gemma.cli.authentication;

import gemma.gsec.authentication.ManualAuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ubic.gemma.cli.logging.log4j.UserDetailsThreadContextPopulator;
import ubic.gemma.cli.metrics.MeterRegistryCliConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class CliAuthConfig {

    @Autowired
    private ManualAuthenticationService manualAuthenticationService;

    @Autowired
    private GemmaRestApiClientAuthenticator gemmaRestApiClientAuthenticator;

    @Autowired
    private UserDetailsThreadContextPopulator userDetailsThreadContextPopulator;

    @Autowired(required = false)
    private MeterRegistryCliConfigurer meterRegistryCliConfigurer;

    @Bean
    public CLIAuthenticationManager cliAuthenticationManager() {
        List<CLIAuthenticationAware> authenticationAware = new ArrayList<>();
        authenticationAware.add( gemmaRestApiClientAuthenticator );
        authenticationAware.add( userDetailsThreadContextPopulator );
        if ( meterRegistryCliConfigurer != null ) {
            authenticationAware.add( meterRegistryCliConfigurer );
        }
        return new CLIAuthenticationManager( manualAuthenticationService, authenticationAware );
    }
}
