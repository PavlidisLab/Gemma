package ubic.gemma.cli.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ubic.gemma.cli.util.ShellUtils.quoteIfNecessary;

public class ShellUtilsTest {

    @Test
    public void test() {
        assertEquals( "' '", quoteIfNecessary( " " ) );
        assertEquals( "' a '", quoteIfNecessary( " a " ) );
    }
}