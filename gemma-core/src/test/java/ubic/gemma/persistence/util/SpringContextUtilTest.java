package ubic.gemma.persistence.util;

import org.junit.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class SpringContextUtilTest {

    @Test
    public void testPrepareContext() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.getEnvironment().addActiveProfile( EnvironmentProfiles.TEST );
        SpringContextUtil.prepareContext( context );
    }

    @Test(expected = IllegalStateException.class)
    public void testPrepareContextWhenMoreThanOneEnvironmentProfileIsActive() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.getEnvironment().addActiveProfile( EnvironmentProfiles.TEST );
        context.getEnvironment().addActiveProfile( EnvironmentProfiles.DEV );
        SpringContextUtil.prepareContext( context );
    }

    @Test(expected = IllegalStateException.class)
    public void testPrepareContextWithIncorrectSecurityContextHolderStrategyIsSet() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.getEnvironment().addActiveProfile( EnvironmentProfiles.TEST );
        try {
            SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_THREADLOCAL );
            SpringContextUtil.prepareContext( context );
        } finally {
            SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_INHERITABLETHREADLOCAL );
        }
    }
}