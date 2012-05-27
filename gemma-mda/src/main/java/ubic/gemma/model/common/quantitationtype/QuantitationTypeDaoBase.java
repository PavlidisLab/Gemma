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
package ubic.gemma.model.common.quantitationtype;

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.quantitationtype.QuantitationType</code>.
 * </p>
 * 
 * @see ubic.gemma.model.common.quantitationtype.QuantitationType
 */
public abstract class QuantitationTypeDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.common.quantitationtype.QuantitationTypeDao {

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeDao#create(int, java.util.Collection)
     */
    public java.util.Collection<? extends QuantitationType> create( final int transform,
            final java.util.Collection<? extends QuantitationType> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "QuantitationType.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends QuantitationType> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( transform, entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }
    
    
    @Override
    public Collection<? extends QuantitationType > load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from QuantitationTypeImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeDao#create(int transform,
     *      ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    public QuantitationType create( final int transform,
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        if ( quantitationType == null ) {
            throw new IllegalArgumentException( "QuantitationType.create - 'quantitationType' can not be null" );
        }
        this.getHibernateTemplate().save( quantitationType );
        return ( QuantitationType ) this.transformEntity( transform, quantitationType );
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeDao#create(java.util.Collection)
     */
    
    @Override
    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeDao#create(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    public QuantitationType create( ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        return this.create( TRANSFORM_NONE, quantitationType );
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeDao#find(int, java.lang.String,
     *      ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    
    public QuantitationType find( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( quantitationType );
        argNames.add( "quantitationType" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.common.quantitationtype.QuantitationType"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, ( ubic.gemma.model.common.quantitationtype.QuantitationType ) result );
        return ( QuantitationType ) result;
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeDao#find(int,
     *      ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    public QuantitationType find( final int transform,
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        return this
                .find(
                        transform,
                        "from DatabaseEntryImpl where accession=databaseEntry.accession and externalDatabase=databaseEntry.externalDatabase",
                        quantitationType );
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeDao#find(java.lang.String,
     *      ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    public ubic.gemma.model.common.quantitationtype.QuantitationType find( final java.lang.String queryString,
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        return this.find( TRANSFORM_NONE, queryString, quantitationType );
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeDao#find(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    public ubic.gemma.model.common.quantitationtype.QuantitationType find(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        return this.find( TRANSFORM_NONE, quantitationType );
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeDao#findOrCreate(int, java.lang.String,
     *      ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    
    public QuantitationType findOrCreate( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( quantitationType );
        argNames.add( "quantitationType" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.common.quantitationtype.QuantitationType"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, ( ubic.gemma.model.common.quantitationtype.QuantitationType ) result );
        return ( QuantitationType ) result;
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeDao#findOrCreate(int,
     *      ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    public QuantitationType findOrCreate( final int transform,
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        return this
                .findOrCreate(
                        transform,
                        "from ubic.gemma.model.common.quantitationtype.QuantitationType as quantitationType where quantitationType.quantitationType = :quantitationType",
                        quantitationType );
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeDao#findOrCreate(java.lang.String,
     *      ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    public ubic.gemma.model.common.quantitationtype.QuantitationType findOrCreate( final java.lang.String queryString,
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        return this.findOrCreate( TRANSFORM_NONE, queryString, quantitationType );
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeDao#findOrCreate(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    public ubic.gemma.model.common.quantitationtype.QuantitationType findOrCreate(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        return this.findOrCreate( TRANSFORM_NONE, quantitationType );
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeDao#load(int, java.lang.Long)
     */

    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "QuantitationType.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.common.quantitationtype.QuantitationTypeImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.common.quantitationtype.QuantitationType ) entity );
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeDao#load(java.lang.Long)
     */

    @Override
    public QuantitationType load( java.lang.Long id ) {
        return ( ubic.gemma.model.common.quantitationtype.QuantitationType ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeDao#loadAll()
     */
    @Override
    public java.util.Collection<? extends QuantitationType> loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeDao#loadAll(int)
     */

    
    public java.util.Collection<? extends QuantitationType> loadAll( final int transform ) {
        final java.util.Collection<? extends QuantitationType> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.quantitationtype.QuantitationTypeImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeDao#remove(java.lang.Long)
     */

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "QuantitationType.remove - 'id' can not be null" );
        }
        ubic.gemma.model.common.quantitationtype.QuantitationType entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    @Override
    public void remove( java.util.Collection<? extends QuantitationType> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "QuantitationType.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeDao#remove(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    public void remove( ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        if ( quantitationType == null ) {
            throw new IllegalArgumentException( "QuantitationType.remove - 'quantitationType' can not be null" );
        }
        this.getHibernateTemplate().delete( quantitationType );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    @Override
    public void update( final java.util.Collection<? extends QuantitationType> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "QuantitationType.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends QuantitationType> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeDao#update(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    public void update( ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        if ( quantitationType == null ) {
            throw new IllegalArgumentException( "QuantitationType.update - 'quantitationType' can not be null" );
        }
        this.getHibernateTemplate().update( quantitationType );
    }

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.common.quantitationtype.QuantitationType)} method. This method does
     * not instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in
     *        <code>ubic.gemma.model.common.quantitationtype.QuantitationTypeDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.common.quantitationtype.QuantitationType)
     */

    protected void transformEntities( final int transform,
            final java.util.Collection<? extends QuantitationType> entities ) {
        switch ( transform ) {
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in
     * <code>ubic.gemma.model.common.quantitationtype.QuantitationTypeDao</code>, please note that the
     * {@link #TRANSFORM_NONE} constant denotes no transformation, so the entity itself will be returned. If the integer
     * argument value is unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in
     *        {@link ubic.gemma.model.common.quantitationtype.QuantitationTypeDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform,
            final ubic.gemma.model.common.quantitationtype.QuantitationType entity ) {
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