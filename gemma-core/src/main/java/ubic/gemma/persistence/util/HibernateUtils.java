package ubic.gemma.persistence.util;

import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.springframework.util.ReflectionUtils;

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
}
