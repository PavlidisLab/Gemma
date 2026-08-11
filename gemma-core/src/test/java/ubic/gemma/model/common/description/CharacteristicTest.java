package ubic.gemma.model.common.description;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;
import ubic.gemma.model.expression.experiment.Statement;
import java.util.Arrays;
import java.util.List;
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

}