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

import ubic.gemma.model.analysis.expression.coexpression.SupportDetails;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpression;
import ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegree;
import ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegreeValueObject;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpression
 */
public interface CoexpressionDao {

    Integer countLinks( Gene gene, BioAssaySet ee );

    /**
     * @param p2plinks in gene order
     */
    void createOrUpdate( BioAssaySet bioAssaySet, List<NonPersistentNonOrderedCoexpLink> p2plinks, LinkCreator c,
            Set<Gene> genesTested );

    void deleteLinks( Taxon taxon, BioAssaySet experiment );

    /**
     * Find coexpression links for a gene that are <em>common</em> to all the given datasets. That is the stringency is
     * bas.size().
     */
    List<CoexpressionValueObject> findCoexpressionRelationships( Gene gene, Collection<Long> bas, int maxResults,
            boolean quick );

    /**
     * Find coexpression links for the genes that are common to all the given datasets, so stringency = bas.size().
     */
    Map<Long, List<CoexpressionValueObject>> findCoexpressionRelationships( Taxon taxon, Collection<Long> genes,
            Collection<Long> bas, int maxResults, boolean quick );

    /**
     * @param bas        limit on which data sets to query, or null (or empty) for no limit.
     * @param stringency minimum number of the datasets the link must be supported by
     * @param maxResults maximum results per gene.
     */
    Map<Long, List<CoexpressionValueObject>> findCoexpressionRelationships( Taxon t, Collection<Long> genes,
            Collection<Long> bas, int stringency, int maxResults, boolean quick );

    /**
     * Return coexpression relationships among the given genes, limited to the given data sets.
     */
    Map<Long, List<CoexpressionValueObject>> findInterCoexpressionRelationships( Taxon taxon, Collection<Long> genes,
            Collection<Long> bas, int stringency, boolean quick );

    /**
     * This is a maintenance method. This requires doing a coexpression query for the gene, and updating (or, if need
     * be, creating) the associated GeneCoexpressionNodeDegree object.
     *
     * @return updated value object
     */
    GeneCoexpressionNodeDegreeValueObject updateNodeDegree( Gene gene, GeneCoexpressionNodeDegree nd );

    /**
     * @return links, but not including flipped versions
     */
    Collection<CoexpressionValueObject> getCoexpression( Taxon taxon, BioAssaySet experiment, boolean quick );

    /**
     * @return number of links that were cached
     */
    int queryAndCache( Gene gene );

    Map<SupportDetails, Gene2GeneCoexpression> initializeFromOldData( Gene gene, Map<Long, Gene> geneIdMap,
            Map<NonPersistentNonOrderedCoexpLink, SupportDetails> linksSoFar, Set<Long> skipGenes );

    Map<Gene, Integer> countOldLinks( Collection<Gene> genes );

    void updateRelativeNodeDegrees( Map<Long, List<Double>> relRanksPerGenePos,
            Map<Long, List<Double>> relRanksPerGeneNeg );

}