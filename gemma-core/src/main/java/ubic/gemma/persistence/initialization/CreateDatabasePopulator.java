package ubic.gemma.persistence.initialization;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.init.DatabasePopulator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Create a new database and drop an existing one if desired.
 * <p>
 * This populator needs to be run with a data source that has been passed through {@link BootstrappedDataSourceFactory},
 * because the database might not exist.
 * @author poirigui
 * @see BootstrappedDataSourceFactory
 */
@Slf4j
public class CreateDatabasePopulator implements DatabasePopulator {

    private final String databaseName;
    private boolean dropIfExists = false;

    public CreateDatabasePopulator( String databaseName ) {
        this.databaseName = databaseName;
    }

    @Override
    public void populate( Connection connection ) throws SQLException {
        // Phase 2 multi-context guard: only the first Spring ApplicationContext in this JVM gets
        // to drop + create the test DB. Subsequent contexts find the DB already there (with its
        // schema materialized + seed data loaded by earlier siblings) and skip. See
        // TestBootstrapState for the rationale.
        if ( !TestBootstrapState.claimDatabaseCreation() ) {
            log.info( "Test database " + databaseName + " already created by an earlier ApplicationContext in this JVM; skipping drop+create." );
            return;
        }
        if ( dropIfExists ) {
            try ( PreparedStatement ps = connection.prepareStatement( "drop database if exists " + databaseName ) ) {
                log.warn( "Dropping database " + databaseName + "..." );
                ps.execute();
            }
        }
        try ( PreparedStatement ps = connection.prepareStatement( "create database " + databaseName + " character set utf8mb4" ) ) {
            log.info( "Creating database " + databaseName );
            ps.execute();
        }
    }

    public void setDropIfExists( boolean dropIfExists ) {
        this.dropIfExists = dropIfExists;
    }
}
