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
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.common.auditAndSecurity.AuditEventDao;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * Service base class for <code>ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService</code>,
 * provides access to all services and entities referenced by this service.
 * 
 * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService
 * @version $Id$
 */
public abstract class Probe2ProbeCoexpressionServiceBase implements
        ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService {

    @Autowired
    private Probe2ProbeCoexpressionDao probe2ProbeCoexpressionDao;

    @Autowired
    private AuditEventDao auditEventDao;

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService#countLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public java.lang.Integer countLinks( final Long expressionExperiment ) {
        return this.handleCountLinks( expressionExperiment );
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService#create(java.util.List)
     */
    @Override
    public java.util.Collection<? extends Probe2ProbeCoexpression> create(
            final Collection<? extends Probe2ProbeCoexpression> p2pExpressions ) {
        return this.handleCreate( p2pExpressions );

    }

    @Override
    public void delete( final java.util.Collection<? extends Probe2ProbeCoexpression> p2pExpressions ) {
        this.handleRemove( p2pExpressions );

    }

    @Override
    public void delete( Probe2ProbeCoexpression p2p ) {
        this.handleRemove( p2p );

    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService#deleteLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public void deleteLinks( final ubic.gemma.model.expression.experiment.ExpressionExperiment ee ) {
        this.handleDeleteLinks( ee );

    }

    /**
     * @return the auditEventDao
     */
    public AuditEventDao getAuditEventDao() {
        return auditEventDao;
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService#getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene,
     *      java.util.Collection, boolean)
     */
    @Override
    public java.util.Collection<BioAssaySet> getExpressionExperimentsLinkTestedIn( final Gene gene,
            final Collection<? extends BioAssaySet> expressionExperiments, final boolean filterNonSpecific ) {
        return this.handleGetExpressionExperimentsLinkTestedIn( gene, expressionExperiments, filterNonSpecific );

    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService#getExpressionExperimentsTestedIn(java.util.Collection,
     *      java.util.Collection, boolean)
     */
    @Override
    public Map<Long, Collection<BioAssaySet>> getExpressionExperimentsTestedIn( final Collection<Long> geneIds,
            final Collection<? extends BioAssaySet> experiments, final boolean filterNonSpecific ) {
        return this.handleGetExpressionExperimentsTestedIn( geneIds, experiments, filterNonSpecific );

    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService#getGenesTestedBy(ubic.gemma.model.expression.experiment.BioAssaySet,
     *      boolean)
     */
    @Override
    public Collection<Long> getGenesTestedBy(
            final ubic.gemma.model.expression.experiment.BioAssaySet expressionExperiment,
            final boolean filterNonSpecific ) {
        return this.handleGetGenesTestedBy( expressionExperiment, filterNonSpecific );

    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService#getProbeCoExpression(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      java.lang.String, boolean)
     */
    @Override
    public Collection<ProbeLink> getProbeCoExpression( final ExpressionExperiment expressionExperiment,
            final java.lang.String taxon, final boolean useWorkingTable ) {
        return this.handleGetProbeCoExpression( expressionExperiment, taxon, useWorkingTable );

    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService#prepareForShuffling(java.util.Collection,
     *      java.lang.String, boolean)
     */
    @Override
    public void prepareForShuffling( final java.util.Collection<BioAssaySet> ees, final java.lang.String taxon,
            final boolean filterNonSpecific ) {
        this.handlePrepareForShuffling( ees, taxon, filterNonSpecific );

    }

    /**
     * Sets the reference to <code>probe2ProbeCoexpression</code>'s DAO.
     */
    public void setProbe2ProbeCoexpressionDao( Probe2ProbeCoexpressionDao probe2ProbeCoexpressionDao ) {
        this.probe2ProbeCoexpressionDao = probe2ProbeCoexpressionDao;
    }

    /**
     * Gets the reference to <code>probe2ProbeCoexpression</code>'s DAO.
     */
    protected Probe2ProbeCoexpressionDao getProbe2ProbeCoexpressionDao() {
        return this.probe2ProbeCoexpressionDao;
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService#getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene,
     *      java.util.Collection, java.util.Collection, boolean)
     */
    @Override
    public Map<Long, Collection<BioAssaySet>> getExpressionExperimentsLinkTestedIn( final Gene geneA,
            final java.util.Collection<Long> genesB, final Collection<? extends BioAssaySet> expressionExperiments,
            final boolean filterNonSpecific ) {
        return this
                .handleGetExpressionExperimentsLinkTestedIn( geneA, genesB, expressionExperiments, filterNonSpecific );
    }

    /**
     * Performs the core logic for {@link #countLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     */
    protected abstract Integer handleCountLinks( Long expressionExperiment );

    /**
     * Performs the core logic for {@link #create(java.util.List)}
     */
    protected abstract Collection<? extends Probe2ProbeCoexpression> handleCreate(
            Collection<? extends Probe2ProbeCoexpression> p2pExpressions );

    /**
     * Performs the core logic for {@link #deleteLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     */
    protected abstract void handleDeleteLinks( ExpressionExperiment ee );

    /**
     * Performs the core logic for
     * {@link #getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene, java.util.Collection, boolean)}
     */
    protected abstract Collection<BioAssaySet> handleGetExpressionExperimentsLinkTestedIn( Gene gene,
            Collection<? extends BioAssaySet> expressionExperiments, boolean filterNonSpecific );

    /**
     * Performs the core logic for
     * {@link #getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene, java.util.Collection, java.util.Collection, boolean)}
     */
    protected abstract java.util.Map<Long, Collection<BioAssaySet>> handleGetExpressionExperimentsLinkTestedIn(
            Gene geneA, Collection<Long> genesB, Collection<? extends BioAssaySet> expressionExperiments,
            boolean filterNonSpecific );

    /**
     * Performs the core logic for
     * {@link #getExpressionExperimentsTestedIn(java.util.Collection, java.util.Collection, boolean)}
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
     * {@link #getProbeCoExpression(ubic.gemma.model.expression.experiment.ExpressionExperiment, java.lang.String, boolean)}
     */
    protected abstract Collection<ProbeLink> handleGetProbeCoExpression( ExpressionExperiment expressionExperiment,
            java.lang.String taxon, boolean useWorkingTable );

    /**
     * Performs the core logic for {@link #prepareForShuffling(java.util.Collection, java.lang.String, boolean)}
     */
    protected abstract void handlePrepareForShuffling( Collection<BioAssaySet> ees, java.lang.String taxon,
            boolean filterNonSpecific );

    protected abstract void handleRemove( Collection<? extends Probe2ProbeCoexpression> expressions );

    protected abstract void handleRemove( Probe2ProbeCoexpression p2p );

}