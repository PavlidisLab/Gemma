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
package ubic.gemma.model.association.coexpression;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression</code>.
 * </p>
 * 
 * @version $Id$
 * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression
 */
public abstract class Probe2ProbeCoexpressionDaoBase extends HibernateDaoSupport implements Probe2ProbeCoexpressionDao {

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#countLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public Integer countLinks( final ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment ) {
        try {
            return this.handleCountLinks( expressionExperiment );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao.countLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#create(List)
     */
    public Collection<? extends Probe2ProbeCoexpression> create(
            final Collection<? extends Probe2ProbeCoexpression> links ) {
        try {
            return this.handleCreate( links );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao.create(List links)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#deleteLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public void deleteLinks( final ubic.gemma.model.expression.experiment.ExpressionExperiment ee ) {
        try {
            this.handleDeleteLinks( ee );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao.deleteLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment ee)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene,
     *      Collection, boolean)
     */
    public Collection<BioAssaySet> getExpressionExperimentsLinkTestedIn( final ubic.gemma.model.genome.Gene gene,
            final Collection<BioAssaySet> expressionExperiments, final boolean filterNonSpecific ) {
        try {
            return this.handleGetExpressionExperimentsLinkTestedIn( gene, expressionExperiments, filterNonSpecific );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao.getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene gene, Collection expressionExperiments, boolean filterNonSpecific)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene,
     *      Collection, Collection, boolean)
     */
    public Map getExpressionExperimentsLinkTestedIn( final ubic.gemma.model.genome.Gene geneA,
            final Collection<Long> genesB, final Collection<BioAssaySet> expressionExperiments,
            final boolean filterNonSpecific ) {
        try {
            return this.handleGetExpressionExperimentsLinkTestedIn( geneA, genesB, expressionExperiments,
                    filterNonSpecific );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao.getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene geneA, Collection genesB, Collection expressionExperiments, boolean filterNonSpecific)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#getExpressionExperimentsTestedIn(Collection,
     *      Collection, boolean)
     */
    public Map getExpressionExperimentsTestedIn( final Collection geneIds, final Collection experiments,
            final boolean filterNonSpecific ) {
        try {
            return this.handleGetExpressionExperimentsTestedIn( geneIds, experiments, filterNonSpecific );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao.getExpressionExperimentsTestedIn(Collection geneIds, Collection experiments, boolean filterNonSpecific)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#getGenesTestedBy(ubic.gemma.model.expression.experiment.BioAssaySet,
     *      boolean)
     */
    public Collection<Long> getGenesTestedBy(
            final ubic.gemma.model.expression.experiment.BioAssaySet expressionExperiment,
            final boolean filterNonSpecific ) {
        try {
            return this.handleGetGenesTestedBy( expressionExperiment, filterNonSpecific );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao.getGenesTestedBy(ubic.gemma.model.expression.experiment.BioAssaySet expressionExperiment, boolean filterNonSpecific)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#getProbeCoExpression(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      String, boolean)
     */
    public Collection getProbeCoExpression(
            final ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment,
            final String taxonCommonName, final boolean useWorkingTable ) {
        try {
            return this.handleGetProbeCoExpression( expressionExperiment, taxonCommonName, useWorkingTable );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao.getProbeCoExpression(ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment, String taxonCommonName, boolean useWorkingTable)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#load(int, Long)
     */
    public Probe2ProbeCoexpression load( final Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Probe2ProbeCoexpression.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( Probe2ProbeCoexpressionImpl.class, id );
        return ( ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression ) entity;
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#prepareForShuffling(Collection, String,
     *      boolean)
     */
    public void prepareForShuffling( final Collection<ExpressionExperiment> ees, final String taxon,
            final boolean filterNonSpecific ) {
        try {
            this.handlePrepareForShuffling( ees, taxon, filterNonSpecific );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao.prepareForShuffling(Collection ees, String taxon, boolean filterNonSpecific)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#remove(Long)
     */
    public void remove( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Probe2ProbeCoexpression.remove - 'id' can not be null" );
        }
        Probe2ProbeCoexpression entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.association.RelationshipDao#remove(Collection)
     */
    public void remove( Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Probe2ProbeCoexpression.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#remove(ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression)
     */
    public void remove( ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression probe2ProbeCoexpression ) {
        if ( probe2ProbeCoexpression == null ) {
            throw new IllegalArgumentException(
                    "Probe2ProbeCoexpression.remove - 'probe2ProbeCoexpression' can not be null" );
        }
        this.getHibernateTemplate().delete( probe2ProbeCoexpression );
    }

    /**
     * @see ubic.gemma.model.association.RelationshipDao#update(Collection)
     */
    public void update( final Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Probe2ProbeCoexpression.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression ) entityIterator
                                    .next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#update(ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression)
     */
    public void update( ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression probe2ProbeCoexpression ) {
        if ( probe2ProbeCoexpression == null ) {
            throw new IllegalArgumentException(
                    "Probe2ProbeCoexpression.update - 'probe2ProbeCoexpression' can not be null" );
        }
        this.getHibernateTemplate().update( probe2ProbeCoexpression );
    }

    /**
     * Performs the core logic for {@link #countLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     */
    protected abstract Integer handleCountLinks(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment ) throws Exception;

    /**
     * Performs the core logic for {@link #create(List)}
     */
    protected abstract Collection<? extends Probe2ProbeCoexpression> handleCreate(
            Collection<? extends Probe2ProbeCoexpression> links ) throws Exception;

    /**
     * Performs the core logic for {@link #deleteLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     */
    protected abstract void handleDeleteLinks( ubic.gemma.model.expression.experiment.ExpressionExperiment ee )
            throws Exception;

    /**
     * Performs the core logic for
     * {@link #getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene, Collection, boolean)}
     */
    protected abstract Collection<BioAssaySet> handleGetExpressionExperimentsLinkTestedIn(
            ubic.gemma.model.genome.Gene gene, Collection<BioAssaySet> expressionExperiments, boolean filterNonSpecific )
            throws Exception;

    /**
     * Performs the core logic for
     * {@link #getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene, Collection, Collection, boolean)}
     */
    protected abstract Map<Long, Collection<BioAssaySet>> handleGetExpressionExperimentsLinkTestedIn(
            ubic.gemma.model.genome.Gene geneA, Collection<Long> genesB, Collection<BioAssaySet> expressionExperiments,
            boolean filterNonSpecific ) throws Exception;

    /**
     * Performs the core logic for {@link #getExpressionExperimentsTestedIn(Collection, Collection, boolean)}
     */
    protected abstract Map<Long, Collection<BioAssaySet>> handleGetExpressionExperimentsTestedIn(
            Collection<Long> geneIds, Collection<BioAssaySet> experiments, boolean filterNonSpecific ) throws Exception;

    /**
     * Performs the core logic for
     * {@link #getGenesTestedBy(ubic.gemma.model.expression.experiment.BioAssaySet, boolean)}
     */
    protected abstract Collection<Long> handleGetGenesTestedBy(
            ubic.gemma.model.expression.experiment.BioAssaySet expressionExperiment, boolean filterNonSpecific )
            throws Exception;

    /**
     * Performs the core logic for
     * {@link #getProbeCoExpression(ubic.gemma.model.expression.experiment.ExpressionExperiment, String, boolean)}
     */
    protected abstract Collection handleGetProbeCoExpression(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment, String taxonCommonName,
            boolean useWorkingTable ) throws Exception;

    /**
     * Performs the core logic for {@link #prepareForShuffling(Collection, String, boolean)}
     */
    protected abstract void handlePrepareForShuffling( Collection<ExpressionExperiment> ees, String taxon,
            boolean filterNonSpecific ) throws Exception;

}