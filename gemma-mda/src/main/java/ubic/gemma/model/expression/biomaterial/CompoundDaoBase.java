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
package ubic.gemma.model.expression.biomaterial;

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.biomaterial.Compound</code>.
 * </p>
 * 
 * @see ubic.gemma.model.expression.biomaterial.Compound
 */
public abstract class CompoundDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.expression.biomaterial.CompoundDao {

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundDao#create(int, java.util.Collection)
     */
    public java.util.Collection<Compound> create( final int transform, final java.util.Collection<Compound> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Compound.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<Compound> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( transform, entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundDao#create(int transform,
     *      ubic.gemma.model.expression.biomaterial.Compound)
     */
    public Object create( final int transform, final ubic.gemma.model.expression.biomaterial.Compound compound ) {
        if ( compound == null ) {
            throw new IllegalArgumentException( "Compound.create - 'compound' can not be null" );
        }
        this.getHibernateTemplate().save( compound );
        return this.transformEntity( transform, compound );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundDao#create(java.util.Collection)
     */

    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundDao#create(ubic.gemma.model.expression.biomaterial.Compound)
     */
    public Compound create( ubic.gemma.model.expression.biomaterial.Compound compound ) {
        return ( ubic.gemma.model.expression.biomaterial.Compound ) this.create( TRANSFORM_NONE, compound );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundDao#find(int, java.lang.String,
     *      ubic.gemma.model.expression.biomaterial.Compound)
     */

    public Compound find( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.expression.biomaterial.Compound compound ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( compound );
        argNames.add( "compound" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.expression.biomaterial.Compound"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }
        result = transformEntity( transform, ( ubic.gemma.model.expression.biomaterial.Compound ) result );
        return ( Compound ) result;
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundDao#find(int,
     *      ubic.gemma.model.expression.biomaterial.Compound)
     */
    public Compound find( final int transform, final ubic.gemma.model.expression.biomaterial.Compound compound ) {
        return this
                .find(
                        transform,
                        "from ubic.gemma.model.expression.biomaterial.Compound as compound where compound.compound = :compound",
                        compound );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundDao#find(java.lang.String,
     *      ubic.gemma.model.expression.biomaterial.Compound)
     */
    public ubic.gemma.model.expression.biomaterial.Compound find( final java.lang.String queryString,
            final ubic.gemma.model.expression.biomaterial.Compound compound ) {
        return this.find( TRANSFORM_NONE, queryString, compound );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundDao#find(ubic.gemma.model.expression.biomaterial.Compound)
     */
    public ubic.gemma.model.expression.biomaterial.Compound find(
            ubic.gemma.model.expression.biomaterial.Compound compound ) {
        return this.find( TRANSFORM_NONE, compound );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundDao#findOrCreate(int, java.lang.String,
     *      ubic.gemma.model.expression.biomaterial.Compound)
     */

    public Compound findOrCreate( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.expression.biomaterial.Compound compound ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( compound );
        argNames.add( "compound" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.expression.biomaterial.Compound"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, ( ubic.gemma.model.expression.biomaterial.Compound ) result );
        return ( Compound ) result;
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundDao#findOrCreate(int,
     *      ubic.gemma.model.expression.biomaterial.Compound)
     */
    public Compound findOrCreate( final int transform, final ubic.gemma.model.expression.biomaterial.Compound compound ) {
        return this
                .findOrCreate(
                        transform,
                        "from ubic.gemma.model.expression.biomaterial.Compound as compound where compound.compound = :compound",
                        compound );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundDao#findOrCreate(java.lang.String,
     *      ubic.gemma.model.expression.biomaterial.Compound)
     */
    public ubic.gemma.model.expression.biomaterial.Compound findOrCreate( final java.lang.String queryString,
            final ubic.gemma.model.expression.biomaterial.Compound compound ) {
        return this.findOrCreate( TRANSFORM_NONE, queryString, compound );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundDao#findOrCreate(ubic.gemma.model.expression.biomaterial.Compound)
     */
    public ubic.gemma.model.expression.biomaterial.Compound findOrCreate(
            ubic.gemma.model.expression.biomaterial.Compound compound ) {
        return this.findOrCreate( TRANSFORM_NONE, compound );
    }

    public Collection<? extends Compound> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from CompoundImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundDao#load(int, java.lang.Long)
     */

    public Compound load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Compound.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.expression.biomaterial.CompoundImpl.class, id );
        return ( Compound ) transformEntity( transform, ( ubic.gemma.model.expression.biomaterial.Compound ) entity );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundDao#load(java.lang.Long)
     */

    public Compound load( java.lang.Long id ) {
        return this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundDao#loadAll()
     */

    public java.util.Collection<Compound> loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundDao#loadAll(int)
     */

    public java.util.Collection<Compound> loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.expression.biomaterial.CompoundImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Compound.remove - 'id' can not be null" );
        }
        ubic.gemma.model.expression.biomaterial.Compound entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection<? extends Compound> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Compound.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundDao#remove(ubic.gemma.model.expression.biomaterial.Compound)
     */
    public void remove( ubic.gemma.model.expression.biomaterial.Compound compound ) {
        if ( compound == null ) {
            throw new IllegalArgumentException( "Compound.remove - 'compound' can not be null" );
        }
        this.getHibernateTemplate().delete( compound );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection<? extends Compound> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Compound.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends Compound> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundDao#update(ubic.gemma.model.expression.biomaterial.Compound)
     */
    public void update( ubic.gemma.model.expression.biomaterial.Compound compound ) {
        if ( compound == null ) {
            throw new IllegalArgumentException( "Compound.update - 'compound' can not be null" );
        }
        this.getHibernateTemplate().update( compound );
    }

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.expression.biomaterial.Compound)} method. This method does not
     * instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in
     *        <code>ubic.gemma.model.expression.biomaterial.CompoundDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.expression.biomaterial.Compound)
     */

    protected void transformEntities( final int transform, final java.util.Collection<Compound> entities ) {
        switch ( transform ) {
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in
     * <code>ubic.gemma.model.expression.biomaterial.CompoundDao</code>, please note that the {@link #TRANSFORM_NONE}
     * constant denotes no transformation, so the entity itself will be returned. If the integer argument value is
     * unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.expression.biomaterial.CompoundDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform, final ubic.gemma.model.expression.biomaterial.Compound entity ) {
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