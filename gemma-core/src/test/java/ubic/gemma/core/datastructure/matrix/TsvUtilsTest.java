package ubic.gemma.core.datastructure.matrix;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ubic.gemma.core.datastructure.matrix.io.TsvUtils.format;

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
}