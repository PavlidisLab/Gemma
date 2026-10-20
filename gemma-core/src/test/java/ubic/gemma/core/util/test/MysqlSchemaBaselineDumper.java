package ubic.gemma.core.util.test;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import ubic.gemma.persistence.hibernate.MySQL57InnoDBDialect;

import java.io.File;

/**
 * One-off Flyway baseline generator for the MySQL/InnoDB path. Sibling of
 * {@link SchemaBaselineDumper} (which targets H2 + MODE=MYSQL). Drives Hibernate via the
 * {@code MySQL57InnoDBDialect} and writes the CREATE DDL using JPA's standard
 * {@code jakarta.persistence.schema-generation.scripts.*} properties — no live MySQL connection
 * needed (Hibernate constructs the SQL dialect-only).
 * <p>
 * Output defaults to {@code src/main/resources/db/migration/mysql/V1__mysql_baseline.sql}.
 * <p>
 * Run from the {@code gemma-core} directory:
 * <pre>
 * mvn -q test-compile
 * mvn -q exec:java -Dexec.mainClass=ubic.gemma.core.util.test.MysqlSchemaBaselineDumper \
 *     -Dexec.classpathScope=test
 * </pre>
 *
 * @see SchemaBaselineDumper
 */
public final class MysqlSchemaBaselineDumper {

    public static void main( String[] args ) throws Exception {
        String outRel = args.length > 0
                ? args[0]
                : "src/main/resources/db/migration/mysql/V1__mysql_baseline.sql";
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

        Configuration cfg = new Configuration();
        cfg.configure( "hibernate.cfg.xml" );
        cfg.setProperty( "hibernate.dialect", MySQL57InnoDBDialect.class.getName() );
        cfg.setProperty( "hibernate.show_sql", "false" );
        cfg.setProperty( "hibernate.format_sql", "false" );
        cfg.setProperty( "hibernate.cache.use_second_level_cache", "false" );
        cfg.setProperty( "hibernate.cache.use_query_cache", "false" );
        // Avoid Hibernate trying to introspect a non-existent JDBC connection during metadata build.
        cfg.setProperty( "hibernate.boot.allow_jdbc_metadata_access", "false" );
        cfg.setProperty( "hibernate.temp.use_jdbc_metadata_defaults", "false" );

        // JPA-portable schema-export properties: the DDL is written to disk as a side effect of
        // SessionFactory boot, no live execution required.
        cfg.setProperty( "jakarta.persistence.schema-generation.scripts.action", "create" );
        cfg.setProperty( "jakarta.persistence.schema-generation.scripts.create-target",
                out.getAbsolutePath() );
        cfg.setProperty( "hibernate.hbm2ddl.auto", "none" );

        SessionFactory sf = cfg.buildSessionFactory();
        sf.close();

        System.out.println( "[MysqlSchemaBaselineDumper] wrote " + out );
    }

    private MysqlSchemaBaselineDumper() {}
}
