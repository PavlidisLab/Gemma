package ubic.gemma.persistence.initialization;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BootstrappedDataSourceInitializerTest {

    @Test
    public void test() {
        BootstrappedDataSourceInitializer initializer = new BootstrappedDataSourceInitializer();
        assertEquals( "jdbc:mysql://test@foo?a=b", initializer.stripPathComponent( "jdbc:mysql://test@foo/bleh?a=b" ) );
        assertEquals( "jdbc:mysql://test@foo", initializer.stripPathComponent( "jdbc:mysql://test@foo/bleh" ) );
        assertEquals( "jdbc:mysql://test@foo?a=b", initializer.stripPathComponent( "jdbc:mysql://test@foo?a=b" ) );
    }
}