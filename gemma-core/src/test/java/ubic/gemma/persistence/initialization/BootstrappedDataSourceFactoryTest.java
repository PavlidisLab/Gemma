package ubic.gemma.persistence.initialization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ubic.gemma.persistence.initialization.BootstrappedDataSourceFactory.stripPathComponent;

public class BootstrappedDataSourceFactoryTest {

    @Test
    public void test() {
        assertEquals( "jdbc:mysql://test@foo?a=b", stripPathComponent( "jdbc:mysql://test@foo/bleh?a=b" ) );
        assertEquals( "jdbc:mysql://test@foo", stripPathComponent( "jdbc:mysql://test@foo/bleh" ) );
        assertEquals( "jdbc:mysql://test@foo?a=b", stripPathComponent( "jdbc:mysql://test@foo?a=b" ) );
    }
}