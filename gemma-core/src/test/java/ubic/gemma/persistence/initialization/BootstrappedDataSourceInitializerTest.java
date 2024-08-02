package ubic.gemma.persistence.initialization;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BootstrappedDataSourceInitializerTest {

    @Test
    public void test() {
        assertEquals( "jdbc:mysql://test@foo?a=b", BootstrappedDataSourceFactory.stripPathComponent( "jdbc:mysql://test@foo/bleh?a=b" ) );
        assertEquals( "jdbc:mysql://test@foo", BootstrappedDataSourceFactory.stripPathComponent( "jdbc:mysql://test@foo/bleh" ) );
        assertEquals( "jdbc:mysql://test@foo?a=b", BootstrappedDataSourceFactory.stripPathComponent( "jdbc:mysql://test@foo?a=b" ) );
    }
}