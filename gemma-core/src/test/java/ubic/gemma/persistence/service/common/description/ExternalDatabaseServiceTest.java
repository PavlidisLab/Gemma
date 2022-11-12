package ubic.gemma.persistence.service.common.description;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ExternalDatabaseServiceTest extends BaseSpringContextTest {

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Autowired
    private UserManager userManager;

    private ExternalDatabase ed;

    @Before
    public void setUp() {
        ed = externalDatabaseService.create( ExternalDatabase.Factory.newInstance( "test", DatabaseType.OTHER ) );
    }

    @After
    public void tearDown() {
        externalDatabaseService.remove( ed );
    }

    @Test
    public void test() throws MalformedURLException {
        User currentUser = userManager.getCurrentUser();
        assertThat( currentUser ).isNotNull();
        ExternalDatabase externalDatabase = externalDatabaseService.findByNameWithAuditTrail( "test" );
        assertThat( externalDatabase ).isEqualTo( ed );
        externalDatabaseService.updateReleaseDetails( externalDatabase, "123", new URL( "http://example.com/test" ), "Yep", new Date() );
        assertThat( externalDatabase )
                .isNotNull()
                .hasFieldOrPropertyWithValue( "name", "test" )
                .hasFieldOrPropertyWithValue( "releaseVersion", "123" )
                .hasFieldOrPropertyWithValue( "releaseUrl", new URL( "http://example.com/test" ) );
        assertThat( externalDatabase.getAuditTrail().getEvents() )
                .hasSize( 3 )
                .extracting( "action", "performer" )
                .containsExactly(
                        tuple( AuditAction.CREATE, currentUser ), // from AuditAdvice on create()
                        tuple( AuditAction.UPDATE, currentUser ), // manually inserted
                        tuple( AuditAction.UPDATE, currentUser ) ); // from AuditAdvice on update()
        assertThat( externalDatabase.getAuditTrail().getEvents().get( 1 ).getNote() )
                .isEqualTo( "Yep" );
    }
}