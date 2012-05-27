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
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.biosequence.BioSequence</code>.
 * 
 * @see ubic.gemma.model.genome.biosequence.BioSequence
 * @version $Id$
 */
public abstract class BioSequenceDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.genome.biosequence.BioSequenceDao {

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#countAll()
     */
    @Override
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
    @Override
    public java.util.Collection<? extends BioSequence> create(
            final java.util.Collection<? extends BioSequence> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BioSequence.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends BioSequence> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
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
    @Override
    public BioSequence create( final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        if ( bioSequence == null ) {
            throw new IllegalArgumentException( "BioSequence.create - 'bioSequence' can not be null" );
        }
        this.getHibernateTemplate().save( bioSequence );
        return bioSequence;
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#findByGenes(java.util.Collection)
     */
    @Override
    public java.util.Map<Gene, Collection<BioSequence>> findByGenes( final java.util.Collection<Gene> genes ) {

        return this.handleFindByGenes( genes );

    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#findByName(java.lang.String)
     */
    @Override
    public java.util.Collection<BioSequence> findByName( final java.lang.String name ) {

        return this.handleFindByName( name );

    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#getGenesByAccession(java.lang.String)
     */
    @Override
    public java.util.Collection<Gene> getGenesByAccession( final java.lang.String search ) {

        return this.handleGetGenesByAccession( search );

    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#getGenesByName(java.lang.String)
     */
    @Override
    public java.util.Collection<Gene> getGenesByName( final java.lang.String search ) {

        return this.handleGetGenesByName( search );

    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#load(int, java.lang.Long)
     */

    @Override
    public BioSequence load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BioSequence.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.genome.biosequence.BioSequenceImpl.class, id );
        return ( ubic.gemma.model.genome.biosequence.BioSequence ) entity;
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#load(java.util.Collection)
     */
    @Override
    public java.util.Collection<BioSequence> load( final java.util.Collection<Long> ids ) {

        return this.handleLoad( ids );

    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#loadAll(int)
     */

    @Override
    public java.util.Collection<? extends BioSequence> loadAll() {
        final java.util.Collection<? extends BioSequence> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.biosequence.BioSequenceImpl.class );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#remove(java.lang.Long)
     */

    @Override
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

    @Override
    public void remove( java.util.Collection<? extends BioSequence> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BioSequence.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#remove(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    public void remove( ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        if ( bioSequence == null ) {
            throw new IllegalArgumentException( "BioSequence.remove - 'bioSequence' can not be null" );
        }
        this.getHibernateTemplate().delete( bioSequence );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#thaw(java.util.Collection)
     */
    @Override
    public Collection<BioSequence> thaw( final java.util.Collection<BioSequence> bioSequences ) {

        return this.handleThaw( bioSequences );

    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDao#thaw(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    public BioSequence thaw( final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {

        return this.handleThaw( bioSequence );

    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    @Override
    public void update( final java.util.Collection<? extends BioSequence> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BioSequence.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
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
    @Override
    public void update( ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        if ( bioSequence == null ) {
            throw new IllegalArgumentException( "BioSequence.update - 'bioSequence' can not be null" );
        }
        this.getHibernateTemplate().update( bioSequence );
    }

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll();

    /**
     * Performs the core logic for {@link #findByGenes(java.util.Collection)}
     */
    protected abstract java.util.Map<Gene, Collection<BioSequence>> handleFindByGenes( java.util.Collection<Gene> genes );

    /**
     * Performs the core logic for {@link #findByName(java.lang.String)}
     */
    protected abstract java.util.Collection<BioSequence> handleFindByName( java.lang.String name );

    /**
     * Performs the core logic for {@link #getGenesByAccession(java.lang.String)}
     */
    protected abstract java.util.Collection<Gene> handleGetGenesByAccession( java.lang.String search );

    /**
     * Performs the core logic for {@link #getGenesByName(java.lang.String)}
     */
    protected abstract java.util.Collection<Gene> handleGetGenesByName( java.lang.String search );

    /**
     * Performs the core logic for {@link #load(java.util.Collection)}
     */
    protected abstract java.util.Collection<BioSequence> handleLoad( java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #thaw(java.util.Collection)}
     */
    protected abstract Collection<BioSequence> handleThaw( java.util.Collection<BioSequence> bioSequences );

    /**
     * Performs the core logic for {@link #thaw(ubic.gemma.model.genome.biosequence.BioSequence)}
     */
    protected abstract BioSequence handleThaw( ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

}