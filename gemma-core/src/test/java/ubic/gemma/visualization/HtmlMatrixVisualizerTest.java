package ubic.gemma.visualization;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HtmlMatrixVisualizerTest extends TestCase {
    private Log log = LogFactory.getLog( this.getClass() );

    MatrixVisualizer matrixVisualizer = null;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        matrixVisualizer = new HtmlMatrixVisualizer();
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        matrixVisualizer = null;
    }

    public void testCreateVisualization() {
        double[][] data = new double[5][5];

        double d0[] = { 1, 2, 3, 4, 5 };
        double d1[] = { 5, 4, 3, 2, 1 };
        double d2[] = { 1, 2, 1, 2, 1 };
        double d3[] = { 9, 5, 12, 3, 8 };
        double d4[] = { 7, 22, 0.02, 3.4, 1.9 };

        data[0] = d0;
        data[1] = d1;
        data[2] = d2;
        data[3] = d3;
        data[4] = d4;

        // for ( int i = 0; i < 5; i++ ) {
        // for ( int j = 0; j < 5; j++ ) {
        // log.debug( data[i][j] );
        //            }
        //        }

        matrixVisualizer.createVisualization( data );

    }

}
