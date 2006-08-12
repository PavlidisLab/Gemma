/*
 * The Gemma project
 * 
 * Copyright (c) 2006 UniverSity of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.visualization;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 * @author keshav
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionDataMatrixVisualizationTest extends TestCase {

    ExpressionDataMatrixVisualizer matrixVisualizer = null;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        matrixVisualizer = new ExpressionDataMatrixVisualizer();
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
        List<String> rowLabelsList = new ArrayList<String>();
        for ( int i = 0; i < rowLabels.length; i++ ) {
            rowLabelsList.add( i, rowLabels[i] );
        }

        List<String> colLabelsList = new ArrayList<String>();
        for ( int i = 0; i < data[0].length; i++ ) {
            colLabelsList.add( i, String.valueOf( i ) );
        }

        matrixVisualizer.setRowLabels( rowLabelsList );
        matrixVisualizer.setColLabels( colLabelsList );

        matrixVisualizer.createVisualization( data );

        assertNotNull( matrixVisualizer.getColorMatrix() );

    }

    public void testSaveImage() throws Exception {
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
        List<String> rowLabelsList = new ArrayList<String>();
        for ( int i = 0; i < rowLabels.length; i++ ) {
            rowLabelsList.add( i, rowLabels[i] );
        }

        List<String> colLabelsList = new ArrayList<String>();
        for ( int i = 0; i < data[0].length; i++ ) {
            colLabelsList.add( i, String.valueOf( i ) );
        }

        matrixVisualizer.setRowLabels( rowLabelsList );
        matrixVisualizer.setColLabels( colLabelsList );

        matrixVisualizer.createVisualization( data );

        File tmp = File.createTempFile( "visualizationTest", ".png" );
        tmp.deleteOnExit();
        matrixVisualizer.saveImage( tmp );
        FileInputStream fis = new FileInputStream( tmp );

        assertNotNull( fis );
        assertTrue( tmp.length() > 0 );
        fis.close();
    }
}
