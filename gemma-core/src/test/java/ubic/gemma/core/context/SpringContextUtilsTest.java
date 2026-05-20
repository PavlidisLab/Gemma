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
    public void clearRequireExplicitProfileSystemProperty() {
        System.clearProperty( SpringContextUtils.REQUIRE_EXPLICIT_PROFILE_PROPERTY );
    }

    @Test
    public void testPrepareContextFallsBackToDevWithLoudWarnWhenNoProfileActive() {
        GenericApplicationContext context = new GenericApplicationContext();
        SpringContextUtils.prepareContext( context );
        assertTrue( context.getEnvironment().acceptsProfiles( EnvironmentProfiles.DEV ) );
    }

    @Test
    public void testPrepareContextFailsFastWhenRequireExplicitProfileIsSet() {
        System.setProperty( SpringContextUtils.REQUIRE_EXPLICIT_PROFILE_PROPERTY, "true" );
        GenericApplicationContext context = new GenericApplicationContext();
        assertThrows( IllegalStateException.class, () -> SpringContextUtils.prepareContext( context ) );
    }

    @Test
    public void testPrepareContextDoesNotFailFastWhenProfileExplicitlyActive() {
        System.setProperty( SpringContextUtils.REQUIRE_EXPLICIT_PROFILE_PROPERTY, "true" );
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