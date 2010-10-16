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
package ubic.gemma.model.genome.biosequence;

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.model.genome.Gene;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.biosequence.BioSequence</code>.
 * </p>
 * 
 * @see ubic.gemma.model.genome.biosequence.BioSequence
 */
public abstract class BioSequenceDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.genome.biosequence.BioSequenceDao {

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#countAll()
     */
    public java.lang.Integer countAll() {
        try {
            return this.handleCountAll();
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceDao.countAll()' --> " + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#create(int, java.util.Collection)
     */
    public java.util.Collection<BioSequence> create( final int transform,
            final java.util.Collection<BioSequence> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BioSequence.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<BioSequence> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( transform, entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#create(int transform,
     *      ubic.gemma.model.genome.biosequence.BioSequence)
     */
    public BioSequence create( final int transform, final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        if ( bioSequence == null ) {
            throw new IllegalArgumentException( "BioSequence.create - 'bioSequence' can not be null" );
        }
        this.getHibernateTemplate().save( bioSequence );
        return this.transformEntity( transform, bioSequence );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#create(java.util.Collection)
     */

    public java.util.Collection<BioSequence> create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#create(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    public BioSequence create( ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        return this.create( TRANSFORM_NONE, bioSequence );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#find(int, java.lang.String,
     *      ubic.gemma.model.genome.biosequence.BioSequence)
     */

    public BioSequence find( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( bioSequence );
        argNames.add( "bioSequence" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.genome.biosequence.BioSequence"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, ( ubic.gemma.model.genome.biosequence.BioSequence ) result );
        return ( BioSequence ) result;
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#find(int,
     *      ubic.gemma.model.genome.biosequence.BioSequence)
     */
    public BioSequence find( final int transform, final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        return this
                .find(
                        transform,
                        "from ubic.gemma.model.genome.biosequence.BioSequence as bioSequence where bioSequence.bioSequence = :bioSequence",
                        bioSequence );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#find(java.lang.String,
     *      ubic.gemma.model.genome.biosequence.BioSequence)
     */
    public ubic.gemma.model.genome.biosequence.BioSequence find( final java.lang.String queryString,
            final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        return this.find( TRANSFORM_NONE, queryString, bioSequence );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#find(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    public ubic.gemma.model.genome.biosequence.BioSequence find(
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        return this.find( TRANSFORM_NONE, bioSequence );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#findByAccession(int, java.lang.String,
     *      ubic.gemma.model.common.description.DatabaseEntry)
     */

    public BioSequence findByAccession( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.common.description.DatabaseEntry accession ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( accession );
        argNames.add( "accession" );
        java.util.Set<BioSequence> results = new java.util.LinkedHashSet<BioSequence>( this.getHibernateTemplate()
                .findByNamedParam( queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        BioSequence result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.genome.biosequence.BioSequence"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, result );
        return result;
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#findByAccession(int,
     *      ubic.gemma.model.common.description.DatabaseEntry)
     */
    public BioSequence findByAccession( final int transform,
            final ubic.gemma.model.common.description.DatabaseEntry accession ) {
        return this
                .findByAccession(
                        transform,
                        "from ubic.gemma.model.genome.biosequence.BioSequence as bioSequence where bioSequence.accession = :accession",
                        accession );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#findByAccession(java.lang.String,
     *      ubic.gemma.model.common.description.DatabaseEntry)
     */
    public ubic.gemma.model.genome.biosequence.BioSequence findByAccession( final java.lang.String queryString,
            final ubic.gemma.model.common.description.DatabaseEntry accession ) {
        return this.findByAccession( TRANSFORM_NONE, queryString, accession );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#findByAccession(ubic.gemma.model.common.description.DatabaseEntry)
     */
    public ubic.gemma.model.genome.biosequence.BioSequence findByAccession(
            ubic.gemma.model.common.description.DatabaseEntry accession ) {
        return this.findByAccession( TRANSFORM_NONE, accession );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#findByGenes(java.util.Collection)
     */
    public java.util.Map<Gene, Collection<BioSequence>> findByGenes( final java.util.Collection<Gene> genes ) {
        try {
            return this.handleFindByGenes( genes );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceDao.findByGenes(java.util.Collection genes)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#findByName(java.lang.String)
     */
    public java.util.Collection<BioSequence> findByName( final java.lang.String name ) {
        try {
            return this.handleFindByName( name );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceDao.findByName(java.lang.String name)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#findOrCreate(int, java.lang.String,
     *      ubic.gemma.model.genome.biosequence.BioSequence)
     */

    public BioSequence findOrCreate( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( bioSequence );
        argNames.add( "bioSequence" );
        java.util.Set<BioSequence> results = new java.util.LinkedHashSet<BioSequence>( this.getHibernateTemplate()
                .findByNamedParam( queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        BioSequence result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.genome.biosequence.BioSequence"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, result );
        return result;
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#findOrCreate(int,
     *      ubic.gemma.model.genome.biosequence.BioSequence)
     */
    public BioSequence findOrCreate( final int transform,
            final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        return this
                .findOrCreate(
                        transform,
                        "from ubic.gemma.model.genome.biosequence.BioSequence as bioSequence where bioSequence.bioSequence = :bioSequence",
                        bioSequence );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#findOrCreate(java.lang.String,
     *      ubic.gemma.model.genome.biosequence.BioSequence)
     */
    public ubic.gemma.model.genome.biosequence.BioSequence findOrCreate( final java.lang.String queryString,
            final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        return this.findOrCreate( TRANSFORM_NONE, queryString, bioSequence );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#findOrCreate(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    public ubic.gemma.model.genome.biosequence.BioSequence findOrCreate(
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        return this.findOrCreate( TRANSFORM_NONE, bioSequence );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#getGenesByAccession(java.lang.String)
     */
    public java.util.Collection<Gene> getGenesByAccession( final java.lang.String search ) {
        try {
            return this.handleGetGenesByAccession( search );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceDao.getGenesByAccession(java.lang.String search)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#getGenesByName(java.lang.String)
     */
    public java.util.Collection<Gene> getGenesByName( final java.lang.String search ) {
        try {
            return this.handleGetGenesByName( search );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceDao.getGenesByName(java.lang.String search)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#load(int, java.lang.Long)
     */

    public BioSequence load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BioSequence.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.genome.biosequence.BioSequenceImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.genome.biosequence.BioSequence ) entity );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#load(java.lang.Long)
     */

    public BioSequence load( java.lang.Long id ) {
        return this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#load(java.util.Collection)
     */
    public java.util.Collection<BioSequence> load( final java.util.Collection<Long> ids ) {
        try {
            return this.handleLoad( ids );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceDao.load(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#loadAll()
     */

    public java.util.Collection<? extends BioSequence> loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#loadAll(int)
     */

    public java.util.Collection<? extends BioSequence> loadAll( final int transform ) {
        final java.util.Collection<? extends BioSequence> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.biosequence.BioSequenceImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BioSequence.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.biosequence.BioSequence entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection<? extends BioSequence> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BioSequence.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#remove(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    public void remove( ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        if ( bioSequence == null ) {
            throw new IllegalArgumentException( "BioSequence.remove - 'bioSequence' can not be null" );
        }
        this.getHibernateTemplate().delete( bioSequence );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#thaw(java.util.Collection)
     */
    public Collection<BioSequence> thaw( final java.util.Collection<BioSequence> bioSequences ) {
        try {
            return this.handleThaw( bioSequences );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceDao.thaw(java.util.Collection bioSequences)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#thaw(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    public BioSequence thaw( final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        try {
            return this.handleThaw( bioSequence );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceDao.thaw(ubic.gemma.model.genome.biosequence.BioSequence bioSequence)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection<? extends BioSequence> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BioSequence.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends BioSequence> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#update(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    public void update( ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        if ( bioSequence == null ) {
            throw new IllegalArgumentException( "BioSequence.update - 'bioSequence' can not be null" );
        }
        this.getHibernateTemplate().update( bioSequence );
    }

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByGenes(java.util.Collection)}
     */
    protected abstract java.util.Map<Gene, Collection<BioSequence>> handleFindByGenes( java.util.Collection<Gene> genes )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByName(java.lang.String)}
     */
    protected abstract java.util.Collection<BioSequence> handleFindByName( java.lang.String name )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getGenesByAccession(java.lang.String)}
     */
    protected abstract java.util.Collection<Gene> handleGetGenesByAccession( java.lang.String search )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getGenesByName(java.lang.String)}
     */
    protected abstract java.util.Collection<Gene> handleGetGenesByName( java.lang.String search )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.util.Collection)}
     */
    protected abstract java.util.Collection<BioSequence> handleLoad( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(java.util.Collection)}
     */
    protected abstract Collection<BioSequence> handleThaw( java.util.Collection<BioSequence> bioSequences )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(ubic.gemma.model.genome.biosequence.BioSequence)}
     */
    protected abstract BioSequence handleThaw( ubic.gemma.model.genome.biosequence.BioSequence bioSequence )
            throws java.lang.Exception;

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.genome.biosequence.BioSequence)} method. This method does not
     * instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in <code>ubic.gemma.model.genome.biosequence.BioSequenceDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.genome.biosequence.BioSequence)
     */

    protected void transformEntities( final int transform, final java.util.Collection<? extends BioSequence> entities ) {
        switch ( transform ) {
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in
     * <code>ubic.gemma.model.genome.biosequence.BioSequenceDao</code>, please note that the {@link #TRANSFORM_NONE}
     * constant denotes no transformation, so the entity itself will be returned. If the integer argument value is
     * unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.genome.biosequence.BioSequenceDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected BioSequence transformEntity( final int transform,
            final ubic.gemma.model.genome.biosequence.BioSequence entity ) {
        BioSequence target = null;
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