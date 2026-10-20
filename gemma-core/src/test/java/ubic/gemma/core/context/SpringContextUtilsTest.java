package ubic.gemma.core.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @AfterEach
    public void clearAllowDevFallbackSystemProperty() {
        System.clearProperty( SpringContextUtils.ALLOW_DEV_FALLBACK_PROPERTY );
    }

    @Test
    public void testPrepareContextFailsFastWhenNoProfileActive() {
        GenericApplicationContext context = new GenericApplicationContext();
        assertThrows( IllegalStateException.class, () -> SpringContextUtils.prepareContext( context ) );
    }

    @Test
    public void testPrepareContextFallsBackToDevWhenAllowDevFallbackIsSet() {
        System.setProperty( SpringContextUtils.ALLOW_DEV_FALLBACK_PROPERTY, "true" );
        GenericApplicationContext context = new GenericApplicationContext();
        SpringContextUtils.prepareContext( context );
        assertTrue( context.getEnvironment().acceptsProfiles( EnvironmentProfiles.DEV ) );
    }

    @Test
    public void testPrepareContextDoesNotFailFastWhenProfileExplicitlyActive() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.getEnvironment().addActiveProfile( EnvironmentProfiles.TEST );
        SpringContextUtils.prepareContext( context );
        assertTrue( context.getEnvironment().acceptsProfiles( EnvironmentProfiles.TEST ) );
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
