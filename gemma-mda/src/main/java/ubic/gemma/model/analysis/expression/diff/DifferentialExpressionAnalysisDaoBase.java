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

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;

import ubic.gemma.model.analysis.AnalysisDaoBase;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;

/**
 * Base DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>DifferentialExpressionAnalysis</code>.
 * 
 * @see DifferentialExpressionAnalysis
 */
public abstract class DifferentialExpressionAnalysisDaoBase extends AnalysisDaoBase<DifferentialExpressionAnalysis>
        implements DifferentialExpressionAnalysisDao {

    /**
     * @see DifferentialExpressionAnalysisDao#create(int, Collection)
     */
    @Override
    public Collection<? extends DifferentialExpressionAnalysis> create(
            final Collection<? extends DifferentialExpressionAnalysis> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "DifferentialExpressionAnalysis.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession( new HibernateCallback<Object>() {
            @Override
            public Object doInHibernate( Session session ) throws HibernateException {
                for ( Iterator<? extends DifferentialExpressionAnalysis> entityIterator = entities.iterator(); entityIterator
                        .hasNext(); ) {
                    create( entityIterator.next() );
                }
                return null;
            }
        } );
        return entities;
    }

    /**
     * @see DifferentialExpressionAnalysisDao#create(int transform, DifferentialExpressionAnalysis)
     */
    @Override
    public DifferentialExpressionAnalysis create(

    final DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        if ( differentialExpressionAnalysis == null ) {
            throw new IllegalArgumentException(
                    "DifferentialExpressionAnalysis.create - 'differentialExpressionAnalysis' can not be null" );
        }
        this.getHibernateTemplate().save( differentialExpressionAnalysis );
        return differentialExpressionAnalysis;
    }

    /**
     * @see DifferentialExpressionAnalysisDao#find(Gene,
     *      ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet, double)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> find( final Gene gene,
            final ExpressionAnalysisResultSet resultSet, final double threshold ) {
        return this.handleFind( gene, resultSet, threshold );
    }

    /**
     * @see DifferentialExpressionAnalysisDao#findByInvestigationIds(Collection)
     */
    @Override
    public Map<Long, Collection<DifferentialExpressionAnalysis>> findByInvestigationIds(
            final Collection<Long> investigationIds ) {
        return this.handleFindByInvestigationIds( investigationIds );
    }

    /**
     * @see DifferentialExpressionAnalysisDao#findExperimentsWithAnalyses(Gene)
     */
    @Override
    public Collection<BioAssaySet> findExperimentsWithAnalyses( final Gene gene ) {
        return this.handleFindExperimentsWithAnalyses( gene );
    }

    @Override
    public Collection<? extends DifferentialExpressionAnalysis> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam(
                "from DifferentialExpressionAnalysisImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see DifferentialExpressionAnalysisDao#load(int, java.lang.Long)
     */

    @Override
    public DifferentialExpressionAnalysis load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "DifferentialExpressionAnalysis.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( DifferentialExpressionAnalysisImpl.class, id );
        return ( DifferentialExpressionAnalysis ) entity;
    }

    /**
     * @see DifferentialExpressionAnalysisDao#loadAll(int)
     */

    @Override
    public Collection<? extends DifferentialExpressionAnalysis> loadAll() {
        final Collection<? extends DifferentialExpressionAnalysis> results = this.getHibernateTemplate().loadAll(
                DifferentialExpressionAnalysisImpl.class );
        return results;
    }

    /**
     * @see DifferentialExpressionAnalysisDao#remove(java.lang.Long)
     */

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "DifferentialExpressionAnalysis.remove - 'id' can not be null" );
        }
        DifferentialExpressionAnalysis entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(Collection)
     */

    @Override
    public void remove( Collection<? extends DifferentialExpressionAnalysis> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "DifferentialExpressionAnalysis.remove - 'entities' can not be null" );
        }
        for ( DifferentialExpressionAnalysis a : entities ) {
            this.remove( a );
        }
    }

    /**
     * @see DifferentialExpressionAnalysisDao#thaw(Collection)
     */
    @Override
    public void thaw( final Collection<DifferentialExpressionAnalysis> expressionAnalyses ) {
        this.handleThaw( expressionAnalyses );
    }

    /**
     * @see DifferentialExpressionAnalysisDao#thaw(DifferentialExpressionAnalysis)
     */
    @Override
    public void thaw( final DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        this.handleThaw( differentialExpressionAnalysis );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(Collection)
     */

    @Override
    public void update( final Collection<? extends DifferentialExpressionAnalysis> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "DifferentialExpressionAnalysis.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().update( entities );
    }

    /**
     * @see DifferentialExpressionAnalysisDao#update(DifferentialExpressionAnalysis)
     */
    @Override
    public void update( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        if ( differentialExpressionAnalysis == null ) {
            throw new IllegalArgumentException(
                    "DifferentialExpressionAnalysis.update - 'differentialExpressionAnalysis' can not be null" );
        }
        this.getHibernateTemplate().update( differentialExpressionAnalysis );
    }

    /**
     * Performs the core logic for
     * {@link #find(Gene, ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet, double)}
     */
    protected abstract Collection<DifferentialExpressionAnalysis> handleFind( Gene gene,
            ExpressionAnalysisResultSet resultSet, double threshold );

    /**
     * Performs the core logic for {@link #findByInvestigationIds(Collection)}
     */
    protected abstract Map<Long, Collection<DifferentialExpressionAnalysis>> handleFindByInvestigationIds(
            Collection<Long> investigationIds );

    /**
     * Performs the core logic for {@link #findExperimentsWithAnalyses(Gene)}
     */
    protected abstract Collection<BioAssaySet> handleFindExperimentsWithAnalyses( Gene gene );

    /**
     * Performs the core logic for {@link #thaw(Collection)}
     */
    protected abstract void handleThaw( Collection<DifferentialExpressionAnalysis> expressionAnalyses );

    /**
     * Performs the core logic for {@link #thaw(DifferentialExpressionAnalysis)}
     */
    protected abstract void handleThaw( DifferentialExpressionAnalysis differentialExpressionAnalysis );

}