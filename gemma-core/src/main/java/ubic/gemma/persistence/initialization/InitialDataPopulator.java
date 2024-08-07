package ubic.gemma.persistence.initialization;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.CompositeDatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Populate some initial data for tests.
 * @author poirigui
 */
@CommonsLog
public class InitialDataPopulator extends CompositeDatabasePopulator {

    private final boolean slim;

    public InitialDataPopulator( boolean slim ) {
        this.slim = slim;
        ResourceDatabasePopulator initDataPopulator = new ResourceDatabasePopulator();
        if ( slim ) {
            initDataPopulator.addScript( new ClassPathResource( "/sql/init-data-slim.sql" ) );
        } else {
            // this file contains procedures, so splitting by ';' isn't adequate, statements are separated by newlines
            initDataPopulator.setSeparator( "\n" );
            initDataPopulator.addScript( new ClassPathResource( "/sql/init-data.sql" ) );
        }
        addPopulators( initDataPopulator );
    }

    @Override
    public void populate( Connection connection ) throws SQLException {
        if ( slim ) {
            log.info( "Populating initial slim data..." );
        } else {
            log.info( "Populating initial data..." );
        }
        super.populate( connection );
        if ( !slim ) {
            // TODO: make this configurable
            log.info( "An agent was created with username 'gemmaAgent' and password 'XXXXXXXX'." );
            log.info( "An administrator was created with username 'administrator' and password 'administrator'." );
        }
    }
}
