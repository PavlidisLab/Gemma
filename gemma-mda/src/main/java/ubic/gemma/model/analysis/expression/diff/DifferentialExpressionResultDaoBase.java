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
package ubic.gemma.model.analysis.expression.diff;

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult</code>.
 * </p>
 * 
 * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult
 */
public abstract class DifferentialExpressionResultDaoBase extends HibernateDaoSupport implements
        DifferentialExpressionResultDao {

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends DifferentialExpressionAnalysisResult> create(
            final java.util.Collection<? extends DifferentialExpressionAnalysisResult> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException(
                    "DifferentialExpressionAnalysisResult.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends DifferentialExpressionAnalysisResult> entityIterator = entities
                                .iterator(); entityIterator.hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultDao#create(int transform,
     *      ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult)
     */
    @Override
    public DifferentialExpressionAnalysisResult create(
            final DifferentialExpressionAnalysisResult DifferentialExpressionAnalysisResult ) {
        if ( DifferentialExpressionAnalysisResult == null ) {
            throw new IllegalArgumentException(
                    "DifferentialExpressionAnalysisResult.create - 'DifferentialExpressionAnalysisResult' can not be null" );
        }
        this.getHibernateTemplate().save( DifferentialExpressionAnalysisResult );
        return DifferentialExpressionAnalysisResult;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#getExperimentalFactors(java.util.Collection)
     */
    @Override
    public java.util.Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> getExperimentalFactors(
            final java.util.Collection<DifferentialExpressionAnalysisResult> DifferentialExpressionAnalysisResults ) {
        try {
            return this.handleGetExperimentalFactors( DifferentialExpressionAnalysisResults );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultDao.getExperimentalFactors(java.util.Collection DifferentialExpressionAnalysisResults)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#getExperimentalFactors(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult)
     */
    @Override
    public java.util.Collection<ExperimentalFactor> getExperimentalFactors(
            final DifferentialExpressionAnalysisResult DifferentialExpressionAnalysisResult ) {
        try {
            return this.handleGetExperimentalFactors( DifferentialExpressionAnalysisResult );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'diff.DifferentialExpressionAnalysisResultDao.getExperimentalFactors(diff.DifferentialExpressionAnalysisResult DifferentialExpressionAnalysisResult)' --> "
                            + th, th );
        }
    }

    

    /**
     * @see diff.DifferentialExpressionResultDao#load(int, java.lang.Long)
     */
    @Override
    public DifferentialExpressionAnalysisResult load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "DifferentialExpressionAnalysisResult.load - 'id' can not be null" );
        }
        return this.getHibernateTemplate().get( DifferentialExpressionAnalysisResultImpl.class, id );
    }

    /**
     * @see diff.DifferentialExpressionResultDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "DifferentialExpressionAnalysisResult.remove - 'id' can not be null" );
        }
        DifferentialExpressionAnalysisResult entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisResultDao#remove(java.util.Collection)
     */
    @Override
    public void remove( java.util.Collection<? extends DifferentialExpressionAnalysisResult> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException(
                    "DifferentialExpressionAnalysisResult.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see diff.DifferentialExpressionResultDao#remove(diff.DifferentialExpressionAnalysisResult)
     */
    @Override
    public void remove( DifferentialExpressionAnalysisResult DifferentialExpressionAnalysisResult ) {
        if ( DifferentialExpressionAnalysisResult == null ) {
            throw new IllegalArgumentException(
                    "DifferentialExpressionAnalysisResult.remove - 'DifferentialExpressionAnalysisResult' can not be null" );
        }
        this.getHibernateTemplate().delete( DifferentialExpressionAnalysisResult );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisResultDao#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection<? extends DifferentialExpressionAnalysisResult> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException(
                    "DifferentialExpressionAnalysisResult.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends DifferentialExpressionAnalysisResult> entityIterator = entities
                                .iterator(); entityIterator.hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see diff.DifferentialExpressionResultDao#update(diff.DifferentialExpressionAnalysisResult)
     */
    @Override
    public void update( DifferentialExpressionAnalysisResult DifferentialExpressionAnalysisResult ) {
        if ( DifferentialExpressionAnalysisResult == null ) {
            throw new IllegalArgumentException(
                    "DifferentialExpressionAnalysisResult.update - 'DifferentialExpressionAnalysisResult' can not be null" );
        }
        this.getHibernateTemplate().update( DifferentialExpressionAnalysisResult );
    }

    /**
     * Performs the core logic for {@link #getExperimentalFactors(java.util.Collection)}
     */
    protected abstract java.util.Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> handleGetExperimentalFactors(
            java.util.Collection<DifferentialExpressionAnalysisResult> DifferentialExpressionAnalysisResults )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getExperimentalFactors(diff.DifferentialExpressionAnalysisResult)}
     */
    protected abstract java.util.Collection<ExperimentalFactor> handleGetExperimentalFactors(
            DifferentialExpressionAnalysisResult DifferentialExpressionAnalysisResult ) throws java.lang.Exception;

}