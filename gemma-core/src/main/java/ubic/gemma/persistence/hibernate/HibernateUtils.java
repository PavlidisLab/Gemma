package ubic.gemma.persistence.hibernate;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.Query;
import ubic.gemma.core.config.Settings;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Pre-phase-2 this class did deep Hibernate-internal introspection
 * ({@code ClassMetadata}, {@code AbstractEntityPersister}, reflective field
 * access for {@code batchSize} and {@code eager}). Those APIs were removed in
 * Hibernate 6.
 * <p>
 * For now we return conservative defaults so callers compile and behave
 * predictably; the per-entity precision can be recovered later by walking the
 * JPA metamodel if it turns out we need it.
 */
@Slf4j
public class HibernateUtils {

    private static final String BATCH_FETCH_SIZE_SETTING = "gemma.hibernate.default_batch_fetch_size";

    /**
     * Obtain the configured default batch fetch size.
     */
    public static int getBatchSize( Class<?> entityClass, SessionFactory sessionFactory ) {
        if ( sessionFactory instanceof SessionFactoryImplementor ) {
            return ( ( SessionFactoryImplementor ) sessionFactory ).getSessionFactoryOptions()
                    .getDefaultBatchFetchSize();
        }
        return Settings.getInt( BATCH_FETCH_SIZE_SETTING, -1 );
    }

    /**
     * Stateless-ness inference used to be derived from the entity's mapping
     * (eager associations / non-lazy collections forced extra queries). Those
     * checks needed Hibernate's internal {@code ClassMetadata} which is gone
     * in Hibernate 6. Callers should not rely on a true here as a guarantee
     * that no additional queries will fire.
     */
    public static boolean isStateless( Class<?> entityClass, SessionFactory sessionFactory ) {
        return true;
    }

    public static boolean isStateless( Query<?> query, SessionFactory sessionFactory ) {
        return true;
    }

    /**
     * Hibernate 6 dropped {@code SessionFactory.getClassMetadata(Class)}. Most callers were just
     * after the JPA entity name — for our mappings that is the simple class name. This shim
     * preserves behavior for those callers.
     */
    public static String getEntityName( SessionFactory sessionFactory, Class<?> entityClass ) {
        try {
            return sessionFactory.getMetamodel().entity( entityClass ).getName();
        } catch ( IllegalArgumentException e ) {
            // not a managed entity; fall back to the simple name
            return entityClass.getSimpleName();
        }
    }

    /**
     * Lift the server-side statement timeout for the life of a streamed read, returning the action
     * that restores it.
     * <p>
     * The timeout is {@code gemma.db.hikari.maxExecutionTime}, applied by MySQL to a whole
     * statement. A streamed read finishes when its <em>consumer</em> finishes: measured against
     * MySQL 5.7.44, a stream under a 3-second cap was killed mid-drain at 16.7 s having delivered
     * 197,269 of 200,000 rows, and under {@code useCursorFetch=true} the kill lands during
     * materialization and delivers nothing at all. A cap sized for request-path queries would
     * therefore break bulk exports — single-cell MEX generation streams this way — partway
     * through, so streams opt out and the cap governs ordinary reads only.
     * <p>
     * Nothing is touched when no timeout is in force, which is the case for gemma-cli and for
     * tests. The variable is MySQL's, so the probe is skipped entirely on any other database —
     * issuing it against H2, which backs {@code BaseDatabaseTest5}, would raise an unknown-variable
     * error inside the caller's transaction on every streamed read.
     *
     * @return an idempotent restore action to register with {@link java.util.stream.Stream#onClose},
     * before the session is closed — after that the connection is back in the pool and would carry
     * the lifted value to its next borrower.
     */
    public static Runnable liftStatementTimeout( Session session ) {
        long[] previous = { 0L };
        try {
            session.doWork( connection -> {
                if ( !isMysql( connection ) ) {
                    return;
                }
                try ( Statement stmt = connection.createStatement() ) {
                    try ( ResultSet rs = stmt.executeQuery( "select @@session.max_execution_time" ) ) {
                        previous[0] = rs.next() ? rs.getLong( 1 ) : 0L;
                    }
                    if ( previous[0] > 0 ) {
                        stmt.execute( "set session max_execution_time = 0" );
                    }
                }
            } );
        } catch ( Exception e ) {
            log.debug( "Could not read the statement timeout for a streamed read; leaving it in place.", e );
            return () -> {};
        }
        if ( previous[0] == 0 ) {
            return () -> {};
        }
        long restored = previous[0];
        return () -> {
            try {
                session.doWork( connection -> {
                    try ( Statement stmt = connection.createStatement() ) {
                        stmt.execute( "set session max_execution_time = " + restored );
                    }
                } );
            } catch ( Exception e ) {
                // the connection is going back to the pool without its cap; say so rather than
                // let a later query silently run unbounded
                log.warn( "Failed to restore the statement timeout of " + restored
                        + " ms after a streamed read; this connection will not enforce it.", e );
            }
        };
    }

    private static boolean isMysql( Connection connection ) {
        try {
            // Connector/J reads this from the handshake, so it costs no round-trip
            return connection.getMetaData().getDatabaseProductName().toLowerCase().contains( "mysql" );
        } catch ( SQLException e ) {
            return false;
        }
    }
}
