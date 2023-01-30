package ubic.gemma.model.common.description;

import org.assertj.core.groups.Tuple;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.*;

public class CharacteristicTest {

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
                        Tuple.tuple( "a", null ),
                        Tuple.tuple( "b", null ),
                        Tuple.tuple( "C", null ),
                        Tuple.tuple( "d", "D" ),
                        Tuple.tuple( "e", null ),
                        Tuple.tuple( null, "TEST" ),
                        Tuple.tuple( null, null )
                );
    }

    private static long i = 0L;

    private static Characteristic createCharacteristic( @Nullable String valueUri, @Nullable String value ) {
        Characteristic c = new Characteristic();
        c.setId( ++i ); // to mimic different terms being aggregated by value/value URI
        c.setValueUri( valueUri );
        c.setValue( value );
        return c;
    }

}