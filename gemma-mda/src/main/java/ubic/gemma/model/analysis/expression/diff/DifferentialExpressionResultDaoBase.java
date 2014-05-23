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
import java.util.Iterator;
import java.util.Map;

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
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#create(int, Collection)
     */
    @Override
    public Collection<? extends DifferentialExpressionAnalysisResult> create(
            final Collection<? extends DifferentialExpressionAnalysisResult> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException(
                    "DifferentialExpressionAnalysisResult.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( Iterator<? extends DifferentialExpressionAnalysisResult> entityIterator = entities
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
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#getExperimentalFactors(Collection)
     */
    @Override
    public Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> getExperimentalFactors(
            final Collection<DifferentialExpressionAnalysisResult> DifferentialExpressionAnalysisResults ) {

        return this.handleGetExperimentalFactors( DifferentialExpressionAnalysisResults );

    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#getExperimentalFactors(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult)
     */
    @Override
    public Collection<ExperimentalFactor> getExperimentalFactors(
            final DifferentialExpressionAnalysisResult DifferentialExpressionAnalysisResult ) {

        return this.handleGetExperimentalFactors( DifferentialExpressionAnalysisResult );

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
     * @see ubic.gemma.model.analysis.AnalysisResultDao#remove(Collection)
     */
    @Override
    public void remove( Collection<? extends DifferentialExpressionAnalysisResult> entities ) {
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
     * @see ubic.gemma.model.analysis.AnalysisResultDao#update(Collection)
     */
    @Override
    public void update( final Collection<? extends DifferentialExpressionAnalysisResult> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException(
                    "DifferentialExpressionAnalysisResult.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( Iterator<? extends DifferentialExpressionAnalysisResult> entityIterator = entities
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
     * Performs the core logic for {@link #getExperimentalFactors(Collection)}
     */
    protected abstract Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> handleGetExperimentalFactors(
            Collection<DifferentialExpressionAnalysisResult> DifferentialExpressionAnalysisResults );

    /**
     * Performs the core logic for {@link #getExperimentalFactors(diff.DifferentialExpressionAnalysisResult)}
     */
    protected abstract Collection<ExperimentalFactor> handleGetExperimentalFactors(
            DifferentialExpressionAnalysisResult DifferentialExpressionAnalysisResult );

}