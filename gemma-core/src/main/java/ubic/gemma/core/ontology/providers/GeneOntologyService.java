/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.ontology.providers;

import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyResource;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.OntologyService;
import ubic.gemma.core.ontology.providers.GeneOntologyServiceImpl.GOAspect;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneOntologyTermValueObject;
import ubic.gemma.model.genome.Taxon;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author paul
 */
@SuppressWarnings("unused") // Possible external use
public interface GeneOntologyService extends OntologyService {

    String BASE_GO_URI = "http://purl.obolibrary.org/obo/";

    /**
     * <p>
     * Given a query Gene, and a collection of gene ids calculates the go term overlap for each pair of queryGene and
     * gene in the given collection. Returns a Map&lt;Gene,Collection&lt;OntologyEntries&gt;&gt;. The key is the gene
     * (from the
     * [queryGene,gene] pair) and the values are a collection of the overlapping ontology entries.
     * </p>
     *
     * @param  queryGene query gene
     * @param  geneIds   gene ids
     * @return map of gene ids to collections of ontologyTerms. This will always be populated but collection
     *                   values
     *                   will be empty when there is no overlap.
     */
    Map<Long, Collection<OntologyTerm>> calculateGoTermOverlap( Gene queryGene, Collection<Long> geneIds );

    /**
     * @param  queryGene1 query gene 1
     * @param  queryGene2 query gene 2
     * @return Collection&gt;OntologyEntries&lt;s
     */
    Collection<OntologyTerm> calculateGoTermOverlap( Gene queryGene1, Gene queryGene2 );

    Map<Long, Collection<OntologyTerm>> calculateGoTermOverlap( Long queryGene, Collection<Long> geneIds );

    Collection<OntologyTerm> computeOverlap( Collection<OntologyTerm> masterOntos,
            Collection<OntologyTerm> comparisonOntos );

    /**
     * @param  entries NOTE terms that are in this collection are NOT explicitly included; however, some of them may be
     *                 included incidentally if they are parents of other terms in the collection.
     * @return ontology terms
     */
    Set<OntologyTerm> getAllParents( Collection<OntologyTerm> entries );

    Set<OntologyTerm> getAllParents( Collection<OntologyTerm> entries, boolean includePartOf );

    /**
     * Retrieve a GO resource by ID or URI.
     */
    @Nullable
    @Override
    OntologyResource getResource( String goId );

    /**
     * Retrieve a GO term by ID or URI.
     */
    @Nullable
    @Override
    OntologyTerm getTerm( String goId );

    /**
     * Retrieve a GO individuals by ID or URI.
     */
    @Override
    Collection<OntologyIndividual> getTermIndividuals( String goId );

    /**
     * Find genes for a given of GO term.
     * @return Collection of all genes in the given taxon that are annotated with the given id, including its
     *         child terms in the hierarchy, or null if the GO term is not found
     */
    Collection<Gene> getGenes( OntologyTerm goTerm, @Nullable Taxon taxon );

    /**
     * Find genes for a given GO term by GO ID.
     * @param goId a GO ID (i.e. GO:000001) or URI
     * @see #getGenes(OntologyTerm, Taxon)
     */
    Collection<Gene> getGenes( String goId, @Nullable Taxon taxon );

    /**
     * @param  gene Take a gene and return a set of all GO terms including the parents of each GO term
     * @return ontology terms
     */
    Collection<OntologyTerm> getGOTerms( Gene gene );

    /**
     * Get all GO terms for a gene, including parents of terms via is-a relationships; and optionally also parents via
     * part-of relationships.
     *
     * @param  gene          gene
     * @param  includePartOf include part of
     * @param  goAspect      limit only to the given aspect (pass null to use all)
     * @return ontology terms
     */
    Collection<OntologyTerm> getGOTerms( Gene gene, boolean includePartOf, @Nullable GOAspect goAspect );

    @Nullable
    GOAspect getTermAspect( Characteristic goId );

    /**
     *
     * @param  goId GO ID e.g. GO:0038128 or URI
     * @return aspect if found, null otherwise
     */
    @Nullable
    GOAspect getTermAspect( String goId );

    /**
     * Return a definition for a GO ID.
     *
     * @param  goId e.g. GO:0094491 or URI
     * @return Definition or null if there is no definition.
     */
    @Nullable
    String getTermDefinition( String goId );

    /**
     * Converts the given Ontology Term to a Gene Ontology Value Object.
     *
     * @param  term the term to be converted.
     * @return value object representing the given term.
     */
    GeneOntologyTermValueObject getValueObject( OntologyTerm term );

    /**
     * Converts the given collection of Ontology Terms to Gene Ontology Value Objects.
     *
     * @param  terms the terms to be converted.
     * @return collection of value objects representing the given terms.
     */
    Collection<GeneOntologyTermValueObject> getValueObjects( Collection<OntologyTerm> terms );

    /**
     * Returns GO Terms VOs for the given Gene.
     *
     * @param  gene the Gene to retrieve GO Terms for and convert them to VOs.
     * @return Gene Ontology VOs representing all GO Terms associated with the given gene.
     */
    List<GeneOntologyTermValueObject> getValueObjects( Gene gene );

    boolean isBiologicalProcess( OntologyTerm term );

    /**
     * Primarily here for testing, to recover memory.
     */
    void clearCaches();
}