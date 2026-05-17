package ubic.gemma.persistence.initialization;

import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.CompositeDatabasePopulator;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.Assert;
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
        Assert.isTrue( vendor.equals( "mysql" ) || vendor.equals( "h2" ) , "expected true");
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
            dialect = vendor.equals( "mysql" ) ? new MySQL57InnoDBDialect() : new H2Dialect();
        }
        ResourceDatabasePopulator rdp = new ResourceDatabasePopulator() {
            @Override
            public void populate( Connection connection ) {
                // Spring 4: populate() no longer throws SQLException (uses ScriptException, unchecked).
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
        public void populate( Connection connection ) {
            // Hibernate 5 removed Configuration.generateSchemaCreationScript(Dialect). The proper replacement
            // is org.hibernate.tool.schema.spi.SchemaCreator driven from Metadata (via MetadataSources +
            // StandardServiceRegistryBuilder) — substantial enough to defer until the broader Hibernate 5
            // migration settles. Production schemas come from sql/migrations/*.sql so this is currently a
            // no-op on the renovations branch. Tests that rely on Hibernate-generated DDL will fall back to
            // hbm2ddl.auto=create-drop on the SessionFactory itself.
            log.info( "HibernateSchemaPopulator is a no-op on the renovations branch; use sql/migrations/ scripts or hbm2ddl.auto=create-drop." );
        }
    }
}
