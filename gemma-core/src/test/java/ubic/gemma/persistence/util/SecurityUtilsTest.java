package ubic.gemma.persistence.util;

import gemma.gsec.util.SecurityUtil;
import org.junit.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

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
