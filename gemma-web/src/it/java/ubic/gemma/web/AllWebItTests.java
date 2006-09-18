package ubic.gemma.web;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Integration tests for web interface
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AllWebItTests {

    public static Test suite() {
        TestSuite suite = new TestSuite( "Integration tests for web interface" );

        suite.addTestSuite( EditProfileTest.class );
        suite.addTestSuite( LoginTest.class );
        suite.addTestSuite( PubmedSearchWebTest.class );
        suite.addTestSuite( FileUploadTest.class );

        System.out.print( "----------------------\nGemma web integration tests\n" + suite.countTestCases()
                + " Tests to run\n----------------------\n" );

        return suite;
    }
}
