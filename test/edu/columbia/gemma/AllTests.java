package edu.columbia.gemma;

import junit.framework.Test;
import junit.framework.TestSuite;
import edu.columbia.gemma.common.description.BibliographicReferenceDaoImplTest;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignServiceImplTest;
import edu.columbia.gemma.loader.arraydesign.AffyProbeReaderTest;
import edu.columbia.gemma.loader.arraydesign.AffymetrixProbeSetTest;
import edu.columbia.gemma.loader.arraydesign.AffymetrixProbeTest;
import edu.columbia.gemma.loader.arraydesign.IlluminaProbeReaderTest;
import edu.columbia.gemma.sequence.QtlDaoImplTest;

public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite( "Test for edu.columbia.gemma" );
        // $JUnit-BEGIN$

        // $JUnit-END$
        suite.addTestSuite( BibliographicReferenceDaoImplTest.class );
        suite.addTestSuite( ArrayDesignServiceImplTest.class );
        suite.addTestSuite( QtlDaoImplTest.class );

        suite.addTestSuite( AffymetrixProbeSetTest.class );
        suite.addTestSuite( AffymetrixProbeTest.class );
        suite.addTestSuite( AffyProbeReaderTest.class );
        suite.addTestSuite( IlluminaProbeReaderTest.class );
        return suite;
    }
}
