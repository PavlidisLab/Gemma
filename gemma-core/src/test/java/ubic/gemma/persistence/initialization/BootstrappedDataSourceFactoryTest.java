package ubic.gemma.persistence.initialization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ubic.gemma.persistence.initialization.BootstrappedDataSourceFactory.stripPathComponent;

public class BootstrappedDataSourceFactoryTest {

    @Test
    public void test() {
        // The '/' before the query must survive, otherwise Connector/J can't parse the
        // authority and falls back to localhost.
        assertEquals( "jdbc:mysql://test@foo/?a=b", stripPathComponent( "jdbc:mysql://test@foo/bleh?a=b" ) );
        assertEquals( "jdbc:mysql://test@foo", stripPathComponent( "jdbc:mysql://test@foo/bleh" ) );
        assertEquals( "jdbc:mysql://test@foo?a=b", stripPathComponent( "jdbc:mysql://test@foo?a=b" ) );

        // Real testdb URLs: host without an explicit port (the case that previously
        // mis-parsed to localhost), and with a port.
        assertEquals( "jdbc:mysql://127.0.0.1/?rewriteBatchedStatements=true",
                stripPathComponent( "jdbc:mysql://127.0.0.1/gemdtest?rewriteBatchedStatements=true" ) );
        assertEquals( "jdbc:mysql://127.0.0.1:3306/?rewriteBatchedStatements=true",
                stripPathComponent( "jdbc:mysql://127.0.0.1:3306/gemdtest?rewriteBatchedStatements=true" ) );
    }
}