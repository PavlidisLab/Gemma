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
package ubic.gemma.model.common.protocol;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.protocol.Parameter</code>.
 * </p>
 * 
 * @see ubic.gemma.model.common.protocol.Parameter
 */
public abstract class ParameterDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.common.protocol.ParameterDao {

    /**
     * @see ubic.gemma.model.common.protocol.ParameterDao#create(int, java.util.Collection)
     */
    public java.util.Collection<Parameter> create( final int transform, final java.util.Collection<Parameter> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Parameter.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<Parameter> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( transform, entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.common.protocol.ParameterDao#create(int transform,
     *      ubic.gemma.model.common.protocol.Parameter)
     */
    public Parameter create( final int transform, final ubic.gemma.model.common.protocol.Parameter parameter ) {
        if ( parameter == null ) {
            throw new IllegalArgumentException( "Parameter.create - 'parameter' can not be null" );
        }
        this.getHibernateTemplate().save( parameter );
        return ( Parameter ) this.transformEntity( transform, parameter );
    }

    /**
     * @see ubic.gemma.model.common.protocol.ParameterDao#create(java.util.Collection)
     */
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection<Parameter> create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.common.protocol.ParameterDao#create(ubic.gemma.model.common.protocol.Parameter)
     */
    public Parameter create( ubic.gemma.model.common.protocol.Parameter parameter ) {
        return this.create( TRANSFORM_NONE, parameter );
    }

    /**
     * @see ubic.gemma.model.common.protocol.ParameterDao#load(int, java.lang.Long)
     */

    public Parameter load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Parameter.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate()
                .get( ubic.gemma.model.common.protocol.ParameterImpl.class, id );
        return ( Parameter ) transformEntity( transform, ( ubic.gemma.model.common.protocol.Parameter ) entity );
    }

    /**
     * @see ubic.gemma.model.common.protocol.ParameterDao#load(java.lang.Long)
     */

    public Parameter load( java.lang.Long id ) {
        return this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.common.protocol.ParameterDao#loadAll()
     */
 
    public java.util.Collection<Parameter> loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.common.protocol.ParameterDao#loadAll(int)
     */

    @SuppressWarnings("unchecked")
    public java.util.Collection<Parameter> loadAll( final int transform ) {
        final java.util.Collection<Parameter> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.protocol.ParameterImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.protocol.ParameterDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Parameter.remove - 'id' can not be null" );
        }
        ubic.gemma.model.common.protocol.Parameter entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection<Parameter> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Parameter.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.common.protocol.ParameterDao#remove(ubic.gemma.model.common.protocol.Parameter)
     */
    public void remove( ubic.gemma.model.common.protocol.Parameter parameter ) {
        if ( parameter == null ) {
            throw new IllegalArgumentException( "Parameter.remove - 'parameter' can not be null" );
        }
        this.getHibernateTemplate().delete( parameter );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection<Parameter> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Parameter.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<Parameter> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.common.protocol.ParameterDao#update(ubic.gemma.model.common.protocol.Parameter)
     */
    public void update( ubic.gemma.model.common.protocol.Parameter parameter ) {
        if ( parameter == null ) {
            throw new IllegalArgumentException( "Parameter.update - 'parameter' can not be null" );
        }
        this.getHibernateTemplate().update( parameter );
    }

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.common.protocol.Parameter)} method. This method does not instantiate
     * a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in <code>ubic.gemma.model.common.protocol.ParameterDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.common.protocol.Parameter)
     */

    protected void transformEntities( final int transform, final java.util.Collection<Parameter> entities ) {
        switch ( transform ) {
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in
     * <code>ubic.gemma.model.common.protocol.ParameterDao</code>, please note that the {@link #TRANSFORM_NONE} constant
     * denotes no transformation, so the entity itself will be returned. If the integer argument value is unknown
     * {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.common.protocol.ParameterDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform, final ubic.gemma.model.common.protocol.Parameter entity ) {
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