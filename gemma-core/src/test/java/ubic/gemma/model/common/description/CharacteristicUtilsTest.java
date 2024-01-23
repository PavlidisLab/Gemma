package ubic.gemma.model.common.description;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CharacteristicUtilsTest {

    @Test
    public void test() {
        // terms with identical URIs are collapsed
        assertEquals( 0, CharacteristicUtils.compareTerm( "a", "test", "b", "test" ) );
        // terms with different URIs are compared by label
        assertEquals( -1, CharacteristicUtils.compareTerm( "a", "test", "b", "bar" ) );
        assertEquals( 1, CharacteristicUtils.compareTerm( "b", "test", "a", "bar" ) );
    }

}