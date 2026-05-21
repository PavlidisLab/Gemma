/*
 * Phase 3 search/ontology Step 3 — new class authored for the in-Gemma
 * baseCode ontology port. Implements {@link SearchIndex} on Lucene 9 by
 * building an in-memory index from an in-memory Jena 4 {@link OntModel}.
 * <p>
 * Replaces baseCode 1.1.34-RENOVATIONS-SNAPSHOT's stubbed
 * {@code OntologyIndexer.indexOntology(...)} (which always returned null),
 * which had reduced the {@code findTerm} family of methods on every
 * {@link AbstractOntologyService} subclass to "log a warning and return
 * empty". This restores per-provider free-text ontology search for
 * URL/classpath/TDB-loaded providers (GO, MONDO, etc.).
 * <p>
 * Scope: indexes URIs that are subjects of any property in
 * {@link OntologyIndexer.IndexableProperty} (defaults: {@code rdfs:label},
 * OBO synonyms, OBO IDs, OBO dbXref, IAO alternativeLabel). Result objects
 * are {@link Resource}s fetched back from the {@code OntModel} at query
 * time, so callers can {@code as(OntClass.class)} / {@code as(Individual.class)}.
 * <p>
 * Lifecycle: built once per ontology load, kept in memory, closed when the
 * service is reloaded or the JVM stops. No on-disk caching at the moment
 * (baseCode's pre-strip implementation cached on disk under
 * {@code Configuration.get("ontology.cache.dir")}; this version trades that
 * for simplicity and the fact that we rebuild on every load anyway).
 */
package ubic.gemma.core.ontology.basecode.jena;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ubic.gemma.core.ontology.basecode.search.OntologySearchException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Lucene-9 backed {@link SearchIndex} for an in-memory Jena {@link OntModel}.
 *
 * @see OntologyIndexer
 */
class LuceneOntologySearchIndex implements SearchIndex {

    private static final Logger log = LoggerFactory.getLogger( LuceneOntologySearchIndex.class );

    /** Lucene field name holding the resource URI (stored, not analyzed). */
    private static final String URI_FIELD = "uri";
    /** Lucene field name holding the concatenated analyzed label/synonym text. */
    private static final String TEXT_FIELD = "text";

    private final Directory directory;
    private final Analyzer analyzer;
    private final DirectoryReader reader;
    private final IndexSearcher searcher;

    LuceneOntologySearchIndex( Directory directory, Analyzer analyzer ) throws IOException {
        this.directory = directory;
        this.analyzer = analyzer;
        this.reader = DirectoryReader.open( directory );
        this.searcher = new IndexSearcher( reader );
    }

    @Override
    public List<JenaSearchResult> search( OntModel model, String queryString, int maxResults ) throws OntologySearchException {
        return doSearch( model, queryString, maxResults, false, false );
    }

    @Override
    public List<JenaSearchResult> searchClasses( OntModel model, String queryString, int maxResults ) throws OntologySearchException {
        return doSearch( model, queryString, maxResults, true, false );
    }

    @Override
    public List<JenaSearchResult> searchIndividuals( OntModel model, String queryString, int maxResults ) throws OntologySearchException {
        return doSearch( model, queryString, maxResults, false, true );
    }

