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
package ubic.gemma.model.expression.experiment;

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.experiment.ExperimentalDesign</code>.
 * </p>
 * 
 * @see ubic.gemma.model.expression.experiment.ExperimentalDesign
 */
public abstract class ExperimentalDesignDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.expression.experiment.ExperimentalDesignDao {

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#create(int, java.util.Collection)
     */
    public java.util.Collection<? extends ExperimentalDesign> create( final int transform,
            final java.util.Collection<? extends ExperimentalDesign> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExperimentalDesign.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform,
                                    ( ubic.gemma.model.expression.experiment.ExperimentalDesign ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#create(int transform,
     *      ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */
    public Object create( final int transform,
            final ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) {
        if ( experimentalDesign == null ) {
            throw new IllegalArgumentException( "ExperimentalDesign.create - 'experimentalDesign' can not be null" );
        }
        this.getHibernateTemplate().save( experimentalDesign );
        return this.transformEntity( transform, experimentalDesign );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#create(java.util.Collection)
     */

    public java.util.Collection<? extends ExperimentalDesign> create(
            final java.util.Collection<? extends ExperimentalDesign> entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#create(ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */
    public ExperimentalDesign create( ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) {
        return ( ubic.gemma.model.expression.experiment.ExperimentalDesign ) this.create( TRANSFORM_NONE,
                experimentalDesign );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#find(int, java.lang.String,
     *      ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */

    public Object find( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( experimentalDesign );
        argNames.add( "experimentalDesign" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.expression.experiment.ExperimentalDesign"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, ( ubic.gemma.model.expression.experiment.ExperimentalDesign ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#find(int,
     *      ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */

    public Object find( final int transform,
            final ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) {
        return this
                .find( transform,
                        "from ubic.gemma.model.expression.experiment.ExperimentalDesign as experimentalDesign where experimentalDesign.experimentalDesign = :experimentalDesign",
                        experimentalDesign );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#find(java.lang.String,
     *      ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */

    public ubic.gemma.model.expression.experiment.ExperimentalDesign find( final java.lang.String queryString,
            final ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) {
        return ( ubic.gemma.model.expression.experiment.ExperimentalDesign ) this.find( TRANSFORM_NONE, queryString,
                experimentalDesign );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#find(ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */
    public ubic.gemma.model.expression.experiment.ExperimentalDesign find(
            ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) {
        return ( ubic.gemma.model.expression.experiment.ExperimentalDesign ) this.find( TRANSFORM_NONE,
                experimentalDesign );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#findByName(int, java.lang.String)
     */

    public Object findByName( final int transform, final java.lang.String name ) {
        return this
                .findByName(
                        transform,
                        "from ubic.gemma.model.expression.experiment.ExperimentalDesign as experimentalDesign where experimentalDesign.name = :name",
                        name );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#findByName(int, java.lang.String,
     *      java.lang.String)
     */

    public Object findByName( final int transform, final java.lang.String queryString, final java.lang.String name ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( name );
        argNames.add( "name" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.expression.experiment.ExperimentalDesign"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, ( ubic.gemma.model.expression.experiment.ExperimentalDesign ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#findByName(java.lang.String)
     */
    public ubic.gemma.model.expression.experiment.ExperimentalDesign findByName( java.lang.String name ) {
        return ( ubic.gemma.model.expression.experiment.ExperimentalDesign ) this.findByName( TRANSFORM_NONE, name );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#findByName(java.lang.String, java.lang.String)
     */

    public ubic.gemma.model.expression.experiment.ExperimentalDesign findByName( final java.lang.String queryString,
            final java.lang.String name ) {
        return ( ubic.gemma.model.expression.experiment.ExperimentalDesign ) this.findByName( TRANSFORM_NONE,
                queryString, name );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#findOrCreate(int, java.lang.String,
     *      ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */

    public Object findOrCreate( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( experimentalDesign );
        argNames.add( "experimentalDesign" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.expression.experiment.ExperimentalDesign"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, ( ubic.gemma.model.expression.experiment.ExperimentalDesign ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#findOrCreate(int,
     *      ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */

    public Object findOrCreate( final int transform,
            final ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) {
        return this
                .findOrCreate(
                        transform,
                        "from ubic.gemma.model.expression.experiment.ExperimentalDesign as experimentalDesign where experimentalDesign.experimentalDesign = :experimentalDesign",
                        experimentalDesign );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#findOrCreate(java.lang.String,
     *      ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */

    public ubic.gemma.model.expression.experiment.ExperimentalDesign findOrCreate( final java.lang.String queryString,
            final ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) {
        return ( ubic.gemma.model.expression.experiment.ExperimentalDesign ) this.findOrCreate( TRANSFORM_NONE,
                queryString, experimentalDesign );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#findOrCreate(ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */
    public ubic.gemma.model.expression.experiment.ExperimentalDesign findOrCreate(
            ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) {
        return ( ubic.gemma.model.expression.experiment.ExperimentalDesign ) this.findOrCreate( TRANSFORM_NONE,
                experimentalDesign );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#getExpressionExperiment(ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperiment getExpressionExperiment(
            final ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) {
        try {
            return this.handleGetExpressionExperiment( experimentalDesign );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExperimentalDesignDao.getExpressionExperiment(ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign)' --> "
                            + th, th );
        }
    }

    public Collection<? extends ExperimentalDesign> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from ExperimentalDesignImpl where id in (:ids)", "ids",
                ids );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#load(int, java.lang.Long)
     */

    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExperimentalDesign.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.expression.experiment.ExperimentalDesignImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.expression.experiment.ExperimentalDesign ) entity );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#load(java.lang.Long)
     */

    public ExperimentalDesign load( java.lang.Long id ) {
        return ( ubic.gemma.model.expression.experiment.ExperimentalDesign ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#loadAll()
     */

    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#loadAll(int)
     */

    public java.util.Collection loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.expression.experiment.ExperimentalDesignImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExperimentalDesign.remove - 'id' can not be null" );
        }
        ubic.gemma.model.expression.experiment.ExperimentalDesign entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExperimentalDesign.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#remove(ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */
    public void remove( ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) {
        if ( experimentalDesign == null ) {
            throw new IllegalArgumentException( "ExperimentalDesign.remove - 'experimentalDesign' can not be null" );
        }
        this.getHibernateTemplate().delete( experimentalDesign );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection<? extends ExperimentalDesign> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExperimentalDesign.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.expression.experiment.ExperimentalDesign ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#update(ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */
    public void update( ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) {
        if ( experimentalDesign == null ) {
            throw new IllegalArgumentException( "ExperimentalDesign.update - 'experimentalDesign' can not be null" );
        }
        this.getHibernateTemplate().update( experimentalDesign );
    }

    /**
     * Performs the core logic for
     * {@link #getExpressionExperiment(ubic.gemma.model.expression.experiment.ExperimentalDesign)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExpressionExperiment handleGetExpressionExperiment(
            ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) throws java.lang.Exception;

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.expression.experiment.ExperimentalDesign)} method. This method does
     * not instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in
     *        <code>ubic.gemma.model.expression.experiment.ExperimentalDesignDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.expression.experiment.ExperimentalDesign)
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
     * <code>ubic.gemma.model.expression.experiment.ExperimentalDesignDao</code>, please note that the
     * {@link #TRANSFORM_NONE} constant denotes no transformation, so the entity itself will be returned. If the integer
     * argument value is unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in
     *        {@link ubic.gemma.model.expression.experiment.ExperimentalDesignDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform,
            final ubic.gemma.model.expression.experiment.ExperimentalDesign entity ) {
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