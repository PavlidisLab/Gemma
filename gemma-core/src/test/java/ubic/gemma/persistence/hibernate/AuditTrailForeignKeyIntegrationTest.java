package ubic.gemma.persistence.hibernate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseIntegrationTest5;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code AUDIT_TRAIL.LAST_EVENT_FK} must be {@code ON DELETE SET NULL} in a schema Hibernate built.
 *
 * <p>Nothing in a JPA mapping can say this — HBM XML's {@code <many-to-one on-delete>} accepts only
 * {@code cascade} or {@code noaction}, and JPA has no portable knob at all. The rule reached an
 * hbm2ddl-built schema through two {@code <database-object>} elements in {@code AuditTrail.hbm.xml},
 * which was the last {@code .hbm.xml} in the repository and cost an {@code HHH90000028} deprecation
 * warning on every boot. It is now an {@code AuxiliaryDatabaseObject} registered from
 * {@link HibernateConfig}, and this test is what says the move kept the behaviour.</p>
 *
 * <p>🛑 <b>Asserted against {@code information_schema}, not against the mapping.</b> The failure this
 * guards is a schema generated differently, so reading it back from the database is the only evidence
 * that means anything — a test inspecting Hibernate's metadata would pass whether or not the DDL ever
 * ran. Without the rule the constraint defaults to RESTRICT and deleting an {@code AuditEvent} a trail
 * still points at fails with a ConstraintViolation, while the Flyway path
 * ({@code V8__audit_trail_last_event_id.sql}) behaves correctly — the two schemas disagreeing, with
 * only the built-from-scratch one wrong.</p>
 */
public class AuditTrailForeignKeyIntegrationTest extends BaseIntegrationTest5 {

    @Autowired
    private DataSource dataSource;

    /**
     * Plain JDBC rather than a Hibernate session: the schema is what is under test, so nothing here
     * needs a persistence context, and reading it through one would only add a transaction to get
     * wrong.
     */
    @Test
    public void testTheLastEventForeignKeyDeletesToNull() throws Exception {
        String rule = null;
        try ( Connection c = dataSource.getConnection(); Statement st = c.createStatement() ) {
            try ( ResultSet rs = st.executeQuery(
                    "select DELETE_RULE from information_schema.REFERENTIAL_CONSTRAINTS "
                            + "where CONSTRAINT_SCHEMA = database() "
                            + "and CONSTRAINT_NAME = 'FK_AUDIT_TRAIL_LAST_EVENT'" ) ) {
                if ( rs.next() ) {
                    rule = rs.getString( 1 );
                }
            }
        }

        assertThat( rule )
                .as( "the auxiliary database object did not run; the FK is whatever Hibernate generated" )
                .isNotNull();
        assertThat( rule ).isEqualTo( "SET NULL" );
    }
}
