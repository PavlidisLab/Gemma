package ubic.gemma.core.util.test;

import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * Extensions for AssertJ's {@link org.assertj.core.util.Maps}.
 */
public class Maps {

    public static <K, V> Map<K, V> map( K key, V value, Object... keyValues ) {
        Assert.isTrue( keyValues.length % 2 == 0, "You must provide an even number of key-value pairs" );
        return new HashMap<K, V>( 1 + keyValues.length / 2 ) {{
            put( key, value );
            for ( int i = 0; i < keyValues.length; i += 2 ) {
                //noinspection unchecked
                put( ( K ) keyValues[i], ( V ) keyValues[i + 1] );
            }
        }};
    }
}
