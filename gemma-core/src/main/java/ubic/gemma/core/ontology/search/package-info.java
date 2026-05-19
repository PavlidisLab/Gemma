/**
 * In-Gemma ontology full-text search subsystem.
 *
 * <p>This package originated as a port of baseCode 1.1.34's
 * {@code ubic.basecode.ontology.search} + {@code ubic.basecode.ontology.jena}
 * search code, pulled into Gemma during Phase 3 of the renovations so the
 * ontology indexer could move off Lucene 3 (baseCode pre-renovations) and onto
 * Jena 4.10's {@code jena-text} module (Lucene 9.x). See
 * {@code SEARCH_RECCE.md} Section 6 for the rationale and the deferred
 * refactor-out checkpoint.
 *
 * <p>The port is API-divergent from baseCode on purpose: baseCode keeps the
 * {@link ubic.basecode.ontology.providers.OntologyService#findTerm(String, int)}
 * machinery anchored on a package-private {@code SearchIndex} owned by
 * {@code AbstractOntologyService} (the {@code OntModel} is never exposed).
 * The Gemma-side {@link ubic.gemma.core.ontology.search.OntologySearchService}
 * therefore operates on a {@code jena-text} {@code TextDataset} that wraps the
 * unified-ontology TDB managed by
 * {@link ubic.gemma.core.ontology.OntologyConfig#unifiedOntologyService} —
 * separate Jena view of the same TDB files, no dependence on baseCode's
 * private state.
 *
 * @see ubic.gemma.core.ontology.search.OntologySearchService
 * @see ubic.gemma.core.ontology.search.JenaTextOntologySearchService
 */
package ubic.gemma.core.ontology.search;
