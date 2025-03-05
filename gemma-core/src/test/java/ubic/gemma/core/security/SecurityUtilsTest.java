package ubic.gemma.core.security;

import gemma.gsec.util.SecurityUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.intercept.RunAsImplAuthenticationProvider;
import org.springframework.security.access.intercept.RunAsManager;
import org.springframework.security.access.intercept.RunAsManagerImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.context.TestComponent;

import static org.junit.Assert.assertTrue;

@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class SecurityUtilsTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    static class SecurityUtilsTestContextConfiguration {

    }

    @Test
    @WithMockUser(authorities = { "GROUP_ADMIN" })
    public void testIsAdmin() {
        assertTrue( SecurityUtil.isUserAdmin() );
    }

    @Test
    @WithMockUser
    public void testIsUserLoggedIn() {
        assertTrue( SecurityUtil.isUserLoggedIn() );
    }

    @Test
    @WithMockUser(authorities = { "GROUP_RUN_AS_ADMIN" })
    public void testRunAsAdmin() {
        assertTrue( SecurityUtil.isRunningAsAdmin() );
    }
}
