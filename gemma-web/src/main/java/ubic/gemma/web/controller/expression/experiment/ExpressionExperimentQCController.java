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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
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

import javax.imageio.ImageIO;

import org.apache.commons.lang.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.graphics.ColorMatrix;
import ubic.basecode.graphics.MatrixDisplay;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionFileUtils;
import ubic.gemma.analysis.preprocess.svd.SVDService;
import ubic.gemma.analysis.preprocess.svd.SVDServiceImpl;
import ubic.gemma.analysis.preprocess.svd.SVDValueObject;
import ubic.gemma.analysis.stats.ExpressionDataSampleCorrelation;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.security.SecurityService;
import ubic.gemma.tasks.analysis.expression.ProcessedExpressionDataVectorCreateTask;
import ubic.gemma.tasks.analysis.expression.ProcessedExpressionDataVectorCreateTaskCommand;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.web.controller.BaseController;

/**
 * @author paul
 * @version $Id$
 */
@Controller
public class ExpressionExperimentQCController extends BaseController {

    private static final int MAX_HEATMAP_CELLSIZE = 12;
    public static final int DEFAULT_QC_IMAGE_SIZE_PX = 200;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private SVDService svdService;

    @Autowired
    private SecurityService securityService;

    /**
     * @param id
     * @param os
     * @return
     * @throws Exception
     */
    @RequestMapping("expressionExperiment/pcaFactors.html")
    public ModelAndView pcaFactors( Long id, OutputStream os ) throws Exception {
        if ( id == null ) return null;

        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            log.warn( "Could not load experiment with id " + id ); // or access deined.
            return null;
        }

        SVDValueObject svdo = svdService.retrieveSvd( ee );

