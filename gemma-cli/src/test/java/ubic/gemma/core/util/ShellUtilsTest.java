package ubic.gemma.core.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ubic.gemma.core.util.ShellUtils.quoteIfNecessary;

public class ShellUtilsTest {

    @Test
    public void test() {
        assertEquals( "' '", quoteIfNecessary( " " ) );
        assertEquals( "' a '", quoteIfNecessary( " a " ) );
    }
}