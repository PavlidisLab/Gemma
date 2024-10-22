package ubic.gemma.core.search.lucene;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;
import org.junit.Test;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.model.common.search.SearchSettings;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.assertj.core.util.Sets.set;
import static org.junit.Assert.*;
import static ubic.gemma.core.search.lucene.LuceneQueryUtils.*;

public class LuceneQueryUtilsTest {

    @Test
    public void testExtractTerms() throws SearchException {
        assertThat( extractTerms( SearchSettings.geneSearch( "BRCA1 OR (BRCA2 AND BRCA3) OR NOT BRCA4 OR -BRCA5", null ) ) )
                .containsExactlyInAnyOrder( "BRCA1", "BRCA2", "BRCA3" );
        // fielded terms are excluded
        assertThat( extractTerms( SearchSettings.geneSearch( "shortName:GSE1234 test", null ) ) )
                .containsExactlyInAnyOrder( "test" );
    }

    @Test
    public void testExtractDnf() throws SearchException {
        assertThat( extractTermsDnf( SearchSettings.geneSearch( "BRCA1 OR (BRCA2 AND BRCA3) OR NOT BRCA4 OR -BRCA5 OR (BRCA6 OR BRCA7)", null ) ) )
                .containsExactlyInAnyOrder( set( "BRCA1" ), set( "BRCA2", "BRCA3" ), set( "BRCA6" ), set( "BRCA7" ) );
        assertThat( extractTermsDnf( SearchSettings.geneSearch( "BRCA1 AND BRCA2", null ) ) )
                .containsExactlyInAnyOrder( set( "BRCA1", "BRCA2" ) );
        assertThat( extractTermsDnf( SearchSettings.geneSearch( "NOT BRCA1 AND NOT BRCA2", null ) ) )
                .isEmpty();
        assertThat( extractTermsDnf( SearchSettings.geneSearch( "NOT BRCA1 OR NOT BRCA2", null ) ) )
                .isEmpty();
        assertThat( extractTermsDnf( SearchSettings.geneSearch( "BRCA1 AND NOT BRCA2", null ) ) )
                .containsExactly( set( "BRCA1" ) );
        assertThat( extractTermsDnf( SearchSettings.geneSearch( "BRCA1 OR NOT (BRCA2 AND BRCA3)", null ) ) )
                .containsExactly( set( "BRCA1" ) );
        assertThat( extractTermsDnf( SearchSettings.geneSearch( "BRCA1 AND (BRCA2 OR BRCA3)", null ) ) )
                .isEmpty();
    }

    @Test
    public void testExtractDnfWithQuotedSpaces() throws SearchException {
        assertThat( extractTermsDnf( SearchSettings.geneSearch( "\"alpha beta\" OR \"gamma delta\"", null ) ) )
                .containsExactlyInAnyOrder( set( "alpha beta" ), set( "gamma delta" ) );
    }

    @Test
    public void testExtractDnfWithNestedOrInClause() throws SearchException {
        assertThat( extractTermsDnf( SearchSettings.geneSearch( "BRCA1 OR (BRCA2 OR (BRCA3 AND BRCA4))", null ) ) )
                .containsExactlyInAnyOrder( set( "BRCA1" ), set( "BRCA2" ), set( "BRCA3", "BRCA4" ) );
    }

    @Test
    public void testExtractDnfWithNestedAndInSubClause() throws SearchException {
        assertThat( extractTermsDnf( SearchSettings.geneSearch( "BRCA1 OR (BRCA2 AND (BRCA3 AND BRCA4))", null ) ) )
                .containsExactlyInAnyOrder( set( "BRCA1" ), set( "BRCA2", "BRCA3", "BRCA4" ) );
    }

    @Test
    public void testExtractDnfWithUris() throws SearchException {
        // this is an important case for searching datasets by ontology terms
        assertThat( extractTermsDnf( SearchSettings.geneSearch( "http://example.com/GO:1234 OR http://example.com/GO:1235", null ) ) )
                .contains( set( "http://example.com/GO:1234" ), set( "http://example.com/GO:1235" ) );
    }

    @Test
    public void testExtractDnfWithOntologyTerms() throws SearchException {
        // reckognized ontology prefixes are listed in ontology.prefixes.txt
        assertThat( extractTermsDnf( SearchSettings.geneSearch( "GO:0100101 OR GO:0100102 OR DGO:1090129", null ) ) )
                .contains( set( "GO:0100101" ), set( "GO:0100102" ) );
    }

