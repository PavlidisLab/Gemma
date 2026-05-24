package ubic.gemma.rest.monitoring.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DbHealthIndicatorTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    private DbHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new DbHealthIndicator( dataSource );
    }

    @Test
    void getName_isDb() {
        assertEquals( "db", indicator.getName() );
    }

    @Test
    void happyPath_isValidTrue_yieldsUpWithMetadata() throws SQLException {
        when( dataSource.getConnection() ).thenReturn( connection );
        when( connection.isValid( 2 ) ).thenReturn( true );
        when( connection.getMetaData() ).thenReturn( metaData );
        when( metaData.getDatabaseProductName() ).thenReturn( "MySQL" );
        when( metaData.getDatabaseProductVersion() ).thenReturn( "8.0.36" );

        HealthResult r = indicator.check();

        assertEquals( HealthResult.Status.UP, r.getStatus() );
        assertEquals( "MySQL", r.getDetails().get( "database" ) );
        assertEquals( "8.0.36", r.getDetails().get( "databaseVersion" ) );
        assertEquals( 2, r.getDetails().get( "validationTimeoutSeconds" ) );
        assertTrue( r.getDetails().containsKey( "elapsedMs" ) );
        verify( connection ).close();
    }

    @Test
    void metadataThrows_butIsValidTrue_stillReturnsUp() throws SQLException {
        when( dataSource.getConnection() ).thenReturn( connection );
        when( connection.isValid( 2 ) ).thenReturn( true );
        when( connection.getMetaData() ).thenThrow( new SQLException( "no metadata" ) );

        HealthResult r = indicator.check();

        assertEquals( HealthResult.Status.UP, r.getStatus() );
        // metadata fields skipped, validation/elapsed still present
        assertEquals( 2, r.getDetails().get( "validationTimeoutSeconds" ) );
        assertTrue( r.getDetails().containsKey( "elapsedMs" ) );
        verify( connection ).close();
    }

    @Test
    void isValidFalse_yieldsDown() throws SQLException {
        when( dataSource.getConnection() ).thenReturn( connection );
        when( connection.isValid( 2 ) ).thenReturn( false );

        HealthResult r = indicator.check();

        assertEquals( HealthResult.Status.DOWN, r.getStatus() );
        assertEquals( "JDBC connection isValid() returned false", r.getDetails().get( "error" ) );
        verify( connection ).close();
    }

    @Test
    void getConnectionThrows_yieldsDown() throws SQLException {
        when( dataSource.getConnection() ).thenThrow( new SQLException( "pool exhausted" ) );

        HealthResult r = indicator.check();

        assertEquals( HealthResult.Status.DOWN, r.getStatus() );
        assertTrue( ( ( String ) r.getDetails().get( "error" ) ).contains( "pool exhausted" ) );
    }
}
