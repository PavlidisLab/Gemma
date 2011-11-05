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
package ubic.gemma.model.genome.sequenceAnalysis;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.sequenceAnalysis.BlatResult</code>.
 * </p>
 * 
 * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResult
 */
public abstract class BlatResultDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao {

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#create(int, java.util.Collection)
     */
    public java.util.Collection create( final int transform, final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BlatResult.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform,
                                    ( ubic.gemma.model.genome.sequenceAnalysis.BlatResult ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#create(int transform,
     *      ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    public BlatResult create( final int transform, final ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        if ( blatResult == null ) {
            throw new IllegalArgumentException( "BlatResult.create - 'blatResult' can not be null" );
        }
        this.getHibernateTemplate().save( blatResult );
        return blatResult;
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#create(java.util.Collection)
     */

    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#create(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    public BlatResult create( ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        return this.create( TRANSFORM_NONE, blatResult );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#find(int, java.lang.String,
     *      ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */

    public BlatResult find( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( blatResult );
        argNames.add( "blatResult" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.genome.sequenceAnalysis.BlatResult"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return ( BlatResult ) result;
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#find(int,
     *      ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */

    public Object find( final int transform, final ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        return this
                .find( transform,
                        "from ubic.gemma.model.genome.sequenceAnalysis.BlatResult as blatResult where blatResult.blatResult = :blatResult",
                        blatResult );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#find(java.lang.String,
     *      ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */

    public ubic.gemma.model.genome.sequenceAnalysis.BlatResult find( final java.lang.String queryString,
            final ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        return this.find( TRANSFORM_NONE, queryString, blatResult );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#find(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    public ubic.gemma.model.genome.sequenceAnalysis.BlatResult find(
            ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        return ( ubic.gemma.model.genome.sequenceAnalysis.BlatResult ) this.find( TRANSFORM_NONE, blatResult );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#findByBioSequence(int, java.lang.String,
     *      ubic.gemma.model.genome.biosequence.BioSequence)
     */

    public java.util.Collection findByBioSequence( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( bioSequence );
        argNames.add( "bioSequence" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#findByBioSequence(int,
     *      ubic.gemma.model.genome.biosequence.BioSequence)
     */

    public java.util.Collection findByBioSequence( final int transform,
            final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        return this.findByBioSequence( transform, "from BlatResultImpl br where br.querySequence = :bioSequence",
                bioSequence );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#findByBioSequence(java.lang.String,
     *      ubic.gemma.model.genome.biosequence.BioSequence)
     */

    public java.util.Collection findByBioSequence( final java.lang.String queryString,
            final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        return this.findByBioSequence( TRANSFORM_NONE, queryString, bioSequence );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#findByBioSequence(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    public java.util.Collection findByBioSequence( ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        return this.findByBioSequence( TRANSFORM_NONE, bioSequence );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#findOrCreate(int, java.lang.String,
     *      ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */

    public Object findOrCreate( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( blatResult );
        argNames.add( "blatResult" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.genome.sequenceAnalysis.BlatResult"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, ( ubic.gemma.model.genome.sequenceAnalysis.BlatResult ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#findOrCreate(int,
     *      ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */

    public Object findOrCreate( final int transform,
            final ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        return this
                .findOrCreate(
                        transform,
                        "from ubic.gemma.model.genome.sequenceAnalysis.BlatResult as blatResult where blatResult.blatResult = :blatResult",
                        blatResult );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#findOrCreate(java.lang.String,
     *      ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */

    public ubic.gemma.model.genome.sequenceAnalysis.BlatResult findOrCreate( final java.lang.String queryString,
            final ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        return ( ubic.gemma.model.genome.sequenceAnalysis.BlatResult ) this.findOrCreate( TRANSFORM_NONE, queryString,
                blatResult );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#findOrCreate(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    public ubic.gemma.model.genome.sequenceAnalysis.BlatResult findOrCreate(
            ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        return ( ubic.gemma.model.genome.sequenceAnalysis.BlatResult ) this.findOrCreate( TRANSFORM_NONE, blatResult );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#load(int, java.lang.Long)
     */
    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BlatResult.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.genome.sequenceAnalysis.BlatResultImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.genome.sequenceAnalysis.BlatResult ) entity );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#load(java.lang.Long)
     */
    public BlatResult load( java.lang.Long id ) {
        return ( ubic.gemma.model.genome.sequenceAnalysis.BlatResult ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#load(java.util.Collection)
     */
    public java.util.Collection load( final java.util.Collection ids ) {
        try {
            return this.handleLoad( ids );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao.load(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#loadAll()
     */
    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#loadAll(int)
     */
    public java.util.Collection loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.sequenceAnalysis.BlatResultImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#remove(java.lang.Long)
     */
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BlatResult.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.sequenceAnalysis.BlatResult entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.SequenceSimilaritySearchResultDao#remove(java.util.Collection)
     */
    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BlatResult.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#remove(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    public void remove( ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        if ( blatResult == null ) {
            throw new IllegalArgumentException( "BlatResult.remove - 'blatResult' can not be null" );
        }
        this.getHibernateTemplate().delete( blatResult );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.SequenceSimilaritySearchResultDao#update(java.util.Collection)
     */
    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BlatResult.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.genome.sequenceAnalysis.BlatResult ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#update(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    public void update( ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        if ( blatResult == null ) {
            throw new IllegalArgumentException( "BlatResult.update - 'blatResult' can not be null" );
        }
        this.getHibernateTemplate().update( blatResult );
    }

    /**
     * Performs the core logic for {@link #load(java.util.Collection)}
     */
    protected abstract java.util.Collection handleLoad( java.util.Collection ids ) throws java.lang.Exception;

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.genome.sequenceAnalysis.BlatResult)} method. This method does not
     * instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in
     *        <code>ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
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
     * <code>ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao</code>, please note that the {@link #TRANSFORM_NONE}
     * constant denotes no transformation, so the entity itself will be returned. If the integer argument value is
     * unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform,
            final ubic.gemma.model.genome.sequenceAnalysis.BlatResult entity ) {
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