package ubic.gemma.core.logging.log4j;


import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import ubic.gemma.core.util.BuildInfo;

/**
 * Populate the Log4j {@link ThreadContext} with useful information for logging.
 *
 * @author poirigui
 */
@Component
// we always want to use this
@Lazy(value = false)
public class ThreadContextConfigurer implements InitializingBean {

    /**
     * Context key used to store the current user.
     */
    private static final String
            CURRENT_USER_CONTEXT_KEY = "ubic.gemma.core.logging.log4j.ThreadContextConfigurer.currentUser",
            BUILD_INFO_CONTEXT_KEY = "ubic.gemma.core.logging.log4j.ThreadContextConfigurer.buildInfo";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private BuildInfo buildInfo;

    @Override
    public void afterPropertiesSet() throws Exception {
        setCurrentUser( "anonymous" );
        if ( applicationContext instanceof ConfigurableApplicationContext ) {
            //noinspection Convert2Lambda
            ( ( ConfigurableApplicationContext ) applicationContext )
                    // unfortunately, the lambda is necessary to let Spring correctly infer the type of handled events
                    //noinspection Convert2Lambda
                    .addApplicationListener( new ApplicationListener<AuthenticationSuccessEvent>() {
                        @Override
                        public void onApplicationEvent( AuthenticationSuccessEvent event ) {
                            Authentication auth = event.getAuthentication();
                            if ( auth.getPrincipal() instanceof UserDetails ) {
                                ThreadContextConfigurer.this.setCurrentUser( ( ( UserDetails ) auth.getPrincipal() ).getUsername() );
                            }
                        }
                    } );
        }
        ThreadContext.put( BUILD_INFO_CONTEXT_KEY, buildInfo.toString() );
    }

    private void setCurrentUser( String username ) {
        ThreadContext.put( CURRENT_USER_CONTEXT_KEY, username );
    }
}
