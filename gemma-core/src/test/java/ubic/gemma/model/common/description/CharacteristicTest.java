package ubic.gemma.model.common.description;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;
import ubic.gemma.model.expression.experiment.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

public class CharacteristicTest {

    @Test
    public void testEquals() {
        Characteristic a = createTransientCharacteristic( "a", null );
        Characteristic A = createTransientCharacteristic( "A", null );
        assertThat( createTransientCharacteristic( "a", null ) )
                .isEqualTo( a )
                .hasSameHashCodeAs( a )
                .isEqualByComparingTo( a )
                .isEqualTo( A )
                .hasSameHashCodeAs( A )
                .isEqualByComparingTo( A )
                .isNotEqualTo( createTransientCharacteristic( "a", null, "foo", null ) )
                .isNotEqualByComparingTo( createTransientCharacteristic( "a", null, "foo", null ) );
    }

    @Test
    public void testComparator() {
        List<Characteristic> cs = Arrays.asList(
                createCharacteristic( "d", "D" ),
                createCharacteristic( "e", null ),
                createCharacteristic( "a", null ),
                createCharacteristic( "A", null ),
                createCharacteristic( "b", null ),
                createCharacteristic( "C", null ),
                createCharacteristic( null, "TEST" ),
                createCharacteristic( null, "test" ),
                createCharacteristic( null, null )
        );
        SortedSet<Characteristic> sortedCs = new TreeSet<>( Characteristic.getByCategoryAndValueComparator() );
        sortedCs.addAll( cs );
        assertThat( sortedCs )
                .extracting( "valueUri", "value" )
                .containsExactly(
                        Tuple.tuple( "d", "D" ),
                        Tuple.tuple( null, "TEST" ),
                        Tuple.tuple( "a", null ),
                        Tuple.tuple( "b", null ),
                        Tuple.tuple( "C", null ),
                        Tuple.tuple( "e", null ),
                        Tuple.tuple( null, null )
                );
    }

    private static Characteristic createTransientCharacteristic( @Nullable String valueUri, @Nullable String value, @Nullable String categoryUri, @Nullable String category ) {
        Characteristic c = new Characteristic();
        c.setValueUri( valueUri );
        c.setValue( value );
        c.setCategoryUri( categoryUri );
        c.setCategory( category );
        return c;
    }

    /**
     * Free-text term fields carry the submitter's spacing. In production 13,179 characteristic
     * values held an internal double space and 12,861 of those had the run in the submitter's own
     * originalValue, so the input reproduces them on every import; a further 4,406 carried edge
     * whitespace. MySQL's PAD SPACE collation hides a TRAILING space from `=` but gives internal
     * runs no cover, so two spellings of one value split under GROUP BY, joins and every
     * exact-label comparison. Normalize where every writer passes.
     */
    @Test
    public void termTextIsNormalizedOnWrite() {
        Characteristic c = Characteristic.Factory.newInstance();
        c.setValue( "  cancer cell line " );
        c.setCategory( "cell  line" );
        assertThat( c.getValue() ).isEqualTo( "cancer cell line" );
        assertThat( c.getCategory() ).isEqualTo( "cell line" );

        // A statement's subject aliases value via super.setValue(), and its objects are the same
        // kind of third-party text (151 with edge whitespace in production).
        Statement s = Statement.Factory.newInstance();
        s.setSubject( "high  fat  diet" );
        s.setObject( " chow " );
        s.setSecondObject( "two  weeks" );
        assertThat( s.getSubject() ).isEqualTo( "high fat diet" );
        assertThat( s.getValue() ).isEqualTo( "high fat diet" );
        assertThat( s.getObject() ).isEqualTo( "chow" );
        assertThat( s.getSecondObject() ).isEqualTo( "two weeks" );
    }

    /**
     * The no-break spaces are the ones a normalizer quietly misses. Java does not classify
     * U+202F or U+2007 as whitespace, so StringUtils.normalizeSpace leaves them alone; and
     * although it maps U+00A0 to a plain space it does not re-collapse afterwards, so a run of
     * them comes back as a run of ORDINARY double spaces -- the normalizer emitting the exact
     * defect it exists to remove. Production carries 2,392 values with U+00A0 and 5 with U+202F.
     */
    @Test
    public void noBreakSpacesAreNormalizedToo() {
        Characteristic c = Characteristic.Factory.newInstance();

        c.setValue( "high\u00A0fat diet" );
        assertThat( c.getValue() ).isEqualTo( "high fat diet" );

        c.setValue( "high\u202Ffat diet" );
        assertThat( c.getValue() ).isEqualTo( "high fat diet" );

        // The re-collapse case: two NBSPs must not become two spaces.
        c.setValue( "high\u00A0\u00A0fat diet" );
        assertThat( c.getValue() ).isEqualTo( "high fat diet" );

        // NBSP beside an ordinary space, the other way the run appears.
        c.setValue( "high\u00A0 fat diet" );
        assertThat( c.getValue() ).isEqualTo( "high fat diet" );

        // A trailing NBSP is edge whitespace like any other.
        c.setValue( "high fat diet\u00A0" );
        assertThat( c.getValue() ).isEqualTo( "high fat diet" );
    }

    /** Null must survive as null: "no value" stays distinct from "blank". */
    @Test
    public void nullTermTextStaysNull() {
        Characteristic c = Characteristic.Factory.newInstance();
        c.setCategory( null );
        assertThat( c.getCategory() ).isNull();
    }

    private static Characteristic createTransientCharacteristic( @Nullable String valueUri, @Nullable String value ) {
        return createTransientCharacteristic( valueUri, value, null, null );
    }

    private static long i = 0L;

    private static Characteristic createCharacteristic( @Nullable String valueUri, @Nullable String value ) {
        Characteristic c = createTransientCharacteristic( valueUri, value );
        c.setId( ++i ); // to mimic different terms being aggregated by value/value URI
        return c;
    }

    @Test
    public void testACharacteristicSurvivesBeingReTermedInsideASet() {
        // 🛑 The regression this pins. Before the constant hashCode, hashing category/value meant a
        // re-term moved the element to a bucket computed from its OLD value, and the set could no
        // longer find an element that was demonstrably in it. Gemma holds tags in a HashSet and
        // curation edits them in place, so this is the shape of a real corruption, not a contrived
        // one. With the content hash, every assertion below except the first fails.
        Characteristic tag = createTransientCharacteristic( null, "CBA/J", null, "strain" );
        tag.setId( 4242L );
        Set<Characteristic> tags = new HashSet<>();
        tags.add( tag );
        assertThat( tags ).contains( tag );

        // the re-term a curator performs
        tag.setValue( "C57BL/6J" );

        assertThat( tags ).as( "the set still finds it after the value changed" ).contains( tag );
        assertThat( tags.remove( tag ) ).as( "and can still remove it" ).isTrue();
        assertThat( tags ).isEmpty();
    }

    @Test
    public void testEqualsAndHashCodeAgreeForTwoInstancesOfOneRow() {
        // The contract violation, independent of mutation: equals() matches by id when both sides
        // have one, so two instances of the same row are equal even when their content has drifted.
        // A content hash gave them different hashCodes, which is the contract broken outright.
        Characteristic a = createTransientCharacteristic( null, "CBA/J", null, "strain" );
        Characteristic b = createTransientCharacteristic( null, "C57BL/6J", null, "strain" );
        a.setId( 77L );
        b.setId( 77L );
        assertThat( a ).isEqualTo( b ).hasSameHashCodeAs( b );
    }

}