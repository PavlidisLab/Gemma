package ubic.gemma.core.util;

import org.junit.Test;

import static org.junit.Assert.*;
import static ubic.gemma.core.util.TsvUtils.*;

public class TsvUtilsTest {

    @Test
    public void testFormatNumber() {
        assertEquals( "0.0", format( 0.0 ) );
        assertEquals( "0.0", format( -0.0 ) );
        assertEquals( "1E-14", format( 1e-14 ) );
        assertEquals( "-1E-14", format( -1e-14 ) );
        assertEquals( "0.1111", format( 0.1111 ) );
        assertEquals( "0.0001", format( 0.0001 ) );
        assertEquals( "1E-5", format( 1e-5 ) );
        assertEquals( "1000.0", format( 1e3 ) );
        assertEquals( "1000.0", format( 1000.0 ) );
        assertEquals( "1234.5", format( 1234.5 ) );
        assertEquals( "100000.0", format( 1e5 ) );
        assertEquals( "-100000.0", format( -1e5 ) );
        assertEquals( "100.1234", format( 100.1234 ) );
        assertEquals( "-100.1234", format( -100.1234 ) );
        assertEquals( "1000.1", format( 1000.1234 ) );
        assertEquals( "100000.1", format( 100000.1234 ) );
        assertEquals( "", format( ( Double ) null ) );
        assertEquals( "", format( Double.NaN ) );
        assertEquals( "inf", format( Double.POSITIVE_INFINITY ) );
        assertEquals( "-inf", format( Double.NEGATIVE_INFINITY ) );
        assertEquals( "", format( ( Object ) Double.NaN ) );
    }

    @Test
    public void testFormatLong() {
        assertEquals( "10", format( 10L ) );
    }

    @Test
    public void testFormat() {
        assertEquals( "\\t\\n\\r\\\\", format( "\t\n\r\\" ) );
        assertEquals( "", format( ( String ) null ) );
    }

    @Test
    public void testParse() {
        assertNull( parseInt( "" ) );
        assertNull( parseInt( null ) );
        assertEquals( ( Integer ) 12, parseInt( "12" ) );
        assertEquals( ( Long ) 12L, parseLong( "12" ) );
        assertEquals( Boolean.TRUE, parseBoolean( "true" ) );
        assertEquals( Boolean.FALSE, parseBoolean( "false" ) );
    }

    @Test
    public void testParseDouble() {
        assertEquals( 1e-5, parseDouble( "1E-5" ), Double.MIN_VALUE );
        assertEquals( 1e3, parseDouble( "1000.0" ), Double.MIN_VALUE );
        assertEquals( 1000.0, parseDouble( "1000.0" ), Double.MIN_VALUE );
        assertEquals( 1234.5, parseDouble( "1234.5" ), Double.MIN_VALUE );
        assertEquals( 1e5, parseDouble( "100000.0" ), Double.MIN_VALUE );
        assertEquals( -1e5, parseDouble( "-100000.0" ), Double.MIN_VALUE );
        assertEquals( 100.1234, parseDouble( "100.1234" ), Double.MIN_VALUE );
        assertEquals( -100.1234, parseDouble( "-100.1234" ), Double.MIN_VALUE );
        assertTrue( Double.isNaN( parseDouble( "" ) ) );
        assertTrue( Double.isNaN( parseDouble( "NaN" ) ) );
        assertEquals( Double.POSITIVE_INFINITY, parseDouble( "inf" ), Double.MIN_VALUE );
        assertEquals( Double.NEGATIVE_INFINITY, parseDouble( "-inf" ), Double.MIN_VALUE );
        assertEquals( Double.POSITIVE_INFINITY, parseDouble( "Infinity" ), Double.MIN_VALUE );
        assertEquals( Double.NEGATIVE_INFINITY, parseDouble( "-Infinity" ), Double.MIN_VALUE );
    }

    @Test
    public void testFormatComment() {
        assertEquals( "# abc", formatComment( "abc" ) );
        assertEquals( "# abc\n# def", formatComment( "abc\ndef" ) );
        assertEquals( "# abc", formatComment( "# abc" ) );
        assertEquals( "# abc\n# def", formatComment( "# abc\n# def" ) );
        assertEquals( "# abc\n# def\n# test\n#ghi", formatComment( "# abc\n# def\ntest\n#ghi" ) );
    }
}