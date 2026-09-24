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
        assertThat( OntologyAnalyzers.foldCodeRuns( "vitamin D3" ) ).isEqualTo( "vitamin D3" );
        assertThat( OntologyAnalyzers.foldCodeRuns( "high fat diet" ) ).isEqualTo( "high fat diet" );
    }

    /**
     * The three- rather than five-character prefix cap on the short-run fold is what keeps these
     * whole. Each is a word carrying a small number, and each is one a reader would search for by
     * its words: welding {@code type 2} would leave {@code type} unable to match anything.
     */
    @Test
    void aWordCarryingASmallNumberIsNotADesignation() {
        assertThat( OntologyAnalyzers.foldCodeRuns( "type 2 diabetes mellitus" ) )
                .isEqualTo( "type 2 diabetes mellitus" );
        assertThat( OntologyAnalyzers.foldCodeRuns( "helper T cell type 1" ) )
                .isEqualTo( "helper T cell type 1" );
        assertThat( OntologyAnalyzers.foldCodeRuns( "group 1 innate lymphoid cell" ) )
                .isEqualTo( "group 1 innate lymphoid cell" );
        assertThat( OntologyAnalyzers.foldCodeRuns( "Angle class 2 malocclusion" ) )
                .isEqualTo( "Angle class 2 malocclusion" );
        assertThat( OntologyAnalyzers.foldCodeRuns( "grade 3" ) ).isEqualTo( "grade 3" );
        assertThat( OntologyAnalyzers.foldCodeRuns( "core-binding factor subunit alpha-2 inhibitor" ) )
                .isEqualTo( "core-binding factor subunit alpha-2 inhibitor" );
    }

    /**
     * The case the short-run fold exists for: CLO labels every cell-line class {@code <name> cell},
     * and a submitter writes the name without CLO's hyphen. Before this, {@code MEC1} reached
     * EFO's same-named term and never CLO's — and only CLO carries the disease restrictions.
     */
    @Test
    void aCellLineReachesItsHyphenatedLabel() {
        assertThat( OntologyAnalyzers.foldCodeRuns( "MEC-1 cell" ) ).isEqualTo( "MEC1 cell" );
        assertThat( OntologyAnalyzers.foldCodeRuns( "MEC-2 cell" ) ).isEqualTo( "MEC2 cell" );
        assertThat( OntologyAnalyzers.foldCodeRuns( "A-10 cell" ) ).isEqualTo( "A10 cell" );
        assertThat( OntologyAnalyzers.foldCodeRuns( "ABC-1 cell" ) ).isEqualTo( "ABC1 cell" );
        // CLO spells the separator both ways; both have to land on the submitter's spelling.
        assertThat( OntologyAnalyzers.foldCodeRuns( "AE 1 cell" ) )
                .isEqualTo( OntologyAnalyzers.foldCodeRuns( "AE-1 cell" ) )
                .isEqualTo( "AE1 cell" );
        // Same shape, other vocabularies: a marker, and a trial code the digit floor was missing.
        assertThat( OntologyAnalyzers.foldCodeRuns( "IL 6" ) )
                .isEqualTo( OntologyAnalyzers.foldCodeRuns( "IL-6" ) )
                .isEqualTo( "IL6" );
        assertThat( OntologyAnalyzers.foldCodeRuns( "H-89 dihydrochloride" ) )
                .isEqualTo( "H89 dihydrochloride" );
    }

    /**
     * A locant is the same shape as a cell line and must not fold. This is the constraint that
     * makes the short-run fold positional rather than a lower digit floor — a floor of one rewrites
     * 27% of CHEBI, which is how much of CHEBI is systematic nomenclature.
     */
    @Test
    void systematicChemicalNamesAreUntouched() {
        for ( String iupac : new String[] {
                "2-(1H-indol-3-yl)ethanamine",
                "pregn-4-ene-3,20-dione",
                "icosa-8,11,14-trien-5-ynoic acid",
                "1-cyclopropyl-6-fluoro-4-oxo-7-(piperazin-1-yl)quinoline-3-carboxylic acid",
                "(2R,3S)-2-aminooctadec-4-ene-1,3-diol" } ) {
            assertThat( OntologyAnalyzers.foldCodeRuns( iupac ) ).isEqualTo( iupac );
        }
        // Dotted enzyme numbering is excluded by the same bound: the digit is followed by a period.
        assertThat( OntologyAnalyzers.foldCodeRuns( "EC 1.5.1.3 (dihydrofolate reductase) inhibitor" ) )
                .isEqualTo( "EC 1.5.1.3 (dihydrofolate reductase) inhibitor" );
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
            // Both folds chained, through the real char-filter stack rather than the helper.
            assertThat( tokens( analyzer, "MEC-1 cell" ) ).isEqualTo( tokens( analyzer, "MEC1 cell" ) );
            assertThat( tokens( analyzer, "MEC-1 cell" ) ).containsExactly( "mec1", "cell" );
            assertThat( tokens( analyzer, "type 2 diabetes" ) ).containsExactly( "type", "2", "diabet" );
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
