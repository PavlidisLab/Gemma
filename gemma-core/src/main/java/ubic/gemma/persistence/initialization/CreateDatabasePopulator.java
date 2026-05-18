package ubic.gemma.persistence.initialization;

import lombok.extern.apachecommons.CommonsLog;
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
@CommonsLog
public class CreateDatabasePopulator implements DatabasePopulator {

    private final String databaseName;
    private boolean dropIfExists = false;

    /**
     * Track whether THIS JVM has already dropped the named test database. Spring's TestContext
     * cache lets integration tests with different @ContextConfiguration shapes each trigger a
     * fresh DataSourceInitializer; dropping on every one invalidates the tables that a
     * previously-cached EMF still expects to use. Drop only on the first context bootstrap of
     * the JVM.
     */
    private static final java.util.concurrent.ConcurrentHashMap<String, Boolean> ALREADY_DROPPED = new java.util.concurrent.ConcurrentHashMap<>();

    public CreateDatabasePopulator( String databaseName ) {
        this.databaseName = databaseName;
    }

    @Override
    public void populate( Connection connection ) throws SQLException {
        if ( dropIfExists && ALREADY_DROPPED.putIfAbsent( databaseName, Boolean.TRUE ) == null ) {
            try ( PreparedStatement ps = connection.prepareStatement( "drop database if exists " + databaseName ) ) {
                log.warn( "Dropping database " + databaseName + " (first context bootstrap of this JVM)..." );
                ps.execute();
            }
        }
        try ( PreparedStatement ps = connection.prepareStatement( "create database if not exists " + databaseName + " character set utf8mb4" ) ) {
            log.info( "Creating database " + databaseName + " (if not exists)" );
            ps.execute();
        }
    }

    public void setDropIfExists( boolean dropIfExists ) {
        this.dropIfExists = dropIfExists;
    }
}
