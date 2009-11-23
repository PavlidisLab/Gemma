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
package ubic.gemma.model.genome.gene;

import java.util.Collection;
import java.util.Map;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.analysis.expression.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.PredictedGene;
import ubic.gemma.model.genome.ProbeAlignedRegion;
import ubic.gemma.model.genome.Qtl;
import ubic.gemma.model.genome.RelativeLocationData;

/**
 * @author kelsey
 * @version $Id$
 */
public interface GeneService {

    public java.lang.Integer countAll();

    @Secured( { "GROUP_ADMIN" })
    public java.util.Collection<Gene> create( java.util.Collection<Gene> genes );

    @Secured( { "GROUP_ADMIN" })
    public ubic.gemma.model.genome.Gene create( ubic.gemma.model.genome.Gene gene );

    /**
     * Find all genes at a physical location. All overlapping genes are returned. The location can be a point or a
     * region. If strand is non-null, only genes on the same strand are returned.
     * 
     * @param physicalLocation
     * @return
     */
    public Collection<Gene> find( PhysicalLocation physicalLocation );

    public ubic.gemma.model.genome.Gene find( ubic.gemma.model.genome.Gene gene );

    public java.util.Collection<Qtl> findAllQtlsByPhysicalMapLocation(
            ubic.gemma.model.genome.PhysicalLocation physicalMapLocation );

    public ubic.gemma.model.genome.Gene findByAccession( java.lang.String accession,
            ubic.gemma.model.common.description.ExternalDatabase source );

    public java.util.Collection<Gene> findByAlias( java.lang.String search );

    public ubic.gemma.model.genome.Gene findByNCBIId( java.lang.String accession );

    public java.util.Collection<Gene> findByOfficialName( java.lang.String officialName );

    public java.util.Collection<Gene> findByOfficialSymbol( java.lang.String officialSymbol );

    public ubic.gemma.model.genome.Gene findByOfficialSymbol( java.lang.String symbol,
            ubic.gemma.model.genome.Taxon taxon );

    public java.util.Collection<Gene> findByOfficialSymbolInexact( java.lang.String officialSymbol );

    /**
     * Find the gene(s) nearest to the location.
     * 
     * @param physicalLocation
     * @param useStrand if true, the nearest Gene on the same strand will be found. Otherwise the nearest gene on either
     *        strand will be returned.
     * @return
     */
    public RelativeLocationData findNearest( PhysicalLocation physicalLocation, boolean useStrand );

    @Secured( { "GROUP_ADMIN" })
    public ubic.gemma.model.genome.Gene findOrCreate( ubic.gemma.model.genome.Gene gene );

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
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Map<Gene, CoexpressionCollectionValueObject> getCoexpressedGenes( Collection<Gene> genes,
            Collection<? extends BioAssaySet> ees, Integer stringency, boolean knownGenesOnly, boolean interGenesOnly );

    /**
     * <p>
     * Function to get coexpressed genes given a gene and a collection of expressionExperiments. Returns the value
     * object:: CoexpressionCollectionValueObject
     * </p>
     * 
     * @param gene
     * @param ees
     * @param stringency
     * @param knownGenesOnly
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public CoexpressionCollectionValueObject getCoexpressedGenes( ubic.gemma.model.genome.Gene gene,
            java.util.Collection<? extends BioAssaySet> ees, java.lang.Integer stringency, boolean knownGenesOnly );

    /**
     * <p>
     * Returns a Collection of Genes. Not ProbeAlignedRegions, Not PredictedGenes, just straight up known genes that
     * didn't have any specificty problems (ie all the probes were clean).
     * </p>
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public java.util.Collection<Gene> getCoexpressedKnownGenes( ubic.gemma.model.genome.Gene gene,
            java.util.Collection<? extends BioAssaySet> ees, java.lang.Integer stringency );

    public long getCompositeSequenceCountById( java.lang.Long id );

    /**
     * <p>
     * Returns a list of compositeSequences associated with the given gene and array design
     * </p>
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public java.util.Collection<CompositeSequence> getCompositeSequences( ubic.gemma.model.genome.Gene gene,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * @param id gemma gene id
     * @return Return probes for a given gene id.
     */
    public java.util.Collection<CompositeSequence> getCompositeSequencesById( java.lang.Long id );

    /**
     * <p>
     * Gets all the genes for a given taxon
     * </p>
     */
    public java.util.Collection<Gene> getGenesByTaxon( ubic.gemma.model.genome.Taxon taxon );

    /**
     * <p>
     * Gets all the microRNA for a given taxon. Note query could be slow or inexact due to use of wild card searching of
     * the genes description
     * </p>
     */
    public java.util.Collection getMicroRnaByTaxon( ubic.gemma.model.genome.Taxon taxon );

    public ubic.gemma.model.genome.Gene load( long id );

    public java.util.Collection<Gene> loadAll();

    /**
     * <p>
     * Returns a collection of geneImpls for the specified taxon. Ie not probe aligned regions and predicted genes
     * </p>
     */
    public java.util.Collection<Gene> loadKnownGenes( ubic.gemma.model.genome.Taxon taxon );

    /**
     * <p>
     * load all genes specified by the given ids.
     * </p>
     */
    public java.util.Collection<Gene> loadMultiple( java.util.Collection<Long> ids );

    /**
     * <p>
     * Returns a collection of Predicted Genes for the specified taxon
     * </p>
     */
    public java.util.Collection<PredictedGene> loadPredictedGenes( ubic.gemma.model.genome.Taxon taxon );

    /**
     * <p>
     * Returns a collection of all ProbeAlignedRegion's for the specfied taxon
     * </p>
     */
    public java.util.Collection<ProbeAlignedRegion> loadProbeAlignedRegions( ubic.gemma.model.genome.Taxon taxon );

    @Secured( { "GROUP_ADMIN" })
    public void remove( Gene gene );

    @Secured( { "GROUP_ADMIN" })
    public void remove( java.util.Collection<Gene> genes );

    public void thaw( ubic.gemma.model.genome.Gene gene );

    /**
     * @param gene
     */
    public void thawLite( Gene gene );

    /**
     * 
     */
    public void thawLite( java.util.Collection<Gene> genes );

    @Secured( { "GROUP_ADMIN" })
    public void update( ubic.gemma.model.genome.Gene gene );

}
