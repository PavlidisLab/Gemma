package ubic.gemma.cli.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import ubic.gemma.core.metrics.AbstractMeterRegistryConfigurer;

/**
 * This configurer adds two tags: {@code environment='cli'} and {@code user} to the given meter registry.
 * @author poirigui
 */
public class MeterRegistryCliConfigurer extends AbstractMeterRegistryConfigurer implements ApplicationContextAware {

    private ConfigurableApplicationContext applicationContext;

    public MeterRegistryCliConfigurer( MeterRegistry registry ) {
        super( registry );
    }

    @Override
    protected void configure( MeterRegistry registry ) {
        registry.config().commonTags( "environment", "cli" );
        if ( applicationContext != null ) {
            // unfortunately, the lambda is necessary to let Spring correctly infer the type of handled events
            //noinspection Convert2Lambda
            applicationContext.addApplicationListener( new ApplicationListener<AuthenticationSuccessEvent>() {
                @Override
                public void onApplicationEvent( AuthenticationSuccessEvent event ) {
                    if ( event.getAuthentication().getPrincipal() instanceof UserDetails ) {
                        UserDetails userDetails = ( UserDetails ) event.getAuthentication().getPrincipal();
                        registry.config().commonTags( "user", userDetails.getUsername() );
                    }
                }
            } );
        }
    }

    @Override
    public void setApplicationContext( ApplicationContext applicationContext ) throws BeansException {
        if ( applicationContext instanceof ConfigurableApplicationContext ) {
            this.applicationContext = ( ConfigurableApplicationContext ) applicationContext;
        }
    }
}
