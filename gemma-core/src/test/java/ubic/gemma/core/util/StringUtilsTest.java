package ubic.gemma.core.util;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;
import static ubic.gemma.core.util.StringUtils.abbreviateInBytes;
import static ubic.gemma.core.util.StringUtils.abbreviateWithSuffix;

public class StringUtilsTest {

    @Test
    public void testAbbreviate() {
        assertNull( abbreviateInBytes( null, "…", 4, StandardCharsets.UTF_8 ) );
        assertEquals( "test", abbreviateInBytes( "test", "…", 4, StandardCharsets.UTF_8 ) );
        assertEquals( "t…", abbreviateInBytes( "test 23", "…", 4, StandardCharsets.UTF_8 ) );
        assertEquals( "µ…", abbreviateInBytes( "µµµ", "…", 5, StandardCharsets.UTF_8 ) );
        assertEquals( "µµ", abbreviateInBytes( "µµµ", "", 5, StandardCharsets.UTF_8 ) );
        assertThrows( IllegalArgumentException.class, () -> abbreviateInBytes( "test", "…", -1, StandardCharsets.UTF_8 ) );
        assertThrows( IllegalArgumentException.class, () -> abbreviateInBytes( "test", "…", 2, StandardCharsets.UTF_8 ) );
        assertThrows( IllegalArgumentException.class, () -> abbreviateInBytes( "test", "…", 3, StandardCharsets.UTF_8 ) );
    }

    @Test
    public void testAbbreviateUtf16() {
        assertEquals( "t…", abbreviateInBytes( "test", "…", 4, StandardCharsets.UTF_16 ) );
    }

    @Test
    public void testAbbreviateWithSuffix() {
        assertEquals( "tes… suffix", abbreviateWithSuffix( "test12313", " suffix", "…", 13, true, StandardCharsets.UTF_8 ) );
        assertThrows( IllegalArgumentException.class, () -> abbreviateWithSuffix( "test12313", " suffix", "…", 5, true, StandardCharsets.UTF_8 ) );
    }

    @Test
    public void testAppendWithDelimiter() {
        assertEquals( "foo bar", StringUtils.appendWithDelimiter( "foo", "bar" ) );
        assertEquals( "foo bar", StringUtils.appendWithDelimiter( "foo ", "bar" ) );
        assertEquals( "foo  bar", StringUtils.appendWithDelimiter( "foo  ", "bar" ) );
        assertEquals( "bar", StringUtils.appendWithDelimiter( "", "bar" ) );
        assertEquals( " bar", StringUtils.appendWithDelimiter( " ", "bar" ) );
        assertEquals( "bar", StringUtils.appendWithDelimiter( null, "bar" ) );
    }

    @Test
    public void testMakeUnique() {
        assertArrayEquals( new String[] { "foo", "foo.1" }, StringUtils.makeUnique( new String[] { "foo", "foo" } ) );
        assertArrayEquals( new String[] { "foo", "bar", "foo.1", "foo.2" }, StringUtils.makeUnique( new String[] { "foo", "bar", "foo", "foo" } ) );
    }
}