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

    public CreateDatabasePopulator( String databaseName ) {
        this.databaseName = databaseName;
    }

    @Override
    public void populate( Connection connection ) throws SQLException {
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
