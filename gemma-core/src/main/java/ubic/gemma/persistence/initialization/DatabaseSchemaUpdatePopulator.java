package ubic.gemma.persistence.initialization;

import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.cfg.Configuration;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.util.Assert;

import java.sql.Connection;

/**
 * Populates an existing database with any incremental schema changes implied by the current Hibernate mapping.
 * <p>
 * The Hibernate 4 implementation used {@code org.hibernate.tool.hbm2ddl.DatabaseMetadata} and
 * {@code SchemaUpdateScript}, both of which were removed in Hibernate 5 in favour of the new
 * {@link org.hibernate.tool.schema.spi.SchemaManagementTool}-based metadata model. This class is currently a no-op
 * stub on the renovations branch; production deployments rely on the versioned scripts under
 * {@code gemma-core/src/main/resources/sql/migrations/} (applied manually by the DBA) rather than this populator.
 * <p>
 * Reinstating automatic schema updates against Hibernate 5+ is future renovation work — likely by integrating Flyway
 * (see {@code RENOVATIONS.md}) rather than reviving Hibernate's built-in tool.
 */
@CommonsLog
public class DatabaseSchemaUpdatePopulator implements DatabasePopulator {

    public DatabaseSchemaUpdatePopulator( Configuration configuration, String vendor ) {
        Assert.isTrue( vendor.equals( "mysql" ) || vendor.equals( "h2" ) );
        // Configuration and vendor are accepted to preserve the constructor signature callers rely on.
    }

    @Override
    public void populate( Connection connection ) {
        // Spring 4 dropped the SQLException from this method; Hibernate 5 removed DatabaseMetadata. This stub does
        // nothing — the production schema update workflow has moved to the file-based migrations under
        // gemma-core/src/main/resources/sql/migrations/.
        log.info( "DatabaseSchemaUpdatePopulator is a no-op on the renovations branch; use the sql/migrations/ scripts instead." );
    }
}
