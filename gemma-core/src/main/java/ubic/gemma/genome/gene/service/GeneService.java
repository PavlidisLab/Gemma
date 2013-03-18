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
package ubic.gemma.genome.gene.service;

import java.util.Collection;
import java.util.Map;
import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.analysis.expression.coexpression.QueryGeneCoexpression;
import ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegree;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.RelativeLocationData;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProductValueObject;

/**
 * @author kelsey
 * @version $Id$
 */
public interface GeneService {

    /**
     * @return
     */
    public Integer countAll();

    /**
     * @param genes
     * @return
     */
    @Secured({ "GROUP_ADMIN" })
    public Collection<Gene> create( Collection<Gene> genes );

    /**
     * @param gene
     * @return
     */
    @Secured({ "GROUP_ADMIN" })
    public Gene create( Gene gene );

    /**
     * @param gene
     * @return
     */
    public Gene find( Gene gene );

    /**
     * Find all genes at a physical location. All overlapping genes are returned. The location can be a point or a
     * region. If strand is non-null, only genes on the same strand are returned.
     * 
     * @param physicalLocation
     * @return
     */
    public Collection<Gene> find( PhysicalLocation physicalLocation );

    /**
     * @param accession
     * @param source
     * @return
     */
    public Gene findByAccession( String accession, ubic.gemma.model.common.description.ExternalDatabase source );

    /**
     * @param search
     * @return
     */
    public Collection<Gene> findByAlias( String search );

    public Collection<? extends Gene> findByEnsemblId( String exactString );

    /**
     * @param accession
     * @return
     */
    public Gene findByNCBIId( Integer accession );

    /**
     * @param accession
     * @return
     */
    public GeneValueObject findByNCBIIdValueObject( Integer accession );

    /**
     * @param officialName
     * @return
     */
    public Collection<Gene> findByOfficialName( String officialName );

    /**
     * @param officialName
     * @return
     */
    public Collection<Gene> findByOfficialNameInexact( String officialName );

    /**
     * @param officialSymbol
     * @return
     */
    public Collection<Gene> findByOfficialSymbol( String officialSymbol );

    /**
     * @param symbol
     * @param taxon
     * @return
     */
    public Gene findByOfficialSymbol( String symbol, Taxon taxon );

    /**
     * @param officialSymbol
     * @return
     */
    public Collection<Gene> findByOfficialSymbolInexact( String officialSymbol );

    public Collection<AnnotationValueObject> findGOTerms( Long geneId );

    /**
     * Find the gene(s) nearest to the location.
     * 
     * @param physicalLocation
     * @param useStrand if true, the nearest Gene on the same strand will be found. Otherwise the nearest gene on either
     *        strand will be returned.
     * @return RelativeLocationData - a value object for containing the gene that is nearest the given physical location
     */
    public RelativeLocationData findNearest( PhysicalLocation physicalLocation, boolean useStrand );

    /**
     * @param genes
     * @param ees
     * @param stringency
     * @param knownGenesOnly
     * @param interGenesOnly
     * @param interGenesOnly if true, only links among the query genes will be returned. This is ingored if only a
     *        single gene is entered
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ" })
    public Map<Gene, QueryGeneCoexpression> getCoexpressedGenes( Collection<Gene> genes,
            Collection<? extends BioAssaySet> ees, Integer stringency, boolean interGenesOnly );

    /**
     * Function to get coexpressed genes given a gene and a collection of expressionExperiments. Returns the value
     * object:: CoexpressionCollectionValueObject
     * 
     * @param gene
     * @param ees
     * @param stringency
     * @param knownGenesOnly
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ" })
    public QueryGeneCoexpression getCoexpressedGenes( Gene gene, Collection<? extends BioAssaySet> ees,
            Integer stringency );

    /**
     * @param id
     * @return
     */
    public long getCompositeSequenceCountById( Long id );

    /**
     * Returns a list of compositeSequences associated with the given gene and array design
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<CompositeSequence> getCompositeSequences( Gene gene, ArrayDesign arrayDesign );

    /**
     * @param id gemma gene id
     * @return Return probes for a given gene id.
     */
    public Collection<CompositeSequence> getCompositeSequencesById( Long id );

