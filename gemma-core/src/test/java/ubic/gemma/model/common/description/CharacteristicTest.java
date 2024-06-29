package ubic.gemma.model.common.description;

import org.assertj.core.groups.Tuple;
import org.junit.Test;
import ubic.gemma.core.lang.Nullable;

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