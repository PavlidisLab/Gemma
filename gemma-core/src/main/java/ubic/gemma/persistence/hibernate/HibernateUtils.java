package ubic.gemma.persistence.hibernate;

import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.springframework.util.ReflectionUtils;
import ubic.gemma.core.config.Settings;

import java.lang.reflect.Field;

@CommonsLog
public class HibernateUtils {

    private static final String BATCH_FETCH_SIZE_SETTING = "gemma.hibernate.default_batch_fetch_size";

    /**
     * Obtain the batch fetch size for the given class.
     */
    public static int getBatchSize( SessionFactory sessionFactory, ClassMetadata classMetadata ) {
        if ( classMetadata instanceof AbstractEntityPersister ) {
            Field field = ReflectionUtils.findField( AbstractEntityPersister.class, "batchSize" );
            ReflectionUtils.makeAccessible( field );
            return ( int ) ReflectionUtils.getField( field, classMetadata );
        } else if ( sessionFactory instanceof SessionFactoryImplementor ) {
            return ( ( SessionFactoryImplementor ) sessionFactory ).getSettings()
                    .getDefaultBatchFetchSize();
        } else {
            log.warn( String.format( "Could not determine batch size for %s, will fallback to the %s setting.",
                    classMetadata.getEntityName(), BATCH_FETCH_SIZE_SETTING ) );
            return Settings.getInt( BATCH_FETCH_SIZE_SETTING, -1 );
        }
    }

    /**
     * Determine if a Hibernate type is eagerly retrieved.
     */
    public static boolean isEager( Type type, SessionFactory sessionFactory ) {
        if ( type.isEntityType() ) {
            Field field = ReflectionUtils.findField( EntityType.class, "eager" );
            ReflectionUtils.makeAccessible( field );
            return ( boolean ) ReflectionUtils.getField( field, type );
        }
        if ( type.isCollectionType() ) {
            return !sessionFactory.getCollectionMetadata( ( ( CollectionType ) type ).getRole() ).isLazy();
        }
        log.warn( "Cannot tell if " + type + " is eagerly fetched, will assume it is." );
        return true;
    }
}
