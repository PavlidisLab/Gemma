/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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

import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.UIManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PolarPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.MatrixSeries;
import org.jfree.data.xy.MatrixSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed2D;
import ubic.basecode.dataStructure.matrix.FastRowAccessDoubleMatrix2DNamed;
import ubic.basecode.gui.ColorMatrix;
import ubic.basecode.gui.JMatrixDisplay;
import ubic.gemma.loader.util.converter.SimpleExpressionExperimentConverter;
import ubic.gemma.loader.util.parser.TabDelimParser;

/**
 * @author keshav
 * @version $Id$
 */
public class VisualizeDataSetApp {
    private static Log log = LogFactory.getLog( VisualizeDataSetApp.class );

    private String filename = "aov.results-2-monocyte-data-bytime.bypat.data.sort";

    private SimpleExpressionExperimentConverter converter = null;

    private static final int DEFAULT_MAX_SIZE = 3;

    /**
     * Add basic gui components.
     * 
     * @param title
     * @return JFrame
     */
    private static JFrame createGui( String title ) {

        try {
            UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName() );
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        JFrame frame = new JFrame( title );
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

        JLabel label = new JLabel( title );
        frame.getContentPane().add( label );

        return frame;
    }

    /**
     * @param title
     * @param matrixDisplay
     */
    public void showDataMatrix( String title, JMatrixDisplay matrixDisplay ) {

        JFrame frame = createGui( title );
        frame.add( matrixDisplay );

        // Display the window.
        frame.pack();
        frame.setVisible( true );
    }

    /**
     * @param title
     * @param dataCol
     * @param numProfiles
     */
    public void showProfilesLineChartView( String title, Collection<double[]> dataCol, int numProfiles ) {

        if ( dataCol == null ) throw new RuntimeException( "dataCol cannot be " + null );

        if ( dataCol.size() < numProfiles ) {
            log.info( "Collection smaller than number of elements.  Will display first " + DEFAULT_MAX_SIZE
                    + " profiles." );
            numProfiles = DEFAULT_MAX_SIZE;
        }

        XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
        Iterator iter = dataCol.iterator();
        for ( int j = 0; j < numProfiles; j++ ) {
            double[] data = ( double[] ) iter.next();
            XYSeries series = new XYSeries( j, true, true );
            for ( int i = 0; i < data.length; i++ ) {
                series.add( i, data[i] );
            }
            xySeriesCollection.addSeries( series );
        }

        JFreeChart chart = ChartFactory.createXYLineChart( title, "Microarray", "Expression Value", xySeriesCollection,
                PlotOrientation.VERTICAL, false, false, false );
        chart.addSubtitle( new TextTitle( "(Raw data values)", new Font( "SansSerif", Font.BOLD, 14 ) ) );

        // XYPlot plot = chart.getXYPlot();

        ChartFrame frame = new ChartFrame( title, chart, true );

        // Display the window.
        frame.setLocationRelativeTo( null );
        frame.pack();
        frame.setVisible( true );
    }

    /**
     * @param title
     * @param dataCol
     * @param numProfiles
     */
    public void showProfilesPolarView( String title, Collection<double[]> dataCol, int numProfiles ) {

        if ( dataCol == null ) throw new RuntimeException( "dataCol cannot be " + null );

        JFreeChart chart = ChartFactory.createPolarChart( title, null, false, false, false );

        if ( dataCol.size() < numProfiles ) {
            log.info( "Collection smaller than number of elements. Will display " + DEFAULT_MAX_SIZE + " profiles." );
            numProfiles = DEFAULT_MAX_SIZE;
        }

        Iterator iter = dataCol.iterator();
        for ( int j = 0; j < numProfiles; j++ ) {
            double[] data = ( double[] ) iter.next();
            XYSeries series = new XYSeries( "" );
            for ( int i = 0; i < data.length; i++ ) {
                series.add( i, data[i] );
            }
            PolarPlot plot = ( PolarPlot ) chart.getPlot();
            plot.setDataset( new XYSeriesCollection( series ) );
        }

        ChartFrame frame = new ChartFrame( title, chart, true );

        // Display the window.
        frame.setLocationRelativeTo( null );
        frame.pack();
        frame.setVisible( true );
    }

