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
package ubic.gemma.persistence.service.genome;

import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledDao;

import java.util.Collection;
import java.util.Map;

/**
 * @see Gene
 */
public interface GeneDao extends BaseVoEnabledDao<Gene, GeneValueObject> {

    /**
     * Find all genes at a physical location. All overlapping genes are returned. The location can be a point or a
     * region. If strand is non-null, only genes on the same strand are returned.
     */
    Collection<Gene> find( PhysicalLocation physicalLocation );

    Gene findByAccession( String accession, ExternalDatabase source );

    /**
     * Locate genes that match the given alias string
     */
    Collection<Gene> findByAlias( String search );

    Collection<? extends Gene> findByEnsemblId( String exactString );

    Gene findByNcbiId( Integer accession );

    /**
     * <p>
     * Finder based on the official name.
     * </p>
     */
    Collection<Gene> findByOfficialSymbol( String officialSymbol );

    Collection<Gene> findByOfficialName( String officialName );

    Collection<Gene> findByOfficialNameInexact( String officialName );

    Gene findByOfficialSymbol( String symbol, Taxon taxon );

    Collection<Gene> findByOfficialSymbolInexact( String officialSymbol );

    /**
     * Quickly load exact matches.
     *
     * @return map of gene symbol (tolowercase()) to the gene. The actual query that led to the gene is not retained.
     */
    Map<String, Gene> findByOfficialSymbols( Collection<String> query, Long taxonId );

    /**
     * Quickly load exact matches.
     *
     * @return map of NCBI Id to the gene.
     */
    Map<Integer, Gene> findByNcbiIds( Collection<Integer> ncbiIds );

    Collection<Gene> findByPhysicalLocation( PhysicalLocation location );

    /**
     * Find the Genes closest to the given location. If the location is in a gene(s), they will be returned. Otherwise a
     * single gene closest to the location will be returned, except in the case of ties in which more than one will be
     * returned.
     *
     * @param useStrand if true, the nearest Gene on the same strand will be found. Otherwise the nearest gene on either
     *                  strand will be returned.
     */
    RelativeLocationData findNearest( PhysicalLocation physicalLocation, boolean useStrand );

    /**
     * @return how many platform elements (e.g. probes) represent this gene, totalled up over all platforms.
     */
    long getCompositeSequenceCountById( long id );

    Collection<CompositeSequence> getCompositeSequences( Gene gene, ArrayDesign arrayDesign );

    Collection<CompositeSequence> getCompositeSequencesById( long id );

    /**
     * returns a collections of genes that match the given taxon
     */
    Collection<Gene> getGenesByTaxon( Taxon taxon );

    /**
     * Returns a collection of genes that are actually MicroRNA for a given taxon
     */
    Collection<Gene> getMicroRnaByTaxon( Taxon taxon );

    /**
     * @return how many platforms have a representation of this gene
     */
    int getPlatformCountById( Long id );

    /**
     * Returns a collection of genes for the specified taxon (not all genes, ie not probe aligned regions and predicted
     * genes)
     */
    Collection<Gene> loadKnownGenes( Taxon taxon );

    /**
     * Only thaw the Aliases, very light version
     */
    void thawAliases( Gene gene );

    void thawLite( Collection<Gene> genes );

    void thawLite( Gene gene );

    void thawLiter( Gene gene );

    Collection<Gene> loadThawed( Collection<Long> ids );

    Collection<Gene> loadThawedLiter( Collection<Long> ids );
}
