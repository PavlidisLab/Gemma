/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.web.controller.expression.experiment;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionFileUtils;
import ubic.gemma.analysis.preprocess.svd.SVDService;
import ubic.gemma.analysis.preprocess.svd.SVDServiceImpl;
import ubic.gemma.analysis.preprocess.svd.SVDValueObject;
import ubic.gemma.analysis.stats.ExpressionDataSampleCorrelation;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.web.controller.BaseController;

/**
 * @author paul
 * @version $Id$
 */
@Controller
public class ExpressionExperimentQCController extends BaseController {

    private static final String DEFAULT_CONTENT_TYPE = "image/png";
    private static final int HISTOGRAM_IMAGE_SIZE = 200;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    SVDService svdService;

    @RequestMapping("expressionExperiment/pcaFactors.html")
    public ModelAndView pcaFactors( HttpServletRequest request, HttpServletResponse response ) throws Exception {
        Long idl = getEEid( request );

        if ( idl == null ) return null;

        ExpressionExperiment ee = expressionExperimentService.load( idl );
        if ( ee == null ) {
            log.warn( "No such experiment with id " + idl ); // or access deined.
            return null;
        }

        SVDValueObject svdo = svdService.retrieveSvd( idl );

        if ( svdo != null ) {
            this.writePCAFactors( response, ee, svdo );
        }
        return null;
    }

    @RequestMapping("/expressionExperiment/pcaScree.html")
    public ModelAndView pcaScree( HttpServletRequest request, HttpServletResponse response ) throws Exception {

        Long idl = getEEid( request );

        if ( idl == null ) return null;

        ExpressionExperiment ee = expressionExperimentService.load( idl );
        if ( ee == null ) {
            log.warn( "No such experiment with id " + idl ); // or access deined.
            return null;
        }

        SVDValueObject svdo = svdService.retrieveSvd( idl );

        if ( svdo != null ) {
            this.writePCAScree( response, svdo );
        }
        return null;
    }

