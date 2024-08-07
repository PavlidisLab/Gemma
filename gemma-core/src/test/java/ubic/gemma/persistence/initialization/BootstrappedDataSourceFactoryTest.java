package ubic.gemma.persistence.initialization;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ubic.gemma.persistence.initialization.BootstrappedDataSourceFactory.stripPathComponent;

public class BootstrappedDataSourceFactoryTest {

    @Test
    public void test() {
        assertEquals( "jdbc:mysql://test@foo?a=b", stripPathComponent( "jdbc:mysql://test@foo/bleh?a=b" ) );
        assertEquals( "jdbc:mysql://test@foo", stripPathComponent( "jdbc:mysql://test@foo/bleh" ) );
        assertEquals( "jdbc:mysql://test@foo?a=b", stripPathComponent( "jdbc:mysql://test@foo?a=b" ) );
    }
}