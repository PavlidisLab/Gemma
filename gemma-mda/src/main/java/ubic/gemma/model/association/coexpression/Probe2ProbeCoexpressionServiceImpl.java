/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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

import org.springframework.stereotype.Service;

import ubic.gemma.model.common.auditAndSecurity.eventType.LinkAnalysisEvent;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService
 * @versio n$Id$
 * @author paul
 */
@Service
public class Probe2ProbeCoexpressionServiceImpl extends
        ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionServiceBase {

    @Override
    public java.util.Collection<ProbeLink> getProbeCoExpression(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment, java.lang.String taxon ) {
        return this.getProbe2ProbeCoexpressionDao().getProbeCoExpression( expressionExperiment, taxon, false );
    }

    @Override
    public Collection<Long> getCoexpressedProbes( Collection<Long> queryProbeIds, Collection<Long> coexpressedProbeIds,
            ExpressionExperiment ee, String taxon ) {
        return this.getProbe2ProbeCoexpressionDao()
                .getCoexpressedProbes( queryProbeIds, coexpressedProbeIds, ee, taxon );
    }

    @Override
    protected Integer handleCountLinks( ExpressionExperiment expressionExperiment ) {
        Integer count = this.getProbe2ProbeCoexpressionDao().countLinks( expressionExperiment );
        if ( count == 0
                && this.getAuditEventDao().getLastEvent( expressionExperiment, LinkAnalysisEvent.class ) == null ) {
            /*
             * analysis has not been run, so reporting zero would be misleading. I only check in case of count == 0 to
             * avoid problems in case the link count > 0 but the audit event is missing for some reason: we still report
             * an accurate count.
             */
            return null;
        }
        return count;
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionServiceBase#handleCreate(java.util.Collection)
     */
    @Override
    protected Collection<? extends Probe2ProbeCoexpression> handleCreate(
            Collection<? extends Probe2ProbeCoexpression> p2pExpressions ) {
        return this.getProbe2ProbeCoexpressionDao().create( p2pExpressions );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionServiceBase#handleDeleteLinks(ubic.gemma.model
     * .expression.experiment.ExpressionExperiment)
     */
    @Override
    protected void handleDeleteLinks( ExpressionExperiment ee ) {
        this.getProbe2ProbeCoexpressionDao().deleteLinks( ee );

    }

    @Override
    protected Collection<BioAssaySet> handleGetExpressionExperimentsLinkTestedIn( Gene gene,
            Collection<? extends BioAssaySet> expressionExperiments, boolean filterNonSpecific ) {
        return this.getProbe2ProbeCoexpressionDao().getExpressionExperimentsLinkTestedIn( gene, expressionExperiments,
                filterNonSpecific );
    }

    @Override
    protected Map<Long, Collection<BioAssaySet>> handleGetExpressionExperimentsLinkTestedIn( Gene geneA,
            Collection<Long> genesB, Collection<? extends BioAssaySet> expressionExperiments, boolean filterNonSpecific ) {
        return this.getProbe2ProbeCoexpressionDao().getExpressionExperimentsLinkTestedIn( geneA, genesB,
                expressionExperiments, filterNonSpecific );
    }

    @Override
    protected Map<Long, Collection<BioAssaySet>> handleGetExpressionExperimentsTestedIn( Collection<Long> genes,
            Collection<? extends BioAssaySet> expressionExperiments, boolean filterNonSpecific ) {
        return this.getProbe2ProbeCoexpressionDao().getExpressionExperimentsTestedIn( genes, expressionExperiments,
                filterNonSpecific );
    }

    @Override
    protected Collection<Long> handleGetGenesTestedBy( BioAssaySet bioAssaySet, boolean filterNonSpecific ) {
        return this.getProbe2ProbeCoexpressionDao().getGenesTestedBy( bioAssaySet, filterNonSpecific );
    }

    @Override
    protected Collection<ProbeLink> handleGetProbeCoExpression( ExpressionExperiment expressionExperiment,
            String taxon, boolean cleaned ) {
        // cleaned: a temporary table is created.s
        return this.getProbe2ProbeCoexpressionDao().getProbeCoExpression( expressionExperiment, taxon, cleaned );
    }

    @Override
    protected void handlePrepareForShuffling( Collection<BioAssaySet> ees, String taxon, boolean filterNonSpecific ) {
        this.getProbe2ProbeCoexpressionDao().prepareForShuffling( ees, taxon, filterNonSpecific );
    }

    @Override
    protected void handleRemove( Collection<? extends Probe2ProbeCoexpression> links ) {
        this.getProbe2ProbeCoexpressionDao().remove( links );

    }

    @Override
    protected void handleRemove( Probe2ProbeCoexpression link ) {
        this.getProbe2ProbeCoexpressionDao().remove( link );

    }

}