/*
 * The basecode project
 *
 * Copyright (c) 2007-2019 University of British Columbia
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
/*
 * Originated in baseCode (ubic.basecode.ontology.*); pulled in-tree for Gemma 2.0
 * (Phase 3 search/ontology Step 3). Ported from Jena 2.x (com.hp.hpl.jena.*)
 * to Jena 4.x (org.apache.jena.*) namespace. Configuration-related lookups
 * continue to use baseCode's ubic.basecode.util.Configuration via the
 * still-classpath baseCode JAR.
 */
package ubic.gemma.core.ontology.basecode.jena;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.shared.JenaException;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Indexer for ontology full-text search.
 * <p>
 * <b>Phase 3 (Gemma 2.0) restoration.</b> baseCode 1.1.34-RENOVATIONS-SNAPSHOT
 * shipped this class as a stub that always returned null — the previous
 * Lucene-3 implementation was deleted on the renovations branch — which
 * silently disabled {@code findTerm} across every provider. Now that the
 * baseCode ontology source lives in-tree on Lucene 9 + Jena 4.10, this
 * indexer builds a real in-memory Lucene 9 index per ontology via
 * {@link LuceneOntologySearchIndex}, restoring per-provider free-text
 * search. {@link AbstractOntologyService} continues to tolerate a null
 * index (search returns empty results with a warning) if construction
 * fails for any reason.
 *
 * @author pavlidis
 */
class OntologyIndexer {

    private static final Logger log = LoggerFactory.getLogger( OntologyIndexer.class );

    public static class IndexableProperty {
        private final Property property;
        private final boolean analyzed;

        public IndexableProperty( Property property, boolean analyzed ) {
            this.property = property;
            this.analyzed = analyzed;
        }

        public Property getProperty() {
            return property;
        }

        public boolean isAnalyzed() {
            return analyzed;
        }
    }

    /**
     * The default set of RDF properties baseCode 1.1.34 indexed. Kept as a static reference even though the indexer
     * no longer reads it, so callers compile and the future re-implementation has a starting point.
     */
    public static final Collection<IndexableProperty> DEFAULT_INDEXABLE_PROPERTIES;

    static {
        Set<IndexableProperty> props = new HashSet<>();
        props.add( new IndexableProperty( RDFS.label, true ) );
        props.add( new IndexableProperty( OBO.id, true ) );
        props.add( new IndexableProperty( OBO.hasDbXref, true ) );
        props.add( new IndexableProperty( OBO.hasSynonym, true ) );
        props.add( new IndexableProperty( OBO.hasExactSynonym, true ) );
        props.add( new IndexableProperty( OBO.hasBroadSynonym, true ) );
        props.add( new IndexableProperty( OBO.hasNarrowSynonym, true ) );
        props.add( new IndexableProperty( OBO.hasRelatedSynonym, true ) );
        props.add( new IndexableProperty( IAO.alternativeLabel, true ) );
        DEFAULT_INDEXABLE_PROPERTIES = Collections.unmodifiableSet( props );
    }

    /**
     * Look up a previously-built index for the named ontology. The Gemma
     * implementation does not persist indexes to disk yet (the pre-strip
     * baseCode version cached under {@code Configuration.get("ontology.cache.dir")});
     * call {@link #indexOntology(String, OntModel, Set, boolean)} to build
     * one on demand. Returning null signals to {@link AbstractOntologyService}
     * that no cached index exists, which triggers a rebuild.
     */
    @Nullable
    public static SearchIndex getSubjectIndex( String name, Set<String> excludedFromStemming ) {
        return null;
    }

    /**
     * @see #getSubjectIndex(String, Set)
     */
    @Nullable
    public static SearchIndex getSubjectIndex( String name, Collection<IndexableProperty> indexableProperties, Set<String> excludedFromStemming ) {
        return null;
    }

    /**
     * Build a fresh in-memory Lucene 9 index over the given {@link OntModel}
     * using {@link #DEFAULT_INDEXABLE_PROPERTIES} as the indexable property
     * set. Returns null only if building the index fails outright; the caller
     * ({@link AbstractOntologyService}) treats null as "search disabled for
     * this ontology" and falls back to returning empty results with a
     * warning.
     */
    @Nullable
    public static SearchIndex indexOntology( String name, OntModel model, Set<String> excludedFromStemming, boolean force ) throws JenaException, IOException {
        return indexOntology( name, model, DEFAULT_INDEXABLE_PROPERTIES, excludedFromStemming, force );
    }

    /**
     * Build a fresh in-memory Lucene 9 index over the given {@link OntModel}
     * with the caller-supplied indexable property set.
     */
    @Nullable
    public static SearchIndex indexOntology( String name, OntModel model, Collection<IndexableProperty> indexableProperties, Set<String> excludedFromStemming, boolean force ) throws JenaException, IOException {
        try {
            log.info( "Building in-memory Lucene 9 index for ontology {}", name );
            return LuceneOntologySearchIndex.build( model, indexableProperties, excludedFromStemming );
        } catch ( RuntimeException e ) {
            // Don't take the whole context down if indexing fails; downstream
            // findTerm calls degrade gracefully via the null-index branch.
            log.error( "Failed to build search index for ontology {} — search will be disabled for it.", name, e );
            return null;
        }
    }
}
