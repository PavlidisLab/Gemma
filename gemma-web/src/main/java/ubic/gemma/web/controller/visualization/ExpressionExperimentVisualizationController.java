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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.web.servlet.ModelAndView;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.gui.ColorMatrix;
import ubic.basecode.gui.JMatrixDisplay;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.DesignElement;
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

    private ExpressionDataMatrix expressionDataMatrix = null;

    private String type = null;

    protected List<String> rowLabels = null;

    protected List<String> colLabels = null;

    private static final int DEFAULT_MAX_SIZE = 3;

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {

        init( request, response );

        String title = null;
        OutputStream out = null;
        try {
            out = response.getOutputStream();
            if ( type.equalsIgnoreCase( "matrix" ) ) {
                title = "Heat Map of Expression Values";
                JMatrixDisplay display = createHeatMap( title, expressionDataMatrix );
                if ( display != null ) {
                    response.setContentType( "image/png" );
                    display.writeOutAsPNG( out, true, true );
                }
            }
            // else if ( type.equalsIgnoreCase( "profile" ) ) {
            // title = "Expression Profiles";
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
     * @param request
     * @param response
     * @return OutputStream
     */
    private void init( HttpServletRequest request, HttpServletResponse response ) {

        this.rowLabels = new ArrayList<String>();

        this.colLabels = new ArrayList<String>();

        type = ( String ) request.getSession().getAttribute( "type" );
        log.debug( "attribute \"type\" from tag: " + type );

        expressionDataMatrix = ( ExpressionDataMatrix ) request.getSession().getAttribute( "expressionDataMatrix" );
        log.debug( "attribute \"expressionDataMatrix\" from tag: " + expressionDataMatrix );
    }

    /**
     * @param title
     * @param expressionDataMatrix
     * @return JMatrixDisplay
     */
    private JMatrixDisplay createHeatMap( String title, ExpressionDataMatrix expressionDataMatrix ) {

        if ( expressionDataMatrix == null )
            throw new RuntimeException( "Cannot create color matrix due to null ExpressionDataMatrix" );

        ColorMatrix colorMatrix = createColorMatrix( expressionDataMatrix );

        JMatrixDisplay display = new JMatrixDisplay( colorMatrix );

        display.setCellSize( new Dimension( 10, 10 ) );

        return display;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#createVisualization(ubic.gemma.datastructure.matrix.ExpressionDataMatrix)
     */
    @SuppressWarnings("unchecked")
    public ColorMatrix createColorMatrix( ExpressionDataMatrix expressionDataMatrix ) {
        Collection<DesignElement> rowMap = expressionDataMatrix.getRowElements(); // row labels

        if ( expressionDataMatrix == null || rowMap.size() == 0 ) {
            throw new IllegalArgumentException( "ExpressionDataMatrix apparently has no data" );
        }

        double[][] data = new double[rowMap.size()][];
        int i = 0;
        for ( DesignElement designElement : rowMap ) {
            Double[] row = ( Double[] ) expressionDataMatrix.getRow( designElement );
            data[i] = ArrayUtils.toPrimitive( row );
            rowLabels.add( designElement.getName() );
            i++;
        }

        int j = 0;
        while ( j < data[0].length ) {
            Collection<BioMaterial> bioMaterials = expressionDataMatrix.getBioMaterialsForColumn( j );
            if ( bioMaterials == null || bioMaterials.size() == 0 ) {
                log.warn( "No BioMaterials found for index + " + j + ". Setting label to column number " + j );
                colLabels.add( String.valueOf( j ) );
            } else {
                if ( bioMaterials.size() > 1 )
                    log.warn( "More than one BioMaterial. Using first one found as column label." );

                Iterator iter = bioMaterials.iterator();
                while ( iter.hasNext() ) {
                    BioMaterial bm = ( BioMaterial ) iter.next();
                    if ( !colLabels.contains( bm.getName() ) ) {
                        log.debug( "adding label " + bm.getName() );
                        colLabels.add( bm.getName() );
                        break;
                    }
                }

            }
            j++;
        }

        return this.createColorMatrix( data, rowLabels, colLabels );
    }

    /**
     * @param data
     * @param rowLabels
     * @param colorMatrix
     */
    public ColorMatrix createColorMatrix( double[][] data, List<String> rowLabels, List<String> colLabels ) {
        assert rowLabels != null && colLabels != null : "Labels not set";

        DoubleMatrixNamed matrix = new DenseDoubleMatrix2DNamed( data );
        matrix.setRowNames( rowLabels );
        matrix.setColumnNames( colLabels );
        ColorMatrix colorMatrix = new ColorMatrix( matrix );

        return colorMatrix;
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
