package ubic.gemma.visualization;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ExpressionDataMatrixVisualizationTest extends TestCase {
    private Log log = LogFactory.getLog( this.getClass() );

    ExpressionDataMatrixVisualization matrixVisualizer = null;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        matrixVisualizer = new ExpressionDataMatrixVisualization();
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

        String[] rowLabels = { "a", "b", "c", "d", "e" };
        List<String> rowLabelsList = new ArrayList();
        for ( int i = 0; i < rowLabels.length; i++ ) {
            rowLabelsList.add( i, rowLabels[i] );
        }

        List<String> colLabelsList = new ArrayList();
        for ( int i = 0; i < data[0].length; i++ ) {
            colLabelsList.add( i, String.valueOf( i ) );
        }

        matrixVisualizer.setRowLabels( rowLabelsList );
        matrixVisualizer.setColLabels( colLabelsList );

        matrixVisualizer.createVisualization( data );
        
        assertNotNull(matrixVisualizer.getColorMatrix());

    }
    
    public void testSaveImage(){
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

        String[] rowLabels = { "a", "b", "c", "d", "e" };
        List<String> rowLabelsList = new ArrayList();
        for ( int i = 0; i < rowLabels.length; i++ ) {
            rowLabelsList.add( i, rowLabels[i] );
        }

        List<String> colLabelsList = new ArrayList();
        for ( int i = 0; i < data[0].length; i++ ) {
            colLabelsList.add( i, String.valueOf( i ) );
        }

        matrixVisualizer.setRowLabels( rowLabelsList );
        matrixVisualizer.setColLabels( colLabelsList );

        matrixVisualizer.createVisualization( data );
        
        matrixVisualizer.saveImage("visualization.png");
    }

}