    private List<JenaSearchResult> doSearch( OntModel model, String queryString, int maxResults, boolean onlyClasses, boolean onlyIndividuals ) throws OntologySearchException {
        if ( queryString == null || queryString.trim().isEmpty() ) {
            throw new IllegalArgumentException( "Query string must be non-empty for ontology full-text search." );
        }
        try {
            QueryParser parser = new QueryParser( TEXT_FIELD, analyzer );
            // Allow * and ? as prefix wildcards too (older baseCode behavior).
            parser.setAllowLeadingWildcard( true );
            Query query;
            try {
                query = parser.parse( queryString );
            } catch ( ParseException e ) {
                // Fall back to escaped term search if the user's query won't parse.
                query = parser.parse( QueryParser.escape( queryString ) );
            }
            // Fetch a few extra to allow for class/individual filtering.
            int fetch = Math.max( maxResults * 2, maxResults );
            TopDocs hits = searcher.search( query, fetch );
            List<JenaSearchResult> out = new ArrayList<>();
            for ( ScoreDoc sd : hits.scoreDocs ) {
                if ( out.size() >= maxResults ) break;
                Document doc = searcher.storedFields().document( sd.doc );
                String uri = doc.get( URI_FIELD );
                if ( uri == null ) continue;
                Resource r = model.getResource( uri );
                if ( r == null || r.getURI() == null ) continue;
                if ( onlyClasses && !r.canAs( org.apache.jena.ontology.OntClass.class ) ) continue;
                if ( onlyIndividuals && !r.canAs( org.apache.jena.ontology.Individual.class ) ) continue;
                out.add( new JenaSearchResult( r, sd.score ) );
            }
            return out;
        } catch ( IOException | ParseException e ) {
            throw new OntologySearchException( "Lucene query failure for ontology index.", queryString, e );
        }
    }

    @Override
    public void close() throws Exception {
        try {
            reader.close();
        } finally {
            try {
                directory.close();
            } finally {
                analyzer.close();
            }
        }
    }

    /**
     * Build a fresh in-memory Lucene 9 index over the given {@link OntModel}
     * using an English Porter-stem analyzer with the given stem-exclusion set
     * so protected words (e.g. ontology terms a caller wants to match verbatim)
     * are indexed verbatim while other words collapse to their Porter stem.
     * Literal values of the given properties are concatenated under a single
     * analyzed text field keyed by the subject URI.
     */
    static LuceneOntologySearchIndex build( OntModel model, Collection<OntologyIndexer.IndexableProperty> properties, Set<String> excludedFromStemming ) throws IOException {
        // ByteBuffersDirectory is Lucene 9's drop-in for the removed RAMDirectory.
        Directory dir = new ByteBuffersDirectory();
        CharArraySet stemExclusion = new CharArraySet(
                excludedFromStemming == null ? Collections.emptySet() : excludedFromStemming,
                false /* not case-sensitive */
        );
        EnglishAnalyzer analyzer = new EnglishAnalyzer(
                EnglishAnalyzer.getDefaultStopSet(),
                stemExclusion
        );
        IndexWriterConfig cfg = new IndexWriterConfig( analyzer );
        cfg.setOpenMode( IndexWriterConfig.OpenMode.CREATE );
        int docCount = 0;
        try ( IndexWriter writer = new IndexWriter( dir, cfg ) ) {
            // Iterate one property at a time and merge documents per URI in memory.
            java.util.Map<String, StringBuilder> perUri = new java.util.HashMap<>();
            for ( OntologyIndexer.IndexableProperty ip : properties ) {
                if ( ip == null || ip.getProperty() == null ) continue;
                StmtIterator it = model.listStatements( null, ip.getProperty(), ( RDFNode ) null );
                while ( it.hasNext() ) {
                    Statement s = it.next();
                    Resource subj = s.getSubject();
                    if ( subj == null || subj.getURI() == null ) continue;
                    RDFNode obj = s.getObject();
                    if ( obj == null ) continue;
                    String text;
                    if ( obj.isLiteral() ) {
                        text = ( ( Literal ) obj ).getLexicalForm();
                    } else if ( obj.isURIResource() ) {
                        text = obj.asResource().getURI();
                    } else {
                        continue;
                    }
                    if ( text == null || text.isEmpty() ) continue;
                    perUri.computeIfAbsent( subj.getURI(), k -> new StringBuilder() )
                        .append( ' ' ).append( text );
                }
            }
            for ( java.util.Map.Entry<String, StringBuilder> e : perUri.entrySet() ) {
                Document doc = new Document();
                doc.add( new StringField( URI_FIELD, e.getKey(), Field.Store.YES ) );
                doc.add( new TextField( TEXT_FIELD, e.getValue().toString().trim(), Field.Store.NO ) );
                writer.addDocument( doc );
                docCount++;
            }
            writer.commit();
        }
        log.info( "Built in-memory Lucene 9 index for ontology: {} documents.", docCount );
        return new LuceneOntologySearchIndex( dir, analyzer );
    }
}
