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

import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
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
     * <p>
     * Get the total number of probe2probe coexpression links for the given experiment.
     * </p>
     */
    public java.lang.Integer countLinks(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    /**
     * <p>
     * Removes the all the probe2probeCoexpression links for a given expression experiment
     * </p>
     */
    public void deleteLinks( ubic.gemma.model.expression.experiment.ExpressionExperiment ee );

    /**
     * <p>
     * Return a list of all BioAssaySets in which the given gene was tested for coexpression in, among the given
     * ExpressionExperiments. A gene was tested if any probe for that gene passed filtering criteria during analysis. It
     * is assumed that in the database there is only one analysis per ExpressionExperiment. The boolean parameter
     * filterNonSpecific can be used to exclude ExpressionExperiments in which the gene was detected by only probes
     * predicted to be non-specific for the gene.
     * </p>
     */
    public java.util.Collection<BioAssaySet> getExpressionExperimentsLinkTestedIn( ubic.gemma.model.genome.Gene gene,
            java.util.Collection<BioAssaySet> expressionExperiments, boolean filterNonSpecific );

    /**
     * <p>
     * Return a map of genes in genesB to all ExpressionExperiments in which the given set of pairs of genes was tested
     * for coexpression in, among the given ExpressionExperiments. A gene was tested if any probe for that gene passed
     * filtering criteria during analysis. It is assumed that in the database there is only one analysis per
     * ExpressionExperiment. The boolean parameter filterNonSpecific can be used to exclude ExpressionExperiments in
     * which one or both of the genes were detected by only probes predicted to be non-specific for the gene.
     * </p>
     */
    public java.util.Map<Long, Collection<BioAssaySet>> getExpressionExperimentsLinkTestedIn(
            ubic.gemma.model.genome.Gene geneA, java.util.Collection<Long> genesB,
            java.util.Collection<BioAssaySet> expressionExperiments, boolean filterNonSpecific );

    /**
     * 
     */
    public java.util.Map<Long, Collection<BioAssaySet>> getExpressionExperimentsTestedIn(
            java.util.Collection<Long> geneIds, java.util.Collection<Long> experiments, boolean filterNonSpecific );

    /**
     * <p>
     * Retrieve all genes that were included in the link analysis for the experiment.
     * </p>
     */
    public java.util.Collection<Long> getGenesTestedBy(
            ubic.gemma.model.expression.experiment.BioAssaySet expressionExperiment, boolean filterNonSpecific );

    /**
     * <p>
     * get the probe coexpression by using native sql query.
     * </p>
     */
    public java.util.Collection<ProbeLink> getProbeCoExpression(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment,
            java.lang.String taxonCommonName, boolean useWorkingTable );

    /**
     * Returns the top coexpressed links under a given threshold for a given experiment up to a given limit. If the
     * limit is null then all results under the threshold will be returned.
     * 
     * @param ee
     * @param threshold
     * @param limit
     * @return
     */
    public Collection<ProbeLink> getTopCoexpressedLinks( ExpressionExperiment ee, double threshold, Integer limit );

    /**
     * <p>
     * Given a collection of Genes, a collection of EE's return a Map of Genes to a collection of
     * DesignElementDataVectors that are coexpressed
     * </p>
     */
    public java.util.Map<Gene, DesignElementDataVector> getVectorsForLinks( java.util.Collection<Gene> genes,
            java.util.Collection<ExpressionExperiment> ees );

    /**
     * 
     */
    public java.util.Collection<DesignElementDataVector> getVectorsForLinks( ubic.gemma.model.genome.Gene gene,
            java.util.Collection<ExpressionExperiment> ees );

    /**
     * <p>
     * Create working table if links to use in shuffled-link experiments.
     * </p>
     */
    public void prepareForShuffling( java.util.Collection<ExpressionExperiment> ees, java.lang.String taxon,
            boolean filterNonSpecific );

    public Collection<Long> getCoexpressedProbes( Collection<Long> queryProbeIds, Collection<Long> coexpressedProbeIds,
            ExpressionExperiment ee, String taxon );

}
