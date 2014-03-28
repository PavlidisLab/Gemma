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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.analysis.expression.coexpression.SupportDetails;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * A key service for working with coexpression at a fairly low level. This is responsible for CUD operations; retrieval
 * is by 'find' methods.
 * <p>
 * Note that all of the 'find' methods require a non-empty set of dataset ids to query. It is the responsibility of the
 * caller to security-filter the ids. The need for security is why we don't permit queries of "all" data sets at this
 * level. Retrieval of information on gene node degree is not constrained by security, which we deemed acceptable
 * because the only information returned is a summary count.
 * 
 * @author Gemma
 * @version $Id$
 */
public interface CoexpressionService {

    /**
     * @param ee
     * @param gene
     * @return the number of links the gene has in the given data set ("node degree")
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public abstract Integer countLinks( BioAssaySet ee, Gene gene );

    /**
     * Maintenance method.
     * 
     * @param bioAssaySet should be all of them for the bioAssaySet (not a batch)
     * @param links
     * @param c
     * @param genes which were tested
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public abstract void createOrUpdate( BioAssaySet bioAssaySet, List<NonPersistentNonOrderedCoexpLink> links,
            LinkCreator c, Set<Gene> geesTested );

    /**
     * Maintenance method. Remove coexpression information from the database about the experiment in question (this does
     * not remove the analysis object).
     * 
     * @param experiment
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public abstract void deleteLinks( BioAssaySet experiment );

    /**
     * Find links which are common to all of the given data sets.
     * 
     * @param gene
     * @param bas data sets the link must be supported by; that is, the stringency is implied by bas.size(). Assumed to
     *        be security-filtered.
     * @param maxResults
     * @return coexpression results.
     */
    public abstract List<CoexpressionValueObject> findCoexpressionRelationships( Gene gene, Collection<Long> bas,
            int maxResults, boolean quick );

    /**
     * Search for coexpression across all available data sets, for the given genes considered individually, subject to a
     * stringency constraint.
     * 
     * @param gene
     * @param bas assumed to be security-filtered.
     * @param stringency the minimum number of data sets for which the coexpression must be observed, among the given
     *        datasets.
     * @param maxResults
     * @return coexpression results.
     */
    public abstract List<CoexpressionValueObject> findCoexpressionRelationships( Gene gene, Collection<Long> bas,
            int stringency, int maxResults, boolean quick );

    /**
     * Find coexpression links for the genes that are common to all the given datasets (that is, the stringency is equal
     * to the size of the set of datasets)
     * 
     * @param genes
     * @param bas - assumed to already be security-filtered
     * @param maxResults
     * @return a map of gene IDs to coexpression results.
     */
    public abstract Map<Long, List<CoexpressionValueObject>> findCoexpressionRelationships( Taxon t,
            Collection<Long> genes, Collection<Long> bas, int maxResults, boolean quick );

    /**
     * Find coexpression links for the genes that are common to at least <em>stringency</em> of the given datasets.
     * 
     * @param genes
     * @param bas assumed to already be security-filtered
     * @param stringency
     * @param maxResults limit to the number of results per gene, but connections among the query genes (if there is
     *        more than one) are given priority and not subject to the limit.
     * @return a map of gene IDs to coexpression results.
     */
    public abstract Map<Long, List<CoexpressionValueObject>> findCoexpressionRelationships( Taxon t,
            Collection<Long> genes, Collection<Long> bas, int stringency, int maxResults, boolean quick );

    /**
     * Return coexpression relationships among the given genes in the given data sets, in a map of query gene to
     * coexpression objects.
     * 
     * @param genes
     * @param bas data sets to be considered, presumed to be security filtered already
     * @param stringency Must be less than or equal to the number of data sets
     * @return
     */
    public abstract Map<Long, List<CoexpressionValueObject>> findInterCoexpressionRelationships( Taxon t,
            Collection<Long> genes, Collection<Long> bas, int stringency, boolean quick );

    /**
     * @param experiment
     * @return all the coexpression links for the given experiment, but not including flipped versions
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<CoexpressionValueObject> getCoexpression( BioAssaySet experiment, boolean quick );

    @Secured("GROUP_ADMIN")
    public abstract void updateNodeDegrees( Taxon taxon );

    /**
     * @param g
     * @return
     */
    public GeneCoexpressionNodeDegreeValueObject getNodeDegree( Gene g );

    /**
     * @param allUsedGenes
     */
    public abstract Map<Long, GeneCoexpressionNodeDegreeValueObject> getNodeDegrees( Collection<Long> genes );

    /**
     * @param gene
     * @param idMap
     * @param skipGenes
     * @return links that were made
     */
    @Secured("GROUP_ADMIN")
    public abstract Map<SupportDetails, Gene2GeneCoexpression> initializeLinksFromOldData( Gene gene,
            Map<Long, Gene> idMap, Map<NonPersistentNonOrderedCoexpLink, SupportDetails> linksSoFar, Set<Long> skipGenes );

    /**
     * @param genes
     * @return
     */
    @Secured("GROUP_ADMIN")
    public abstract Map<Gene, Integer> countOldLinks( Collection<Gene> genes );

}
