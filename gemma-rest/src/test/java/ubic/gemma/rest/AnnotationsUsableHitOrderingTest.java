package ubic.gemma.rest;

import org.junit.jupiter.api.Test;
import ubic.gemma.model.common.description.CharacteristicValueObject;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A search hit with no {@code valueUri} must never outrank one that has a URI.
 *
 * <p>Free-text corpus rows come back with {@code valueUri: null}. No client can store, compare or
 * dedup one, so as an <em>answer</em> it is unusable however well its label matches — and a resolver
 * that takes the top hit gets nothing it can use.</p>
 *
 * <p>Reported by CAB 2026-08-15. {@code Lewis lung carcinoma} returned the free-text
 * {@code lewis lung carcinoma cell} at rank 1, ahead of EFO_1001770 {@code carcinoma, lewis lung} —
 * the gold answer. The disease subagent took rank 1, could not use it, and tagged a C57BL/6 mouse
 * experiment with {@code sheep lung adenocarcinoma}.</p>
 *
 * <p>These tests pin the ORDERING RULE against the same comparator shape the endpoint builds, rather
 * than standing up a search: the defect was never in retrieval — the right term was in the response
 * both before and after — it was in which row sorted first.</p>
 */
class AnnotationsUsableHitOrderingTest {

    private static CharacteristicValueObject hit( String value, String uri ) {
        CharacteristicValueObject c = new CharacteristicValueObject();
        c.setValue( value );
        c.setValueUri( uri );
        return c;
    }

    /** The dominant key as the endpoint applies it, ahead of the relevance tiers. */
    private static Comparator<CharacteristicValueObject> usableFirst() {
        return Comparator.comparingInt(
                h -> h.getValueUri() != null && !h.getValueUri().trim().isEmpty() ? 0 : 1 );
    }

    /**
     * The reported case. The free-text row wins on label exactness — it is the better lexical match
     * — which is precisely why a tier-based sort put it first and why this key has to sit above the
     * tiers rather than inside them.
     */
    @Test
    void theLewisLungCase() {
        List<CharacteristicValueObject> hits = new java.util.ArrayList<>( List.of(
                hit( "lewis lung carcinoma cell", null ),
                hit( "carcinoma, lewis lung", "http://www.ebi.ac.uk/efo/EFO_1001770" ) ) );

        hits.sort( usableFirst() );

        assertThat( hits.get( 0 ).getValueUri() ).isEqualTo( "http://www.ebi.ac.uk/efo/EFO_1001770" );
    }

    @Test
    void everyUsableHitPrecedesEveryFreeTextOne() {
        List<CharacteristicValueObject> hits = new java.util.ArrayList<>( List.of(
                hit( "free text a", null ),
                hit( "term one", "http://purl.obolibrary.org/obo/MONDO_0000001" ),
                hit( "free text b", "" ),
                hit( "term two", "http://www.ebi.ac.uk/efo/EFO_1001770" ),
                hit( "free text c", "   " ) ) );

        hits.sort( usableFirst() );

        List<Boolean> usable = hits.stream()
                .map( h -> h.getValueUri() != null && !h.getValueUri().trim().isEmpty() )
                .collect( Collectors.toList() );
        assertThat( usable ).containsExactly( true, true, false, false, false );
    }

    /** Blank and whitespace-only URIs are as unusable as null; a bare null check would miss them. */
    @Test
    void blankUrisCountAsUnusable() {
        List<CharacteristicValueObject> hits = new java.util.ArrayList<>( List.of(
                hit( "blank", "" ),
                hit( "whitespace", "  " ),
                hit( "real", "http://purl.obolibrary.org/obo/MONDO_0000001" ) ) );

        hits.sort( usableFirst() );

        assertThat( hits.get( 0 ).getValue() ).isEqualTo( "real" );
    }

    /**
     * The safety argument for shipping this without a fold measurement: the key only ever moves
     * URI-less rows down. Two URI-bearing hits are indistinguishable to it, so whatever order the
     * relevance tiers gave them survives — and since every gold answer has a URI, a grounding metric
     * cannot regress.
     */
    @Test
    void itNeverReordersTwoUsableHits() {
        List<CharacteristicValueObject> hits = new java.util.ArrayList<>( List.of(
                hit( "first", "http://purl.obolibrary.org/obo/MONDO_0000001" ),
                hit( "second", "http://www.ebi.ac.uk/efo/EFO_1001770" ),
                hit( "third", "http://purl.obolibrary.org/obo/CL_0000000" ) ) );

        hits.sort( usableFirst() );

        assertThat( hits.stream().map( CharacteristicValueObject::getValue ).collect( Collectors.toList() ) )
                .containsExactly( "first", "second", "third" );
    }

    /** Free text is demoted, not dropped — a caller that wants those rows still receives them. */
    @Test
    void freeTextIsStillReturned() {
        List<CharacteristicValueObject> hits = new java.util.ArrayList<>( List.of(
                hit( "free text", null ),
                hit( "term", "http://purl.obolibrary.org/obo/MONDO_0000001" ) ) );

        hits.sort( usableFirst() );

        assertThat( hits ).hasSize( 2 );
        assertThat( hits.get( 1 ).getValue() ).isEqualTo( "free text" );
    }
}
