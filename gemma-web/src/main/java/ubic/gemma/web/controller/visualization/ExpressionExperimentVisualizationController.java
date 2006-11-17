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
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.web.servlet.ModelAndView;

import ubic.basecode.gui.ColorMatrix;
import ubic.basecode.gui.JMatrixDisplay;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.visualization.ExpressionDataMatrixVisualizer;
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

    private ExpressionDataMatrixVisualizer expressionDataMatrixVisualizer = null;

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

        expressionDataMatrixVisualizer = ( ExpressionDataMatrixVisualizer ) request.getSession().getAttribute(
                "expressionDataMatrixVisualizer" );
        log.debug( "attribute \"expressionDataMatrixVisualizer\" from tag: " + expressionDataMatrixVisualizer );

        ExpressionDataMatrix expressionDataMatrix = expressionDataMatrixVisualizer.getExpressionDataMatrix();

        OutputStream out = null;
        try {
            out = response.getOutputStream();
            if ( type.equals( "matrix" ) ) {
                String title = "Heat Map of Expression Values";
                JMatrixDisplay display = createHeatMap( title, expressionDataMatrix );
                if ( display != null ) {
                    response.setContentType( "image/png" );
                    display.writeOutAsPNG( out, true, true );
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

    /**
     * @param title
     * @param expressionDataMatrix
     * @return JMatrixDisplay
     */
    private JMatrixDisplay createHeatMap( String title, ExpressionDataMatrix expressionDataMatrix ) {

        if ( expressionDataMatrixVisualizer == null )
            throw new RuntimeException( "Cannot create color matrix due to null ExpressionDataMatrixVisualizer" );

        ColorMatrix colorMatrix = expressionDataMatrixVisualizer.createColorMatrix( expressionDataMatrix );

        JMatrixDisplay display = new JMatrixDisplay( colorMatrix );

        display.setCellSize( new Dimension( 10, 10 ) );

        return display;
    }

    /**
     * @param title
     * @param dataCol
     * @param numProfiles
     * @return JFreeChart
     */
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
}
