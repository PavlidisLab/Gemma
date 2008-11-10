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
    public ubic.gemma.model.common.auditAndSecurity.AuditEvent addEvent(
            final ubic.gemma.model.common.Auditable auditable,
            final ubic.gemma.model.common.auditAndSecurity.AuditEvent auditEvent ) {
        try {
            return this.handleAddEvent( auditable, auditEvent );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.AuditTrailDao.addEvent(ubic.gemma.model.common.Auditable auditable, ubic.gemma.model.common.auditAndSecurity.AuditEvent auditEvent)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao#create(int, java.util.Collection)
     */
    public java.util.Collection create( final int transform, final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "AuditTrail.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform, ( ubic.gemma.model.common.auditAndSecurity.AuditTrail ) entityIterator
                                    .next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao#create(int transform,
     *      ubic.gemma.model.common.auditAndSecurity.AuditTrail)
     */
    public Object create( final int transform, final ubic.gemma.model.common.auditAndSecurity.AuditTrail auditTrail ) {
        if ( auditTrail == null ) {
            throw new IllegalArgumentException( "AuditTrail.create - 'auditTrail' can not be null" );
        }
        this.getHibernateTemplate().save( auditTrail );
        return this.transformEntity( transform, auditTrail );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao#create(java.util.Collection)
     */
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao#create(ubic.gemma.model.common.auditAndSecurity.AuditTrail)
     */
    public ubic.gemma.model.common.auditAndSecurity.AuditTrail create(
            ubic.gemma.model.common.auditAndSecurity.AuditTrail auditTrail ) {
        return ( ubic.gemma.model.common.auditAndSecurity.AuditTrail ) this.create( TRANSFORM_NONE, auditTrail );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao#load(int, java.lang.Long)
     */
    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "AuditTrail.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.common.auditAndSecurity.AuditTrailImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.common.auditAndSecurity.AuditTrail ) entity );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao#load(java.lang.Long)
     */
    public ubic.gemma.model.common.auditAndSecurity.AuditTrail load( java.lang.Long id ) {
        return ( ubic.gemma.model.common.auditAndSecurity.AuditTrail ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao#loadAll()
     */
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao#loadAll(int)
     */
    public java.util.Collection loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.auditAndSecurity.AuditTrailImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao#remove(java.lang.Long)
     */
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "AuditTrail.remove - 'id' can not be null" );
        }
        ubic.gemma.model.common.auditAndSecurity.AuditTrail entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao#remove(java.util.Collection)
     */
    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "AuditTrail.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao#remove(ubic.gemma.model.common.auditAndSecurity.AuditTrail)
     */
    public void remove( ubic.gemma.model.common.auditAndSecurity.AuditTrail auditTrail ) {
        if ( auditTrail == null ) {
            throw new IllegalArgumentException( "AuditTrail.remove - 'auditTrail' can not be null" );
        }
        this.getHibernateTemplate().delete( auditTrail );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao#thaw(ubic.gemma.model.common.Auditable)
     */
    public void thaw( final ubic.gemma.model.common.Auditable auditable ) {
        try {
            this.handleThaw( auditable );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.AuditTrailDao.thaw(ubic.gemma.model.common.Auditable auditable)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao#thaw(ubic.gemma.model.common.auditAndSecurity.AuditTrail)
     */
    public void thaw( final ubic.gemma.model.common.auditAndSecurity.AuditTrail auditTrail ) {
        try {
            this.handleThaw( auditTrail );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.AuditTrailDao.thaw(ubic.gemma.model.common.auditAndSecurity.AuditTrail auditTrail)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao#update(java.util.Collection)
     */
    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "AuditTrail.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.common.auditAndSecurity.AuditTrail ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao#update(ubic.gemma.model.common.auditAndSecurity.AuditTrail)
     */
    public void update( ubic.gemma.model.common.auditAndSecurity.AuditTrail auditTrail ) {
        if ( auditTrail == null ) {
            throw new IllegalArgumentException( "AuditTrail.update - 'auditTrail' can not be null" );
        }
        this.getHibernateTemplate().update( auditTrail );
    }

    /**
     * Performs the core logic for
     * {@link #addEvent(ubic.gemma.model.common.Auditable, ubic.gemma.model.common.auditAndSecurity.AuditEvent)}
     */
    protected abstract ubic.gemma.model.common.auditAndSecurity.AuditEvent handleAddEvent(
            ubic.gemma.model.common.Auditable auditable, ubic.gemma.model.common.auditAndSecurity.AuditEvent auditEvent )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(ubic.gemma.model.common.Auditable)}
     */
    protected abstract void handleThaw( ubic.gemma.model.common.Auditable auditable ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(ubic.gemma.model.common.auditAndSecurity.AuditTrail)}
     */
    protected abstract void handleThaw( ubic.gemma.model.common.auditAndSecurity.AuditTrail auditTrail )
            throws java.lang.Exception;

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.common.auditAndSecurity.AuditTrail)} method. This method does not
     * instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in
     *        <code>ubic.gemma.model.common.auditAndSecurity.AuditTrailDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.common.auditAndSecurity.AuditTrail)
     */
    protected void transformEntities( final int transform, final java.util.Collection entities ) {
        switch ( transform ) {
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in
     * <code>ubic.gemma.model.common.auditAndSecurity.AuditTrailDao</code>, please note that the {@link #TRANSFORM_NONE}
     * constant denotes no transformation, so the entity itself will be returned. If the integer argument value is
     * unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.common.auditAndSecurity.AuditTrailDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform,
            final ubic.gemma.model.common.auditAndSecurity.AuditTrail entity ) {
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