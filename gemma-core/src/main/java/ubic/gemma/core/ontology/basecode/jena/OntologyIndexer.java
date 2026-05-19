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
 * Stub indexer for ontology full-text search.
 * <p>
 * The previous implementation indexed an OntModel into Lucene 3.x via a private {@code LuceneSearchIndex}
 * implementation of {@link SearchIndex}. Lucene 3 has been removed on the renovations branch so this indexer is now
 * disabled — both factory methods return null. {@link AbstractOntologyService} already tolerates a null index (search
 * returns empty results with a warning).
 * <p>
 * Reinstating ontology full-text search is future renovation work (proper Lucene 5+ port, or migration to Jena's
 * {@code jena-text} module). The {@link IndexableProperty} type is preserved because callers in baseCode and Gemma
 * reference {@link #DEFAULT_INDEXABLE_PROPERTIES} for their own configuration.
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

    /** Stub: always null. */
    @Nullable
    public static SearchIndex getSubjectIndex( String name, Set<String> excludedFromStemming ) {
        return null;
    }

    /** Stub: always null. */
    @Nullable
    public static SearchIndex getSubjectIndex( String name, Collection<IndexableProperty> indexableProperties, Set<String> excludedFromStemming ) {
        return null;
    }

    /** Stub: always null; logs once. */
    @Nullable
    public static SearchIndex indexOntology( String name, OntModel model, Set<String> excludedFromStemming, boolean force ) throws JenaException, IOException {
        log.info( "Ontology search is disabled (renovations stub); skipping indexing of {}", name );
        return null;
    }

    /** Stub: always null; logs once. */
    @Nullable
    public static SearchIndex indexOntology( String name, OntModel model, Collection<IndexableProperty> indexableProperties, Set<String> excludedFromStemming, boolean force ) throws JenaException, IOException {
        log.info( "Ontology search is disabled (renovations stub); skipping indexing of {}", name );
        return null;
    }
}
