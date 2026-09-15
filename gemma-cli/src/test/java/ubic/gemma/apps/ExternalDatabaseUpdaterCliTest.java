package ubic.gemma.apps;

import ubic.gemma.core.security.authentication.ManualAuthenticationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.cli.util.test.BaseCliTest5;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;

import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class ExternalDatabaseUpdaterCliTest extends BaseCliTest5 {

    @Configuration
    @TestComponent
    static class ExternalDatabaseUpdaterCliTestContextConfiguration {

        @Bean
        public ExternalDatabaseUpdaterCli externalDatabaseUpdaterCli() {
            return new ExternalDatabaseUpdaterCli();
        }

        @Bean
        public ExternalDatabaseService externalDatabaseService() {
            return mock( ExternalDatabaseService.class );
        }

        @Bean
        public UserManager userManager() {
            return mock( UserManager.class );
        }

        @Bean
        public ManualAuthenticationService manualAuthenticationService() {
            return mock();
        }

        @Bean
        public AuditTrailService auditTrailService() {
            return mock();
        }

        @Bean
        public GemmaRestApiClient gemmaRestApiClient() {
            return mock();
        }
    }

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Autowired
    private ExternalDatabaseUpdaterCli externalDatabaseUpdaterCli;

    @Autowired
    private UserManager userManager;

    private ExternalDatabase ed, ed2;

    @BeforeEach
    public void setUp() {
        ed = ExternalDatabase.Factory.newInstance( "test", DatabaseType.OTHER );
        ed2 = ExternalDatabase.Factory.newInstance( "test2", DatabaseType.OTHER );
    }

    @AfterEach
    public void tearDown() {
        externalDatabaseService.remove( ed );
    }

    @Test
    @WithMockUser
    public void test() throws MalformedURLException {
        User user = User.Factory.newInstance( "foo" );
        when( userManager.getCurrentUser() ).thenReturn( user );
        when( externalDatabaseService.findByNameWithAuditTrail( "test" ) ).thenReturn( ed );
        when( externalDatabaseService.findByNameWithExternalDatabases( "test2" ) ).thenReturn( ed2 );
        assertThat( externalDatabaseUpdaterCli )
                .withArguments( "--name", "test", "--description", "Youpi!", "--release", "--release-note", "Yep", "--release-version", "123", "--release-url", "http://example.com/test", "--parent-database", "test2" )
                .succeeds();
        verify( externalDatabaseService ).findByNameWithExternalDatabases( "test2" );
        verify( externalDatabaseService ).findByNameWithAuditTrail( "test" );
        assertThat( ed.getDescription() ).isEqualTo( "Youpi!" );
        verify( externalDatabaseService ).updateReleaseDetails( eq( ed ), eq( "123" ), eq( new URL( "http://example.com/test" ) ), eq( "Yep" ), any() );
        verify( externalDatabaseService ).update( ed2 );
    }
}