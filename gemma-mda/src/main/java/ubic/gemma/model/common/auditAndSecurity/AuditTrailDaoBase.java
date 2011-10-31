/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.model.common.auditAndSecurity;

import java.util.Collection;
import java.util.Iterator;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.auditAndSecurity.AuditTrail</code>.
 * </p>
 * 
 * @see ubic.gemma.model.common.auditAndSecurity.AuditTrail
 */
public abstract class AuditTrailDaoBase extends org.springframework.orm.hibernate3.support.HibernateDaoSupport
        implements ubic.gemma.model.common.auditAndSecurity.AuditTrailDao {

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao#addEvent(ubic.gemma.model.common.Auditable,
     *      ubic.gemma.model.common.auditAndSecurity.AuditEvent)
     */
    public AuditEvent addEvent( final ubic.gemma.model.common.Auditable auditable, final AuditEvent auditEvent ) {
        try {
            return this.handleAddEvent( auditable, auditEvent );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'AuditTrailDao.addEvent(ubic.gemma.model.common.Auditable auditable, AuditEvent auditEvent)' --> "
                            + th, th );
        }
    }

    /**
     * @see AuditTrailDao#create(int, Collection)
     */
    public Collection<? extends AuditTrail> create( final int transform, final Collection<? extends AuditTrail> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "AuditTrail.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( Iterator<? extends AuditTrail> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( transform, entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see AuditTrailDao#create(int transform, AuditTrail)
     */
    public Object create( final int transform, final AuditTrail auditTrail ) {
        if ( auditTrail == null ) {
            throw new IllegalArgumentException( "AuditTrail.create - 'auditTrail' can not be null" );
        }
        this.getHibernateTemplate().save( auditTrail );
        return this.transformEntity( transform, auditTrail );
    }

    /**
     * @see AuditTrailDao#create(Collection)
     */

    public Collection<? extends AuditTrail> create( final Collection<? extends AuditTrail> entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see AuditTrailDao#create(AuditTrail)
     */
    public AuditTrail create( AuditTrail auditTrail ) {
        return ( AuditTrail ) this.create( TRANSFORM_NONE, auditTrail );
    }

    /**
     * @see AuditTrailDao#load(int, java.lang.Long)
     */
    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "AuditTrail.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( AuditTrailImpl.class, id );
        return transformEntity( transform, ( AuditTrail ) entity );
    }

    /**
     * @see AuditTrailDao#load(java.lang.Long)
     */
    public AuditTrail load( java.lang.Long id ) {
        return ( AuditTrail ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see AuditTrailDao#loadAll()
     */

    public Collection<? extends AuditTrail> loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see AuditTrailDao#loadAll(int)
     */
    public Collection<? extends AuditTrail> loadAll( final int transform ) {
        final Collection results = this.getHibernateTemplate().loadAll( AuditTrailImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see AuditTrailDao#remove(java.lang.Long)
     */
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "AuditTrail.remove - 'id' can not be null" );
        }
        AuditTrail entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see AuditTrailDao#remove(Collection)
     */
    public void remove( Collection<? extends AuditTrail> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "AuditTrail.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see AuditTrailDao#remove(AuditTrail)
     */
    public void remove( AuditTrail auditTrail ) {
        if ( auditTrail == null ) {
            throw new IllegalArgumentException( "AuditTrail.remove - 'auditTrail' can not be null" );
        }
        this.getHibernateTemplate().delete( auditTrail );
    }

    /**
     * @see AuditTrailDao#update(Collection)
     */
    public void update( final Collection<? extends AuditTrail> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "AuditTrail.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( Iterator<? extends AuditTrail> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see AuditTrailDao#update(AuditTrail)
     */
    public void update( AuditTrail auditTrail ) {
        if ( auditTrail == null ) {
            throw new IllegalArgumentException( "AuditTrail.update - 'auditTrail' can not be null" );
        }
        this.getHibernateTemplate().update( auditTrail );
    }

    /**
     * Performs the core logic for {@link #addEvent(ubic.gemma.model.common.Auditable, AuditEvent)}
     */
    protected abstract AuditEvent handleAddEvent( ubic.gemma.model.common.Auditable auditable, AuditEvent auditEvent )
            throws java.lang.Exception;

    /**
     * Transforms a collection of entities using the {@link #transformEntity(int,AuditTrail)} method. This method does
     * not instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in <code>AuditTrailDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,AuditTrail)
     */
    protected void transformEntities( final int transform, final Collection<? extends AuditTrail> entities ) {
        switch ( transform ) {
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in <code>AuditTrailDao</code>, please note
     * that the {@link #TRANSFORM_NONE} constant denotes no transformation, so the entity itself will be returned. If
     * the integer argument value is unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link AuditTrailDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,Collection)
     */
    protected Object transformEntity( final int transform, final AuditTrail entity ) {
        Object target = null;
        if ( entity != null ) {
            switch ( transform ) {
                case TRANSFORM_NONE: // fall-through
                default:
                    target = entity;
            }
        }
        return target;
    }

}