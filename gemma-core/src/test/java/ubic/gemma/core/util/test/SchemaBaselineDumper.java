package ubic.gemma.core.util.test;

import org.h2.Driver;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import ubic.gemma.persistence.hibernate.H2Dialect;

import javax.sql.DataSource;
import java.io.File;

/**
 * One-off Flyway baseline generator. Boots Hibernate the same way {@link BaseDatabaseTest} does
 * (H2 in {@code MODE=MYSQL}, native bootstrap from {@code hibernate.cfg.xml}, gsec mappings) and
 * writes the {@code CREATE} DDL to a target file via Hibernate's standard
 * {@code jakarta.persistence.schema-generation.scripts.*} properties.
 * <p>
 * The dump is taken from the in-memory metadata Hibernate would otherwise execute against the
 * database during {@code hbm2ddl.auto=create}; we additionally point a live H2 datasource at the
 * same SessionFactory so HB does not try to introspect a non-existent database.
 * <p>
 * Run from the {@code gemma-core} directory:
 * <pre>
 * mvn -q test-compile
 * mvn -q exec:java -Dexec.mainClass=ubic.gemma.core.util.test.SchemaBaselineDumper \
 *     -Dexec.classpathScope=test
 * </pre>
 * Output defaults to {@code src/main/resources/db/migration/h2/V1__hibernate_baseline.sql}.
 */
public final class SchemaBaselineDumper {

    public static void main( String[] args ) throws Exception {
        String outRel = args.length > 0
                ? args[0]
                : "src/main/resources/db/migration/h2/V1__hibernate_baseline.sql";
        File out = new File( outRel ).getAbsoluteFile();
        File parent = out.getParentFile();
        if ( parent != null && !parent.exists() ) {
            if ( !parent.mkdirs() ) {
                throw new IllegalStateException( "Could not create " + parent );
            }
        }
        if ( out.exists() && !out.delete() ) {
            throw new IllegalStateException( "Could not delete pre-existing " + out );
        }

        DataSource ds = new SimpleDriverDataSource(
                new Driver(),
                "jdbc:h2:mem:gemma_baseline_dump;MODE=MYSQL;DB_CLOSE_DELAY=-1" );

        Configuration cfg = new Configuration();
        cfg.getProperties().put( org.hibernate.cfg.AvailableSettings.DATASOURCE, ds );
        cfg.configure( "hibernate.cfg.xml" );
        cfg.setProperty( "hibernate.dialect", H2Dialect.class.getName() );
        cfg.setProperty( "hibernate.show_sql", "false" );
        cfg.setProperty( "hibernate.format_sql", "false" );
        // No L2 cache / query cache during dump
        cfg.setProperty( "hibernate.cache.use_second_level_cache", "false" );
        cfg.setProperty( "hibernate.cache.use_query_cache", "false" );

        // Drive schema export via JPA-portable scripts properties: Hibernate writes the CREATE DDL
        // to the target file as a side effect of SessionFactory boot, no live execution required.
        cfg.setProperty( "jakarta.persistence.schema-generation.scripts.action", "create" );
        cfg.setProperty( "jakarta.persistence.schema-generation.scripts.create-target",
                out.getAbsolutePath() );
        // hbm2ddl.auto=none — we don't want HB to also run CREATE against the live DB; the
        // schema-generation script output is what we want.
        cfg.setProperty( "hibernate.hbm2ddl.auto", "none" );

        SessionFactory sf = cfg.buildSessionFactory();
        sf.close();

        System.out.println( "[SchemaBaselineDumper] wrote " + out );
    }

    private SchemaBaselineDumper() {}
}
