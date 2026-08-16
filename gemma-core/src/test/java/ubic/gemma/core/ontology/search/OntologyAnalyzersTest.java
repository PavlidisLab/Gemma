package ubic.gemma.core.ontology.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the separator folding that lets a submitter's {@code SU11248} reach the compound CHEBI
 * stores as {@code SU-11248}.
 *
 * <p>The folding is only safe because it is symmetric — both the indexed text and the query pass
 * through the same filter — so the tests below assert on the shared normal form rather than on one
 * side of it.</p>
 */
class OntologyAnalyzersTest {

    @Test
    void separatorVariantsOfATrialCodeShareANormalForm() {
        // The three spellings of one compound: CHEBI's, the submitter's, and the spaced form.
        assertThat( OntologyAnalyzers.foldCodeRuns( "SU-11248" ) ).isEqualTo( "SU11248" );
        assertThat( OntologyAnalyzers.foldCodeRuns( "SU 11248" ) ).isEqualTo( "SU11248" );
        assertThat( OntologyAnalyzers.foldCodeRuns( "SU11248" ) ).isEqualTo( "SU11248" );

        assertThat( OntologyAnalyzers.foldCodeRuns( "CP-690550" ) )
                .isEqualTo( OntologyAnalyzers.foldCodeRuns( "CP 690550" ) );
        assertThat( OntologyAnalyzers.foldCodeRuns( "PD 0325901" ) )
                .isEqualTo( OntologyAnalyzers.foldCodeRuns( "PD0325901" ) );
    }

    @Test
    void hyphenatedCodeGroupsFoldWhole() {
        // Sorafenib's code carries a second digit group; folding must not stop at the first.
        assertThat( OntologyAnalyzers.foldCodeRuns( "BAY 43-9006" ) ).isEqualTo( "BAY43-9006" );
        assertThat( OntologyAnalyzers.foldCodeRuns( "BAY43-9006" ) ).isEqualTo( "BAY43-9006" );
    }

    @Test
    void ordinaryProseIsUntouched() {
        // Prefix too long, number too short, or both — none of these are coined identifiers, and
        // silently welding them would change matching for text that has nothing to do with codes.
        assertThat( OntologyAnalyzers.foldCodeRuns( "interleukin 6" ) ).isEqualTo( "interleukin 6" );
        assertThat( OntologyAnalyzers.foldCodeRuns( "CD 34" ) ).isEqualTo( "CD 34" );
        assertThat( OntologyAnalyzers.foldCodeRuns( "vitamin D3" ) ).isEqualTo( "vitamin D3" );
        assertThat( OntologyAnalyzers.foldCodeRuns( "type 2 diabetes mellitus" ) )
                .isEqualTo( "type 2 diabetes mellitus" );
        assertThat( OntologyAnalyzers.foldCodeRuns( "high fat diet" ) ).isEqualTo( "high fat diet" );
    }

    /**
     * The folding is a char filter, so it has to survive the trip through the real analyzer — this
     * is what the index and the query parser actually see.
     */
    @Test
    void analyzerEmitsTheSameTokensForBothSpellings() throws IOException {
        try ( Analyzer analyzer = OntologyAnalyzers.english( Collections.emptySet() ) ) {
            assertThat( tokens( analyzer, "SU-11248" ) ).isEqualTo( tokens( analyzer, "SU11248" ) );
            assertThat( tokens( analyzer, "CP 690550" ) ).isEqualTo( tokens( analyzer, "CP690550" ) );
            // A single token, not two halves — the whole point of folding before tokenization.
            assertThat( tokens( analyzer, "SU-11248" ) ).containsExactly( "su11248" );
        }
    }

    @Test
    void analyzerStillStemsOrdinaryWords() throws IOException {
        try ( Analyzer analyzer = OntologyAnalyzers.english( Collections.emptySet() ) ) {
            // The Porter chain must be intact: wrapping is not supposed to change English analysis.
            assertThat( tokens( analyzer, "treatments" ) ).containsExactly( "treatment" );
            assertThat( tokens( analyzer, "the brain" ) ).containsExactly( "brain" );
        }
    }

    private static List<String> tokens( Analyzer analyzer, String text ) throws IOException {
        List<String> out = new ArrayList<>();
        try ( TokenStream ts = analyzer.tokenStream( "text", text ) ) {
            CharTermAttribute term = ts.addAttribute( CharTermAttribute.class );
            ts.reset();
            while ( ts.incrementToken() ) {
                out.add( term.toString() );
            }
            ts.end();
        }
        return out;
    }
}
