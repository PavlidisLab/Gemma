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

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.OntologyService;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.gemma.core.ontology.providers.GeneOntologyServiceImpl.GOAspect;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneOntologyTermValueObject;
import ubic.gemma.model.genome.Taxon;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
     * Search by inexact string
     *
     * @param  queryString query string
     * @return ontology terms
     */
    Collection<OntologyTerm> findTerm( String queryString ) throws OntologySearchException;

    /**
     * @param  entry entry
     * @return children, NOT including part-of relations.
     */
    Collection<OntologyTerm> getAllChildren( OntologyTerm entry );

    Collection<OntologyTerm> getAllChildren( OntologyTerm entry, boolean includePartOf );

    /**
     * @param  entries NOTE terms that are in this collection are NOT explicitly included; however, some of them may be
     *                 included incidentally if they are parents of other terms in the collection.
     * @return ontology terms
     */
    Collection<OntologyTerm> getAllParents( Collection<OntologyTerm> entries );

    Collection<OntologyTerm> getAllParents( Collection<OntologyTerm> entries, boolean includePartOf );

    /**
     * Return all the parents of GO OntologyEntry, up to the root, as well as terms that this has a restriction
     * relationship with (part_of). NOTE: the term itself is NOT included; nor is the root.
     *
     * @param  entry entry
     * @return parents (excluding the root)
     */
    Collection<OntologyTerm> getAllParents( OntologyTerm entry );

    Collection<OntologyTerm> getAllParents( OntologyTerm entry, boolean includePartOf );

    /**
     * Returns the immediate children of the given entry
     *
     * @param  entry entry
     * @return children of entry, or null if there are no children (or if entry is null)
     */

    Collection<OntologyTerm> getChildren( OntologyTerm entry );

    Collection<OntologyTerm> getChildren( OntologyTerm entry, boolean includePartOf );

    /**
     * @param  taxon taxon
     * @param  goId  go id
     * @return Collection of all genes in the given taxon that are annotated with the given id, including its
     *               child
     *               terms in the hierarchy.
     */
    Collection<Gene> getGenes( String goId, Taxon taxon );

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
     * @return ontology terms
     */
    Collection<OntologyTerm> getGOTerms( Gene gene, boolean includePartOf );

    /**
     * Get all GO terms for a gene, including parents of terms via is-a relationships; and optionally also parents via
     * part-of relationships.
     *
     * @param  gene          gene
     * @param  includePartOf include part of
     * @param  goAspect      limit only to the given aspect (pass null to use all)
     * @return ontology terms
     */
    Collection<OntologyTerm> getGOTerms( Gene gene, boolean includePartOf, GOAspect goAspect );

    Collection<OntologyTerm> getGOTerms( Long geneId );

    /**
     * Return the immediate parent(s) of the given entry. The root node is never returned.
     *
     * @param  entry entry
     * @return collection, because entries can have multiple parents. (only allroot is excluded)
     */
    Collection<OntologyTerm> getParents( OntologyTerm entry );

    /**
     * @param  entry         entry
     * @param  includePartOf include part of
     * @return ontology terms
     */
    Collection<OntologyTerm> getParents( OntologyTerm entry, boolean includePartOf );

    /**
     *
     * @param  uri of the term
     * @return term if found or null otherwise.
     */
    OntologyTerm getTerm( String uri );

    GOAspect getTermAspect( Characteristic goId );

    /**
     *
     * @param  goId GO ID e.g. GO:0038128 (not URI)
     * @return aspect if found, null otherwise
     */
    GOAspect getTermAspect( String goId );

    /**
     * Return a definition for a GO Id.
     *
     * @param  goId e.g. GO:0094491
     * @return Definition or null if there is no definition.
     */
    String getTermDefinition( String goId );

    /**
     * @param  value e.g. GO:0038128
     * @return term or null if not found
     */
    OntologyTerm getTermForId( String value );

    /**
     * Return human-readable term ("protein kinase") for a GO Id.
     *
     * @param  goId go id
     * @return term name
     */
    String getTermName( String goId );

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

    /**
     * Determines if one ontology entry is a child (direct or otherwise) of a given parent term.
     *
     * @param  parent         parent
     * @param  potentialChild potential child
     * @return is a child of
     */
    boolean isAChildOf( OntologyTerm parent, OntologyTerm potentialChild );

    /**
     * Determines if one ontology entry is a parent (direct or otherwise) of a given child term.
     *
     * @param  child           child
     * @param  potentialParent potential parent
     * @return True if potentialParent is in the parent graph of the child; false otherwise.
     */
    boolean isAParentOf( OntologyTerm child, OntologyTerm potentialParent );

    boolean isBiologicalProcess( OntologyTerm term );

    /**
     * Primarily here for testing, to recover memory.
     */
    void shutDown();
}