    /**
     * Get summarized node degree
     * 
     * @param genes
     * @return
     */
    public Map<Gene, GeneCoexpressionNodeDegree> getGeneCoexpressionNodeDegree( Collection<Gene> genes );

    public Map<Long, GeneCoexpressionNodeDegree> getGeneIdCoexpressionNodeDegree( Collection<Long> geneIds );

    /**
     * Get summarized node degree
     * 
     * @param gene
     * @return
     */
    public GeneCoexpressionNodeDegree getGeneCoexpressionNodeDegree( Gene gene );

    /**
     * Get dataset-by-dataset node degree information -- used to populate the tables for faster access methods. This
     * method is likely to be slow.
     * 
     * @param gene
     * @param ees
     * @return map of BioAssaySet to relative rank.
     */
    public Map<BioAssaySet, Double> getGeneCoexpressionNodeDegree( Gene gene, Collection<? extends BioAssaySet> ees );

    /**
     * Gets all the genes for a given taxon
     */
    public Collection<Gene> getGenesByTaxon( Taxon taxon );

    /**
     * Given the gemma id of a valid gemma gene will try to calculate the maximum extend of the transcript length. Does
     * this by using the gene products to find the largest max and min nucliotide positions
     * 
     * @param geneId
     * @return
     */
    public PhysicalLocation getMaxPhysicalLength( Gene gene );

    /**
     * @param geneId
     * @return empty collection if no products
     */
    public Collection<GeneProductValueObject> getProducts( Long geneId );

    /**
     * @param id
     * @return
     */
    public Gene load( long id );

    /**
     * @return
     */
    public Collection<Gene> loadAll();

    public GeneValueObject loadGenePhenotypes( Long geneId );

    /**
     * Returns a collection of genes for the specified taxon
     */
    public Collection<Gene> loadAll( Taxon taxon );

    /**
     * Gets all the microRNAs for a given taxon. Note query could be slow or inexact due to use of wild card searching
     * of the genes description
     */
    public Collection<Gene> loadMicroRNAs( Taxon taxon );

    /**
     * load all genes specified by the given ids.
     * 
     * @return A collection containing up to ids.size() genes.
     */
    public Collection<Gene> loadMultiple( Collection<Long> ids );

    /**
     * Load with objects already thawed.
     * 
     * @param ids
     * @return
     */
    public Collection<Gene> loadThawed( Collection<Long> ids );

    public Collection<Gene> loadThawedLiter( Collection<Long> ids );

    /**
     * @param ids
     * @return
     */
    public GeneValueObject loadValueObject( Long id );
    
    /**
     * @param ids
     * @return
     */
    public GeneValueObject loadFullyPopulatedValueObject( Long id );

    /**
     * @param ids
     * @return
     */
    public Collection<GeneValueObject> loadValueObjects( Collection<Long> ids );

    public Collection<GeneValueObject> loadValueObjectsLiter( Collection<Long> ids );

    /**
     * @param genes
     */
    @Secured({ "GROUP_ADMIN" })
    public void remove( Collection<Gene> genes );

    /**
     * @param gene
     */
    @Secured({ "GROUP_ADMIN" })
    public void remove( Gene gene );

    /**
     * @param gene
     */
    public Gene thaw( Gene gene );

    /**
     * Only thaw the Aliases, very light version
     * 
     * @param gene
     */
    public Gene thawAliases( Gene gene );

    /**
     * @param genes
     * @see loadThawed as a way to avoid the load..thaw pattern.
     */
    public Collection<Gene> thawLite( Collection<Gene> genes );

    /**
     * @param gene
     */
    public Gene thawLite( Gene gene );

    public Gene thawLiter( Gene gene );

    /**
     * @param gene
     */
    @Secured({ "GROUP_ADMIN" })
    /* we would need to relax this to allow phenotype associations to be added, but I think we should avoid doing that */
    public void update( Gene gene );

    @Secured({ "GROUP_ADMIN" })
    public void update( Collection<Gene> genes );

}
