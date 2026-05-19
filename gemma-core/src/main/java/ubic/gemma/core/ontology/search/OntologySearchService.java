/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.ontology.search;

import org.apache.jena.rdf.model.Resource;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Full-text search over ontology resources backed by Jena's
 * {@code jena-text} (Lucene 9) integration over a TDB dataset.
 *
 * <p><b>Origin.</b> This interface is a Gemma-internal restatement of the
 * search API that baseCode 1.1.34 exposed via
 * {@code ubic.basecode.ontology.jena.OntologyIndexer} +
 * {@code ubic.basecode.ontology.providers.OntologyService#findTerm(String, int)}.
 * baseCode's machinery was retired with the Lucene-3 strip
 * (baseCode commit {@code a7e7112}); the search code was pulled into Gemma
 * during Phase 3 search restoration (see {@code SEARCH_RECCE.md} Section 6)
 * rather than upgraded upstream, so we control the Lucene-9 / Jena-4.10
 * port from one repo for the duration of Phase 3. The Gemma-side API is
 * deliberately narrower: it returns {@link org.apache.jena.rdf.model.Resource}
 * handles into the dataset rather than baseCode's
 * {@code ubic.basecode.ontology.model.OntologyResource} wrappers, because the
 * search subsystem and the baseCode resource model are now decoupled (the
 * baseCode {@code OntModel} is package-private and unreachable from outside
 * baseCode).
 *
 * <p><b>Caller responsibility.</b> Callers translate the returned
 * {@link Result#getResource()} URIs into whatever domain object they need
 * (typically by consulting baseCode's per-ontology services for
 * label / parent / child resolution, or Gemma's
 * {@code CharacteristicService} for term-URI -&gt; experiment joins).
 *
 * <p>This service is search-only. Indexing is performed at TDB-write time
 * by Jena's {@code TextDocProducer} listener; callers do not invoke it
 * directly.
 *
 * @see JenaTextOntologySearchService
 */
public interface OntologySearchService {

    /**
     * Search the backing dataset for resources whose indexed properties
     * (label, synonyms, OBO ids, etc.) match the given Lucene query.
     *
     * <p>The query is forwarded to Jena's {@code text:query} property
     * function. Special characters that would confuse the Lucene query
     * parser should be escaped with {@link #escape(String)} before
     * invocation; passing a raw user query may throw or yield empty results.
     *
     * @param query      Lucene-syntax query string; must not be blank
     * @param maxResults maximum number of results to return; must be &gt; 0
     * @return ordered list of hits (descending score); empty if no match
     *         or the dataset is empty / disabled
     */
    List<Result> search( String query, int maxResults );

    /**
     * Escape Lucene metacharacters in a free-text query.
     *
     * <p>Equivalent to {@code org.apache.lucene.queryparser.classic.QueryParserBase.escape}
     * but reproduced here so callers do not need a direct dependency on
     * the Lucene query-parser module. Pre-strip Gemma routed through
     * {@code LuceneQueryUtils.escape} for the same purpose;
     * {@code OntologySearchSource} (when restored) routes its retry path
     * through this method.
     */
    String escape( String query );

    /**
     * Whether the underlying {@code jena-text} index is available and
     * non-empty. {@code false} typically means the TDB has not been
     * populated, the unified-ontology feature is disabled
     * ({@code gemma.ontology.unified.enabled=false}), or indexing has not
     * yet completed.
     */
    boolean isReady();

    /**
     * Lightweight result envelope. The caller is responsible for
     * translating {@link #getResource()} into a domain-level object.
     */
    final class Result {
        private final Resource resource;
        @Nullable
        private final String label;
        private final double score;

        public Result( Resource resource, @Nullable String label, double score ) {
            this.resource = resource;
            this.label = label;
            this.score = score;
        }

        public Resource getResource() {
            return resource;
        }

        public String getUri() {
            return resource.getURI();
        }

        @Nullable
        public String getLabel() {
            return label;
        }

        public double getScore() {
            return score;
        }

        @Override
        public String toString() {
            return String.format( "%s [score=%.3f]", resource, score );
        }
    }
}
