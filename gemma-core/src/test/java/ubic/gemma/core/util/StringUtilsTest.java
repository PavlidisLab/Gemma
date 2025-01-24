package ubic.gemma.core.util;

import org.junit.Test;

import static org.junit.Assert.*;
import static ubic.gemma.core.util.StringUtils.abbreviateInUTF8Bytes;
import static ubic.gemma.core.util.StringUtils.abbreviateWithSuffixInUTF8Bytes;

public class StringUtilsTest {

    @Test
    public void test() {
        assertNull( abbreviateInUTF8Bytes( null, "…", 4 ) );
        assertEquals( "test", abbreviateInUTF8Bytes( "test", "…", 4 ) );
        assertEquals( "t…", abbreviateInUTF8Bytes( "test 23", "…", 4 ) );
        assertEquals( "µ…", abbreviateInUTF8Bytes( "µµµ", "…", 5 ) );
        assertEquals( "µµ", abbreviateInUTF8Bytes( "µµµ", "", 5 ) );
        assertThrows( IllegalArgumentException.class, () -> abbreviateInUTF8Bytes( "test", "…", -1 ) );
        assertThrows( IllegalArgumentException.class, () -> abbreviateInUTF8Bytes( "test", "…", 2 ) );
        assertThrows( IllegalArgumentException.class, () -> abbreviateInUTF8Bytes( "test", "…", 3 ) );
    }

    @Test
    public void testAbbreviateWithSuffix() {
        assertEquals( "tes… suffix", abbreviateWithSuffixInUTF8Bytes( "test12313", " suffix", "…", 13 ) );
    }
}