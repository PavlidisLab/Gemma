package ubic.gemma.core.search;

import org.junit.Test;
import ubic.gemma.model.common.search.SearchSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Sets.set;
import static org.junit.Assert.*;
import static ubic.gemma.core.search.QueryUtils.*;

public class QueryUtilsTest {

    @Test
    public void testExtractTerms() throws SearchException {
        assertThat( extractTerms( SearchSettings.geneSearch( "BRCA1 OR (BRCA2 AND BRCA3) OR NOT BRCA4 OR -BRCA5", null ) ) )
                .containsExactlyInAnyOrder( "BRCA1", "BRCA2", "BRCA3" );
    }

    @Test
    public void testExtractDnf() throws SearchException {
        assertThat( extractDnf( SearchSettings.geneSearch( "BRCA1 OR (BRCA2 AND BRCA3) OR NOT BRCA4 OR -BRCA5 OR (BRCA6 OR BRCA7) AND BRCA9", null ) ) )
                .containsExactlyInAnyOrder( set( "BRCA1" ), set( "BRCA2", "BRCA3" ) );
        assertThat( extractDnf( SearchSettings.geneSearch( "BRCA1 AND BRCA2", null ) ) )
                .containsExactlyInAnyOrder( set( "BRCA1", "BRCA2" ) );
        assertThat( extractDnf( SearchSettings.geneSearch( "BRCA1 AND NOT BRCA2", null ) ) )
                .isEmpty();
        assertThat( extractDnf( SearchSettings.geneSearch( "BRCA1 AND (BRCA2 OR BRCA3)", null ) ) )
                .isEmpty();
    }

    @Test
    public void testPrepareDatabaseQuery() throws SearchException {
        assertEquals( "BRCA1", prepareDatabaseQuery( SearchSettings.geneSearch( "BRCA1", null ) ) );
        assertEquals( "BRCA1", prepareDatabaseQuery( SearchSettings.geneSearch( "BRCA1^4", null ) ) );
        assertEquals( "BRCA1", prepareDatabaseQuery( SearchSettings.geneSearch( "\"BRCA1\"", null ) ) );
        assertEquals( "BRCA1", prepareDatabaseQuery( SearchSettings.geneSearch( "(BRCA1)", null ) ) );
        assertEquals( "BRCA1", prepareDatabaseQuery( SearchSettings.geneSearch( "symbol:BRCA1", null ) ) );
        assertEquals( "+BRCA", prepareDatabaseQueryForInexactMatch( SearchSettings.geneSearch( "\\+BRCA", null ) ) );
        assertEquals( "BRCA", prepareDatabaseQuery( SearchSettings.geneSearch( "BRCA OR TCGA", null ) ) );
        assertEquals( "BRCA", prepareDatabaseQuery( SearchSettings.geneSearch( "BRCA AND TCGA", null ) ) );
        assertEquals( "BRCA", prepareDatabaseQuery( SearchSettings.geneSearch( "BRCA AND NOT TCGA", null ) ) );
        assertEquals( "TCGA", prepareDatabaseQuery( SearchSettings.geneSearch( "NOT BRCA AND TCGA", null ) ) );
        assertEquals( "BRCA", prepareDatabaseQuery( SearchSettings.geneSearch( "BRCA -TCGA", null ) ) );
        assertEquals( "BRCA AND TCGA", prepareDatabaseQuery( SearchSettings.geneSearch( "\"BRCA AND TCGA\"", null ) ) );
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
        assertEquals( "BRCA", prepareDatabaseQueryForInexactMatch( SearchSettings.geneSearch( "\"BRCA\"", null ) ) );
        assertEquals( "br%ca", prepareDatabaseQueryForInexactMatch( SearchSettings.geneSearch( "BR*CA", null ) ) );
        assertEquals( "brca%", prepareDatabaseQueryForInexactMatch( SearchSettings.geneSearch( "BRCA*", null ) ) );
        assertEquals( "BRCA*", prepareDatabaseQueryForInexactMatch( SearchSettings.geneSearch( "\"BRCA\\*\"", null ) ) );
        assertEquals( "brca_", prepareDatabaseQueryForInexactMatch( SearchSettings.geneSearch( "BRCA?", null ) ) );
        assertEquals( "BRCA?", prepareDatabaseQueryForInexactMatch( SearchSettings.geneSearch( "\"BRCA?\"", null ) ) );
        assertEquals( "BRCA", prepareDatabaseQueryForInexactMatch( SearchSettings.geneSearch( "+BRCA", null ) ) );
        // forbidden prefix-style searches
        assertEquals( "*", prepareDatabaseQueryForInexactMatch( SearchSettings.geneSearch( "*", null ) ) );
        assertEquals( "*BRCA", prepareDatabaseQueryForInexactMatch( SearchSettings.geneSearch( "*BRCA", null ) ) );
        assertEquals( "?", prepareDatabaseQueryForInexactMatch( SearchSettings.geneSearch( "?", null ) ) );
        assertEquals( "?RCA", prepareDatabaseQueryForInexactMatch( SearchSettings.geneSearch( "?RCA", null ) ) );
        // check for escaping LIKE patterns
        assertEquals( "BRCA\\\\", prepareDatabaseQueryForInexactMatch( SearchSettings.geneSearch( "BRCA\\", null ) ) );
        assertEquals( "BRCA\\%", prepareDatabaseQueryForInexactMatch( SearchSettings.geneSearch( "BRCA\\%", null ) ) );
        assertEquals( "BRCA\\%", prepareDatabaseQueryForInexactMatch( SearchSettings.geneSearch( "BRCA%", null ) ) );
        assertEquals( "BRCA\\_", prepareDatabaseQueryForInexactMatch( SearchSettings.geneSearch( "BRCA_", null ) ) );
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
}