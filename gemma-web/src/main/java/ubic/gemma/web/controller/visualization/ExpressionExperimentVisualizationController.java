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
package ubic.gemma.web.controller.visualization;

import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.date.MonthConstants;
import org.springframework.web.servlet.ModelAndView;

import ubic.basecode.gui.ColorMatrix;
import ubic.basecode.gui.JMatrixDisplay;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.visualization.HttpExpressionDataMatrixVisualizer;
import ubic.gemma.web.controller.BaseMultiActionController;

/**
 * Use to generate multiple types of visualizations.
 * 
 * @spring.bean id="expressionExperimentVisualizationController"
 * @spring.property name="methodNameResolver" ref="expressionExperimentVisualizationActions"
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentVisualizationController extends BaseMultiActionController {
    private Log log = LogFactory.getLog( ExpressionExperimentVisualizationController.class );

    private HttpExpressionDataMatrixVisualizer httpExpressionDataMatrixVisualizer = null;

    private static final int DEFAULT_MAX_SIZE = 3;

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {

        String type = ( String ) request.getSession().getAttribute( "type" );
        log.debug( "attribute \"type\" from tag: " + type );

        httpExpressionDataMatrixVisualizer = ( HttpExpressionDataMatrixVisualizer ) request.getSession().getAttribute(
                "httpExpressionDataMatrixVisualizer" );
        log.debug( "attribute \"httpExpressionDataMatrixVisualizer\" from tag: " + httpExpressionDataMatrixVisualizer );

        ExpressionDataMatrix expressionDataMatrix = httpExpressionDataMatrixVisualizer.getExpressionDataMatrix();

        type = "matrix";
        OutputStream out = null;
        try {
            out = response.getOutputStream();
            if ( type.equals( "matrix" ) ) {
                String title = "Heat Map of Expression Values";// TODO read in?
                JMatrixDisplay display = createHeatMap( title, expressionDataMatrix );
                if ( display != null ) {
                    response.setContentType( "image/png" );
                    display.writeOutAsPNG( out, false, false );
                }
            }
            // else if ( type.equals( "profile" ) ) {
            // String title = "Expression Profiles";
            // JFreeChart chart = createXYLineChart( title, httpExpressionDataMatrixVisualizer
            // .getRowData( expressionDataMatrix ), DEFAULT_MAX_SIZE );
            // if ( chart != null ) {
            // response.setContentType( "image/png" );
            // ChartUtilities.writeChartAsPNG( out, chart, 400, 300 );
            // }
            // }

            else if ( type.equals( "bar" ) ) {
                JFreeChart chart = createBarChart();
                if ( chart != null ) {
                    response.setContentType( "image/png" );
                    ChartUtilities.writeChartAsPNG( out, chart, 400, 300 );
                }
            }
        } catch ( Exception e ) {
            log.error( "Error is: " );
            e.printStackTrace();
        } finally {
            if ( out != null ) {
                try {
                    out.close();
                } catch ( IOException e ) {
                    log.warn( "Problems closing output stream.  Issues were: " + e.toString() );
                }
            }
        }
        return null; // nothing to return;
    }

    // // TODO Currently contains dummy charts. Change to data matrix.
    // /**
    // * @param request
    // * @param response
    // * @param errors
    // * @return ModelAndView
    // */
    // @SuppressWarnings("unused")
    // public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {
    //
    // OutputStream out = null;
    // try {
    // out = response.getOutputStream();
    // String type = request.getParameter( "type" );
    // JFreeChart chart = null;
    // if ( type.equals( "pie" ) ) {
    // chart = createPieChart();
    // } else if ( type.equals( "bar" ) ) {
    // chart = createBarChart();
    // } else if ( type.equals( "time" ) ) {
    // chart = createTimeSeriesChart();
    // }
    // if ( chart != null ) {
    // response.setContentType( "image/png" );
    // ChartUtilities.writeChartAsPNG( out, chart, 400, 300 );
    // }
    // } catch ( Exception e ) {
    // log.error( e.toString() );
    // } finally {
    // if ( out != null ) {
    // try {
    // out.close();
    // } catch ( IOException e ) {
    // log.warn( "Problems closing output stream. Issues were: " + e.toString() );
    // }
    // }
    // }
    // return null; // nothing to return;
    // }

    private JMatrixDisplay createHeatMap( String title, ExpressionDataMatrix expressionDataMatrix ) {

        if ( httpExpressionDataMatrixVisualizer == null )
            throw new RuntimeException( "Cannot create color matrix due to null HttpExpressionDataMatrixVisualizer" );

        ColorMatrix colorMatrix = httpExpressionDataMatrixVisualizer.createColorMatrix( expressionDataMatrix );

        // TODO move from JMatrixDisplay
        JMatrixDisplay display = new JMatrixDisplay( colorMatrix );

        display.setCellSize( new Dimension( 16, 16 ) );

        return display;
    }

    private JFreeChart createXYLineChart( String title, Collection<double[]> dataCol, int numProfiles ) {
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

        return chart;
    }

    /**
     * Creates a sample pie chart.
     * 
     * @return a pie chart.
     */
    private JFreeChart createPieChart() {

        // create a dataset...
        DefaultPieDataset data = new DefaultPieDataset();
        data.setValue( "One", new Double( 43.2 ) );
        data.setValue( "Two", new Double( 10.0 ) );
        data.setValue( "Three", new Double( 27.5 ) );
        data.setValue( "Four", new Double( 17.5 ) );
        data.setValue( "Five", new Double( 11.0 ) );
        data.setValue( "Six", new Double( 19.4 ) );

        JFreeChart chart = ChartFactory.createPieChart( "Pie Chart", data, true, true, false );
        return chart;

    }

    /**
     * Creates a sample bar chart.
     * 
     * @return a bar chart.
     */
    private JFreeChart createBarChart() {

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue( 10.0, "S1", "C1" );
        dataset.addValue( 4.0, "S1", "C2" );
        dataset.addValue( 15.0, "S1", "C3" );
        dataset.addValue( 14.0, "S1", "C4" );
        dataset.addValue( -5.0, "S2", "C1" );
        dataset.addValue( -7.0, "S2", "C2" );
        dataset.addValue( 14.0, "S2", "C3" );
        dataset.addValue( -3.0, "S2", "C4" );
        dataset.addValue( 6.0, "S3", "C1" );
        dataset.addValue( 17.0, "S3", "C2" );
        dataset.addValue( -12.0, "S3", "C3" );
        dataset.addValue( 7.0, "S3", "C4" );
        dataset.addValue( 7.0, "S4", "C1" );
        dataset.addValue( 15.0, "S4", "C2" );
        dataset.addValue( 11.0, "S4", "C3" );
        dataset.addValue( 0.0, "S4", "C4" );
        dataset.addValue( -8.0, "S5", "C1" );
        dataset.addValue( -6.0, "S5", "C2" );
        dataset.addValue( 10.0, "S5", "C3" );
        dataset.addValue( -9.0, "S5", "C4" );
        dataset.addValue( 9.0, "S6", "C1" );
        dataset.addValue( 8.0, "S6", "C2" );
        dataset.addValue( null, "S6", "C3" );
        dataset.addValue( 6.0, "S6", "C4" );
        dataset.addValue( -10.0, "S7", "C1" );
        dataset.addValue( 9.0, "S7", "C2" );
        dataset.addValue( 7.0, "S7", "C3" );
        dataset.addValue( 7.0, "S7", "C4" );
        dataset.addValue( 11.0, "S8", "C1" );
        dataset.addValue( 13.0, "S8", "C2" );
        dataset.addValue( 9.0, "S8", "C3" );
        dataset.addValue( 9.0, "S8", "C4" );
        dataset.addValue( -3.0, "S9", "C1" );
        dataset.addValue( 7.0, "S9", "C2" );
        dataset.addValue( 11.0, "S9", "C3" );
        dataset.addValue( -10.0, "S9", "C4" );

        JFreeChart chart = ChartFactory.createBarChart3D( "Bar Chart", "Category", "Value", dataset,
                PlotOrientation.VERTICAL, true, true, false );
        return chart;

    }

    /**
     * Creates a sample time series chart.
     * 
     * @return a time series chart.
     */
    private JFreeChart createTimeSeriesChart() {

        // here we just populate a series with random data...
        TimeSeries series = new TimeSeries( "Random Data" );
        Day current = new Day( 1, MonthConstants.JANUARY, 2001 );
        for ( int i = 0; i < 100; i++ ) {
            series.add( current, Math.random() * 100 );
            current = ( Day ) current.next();
        }
        XYDataset data = new TimeSeriesCollection( series );

        JFreeChart chart = ChartFactory.createTimeSeriesChart( "Time Series Chart", "Date", "Rate", data, true, true,
                false );
        return chart;

    }
}
