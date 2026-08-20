package ubic.gemma.core.ontology.lexical;

import org.apache.lucene.analysis.Analyzer;
import ubic.gemma.core.ontology.search.OntologyAnalyzers;
import ubic.gemma.core.ontology.search.OntologyQueries;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A Jena-agnostic, in-memory Lucene 9 full-text index over a flat lexical vocabulary.
 * <p>
 * This is the flat-vocabulary counterpart to {@code jena.LuceneOntologySearchIndex}: it uses the same
 * analysis recipe (an {@link EnglishAnalyzer} Porter stemmer with a caller-supplied stem-exclusion set,
 * a leading-wildcard-tolerant {@link QueryParser} with an escaped-query fallback, and a two-field
 * {@code uri}/{@code text} document) so that matching behaves consistently across every provider merged
 * in the search fan-out. It differs only in that it is fed {@link LexicalTerm}s directly rather than
 * iterating a Jena {@code OntModel}, and returns bare {@code (uri, score)} hits — the caller rehydrates
 * them to terms — so no Jena machinery is dragged in for a vocabulary that has no hierarchy.
 */
public class LexicalOntologyIndex implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger( LexicalOntologyIndex.class );

    /** Lucene field holding the term URI (stored, not analyzed). */
    private static final String URI_FIELD = "uri";
    /** Lucene field holding the concatenated analyzed label + synonym text (recall). */
    private static final String TEXT_FIELD = "text";
    /** Lucene field holding each name/synonym verbatim (lower-cased, un-analyzed) for exact-match boosting (precision). */
    private static final String NAME_FIELD = "name_norm";

    /**
     * Boost applied to an exact (case-insensitive) match of a term's name or synonym, so the canonical
     * entry ranks above its many partial-substring neighbours (e.g. "HeLa" must beat "HeLa Kyoto").
     */
    private static final float EXACT_MATCH_BOOST = 100f;

    public record Hit( String uri, double score ) {
    }

    private final Directory directory;
    private final Analyzer analyzer;
    private final DirectoryReader reader;
    private final IndexSearcher searcher;

    private LexicalOntologyIndex( Directory directory, Analyzer analyzer ) throws IOException {
        this.directory = directory;
        this.analyzer = analyzer;
        this.reader = DirectoryReader.open( directory );
        this.searcher = new IndexSearcher( reader );
    }

    /**
     * Run a full-text query, returning up to {@code maxResults} URI hits ordered by descending score.
     */
    public List<Hit> search( String queryString, int maxResults ) throws IOException {
        if ( queryString == null || queryString.trim().isEmpty() ) {
            throw new IllegalArgumentException( "Query string must be non-empty for lexical ontology search." );
        }
        QueryParser parser = new QueryParser( TEXT_FIELD, analyzer );
        parser.setAllowLeadingWildcard( true );
        Query textQuery;
        try {
            textQuery = parser.parse( queryString );
        } catch ( ParseException e ) {
            try {
                // Fall back to an escaped term query if the raw query won't parse.
                textQuery = parser.parse( QueryParser.escape( queryString ) );
            } catch ( ParseException e2 ) {
                throw new IOException( "Failed to parse lexical ontology query: " + queryString, e2 );
            }
        }
        // Same OR-default problem as the Jena index: without this a catalogue of 118k strain names
        // answers any multi-word query on a single shared token.
        textQuery = OntologyQueries.withMinimumShouldMatch( textQuery, OntologyQueries.DEFAULT_MIN_SHOULD_MATCH );
        // Combine an analyzed-text clause (recall) with a heavily-boosted exact name/synonym clause
        // (precision), so an exact match of the whole query against a name or synonym ranks first.
        Query exactQuery = new BoostQuery(
                new TermQuery( new Term( NAME_FIELD, normalizeExactName( queryString ) ) ),
                EXACT_MATCH_BOOST );
        Query query = new BooleanQuery.Builder()
                .add( textQuery, BooleanClause.Occur.SHOULD )
                .add( exactQuery, BooleanClause.Occur.SHOULD )
                .build();
        TopDocs hits = searcher.search( query, Math.max( maxResults, 1 ) );
        List<Hit> out = new ArrayList<>();
        for ( ScoreDoc sd : hits.scoreDocs ) {
            if ( out.size() >= maxResults ) {
                break;
            }
            Document doc = searcher.storedFields().document( sd.doc );
            String uri = doc.get( URI_FIELD );
            if ( uri == null ) {
                continue;
            }
            out.add( new Hit( uri, sd.score ) );
        }
        return out;
    }

    @Override
    public void close() throws IOException {
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
     * Build a fresh in-memory Lucene 9 index over the given terms. The label and all synonyms of each
     * term are concatenated under a single analyzed text field keyed by the term URI.
     */
    public static LexicalOntologyIndex build( Iterable<LexicalTerm> terms, Set<String> excludedFromStemming ) throws IOException {
        Directory dir = new ByteBuffersDirectory();
        Analyzer analyzer = OntologyAnalyzers.english( excludedFromStemming );
        IndexWriterConfig cfg = new IndexWriterConfig( analyzer );
        cfg.setOpenMode( IndexWriterConfig.OpenMode.CREATE );
        int docCount = 0;
        try ( IndexWriter writer = new IndexWriter( dir, cfg ) ) {
            for ( LexicalTerm t : terms ) {
                if ( t == null || t.uri() == null ) {
                    continue;
                }
                StringBuilder text = new StringBuilder();
                Document doc = new Document();
                if ( t.label() != null && !t.label().isEmpty() ) {
                    text.append( ' ' ).append( t.label() );
                    addExactName( doc, t.label() );
                }
                for ( String syn : t.synonyms() ) {
                    if ( syn != null && !syn.isEmpty() ) {
                        text.append( ' ' ).append( syn );
                        addExactName( doc, syn );
                    }
                }
                if ( text.length() == 0 ) {
                    continue;
                }
                doc.add( new StringField( URI_FIELD, t.uri(), Field.Store.YES ) );
                doc.add( new TextField( TEXT_FIELD, text.toString().trim(), Field.Store.NO ) );
                writer.addDocument( doc );
                docCount++;
            }
            writer.commit();
        }
        log.info( "Built in-memory Lucene 9 lexical index: {} documents.", docCount );
        return new LexicalOntologyIndex( dir, analyzer );
    }

    /** Index a name/synonym verbatim (lower-cased, un-analyzed) for exact-match boosting. */
    private static void addExactName( Document doc, String value ) {
        String norm = normalizeExactName( value );
        if ( !norm.isEmpty() ) {
            doc.add( new StringField( NAME_FIELD, norm, Field.Store.NO ) );
        }
    }

    /**
     * Normalisation for the un-analyzed {@link #NAME_FIELD}, applied identically when indexing a
     * name and when building the exact-match clause for a query.
     *
     * <p>This field bypasses the analyzer by design — it exists to compare whole strings — which
     * means it also bypasses the separator folding {@link OntologyAnalyzers} applies to
     * {@link #TEXT_FIELD}. Left alone, the two halves of this index would disagree: a query of
     * {@code SU 11248} would fold into the text clause but not the exact clause, so the term it
     * names would lose its {@link #EXACT_MATCH_BOOST} and sink under partial neighbours. Folding
     * both sides here keeps the boost aligned with what the analyzed path already matches.</p>
     */
    private static String normalizeExactName( String value ) {
        return OntologyAnalyzers.foldCodeRuns( value.trim().toLowerCase( Locale.ROOT ) );
    }
}
