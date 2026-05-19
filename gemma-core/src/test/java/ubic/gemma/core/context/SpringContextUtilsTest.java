package ubic.gemma.core.context;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SpringContextUtilsTest {

    @Test
    public void testPrepareContext() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.getEnvironment().addActiveProfile( EnvironmentProfiles.TEST );
        SpringContextUtils.prepareContext( context );
    }

    @Test
    public void testPrepareContextWhenMoreThanOneEnvironmentProfileIsActive() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.getEnvironment().addActiveProfile( EnvironmentProfiles.TEST );
        context.getEnvironment().addActiveProfile( EnvironmentProfiles.DEV );
        assertThrows( IllegalStateException.class, () -> SpringContextUtils.prepareContext( context ) );
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