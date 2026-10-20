package ubic.gemma.rest.monitoring.health;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Database health probe. Borrows a connection from the configured (HikariCP) {@link DataSource}
 * and asks the JDBC driver to validate it with a 2-second timeout. Reports DOWN on any failure.
 *
 * @author Phase 3 actuator wiring
 */
@Component
@CommonsLog
public class DbHealthIndicator implements HealthIndicator {

    private static final int VALIDATION_TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;

    @Autowired
    public DbHealthIndicator( DataSource dataSource ) {
        this.dataSource = dataSource;
    }

    @Override
    public String getName() {
        return "db";
    }

    @Override
    public HealthResult check() {
        long start = System.currentTimeMillis();
        try ( Connection conn = dataSource.getConnection() ) {
            if ( !conn.isValid( VALIDATION_TIMEOUT_SECONDS ) ) {
                return HealthResult.down( "JDBC connection isValid() returned false" );
            }
            Map<String, Object> details = new LinkedHashMap<>();
            try {
                DatabaseMetaData md = conn.getMetaData();
                details.put( "database", md.getDatabaseProductName() );
                details.put( "databaseVersion", md.getDatabaseProductVersion() );
            } catch ( SQLException e ) {
                // metadata is best-effort; don't fail health on it
                log.debug( "Could not read JDBC metadata for health probe", e );
            }
            details.put( "validationTimeoutSeconds", VALIDATION_TIMEOUT_SECONDS );
            details.put( "elapsedMs", System.currentTimeMillis() - start );
            return HealthResult.up( details );
        } catch ( SQLException e ) {
            return HealthResult.down( "JDBC connection failed: " + e.getMessage() );
        }
    }
}
