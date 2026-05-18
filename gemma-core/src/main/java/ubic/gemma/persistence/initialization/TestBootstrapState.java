package ubic.gemma.persistence.initialization;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JVM-wide one-shot guard for the integration-test database bootstrap.
 * <p>
 * Renovations Phase 2 (multi-context schema-drop bug): Spring's TestContext cache reuses an
 * ApplicationContext across test classes that share the same {@code @ContextConfiguration},
 * but builds a new ApplicationContext when the configuration shape differs. Each new
 * ApplicationContext bootstraps a fresh {@code createDatabaseInitializer → sessionFactory →
 * dataSourceInitializer} chain, which previously meant:
 * <ol>
 *     <li>The test DB was dropped and recreated (wiping tables for every <em>other</em>
 *         still-cached EntityManagerFactory).</li>
 *     <li>Hibernate's {@code hbm2ddl.auto=create} dropped and recreated tables again (further
 *         wiping the still-cached EMFs' view of the schema).</li>
 *     <li>{@code init-*.sql} re-ran (primary-key collisions or wasted I/O).</li>
 * </ol>
 * <p>
 * This class is the centralised gate: every member is a once-per-JVM toggle. Components consult
 * it to decide whether to run destructive bootstrap work or skip because an earlier context
 * already paid the cost.
 * <p>
 * The flags survive across Spring ApplicationContext lifecycles because they live on a
 * classloader-static {@link AtomicBoolean}; a fresh JVM (i.e. a fresh Surefire/Failsafe fork)
 * starts with all flags false, so each forked test JVM still gets a clean DB on first boot.
 *
 * @author phase2-bot
 */
public final class TestBootstrapState {

    /** True once a {@link CreateDatabasePopulator} has dropped + recreated the test DB. */
    private static final AtomicBoolean databaseCreated = new AtomicBoolean( false );

    /** True once a {@link InitialDataPopulator} has run the seed scripts. */
    private static final AtomicBoolean dataSeeded = new AtomicBoolean( false );

    /** True once Hibernate has materialized the schema via {@code hbm2ddl.auto=create}. */
    private static final AtomicBoolean schemaMaterialized = new AtomicBoolean( false );

    /** True once the schema-extras (ACL SIDs/OIDs, indices, additional tables) have been applied. */
    private static final AtomicBoolean schemaExtrasApplied = new AtomicBoolean( false );

    private TestBootstrapState() {
    }

    /**
     * Atomically claim the right to (drop and) create the test database.
     * @return true if the caller is the first claimant and should perform the work; false if a
     * prior caller already did it.
     */
    public static boolean claimDatabaseCreation() {
        return databaseCreated.compareAndSet( false, true );
    }

    /**
     * Atomically claim the right to seed initial data.
     * @return true if the caller is the first claimant and should run the SQL scripts; false if a
     * prior caller already did it.
     */
    public static boolean claimDataSeeding() {
        return dataSeeded.compareAndSet( false, true );
    }

    /**
     * Atomically claim the right to let Hibernate materialize the schema (via {@code hbm2ddl=create}).
     * @return true if the caller is the first claimant and should keep {@code hbm2ddl=create};
     * false if a prior caller already materialized the schema and the EMF should downgrade to
     * {@code none}.
     */
    public static boolean claimSchemaMaterialization() {
        return schemaMaterialized.compareAndSet( false, true );
    }

    /**
     * Atomically claim the right to run the schema-extras populator (ACL seed INSERTs, indices,
     * additional tables defined in {@code sql/init-acls.sql} + {@code sql/init-entities.sql} +
     * {@code sql/<vendor>/init-entities.sql}).
     * @return true on the first caller; false thereafter.
     */
    public static boolean claimSchemaExtras() {
        return schemaExtrasApplied.compareAndSet( false, true );
    }

    /**
     * Reset all flags. Intended for tests of the bootstrap chain itself; do not call from
     * production paths.
     */
    public static void resetForTesting() {
        databaseCreated.set( false );
        dataSeeded.set( false );
        schemaMaterialized.set( false );
        schemaExtrasApplied.set( false );
    }
}
