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
 * Base DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis</code>.
 * 
 * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis
 */
public abstract class DifferentialExpressionAnalysisDaoBase extends AnalysisDaoImpl<DifferentialExpressionAnalysis>
        implements DifferentialExpressionAnalysisDao {

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#create(int,
     *      java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends DifferentialExpressionAnalysis> create(
            final java.util.Collection<? extends DifferentialExpressionAnalysis> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "DifferentialExpressionAnalysis.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends DifferentialExpressionAnalysis> entityIterator = entities
                                .iterator(); entityIterator.hasNext(); ) {
                            create( entityIterator.next() );
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
    @Override
    public DifferentialExpressionAnalysis create(

    final ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        if ( differentialExpressionAnalysis == null ) {
            throw new IllegalArgumentException(
                    "DifferentialExpressionAnalysis.create - 'differentialExpressionAnalysis' can not be null" );
        }
        this.getHibernateTemplate().save( differentialExpressionAnalysis );
        return differentialExpressionAnalysis;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#find(ubic.gemma.model.genome.Gene,
     *      ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet, double)
     */
    @Override
    public java.util.Collection<DifferentialExpressionAnalysis> find( final ubic.gemma.model.genome.Gene gene,
            final ExpressionAnalysisResultSet resultSet, final double threshold ) {
        return this.handleFind( gene, resultSet, threshold );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#findByInvestigationIds(java.util.Collection)
     */
    @Override
    public java.util.Map<Long, Collection<DifferentialExpressionAnalysis>> findByInvestigationIds(
            final java.util.Collection<Long> investigationIds ) {
        return this.handleFindByInvestigationIds( investigationIds );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#findExperimentsWithAnalyses(ubic.gemma.model.genome.Gene)
     */
    @Override
    public java.util.Collection<BioAssaySet> findExperimentsWithAnalyses( final ubic.gemma.model.genome.Gene gene ) {
        return this.handleFindExperimentsWithAnalyses( gene );
    }

    @Override
    public Collection<? extends DifferentialExpressionAnalysis> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam(
                "from DifferentialExpressionAnalysisImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#load(int, java.lang.Long)
     */

    @Override
    public DifferentialExpressionAnalysis load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "DifferentialExpressionAnalysis.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl.class, id );
        return ( ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis ) entity;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#loadAll(int)
     */

    @Override
    public java.util.Collection<? extends DifferentialExpressionAnalysis> loadAll() {
        final java.util.Collection<? extends DifferentialExpressionAnalysis> results = this.getHibernateTemplate()
                .loadAll( ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl.class );
        return results;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#remove(java.lang.Long)
     */

    @Override
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

    @Override
    public void remove( java.util.Collection<? extends DifferentialExpressionAnalysis> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "DifferentialExpressionAnalysis.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#remove(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis)
     */
    @Override
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
    @Override
    public void thaw( final java.util.Collection<DifferentialExpressionAnalysis> expressionAnalyses ) {
        this.handleThaw( expressionAnalyses );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#thaw(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis)
     */
    @Override
    public void thaw(
            final ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        this.handleThaw( differentialExpressionAnalysis );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    @Override
    public void update( final java.util.Collection<? extends DifferentialExpressionAnalysis> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "DifferentialExpressionAnalysis.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
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
    @Override
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
            ubic.gemma.model.genome.Gene gene, ExpressionAnalysisResultSet resultSet, double threshold );

    /**
     * Performs the core logic for {@link #findByInvestigationIds(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, Collection<DifferentialExpressionAnalysis>> handleFindByInvestigationIds(
            java.util.Collection<Long> investigationIds );

    /**
     * Performs the core logic for {@link #findExperimentsWithAnalyses(ubic.gemma.model.genome.Gene)}
     */
    protected abstract java.util.Collection<BioAssaySet> handleFindExperimentsWithAnalyses(
            ubic.gemma.model.genome.Gene gene );

    /**
     * Performs the core logic for {@link #thaw(java.util.Collection)}
     */
    protected abstract void handleThaw( java.util.Collection<DifferentialExpressionAnalysis> expressionAnalyses );

    /**
     * Performs the core logic for
     * {@link #thaw(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis)}
     */
    protected abstract void handleThaw(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis differentialExpressionAnalysis );

}