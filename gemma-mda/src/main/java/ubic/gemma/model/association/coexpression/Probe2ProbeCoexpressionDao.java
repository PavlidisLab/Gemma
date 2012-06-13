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

import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression
 */
public interface Probe2ProbeCoexpressionDao {

    public Collection<? extends Probe2ProbeCoexpression> create( Collection<? extends Probe2ProbeCoexpression> entities );

    public void remove( Collection<? extends Probe2ProbeCoexpression> links );

    public void remove( Probe2ProbeCoexpression link );

    /**
     * Get the total number of probe2probe coexpression links for the given experiment.
     */
    public Integer countLinks( ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    /**
     * Removes the all the probe2probeCoexpression links for a given expression experiment
     */
    public void deleteLinks( BioAssaySet bioAssaySet );

    /**
     * Return a list of all BioAssaySets in which the given gene was tested for coexpression in, among the given
     * ExpressionExperiments. A gene was tested if any probe for that gene passed filtering criteria during analysis. It
     * is assumed that in the database there is only one analysis per ExpressionExperiment. The boolean parameter
     * filterNonSpecific can be used to exclude ExpressionExperiments in which the gene was detected by only probes
     * predicted to be non-specific for the gene.
     */
    public Collection<BioAssaySet> getExpressionExperimentsLinkTestedIn( Gene gene,
            Collection<? extends BioAssaySet> expressionExperiments, boolean filterNonSpecific );

    /**
     * Return a map of genes in genesB to all ExpressionExperiments in which the given set of pairs of genes was tested
     * for coexpression in, among the given ExpressionExperiments. A gene was tested if any probe for that gene passed
     * filtering criteria during analysis. It is assumed that in the database there is only one analysis per
     * ExpressionExperiment. The boolean parameter filterNonSpecific can be used to exclude ExpressionExperiments in
     * which one or both of the genes were detected by only probes predicted to be non-specific for the gene.
     */
    public Map<Long, Collection<BioAssaySet>> getExpressionExperimentsLinkTestedIn( Gene geneA,
            Collection<Long> genesB, Collection<? extends BioAssaySet> expressionExperiments, boolean filterNonSpecific );

    /**
     * 
     */
    public Map<Long, Collection<BioAssaySet>> getExpressionExperimentsTestedIn( Collection<Long> geneIds,
            Collection<? extends BioAssaySet> experiments, boolean filterNonSpecific );

    /**
     * Retrieve all genes that were included in the link analysis for the experiment.
     */
    public Collection<Long> getGenesTestedBy( BioAssaySet expressionExperiment, boolean filterNonSpecific );

    /**
     * get probe coexpression by using native sql query.
     */
    public Collection<ProbeLink> getProbeCoExpression( ExpressionExperiment expressionExperiment,
            String taxonCommonName, boolean useWorkingTable );

    /**
     * Create working table if links to use in shuffled-link experiments.
     */
    public void prepareForShuffling( Collection<BioAssaySet> ees, java.lang.String taxon, boolean filterNonSpecific );

    public Collection<Long> getCoexpressedProbes( Collection<Long> queryProbeIds, Collection<Long> coexpressedProbeIds,
            ExpressionExperiment ee, String taxon );

}
