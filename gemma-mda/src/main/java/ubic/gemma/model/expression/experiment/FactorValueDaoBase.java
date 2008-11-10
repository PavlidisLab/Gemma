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

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.experiment.FactorValue</code>.
 * </p>
 * 
 * @see ubic.gemma.model.expression.experiment.FactorValue
 */
public abstract class FactorValueDaoBase extends org.springframework.orm.hibernate3.support.HibernateDaoSupport
        implements ubic.gemma.model.expression.experiment.FactorValueDao {

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#create(int, java.util.Collection)
     */
    public java.util.Collection create( final int transform, final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "FactorValue.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform, ( ubic.gemma.model.expression.experiment.FactorValue ) entityIterator
                                    .next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#create(int transform,
     *      ubic.gemma.model.expression.experiment.FactorValue)
     */
    public Object create( final int transform, final ubic.gemma.model.expression.experiment.FactorValue factorValue ) {
        if ( factorValue == null ) {
            throw new IllegalArgumentException( "FactorValue.create - 'factorValue' can not be null" );
        }
        this.getHibernateTemplate().save( factorValue );
        return this.transformEntity( transform, factorValue );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#create(java.util.Collection)
     */
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#create(ubic.gemma.model.expression.experiment.FactorValue)
     */
    public ubic.gemma.model.expression.experiment.FactorValue create(
            ubic.gemma.model.expression.experiment.FactorValue factorValue ) {
        return ( ubic.gemma.model.expression.experiment.FactorValue ) this.create( TRANSFORM_NONE, factorValue );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#find(int, java.lang.String,
     *      ubic.gemma.model.expression.experiment.FactorValue)
     */
    @SuppressWarnings( { "unchecked" })
    public Object find( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.expression.experiment.FactorValue factorValue ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( factorValue );
        argNames.add( "factorValue" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of 'ubic.gemma.model.expression.experiment.FactorValue"
                                + "' was found when executing query --> '" + queryString + "'" );
            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        result = transformEntity( transform, ( ubic.gemma.model.expression.experiment.FactorValue ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#find(int,
     *      ubic.gemma.model.expression.experiment.FactorValue)
     */
    @SuppressWarnings( { "unchecked" })
    public Object find( final int transform, final ubic.gemma.model.expression.experiment.FactorValue factorValue ) {
        return this
                .find(
                        transform,
                        "from ubic.gemma.model.expression.experiment.FactorValue as factorValue where factorValue.factorValue = :factorValue",
                        factorValue );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#find(java.lang.String,
     *      ubic.gemma.model.expression.experiment.FactorValue)
     */
    @SuppressWarnings( { "unchecked" })
    public ubic.gemma.model.expression.experiment.FactorValue find( final java.lang.String queryString,
            final ubic.gemma.model.expression.experiment.FactorValue factorValue ) {
        return ( ubic.gemma.model.expression.experiment.FactorValue ) this.find( TRANSFORM_NONE, queryString,
                factorValue );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#find(ubic.gemma.model.expression.experiment.FactorValue)
     */
    public ubic.gemma.model.expression.experiment.FactorValue find(
            ubic.gemma.model.expression.experiment.FactorValue factorValue ) {
        return ( ubic.gemma.model.expression.experiment.FactorValue ) this.find( TRANSFORM_NONE, factorValue );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#findOrCreate(int, java.lang.String,
     *      ubic.gemma.model.expression.experiment.FactorValue)
     */
    @SuppressWarnings( { "unchecked" })
    public Object findOrCreate( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.expression.experiment.FactorValue factorValue ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( factorValue );
        argNames.add( "factorValue" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of 'ubic.gemma.model.expression.experiment.FactorValue"
                                + "' was found when executing query --> '" + queryString + "'" );
            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        result = transformEntity( transform, ( ubic.gemma.model.expression.experiment.FactorValue ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#findOrCreate(int,
     *      ubic.gemma.model.expression.experiment.FactorValue)
     */
    @SuppressWarnings( { "unchecked" })
    public Object findOrCreate( final int transform,
            final ubic.gemma.model.expression.experiment.FactorValue factorValue ) {
        return this
                .findOrCreate(
                        transform,
                        "from ubic.gemma.model.expression.experiment.FactorValue as factorValue where factorValue.factorValue = :factorValue",
                        factorValue );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#findOrCreate(java.lang.String,
     *      ubic.gemma.model.expression.experiment.FactorValue)
     */
    @SuppressWarnings( { "unchecked" })
    public ubic.gemma.model.expression.experiment.FactorValue findOrCreate( final java.lang.String queryString,
            final ubic.gemma.model.expression.experiment.FactorValue factorValue ) {
        return ( ubic.gemma.model.expression.experiment.FactorValue ) this.findOrCreate( TRANSFORM_NONE, queryString,
                factorValue );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#findOrCreate(ubic.gemma.model.expression.experiment.FactorValue)
     */
    public ubic.gemma.model.expression.experiment.FactorValue findOrCreate(
            ubic.gemma.model.expression.experiment.FactorValue factorValue ) {
        return ( ubic.gemma.model.expression.experiment.FactorValue ) this.findOrCreate( TRANSFORM_NONE, factorValue );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#load(int, java.lang.Long)
     */
    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "FactorValue.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.expression.experiment.FactorValueImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.expression.experiment.FactorValue ) entity );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#load(java.lang.Long)
     */
    public ubic.gemma.model.expression.experiment.FactorValue load( java.lang.Long id ) {
        return ( ubic.gemma.model.expression.experiment.FactorValue ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#loadAll()
     */
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#loadAll(int)
     */
    public java.util.Collection loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.expression.experiment.FactorValueImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#remove(java.lang.Long)
     */
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "FactorValue.remove - 'id' can not be null" );
        }
        ubic.gemma.model.expression.experiment.FactorValue entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#remove(java.util.Collection)
     */
    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "FactorValue.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#remove(ubic.gemma.model.expression.experiment.FactorValue)
     */
    public void remove( ubic.gemma.model.expression.experiment.FactorValue factorValue ) {
        if ( factorValue == null ) {
            throw new IllegalArgumentException( "FactorValue.remove - 'factorValue' can not be null" );
        }
        this.getHibernateTemplate().delete( factorValue );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#update(java.util.Collection)
     */
    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "FactorValue.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.expression.experiment.FactorValue ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#update(ubic.gemma.model.expression.experiment.FactorValue)
     */
    public void update( ubic.gemma.model.expression.experiment.FactorValue factorValue ) {
        if ( factorValue == null ) {
            throw new IllegalArgumentException( "FactorValue.update - 'factorValue' can not be null" );
        }
        this.getHibernateTemplate().update( factorValue );
    }

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.expression.experiment.FactorValue)} method. This method does not
     * instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in
     *        <code>ubic.gemma.model.expression.experiment.FactorValueDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.expression.experiment.FactorValue)
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
     * <code>ubic.gemma.model.expression.experiment.FactorValueDao</code>, please note that the {@link #TRANSFORM_NONE}
     * constant denotes no transformation, so the entity itself will be returned. If the integer argument value is
     * unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.expression.experiment.FactorValueDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform,
            final ubic.gemma.model.expression.experiment.FactorValue entity ) {
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