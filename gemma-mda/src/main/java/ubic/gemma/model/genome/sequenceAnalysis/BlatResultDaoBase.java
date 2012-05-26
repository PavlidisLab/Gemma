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
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#create(int transform,
     *      ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    @Override
    public BlatResult create( final BlatResult blatResult ) {
        if ( blatResult == null ) {
            throw new IllegalArgumentException( "BlatResult.create - 'blatResult' can not be null" );
        }
        this.getHibernateTemplate().save( blatResult );
        return blatResult;
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends BlatResult> create( final java.util.Collection<? extends BlatResult> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BlatResult.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends BlatResult> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#load(int, java.lang.Long)
     */
    @Override
    public BlatResult load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BlatResult.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.genome.sequenceAnalysis.BlatResultImpl.class, id );
        return ( ubic.gemma.model.genome.sequenceAnalysis.BlatResult ) entity;
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#load(java.util.Collection)
     */
    @Override
    public java.util.Collection<BlatResult> load( final java.util.Collection<Long> ids ) {
        try {
            return this.handleLoad( ids );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao.load(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#loadAll(int)
     */
    @Override
    @SuppressWarnings("unchecked")
    public java.util.Collection<BlatResult> loadAll() {
        final java.util.Collection<?> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.sequenceAnalysis.BlatResultImpl.class );
        return ( Collection<BlatResult> ) results;
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#remove(java.lang.Long)
     */
    @Override
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
    @Override
    public void remove( java.util.Collection<? extends BlatResult> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BlatResult.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#remove(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    @Override
    public void remove( ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        if ( blatResult == null ) {
            throw new IllegalArgumentException( "BlatResult.remove - 'blatResult' can not be null" );
        }
        this.getHibernateTemplate().delete( blatResult );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.SequenceSimilaritySearchResultDao#update(java.util.Collection)
     */
    @Override
    public void update( final Collection<? extends BlatResult> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BlatResult.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends BlatResult> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#update(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    @Override
    public void update( ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        if ( blatResult == null ) {
            throw new IllegalArgumentException( "BlatResult.update - 'blatResult' can not be null" );
        }
        this.getHibernateTemplate().update( blatResult );
    }

    /**
     * Performs the core logic for {@link #load(java.util.Collection)}
     */
    protected abstract java.util.Collection<BlatResult> handleLoad( java.util.Collection<Long> ids )
            throws java.lang.Exception;

}