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
package ubic.gemma.model.common.description;

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.description.Property</code>.
 * </p>
 * 
 * @see ubic.gemma.model.common.description.Property
 */
public abstract class PropertyDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.common.description.PropertyDao {

    /**
     * @see ubic.gemma.model.common.description.PropertyDao#create(int, java.util.Collection)
     */

    public java.util.Collection create( final int transform, final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Property.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform, ( ubic.gemma.model.common.description.Property ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    
    public Collection<? extends Property > load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from PropertyImpl where id in (:ids)", "ids", ids );
    }
    
    /**
     * @see ubic.gemma.model.common.description.PropertyDao#create(int transform,
     *      ubic.gemma.model.common.description.Property)
     */
    public Object create( final int transform, final ubic.gemma.model.common.description.Property property ) {
        if ( property == null ) {
            throw new IllegalArgumentException( "Property.create - 'property' can not be null" );
        }
        this.getHibernateTemplate().save( property );
        return this.transformEntity( transform, property );
    }

    /**
     * @see ubic.gemma.model.common.description.PropertyDao#create(java.util.Collection)
     */

    
    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.common.description.PropertyDao#create(ubic.gemma.model.common.description.Property)
     */
    public Property create( ubic.gemma.model.common.description.Property property ) {
        return ( ubic.gemma.model.common.description.Property ) this.create( TRANSFORM_NONE, property );
    }

    /**
     * @see ubic.gemma.model.common.description.PropertyDao#load(int, java.lang.Long)
     */

    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Property.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ubic.gemma.model.common.description.PropertyImpl.class,
                id );
        return transformEntity( transform, ( ubic.gemma.model.common.description.Property ) entity );
    }

    /**
     * @see ubic.gemma.model.common.description.PropertyDao#load(java.lang.Long)
     */

    public Property load( java.lang.Long id ) {
        return ( ubic.gemma.model.common.description.Property ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.common.description.PropertyDao#loadAll()
     */

    
    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.common.description.PropertyDao#loadAll(int)
     */

    public java.util.Collection loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.description.PropertyImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.description.PropertyDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Property.remove - 'id' can not be null" );
        }
        ubic.gemma.model.common.description.Property entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Property.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.common.description.PropertyDao#remove(ubic.gemma.model.common.description.Property)
     */
    public void remove( ubic.gemma.model.common.description.Property property ) {
        if ( property == null ) {
            throw new IllegalArgumentException( "Property.remove - 'property' can not be null" );
        }
        this.getHibernateTemplate().delete( property );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Property.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.common.description.Property ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.common.description.PropertyDao#update(ubic.gemma.model.common.description.Property)
     */
    public void update( ubic.gemma.model.common.description.Property property ) {
        if ( property == null ) {
            throw new IllegalArgumentException( "Property.update - 'property' can not be null" );
        }
        this.getHibernateTemplate().update( property );
    }

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.common.description.Property)} method. This method does not
     * instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in <code>ubic.gemma.model.common.description.PropertyDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.common.description.Property)
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
     * <code>ubic.gemma.model.common.description.PropertyDao</code>, please note that the {@link #TRANSFORM_NONE}
     * constant denotes no transformation, so the entity itself will be returned. If the integer argument value is
     * unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.common.description.PropertyDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform, final ubic.gemma.model.common.description.Property entity ) {
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