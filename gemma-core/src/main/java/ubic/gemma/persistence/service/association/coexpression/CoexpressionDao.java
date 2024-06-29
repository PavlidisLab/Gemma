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

import ubic.gemma.core.lang.Nullable;
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

    boolean hasLinks( Taxon taxon, BioAssaySet ee );

    Integer countLinks( Gene gene, BioAssaySet ee );

    /**
     * @param p2plinks    in gene order
     * @param bioAssaySet bio assay set
     * @param c           link creator
     * @param genesTested genes tested
     */
    void createOrUpdate( BioAssaySet bioAssaySet, List<NonPersistentNonOrderedCoexpLink> p2plinks, LinkCreator c,
            Set<Gene> genesTested );

    void deleteLinks( Taxon taxon, BioAssaySet experiment );

    /**
     * Find coexpression links for a gene that are <em>common</em> to all the given datasets. That is the stringency is
     * bas.size().
     *
     * @param bas        bio assays
     * @param gene       gene
     * @param maxResults max results
     * @param quick      quick
     * @return coexpression VO
     */
    List<CoexpressionValueObject> findCoexpressionRelationships( Gene gene, Collection<Long> bas, int maxResults,
            boolean quick );

    /**
     * Find coexpression links for the genes that are common to all the given datasets, so stringency = bas.size().
     *
     * @param quick      quick
     * @param maxResults max results
     * @param bas        bio assays
     * @param taxon      taxon
     * @param genes      genes
     * @return map of ids to coexp. VOs
     */
    Map<Long, List<CoexpressionValueObject>> findCoexpressionRelationships( Taxon taxon, Collection<Long> genes,
            Collection<Long> bas, int maxResults, boolean quick );

    /**
     * @param bas        limit on which data sets to query, or null (or empty) for no limit.
     * @param stringency minimum number of the datasets the link must be supported by
     * @param maxResults maximum results per gene.
     * @param genes      genes
     * @param quick      quick
     * @param t          taxon
     * @return map of ids to coexp. VOs
     */
    Map<Long, List<CoexpressionValueObject>> findCoexpressionRelationships( Taxon t, Collection<Long> genes,
            Collection<Long> bas, int stringency, int maxResults, boolean quick );

    /**
     * Return coexpression relationships among the given genes, limited to the given data sets.
     *
     * @param quick      quick
     * @param genes      genes
     * @param taxon      taxon
     * @param bas        bio assays
     * @param stringency stringency
     * @return map of ids to coexp. VOs
     */
    Map<Long, List<CoexpressionValueObject>> findInterCoexpressionRelationships( Taxon taxon, Collection<Long> genes,
            Collection<Long> bas, int stringency, boolean quick );

    /**
     * This is a maintenance method. This requires doing a coexpression query for the gene, and updating (or, if need
     * be, creating) the associated GeneCoexpressionNodeDegree object.
     *
     * @param gene gene
     * @param nd   node degree
     * @return updated value object
     */
    GeneCoexpressionNodeDegreeValueObject updateNodeDegree( Gene gene, GeneCoexpressionNodeDegree nd );

    /**
     * @param taxon      taxon
     * @param quick      quick
     * @param experiment experiment
     * @return links, but not including flipped versions
     */
    Collection<CoexpressionValueObject> getCoexpression( Taxon taxon, BioAssaySet experiment, boolean quick );

    /**
     * @param gene gene
     * @return number of links that were cached
     */
    int queryAndCache( Gene gene );

    @Nullable
    Map<SupportDetails, Gene2GeneCoexpression> initializeFromOldData( Gene gene, Map<Long, Gene> geneIdMap,
            Map<NonPersistentNonOrderedCoexpLink, SupportDetails> linksSoFar, Set<Long> skipGenes );

    Map<Gene, Integer> countOldLinks( Collection<Gene> genes );

    void updateRelativeNodeDegrees( Map<Long, List<Double>> relRanksPerGenePos,
            Map<Long, List<Double>> relRanksPerGeneNeg );

}