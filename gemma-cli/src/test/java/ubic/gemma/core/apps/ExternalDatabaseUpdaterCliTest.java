package ubic.gemma.core.apps;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.util.test.BaseCliTest;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.util.TestComponent;

import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class ExternalDatabaseUpdaterCliTest extends BaseCliTest {

    @Configuration
    @TestComponent
    static class ExternalDatabaseUpdaterCliTestContextConfiguration extends BaseCliTestContextConfiguration {

        @Bean
        public ExternalDatabaseUpdaterCli externalDatabaseUpdaterCli() {
            return new ExternalDatabaseUpdaterCli();
        }

        @Bean
        public ExternalDatabaseService externalDatabaseService() {
            return mock( ExternalDatabaseService.class );
        }
    }

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Autowired
    private ExternalDatabaseUpdaterCli externalDatabaseUpdaterCli;

    @Autowired
    private UserManager userManager;

    private ExternalDatabase ed;

    @Before
    public void setUp() {
        ed = ExternalDatabase.Factory.newInstance( "test", DatabaseType.OTHER );
    }

    @After
    public void tearDown() {
        externalDatabaseService.remove( ed );
    }

    @Test
    public void test() throws MalformedURLException {
        User user = User.Factory.newInstance();
        when( userManager.getCurrentUser() ).thenReturn( user );
        when( externalDatabaseService.findByNameWithAuditTrail( "test" ) ).thenReturn( ed );
        externalDatabaseUpdaterCli.executeCommand( new String[] { "--name", "test", "--description", "Youpi!", "--release-note", "Yep", "--release-version", "123", "--release-url", "http://example.com/test" } );
        verify( externalDatabaseService ).findByNameWithAuditTrail( "test" );
        assertThat( ed.getDescription() ).isEqualTo( "Youpi!" );
        verify( externalDatabaseService ).updateReleaseDetails( eq( ed ), eq( "123" ), eq( new URL( "http://example.com/test" ) ), eq( "Yep" ), any() );
    }
}