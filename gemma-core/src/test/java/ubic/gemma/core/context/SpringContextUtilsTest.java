package ubic.gemma.core.context;

import org.junit.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.Assert.assertEquals;

public class SpringContextUtilsTest {

    @Test
    public void testPrepareContext() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.getEnvironment().addActiveProfile( EnvironmentProfiles.TEST );
        SpringContextUtils.prepareContext( context );
    }

    @Test(expected = IllegalStateException.class)
    public void testPrepareContextWhenMoreThanOneEnvironmentProfileIsActive() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.getEnvironment().addActiveProfile( EnvironmentProfiles.TEST );
        context.getEnvironment().addActiveProfile( EnvironmentProfiles.DEV );
        SpringContextUtils.prepareContext( context );
    }

    @Test
    public void testPrepareContextWithIncorrectSecurityContextHolderStrategyIsSet() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.getEnvironment().addActiveProfile( EnvironmentProfiles.TEST );
        SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_THREADLOCAL );
        SpringContextUtils.prepareContext( context );
        assertEquals( "org.springframework.security.core.context.InheritableThreadLocalSecurityContextHolderStrategy",
                SecurityContextHolder.getContextHolderStrategy().getClass().getName() );
    }
}