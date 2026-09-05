package ubic.gemma.model.common.auditAndSecurity.curation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The counts are a HINT read off a payload whose shape belongs to someone else, so what these pin is
 * mostly the failure behaviour: everything unrecognized has to come back null rather than zero.
 */
public class AnnotationSetPayloadCountsTest {

    @Test
    @DisplayName("counts the CurationDocument shape both sides speak")
    public void readsFactorsAndTags() {
        String payload = "{\"design\":{\"factors\":{\"items\":[{},{},{}]}},"
                + "\"tags\":{\"items\":[{},{}]}}";
        AnnotationSetPayloadCounts c = AnnotationSetPayloadCounts.of( payload );
        assertThat( c.getFactorCount() ).isEqualTo( 3 );
        assertThat( c.getTagCount() ).isEqualTo( 2 );
    }

    @Test
    @DisplayName("an empty array is 0 — the payload said so")
    public void emptyArraysAreZero() {
        AnnotationSetPayloadCounts c = AnnotationSetPayloadCounts.of(
                "{\"design\":{\"factors\":{\"items\":[]}},\"tags\":{\"items\":[]}}" );
        assertThat( c.getFactorCount() ).isZero();
        assertThat( c.getTagCount() ).isZero();
    }

    /**
     * 🛑 The distinction the whole class exists for. A shape this cannot read must not report 0,
     * because 0 reads as "this proposal changes nothing" — a wrong answer that looks plausible and
     * that a card would print without hesitating.
     */
    @Test
    @DisplayName("an unrecognized shape is null, NOT zero")
    public void unrecognizedShapeIsUnknownRatherThanZero() {
        // An audit payload: the shape 6 of 8 role=proposal rows actually carry.
        AnnotationSetPayloadCounts audit = AnnotationSetPayloadCounts.of(
                "{\"audit_proposal\":{},\"n_proposed\":4}" );
        assertThat( audit.getFactorCount() ).isNull();
        assertThat( audit.getTagCount() ).isNull();

        // Present but not an array.
        AnnotationSetPayloadCounts wrongType = AnnotationSetPayloadCounts.of(
                "{\"tags\":{\"items\":\"lots\"}}" );
        assertThat( wrongType.getTagCount() ).isNull();

        // Half a match: factors readable, tags absent. The readable half still answers.
        AnnotationSetPayloadCounts partial = AnnotationSetPayloadCounts.of(
                "{\"design\":{\"factors\":{\"items\":[{}]}}}" );
        assertThat( partial.getFactorCount() ).isEqualTo( 1 );
        assertThat( partial.getTagCount() ).isNull();
    }

    @Test
    @DisplayName("garbage and absence never throw — Gemma never promised to read this")
    public void malformedOrAbsentIsUnknown() {
        for ( String bad : new String[] { null, "", "not json at all", "[1,2,3]", "null", "{" } ) {
            AnnotationSetPayloadCounts c = AnnotationSetPayloadCounts.of( bad );
            assertThat( c.getFactorCount() ).as( "factors for %s", bad ).isNull();
            assertThat( c.getTagCount() ).as( "tags for %s", bad ).isNull();
        }
    }
}
