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
import ubic.gemma.persistence.service.FilteringVoEnabledDao;
import ubic.gemma.persistence.util.ObjectFilter;

import java.util.Collection;
import java.util.Map;

/**
 * @see Gene
 */
public interface GeneDao extends FilteringVoEnabledDao<Gene, GeneValueObject> {

    String OBJECT_ALIAS = "gene";

    /**
     * Find all genes at a physical location. All overlapping genes are returned. The location can be a point or a
     * region. If strand is non-null, only genes on the same strand are returned.
     *
     * @param physicalLocation physical location
     * @return found genes
     */
    Collection<Gene> find( PhysicalLocation physicalLocation );

    Gene findByAccession( String accession, ExternalDatabase source );

    /**
     * Locate genes that match the given alias string
     *
     * @param search search string
     * @return found genes
     */
    Collection<Gene> findByAlias( String search );

    Gene findByEnsemblId( String exactString );

    Gene findByNcbiId( Integer accession );

    Collection<Gene> findByOfficialSymbol( String officialSymbol );

    Collection<Gene> findByOfficialName( String officialName );

    Collection<Gene> findByOfficialNameInexact( String officialName );

    Gene findByOfficialSymbol( String symbol, Taxon taxon );

    Collection<Gene> findByOfficialSymbolInexact( String officialSymbol );

    /**
     * Quickly load exact matches.
     *
     * @param query   query
     * @param taxonId taxon id
     * @return map of gene symbol (toLowerCase()) to the gene. The actual query that led to the gene is not retained.
     */
    Map<String, Gene> findByOfficialSymbols( Collection<String> query, Long taxonId );

    /**
     * Quickly load exact matches.
     *
     * @param ncbiIds ncbi ids
     * @return map of NCBI Id to the gene.
     */
    Map<Integer, Gene> findByNcbiIds( Collection<Integer> ncbiIds );

    @Deprecated
    Collection<Gene> findByPhysicalLocation( PhysicalLocation location );

    /**
     * @param id id
     * @return how many platform elements (e.g. probes) represent this gene, totalled up over all platforms.
     */
    long getCompositeSequenceCountById( long id );

    Collection<CompositeSequence> getCompositeSequences( Gene gene, ArrayDesign arrayDesign );

    Collection<CompositeSequence> getCompositeSequencesById( long id );

    /**
     * @param taxon taxon
     * @return a collections of genes that match the given taxon
     */
    Collection<Gene> getGenesByTaxon( Taxon taxon );

    /**
     * @param taxon taxon
     * @return a collection of genes that are actually MicroRNA for a given taxon
     */
    Collection<Gene> getMicroRnaByTaxon( Taxon taxon );

    /**
     * @param id id
     * @return how many platforms have a representation of this gene
     */
    int getPlatformCountById( Long id );

    /**
     * @param taxon taxon
     * @return a collection of genes for the specified taxon (not all genes, ie not probe aligned regions and predicted
     * genes)
     */
    Collection<Gene> loadKnownGenes( Taxon taxon );

    Collection<Gene> loadThawed( Collection<Long> ids );

    Collection<Gene> loadThawedLiter( Collection<Long> ids );

    void thawAliases( Gene gene );

    void thawLite( Collection<Gene> genes );

    void thawLite( Gene gene );

    void thawLiter( Gene gene );
}
