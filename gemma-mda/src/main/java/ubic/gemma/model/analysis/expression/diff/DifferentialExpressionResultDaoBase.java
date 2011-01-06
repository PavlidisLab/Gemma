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
import java.util.List;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.genome.Gene;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.analysis.expression.diff.ProbeAnalysisResult</code>.
 * </p>
 * 
 * @see ubic.gemma.model.analysis.expression.diff.ProbeAnalysisResult
 */
public abstract class DifferentialExpressionResultDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao {

        
    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#create(int, java.util.Collection)
     */
    public java.util.Collection<? extends ProbeAnalysisResult> create(
            final java.util.Collection<? extends ProbeAnalysisResult> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ProbeAnalysisResult.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends ProbeAnalysisResult> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.ProbeAnalysisResultDao#create(int transform,
     *      ubic.gemma.model.analysis.expression.diff.ProbeAnalysisResult)
     */
    public ProbeAnalysisResult create(
            final ubic.gemma.model.analysis.expression.ProbeAnalysisResult probeAnalysisResult ) {
        if ( probeAnalysisResult == null ) {
            throw new IllegalArgumentException( "ProbeAnalysisResult.create - 'ProbeAnalysisResult' can not be null" );
        }
        this.getHibernateTemplate().save( probeAnalysisResult );
        return probeAnalysisResult;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#getExperimentalFactors(java.util.Collection)
     */
    public java.util.Map<ProbeAnalysisResult, Collection<ExperimentalFactor>> getExperimentalFactors(
            final java.util.Collection<ProbeAnalysisResult> ProbeAnalysisResults ) {
        try {
            return this.handleGetExperimentalFactors( ProbeAnalysisResults );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.analysis.expression.diff.ProbeAnalysisResultDao.getExperimentalFactors(java.util.Collection ProbeAnalysisResults)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#getExperimentalFactors(ubic.gemma.model.analysis.expression.diff.ProbeAnalysisResult)
     */
    public java.util.Collection<ExperimentalFactor> getExperimentalFactors(
            final ubic.gemma.model.analysis.expression.ProbeAnalysisResult ProbeAnalysisResult ) {
        try {
            return this.handleGetExperimentalFactors( ProbeAnalysisResult );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.analysis.expression.diff.ProbeAnalysisResultDao.getExperimentalFactors(ubic.gemma.model.analysis.expression.diff.ProbeAnalysisResult ProbeAnalysisResult)' --> "
                            + th, th );
        }
    }

    public Collection<? extends ProbeAnalysisResult> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from ProbeAnalysisResultImpl where id in (:ids)", "ids",
                ids );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#load(int, java.lang.Long)
     */
    public ProbeAnalysisResult load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ProbeAnalysisResult.load - 'id' can not be null" );
        }
        return this.getHibernateTemplate().get( ubic.gemma.model.analysis.expression.ProbeAnalysisResultImpl.class, id );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#remove(java.lang.Long)
     */
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ProbeAnalysisResult.remove - 'id' can not be null" );
        }
        ubic.gemma.model.analysis.expression.ProbeAnalysisResult entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisResultDao#remove(java.util.Collection)
     */
    public void remove( java.util.Collection<? extends ProbeAnalysisResult> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ProbeAnalysisResult.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#remove(ubic.gemma.model.analysis.expression.diff.ProbeAnalysisResult)
     */
    public void remove( ubic.gemma.model.analysis.expression.ProbeAnalysisResult ProbeAnalysisResult ) {
        if ( ProbeAnalysisResult == null ) {
            throw new IllegalArgumentException( "ProbeAnalysisResult.remove - 'ProbeAnalysisResult' can not be null" );
        }
        this.getHibernateTemplate().delete( ProbeAnalysisResult );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisResultDao#update(java.util.Collection)
     */
    public void update( final java.util.Collection<? extends ProbeAnalysisResult> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ProbeAnalysisResult.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends ProbeAnalysisResult> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#update(ubic.gemma.model.analysis.expression.diff.ProbeAnalysisResult)
     */
    public void update( ubic.gemma.model.analysis.expression.ProbeAnalysisResult ProbeAnalysisResult ) {
        if ( ProbeAnalysisResult == null ) {
            throw new IllegalArgumentException( "ProbeAnalysisResult.update - 'ProbeAnalysisResult' can not be null" );
        }
        this.getHibernateTemplate().update( ProbeAnalysisResult );
    }

    /**
     * Performs the core logic for {@link #getExperimentalFactors(java.util.Collection)}
     */
    protected abstract java.util.Map<ProbeAnalysisResult, Collection<ExperimentalFactor>> handleGetExperimentalFactors(
            java.util.Collection<ProbeAnalysisResult> ProbeAnalysisResults ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getExperimentalFactors(ubic.gemma.model.analysis.expression.diff.ProbeAnalysisResult)}
     */
    protected abstract java.util.Collection<ExperimentalFactor> handleGetExperimentalFactors(
            ubic.gemma.model.analysis.expression.ProbeAnalysisResult ProbeAnalysisResult ) throws java.lang.Exception;

}