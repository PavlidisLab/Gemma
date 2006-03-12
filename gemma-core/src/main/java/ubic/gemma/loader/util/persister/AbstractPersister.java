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
package ubic.gemma.loader.util.persister;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.CascadeStyle;
import org.hibernate.engine.CascadingAction;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.beans.BeanUtils;

import ubic.gemma.security.interceptor.CrudInterceptorUtils;

/**
 * Base class for persisters, provides session management.
 * 
 * @spring.property name="sessionFactory" ref="sessionFactory"
 * @author pavlidis
 * @version $Id$
 */
public abstract class AbstractPersister implements Persister {
    protected static Log log = LogFactory.getLog( AbstractPersister.class.getName() );

    /**
     * This should match the JDBC batch size for Hibernate.
     */
    protected static final int SESSION_BATCH_SIZE = 50;

    /**
     * This is here only to allow optimization of hibernate.
     */
    protected SessionFactory sessionFactory;

    CrudInterceptorUtils crudUtils = new CrudInterceptorUtils();

    /*
     * @see ubic.gemma.model.loader.loaderutils.Loader#create(java.util.Collection)
     */
    public Collection<Object> persist( Collection<Object> col ) {

        try {
            int count = 0;
            log.debug( "Entering + " + this.getClass().getName() + ".persist() with " + col.size() + " objects." );
            for ( Object entity : col ) {
                persist( entity );
                if ( ++count % 20 == 0 ) {
                    this.flushAndClearSession();
                }
            }
        } catch ( Exception e ) {
            log.fatal( "Error while persisting collection " + col, e );
            throw new RuntimeException( e );
        }
        return col;
    }

    /**
     * @param sessionFactory The sessionFactory to set.
     */
    public void setSessionFactory( SessionFactory sessionFactory ) {
        this.sessionFactory = sessionFactory;
        crudUtils.initMetaData( sessionFactory );
    }

    private int flushed = 0;

    /**
     * Flush and clear the hibernate cache. Call during persistence of large collections.
     */
    protected void flushAndClearSession() {
        // this.getCurrentSession().flush();
        // this.getCurrentSession().clear();
        // log.info( "Flushed " + ++flushed + " times" );
    }

    /**
     * Recursively "Fixup" persistent collections so we don't get the "two copies" error
     * 
     * @param entity
     */
    @SuppressWarnings("unchecked")
    protected void refreshCollections( Object entity ) {

        this.crudUtils.initMetaData( this.sessionFactory );

        EntityPersister persister = crudUtils.getEntityPersister( entity );

        String[] propertyNames;
        CascadeStyle[] cascadeStyles = null;

        if ( persister == null ) { // it's not a domain object
            return;
        }
        cascadeStyles = persister.getPropertyCascadeStyles();
        propertyNames = persister.getPropertyNames();

        log.debug( "Refreshing collections for " + entity );

        for ( int j = 0; j < propertyNames.length; j++ ) {

            if ( cascadeStyles != null ) {
                CascadeStyle cs = cascadeStyles[j];

                if ( !crudUtils.needCascade( cs ) && !cs.doCascade( CascadingAction.DELETE ) ) {
                    continue;
                }

            }

            PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor( entity.getClass(), propertyNames[j] );

            Method setter = descriptor.getWriteMethod();
            if ( setter == null ) continue;
            Method getter = descriptor.getReadMethod();

            try {
                Object value = getter.invoke( entity, new Object[] {} );
                if ( value == null ) continue;
                if ( !( value instanceof Collection ) ) continue;

                if ( value instanceof List ) {
                    setter.invoke( entity, new Object[] { new ArrayList( ( List ) value ) } );
                } else if ( value instanceof Set ) {
                    setter.invoke( entity, new Object[] { new HashSet( ( Set ) value ) } );
                }

                // recurse
                refreshCollections( value );

            } catch ( IllegalArgumentException e ) {
                throw new RuntimeException( e );
            } catch ( IllegalAccessException e ) {
                throw new RuntimeException( e );
            } catch ( InvocationTargetException e ) {
                throw new RuntimeException( e );
            }

        }
    }

    /**
     * @return Current Hibernate Session.
     */
    protected Session getCurrentSession() {
        return this.sessionFactory.getCurrentSession();
    }

    /**
     * Determine if a entity is transient (not persistent).
     * 
     * @param entity
     * @return If the entity is null, return false. If the entity is non-null and has a null "id" property, return true;
     *         Otherwise return false.
     */
    protected boolean isTransient( Object entity ) {
        if ( entity == null ) return false;
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
