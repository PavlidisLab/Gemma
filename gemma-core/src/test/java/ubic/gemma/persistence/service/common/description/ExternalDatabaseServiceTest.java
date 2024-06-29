package ubic.gemma.persistence.service.common.description;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.util.test.BaseIntegrationTest;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertNotNull;

public class ExternalDatabaseServiceTest extends BaseIntegrationTest {

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Autowired
    private UserManager userManager;

    /* fixtures */
    private ExternalDatabase ed, ed2;

    @After
    public void tearDown() {
        if ( ed != null ) {
            externalDatabaseService.remove( ed );
        }
        if ( ed2 != null ) {
            externalDatabaseService.remove( ed2 );
        }
    }

    @Test
    public void testUpdateReleaseDetails() throws MalformedURLException {
        ed = externalDatabaseService.create( ExternalDatabase.Factory.newInstance( "test", DatabaseType.OTHER ) );
        User currentUser = userManager.getCurrentUser();
        assertThat( currentUser ).isNotNull();
        ExternalDatabase externalDatabase = externalDatabaseService.findByNameWithAuditTrail( "test" );
        assertNotNull( externalDatabase );
        assertThat( externalDatabase.getAuditTrail() ).isNotNull();
        assertThat( externalDatabase.getAuditTrail().getEvents() ).hasSize( 1 );
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
        // make sure that the last updated date is properly stored
        Date lastUpdated = externalDatabase.getLastUpdated();
        assertThat( lastUpdated ).isNotNull();
        externalDatabase = externalDatabaseService.find( externalDatabase );
        assertThat( externalDatabase ).isNotNull();
        assertThat( externalDatabase.getLastUpdated() ).isNotNull();
        assertThat( externalDatabase.getLastUpdated().getTime() )
                .isEqualTo( lastUpdated.getTime() );
    }

    /**
     * This test applies to fixtures found in init-entities.sql.
     */
    @Test
    public void testExternalDatabaseWithRelatedDatabases() {
        ExternalDatabase hg19 = externalDatabaseService.findByName( "hg19" );
        assertNotNull( hg19 );
        assertThat( hg19.getExternalDatabases() )
                .hasSize( 1 )
                .extracting( "name" ).contains( "hg19 annotations" );
        ExternalDatabase hg38 = externalDatabaseService.findByName( "hg38" );
        assertNotNull( hg38 );
        assertThat( hg38.getExternalDatabases() )
                .hasSize( 2 )
                .extracting( "name" ).contains( "hg38 annotations", "hg38 RNA-Seq annotations" );
    }

    @Test
    public void testUpdateExternalDatabaseDontCascadeToRelatedDatabases() {
        ed = externalDatabaseService.create( ExternalDatabase.Factory.newInstance( "ed", DatabaseType.OTHER ) );
        ed2 = ExternalDatabase.Factory.newInstance( "ed2", DatabaseType.OTHER );
        ed2.setExternalDatabases( Collections.singleton( ed ) );
        ed2 = externalDatabaseService.create( ed2 );
        ed2.setDescription( "1234" );
        externalDatabaseService.update( ed2 );
        assertThat( ed2.getExternalDatabases() ).contains( ed );
        assertThat( ed2.getAuditTrail().getEvents() ).hasSize( 2 );
        ed = externalDatabaseService.findByNameWithAuditTrail( ed.getName() );
        assertNotNull( ed );
        assertThat( ed.getAuditTrail().getEvents() ).hasSize( 1 );
    }
}