package ubic.gemma.core.search.lucene;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Regression guard for free-text queries that carry Lucene operators — biological search
 * strings routinely do ("100 ng/ml", bracketed chemical names). These used to throw a raw
 * {@link org.apache.lucene.queryparser.classic.TokenMgrError} (an {@link Error}, uncaught by
 * the {@code ParseException} fallback) and/or fail the escape-retry because '/' was not in the
 * escaped-character set, bubbling up to the retry interceptor as log noise.
 *
 * @author poirigui
 */
class LuceneQueryUtilsTest {

    /**
     * An unterminated Lucene regex ('/ml') makes the lexer throw {@code TokenMgrError}. It must be
     * caught, the query re-escaped (now including '/'), and a usable term returned — no throw.
     */
    @Test
    void prepareDatabaseQuery_withUnterminatedRegexSlash_recovers() throws Exception {
        assertThatNoException().isThrownBy( () -> LuceneQueryUtils.prepareDatabaseQuery( "100 ng/ml", false ) );
        assertThat( LuceneQueryUtils.prepareDatabaseQuery( "100 ng/ml", false ) ).isNotNull();
    }

    /**
     * A lone trailing slash is the minimal reproducer for the unterminated-regex lex error.
     */
    @Test
    void prepareDatabaseQuery_withTrailingSlash_recovers() {
        assertThatNoException().isThrownBy( () -> LuceneQueryUtils.prepareDatabaseQuery( "cd4/", false ) );
    }

    /**
     * Bracketed chemical names blow up the grammar (open range); the escape fallback already
     * covered '[' / ']', so this must keep working.
     */
    @Test
    void prepareDatabaseQuery_withBrackets_recovers() {
        assertThatNoException().isThrownBy( () -> LuceneQueryUtils
                .prepareDatabaseQuery( "[3-anilino-4-[oxo-[4-(1-pyrrolidinyl)-1-piperidinyl]methyl]phenyl]", false ) );
    }

    /**
     * A plain gene symbol still round-trips to itself.
     */
    @Test
    void prepareDatabaseQuery_withPlainSymbol_returnsSymbol() throws Exception {
        assertThat( LuceneQueryUtils.prepareDatabaseQuery( "TP53", false ) ).isEqualTo( "TP53" );
    }
}
