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
package ubic.gemma.visualization;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.io.StringConverter;
import ubic.gemma.loader.util.parser.TabDelimParser;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author keshav
 * @version $Id$
 */
public class ExpessionDataMatrixTest extends TestCase {
    Log log = LogFactory.getLog( this.getClass() );
    String[] rowNames = null;
    String[] colNames = null;

    ExpressionDataMatrix matrixData = null;
    ByteArrayConverter bconverter = null;
    StringConverter sconverter = null;
    byte[][] values;

    /**
     * @param filename
     * @return double [][]
     */
    public byte[][] readTabFile( String filename ) {
        TabDelimParser parser = new TabDelimParser();
        InputStream is;
        Collection results = new HashSet();
        try {
            is = new FileInputStream( new File( filename ) );
            parser.parse( is );
            results = parser.getResults();
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        colNames = parser.getHeader();

        bconverter = new ByteArrayConverter();
        sconverter = new StringConverter();
        values = new byte[results.size()][];
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

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void setUp() throws Exception {

        Configuration config = new PropertiesConfiguration( "Gemma.properties" );
        String baseDir = ( String ) config.getProperty( "gemma.baseDir" );
        String filename = baseDir + ( String ) config.getProperty( "testData_100" );

        byte[][] data = readTabFile( filename );

        Collection<DesignElement> designElements = new HashSet();
        for ( int i = 0; i < rowNames.length; i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance();
            cs.setName( rowNames[i] );

            DesignElementDataVector vector = DesignElementDataVector.Factory.newInstance();
            vector.setData( data[i] );
            vector.setDesignElement( cs );

            // cs.setDesignElementDataVector( vector );
            Collection<DesignElementDataVector> vectors = new HashSet();
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
        values = null;
        rowNames = null;
        colNames = null;
        matrixData = null;
    }

    /**
     * 
     *
     */
    public void testMatrixVisualizationDataPrimitiveMatrix() {
        double[][] ddata = new double[rowNames.length][];
        for ( int i = 0; i < ddata.length; i++ ) {
            ddata[i] = bconverter.byteArrayToDoubles( values[i] );
        }

        HtmlMatrixVisualizer visualizer = new HtmlMatrixVisualizer();

        visualizer.createVisualization( matrixData );
        visualizer.saveImage( "gemma-core/src/test/java/ubic/gemma/visualization/outImage0.png" );

    }

    /**
     * 
     *
     */
    public void testMatrixVisualizationData() {

        // vizualizationData.printData();

        HtmlMatrixVisualizer visualizer = new HtmlMatrixVisualizer();

        // visualizer.setColLabels( Arrays.asList( colNames ) );
        // visualizer.setRowLabels( Arrays.asList( rowNames ) );

        // visualizer.setColorMap( ColorMap.GREENRED_COLORMAP );

        visualizer.createVisualization( matrixData );
        visualizer.saveImage( "gemma-core/src/test/java/ubic/gemma/visualization/outImage1.png" );

    }
}
