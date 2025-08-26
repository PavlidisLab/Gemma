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
package ubic.gemma.persistence.service.association.coexpression;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.analysis.expression.coexpression.SupportDetails;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpression;
import ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegreeValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A key service for working with coexpression at a fairly low level. This is responsible for CUD operations; retrieval
 * is by 'find' methods.
 * Note that all of the 'find' methods require a non-empty set of dataset ids to query. It is the responsibility of the
 * caller to security-filter the ids. The need for security is why we don't permit queries of "all" data sets at this
 * level. Retrieval of information on gene node degree is not constrained by security, which we deemed acceptable
 * because the only information returned is a summary count.
 *
 * @author Gemma
 */
public interface CoexpressionService {

    /**
     * Check if a given dataset has coexpression links.
     */
    boolean hasLinks( ExpressionExperiment ee );

    /**
     * @param gene gene
     * @param ee   bio assay set
     * @return the number of links the gene has in the given data set ("node degree")
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Integer countLinks( ExpressionExperiment ee, Gene gene );

    /**
     * Maintenance method.
     *
     * @param ee should be all of them for the bioAssaySet (not a batch)
     * @param geesTested  genes which were tested
     * @param c           link creator
     * @param links       links
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void createOrUpdate( ExpressionExperiment ee, List<NonPersistentNonOrderedCoexpLink> links, LinkCreator c,
            Set<Gene> geesTested );

    /**
     * Maintenance method. Remove coexpression information from the database about the experiment in question (this does
     * not remove the analysis object).
     *
     * @param experiment experiment
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void deleteLinks( ExpressionExperiment experiment );

    /**
     * Find links which are common to all of the given data sets.
     *
     * @param bas        data sets the link must be supported by; that is, the stringency is implied by bas.size(). Assumed to
     *                   be security-filtered.
     * @param gene       gene
     * @param quick      quick
     * @param maxResults max results
     * @return coexpression results.
     */
    List<CoexpressionValueObject> findCoexpressionRelationships( Gene gene, Collection<Long> bas, int maxResults,
            boolean quick );

    /**
     * Search for coexpression across all available data sets, for the given genes considered individually, subject to a
     * stringency constraint.
     *
     * @param bas        assumed to be security-filtered.
     * @param stringency the minimum number of data sets for which the coexpression must be observed, among the given
     *                   datasets.
     * @param quick      quick
     * @param maxResults max results
     * @param gene       gene
     * @return coexpression results.
     */
    List<CoexpressionValueObject> findCoexpressionRelationships( Gene gene, Collection<Long> bas, int stringency,
            int maxResults, boolean quick );

    /**
     * Find coexpression links for the genes that are common to all the given datasets (that is, the stringency is equal
     * to the size of the set of datasets)
     *
     * @param bas        - assumed to already be security-filtered
     * @param quick      quick
     * @param maxResults max results
     * @param genes      genes
     * @param t          taxon
     * @return a map of gene IDs to coexpression results.
     */
    Map<Long, List<CoexpressionValueObject>> findCoexpressionRelationships( Taxon t, Collection<Long> genes,
            Collection<Long> bas, int maxResults, boolean quick );

    /**
     * Find coexpression links for the genes that are common to at least <em>stringency</em> of the given datasets.
     *
     * @param bas        assumed to already be security-filtered
     * @param maxResults limit to the number of results per gene, but connections among the query genes (if there is
     *                   more than one) are given priority and not subject to the limit.
     * @param stringency the minimum number of data sets for which the coexpression must be observed, among the given
     *                   datasets.
     * @param quick      quick
     * @param t          taxon
     * @param genes      genes
     * @return a map of gene IDs to coexpression results.
     */
    Map<Long, List<CoexpressionValueObject>> findCoexpressionRelationships( Taxon t, Collection<Long> genes,
            Collection<Long> bas, int stringency, int maxResults, boolean quick );

    /**
     * Return coexpression relationships among the given genes in the given data sets, in a map of query gene to
     * coexpression objects.
     *
     * @param bas        data sets to be considered, presumed to be security filtered already
     * @param stringency Must be less than or equal to the number of data sets
     *                   datasets.
     * @param quick      quick
     * @param t          taxon
     * @param genes      genes
     * @return a map of gene IDs to coexpression results.
     */
    Map<Long, List<CoexpressionValueObject>> findInterCoexpressionRelationships( Taxon t, Collection<Long> genes,
            Collection<Long> bas, int stringency, boolean quick );

    /**
     * @param quick      quick
     * @param experiment experiment
     * @return all the coexpression links for the given experiment, but not including flipped versions
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<CoexpressionValueObject> getCoexpression( ExpressionExperiment experiment, boolean quick );

    @Secured("GROUP_ADMIN")
    void updateNodeDegrees( Taxon taxon );

    GeneCoexpressionNodeDegreeValueObject getNodeDegree( Gene g );

    Map<Long, GeneCoexpressionNodeDegreeValueObject> getNodeDegrees( Collection<Long> genes );

    /**
     * @param gene       gene
     * @param idMap      id map
     * @param linksSoFar links so far
     * @param skipGenes  skip genes
     * @return links that were made
     */
    @Secured("GROUP_ADMIN")
    Map<SupportDetails, Gene2GeneCoexpression> initializeLinksFromOldData( Gene gene, Map<Long, Gene> idMap,
            Map<NonPersistentNonOrderedCoexpLink, SupportDetails> linksSoFar, Set<Long> skipGenes );

    @Secured("GROUP_ADMIN")
    Map<Gene, Integer> countOldLinks( Collection<Gene> genes );
}