    @Test
    public void testPrepareDatabaseQuery() throws SearchException {
        assertEquals( "BRCA1", prepareDatabaseQuery( SearchSettings.geneSearch( "BRCA1", null ) ) );
        assertEquals( "BRCA1", prepareDatabaseQuery( SearchSettings.geneSearch( "BRCA1^4", null ) ) );
        assertEquals( "BRCA1", prepareDatabaseQuery( SearchSettings.geneSearch( "\"BRCA1\"", null ) ) );
        assertEquals( "BRCA1", prepareDatabaseQuery( SearchSettings.geneSearch( "(BRCA1)", null ) ) );
        // fielded term are ignored
        assertNull( prepareDatabaseQuery( SearchSettings.geneSearch( "symbol:BRCA1", null ) ) );
        assertEquals( "+BRCA", prepareDatabaseQuery( SearchSettings.geneSearch( "\\+BRCA", null ), true ) );
        assertEquals( "BRCA", prepareDatabaseQuery( SearchSettings.geneSearch( "BRCA OR TCGA", null ) ) );
        assertEquals( "BRCA", prepareDatabaseQuery( SearchSettings.geneSearch( "BRCA AND TCGA", null ) ) );
        assertEquals( "BRCA", prepareDatabaseQuery( SearchSettings.geneSearch( "BRCA AND NOT TCGA", null ) ) );
        assertEquals( "TCGA", prepareDatabaseQuery( SearchSettings.geneSearch( "NOT BRCA AND TCGA", null ) ) );
        assertEquals( "BRCA", prepareDatabaseQuery( SearchSettings.geneSearch( "BRCA -TCGA", null ) ) );
        assertEquals( "BRCA AND TCGA", prepareDatabaseQuery( SearchSettings.geneSearch( "\"BRCA AND TCGA\"", null ) ) );
        // wildcards and prefix queries are ignored for database queries
        assertNull( prepareDatabaseQuery( SearchSettings.geneSearch( "BRCA*", null ) ) );
    }

    @Test
    public void testPrepareDatabaseQueryWithUri() throws SearchException {
        // ideal case, using quotes
        assertEquals( "http://example.com/GO:1234", prepareDatabaseQuery( SearchSettings.geneSearch( "\"http://example.com/GO:1234\"", null ) ) );
        assertEquals( "http://example.com/GO:1234", prepareDatabaseQuery( SearchSettings.geneSearch( "http://example.com/GO:1234", null ) ) );
        assertEquals( "http://example.com/GO:1234?a=b#c=d", prepareDatabaseQuery( SearchSettings.geneSearch( "http://example.com/GO:1234?a=b#c=d", null ) ) );
        assertEquals( "http://example.com/GO_1234", prepareDatabaseQuery( SearchSettings.geneSearch( "http://example.com/GO_1234", null ) ) );
        assertEquals( "http://example.com/#GO_1234", prepareDatabaseQuery( SearchSettings.geneSearch( "http://example.com/#GO_1234", null ) ) );
        assertEquals( "http://example.com/GO:1234", prepareDatabaseQuery( SearchSettings.geneSearch( "http://example.com/GO:1234 OR http://example.com/GO:1235", null ) ) );
    }

