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
    @Override
    public Integer countLinks( final Long expressionExperiment ) {
        return this.handleCountLinks( expressionExperiment );
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#createFromValueObject(List)
     */
    @Override
    public Collection<? extends Probe2ProbeCoexpression> create(
            final Collection<? extends Probe2ProbeCoexpression> links ) {
        return this.handleCreate( links );
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#deleteLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public void deleteLinks( final BioAssaySet bioAssaySet ) {
        this.handleDeleteLinks( bioAssaySet );
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene,
     *      Collection, boolean)
     */
    @Override
    public Collection<BioAssaySet> getExpressionExperimentsLinkTestedIn( final ubic.gemma.model.genome.Gene gene,
            final Collection<? extends BioAssaySet> expressionExperiments, final boolean filterNonSpecific ) {
        return this.handleGetExpressionExperimentsLinkTestedIn( gene, expressionExperiments, filterNonSpecific );
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene,
     *      Collection, Collection, boolean)
     */
    @Override
    public Map<Long, Collection<BioAssaySet>> getExpressionExperimentsLinkTestedIn(
            final ubic.gemma.model.genome.Gene geneA, final Collection<Long> genesB,
            final Collection<? extends BioAssaySet> expressionExperiments, final boolean filterNonSpecific ) {
        return this
                .handleGetExpressionExperimentsLinkTestedIn( geneA, genesB, expressionExperiments, filterNonSpecific );
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#getExpressionExperimentsTestedIn(Collection,
     *      Collection, boolean)
     */
    @Override
    public Map<Long, Collection<BioAssaySet>> getExpressionExperimentsTestedIn( final Collection<Long> geneIds,
            final Collection<? extends BioAssaySet> experiments, final boolean filterNonSpecific ) {
        return this.handleGetExpressionExperimentsTestedIn( geneIds, experiments, filterNonSpecific );

    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#getGenesTestedBy(ubic.gemma.model.expression.experiment.BioAssaySet,
     *      boolean)
     */
    @Override
    public Collection<Long> getGenesTestedBy(
            final ubic.gemma.model.expression.experiment.BioAssaySet expressionExperiment,
            final boolean filterNonSpecific ) {
        return this.handleGetGenesTestedBy( expressionExperiment, filterNonSpecific );

    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#getProbeCoExpression(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      String, boolean)
     */
    @Override
    public Collection<ProbeLink> getProbeCoExpression( final ExpressionExperiment expressionExperiment,
            final String taxonCommonName, final boolean useWorkingTable ) {
        return this.handleGetProbeCoExpression( expressionExperiment, taxonCommonName, useWorkingTable );

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
    @Override
    public void prepareForShuffling( final Collection<BioAssaySet> ees, final String taxon,
            final boolean filterNonSpecific ) {
        this.handlePrepareForShuffling( ees, taxon, filterNonSpecific );
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
    @Override
    public void remove( Collection<? extends Probe2ProbeCoexpression> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Probe2ProbeCoexpression.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#remove(ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression)
     */
    @Override
    public void remove( Probe2ProbeCoexpression probe2ProbeCoexpression ) {
        if ( probe2ProbeCoexpression == null ) {
            throw new IllegalArgumentException(
                    "Probe2ProbeCoexpression.remove - 'probe2ProbeCoexpression' can not be null" );
        }
        this.getHibernateTemplate().delete( probe2ProbeCoexpression );
    }

    /**
     * @see ubic.gemma.model.association.RelationshipDao#update(Collection)
     */
    public void update( final Collection<? extends Probe2ProbeCoexpression> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Probe2ProbeCoexpression.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( Iterator<? extends Probe2ProbeCoexpression> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#update(ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression)
     */
    public void update( Probe2ProbeCoexpression probe2ProbeCoexpression ) {
        if ( probe2ProbeCoexpression == null ) {
            throw new IllegalArgumentException(
                    "Probe2ProbeCoexpression.update - 'probe2ProbeCoexpression' can not be null" );
        }
        this.getHibernateTemplate().update( probe2ProbeCoexpression );
    }

    /**
     * Performs the core logic for {@link #countLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     */
    protected abstract Integer handleCountLinks( Long expressionExperiment );

    /**
     * Performs the core logic for {@link #createFromValueObject(List)}
     */
    protected abstract Collection<? extends Probe2ProbeCoexpression> handleCreate(
            Collection<? extends Probe2ProbeCoexpression> links );

    /**
     * Performs the core logic for {@link #deleteLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     */
    protected abstract void handleDeleteLinks( BioAssaySet bioAssaySet );

    /**
     * Performs the core logic for
     * {@link #getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene, Collection, boolean)}
     */
    protected abstract Collection<BioAssaySet> handleGetExpressionExperimentsLinkTestedIn( Gene gene,
            Collection<? extends BioAssaySet> expressionExperiments, boolean filterNonSpecific );

    /**
     * Performs the core logic for
     * {@link #getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene, Collection, Collection, boolean)}
     */
    protected abstract Map<Long, Collection<BioAssaySet>> handleGetExpressionExperimentsLinkTestedIn( Gene geneA,
            Collection<Long> genesB, Collection<? extends BioAssaySet> expressionExperiments, boolean filterNonSpecific );

    /**
     * Performs the core logic for {@link #getExpressionExperimentsTestedIn(Collection, Collection, boolean)}
     */
    protected abstract Map<Long, Collection<BioAssaySet>> handleGetExpressionExperimentsTestedIn(
            Collection<Long> geneIds, Collection<? extends BioAssaySet> experiments, boolean filterNonSpecific );

    /**
     * Performs the core logic for
     * {@link #getGenesTestedBy(ubic.gemma.model.expression.experiment.BioAssaySet, boolean)}
     */
    protected abstract Collection<Long> handleGetGenesTestedBy( BioAssaySet expressionExperiment,
            boolean filterNonSpecific );

    /**
     * Performs the core logic for
     * {@link #getProbeCoExpression(ubic.gemma.model.expression.experiment.ExpressionExperiment, String, boolean)}
     */
    protected abstract Collection<ProbeLink> handleGetProbeCoExpression( ExpressionExperiment expressionExperiment,
            String taxonCommonName, boolean useWorkingTable );

    /**
     * Performs the core logic for {@link #prepareForShuffling(Collection, String, boolean)}
     */
    protected abstract void handlePrepareForShuffling( Collection<BioAssaySet> ees, String taxon,
            boolean filterNonSpecific );

}