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

import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.common.auditAndSecurity.AuditEventDao;
import ubic.gemma.model.expression.experiment.BioAssaySet;

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
    public java.lang.Integer countLinks(
            final ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment ) {
        try {
            return this.handleCountLinks( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionServiceException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService.countLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService#create(java.util.List)
     */
    public java.util.Collection<? extends Probe2ProbeCoexpression> create(
            final java.util.Collection<? extends Probe2ProbeCoexpression> p2pExpressions ) {
        try {
            return this.handleCreate( p2pExpressions );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionServiceException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService.create(java.util.List p2pExpressions)' --> "
                            + th, th );
        }
    }

    public void delete( final java.util.Collection<? extends Probe2ProbeCoexpression> p2pExpressions ) {
        try {
            this.handleRemove( p2pExpressions );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionServiceException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService.create(java.util.List p2pExpressions)' --> "
                            + th, th );
        }
    }

    public void delete( Probe2ProbeCoexpression p2p ) {
        try {
            this.handleRemove( p2p );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionServiceException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService.create(java.util.List p2pExpressions)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService#deleteLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public void deleteLinks( final ubic.gemma.model.expression.experiment.ExpressionExperiment ee ) {
        try {
            this.handleDeleteLinks( ee );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionServiceException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService.deleteLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment ee)' --> "
                            + th, th );
        }
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
    public java.util.Collection<BioAssaySet> getExpressionExperimentsLinkTestedIn(
            final ubic.gemma.model.genome.Gene gene,
            final java.util.Collection<? extends BioAssaySet> expressionExperiments, final boolean filterNonSpecific ) {
        try {
            return this.handleGetExpressionExperimentsLinkTestedIn( gene, expressionExperiments, filterNonSpecific );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionServiceException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService.getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene gene, java.util.Collection expressionExperiments, boolean filterNonSpecific)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService#getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene,
     *      java.util.Collection, java.util.Collection, boolean)
     */
    public java.util.Map<Long, Collection<BioAssaySet>> getExpressionExperimentsLinkTestedIn(
            final ubic.gemma.model.genome.Gene geneA, final java.util.Collection<Long> genesB,
            final java.util.Collection<? extends BioAssaySet> expressionExperiments, final boolean filterNonSpecific ) {
        try {
            return this.handleGetExpressionExperimentsLinkTestedIn( geneA, genesB, expressionExperiments,
                    filterNonSpecific );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionServiceException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService.getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene geneA, java.util.Collection genesB, java.util.Collection expressionExperiments, boolean filterNonSpecific)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService#getExpressionExperimentsTestedIn(java.util.Collection,
     *      java.util.Collection, boolean)
     */
    public java.util.Map<Long, Collection<BioAssaySet>> getExpressionExperimentsTestedIn(
            final java.util.Collection<Long> geneIds, final java.util.Collection<? extends BioAssaySet> experiments,
            final boolean filterNonSpecific ) {
        try {
            return this.handleGetExpressionExperimentsTestedIn( geneIds, experiments, filterNonSpecific );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionServiceException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService.getExpressionExperimentsTestedIn(java.util.Collection geneIds, java.util.Collection experiments, boolean filterNonSpecific)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService#getGenesTestedBy(ubic.gemma.model.expression.experiment.BioAssaySet,
     *      boolean)
     */
    public java.util.Collection<Long> getGenesTestedBy(
            final ubic.gemma.model.expression.experiment.BioAssaySet expressionExperiment,
            final boolean filterNonSpecific ) {
        try {
            return this.handleGetGenesTestedBy( expressionExperiment, filterNonSpecific );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionServiceException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService.getGenesTestedBy(ubic.gemma.model.expression.experiment.BioAssaySet expressionExperiment, boolean filterNonSpecific)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService#getProbeCoExpression(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      java.lang.String, boolean)
     */
    public java.util.Collection<ProbeLink> getProbeCoExpression(
            final ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment,
            final java.lang.String taxon, final boolean useWorkingTable ) {
        try {
            return this.handleGetProbeCoExpression( expressionExperiment, taxon, useWorkingTable );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionServiceException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService.getProbeCoExpression(ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment, java.lang.String taxon, boolean useWorkingTable)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService#prepareForShuffling(java.util.Collection,
     *      java.lang.String, boolean)
     */
    public void prepareForShuffling( final java.util.Collection<BioAssaySet> ees, final java.lang.String taxon,
            final boolean filterNonSpecific ) {
        try {
            this.handlePrepareForShuffling( ees, taxon, filterNonSpecific );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionServiceException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService.prepareForShuffling(java.util.Collection ees, java.lang.String taxon, boolean filterNonSpecific)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>probe2ProbeCoexpression</code>'s DAO.
     */
    public void setProbe2ProbeCoexpressionDao(
            ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao probe2ProbeCoexpressionDao ) {
        this.probe2ProbeCoexpressionDao = probe2ProbeCoexpressionDao;
    }

    /**
     * Gets the reference to <code>probe2ProbeCoexpression</code>'s DAO.
     */
    protected ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao getProbe2ProbeCoexpressionDao() {
        return this.probe2ProbeCoexpressionDao;
    }

    /**
     * Performs the core logic for {@link #countLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     */
    protected abstract java.lang.Integer handleCountLinks(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #create(java.util.List)}
     */
    protected abstract java.util.Collection<? extends Probe2ProbeCoexpression> handleCreate(
            java.util.Collection<? extends Probe2ProbeCoexpression> p2pExpressions ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #deleteLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     */
    protected abstract void handleDeleteLinks( ubic.gemma.model.expression.experiment.ExpressionExperiment ee )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene, java.util.Collection, boolean)}
     */
    protected abstract java.util.Collection<BioAssaySet> handleGetExpressionExperimentsLinkTestedIn(
            ubic.gemma.model.genome.Gene gene, java.util.Collection<? extends BioAssaySet> expressionExperiments,
            boolean filterNonSpecific ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene, java.util.Collection, java.util.Collection, boolean)}
     */
    protected abstract java.util.Map<Long, Collection<BioAssaySet>> handleGetExpressionExperimentsLinkTestedIn(
            ubic.gemma.model.genome.Gene geneA, java.util.Collection<Long> genesB,
            java.util.Collection<? extends BioAssaySet> expressionExperiments, boolean filterNonSpecific )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getExpressionExperimentsTestedIn(java.util.Collection, java.util.Collection, boolean)}
     */
    protected abstract java.util.Map<Long, Collection<BioAssaySet>> handleGetExpressionExperimentsTestedIn(
            java.util.Collection<Long> geneIds, java.util.Collection<? extends BioAssaySet> experiments,
            boolean filterNonSpecific ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getGenesTestedBy(ubic.gemma.model.expression.experiment.BioAssaySet, boolean)}
     */
    protected abstract java.util.Collection<Long> handleGetGenesTestedBy(
            ubic.gemma.model.expression.experiment.BioAssaySet expressionExperiment, boolean filterNonSpecific )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getProbeCoExpression(ubic.gemma.model.expression.experiment.ExpressionExperiment, java.lang.String, boolean)}
     */
    protected abstract java.util.Collection<ProbeLink> handleGetProbeCoExpression(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment, java.lang.String taxon,
            boolean useWorkingTable ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #prepareForShuffling(java.util.Collection, java.lang.String, boolean)}
     */
    protected abstract void handlePrepareForShuffling( java.util.Collection<BioAssaySet> ees, java.lang.String taxon,
            boolean filterNonSpecific ) throws java.lang.Exception;

    protected abstract void handleRemove( Collection<? extends Probe2ProbeCoexpression> expressions );

    protected abstract void handleRemove( Probe2ProbeCoexpression p2p );

}