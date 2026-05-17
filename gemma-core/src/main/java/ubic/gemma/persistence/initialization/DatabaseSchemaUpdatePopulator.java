package ubic.gemma.persistence.initialization;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.util.Assert;

import java.sql.Connection;

/**
 * Populates an existing database with any incremental schema changes implied by the current
 * Hibernate mapping.
 * <p>
 * The Hibernate 4 implementation used {@code org.hibernate.tool.hbm2ddl.DatabaseMetadata} and
 * {@code SchemaUpdateScript}, both removed in Hibernate 5 in favour of the new
 * {@link org.hibernate.tool.schema.spi.SchemaManagementTool}-based metadata model. Phase 2 also
 * moved Spring wiring from a {@code LocalSessionFactoryBean} (whose {@code Configuration} was
 * the old constructor arg here) to a JPA {@code LocalContainerEntityManagerFactoryBean}, so the
 * Configuration arg is gone too.
 * <p>
 * This class is currently a no-op stub on the renovations branch; production deployments rely on
 * the versioned scripts under {@code gemma-core/src/main/resources/sql/migrations/} (applied
 * manually by the DBA) rather than this populator. Reinstating automatic schema updates is
 * future work — likely Flyway (see {@code RENOVATIONS.md}).
 */
@CommonsLog
public class DatabaseSchemaUpdatePopulator implements DatabasePopulator {

    public DatabaseSchemaUpdatePopulator( String vendor ) {
        Assert.isTrue( vendor.equals( "mysql" ) || vendor.equals( "h2" ), "expected true" );
    }

    @Override
    public void populate( Connection connection ) {
        log.info( "DatabaseSchemaUpdatePopulator is a no-op on the renovations branch; use the sql/migrations/ scripts instead." );
    }
}
