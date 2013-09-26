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

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.graphics.ColorMatrix;
import ubic.basecode.graphics.MatrixDisplay;
import ubic.basecode.io.reader.DoubleMatrixReader;

/**
 * @author keshav
 * @version $Id$
 */
public class VisualizeDataSetApp {
    private static Log log = LogFactory.getLog( VisualizeDataSetApp.class );

    /**
     * @param args
     */
    public static void main( String[] args ) {

        VisualizeDataSetApp visualizeDataSet = new VisualizeDataSetApp();

        DoubleMatrix<String, String> matrix = null;
        try {
            matrix = visualizeDataSet.parseData( true );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        ColorMatrix<String, String> colorMatrix = ColorMatrix.newInstance( matrix );
        visualizeDataSet.showDataMatrix( "A heat map", MatrixDisplay.newInstance( colorMatrix ) );

        List<double[]> data = new ArrayList<double[]>();

        data.add( matrix.getRow( 4 ) );
        data.add( matrix.getRow( 16 ) );
        data.add( matrix.getRow( 22 ) );
        data.add( matrix.getRow( 26 ) );
        data.add( matrix.getRow( 36 ) );

        visualizeDataSet.showProfilesLineChartView( "A line chart", data, 5 );

        visualizeDataSet.showProfilesPolarView( "A polar chart", data, 5 );

    }

    private String filePath = "aov.results-2-monocyte-data-bytime.bypat.data.sort";

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
    public void showDataMatrix( String title, MatrixDisplay<String, String> matrixDisplay ) {

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
    public void showProfilesBubbleChartView( String title, double[][] dataMatrix, int numProfiles ) {

        if ( dataMatrix == null ) throw new RuntimeException( "dataMatrix cannot be " + null );

        JFreeChart chart = ChartFactory.createXYLineChart( title, "Platform", "Expression Value", null,
                PlotOrientation.VERTICAL, false, false, false );

        MatrixSeries series = new MatrixSeries( title, dataMatrix[0].length, dataMatrix.length );
        XYPlot plot = chart.getXYPlot();
        plot.setDataset( new MatrixSeriesCollection( series ) );

        ChartFrame frame = new ChartFrame( title, chart, true );

        showWindow( frame );
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
        Iterator<double[]> iter = dataCol.iterator();
        for ( int j = 0; j < numProfiles; j++ ) {
            XYSeries series = getSeries( j, iter.next() );
            xySeriesCollection.addSeries( series );
        }

        JFreeChart chart = ChartFactory.createXYLineChart( title, "Platform", "Expression Value", xySeriesCollection,
                PlotOrientation.VERTICAL, false, false, false );
        chart.addSubtitle( new TextTitle( "(Raw data values)", new Font( "SansSerif", Font.BOLD, 14 ) ) );

        // XYPlot plot = chart.getXYPlot();

        ChartFrame frame = new ChartFrame( title, chart, true );

        showWindow( frame );
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

        Iterator<double[]> iter = dataCol.iterator();
        for ( int j = 0; j < numProfiles; j++ ) {
            XYSeries series = getSeries( j, iter.next() );
            PolarPlot plot = ( PolarPlot ) chart.getPlot();
            plot.setDataset( new XYSeriesCollection( series ) );
        }

        ChartFrame frame = new ChartFrame( title, chart, true );

        showWindow( frame );
    }

    /**
     * @param iter
     * @return
     */
    private XYSeries getSeries( Comparable<Integer> key, double[] data ) {
        XYSeries series = new XYSeries( key, true, true );
        for ( int i = 0; i < data.length; i++ ) {
            series.add( i, data[i] );
        }
        return series;
    }

    /**
     * @param headerExists
     * @return
     * @throws IOException
     */
    private DoubleMatrix<String, String> parseData( boolean headerExists ) throws IOException {
        try (InputStream is = this.getClass().getResourceAsStream( "/data/loader/" + filePath );) {
            if ( is == null ) throw new RuntimeException( "could not load data" );
            DoubleMatrixReader reader = new DoubleMatrixReader();
            return reader.read( is );
        }
    }

    /**
     * @param frame
     */
    private void showWindow( ChartFrame frame ) {
        // Display the window.
        frame.setLocationRelativeTo( null );
        frame.pack();
        frame.setVisible( true );
    }

}
