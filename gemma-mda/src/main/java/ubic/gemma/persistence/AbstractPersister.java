/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.persistence;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

// import ubic.gemma.security.interceptor.CrudInterceptorUtils;

/**
 * Base class for persisters, provides session management.
 * 
 * @spring.property name="sessionFactory" ref="sessionFactory"
 * @author pavlidis
 * @version $Id$
 */
public abstract class AbstractPersister implements Persister {
    /**
     * 
     */
    private static final int COLLECTION_INFO_FREQUENCY = 1000;

    protected static Log log = LogFactory.getLog( AbstractPersister.class.getName() );

    /**
     * This should match the JDBC batch size for Hibernate.
     */
    protected static final int SESSION_BATCH_SIZE = 50;

    /**
     * This is here only to allow optimization of hibernate.
     */
    protected SessionFactory sessionFactory;

    // CrudInterceptorUtils crudUtils = new CrudInterceptorUtils();

    /*
     * @see ubic.gemma.model.loader.loaderutils.Loader#create(java.util.Collection)
     */
    public Collection<?> persist( Collection<?> col ) {
        Collection<Object> result = new HashSet<Object>();
        try {
            int count = 0;
            log.debug( "Entering + " + this.getClass().getName() + ".persist() with " + col.size() + " objects." );
            for ( Object entity : col ) {
                result.add( persist( entity ) );
                // if ( ++count % 20 == 0 ) {
                // this.getCurrentSession().flush();
                // this.getCurrentSession().clear();
                // }
                if ( ++count % COLLECTION_INFO_FREQUENCY == 0 ) {
                    log.info( "Persisted " + count + " objects in collection" );
                }
            }
        } catch ( Exception e ) {
            log.fatal( "Error while persisting collection: ", e );
            throw new RuntimeException( e );
        }
        return result;
    }

    /**
     * Persist the elements in a collection.
     * <p>
     * This method is necessary because in-place persisting does not work.
     * 
     * @param collection
     * @return
     */
    protected void persistCollectionElements( Collection collection ) {
        if ( collection == null ) return;
        if ( collection.size() == 0 ) return;

        try {
            // Collection<Object> persistedCollection = new HashSet<Object>();
            for ( Object object : collection ) {
                Object persistedObj = persist( object );
                // persistedCollection.add( persistedObj );
                BeanUtils.setProperty( object, "id", BeanUtils.getSimpleProperty( persistedObj, "id" ) );
                assert BeanUtils.getSimpleProperty( object, "id" ) != null;
            }
        } catch ( IllegalAccessException e ) {
            throw new RuntimeException( e );
        } catch ( InvocationTargetException e ) {
            throw new RuntimeException( e );
        } catch ( NoSuchMethodException e ) {
            throw new RuntimeException( e );
        }

        // collection = persistedCollection;
    }

    /**
     * @param sessionFactory The sessionFactory to set.
     */
    public void setSessionFactory( SessionFactory sessionFactory ) {
        this.sessionFactory = sessionFactory;
        // crudUtils.initMetaData( sessionFactory );
    }

    /**
     * @return Current Hibernate Session.
     */
    protected Session getCurrentSession() {
        try {
            return this.sessionFactory.getCurrentSession();
        } catch ( HibernateException e ) {
            return sessionFactory.openSession();
        }
    }

    /**
     * Determine if a entity is transient (not persistent).
     * 
     * @param entity
     * @return If the entity is null, return true. If the entity is non-null and has a null "id" property, return true;
     *         Otherwise return false.
     */
    protected boolean isTransient( Object entity ) {
        if ( entity == null ) return true;
        try {
            return org.apache.commons.beanutils.BeanUtils.getSimpleProperty( entity, "id" ) == null;
        } catch ( IllegalAccessException e ) {
            throw new RuntimeException( e );
        } catch ( InvocationTargetException e ) {
            throw new RuntimeException( e );
        } catch ( NoSuchMethodException e ) {
            throw new RuntimeException( e );
        }
    }

}
