package ubic.gemma.cli.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import ubic.gemma.cli.authentication.CLIAuthenticationAware;
import ubic.gemma.core.metrics.AbstractMeterRegistryConfigurer;

/**
 * This configurer adds two tags: {@code environment='cli'} and {@code user} to the given meter registry.
 * @author poirigui
 */
@Component
@Lazy(false)
@Profile("metrics")
public class MeterRegistryCliConfigurer extends AbstractMeterRegistryConfigurer implements CLIAuthenticationAware {

    private final MeterRegistry registry;

    @Autowired
    public MeterRegistryCliConfigurer( MeterRegistry registry ) {
        super( registry );
        this.registry = registry;
    }

    @Override
    protected void configure( MeterRegistry registry ) {
        registry.config().commonTags( "environment", "cli" );
    }

    @Override
    public void setAuthentication( Authentication authentication ) {
        if ( authentication.getPrincipal() instanceof UserDetails ) {
            UserDetails userDetails = ( UserDetails ) authentication.getPrincipal();
            registry.config().commonTags( "user", userDetails.getUsername() );
        } else if ( authentication.getPrincipal() instanceof String ) {
            registry.config().commonTags( "user", ( String ) authentication.getPrincipal() );
        }
    }

    @Override
    public void clearAuthentication() {
        registry.config().commonTags( "user", "anonymous" );
    }
}
