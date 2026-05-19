package ubic.gemma.persistence.hibernate;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.Query;
import ubic.gemma.core.config.Settings;

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
}
