package ubic.gemma.persistence.initialization;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.jdbc.datasource.init.DatabasePopulator;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Create a new database and drop an existing one if desired.
 * <p>
 * This populator needs to be run with a {@link BootstrappedDataSourceInitializer} because the database might not exist.
 * @author poirigui
 * @see BootstrappedDataSourceInitializer
 */
@CommonsLog
public class CreateDatabasePopulator implements DatabasePopulator {

    private final String databaseName;
    private boolean dropIfExists = false;

    public CreateDatabasePopulator( String databaseName ) {
        this.databaseName = databaseName;
    }

    @Override
    public void populate( Connection connection ) throws SQLException {
        if ( dropIfExists ) {
            log.warn( "Dropping database " + databaseName + "..." );
            connection.prepareStatement( "drop database if exists " + databaseName ).execute();
        }
        log.info( "Creating database " + databaseName );
        connection.prepareStatement( "create database " + databaseName + " character set utf8mb4" ).execute();
    }

    public void setDropIfExists( boolean dropIfExists ) {
        this.dropIfExists = dropIfExists;
    }
}
