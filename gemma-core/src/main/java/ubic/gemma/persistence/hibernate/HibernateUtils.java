package ubic.gemma.persistence.hibernate;

import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
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
    public static int getBatchSize( ClassMetadata classMetadata, SessionFactory sessionFactory ) {
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
     * Determine if a {@link Query} is stateless, which means that upon being performed, no additional queries will be
     * issued.
     * <p>
     * You can prevent additional queries by proactively retrieving associated entities in the session.
     */
    public static boolean isStateless( Query query, SessionFactory sessionFactory ) {
        Type[] types = query.getReturnTypes();
        for ( int i = 0; i < types.length; i++ ) {
            Type type = types[i];
            if ( type.isEntityType() ) {
                ClassMetadata classMetadata = sessionFactory.getClassMetadata( type.getReturnedClass() );
                if ( !isStateless( classMetadata, sessionFactory ) ) {
                    log.debug( "Alias " + query.getReturnAliases()[i] + " is not a stateless entity." );
                    return false;
                }
            }
            if ( type.isCollectionType() ) {
                String entityName = ( ( CollectionType ) type ).getAssociatedEntityName( ( SessionFactoryImplementor ) sessionFactory );
                ClassMetadata classMetadata = sessionFactory.getClassMetadata( entityName );
                if ( !isStateless( classMetadata, sessionFactory ) ) {
                    log.debug( "Alias " + query.getReturnAliases()[i] + " is not a stateless collection." );
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check if querying a particular entity is stateless, which means that upon being performed, no additional queries
     * will be issued.
     */
    public static boolean isStateless( ClassMetadata classMetadata, SessionFactory sessionFactory ) {
        Type[] propertyTypes = classMetadata.getPropertyTypes();
        for ( int i = 0; i < propertyTypes.length; i++ ) {
            Type type = propertyTypes[i];
            if ( type.isEntityType() && HibernateUtils.isEager( ( EntityType ) type ) ) {
                log.debug( classMetadata.getEntityName() + "." + classMetadata.getPropertyNames()[i] + " is eagerly retrieved." );
                return false;
            }
            if ( type.isCollectionType() ) {
                CollectionMetadata collectionMetadata = sessionFactory.getCollectionMetadata( ( ( CollectionType ) type ).getRole() );
                if ( !collectionMetadata.isLazy() ) {
                    log.debug( classMetadata.getEntityName() + "." + classMetadata.getPropertyNames()[i] + " is eagerly retrieved." );
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Determine if an entity association is eagerly retrieved.
     */
    private static boolean isEager( EntityType type ) {
        Field field = ReflectionUtils.findField( EntityType.class, "eager" );
        ReflectionUtils.makeAccessible( field );
        return ( boolean ) ReflectionUtils.getField( field, type );
    }
}
