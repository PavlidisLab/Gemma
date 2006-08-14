/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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
package ubic.gemma.datastructure.matrix;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.io.StringConverter;
import ubic.gemma.loader.util.parser.TabDelimParser;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.visualization.ExpressionDataMatrixVisualizer;
import ubic.gemma.visualization.MatrixVisualizer;

/**
 * @author keshav
 * @version $Id$
 */
public class ExpressionDataMatrixTest extends TestCase {
    Log log = LogFactory.getLog( this.getClass() );
    String[] rowNames = null;
    String[] colNames = null;

    ExpressionDataMatrix matrixData = null;
    ByteArrayConverter bconverter = null;
    StringConverter sconverter = null;

    private byte[][] readTabFile( InputStream stream ) throws IOException {
        TabDelimParser parser = new TabDelimParser();
        Collection results = new HashSet();
        parser.parse( stream );
        results = parser.getResults();

        colNames = parser.getHeader();

        bconverter = new ByteArrayConverter();
        sconverter = new StringConverter();
        byte[][] values = new byte[results.size()][];
        rowNames = new String[results.size()];
        Iterator iter = results.iterator();
        int i = 0;
        while ( iter.hasNext() ) {
            String[] array = ( String[] ) iter.next();

            String[] sarray = { array[1], array[2], array[3], array[4], array[5], array[6], array[7], array[8],
                    array[9], array[10] };

            rowNames[i] = array[0];

            double[] row = sconverter.stringArrayToDoubles( sarray );
            values[i] = bconverter.doubleArrayToBytes( row );

            i++;
        }

        return values;
    }

    /**
     * @param filename
     * @return double [][]
     */
    public byte[][] readTabFile( String filename ) throws IOException {
        InputStream is = new FileInputStream( new File( filename ) );
        return this.readTabFile( is );
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        byte[][] data = readTabFile( this.getClass().getResourceAsStream( "/data/visualization/testData_100.txt" ) );

        assert data != null;
        assert data[0] != null;
        assert data[0].length > 0;

        Collection<DesignElement> designElements = new HashSet<DesignElement>();
        for ( int i = 0; i < rowNames.length; i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance();
            cs.setName( rowNames[i] );

            DesignElementDataVector vector = DesignElementDataVector.Factory.newInstance();
            vector.setData( data[i] );
            vector.setDesignElement( cs );

            Collection<DesignElementDataVector> vectors = new HashSet<DesignElementDataVector>();
            vectors.add( vector );
            cs.setDesignElementDataVectors( vectors );
            designElements.add( cs );
        }

        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        matrixData = new ExpressionDataMatrix( ee, designElements );

    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        rowNames = null;
        colNames = null;
        matrixData = null;
    }

    public void testMatrixVisualizationData() throws Exception {

        // vizualizationData.printData();

        MatrixVisualizer visualizer = new ExpressionDataMatrixVisualizer();

        visualizer.createVisualization( matrixData );

        File tmp = File.createTempFile( "testOut", ".png" );

        visualizer.saveImage( tmp );

        tmp.deleteOnExit();

        FileInputStream fis = new FileInputStream( tmp );

        assertNotNull( fis );
        assertTrue( tmp.length() > 0 );
        fis.close();
    }
}