    /**
     * @param title
     * @param dataCol
     * @param numProfiles
     */
    public void showProfilesBubbleChartView( String title, double[][] dataMatrix, int numProfiles ) {

        if ( dataMatrix == null ) throw new RuntimeException( "dataMatrix cannot be " + null );

        JFreeChart chart = ChartFactory.createXYLineChart( title, "Microarray", "Expression Value", null,
                PlotOrientation.VERTICAL, false, false, false );

        MatrixSeries series = new MatrixSeries( title, dataMatrix[0].length, dataMatrix.length );
        XYPlot plot = chart.getXYPlot();
        plot.setDataset( new MatrixSeriesCollection( series ) );

        ChartFrame frame = new ChartFrame( title, chart, true );

        // Display the window.
        frame.setLocationRelativeTo( null );
        frame.pack();
        frame.setVisible( true );
    }

    /**
     * @param headerExists
     * @return
     * @throws IOException
     */
    private Collection<String[]> parseData( boolean headerExists ) throws IOException {

        InputStream is = VisualizeDataSetApp.class.getResourceAsStream( "/data/loader/" + filename );

        TabDelimParser parser = new TabDelimParser();
        parser.parse( is );

        Collection<String[]> results = parser.getResults();

        return results;
    }

    /**
     * @param dataCol
     * @return Collection<String[]>
     */
    private Collection<String[]> prepareData( Collection<String[]> dataCol ) {

        if ( converter == null ) {
            log.info( "Null converter.  Creating a new one" );
            converter = new SimpleExpressionExperimentConverter( "Expression experiment " + filename,
                    "An expression experiment based on data from " + filename, "Pappapanou", "Pavlidis collaboration" );
        }
        Collection<String[]> rawDataCol = converter.prepareData( dataCol, true, true );
        return rawDataCol;

    }

    /**
     * 
     *
     */
    private double[][] parsePrepareAndConvertRawData() {

        try {
            Collection<String[]> results = parseData( true );
            assert 201 == results.size();

            Collection<String[]> data = prepareData( results );
            assert 200 == results.size();

            return converter.convertRawData( data );

        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {

        VisualizeDataSetApp visualizeDataSet = new VisualizeDataSetApp();

        double[][] rawData = visualizeDataSet.parsePrepareAndConvertRawData();

        ExpressionDataMatrixVisualizationService visualizationService = new ExpressionDataMatrixVisualizationService();

        DoubleMatrixNamed2D m = new FastRowAccessDoubleMatrix2DNamed( rawData );
        m.setRowNames( Arrays.asList( visualizeDataSet.converter.getRowNames() ) );
        m.setColumnNames( Arrays.asList( visualizeDataSet.converter.getHeader() ) );
        ColorMatrix colorMatrix = new ColorMatrix( m );
        // ColorMatrix colorMatrix = visualizationService.createColorMatrix( rawData, new LinkedHashSet( Arrays
        // .asList( visualizeDataSet.converter.getRowNames() ) ), new LinkedHashSet( Arrays
        // .asList( visualizeDataSet.converter.getHeader() ) ) );

        visualizeDataSet.showDataMatrix( "aov.results-2-monocyte-data-bytime.bypat.data.sort", new JMatrixDisplay(
                colorMatrix ) );

        List<double[]> data = new ArrayList<double[]>();

        data.add( rawData[4] );
        data.add( rawData[13] );
        data.add( rawData[22] );
        data.add( rawData[26] );
        data.add( rawData[36] );

        visualizeDataSet.showProfilesLineChartView( "aov.results-2-monocyte-data-bytime.bypat.data.sort", data, 5 );

        visualizeDataSet.showProfilesPolarView( "aov.results-2-monocyte-data-bytime.bypat.data.sort", data, 5 );

    }
}
