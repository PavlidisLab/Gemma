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
package ubic.gemma.core.genome.gene.service;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.PhysicalLocationValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledService;
import ubic.gemma.persistence.service.FilteringVoEnabledService;

import javax.annotation.CheckReturnValue;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author kelsey
 */
@SuppressWarnings("unused") // Possible external use
public interface GeneService extends FilteringVoEnabledService<Gene, GeneValueObject> {

    @Override
    @Secured({ "GROUP_ADMIN" })
    Gene create( Gene gene );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( Gene gene );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void update( Collection<Gene> genes );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void update( Gene gene );

    /**
     * Find all genes at a physical location. All overlapping genes are returned. The location can be a point or a
     * region. If strand is non-null, only genes on the same strand are returned.
     *
     * @param physicalLocation physical location of gene
     * @return all genes at given location
     */
    Collection<Gene> find( PhysicalLocation physicalLocation );

    Gene findByAccession( String accession, ExternalDatabase source );

    Collection<Gene> findByAlias( String search );

    /**
     * Searches for a gene based on its ensembl ID.
     * There is a small amount of genes in our database that have duplicate ensembl IDs. These genes are believed
     * to be somehow unusable anyway, so we ignore those cases at the moment - Aug. 11th 2017.
     *
     * @param exactString the ensembl ID that the gene will be looked up by.
     * @return a Gene with the given Ensembl ID.
     */
    Gene findByEnsemblId( String exactString );

    Gene findByNCBIId( Integer accession );

    GeneValueObject findByNCBIIdValueObject( Integer accession );

    /**
     * Quickly load exact matches.
     *
     * @param ncbiIds ncbi IDs
     * @return map of NCBI Id to the gene.
     */
    Map<Integer, GeneValueObject> findByNcbiIds( Collection<Integer> ncbiIds );

    @SuppressWarnings("UnusedReturnValue")
        // Consistency
    Collection<Gene> findByOfficialName( String officialName );

    Collection<Gene> findByOfficialNameInexact( String officialName );

    Collection<Gene> findByOfficialSymbol( String officialSymbol );

    Gene findByOfficialSymbol( String symbol, Taxon taxon );

    Collection<Gene> findByOfficialSymbolInexact( String officialSymbol );

    /**
     * Quickly load exact matches.
     *
     * @param query   query
     * @param taxonId taxon id
     * @return map of lower-cased gene symbol  to the gene. The actual query that led to the gene is not retained.
     */
    Map<String, GeneValueObject> findByOfficialSymbols( Collection<String> query, Long taxonId );

    Collection<AnnotationValueObject> findGOTerms( Long geneId );

    long getCompositeSequenceCountById( Long id );

    /**
     * Returns a list of compositeSequences associated with the given gene and array design
     *
     * @param gene        gene
     * @param arrayDesign platform
     * @return composite sequences
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_ARRAYDESIGN_COLLECTION_READ" })
    Collection<CompositeSequence> getCompositeSequences( Gene gene, ArrayDesign arrayDesign );

    /**
     * @param id Gemma gene id
     * @return Return probes for a given gene id.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_ARRAYDESIGN_COLLECTION_READ" })
    Collection<CompositeSequence> getCompositeSequencesById( Long id );

    /**
     * Gets all the genes for a given taxon
     *
     * @param taxon taxon
     * @return genes for given taxon
     */
    Collection<Gene> getGenesByTaxon( Taxon taxon );

    List<PhysicalLocationValueObject> getPhysicalLocationsValueObjects( Gene gene );

    /**
     * @param geneId gene id
     * @return empty collection if no products
     */
    Collection<GeneProductValueObject> getProducts( Long geneId );

    /**
     * @param taxon taxon
     * @return a collection of genes for the specified taxon
     */
    Collection<Gene> loadAll( Taxon taxon );

    /**
     * Returns a detailVO for a geneDd This method may be unnecessary now that we have put all the logic into the
     * GeneService
     *
     * @param id The gene id
     * @return GeneDetailsValueObject a representation of that gene
     */
    GeneValueObject loadFullyPopulatedValueObject( Long id );

    GeneValueObject loadGenePhenotypes( Long geneId );

    /**
     * @param taxon taxon
     * @return all the microRNAs for a given taxon. Note query could be slow or inexact due to use of wild card searching
     * of the genes description
     */
    Collection<Gene> loadMicroRNAs( Taxon taxon );

    /**
     * Load with objects already thawed.
     *
     * @param ids ids
     * @return pre-thawed genes
     */
    Collection<Gene> loadThawed( Collection<Long> ids );

    Collection<Gene> loadThawedLiter( Collection<Long> ids );

    GeneValueObject loadValueObjectById( Long id );

    Collection<GeneValueObject> loadValueObjectsByIds( Collection<Long> ids );

    Collection<GeneValueObject> loadValueObjectsByIdsLiter( Collection<Long> ids );

    /**
     * @param gene gene
     * @return thaw the Aliases, very light version
     */
    @Deprecated
    @CheckReturnValue
    Gene thawAliases( Gene gene );

    @Deprecated
    @CheckReturnValue
    Collection<Gene> thawLite( Collection<Gene> genes );

    @Deprecated
    @CheckReturnValue
    Gene thawLite( Gene gene );

    @Deprecated
    @CheckReturnValue
    Gene thawLiter( Gene gene );

    Collection<GeneValueObject> searchGenes( String query, Long taxonId );
}
