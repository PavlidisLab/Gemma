package ubic.gemma.web.util.progress;

import ubic.gemma.util.progress.ProgressData;
import junit.framework.TestCase;

public class ProgressDataTest extends TestCase {

    protected ProgressData pd;

    protected void setUp() throws Exception {
        super.setUp();
        pd = new ProgressData( 1, "test", false );

    }

    protected void tearDown() throws Exception {
        super.tearDown();
        pd = null;

    }

    /*
     * Test method for 'ubic.gemma.web.util.progress.ProgressData.ProgressData(int, String, boolean)'
     */
    public void testProgressData() {

        assertEquals( pd.getPercent(), 1 );
        assertEquals( pd.getDescription(), "test" );
        assertEquals( pd.isDone(), false );

    }

    /*
     * Test method for 'ubic.gemma.web.util.progress.ProgressData.setDone(boolean)'
     */
    public void testSetDone() {
        pd.setDone( true );
        assert ( pd.isDone() );
    }

    /*
     * Test method for 'ubic.gemma.web.util.progress.ProgressData.setDescription(String)'
     */
    public void testSetDescription() {
        pd.setDescription( "testDescription" );
        assertEquals( "testDescription", pd.getDescription() );
    }

    /*
     * Test method for 'ubic.gemma.web.util.progress.ProgressData.setPercent(int)'
     */
    public void testSetPercent() {
        pd.setPercent( 88 );
        assertEquals( pd.getPercent(), 88 );

    }

}
