package ubic.gemma.persistence.initialization;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.CompositeDatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Populate some initial data for tests.
 * @author poirigui
 */
@Slf4j
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
        // Phase 2 multi-context guard: every Spring ApplicationContext drags a fresh
        // dataSourceInitializer through the bean factory, but the underlying gemdtest DB is a
        // process-wide singleton. Seed data must be inserted exactly once per JVM or we hit
        // primary-key collisions on the second context. See TestBootstrapState.
        //
        // The guard only applies to the integration tier (full init-data.sql); the slim variant
        // is used by BaseDatabaseTest with its own ephemeral H2 in-memory database, which is
        // (re-)created on every Spring context and therefore needs the seed every time.
        if ( !slim && !TestBootstrapState.claimDataSeeding() ) {
            log.info( "Initial seed data already inserted by an earlier ApplicationContext in this JVM; skipping." );
            return;
        }
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
