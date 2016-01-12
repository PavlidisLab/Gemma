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

import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.BaseDao;

/**
 * @see Gene
 */
public interface GeneDao extends BaseDao<Gene> {

    public Integer countAll();

    /**
     * 
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
     * 
     */
    public Gene findByAccession( String accession, ExternalDatabase source );

    /**
     * Locate genes that match the given alias string
     */
    public Collection<Gene> findByAlias( String search );

    /**
     * @param exactString
     * @return
     */
    public Collection<? extends Gene> findByEnsemblId( String exactString );

    /**
     * @param accession
     * @return
     */
    public Gene findByNcbiId( Integer accession );

    /**
     * <p>
     * Finder based on the official name.
     * </p>
     */
    public Collection<Gene> findByOfficalSymbol( String officialSymbol );

    /**
     * 
     */
    public Collection<Gene> findByOfficialName( String officialName );

    /**
     * @param officialName
     * @return
     */
    public Collection<Gene> findByOfficialNameInexact( String officialName );

    /**
     * 
     */
    public Gene findByOfficialSymbol( String symbol, Taxon taxon );

    /**
     * 
     */
    public Collection<Gene> findByOfficialSymbolInexact( String officialSymbol );

    /**
     * Quickly load exact matches.
     * 
     * @param query
     * @param taxonId
     * @return map of gene symbol (tolowercase()) to the gene. The actual query that led to the gene is not retained.
     */
    public Map<String, Gene> findByOfficialSymbols( Collection<String> query, Long taxonId );

    public Collection<Gene> findByPhysicalLocation( PhysicalLocation location );

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
     * @param id
     * @return how many platform elements (e.g. probes) represent this gene, totalled up over all platforms.
     */
    public long getCompositeSequenceCountById( long id );

    /**
     * 
     */
    public Collection<CompositeSequence> getCompositeSequences( Gene gene, ArrayDesign arrayDesign );

    /**
     * 
     */
    public Collection<CompositeSequence> getCompositeSequencesById( long id );

    /**
     * returns a collections of genes that match the given taxon
     */
    public Collection<Gene> getGenesByTaxon( Taxon taxon );

    /**
     * Returns a collection of genes that are actually MicroRNA for a given taxon
     */
    public Collection<Gene> getMicroRnaByTaxon( Taxon taxon );

    /**
     * @param id
     * @return how many platforms have a representation of this gene
     */
    public int getPlatformCountById( Long id );

    /**
     * Returns a collection of genes for the specified taxon (not all genes, ie not probe aligned regions and predicted
     * genes)
     */
    public Collection<Gene> loadKnownGenes( Taxon taxon );

    /**
     * @param ids
     * @return
     */
    public Collection<Gene> loadThawed( Collection<Long> ids );

    public Collection<Gene> loadThawedLiter( Collection<Long> ids );

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
     * @param genes
     * @return
     * @see loadThawed, which you should use instead of this method if you know you want to load thawed objects.
     */
    public Collection<Gene> thawLite( Collection<Gene> genes );

    /**
     * @param gene
     * @return
     */
    public Gene thawLite( Gene gene );

    public Gene thawLiter( Gene gene );
}