    public void setExpressionExperimentService( ExpressionExperimentService ees ) {
        expressionExperimentService = ees;
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/expressionExperiment/visualizeCorrMat.html")
    public ModelAndView visualizeCorrMat( HttpServletRequest request, HttpServletResponse response ) throws Exception {
        String id = request.getParameter( "id" );
        String size = request.getParameter( "size" ); // okay if null
        String contrast = request.getParameter( "contr" ); // okay if null, default is 'hi'
        String text = request.getParameter( "text" );

        if ( id == null ) {
            log.warn( "No id!" );
            return null;
        }

        String contrVal = "hi";
        if ( StringUtils.isNotBlank( contrast ) ) {
            contrVal = contrast;
        }

        Long idl;
        try {
            idl = Long.parseLong( id );
        } catch ( NumberFormatException e ) {
            log.warn( "Invalid id: " + id );
            return null;
        }
        assert idl != null;

        ExpressionExperiment ee = expressionExperimentService.load( idl );
        if ( ee == null ) {
            log.warn( "No such experiment with id " + idl );
            return null;
        }
        boolean ok = false;
        if ( StringUtils.isNotBlank( text ) ) {
            ok = writeCorrData( response, ee );
        } else {
            ok = writeCorrMatImage( response, ee, size, contrVal );
        }

        if ( !ok ) {
            // TODO
        }

        return null; // nothing to return;
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/expressionExperiment/visualizeProbeCorrDist.html")
    public ModelAndView visualizeProbeCorrDist( HttpServletRequest request, HttpServletResponse response )
            throws Exception {
        Long idl = getEEid( request );

        ExpressionExperiment ee = expressionExperimentService.load( idl );
        if ( ee == null ) {
            log.warn( "No such experiment with id " + idl );
            return null;
        }

        boolean ok = writeProbeCorrHistImage( response, ee );

        if ( !ok ) {
            // TODO
        }

        return null; // nothing to return;
    }

    /**
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @RequestMapping("/expressionExperiment/visualizePvalueDist.html")
    public ModelAndView visualizePvalueDist( HttpServletRequest request, HttpServletResponse response )
            throws Exception {
        Long idl = getEEid( request );

        ExpressionExperiment ee = expressionExperimentService.load( idl );
        if ( ee == null ) {
            log.warn( "No such experiment with id " + idl );
            return null;
        }

        boolean ok = this.writePValueHistImages( response, ee );

        if ( !ok ) {
            // TODO
        }

        return null; // nothing to return;
    }

    /**
     * @param ee
     * @return JFreeChart XYSeries representing the histogram.
     * @throws FileNotFoundException
     * @throws IOException
     */
    private XYSeries getCorrelHist( ExpressionExperiment ee ) throws FileNotFoundException, IOException {
        File f = this.locateProbeCorrFile( ee );

        XYSeries series = new XYSeries( ee.getId(), true, true );
        BufferedReader in = new BufferedReader( new FileReader( f ) );
        while ( in.ready() ) {
            String line = in.readLine().trim();
            if ( line.startsWith( "#" ) ) continue;
            String[] split = StringUtils.split( line );
            if ( split.length < 2 ) continue;
            try {
                double x = Double.parseDouble( split[0] );
                double y = Double.parseDouble( split[1] );
                series.add( x, y );
            } catch ( NumberFormatException e ) {
                // line wasn't useable.. no big deal. Heading is included.
            }
        }
        return series;
    }

    /**
     * @param ee
     * @return Collection of JFreeChart XYSeries representing the histograms (one for each ResultSet.
     * @throws FileNotFoundException
     * @throws IOException
     */
    private Collection<XYSeries> getDiffExPvalueHists( ExpressionExperiment ee ) throws FileNotFoundException,
            IOException {
        Collection<File> fs = this.locatePvalueDistFiles( ee );

        /*
         * new format is to have just one file.
         */

        List<XYSeries> results = new ArrayList<XYSeries>();

        if ( fs.size() == 1 ) {

            BufferedReader in = new BufferedReader( new FileReader( fs.iterator().next() ) );
            while ( in.ready() ) {
                String line = in.readLine().trim();
                if ( line.startsWith( "#" ) ) continue;
                String[] split = StringUtils.split( line );
                if ( split.length < 2 ) continue;
                try {
                    double x = Double.parseDouble( split[0] );

                    for ( int i = 1; i < split.length; i++ ) {
                        double y = Double.parseDouble( split[i] );

                        if ( results.size() < i ) {
                            results.add( new XYSeries( ee.getId(), true, true ) );
                        }

                        results.get( i - 1 ).add( x, y );
                    }

                } catch ( NumberFormatException e ) {
                    // line wasn't useable.. no big deal. Heading is included.
                }
            }

        } else {
            /*
             * old format, one file per series.
             */
            for ( File f : fs ) {
                XYSeries series = new XYSeries( ee.getId(), true, true );
                BufferedReader in = new BufferedReader( new FileReader( f ) );
                while ( in.ready() ) {
                    String line = in.readLine().trim();
                    if ( line.startsWith( "#" ) ) continue;
                    String[] split = StringUtils.split( line );
                    if ( split.length < 2 ) continue;
                    try {
                        double x = Double.parseDouble( split[0] );
                        double y = Double.parseDouble( split[1] );
                        series.add( x, y );
                    } catch ( NumberFormatException e ) {
                        // line wasn't useable.. no big deal. Heading is included.
                    }
                }
                results.add( series );
            }
        }
        return results;
    }

    /**
     * @param request
     * @return
     */
    private Long getEEid( HttpServletRequest request ) {
        String id = request.getParameter( "id" );

        if ( id == null ) {
            log.warn( "No id!" );
            return null;
        }
        Long idl;
        try {
            idl = Long.parseLong( id );
        } catch ( NumberFormatException e ) {
            log.warn( "Invalid id: " + id );
            return null;
        }
        assert idl != null;
        return idl;
    }

    private CategoryDataset getPCAScree( SVDValueObject svdo ) {
        DefaultCategoryDataset series = new DefaultCategoryDataset();

        Double[] variances = svdo.getVariances();
        if ( variances == null || variances.length == 0 ) {
            return series;
        }
        for ( int i = 0; i < variances.length; i++ ) {
            series.addValue( variances[i], new Integer( 1 ), new Integer( i + 1 ) );
        }
        return series;
    }

    /**
     * @param ee
     * @return
     */
    private Collection<File> locateCorrectedPvalueDistFiles( ExpressionExperiment ee ) {
        String shortName = ee.getShortName();

        Collection<File> files = new HashSet<File>();
        File directory = DifferentialExpressionFileUtils.getBaseDifferentialDirectory( shortName );
        if ( !directory.exists() ) {
            return files;
        }

        String[] fileNames = directory.list();
        String suffix = ".qvalues" + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;
        for ( String fileName : fileNames ) {
            if ( !fileName.endsWith( suffix ) ) {
                continue;
            }
            File f = new File( directory.getAbsolutePath() + File.separatorChar + fileName );
            files.add( f );
        }

        return files;
    }

    private File locateCorrMatDataFile( ExpressionExperiment ee ) {
        String shortName = ee.getShortName();
        String analysisStoragePath = ConfigUtils.getAnalysisStoragePath() + File.separatorChar
                + ExpressionDataSampleCorrelation.CORRMAT_DIR_NAME;
        File f = new File( analysisStoragePath + File.separatorChar + shortName + "_corrmat" + ".txt" );
        return f;
    }

    /**
     * @param ee
     * @param size 'large' or 'small'.
     * @param contrast
     * @return
     */
    private File locateCorrMatImageFile( ExpressionExperiment ee, String size, String contrast ) {
        // locate the image.
        String shortName = ee.getShortName();
        String analysisStoragePath = ConfigUtils.getAnalysisStoragePath() + File.separatorChar
                + ExpressionDataSampleCorrelation.CORRMAT_DIR_NAME;

        String suffix;
        if ( contrast.equalsIgnoreCase( "hi" ) ) {
            suffix = ExpressionDataSampleCorrelation.SMALL_HIGHCONTRAST;
        } else {
            suffix = ExpressionDataSampleCorrelation.SMALL_LOWCONTRAST;
        }

        if ( size != null && size.equals( "large" ) ) {
            if ( contrast.equalsIgnoreCase( "hi" ) ) {
                suffix = ExpressionDataSampleCorrelation.LARGE_HIGHCONTRAST;
            } else {
                suffix = ExpressionDataSampleCorrelation.LARGE_LOWCONTRAST;
            }
        }

        File f = new File( analysisStoragePath + File.separatorChar + shortName + "_corrmat" + suffix );
        return f;
    }

    /**
     * @param ee
     * @return
     */
    private Collection<File> locateEffectSizeDistFiles( ExpressionExperiment ee ) {
        String shortName = ee.getShortName();

        Collection<File> files = new HashSet<File>();
        File directory = DifferentialExpressionFileUtils.getBaseDifferentialDirectory( shortName );
        if ( !directory.exists() ) {
            return files;
        }

        String[] fileNames = directory.list();
        String suffix = ".scores" + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;
        for ( String fileName : fileNames ) {
            if ( !fileName.endsWith( suffix ) ) {
                continue;
            }
            File f = new File( directory.getAbsolutePath() + File.separatorChar + fileName );
            files.add( f );
        }

        return files;
    }

    private Collection<File> locatePCAFiles( ExpressionExperiment ee ) {
        Long id = ee.getId();
        Collection<File> files = new HashSet<File>();
        if ( ExpressionExperimentQCUtils.hasPCAFile( ee ) ) {
            files.add( new File( SVDServiceImpl.getReportPath( id ) ) );
        }
        return files;
    }

    /**
     * @param ee
     * @return
     */
    private File locateProbeCorrFile( ExpressionExperiment ee ) {
        String shortName = ee.getShortName();
        String analysisStoragePath = ConfigUtils.getAnalysisStoragePath();

        String suffix = ".correlDist.txt";
        File f = new File( analysisStoragePath + File.separatorChar + shortName + suffix );
        return f;
    }

    /**
     * @param ee
     * @return
     */
    private Collection<File> locatePvalueDistFiles( ExpressionExperiment ee ) {
        String shortName = ee.getShortName();

        Collection<File> files = new HashSet<File>();
        File directory = DifferentialExpressionFileUtils.getBaseDifferentialDirectory( shortName );
        if ( !directory.exists() ) {
            return files;
        }

        String[] fileNames = directory.list();
        String suffix = ".pvalues" + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;
        for ( String fileName : fileNames ) {
            if ( !fileName.endsWith( suffix ) ) {
                continue;
            }
            File f = new File( directory.getAbsolutePath() + File.separatorChar + fileName );
            files.add( f );
        }

        if ( files.isEmpty() ) {
            /*
             * Try old format - one file per resultset for backwards compatibility.
             */
            suffix = DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;
            for ( String fileName : fileNames ) {
                if ( !fileName.endsWith( suffix ) ) {
                    continue;
                }
                File f = new File( directory.getAbsolutePath() + File.separatorChar + fileName );
                files.add( f );
            }
        }

        return files;
    }

    private boolean writeCorrData( HttpServletResponse response, ExpressionExperiment ee ) {
        File f = locateCorrMatDataFile( ee );
        return writeFile( response, f );
    }

    /**
     * @param response
     * @param ee
     * @param size
     */
    private boolean writeCorrMatImage( HttpServletResponse response, ExpressionExperiment ee, String size,
            String contrast ) {
        File f = locateCorrMatImageFile( ee, size, contrast );
        return writeImage( response, f );
    }

    /**
     * @param response
     * @param f
     */
    private boolean writeFile( HttpServletResponse response, File f ) {
        if ( !f.canRead() ) {
            return false;
        }
        writeToClient( response, f, "text/plain" );
        return true;
    }

    /**
     * Write an image from a file to the user's browser FIXME move this.
     * 
     * @param response
     * @param f
     */
    private boolean writeImage( HttpServletResponse response, File f ) {
        if ( !f.canRead() ) {
            return false;
        }
        writeToClient( response, f, DEFAULT_CONTENT_TYPE );
        return true;
    }

    /**
     * @param response
     * @param ee
     * @param svdo
     */
    private void writePCAFactors( HttpServletResponse response, ExpressionExperiment ee, SVDValueObject svdo ) {
        Map<Integer, Map<Long, Double>> factorCorrelations = svdo.getFactorCorrelations();
        Map<Integer, Map<Long, Double>> factorPvalues = svdo.getFactorPvalues();
        Map<Integer, Double> dateCorrelations = svdo.getDateCorrelations();

        /*
         * TEST
         */
        // dateCorrelations.put( 0, 0.2 );
        // dateCorrelations.put( 1, 0.22 );
        // dateCorrelations.put( 2, -0.7 );

        if ( factorCorrelations.isEmpty() && factorPvalues.isEmpty() && dateCorrelations.isEmpty() ) {
            /*
             * Perhaps put in some kind of placeholder image.
             */
            return;
        }
        ee = expressionExperimentService.thawLite( ee ); // need the experimental design

        Collection<ExperimentalFactor> factors = ee.getExperimentalDesign().getExperimentalFactors();

        Map<Long, String> efs = new HashMap<Long, String>();
        for ( ExperimentalFactor ef : factors ) {
            efs.put( ef.getId(), StringUtils.abbreviate( StringUtils.capitalize( ef.getName() ), 10 ) );
        }

        DefaultCategoryDataset series = new DefaultCategoryDataset();

        /*
         * With two groups, or a continuous factor, we get rank correlations
         */
        int MAX_COMP = 3;
        for ( Integer component : factorCorrelations.keySet() ) {
            if ( component >= MAX_COMP ) break;
            for ( Long efId : factorCorrelations.get( component ).keySet() ) {
                Double corr = Math.abs( factorCorrelations.get( component ).get( efId ) );
                series.addValue( corr, "PC" + ( component + 1 ), efs.get( efId ) );
            }
        }

        for ( Integer component : dateCorrelations.keySet() ) {
            if ( component >= MAX_COMP ) break;
            Double corr = Math.abs( dateCorrelations.get( component ) );
            series.addValue( corr, "PC" + ( component + 1 ), "Date run" );
        }

        /*
         * When there are more than two groups we get pvalues from the Kruskal-Wallis test.
         */
        for ( Integer component : factorPvalues.keySet() ) {
            if ( component >= MAX_COMP ) break;

            for ( Long efId : factorPvalues.get( component ).keySet() ) {
                Double pval = factorPvalues.get( component ).get( efId );
                if ( pval == null ) continue;
                pval = -Math.log10( Math.min( 10e-4, pval ) ) / 4.0; // FIXME weak attempt to scale pvalues.
                series.addValue( pval, "PC" + ( component + 1 ), efs.get( efId ) );

            }
        }

        JFreeChart chart = ChartFactory.createBarChart( "", "Factors", "Component assoc.", series,
                PlotOrientation.VERTICAL, true, false, false );

        OutputStream out = null;
        try {
            response.setContentType( DEFAULT_CONTENT_TYPE );
            out = response.getOutputStream();
            ChartRenderingInfo info = new ChartRenderingInfo();
            chart.setBackgroundPaint( Color.white );
            ChartUtilities.writeChartAsPNG( out, chart, HISTOGRAM_IMAGE_SIZE, HISTOGRAM_IMAGE_SIZE, info );
        } catch ( IOException e ) {
            log.error( "While writing image", e );
        } finally {
            if ( out != null ) {
                try {
                    out.close();
                } catch ( IOException e ) {
                    log.warn( "Problems closing output stream.  Issues were: " + e.toString() );
                }
            }
        }
    }

    private boolean writePCAScree( HttpServletResponse response, SVDValueObject svdo ) {
        /*
         * Make a scree plot.
         */
        CategoryDataset series = getPCAScree( svdo );

        if ( series.getColumnCount() == 0 ) {
            return false;
        }

        JFreeChart chart = ChartFactory.createBarChart( "", "Component", "Fraction of var.", series,
                PlotOrientation.VERTICAL, false, false, false );

        OutputStream out = null;
        try {
            response.setContentType( DEFAULT_CONTENT_TYPE );
            out = response.getOutputStream();
            ChartRenderingInfo info = new ChartRenderingInfo();
            chart.setBackgroundPaint( Color.white );
            ChartUtilities.writeChartAsPNG( out, chart, HISTOGRAM_IMAGE_SIZE, HISTOGRAM_IMAGE_SIZE, info );
        } catch ( IOException e ) {
            log.error( "While writing image", e );
        } finally {
            if ( out != null ) {
                try {
                    out.close();
                } catch ( IOException e ) {
                    log.warn( "Problems closing output stream.  Issues were: " + e.toString() );
                }
            }
        }
        return true;
    }

    /**
     * @param response
     * @param ee
     */
    private boolean writeProbeCorrHistImage( HttpServletResponse response, ExpressionExperiment ee ) throws IOException {
        XYSeries series = getCorrelHist( ee );

        if ( series.getItemCount() == 0 ) {
            return false;
        }

        XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
        xySeriesCollection.addSeries( series );
        JFreeChart chart = ChartFactory.createXYLineChart( "", "Correlation", "Frequency", xySeriesCollection,
                PlotOrientation.VERTICAL, false, false, false );

        OutputStream out = null;
        try {
            response.setContentType( DEFAULT_CONTENT_TYPE );
            out = response.getOutputStream();
            ChartRenderingInfo info = new ChartRenderingInfo();
            chart.setBackgroundPaint( Color.white );
            ChartUtilities.writeChartAsPNG( out, chart, ( int ) ( HISTOGRAM_IMAGE_SIZE * 1.4 ), HISTOGRAM_IMAGE_SIZE,
                    info );
        } catch ( IOException e ) {
            log.error( "While writing image", e );
        } finally {
            if ( out != null ) {
                try {
                    out.close();
                } catch ( IOException e ) {
                    log.warn( "Problems closing output stream.  Issues were: " + e.toString() );
                }
            }
        }
        return true;
    }

    /**
     * Has to handle the situation where there might be more than one ResultSet.
     * 
     * @param response
     * @param ee
     * @throws IOException
     */
    private boolean writePValueHistImages( HttpServletResponse response, ExpressionExperiment ee ) throws IOException {

        Collection<XYSeries> series = getDiffExPvalueHists( ee );

        XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
        for ( XYSeries s : series ) {
            xySeriesCollection.addSeries( s );
            if ( s.getItemCount() == 0 ) {
                return false;
            }
        }
        JFreeChart chart = ChartFactory.createXYLineChart( "", "P-value", "Frequency", xySeriesCollection,
                PlotOrientation.VERTICAL, false, false, false );

        OutputStream out = null;
        try {
            response.setContentType( DEFAULT_CONTENT_TYPE );
            out = response.getOutputStream();
            ChartRenderingInfo info = new ChartRenderingInfo();
            chart.setBackgroundPaint( Color.white );
            ChartUtilities.writeChartAsPNG( out, chart, ( int ) ( HISTOGRAM_IMAGE_SIZE * 1.4 ), HISTOGRAM_IMAGE_SIZE,
                    info );
        } catch ( IOException e ) {
            log.error( "While writing image", e );
        } finally {
            if ( out != null ) {
                try {
                    out.close();
                } catch ( IOException e ) {
                    log.warn( "Problems closing output stream.  Issues were: " + e.toString() );
                }
            }
        }
        return true;
    }

    /**
     * @param response
     * @param f
     * @param contentType
     */
    private void writeToClient( HttpServletResponse response, File f, String contentType ) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream( f );
            out = response.getOutputStream();

            response.setContentType( contentType );

            byte[] buf = new byte[1024];
            int len;
            while ( ( len = in.read( buf ) ) > 0 ) {
                out.write( buf, 0, len );
            }
            in.close();
        } catch ( IOException e ) {
            log.error( "While writing image", e );
        } finally {
            if ( out != null ) {
                try {
                    out.close();
                } catch ( IOException e ) {
                    log.warn( "Problems closing output stream.  Issues were: " + e.toString() );
                }
            }
        }
    }
}