    @Test
    public void testPrepareDatabaseQueryForInexactMatch() throws SearchException {
        assertEquals( "BRCA", prepareDatabaseQuery( SearchSettings.geneSearch( "\"BRCA\"", null ), true ) );
        assertEquals( "br%ca", prepareDatabaseQuery( SearchSettings.geneSearch( "BR*CA", null ), true ) );
        assertEquals( "brca%", prepareDatabaseQuery( SearchSettings.geneSearch( "BRCA*", null ), true ) );
        assertEquals( "BRCA*", prepareDatabaseQuery( SearchSettings.geneSearch( "\"BRCA\\*\"", null ), true ) );
        assertEquals( "brca_", prepareDatabaseQuery( SearchSettings.geneSearch( "BRCA?", null ), true ) );
        assertEquals( "BRCA?", prepareDatabaseQuery( SearchSettings.geneSearch( "\"BRCA?\"", null ), true ) );
        assertEquals( "BRCA", prepareDatabaseQuery( SearchSettings.geneSearch( "+BRCA", null ), true ) );
        // escaped wildcard
        assertEquals( "BRCA?", prepareDatabaseQuery( SearchSettings.geneSearch( "BRCA\\?", null ), true ) );
        assertEquals( "BRCA*", prepareDatabaseQuery( SearchSettings.geneSearch( "BRCA\\*", null ), true ) );
        // forbidden prefix-style searches
        assertEquals( "*", prepareDatabaseQuery( SearchSettings.geneSearch( "*", null ), true ) );
        assertEquals( "*BRCA", prepareDatabaseQuery( SearchSettings.geneSearch( "*BRCA", null ), true ) );
        assertEquals( "?", prepareDatabaseQuery( SearchSettings.geneSearch( "?", null ), true ) );
        assertEquals( "?RCA", prepareDatabaseQuery( SearchSettings.geneSearch( "?RCA", null ), true ) );
        // check for escaping LIKE patterns
        assertEquals( "BRCA\\\\", prepareDatabaseQuery( SearchSettings.geneSearch( "BRCA\\", null ), true ) );
        assertEquals( "BRCA\\%", prepareDatabaseQuery( SearchSettings.geneSearch( "BRCA\\%", null ), true ) );
        assertEquals( "BRCA\\%", prepareDatabaseQuery( SearchSettings.geneSearch( "BRCA%", null ), true ) );
        assertEquals( "BRCA\\_", prepareDatabaseQuery( SearchSettings.geneSearch( "BRCA_", null ), true ) );
    }

    @Test
    public void testIsWildcard() {
        assertFalse( isWildcard( SearchSettings.geneSearch( "*", null ) ) );
        assertFalse( isWildcard( SearchSettings.geneSearch( "*BRCA", null ) ) );
        assertTrue( isWildcard( SearchSettings.geneSearch( "BR*CA", null ) ) );
        assertTrue( isWildcard( SearchSettings.geneSearch( "BRCA*", null ) ) );
        assertFalse( isWildcard( SearchSettings.geneSearch( "BRCA1 BRCA*", null ) ) );
        assertFalse( isWildcard( SearchSettings.geneSearch( "\"BRCA*\"", null ) ) );
        assertTrue( isWildcard( SearchSettings.geneSearch( "BRCA?", null ) ) );
        assertFalse( isWildcard( SearchSettings.geneSearch( "BRCA\\*", null ) ) );
        assertFalse( isWildcard( SearchSettings.geneSearch( "\"BRCA1\" \"BRCA2\"", null ) ) );
    }

    @Test
    public void testPrepareTermUriQuery() throws SearchException {
        assertEquals( URI.create( "http://example.com" ), prepareTermUriQuery( SearchSettings.geneSearch( "http://example.com", null ) ) );
        assertEquals( URI.create( "http://example.com" ), prepareTermUriQuery( SearchSettings.geneSearch( "\"http://example.com\"", null ) ) );
        // an invalid URI
        assertNull( prepareTermUriQuery( SearchSettings.geneSearch( "\"http://example.com /test\"", null ) ) );
        // an interesting case: a fielded search for a URI
        assertEquals( URI.create( "http://example.com" ), prepareTermUriQuery( SearchSettings.geneSearch( "http:\"http://example.com\"", null ) ) );
    }

    @Test
    public void testQuote() {
        assertEquals( "\"alpha beta\"", quote( "alpha beta" ) );
        assertEquals( "BRCA1", quote( "BRCA1" ) );
        assertEquals( "BRCA?", quote( "BRCA?" ) );
        assertEquals( "BRCA1\\\"", quote( "BRCA1\"" ) );
    }

    /**
     * Make sure that a query containing a hyphen is not parsed as a negative query.
     * @throws SearchException
     */
    @Test
    public void testQueryWithHyphen() throws SearchException {
        Query query = parseSafely( SearchSettings.expressionExperimentSearch( "single-cell transcriptomics" ), new QueryParser( Version.LUCENE_36, "*", new EnglishAnalyzer( Version.LUCENE_36 ) ) );
        assertThat( query ).asInstanceOf( type( BooleanQuery.class ) ).satisfies( tq -> {
            assertThat( tq.clauses() )
                    .satisfiesExactly(
                            c -> assertThat( c ).asInstanceOf( type( TermQuery.class ) )
                                    .extracting( TermQuery::getTerm )
                                    .isEqualTo( "single-cell" ),
                            c -> assertThat( c ).asInstanceOf( type( TermQuery.class ) )
                                    .extracting( TermQuery::getTerm )
                                    .isEqualTo( "transcriptomics" )
                    );
        } );
    }
}