package ubic.gemma.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The three corpus strings uib read out of production, plus the cases where doing nothing is the
 * correct behaviour.
 *
 * <p>🛑 The inputs are written as {@code \\uF0xx} escapes on purpose. A private-use codepoint pasted
 * in literally is invisible in a diff, survives a careless editor badly, and reads as an ordinary
 * space to a reviewer &mdash; which is the entire defect this class exists for.</p>
 *
 * @see SymbolFontPua
 */
public class SymbolFontPuaTest {

    @Test
    @DisplayName("the three strings read out of the real corpus repair to what they were meant to say")
    public void repairsTheCorpusCases() {
        // GSE245831 — the one that started this: "A▢ burden" where it should read "Aβ burden"
        assertThat( SymbolFontPua.repair( "restores A burden and influ" ) )
                .isEqualTo( "restores Aβ burden and influ" );
        // GSE228157 — Delta, slash, Delta
        assertThat( SymbolFontPua.repair( "ADAM17adipoq-cre" ) )
                .isEqualTo( "ADAM17adipoq-creΔ/Δ" );
        // GSE241044 — gamma, period, space. The space is a PUA one too and has to become a real space.
        assertThat( SymbolFontPua.repair( "IFN-" ) )
                .isEqualTo( "IFN-γ. " );
    }

    @Test
    @DisplayName("clean text comes back as the SAME instance, since almost every line is clean")
    public void leavesCleanTextAlone() {
        String clean = "Bronchoalveolar lavage, 10 samples";
        assertThat( SymbolFontPua.repair( clean ) ).isSameAs( clean );
        assertThat( SymbolFontPua.isAffected( clean ) ).isFalse();
        assertThat( SymbolFontPua.repair( null ) ).isNull();
    }

    @Test
    @DisplayName("real Greek is left alone — only the private-use block is touched")
    public void doesNotTouchRealGreek() {
        String alreadyRight = "Aβ burden, IFN-γ, TNF-α";
        assertThat( SymbolFontPua.repair( alreadyRight ) ).isSameAs( alreadyRight );
    }

    @Test
    @DisplayName("a private-use codepoint with no standard equivalent is REPORTED, never guessed")
    public void reportsRatherThanGuesses() {
        // 0xF0 is the Apple logo: private-use in the font too, so there is nothing to map it to.
        String withApple = "vendor  tool";
        assertThat( SymbolFontPua.repair( withApple ) ).isEqualTo( withApple );
        assertThat( SymbolFontPua.unmappable( withApple ) ).containsExactly( 0xF0F0 );
        // a repairable one is not reported as needing a human
        assertThat( SymbolFontPua.unmappable( "A" ) ).isEmpty();
    }

    @Test
    @DisplayName("U+FFFD is out of scope: the original byte is already gone")
    public void ignoresTheReplacementCharacter() {
        String lost = "already � lost";
        assertThat( SymbolFontPua.repair( lost ) ).isSameAs( lost );
        assertThat( SymbolFontPua.unmappable( lost ) ).isEmpty();
    }

    @Test
    @DisplayName("every position uib resolved from the Adobe AFM agrees with the font-derived table")
    public void agreesWithTheIndependentlyResolvedPositions() {
        // the fourteen they asserted, in one string each for the letter and punctuation halves
        assertThat( SymbolFontPua.repair( "" ) )
                .isEqualTo( "βαγκΔεωσ" );
        assertThat( SymbolFontPua.repair( "" ) )
                .isEqualTo( " −/.±¬" );
    }

    @Test
    @DisplayName("no replacement is itself a private-use codepoint — that would swap one box for another")
    public void neverReplacesOnePrivateUseCharacterWithAnother() {
        // Adobe's own table assigns 29 positions into the Corporate Use Subarea, so a table built
        // from it alone would "repair" U+F0BE to U+F8E7 and change nothing a reader can see. This
        // pins the rule the table is generated under.
        for ( char c = 0xF020; c <= 0xF0FE; c++ ) {
            String one = String.valueOf( c );
            String repaired = SymbolFontPua.repair( one );
            if ( repaired.equals( one ) ) {
                continue; // left alone on purpose; the five unmappable positions land here
            }
            char out = repaired.charAt( 0 );
            assertThat( out >= 0xE000 && out <= 0xF8FF )
                    .withFailMessage( "U+%04X was replaced by U+%04X, which is still private-use",
                            ( int ) c, ( int ) out )
                    .isFalse();
        }
    }

    @Test
    @DisplayName("the four positions the Adobe table corrected in the font-derived one")
    public void prefersTheAuthoritativeAdobeMapping() {
        assertThat( SymbolFontPua.repair( "\uF0A2" ) ).isEqualTo( "\u2032" ); // PRIME, not modifier prime
        assertThat( SymbolFontPua.repair( "\uF0B2" ) ).isEqualTo( "\u2033" ); // DOUBLE PRIME
        assertThat( SymbolFontPua.repair( "\uF027" ) ).isEqualTo( "\u220B" ); // CONTAINS AS MEMBER
        assertThat( SymbolFontPua.repair( "\uF06D" ) ).isEqualTo( "\u03BC" ); // Greek mu, not the micro sign
    }

    @Test
    @DisplayName("U+F0BE, which uib declined to resolve, maps to the line-extension the font names")
    public void resolvesTheExtenderTheFontKnowsAbout() {
        assertThat( SymbolFontPua.repair( "" ) ).isEqualTo( "⎯" );
    }
}
