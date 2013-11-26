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

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * @author paul
 * @version $Id$
 */
public interface Probe2ProbeCoexpressionService {

    /*
     * Security notes: p2p mod methods set so users can update coexpression.
     */

    /**
     * @param id
     * @return number of coexpression links for the given experiment or null if the analysis has not been run
     */
    public Integer countLinks( Long id );

    /**
     * Adds a collection of probe2probeCoexpression objects at one time to the DB, in the order given.
     */
    @Secured({ "GROUP_USER" })
    public Collection<? extends Probe2ProbeCoexpression> create(
            Collection<? extends Probe2ProbeCoexpression> p2pExpressions );

    /**
     * @param deletes
     */
    @Secured({ "GROUP_USER" })
    public void delete( Collection<? extends Probe2ProbeCoexpression> deletes );

    /**
     * @param toDelete
     */
    @Secured({ "GROUP_USER" })
    public void delete( ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression toDelete );

    /**
     * removes all the probe2probeCoexpression links for the given expression experiment
     * 
     * @param ee
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void deleteLinks( ExpressionExperiment ee );

    /**
     * Determine which probes, among those provided, actually appear together in links.
     * 
     * @param queryProbeIds
     * @param coexpressedProbeIds
     * @param ee
     * @param taxon (to save another query) common name
     * @return the probes, among the query and coexpressed probes given, which appear in coexpression links.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<Long> getCoexpressedProbes( Collection<Long> queryProbeIds, Collection<Long> coexpressedProbeIds,
            ExpressionExperiment ee, String taxon );

    /***
     * Return a list of all ExpressionExperiments in which the given gene was tested for coexpression in, among the
     * given ExpressionExperiments. A gene was tested if any probe for that gene passed filtering criteria during
     * analysis. It is assumed that in the database there is only one analysis per ExpressionExperiment. The boolean
     * parameter filterNonSpecific can be used to exclude ExpressionExperiments in which the gene was detected by only
     * probes predicted to be non-specific for the gene.
     * 
     * @param gene
     * @param expressionExperiments
     * @param filterNonSpecific
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ", "ACL_SECURABLE_COLLECTION_READ" })
    public Collection<BioAssaySet> getExpressionExperimentsLinkTestedIn( Gene gene,
            Collection<? extends BioAssaySet> expressionExperiments, boolean filterNonSpecific );

    /***
     * Return a map of genes in genesB to all ExpressionExperiments in which the given set of pairs of genes was tested
     * for coexpression in, among the given ExpressionExperiments. A gene was tested if any probe for that gene passed
     * filtering criteria during analysis. It is assumed that in the database there is only one analysis per
     * ExpressionExperiment. The boolean parameter filterNonSpecific can be used to exclude ExpressionExperiments in
     * which one or both of the genes were detected by only probes predicted to be non-specific for the gene.
     * 
     * @param geneA
     * @param genesB
     * @param expressionExperiments
     * @param filterNonSpecific
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ" })
    public Map<Long, Collection<BioAssaySet>> getExpressionExperimentsLinkTestedIn( ubic.gemma.model.genome.Gene geneA,
            Collection<Long> genesB, Collection<? extends BioAssaySet> expressionExperiments, boolean filterNonSpecific );

    /**
     * @param geneIds
     * @param experiments
     * @param filterNonSpecific
     * @return Map of gene ids to BioAssaySets among those provided in which the gene was tested for coexpression.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ" })
    public Map<Long, Collection<BioAssaySet>> getExpressionExperimentsTestedIn( Collection<Long> geneIds,
            Collection<? extends BioAssaySet> experiments, boolean filterNonSpecific );

    /**
     * Retrieve all genes that were included in the link analysis for the experiment.
     * 
     * @param expressionExperiment
     * @param filterNonSpecific
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<Long> getGenesTestedBy( ubic.gemma.model.expression.experiment.BioAssaySet expressionExperiment,
            boolean filterNonSpecific );

    /**
     * get the co-expression by using native sql query but doesn't use a temporary DB table.
     * 
     * @param expressionExperiment
     * @param taxon
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<ProbeLink> getProbeCoExpression(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment, java.lang.String taxon );

    /**
     * Get the co-expression by using native sql query
     * 
     * @param expressionExperiment
     * @param taxon
     * @param useWorkingTable
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<ProbeLink> getProbeCoExpression( ExpressionExperiment expressionExperiment, String taxon,
            boolean useWorkingTable );

    /**
     * Create a working table containing links by removing redundant and (optionally) non-specific probes from
     * PROBE_CO_EXPRESSION. Results are stored in a species-specific temporary table managed by this method. This is
     * only used for statistics gathering in probe evaluation experiments, not used by normal applications.
     * 
     * @param ees
     * @param taxon
     * @param filterNonSpecific
     */
    @Secured({ "GROUP_ADMIN", "ACL_SECURABLE_COLLECTION_READ" })
    public void prepareForShuffling( Collection<BioAssaySet> ees, java.lang.String taxon, boolean filterNonSpecific );

}
