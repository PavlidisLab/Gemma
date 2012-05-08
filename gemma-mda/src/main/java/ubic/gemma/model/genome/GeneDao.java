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
package ubic.gemma.model.genome;

import java.util.Collection;
import java.util.Map;

import ubic.gemma.model.analysis.expression.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegree;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.persistence.BaseDao;

/**
 * @see ubic.gemma.model.genome.Gene
 */
public interface GeneDao extends BaseDao<Gene> {

    public Integer countAll();

    /**
     * Find all genes at a physical location. All overlapping genes are returned. The location can be a point or a
     * region. If strand is non-null, only genes on the same strand are returned.
     * 
     * @param physicalLocation
     * @return
     */
    public Collection<Gene> find( PhysicalLocation physicalLocation );

    /**
     * 
     */
    public Gene find( Gene gene );

    /**
     * 
     */
    public Gene findByAccession( String accession,
            ExternalDatabase source );

    /**
     * <p>
     * Locate genes that match the given alias string
     * </p>
     */
    public Collection<Gene> findByAlias( java.lang.String search );

    public Collection<? extends Gene> findByEnsemblId( String exactString );

    public Gene findByNcbiId( Integer accession );

    /**
     * <p>
     * Finder based on the official name.
     * </p>
     */
    public java.util.Collection<Gene> findByOfficalSymbol( java.lang.String officialSymbol );

    /**
     * 
     */
    public java.util.Collection<Gene> findByOfficialName( java.lang.String officialName );

    /**
     * @param officialName
     * @return
     */
    public Collection<Gene> findByOfficialNameInexact( String officialName );

    /**
     * 
     */
    public ubic.gemma.model.genome.Gene findByOfficialSymbol( java.lang.String symbol,
            ubic.gemma.model.genome.Taxon taxon );

    /**
     * 
     */
    public java.util.Collection<Gene> findByOfficialSymbolInexact( java.lang.String officialSymbol );

    /**
     * Find the Genes closest to the given location. If the location is in a gene(s), they will be returned. Otherwise a
     * single gene closest to the location will be returned, except in the case of ties in which more than one will be
     * returned.
     * 
     * @param physicalLocation
     * @param useStrand if true, the nearest Gene on the same strand will be found. Otherwise the nearest gene on either
     *        strand will be returned.
     * @return
     */
    public RelativeLocationData findNearest( PhysicalLocation physicalLocation, boolean useStrand );

    /**
     * 
     */
    public ubic.gemma.model.genome.Gene findOrCreate( ubic.gemma.model.genome.Gene gene );

    /**
     * Function to get coexpressed genes given a set of genes and a collection of expressionExperiments. The return
     * value is a Map of CoexpressionCollectionValueObjects.
     * 
     * @param genes
     * @param ees
     * @param stringency
     * @param interGeneOnly if true, only links among the query genes will be returned. This is ingored if only a single
     *        gene is entered
     * @return
     */
    public Map<Gene, CoexpressionCollectionValueObject> getCoexpressedGenes(
            Collection<ubic.gemma.model.genome.Gene> genes, java.util.Collection<? extends BioAssaySet> ees,
            java.lang.Integer stringency, boolean interGeneOnly );

    /**
     * <p>
     * Function to get coexpressed genes given a gene and a collection of expressionExperiments. The return value is a
     * CoexpressionCollectionValueObject.
     * </p>
     */
    public CoexpressionCollectionValueObject getCoexpressedGenes( ubic.gemma.model.genome.Gene gene,
            java.util.Collection<? extends BioAssaySet> ees, java.lang.Integer stringency );

    /**
     * 
     */
    public long getCompositeSequenceCountById( long id );

    /**
     * 
     */
    public Collection<CompositeSequence> getCompositeSequences( Gene gene,
            ArrayDesign arrayDesign );

    /**
     * 
     */
    public Collection<CompositeSequence> getCompositeSequencesById( long id );

    /**
     * Get precomputed node degree based on all available data sets.
     * 
     * @param genes
     * @return
     */
    public Map<Gene, GeneCoexpressionNodeDegree> getGeneCoexpressionNodeDegree( Collection<Gene> genes );

    /**
     * Get aggregated node degree based on a selected set of data sets. Likely to be slow.
     * 
     * @param genes
     * @param ees
     * @return
     */
    @Deprecated
    public Map<Gene, Double> getGeneCoexpressionNodeDegree( Collection<Gene> genes,
            Collection<? extends BioAssaySet> ees );

    /**
     * Get precomputed node degree based on all available data sets.
     * 
     * @param gene
     * @return
     */
    public GeneCoexpressionNodeDegree getGeneCoexpressionNodeDegree( Gene gene );

    /**
     * Get node degree information for genes for specified experiments. Likely to be slow, used for populating the data
     * for the faster methods.
     * 
     * @param gene
     * @param ees
     * @return map of bioassay set to the node degree computed for the gene at the probe level. The value returned is
     *         the node degree rank.
     */
    public Map<BioAssaySet, Double> getGeneCoexpressionNodeDegree( Gene gene, Collection<? extends BioAssaySet> ees );

    /**
     * returns a collections of genes that match the given taxon
     */
    public java.util.Collection<Gene> getGenesByTaxon( ubic.gemma.model.genome.Taxon taxon );

    /**
     * Returns a collection of genes that are actually MicroRNA for a given taxon
     */
    public java.util.Collection<Gene> getMicroRnaByTaxon( ubic.gemma.model.genome.Taxon taxon );

    /**
     * Returns a collection of genes for the specified taxon (not all genes, ie not probe aligned regions and predicted
     * genes)
     */
    public java.util.Collection<Gene> loadKnownGenes( ubic.gemma.model.genome.Taxon taxon );

    public Collection<Gene> loadKnownGenesWithProducts( Taxon taxon );

    /**
     * @param ids
     * @return
     */
    public Collection<Gene> loadThawed( Collection<Long> ids );

    /**
     * 
     */
    public Gene thaw( Gene gene );

    /**
     * Only thaw the Aliases, very light version
     * 
     * @param gene
     */
    public Gene thawAliases( Gene gene );

    /**
     * @param gene
     * @return
     */
    public Gene thawLite( Gene gene );

    /**
     * @param genes
     * @return
     * @see loadThawed, which you should use instead of this method if you know you want to load thawed objects.
     */
    public Collection<Gene> thawLite( java.util.Collection<Gene> genes );

    public Collection<Gene> findByPhysicalLocation( PhysicalLocation location );

}
