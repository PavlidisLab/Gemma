package ubic.gemma.persistence.hibernate;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.MySQLDialect;

/**
 * Gemma's MySQL dialect, pinned to MySQL 5.7 / InnoDB semantics.
 * <p>
 * Pre-phase-2 this class extended the now-removed {@code org.hibernate.dialect.MySQL57InnoDBDialect} and registered
 * a {@code bitwise_and} SQL function via the also-removed {@code SQLFunction} contract. Hibernate 6 collapsed the
 * MySQL family into a single {@link MySQLDialect} parameterised by {@link DatabaseVersion}, and replaced the
 * function-registration API. The {@code bitwise_and} HQL function is no longer registered; callers should render
 * the {@code &} operator directly (see {@code AclLinterServiceImpl}).
 */
public class MySQL57InnoDBDialect extends MySQLDialect {

    public MySQL57InnoDBDialect() {
        super( DatabaseVersion.make( 5, 7 ) );
    }
}
