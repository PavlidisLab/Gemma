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
package ubic.gemma.persistence.service.genome.gene;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.core.search.SearchException;
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
import ubic.gemma.persistence.service.FilteringVoEnabledService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AdminEditableBaseService;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author kelsey
 */
@SuppressWarnings("unused") // Possible external use
public interface GeneService extends AdminEditableBaseService<Gene>, FilteringVoEnabledService<Gene, GeneValueObject> {

    @Secured({ "GROUP_ADMIN" })
    int removeAll();

    /**
     * Find all genes at a physical location. All overlapping genes are returned. The location can be a point or a
     * region. If strand is non-null, only genes on the same strand are returned.
     *
     * @param physicalLocation physical location of gene
     * @return all genes at given location
     */
    Collection<Gene> find( PhysicalLocation physicalLocation );

    @Nullable
    Gene findByAccession( String accession, @Nullable ExternalDatabase source );

    Collection<Gene> findByAlias( String search );

    /**
     * Searches for a gene based on its ensembl ID.
     * There is a small amount of genes in our database that have duplicate ensembl IDs. These genes are believed
     * to be somehow unusable anyway, so we ignore those cases at the moment - Aug. 11th 2017.
     *
     * @param exactString the ensembl ID that the gene will be looked up by.
     * @return a Gene with the given Ensembl ID.
     */
    @Nullable
    Gene findByEnsemblId( String exactString );

    @Nullable
    Gene findByNCBIId( Integer accession );

    @Nullable
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

    @Nullable
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

    /**
     * @see ubic.gemma.persistence.service.genome.GeneDao#getCompositeSequenceCount(Gene, boolean)
     */
    long getCompositeSequenceCount( Gene gene, boolean includeDummyProducts );

    /**
     * @see ubic.gemma.persistence.service.genome.GeneDao#getCompositeSequenceCountById(long, boolean)
     */
    long getCompositeSequenceCountById( Long id, boolean includeDummyProducts );

    /**
     * @see ubic.gemma.persistence.service.genome.GeneDao#getCompositeSequences(Gene, ArrayDesign, boolean)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COMPOSITE_SEQUENCE_COLLECTION_READ" })
    Collection<CompositeSequence> getCompositeSequences( Gene gene, ArrayDesign arrayDesign, boolean includeDummyProducts );

    /**
     * @see ubic.gemma.persistence.service.genome.GeneDao#getCompositeSequencesById(long, boolean)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COMPOSITE_SEQUENCE_COLLECTION_READ" })
    Collection<CompositeSequence> getCompositeSequences( Gene gene, boolean includeDummyProducts );

    /**
     * @see ubic.gemma.persistence.service.genome.GeneDao#getCompositeSequencesById(long, boolean)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COMPOSITE_SEQUENCE_COLLECTION_READ" })
    Collection<CompositeSequence> getCompositeSequencesById( Long geneId, boolean includeDummyProducts );

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
    @Nullable
    GeneValueObject loadFullyPopulatedValueObject( Long id );

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

    Collection<GeneValueObject> loadValueObjectsByIdsLiter( Collection<Long> ids );

    Gene thaw( Gene gene );

    /**
     * @param gene gene
     * @return thaw the Aliases, very light version
     */
    Gene thawAliases( Gene gene );

    Collection<Gene> thawLite( Collection<Gene> genes );

    Gene thawLite( Gene gene );

    Gene thawLiter( Gene gene );

    Collection<GeneValueObject> searchGenes( String query, @Nullable Long taxonId ) throws SearchException;
}
