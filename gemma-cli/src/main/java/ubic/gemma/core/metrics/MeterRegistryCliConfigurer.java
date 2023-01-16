package ubic.gemma.core.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * This configurer adds two tags: {@code environment} and {@code user} to the given meter registry.
 * @author poirigui
 */
public class MeterRegistryCliConfigurer implements InitializingBean, ApplicationContextAware {

    private final MeterRegistry registry;

    private ConfigurableApplicationContext applicationContext;

    public MeterRegistryCliConfigurer( MeterRegistry registry ) {
        this.registry = registry;
    }

    @Override
    public void afterPropertiesSet() {
        registry.config().commonTags( "environment", "cli" );
        if ( applicationContext != null ) {
            applicationContext.addApplicationListener( new AddTagOnAuthenticationListener() );
        }
    }

    @Override
    public void setApplicationContext( ApplicationContext applicationContext ) throws BeansException {
        if ( applicationContext instanceof ConfigurableApplicationContext ) {
            this.applicationContext = ( ConfigurableApplicationContext ) applicationContext;
        }
    }

    private class AddTagOnAuthenticationListener implements ApplicationListener<AuthenticationSuccessEvent> {

        @Override
        public void onApplicationEvent( AuthenticationSuccessEvent event ) {
            if ( event.getAuthentication().getPrincipal() instanceof UserDetails ) {
                UserDetails userDetails = ( UserDetails ) event.getAuthentication().getPrincipal();
                registry.config().commonTags( "user", userDetails.getUsername() );
            }
        }
    }
}