        if ( svdo != null ) {
            this.writePCAFactors( os, ee, svdo );
        } else
            this.writePlaceholderImage( os );
        return null;
    }

    /**
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @RequestMapping("/expressionExperiment/pcaScree.html")
    public ModelAndView pcaScree( Long id, OutputStream os ) throws Exception {
        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            log.warn( "Could not load experiment with id " + id ); // or access deined.
            return null;
        }

        SVDValueObject svdo = svdService.retrieveSvd( ee );

        if ( svdo != null ) {
            this.writePCAScree( os, svdo );
        } else {
            writePlaceholderImage( os );
        }
        return null;
    }

    public void setExpressionExperimentService( ExpressionExperimentService ees ) {
        expressionExperimentService = ees;
    }

    @Autowired
    ProcessedExpressionDataVectorCreateTask processedExpressionDataVectorCreateTask;

    /**
     * @param id of experiment
     * @param size Multiplier on the cell size. 1 or null for standard small size.
     * @param contrVal
     * @param text if true, output a tabbed file instead of a png
     * @param showLabels if the row and column labels of the matrix should be shown.
     * @param os response output stream
     * @return
     * @throws Exception
     */
    @RequestMapping("/expressionExperiment/visualizeCorrMat.html")
    public ModelAndView visualizeCorrMat( Long id, Double size, String contrVal, Boolean text, Boolean showLabels,
            OutputStream os ) throws Exception {

        if ( id == null ) {
            log.warn( "No id!" );
            return null;
        }

        if ( StringUtils.isBlank( contrVal ) ) {
            contrVal = "hi"; // FIXME use this.
        }

        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            log.warn( "Could not load experiment with id " + id );
            return null;
        }

        File f = locateCorrMatDataFile( ee );

        if ( !f.exists() || !f.canRead() ) {

            if ( !securityService.isEditable( ee ) ) {
                writePlaceholderImage( os );
                return null;
            }

            /*
             * We try to generate it. This _could_ be slow. Should do in a task.
             */
            processedExpressionDataVectorCreateTask.execute( new ProcessedExpressionDataVectorCreateTaskCommand( ee,
                    true ) );
            f = locateCorrMatDataFile( ee ); // did we get it?
            if ( f == null ) {
                writePlaceholderImage( os );
                return null;
            }
        }

        if ( text != null && text ) {
            writeFile( os, f );
        } else {
            DoubleMatrixReader r = new DoubleMatrixReader();
            DoubleMatrix<String, String> matrix = r.read( f.getAbsolutePath() );
            ColorMatrix<String, String> cm = new ColorMatrix<String, String>( matrix );

            int row = matrix.rows();
            int cellsize = ( int ) Math
                    .min( MAX_HEATMAP_CELLSIZE, Math.max( 1, size * DEFAULT_QC_IMAGE_SIZE_PX / row ) );

            MatrixDisplay<String, String> writer = new MatrixDisplay<String, String>( cm );

            showLabels = showLabels == null ? false : showLabels && cellsize > 8;

            // writer.setLabelsVisible( showLabels == null ? false : showLabels ); // shouldn't need to do this.
            writer.setCellSize( new Dimension( cellsize, cellsize ) );

            writer.writeToPng( cm, os, showLabels /* minimum size for text to show up */);
        }
        return null; // nothing to return;
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/expressionExperiment/visualizeProbeCorrDist.html")
    public ModelAndView visualizeProbeCorrDist( Long id, OutputStream os ) throws Exception {
        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            log.warn( "Could not load experiment with id " + id );
            return null;
        }

        writeProbeCorrHistImage( os, ee );
        return null; // nothing to return;
    }

    /**
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @RequestMapping("/expressionExperiment/visualizePvalueDist.html")
    public ModelAndView visualizePvalueDist( Long id, OutputStream os ) throws Exception {
        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            log.warn( "Could not load experiment with id " + id );
            return null;
        }

        boolean ok = this.writePValueHistImages( os, ee );

        if ( !ok ) {
            writePlaceholderImage( os );
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
         * new format is to have just one file?
         */

        List<XYSeries> results = new ArrayList<XYSeries>();

        if ( fs.size() == 1 ) {

            BufferedReader in = new BufferedReader( new FileReader( fs.iterator().next() ) );
            List<String> factorNames = new ArrayList<String>();

            boolean readHeader = false;
            while ( in.ready() ) {
                String line = in.readLine().trim();
                if ( line.startsWith( "#" ) ) continue;
                String[] split = StringUtils.split( line );
                if ( split.length < 2 ) continue;

                if ( !readHeader ) {
                    for ( int i = 1; i < split.length; i++ ) {
                        String factorName = split[i];
                        factorNames.add( factorName );
                    }
                    readHeader = true;
                    continue;
                }

                try {
                    double x = Double.parseDouble( split[0] );

                    for ( int i = 1; i < split.length; i++ ) {
                        double y = Double.parseDouble( split[i] );

                        if ( results.size() < i ) {
                            results.add( new XYSeries( factorNames.get( i - 1 ), true, true ) );
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
                boolean readHeader = false;
                while ( in.ready() ) {
                    String line = in.readLine().trim();
                    if ( line.startsWith( "#" ) ) continue;
                    String[] split = StringUtils.split( line );
                    if ( split.length < 2 ) continue;

                    if ( !readHeader ) {
                        String factorName = split[1];
                        series.setKey( factorName );
                        readHeader = true;
                        continue;
                    }
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

    private CategoryDataset getPCAScree( SVDValueObject svdo ) {
        DefaultCategoryDataset series = new DefaultCategoryDataset();

        Double[] variances = svdo.getVariances();
        if ( variances == null || variances.length == 0 ) {
            return series;
        }
        int MAX_COMPONENTS_FOR_SCREE = 10; // make constant
        for ( int i = 0; i < Math.min( MAX_COMPONENTS_FOR_SCREE, variances.length ); i++ ) {
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

    /**
     * @param response
     * @param f
     */
    private boolean writeFile( OutputStream os, File f ) {
        if ( !f.canRead() ) {
            return false;
        }
        writeToClient( os, f, "text/plain" );
        return true;
    }

    /**
     * Visualization of the correlation of principal components with factors or the date samples were run.
     * 
     * @param response
     * @param ee
     * @param svdo SVD value object
     */
    private void writePCAFactors( OutputStream os, ExpressionExperiment ee, SVDValueObject svdo ) throws Exception {
        Map<Integer, Map<Long, Double>> factorCorrelations = svdo.getFactorCorrelations();
        Map<Integer, Map<Long, Double>> factorPvalues = svdo.getFactorPvalues();
        Map<Integer, Double> dateCorrelations = svdo.getDateCorrelations();

        assert ee.getId().equals( svdo.getId() );

        /*
         * TEST
         */
        // dateCorrelations.put( 0, 0.2 );
        // dateCorrelations.put( 1, 0.22 );
        // dateCorrelations.put( 2, -0.7 );

        if ( factorCorrelations.isEmpty() && factorPvalues.isEmpty() && dateCorrelations.isEmpty() ) {
            writePlaceholderImage( os );
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
                Double a = factorCorrelations.get( component ).get( efId );
                String facname = efs.get( efId ) == null ? "?" : efs.get( efId );
                if ( a != null && !Double.isNaN( a ) ) {
                    Double corr = Math.abs( a );
                    series.addValue( corr, "PC" + ( component + 1 ), facname );
                }
            }
        }

        for ( Integer component : dateCorrelations.keySet() ) {
            if ( component >= MAX_COMP ) break;
            Double a = dateCorrelations.get( component );
            if ( a != null && !Double.isNaN( a ) ) {
                Double corr = Math.abs( a );
                series.addValue( corr, "PC" + ( component + 1 ), "Date run" );
            }
        }

        // /*
        // * When there are more than two groups we get pvalues from the Kruskal-Wallis test. FIXME not used.
        // */
        // for ( Integer component : factorPvalues.keySet() ) {
        // if ( component >= MAX_COMP ) break;
        //
        // for ( Long efId : factorPvalues.get( component ).keySet() ) {
        // Double pval = factorPvalues.get( component ).get( efId );
        // if ( pval == null || Double.isNaN( pval ) ) continue;
        // // pval = -Math.log10( Math.max( 10e-4, pval ) ) / 4.0; // FIXME weak attempt to scale pvalues between 0
        // double probit = -Probability.normalInverse( pval );
        // // and 1.
        // series.addValue( probit, "PC" + ( component + 1 ), efs.get( efId ) );
        //
        // }
        // }

        JFreeChart chart = ChartFactory.createBarChart( "", "Factors", "Component assoc.", series,
                PlotOrientation.VERTICAL, true, false, false );

        CategoryItemRenderer renderer = chart.getCategoryPlot().getRenderer();
        CategoryAxis domainAxis = chart.getCategoryPlot().getDomainAxis();
        domainAxis.setCategoryLabelPositions( CategoryLabelPositions.UP_45 );
        for ( int i = 0; i < MAX_COMP; i++ ) {
            renderer.setSeriesPaint( i, Color.getHSBColor( 0.0f, 1.0f - ( ( 3 * ( i + 1.0f ) ) / ( 3 * MAX_COMP ) ),
                    0.7f ) );

        }

        /*
         * Give figure more room .. up to a limit
         */
        int width = DEFAULT_QC_IMAGE_SIZE_PX;
        if ( chart.getCategoryPlot().getCategories().size() > 3 ) {
            width = width + 40 * ( chart.getCategoryPlot().getCategories().size() - 2 );
        }
        int MAX_QC_IMAGE_SIZE_PX = 500;
        width = Math.min( width, MAX_QC_IMAGE_SIZE_PX );
        ChartUtilities.writeChartAsPNG( os, chart, width, DEFAULT_QC_IMAGE_SIZE_PX );
    }

    /**
     * @param response
     * @param svdo
     * @return
     */
    private boolean writePCAScree( OutputStream os, SVDValueObject svdo ) throws Exception {
        /*
         * Make a scree plot.
         */
        CategoryDataset series = getPCAScree( svdo );

        if ( series.getColumnCount() == 0 ) {
            return false;
        }
        int MAX_COMPONENTS_FOR_SCREE = 10;
        JFreeChart chart = ChartFactory.createBarChart( "", "Component (up to" + MAX_COMPONENTS_FOR_SCREE + ")",
                "Fraction of var.", series, PlotOrientation.VERTICAL, false, false, false );

        ChartUtilities.writeChartAsPNG( os, chart, DEFAULT_QC_IMAGE_SIZE_PX, DEFAULT_QC_IMAGE_SIZE_PX );
        return true;
    }

    private void writePlaceholderImage( OutputStream os ) throws IOException {
        // put in a placeholder image.
        BufferedImage buffer = new BufferedImage( DEFAULT_QC_IMAGE_SIZE_PX, DEFAULT_QC_IMAGE_SIZE_PX,
                BufferedImage.TYPE_INT_RGB );
        Graphics g = buffer.createGraphics();
        g.setColor( Color.lightGray );
        g.fillRect( 0, 0, DEFAULT_QC_IMAGE_SIZE_PX, DEFAULT_QC_IMAGE_SIZE_PX );
        g.setColor( Color.black );
        g.drawString( "Not available", DEFAULT_QC_IMAGE_SIZE_PX / 4, DEFAULT_QC_IMAGE_SIZE_PX / 4 );
        ImageIO.write( buffer, "png", os );
    }

    /**
     * @param response
     * @param ee
     */
    private boolean writeProbeCorrHistImage( OutputStream os, ExpressionExperiment ee ) throws IOException {
        XYSeries series = getCorrelHist( ee );

        if ( series.getItemCount() == 0 ) {
            return false;
        }

        XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
        xySeriesCollection.addSeries( series );
        JFreeChart chart = ChartFactory.createXYLineChart( "", "Correlation", "Frequency", xySeriesCollection,
                PlotOrientation.VERTICAL, false, false, false );

        ChartUtilities
                .writeChartAsPNG( os, chart, ( int ) ( DEFAULT_QC_IMAGE_SIZE_PX * 1.4 ), DEFAULT_QC_IMAGE_SIZE_PX );

        return true;
    }

    /**
     * Has to handle the situation where there might be more than one ResultSet.
     * 
     * @param response
     * @param ee
     * @throws IOException
     */
    private boolean writePValueHistImages( OutputStream os, ExpressionExperiment ee ) throws IOException {

        Collection<XYSeries> series = getDiffExPvalueHists( ee );

        XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
        for ( XYSeries s : series ) {
            xySeriesCollection.addSeries( s );
            if ( s.getItemCount() == 0 ) {
                return false;
            }
        }
        JFreeChart chart = ChartFactory.createXYLineChart( "", "P-value", "Frequency", xySeriesCollection,
                PlotOrientation.VERTICAL, true, false, false );
        chart.getXYPlot().setRangeGridlinesVisible( false );
        chart.getXYPlot().setDomainGridlinesVisible( false );

        ChartUtilities
                .writeChartAsPNG( os, chart, ( int ) ( DEFAULT_QC_IMAGE_SIZE_PX * 1.4 ), DEFAULT_QC_IMAGE_SIZE_PX );
        return true;
    }

    /**
     * @param response
     * @param f
     * @param contentType
     */
    private void writeToClient( OutputStream os, File f, String contentType ) {

        try {
            InputStream in = new FileInputStream( f );
            byte[] buf = new byte[1024];
            int len;
            while ( ( len = in.read( buf ) ) > 0 ) {
                os.write( buf, 0, len );
            }
            in.close();
        } catch ( IOException e ) {
            log.error( "While writing content", e );
        }
    }
}
