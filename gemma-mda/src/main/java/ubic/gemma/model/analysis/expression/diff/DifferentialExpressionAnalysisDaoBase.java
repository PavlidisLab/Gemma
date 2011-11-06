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

import ubic.gemma.model.analysis.AnalysisDaoImpl;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis</code>.
 * </p>
 * 
 * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis
 */
public abstract class DifferentialExpressionAnalysisDaoBase extends AnalysisDaoImpl<DifferentialExpressionAnalysis>
        implements DifferentialExpressionAnalysisDao {

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#create(int,
     *      java.util.Collection)
     */
    public java.util.Collection<DifferentialExpressionAnalysis> create( final int transform,
            final java.util.Collection<DifferentialExpressionAnalysis> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "DifferentialExpressionAnalysis.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<DifferentialExpressionAnalysis> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( transform, entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#create(int transform,
     *      ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis)
     */
    public DifferentialExpressionAnalysis create(
            final int transform,
            final ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        if ( differentialExpressionAnalysis == null ) {
            throw new IllegalArgumentException(
                    "DifferentialExpressionAnalysis.create - 'differentialExpressionAnalysis' can not be null" );
        }
        this.getHibernateTemplate().save( differentialExpressionAnalysis );
        return differentialExpressionAnalysis;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#create(java.util.Collection)
     */

    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#create(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis)
     */
    public DifferentialExpressionAnalysis create(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        return this.create( TRANSFORM_NONE, differentialExpressionAnalysis );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#find(ubic.gemma.model.genome.Gene,
     *      ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet, double)
     */
    public java.util.Collection<DifferentialExpressionAnalysis> find( final ubic.gemma.model.genome.Gene gene,
            final ExpressionAnalysisResultSet resultSet, final double threshold ) {
        try {
            return this.handleFind( gene, resultSet, threshold );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao.find(ubic.gemma.model.genome.Gene gene, ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet resultSet, double threshold)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#findByInvestigationIds(java.util.Collection)
     */
    public java.util.Map findByInvestigationIds( final java.util.Collection<Long> investigationIds ) {
        try {
            return this.handleFindByInvestigationIds( investigationIds );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao.findByInvestigationIds(java.util.Collection investigationIds)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#findByName(int,
     *      java.lang.String)
     */
    @Override
    public java.util.Collection<DifferentialExpressionAnalysis> findByName( final int transform,
            final java.lang.String name ) {
        return this.findByName( transform, "select a from AnalysisImpl as a where a.name like :name", name );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#findByName(int,
     *      java.lang.String, java.lang.String)
     */

    @Override
    public java.util.Collection<DifferentialExpressionAnalysis> findByName( final int transform,
            final java.lang.String queryString, final java.lang.String name ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( name );
        argNames.add( "name" );
        java.util.List<DifferentialExpressionAnalysis> results = this.getHibernateTemplate().findByNamedParam(
                queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return results;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#findByName(java.lang.String)
     */

    @Override
    public java.util.Collection<DifferentialExpressionAnalysis> findByName( java.lang.String name ) {
        return this.findByName( TRANSFORM_NONE, name );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#findByName(java.lang.String,
     *      java.lang.String)
     */
    @Override
    public java.util.Collection<DifferentialExpressionAnalysis> findByName( final java.lang.String queryString,
            final java.lang.String name ) {
        return this.findByName( TRANSFORM_NONE, queryString, name );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#findExperimentsWithAnalyses(ubic.gemma.model.genome.Gene)
     */
    public java.util.Collection<BioAssaySet> findExperimentsWithAnalyses( final ubic.gemma.model.genome.Gene gene ) {
        try {
            return this.handleFindExperimentsWithAnalyses( gene );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao.findExperimentsWithAnalyses(ubic.gemma.model.genome.Gene gene)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#getResultSets(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public java.util.Collection<ExpressionAnalysisResultSet> getResultSets(
            final ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment ) {
        try {
            return this.handleGetResultSets( expressionExperiment );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao.getResultSets(ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    public Collection<? extends DifferentialExpressionAnalysis> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam(
                "from DifferentialExpressionAnalysisImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#load(int, java.lang.Long)
     */

    public DifferentialExpressionAnalysis load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "DifferentialExpressionAnalysis.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl.class, id );
        return ( ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis ) entity;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#load(java.lang.Long)
     */

    public DifferentialExpressionAnalysis load( java.lang.Long id ) {
        return this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#loadAll()
     */

    public java.util.Collection<? extends DifferentialExpressionAnalysis> loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#loadAll(int)
     */

    public java.util.Collection<? extends DifferentialExpressionAnalysis> loadAll( final int transform ) {
        final java.util.Collection<? extends DifferentialExpressionAnalysis> results = this.getHibernateTemplate()
                .loadAll( ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl.class );
        return results;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "DifferentialExpressionAnalysis.remove - 'id' can not be null" );
        }
        ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection<? extends DifferentialExpressionAnalysis> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "DifferentialExpressionAnalysis.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#remove(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis)
     */
    public void remove(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        if ( differentialExpressionAnalysis == null ) {
            throw new IllegalArgumentException(
                    "DifferentialExpressionAnalysis.remove - 'differentialExpressionAnalysis' can not be null" );
        }
        this.getHibernateTemplate().delete( differentialExpressionAnalysis );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#thaw(java.util.Collection)
     */
    public void thaw( final java.util.Collection<DifferentialExpressionAnalysis> expressionAnalyses ) {
        try {
            this.handleThaw( expressionAnalyses );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao.thaw(java.util.Collection expressionAnalyses)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#thaw(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis)
     */
    public void thaw(
            final ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        try {
            this.handleThaw( differentialExpressionAnalysis );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao.thaw(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis differentialExpressionAnalysis)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection<? extends DifferentialExpressionAnalysis> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "DifferentialExpressionAnalysis.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends DifferentialExpressionAnalysis> entityIterator = entities
                                .iterator(); entityIterator.hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#update(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis)
     */
    public void update(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        if ( differentialExpressionAnalysis == null ) {
            throw new IllegalArgumentException(
                    "DifferentialExpressionAnalysis.update - 'differentialExpressionAnalysis' can not be null" );
        }
        this.getHibernateTemplate().update( differentialExpressionAnalysis );
    }

    /**
     * Performs the core logic for
     * {@link #find(ubic.gemma.model.genome.Gene, ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet, double)}
     */
    protected abstract java.util.Collection<DifferentialExpressionAnalysis> handleFind(
            ubic.gemma.model.genome.Gene gene, ExpressionAnalysisResultSet resultSet, double threshold )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByInvestigationIds(java.util.Collection)}
     */
    protected abstract java.util.Map handleFindByInvestigationIds( java.util.Collection<Long> investigationIds )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findExperimentsWithAnalyses(ubic.gemma.model.genome.Gene)}
     */
    protected abstract java.util.Collection<BioAssaySet> handleFindExperimentsWithAnalyses(
            ubic.gemma.model.genome.Gene gene ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getResultSets(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     */
    protected abstract java.util.Collection<ExpressionAnalysisResultSet> handleGetResultSets(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(java.util.Collection)}
     */
    protected abstract void handleThaw( java.util.Collection<DifferentialExpressionAnalysis> expressionAnalyses )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #thaw(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis)}
     */
    protected abstract void handleThaw(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis differentialExpressionAnalysis )
            throws java.lang.Exception;

}