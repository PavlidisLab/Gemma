package ubic.gemma.core.util.test;

import gemma.gsec.authentication.ManualAuthenticationService;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.SpringProfiles;

import static org.mockito.Mockito.mock;

/**
 * Minimal setup
 */
@ActiveProfiles({ "cli", SpringProfiles.TEST })
public abstract class BaseCliTest extends AbstractJUnit4SpringContextTests {

    /**
     * Basic context configuration
     */
    public static class BaseCliTestContextConfiguration {

        @Bean
        public ManualAuthenticationService manualAuthenticationService() {
            return mock( ManualAuthenticationService.class );
        }

        @Bean
        public AuditTrailService auditTrailService() {
            return mock( AuditTrailService.class );
        }

        @Bean
        public AuditEventService auditEventService() {
            return mock( AuditEventService.class );
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock( ExpressionExperimentService.class );
        }

        @Bean
        public Persister persisterHelper() {
            return mock( Persister.class );
        }

        @Bean
        public TestAuthenticationUtils testAuthenticationUtils() {
            return new TestAuthenticationUtilsImpl();
        }

        @Bean
        public UserManager userManager() {
            return mock( UserManager.class );
        }

        @Bean
        public AuthenticationManager authenticationManager() {
            return mock( AuthenticationManager.class );
        }
    }

    @Autowired
    private TestAuthenticationUtils testAuthenticationUtils;

    @Before
    public void setUpAuthentication() {
        testAuthenticationUtils.runAsAdmin();
    }
}
