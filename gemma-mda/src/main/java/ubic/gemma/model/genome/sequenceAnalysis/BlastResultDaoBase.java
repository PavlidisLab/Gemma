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

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.sequenceAnalysis.BlastResult</code>.
 * 
 * @see ubic.gemma.model.genome.sequenceAnalysis.BlastResult
 */
public abstract class BlastResultDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.genome.sequenceAnalysis.BlastResultDao {

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlastResultDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends BlastResult> create(
            final java.util.Collection<? extends BlastResult> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BlastResult.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends BlastResult> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlastResultDao#create(int transform,
     *      ubic.gemma.model.genome.sequenceAnalysis.BlastResult)
     */
    @Override
    public BlastResult create( final ubic.gemma.model.genome.sequenceAnalysis.BlastResult blastResult ) {
        if ( blastResult == null ) {
            throw new IllegalArgumentException( "BlastResult.create - 'blastResult' can not be null" );
        }
        this.getHibernateTemplate().save( blastResult );
        return blastResult;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#load(java.util.Collection)
     */

    @Override
    public Collection<? extends BlastResult> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from BlastResultImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlastResultDao#load(int, java.lang.Long)
     */
    @Override
    public BlastResult load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BlastResult.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.genome.sequenceAnalysis.BlastResultImpl.class, id );
        return ( ubic.gemma.model.genome.sequenceAnalysis.BlastResult ) entity;
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlastResultDao#loadAll(int)
     */
    @SuppressWarnings("unchecked")
    @Override
    public java.util.Collection<? extends BlastResult> loadAll() {
        final java.util.Collection<?> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.sequenceAnalysis.BlastResultImpl.class );

        return ( Collection<? extends BlastResult> ) results;
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlastResultDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BlastResult.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.sequenceAnalysis.BlastResult entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.SequenceSimilaritySearchResultDao#remove(java.util.Collection)
     */
    @Override
    public void remove( java.util.Collection<? extends BlastResult> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BlastResult.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlastResultDao#remove(ubic.gemma.model.genome.sequenceAnalysis.BlastResult)
     */
    @Override
    public void remove( ubic.gemma.model.genome.sequenceAnalysis.BlastResult blastResult ) {
        if ( blastResult == null ) {
            throw new IllegalArgumentException( "BlastResult.remove - 'blastResult' can not be null" );
        }
        this.getHibernateTemplate().delete( blastResult );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.SequenceSimilaritySearchResultDao#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection<? extends BlastResult> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BlastResult.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends BlastResult> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlastResultDao#update(ubic.gemma.model.genome.sequenceAnalysis.BlastResult)
     */
    @Override
    public void update( ubic.gemma.model.genome.sequenceAnalysis.BlastResult blastResult ) {
        if ( blastResult == null ) {
            throw new IllegalArgumentException( "BlastResult.update - 'blastResult' can not be null" );
        }
        this.getHibernateTemplate().update( blastResult );
    }

}