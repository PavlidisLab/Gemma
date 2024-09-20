package ubic.gemma.persistence.initialization;

import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.CompositeDatabasePopulator;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import ubic.gemma.persistence.hibernate.H2Dialect;
import ubic.gemma.persistence.hibernate.LocalSessionFactoryBean;
import ubic.gemma.persistence.hibernate.MySQL57InnoDBDialect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Populates the database schema.
 * @author poirigui
 */
@CommonsLog
public class DatabaseSchemaPopulator extends CompositeDatabasePopulator {

    public DatabaseSchemaPopulator( LocalSessionFactoryBean sessionFactoryBean, String vendor ) {
        Configuration configuration = sessionFactoryBean.getConfiguration();
        Dialect dialect;
        if ( configuration.getProperty( "hibernate.dialect" ) != null ) {
            try {
                dialect = ( Dialect ) Class.forName( configuration.getProperty( "hibernate.dialect" ) )
                        .getConstructor().newInstance();
            } catch ( Exception e ) {
                throw new RuntimeException( e );
            }
        } else {
            dialect = vendor.equals( "h2" ) ? new H2Dialect() : new MySQL57InnoDBDialect();
        }
        ResourceDatabasePopulator rdp = new ResourceDatabasePopulator() {
            @Override
            public void populate( Connection connection ) throws SQLException {
                log.info( "Populating ACLs, indices, additional tables, etc..." );
                super.populate( connection );
            }
        };
        rdp.addScript( new ClassPathResource( "/sql/init-acls.sql" ) );
        rdp.addScript( new ClassPathResource( "/sql/init-entities.sql" ) );
        rdp.addScript( new ClassPathResource( "/sql/" + vendor + "/init-entities.sql" ) );
        addPopulators( new HibernateSchemaPopulator( configuration, dialect ), rdp );
    }

    /**
     * Populate the database with the Hibernate DDL schema.
     * @author poirigui
     */
    private static class HibernateSchemaPopulator implements DatabasePopulator {

        private final Configuration configuration;
        private final Dialect dialect;

        public HibernateSchemaPopulator( Configuration configuration, Dialect dialect ) {
            this.configuration = configuration;
            this.dialect = dialect;
        }

        @Override
        public void populate( Connection connection ) throws SQLException {
            log.info( "Populating Hibernate schema..." );
            String[] ddl = configuration.generateSchemaCreationScript( dialect );
            for ( String sql : ddl ) {
                try ( PreparedStatement ps = connection.prepareStatement( sql ) ) {
                    ps.execute();
                }
            }
        }
    }
}
