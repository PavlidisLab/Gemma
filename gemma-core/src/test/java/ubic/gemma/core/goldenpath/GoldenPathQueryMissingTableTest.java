package ubic.gemma.core.goldenpath;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.BadSqlGrammarException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * blatPlatform died on the first probe carrying an accession when the goldenpath database was built from a UCSC
 * assembly hub, which has no {@code all_est} or {@code all_mrna}. Having no precomputed alignment is a case the
 * caller already handles — it runs a real BLAT — but a query this code got wrong still has to fail.
 */
class GoldenPathQueryMissingTableTest {

    @Test
    void aMissingTableIsRecognized() {
        assertThat( GoldenPathQuery.isMissingTable( badGrammar(
                new SQLException( "Table 'rn7_2026.all_est' doesn't exist", "42S02", 1146 ) ) ) ).isTrue();
    }

    @Test
    void anythingElseIsNot() {
        assertThat( GoldenPathQuery.isMissingTable( badGrammar(
                new SQLException( "Unknown column 'qNam' in 'where clause'", "42S22", 1054 ) ) ) ).isFalse();
        assertThat( GoldenPathQuery.isMissingTable( badGrammar(
                new SQLException( "No database selected", "3D000", 1046 ) ) ) ).isFalse();
        assertThat( GoldenPathQuery.isMissingTable( badGrammar( new SQLException( "broken" ) ) ) ).isFalse();
    }

    private static BadSqlGrammarException badGrammar( SQLException cause ) {
        return new BadSqlGrammarException( "findAlignments", "SELECT * FROM all_est WHERE qName = ?", cause );
    }
}
