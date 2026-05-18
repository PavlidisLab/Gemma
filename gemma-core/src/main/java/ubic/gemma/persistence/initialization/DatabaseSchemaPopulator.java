package ubic.gemma.persistence.initialization;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.CompositeDatabasePopulator;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.Assert;

import java.sql.Connection;

/**
 * Populates the database schema.
 * <p>
 * Renovations Phase 2: previously took a {@code LocalSessionFactoryBean} to pull a Hibernate
 * {@code Configuration} for DDL generation. That path is gone — Hibernate 6 builds the schema via
 * {@code org.hibernate.tool.schema.spi.SchemaCreator} (driven from {@code MetadataSources}) and
 * Spring's wiring is JPA-based. Production schemas come from
 * {@code gemma-core/src/main/resources/sql/migrations/*.sql} (applied by the DBA), so the
 * Hibernate-DDL portion of this populator is a no-op; tests rely on the JPA EMF's
 * {@code hibernate.hbm2ddl.auto=create} property instead.
 *
 * @author poirigui
 */
@CommonsLog
public class DatabaseSchemaPopulator extends CompositeDatabasePopulator {

    public DatabaseSchemaPopulator( String vendor ) {
        Assert.isTrue( vendor.equals( "mysql" ) || vendor.equals( "h2" ), "expected true" );
        ResourceDatabasePopulator rdp = new ResourceDatabasePopulator() {
            @Override
            public void populate( Connection connection ) {
                // Phase 2 multi-context guard: index creation + ACL seed INSERTs must not re-run on
                // the second ApplicationContext (would explode with duplicate-key / duplicate-index
                // errors). The InitialDataPopulator that runs immediately after has the same
                // guard, so the entire schema-extras bootstrap is a once-per-JVM affair.
                if ( !TestBootstrapState.claimSchemaExtras() ) {
                    log.info( "Schema extras (ACLs, indices, additional tables) already populated in this JVM; skipping." );
                    return;
                }
                log.info( "Populating ACLs, indices, additional tables, etc..." );
                super.populate( connection );
            }
        };
        rdp.addScript( new ClassPathResource( "/sql/init-acls.sql" ) );
        rdp.addScript( new ClassPathResource( "/sql/init-entities.sql" ) );
        rdp.addScript( new ClassPathResource( "/sql/" + vendor + "/init-entities.sql" ) );
        addPopulators( new HibernateSchemaPopulator(), rdp );
    }

    /**
     * Schema population is delegated to Hibernate's hbm2ddl.auto on the JPA EMF (tests) or
     * sql/migrations/*.sql (production); this inner class is a no-op kept for callsite shape.
     */
    private static class HibernateSchemaPopulator implements DatabasePopulator {
        @Override
        public void populate( Connection connection ) {
            log.info( "HibernateSchemaPopulator is a no-op; schema comes from hbm2ddl.auto (tests) or sql/migrations/ (prod)." );
        }
    }
}
