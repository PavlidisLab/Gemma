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

import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.doublealgo.Formatter;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.jet.stat.Descriptive;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.ScatterRenderer;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.DefaultMultiValueCategoryDataset;
import org.jfree.data.time.Hour;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.graphics.ColorMatrix;
import ubic.basecode.graphics.MatrixDisplay;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.io.writer.MatrixWriter;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.distribution.Histogram;
import ubic.gemma.core.analysis.preprocess.OutlierDetails;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.analysis.preprocess.svd.SVDValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalDesignUtils;
import ubic.gemma.core.datastructure.matrix.ExperimentalDesignWriter;
import ubic.gemma.core.datastructure.matrix.ExpressionDataWriterUtils;
import ubic.gemma.model.analysis.expression.coexpression.CoexpCorrelationDistribution;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueUtils;
import ubic.gemma.persistence.service.analysis.expression.coexpression.CoexpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.core.config.Settings;
import ubic.gemma.web.controller.BaseController;
import ubic.gemma.web.util.EntityNotFoundException;
import ubic.gemma.web.view.TextView;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.DecimalFormat;
import java.util.List;
import java.util.*;

//

/**
 * @author paul
 */
@Controller
public class ExpressionExperimentQCController extends BaseController {

    public static final int DEFAULT_QC_IMAGE_SIZE_PX = 200;
    private static final int MAX_HEATMAP_CELLSIZE = 12;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private SVDService svdService;
    @Autowired
    private SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService;
    @Autowired
    private OutlierDetectionService outlierDetectionService;
    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;
    @Autowired
    private CoexpressionAnalysisService coexpressionAnalysisService;

    @RequestMapping(value = "/expressionExperiment/detailedFactorAnalysis.html", method = RequestMethod.GET)
    public void detailedFactorAnalysis( @RequestParam("id") Long id, HttpServletResponse response ) throws Exception {
        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            log.warn( "Could not load experiment with id " + id );
            return;
        }

        this.writeDetailedFactorAnalysis( ee, response );
    }

    @RequestMapping(value = "/expressionExperiment/outliersRemoved.html", method = RequestMethod.GET)
    public ModelAndView identifyOutliersRemoved( @RequestParam("id") Long id ) throws IOException {
        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            log.warn( "Could not load experiment with id " + id );
            return null;
        }

        ee = expressionExperimentService.thawLite( ee );
        Collection<BioAssay> bioAssays = new HashSet<>();
        for ( BioAssay assay : ee.getBioAssays() ) {
            if ( assay.getIsOutlier() ) {
                bioAssays.add( assay );
            }
        }

        // and write it out
        StringWriter writer = new StringWriter();
        StringBuffer buf = writer.getBuffer();

        ExpressionDataWriterUtils.appendBaseHeader( ee, "Outliers removed", buf );

        ExperimentalDesignWriter edWriter = new ExperimentalDesignWriter();
        ee = expressionExperimentService.thawLiter( ee );
        edWriter.write( writer, ee, bioAssays, false, true );

        ModelAndView mav = new ModelAndView( new TextView() );
        mav.addObject( TextView.TEXT_PARAM, buf.toString() );
        return mav;
    }

    @RequestMapping(value = "/expressionExperiment/possibleOutliers.html", method = RequestMethod.GET)
    public ModelAndView identifyPossibleOutliers( @RequestParam("id") Long id ) throws IOException {
        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            log.warn( "Could not load experiment with id " + id );
            return null;
        }

        // identify outliers
        if ( !sampleCoexpressionAnalysisService.hasAnalysis( ee ) ) {
            log.warn( "Experiment doesn't have correlation matrix computed (will not create right now)" );
            return null;
        }

        DoubleMatrix<BioAssay, BioAssay> sampleCorrelationMatrix = null;
        try {
            sampleCorrelationMatrix = sampleCoexpressionAnalysisService
                    .loadRegressedMatrix( ee );
        } catch ( FilteringException e ) {
            log.warn( "Error when filtering the expression data matrix for " + ee + ", nothing will be displayed.", e );
        }
        if ( sampleCorrelationMatrix == null || sampleCorrelationMatrix.rows() < 3 ) {
            return null;
        }

        Collection<OutlierDetails> outliers = outlierDetectionService
                .identifyOutliersByMedianCorrelation( sampleCorrelationMatrix );

        Collection<BioAssay> bioAssays = new HashSet<>();
        if ( !outliers.isEmpty() ) {
            for ( OutlierDetails details : outliers ) {
                bioAssays.add( details.getBioAssay() );

            }
        }

        // and write it out
        StringWriter writer = new StringWriter();
        StringBuffer buf = writer.getBuffer();

        ExpressionDataWriterUtils.appendBaseHeader( ee, "Sample outlier", buf );

        ExperimentalDesignWriter edWriter = new ExperimentalDesignWriter();
        ee = expressionExperimentService.thawLiter( ee );
        edWriter.write( writer, ee, bioAssays, false, true );

        ModelAndView mav = new ModelAndView( new TextView() );
        mav.addObject( TextView.TEXT_PARAM, buf.toString() );
        return mav;
    }

    @RequestMapping(value = "/expressionExperiment/pcaFactors.html", method = RequestMethod.GET)
    public void pcaFactors( @RequestParam("id") Long id, HttpServletResponse response ) throws Exception {
        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            log.warn( "Could not load experiment with id " + id ); // or access denied.
            this.writePlaceholderImage( response );
            return;
        }

        SVDValueObject svdo = null;
        try {
            svdo = svdService.getSvdFactorAnalysis( ee.getId() );
        } catch ( Exception e ) {
            // if there is no pca
            // log.error( e, e );
        }

        if ( svdo != null ) {
            this.writePCAFactors( ee, svdo, response );
        } else {
            this.writePlaceholderImage( response );
        }
    }

    @RequestMapping(value = "/expressionExperiment/pcaScree.html", method = RequestMethod.GET)
    public void pcaScree( @RequestParam("id") Long id, HttpServletResponse response ) throws Exception {
        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            log.warn( "Could not load experiment with id " + id ); // or access deined.
            this.writePlaceholderImage( response );
            return;
        }

        SVDValueObject svdo = svdService.getSvd( ee.getId() );

        if ( svdo != null ) {
            this.writePCAScree( svdo, response );
        } else {
            this.writePlaceholderImage( response );
        }
    }

    /**
     * @param id of experiment
     * @param size Multiplier on the cell size. 1 or null for standard small size.
     * @param text if true, output a tabbed file instead of a png
     * @param showLabels if the row and column labels of the matrix should be shown.
     * @param forceShowLabels forces the display of labels in the picture
     * @param reg uses the regressed matrix (if available).
     */
    @RequestMapping(value = "/expressionExperiment/visualizeCorrMat.html", method = RequestMethod.GET)
    public void visualizeCorrMat(
            @RequestParam("id") Long id,
            @RequestParam(value = "size", required = false) Double size,
            @RequestParam(value = "contrVal", required = false) String contrVal,
            @RequestParam(value = "text", required = false) Boolean text,
            @RequestParam(value = "showLabels", required = false) Boolean showLabels,
            @RequestParam(value = "forceShowLabels", required = false) Boolean forceShowLabels,
            @RequestParam(value = "reg", required = false) Boolean reg,
            HttpServletResponse response ) throws Exception {
        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            log.warn( "Could not load experiment with id " + id );
            return;
        }

        ee = expressionExperimentService.thawLiter( ee );
        DoubleMatrix<BioAssay, BioAssay> omatrix = ( reg != null && reg ) ? sampleCoexpressionAnalysisService.loadBestMatrix( ee )
                : sampleCoexpressionAnalysisService.loadFullMatrix( ee );
        if ( omatrix == null ) {
            log.warn( "No correlation matrix for ee " + id );
            return;
        }

        List<String> stringNames = new ArrayList<>();
        for ( BioAssay ba : omatrix.getRowNames() ) {
            stringNames.add( ba.getName() + " ID=" + ba.getId() );
        }
        DoubleMatrix<String, String> matrix = new DenseDoubleMatrix<>( omatrix.getRawMatrix() );
        matrix.setRowNames( stringNames );
        matrix.setColumnNames( stringNames );

        if ( text != null && text ) {
            StringWriter s = new StringWriter();
            MatrixWriter<String, String> mw = new MatrixWriter<>( s, new DecimalFormat( "#.##" ) );
            mw.writeMatrix( matrix, true );
            // This does not solve the root issue, but I wasted too much time on it
            response.setContentType( "text/tab-separated-values" );
            response.getOutputStream().write( s.toString().replace( "\uFFFD", "\t" ).getBytes() );
            return;
        }

        /*
         * Blank out the diagonal so it doesn't affect the colour scale.
         */
        for ( int i = 0; i < matrix.rows(); i++ ) {
            matrix.set( i, i, Double.NaN );
        }

        ColorMatrix<String, String> cm = new ColorMatrix<>( matrix );

        int row = matrix.rows();
        int cellsize = ( int ) Math.min( ExpressionExperimentQCController.MAX_HEATMAP_CELLSIZE,
                Math.max( 1, size * ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX / row ) );

        MatrixDisplay<String, String> writer = new MatrixDisplay<>( cm );

        boolean reallyShowLabels;
        int minimumCellSizeForText = 9;
        if ( forceShowLabels != null && forceShowLabels ) {
            cellsize = minimumCellSizeForText;
            reallyShowLabels = true;
        } else {
            reallyShowLabels = showLabels != null && ( showLabels && cellsize >= minimumCellSizeForText );
        }

        writer.setCellSize( new Dimension( cellsize, cellsize ) );
        boolean showScalebar = size > 2;
        response.setContentType( MediaType.IMAGE_PNG_VALUE );
        writer.writeToPng( cm, response.getOutputStream(), reallyShowLabels, showScalebar );
    }

    /**
     * @param id of experiment
     * @param size Multiplier on the cell size. 1 or null for standard small size.
     * @param text if true, output a tabbed file instead of a png
     * @return ModelAndView object if text is true, otherwise null
     */
    @RequestMapping(value = "/expressionExperiment/visualizeMeanVariance.html", method = RequestMethod.GET)
    public ModelAndView visualizeMeanVariance(
            @RequestParam("id") Long id,
            @RequestParam(value = "size", required = false) Double size,
            @RequestParam(value = "text", required = false) Boolean text,
            HttpServletResponse response ) throws Exception {
        ExpressionExperiment ee = expressionExperimentService.loadWithMeanVarianceRelation( id );
        if ( ee == null ) {
            log.warn( "Could not load experiment with id " + id );
            return null;
        }

        MeanVarianceRelation mvr = ee.getMeanVarianceRelation();

        if ( mvr == null ) {
            log.warn( "EE " + id + " does not have a mean-variance relation." );
            return null;
        }

        if ( text != null && text ) {
            final ByteArrayConverter bac = new ByteArrayConverter();

            double[] means = bac.byteArrayToDoubles( mvr.getMeans() );
            double[] variances = bac.byteArrayToDoubles( mvr.getVariances() );

            DoubleMatrix2D matrix = new DenseDoubleMatrix2D( means.length, 2 );
            matrix.viewColumn( 0 ).assign( means );
            matrix.viewColumn( 1 ).assign( variances );

            String matrixString = new Formatter( "%1.2G" )
                    .toTitleString( matrix, null, new String[] { "mean", "variance" }, null, null, null, null );
            return new ModelAndView( new TextView( "tab-separated-values" ) )
                    .addObject( TextView.TEXT_PARAM, matrixString );
        }

        // FIXME might be something better to do
        response.setContentType( MediaType.IMAGE_PNG_VALUE );
        writeMeanVariance( mvr, size, response );
        return null;
    }

    @RequestMapping(value = "/expressionExperiment/visualizeProbeCorrDist.html", method = RequestMethod.GET)
    public void visualizeProbeCorrDist( @RequestParam("id") Long id, HttpServletResponse response ) throws Exception {
        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            log.warn( "Could not load experiment with id " + id );
            return;
        }
        writeProbeCorrHistImage( ee, response );
    }

    /**
     * @param id of the experiment
     * @param analysisId of the analysis
     * @param rsid resultSet Id
     * @param factorName deprecated, we will use rsId instead. Maintained for backwards compatibility.
     * @param size of the image.
     * @param response stream to write the image to.
     */
    @RequestMapping(value = "/expressionExperiment/visualizePvalueDist.html", method = RequestMethod.GET)
    public void visualizePvalueDist(
            @RequestParam("id") Long id,
            @RequestParam("analysisId") Long analysisId,
            @RequestParam("rsid") Long rsid,
            @RequestParam(value = "factorName", required = false) String factorName,
            @RequestParam(value = "size", required = false) Integer size,
            HttpServletResponse response ) throws Exception {
        ExpressionExperiment ee = this.expressionExperimentService.loadOrFail( id, EntityNotFoundException::new,
                "Could not load experiment with id " + id );
        if ( size == null ) {
            writePValueHistImage( ee, analysisId, rsid, factorName, response );
        } else {
            writePValueHistThumbnailImage( ee, analysisId, rsid, factorName, size, response );
        }
    }

    @RequestMapping(value = "/expressionExperiment/eigenGenes.html", method = RequestMethod.GET)
    public ModelAndView writeEigenGenes( @RequestParam("eeid") Long eeid ) throws IOException {
        ExpressionExperiment ee = expressionExperimentService.loadOrFail( eeid,
                EntityNotFoundException::new, "Could not load experiment with id " + eeid );// or access deined.
        SVDValueObject svdo = svdService.getSvd( ee.getId() );

        DoubleMatrix<Long, Integer> vMatrix = svdo.getvMatrix();

        /*
         * FIXME put the biomaterial names in there instead of the IDs.
         */
        /*
         * new DenseDoubleMatrix<String, String>() DoubleMatrix<String, String> matrix = new DenseDoubleMatrix<String,
         * String>( omatrix.getRawMatrix() ); matrix.setRowNames( stringNames ); matrix.setColumnNames( stringNames );
         */
        StringWriter s = new StringWriter();
        MatrixWriter<Long, Integer> mw = new MatrixWriter<>( s, new DecimalFormat( "#.######" ) );
        mw.writeMatrix( vMatrix, true );
        return new ModelAndView( new TextView( "tab-separated-values" ) )
                .addObject( TextView.TEXT_PARAM, s.toString() );
    }

    private void addChartToGraphics( JFreeChart chart, Graphics2D g2, double x, double y, double width,
            double height ) {
        chart.draw( g2, new Rectangle2D.Double( x, y, width, height ), null, null );
    }

    /**
     * Support method for writeDetailedFactorAnalysis
     *
     * @param categories map of factor ID to text value. Strings will be unique, but possibly abbreviated and/or munged.
     */
    private void getCategories( Map<Long, ExperimentalFactor> efIdMap, Long efId, Map<Long, String> categories ) {
        ExperimentalFactor ef = efIdMap.get( efId );
        if ( ef == null )
            return;
        int maxCategoryLabelLength = 10;

        for ( FactorValue fv : ef.getFactorValues() ) {
            String value = FactorValueUtils.getSummaryString( fv, "; " );

            if ( StringUtils.isBlank( value ) ) {
                value = fv + "--??";
            }

            if ( value.startsWith( ExperimentalDesignUtils.BATCH_FACTOR_NAME_PREFIX ) ) {
                value = value.replaceFirst( ExperimentalDesignUtils.BATCH_FACTOR_NAME_PREFIX, "" );
            } else {
                value = StringUtils.abbreviate( value, maxCategoryLabelLength );
            }

            while ( categories.containsValue( value ) ) {
                value = value + "+";// make unique, kludge, will end up with string of ++++
            }

            categories.put( fv.getId(), value );

        }
    }

    /**
     * @return JFreeChart XYSeries representing the histogram.
     * @throws FileNotFoundException - only if the coexp dist is being read from a file; when migration to db storage is
     *         complete this can be removed
     * @throws IOException - only if the coexp dist is being read from a file; when migration to db storage is complete
     *         this can be removed
     */
    private XYSeries getCorrelHist( ExpressionExperiment ee ) throws IOException {
        CoexpCorrelationDistribution coexpCorrelationDistribution = coexpressionAnalysisService
                .getCoexpCorrelationDistribution( ee );

        if ( coexpCorrelationDistribution == null ) {
            // try to get it from the file.
            return this.getCorrelHistFromFile( ee );
        }

        XYSeries series = new XYSeries( ee.getId(), true, true );

        byte[] binCountsBytes = coexpCorrelationDistribution.getBinCounts();
        ByteArrayConverter bac = new ByteArrayConverter();
        double[] binCounts = bac.byteArrayToDoubles( binCountsBytes );
        Integer numBins = coexpCorrelationDistribution.getNumBins();

        double step = 2.0 / numBins;

        double lim = -1.0;

        for ( double d : binCounts ) {
            series.add( lim, d );
            lim += step;
        }
        return series;

    }

    /**
     * For backwards compatibility - read from the file. Remove this method when no longer needed.
     */
    private XYSeries getCorrelHistFromFile( ExpressionExperiment ee ) throws IOException {

        File file = this.locateProbeCorrFile( ee );

        // Current format is to have just one file for each analysis.
        if ( !file.canRead() ) {
            return null;
        }

        try ( BufferedReader in = new BufferedReader( new FileReader( file ) ) ) {
            XYSeries series = new XYSeries( ee.getId(), true, true );
            DoubleArrayList counts = new DoubleArrayList();

            while ( in.ready() ) {
                String line = in.readLine().trim();
                if ( line.startsWith( "#" ) )
                    continue;
                String[] split = StringUtils.split( line );
                if ( split.length < 2 )
                    continue;
                try {
                    double x = Double.parseDouble( split[0] );
                    double y = Double.parseDouble( split[1] );
                    series.add( x, y );
                    counts.add( y );
                } catch ( NumberFormatException e ) {
                    // line wasn't useable.. no big deal. Heading is included.
                }
            }

            if ( !counts.isEmpty() ) {
                // Backfill.
                this.corrDistFileToPersistent( file, ee, counts );
            }

            return series;

        }
    }

    /**
     * @return JFreeChart XYSeries representing the histogram for the requested result set
     */
    private XYSeries getDiffExPvalueHistXYSeries( ExpressionExperiment ee, Long analysisId, Long rsId,
            String factorName ) {
        if ( ee == null || analysisId == null || rsId == null ) {
            log.warn( "Got invalid values: " + ee + " " + analysisId + " " + rsId + " " + factorName );
            return null;
        }

        Histogram hist = differentialExpressionResultService.loadPvalueDistribution( rsId );

        XYSeries xySeries;

        if ( hist != null ) {
            xySeries = new XYSeries( rsId, true, true );
            Double[] binEdges = hist.getBinEdges();
            double[] counts = hist.getArray();
            assert binEdges.length == counts.length;
            for ( int i = 0; i < binEdges.length; i++ ) {
                xySeries.add( binEdges[i].doubleValue(), counts[i] );
            }
            return xySeries;
        }
        return null;
    }

    /**
     * Get the eigengene for the given component.
     * The values are rescaled so that jfreechart can cope. Small numbers give it fits.
     */
    private Double[] getEigenGene( SVDValueObject svdo, Integer component ) {
        DoubleArrayList eigenGeneL = new DoubleArrayList(
                ArrayUtils.toPrimitive( svdo.getvMatrix().getColObj( component ) ) );
        DescriptiveWithMissing.standardize( eigenGeneL );
        return ArrayUtils.toObject( eigenGeneL.elements() );
    }

    private Map<Long, String> getFactorNames( ExpressionExperiment ee, int maxWidth ) {
        Collection<ExperimentalFactor> factors = ee.getExperimentalDesign().getExperimentalFactors();

        Map<Long, String> efs = new HashMap<>();
        for ( ExperimentalFactor ef : factors ) {
            efs.put( ef.getId(), StringUtils.abbreviate( StringUtils.capitalize( ef.getName() ), maxWidth ) );
        }
        return efs;
    }

    /**
     * @param mvr MeanVarianceRelation object that contains the datapoints to plot
     * @return XYSeriesCollection which contains the Mean-variance and Loess series
     */
    private XYSeriesCollection getMeanVariance( MeanVarianceRelation mvr ) {

        final ByteArrayConverter bac = new ByteArrayConverter();

        XYSeriesCollection dataset = new XYSeriesCollection();

        if ( mvr == null ) {
            return dataset;
        }

        double[] means = bac.byteArrayToDoubles( mvr.getMeans() );
        double[] variances = bac.byteArrayToDoubles( mvr.getVariances() );

        if ( means == null || variances == null ) {
            return dataset;
        }

        XYSeries series = new XYSeries( "Mean-variance" );
        for ( int i = 0; i < means.length; i++ ) {
            series.add( means[i], variances[i] );
        }

        dataset.addSeries( series );

        return dataset;
    }

    private CategoryDataset getPCAScree( SVDValueObject svdo ) {
        DefaultCategoryDataset series = new DefaultCategoryDataset();

        double[] variances = svdo.getVariances();
        if ( variances == null || variances.length == 0 ) {
            return series;
        }
        int MAX_COMPONENTS_FOR_SCREE = 10; // make constant
        for ( int i = 0; i < Math.min( MAX_COMPONENTS_FOR_SCREE, variances.length ); i++ ) {
            series.addValue( variances[i], Integer.valueOf( 1 ), Integer.valueOf( i + 1 ) );
        }
        return series;
    }

    /**
     * For backwards compatibility only; remove when no longer needed.
     */
    private File locateProbeCorrFile( ExpressionExperiment ee ) {
        String shortName = ee.getShortName();
        String analysisStoragePath = Settings.getAnalysisStoragePath();

        String suffix = ".correlDist.txt";
        return new File( analysisStoragePath + File.separatorChar + shortName + suffix );
    }

    /**
     * For conversion from legacy system.
     */
    private void corrDistFileToPersistent( File file, ExpressionExperiment ee, DoubleArrayList counts ) {
        log.info( "Converting from pvalue distribution file to persistent stored version" );
        ByteArrayConverter bac = new ByteArrayConverter();
        byte[] bytes = bac.doubleArrayToBytes( counts );

        CoexpCorrelationDistribution coexpd = CoexpCorrelationDistribution.Factory.newInstance();
        coexpd.setNumBins( counts.size() );
        coexpd.setBinCounts( bytes );

        try {
            coexpressionAnalysisService.addCoexpCorrelationDistribution( ee, coexpd );

            if ( file.delete() ) {
                log.info( "Old file deleted" );
            } else {
                log.info( "Old file could not be deleted" );
            }
        } catch ( Exception e ) {
            log.info( "Could not save the corr dist: " + e.getMessage() );
        }
    }

    private void writeDetailedFactorAnalysis( ExpressionExperiment ee, HttpServletResponse os ) throws Exception {
        SVDValueObject svdo = svdService.getSvdFactorAnalysis( ee.getId() );
        if ( svdo == null ) {
            writePlaceholderImage( os );
            return;
        }

        if ( svdo.getFactors().isEmpty() && svdo.getDates().isEmpty() ) {
            writePlaceholderImage( os );
            return;
        }
        Map<Integer, Map<Long, Double>> factorCorrelations = svdo.getFactorCorrelations();
        // Map<Integer, Map<Long, Double>> factorPvalues = svdo.getFactorPvalues();
        Map<Integer, Double> dateCorrelations = svdo.getDateCorrelations();

        assert ee.getId().equals( svdo.getId() );

        ee = expressionExperimentService.thawLite( ee ); // need the experimental design
        int maxWidth = 30;
        Map<Long, String> efs = this.getFactorNames( ee, maxWidth );
        Map<Long, ExperimentalFactor> efIdMap = EntityUtils
                .getIdMap( ee.getExperimentalDesign().getExperimentalFactors() );
        Collection<Long> continuousFactors = new HashSet<>();
        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            boolean isContinous = ExperimentalDesignUtils.isContinuous( ef );
            if ( isContinous ) {
                continuousFactors.add( ef.getId() );
            }
        }

        /*
         * Make plots of the dates vs. PCs, factors vs. PCs.
         */
        int MAX_COMP = 3;

        Map<Long, List<JFreeChart>> charts = new LinkedHashMap<>();
        ChartFactory.setChartTheme( StandardChartTheme.createLegacyTheme() );
        /*
         * FACTORS
         */
        String componentShorthand = "PC";
        for ( Integer component : factorCorrelations.keySet() ) {

            if ( component >= MAX_COMP )
                break;
            String xaxisLabel = componentShorthand + ( component + 1 );

            for ( Long efId : factorCorrelations.get( component ).keySet() ) {

                /*
                 * Should not happen.
                 */
                if ( !efs.containsKey( efId ) ) {
                    log.warn( "No experimental factor with id " + efId );
                    continue;
                }

                if ( !svdo.getFactors().containsKey( efId ) ) {
                    // this should not happen.
                    continue;
                }

                boolean isCategorical = !continuousFactors.contains( efId );

                Map<Long, String> categories = new HashMap<>();

                if ( isCategorical ) {
                    this.getCategories( efIdMap, efId, categories );
                }

                if ( !charts.containsKey( efId ) ) {
                    charts.put( efId, new ArrayList<>() );
                }

                Double a = factorCorrelations.get( component ).get( efId );
                String plotname = ( efs.get( efId ) == null ? "?" : efs.get( efId ) ) + " " + xaxisLabel; // unique?

                if ( a != null && !Double.isNaN( a ) ) {
                    String title = plotname + " " + String.format( "%.2f", a );
                    List<Double> values = svdo.getFactors().get( efId );
                    Double[] eigenGene = this.getEigenGene( svdo, component );
                    assert values.size() == eigenGene.length;

                    /*
                     * Plot eigengene vs values, add correlation to the plot
                     */
                    JFreeChart chart;
                    if ( isCategorical ) {

                        /*
                         * Categorical factor
                         */

                        // use the absolute value of the correlation, since direction is arbitrary.
                        title = plotname + " " + String.format( "r=%.2f", Math.abs( a ) );

                        DefaultMultiValueCategoryDataset dataset = new DefaultMultiValueCategoryDataset();

                        /*
                         * What this code does is organize the factor values by the groups.
                         */
                        Map<String, List<Double>> groupedValues = new TreeMap<>();
                        for ( int i = 0; i < values.size(); i++ ) {
                            Long fvId = values.get( i ).longValue();
                            String fvValue = categories.get( fvId );
                            if ( fvValue == null ) {
                                /*
                                 * Problem ...eg gill2006fateinocean id=1748 -- missing values. We just don't plot
                                 * anything for this sample.
                                 */
                                continue; // is this all we need to do?
                            }
                            if ( !groupedValues.containsKey( fvValue ) ) {
                                groupedValues.put( fvValue, new ArrayList<>() );
                            }

                            groupedValues.get( fvValue ).add( eigenGene[i] );

                            if ( log.isDebugEnabled() )
                                log.debug( fvValue + " " + values.get( i ) );
                        }

                        for ( String key : groupedValues.keySet() ) {
                            dataset.add( groupedValues.get( key ), plotname, key );
                        }

                        // don't show the name of the X axis: it's redundant with the title.
                        NumberAxis rangeAxis = new NumberAxis( xaxisLabel );
                        rangeAxis.setAutoRangeIncludesZero( false );
                        // rangeAxis.setAutoRange( false );
                        rangeAxis.setAutoRangeMinimumSize( 4.0 );
                        // rangeAxis.setRange( new Range( -2, 2 ) );

                        CategoryPlot plot = new CategoryPlot( dataset, new CategoryAxis( null ), rangeAxis,
                                new ScatterRenderer() );
                        plot.setRangeGridlinesVisible( false );
                        plot.setDomainGridlinesVisible( false );

                        chart = new JFreeChart( title, new Font( "SansSerif", Font.BOLD, 12 ), plot, false );

                        ScatterRenderer renderer = ( ScatterRenderer ) plot.getRenderer();
                        float saturationDrop = ( float ) Math.min( 1.0, component * 0.8f / MAX_COMP );
                        renderer.setSeriesFillPaint( 0, Color.getHSBColor( 0.0f, 1.0f - saturationDrop, 0.7f ) );
                        renderer.setSeriesShape( 0, new Ellipse2D.Double( 0, 0, 3, 3 ) );
                        renderer.setUseOutlinePaint( false );
                        renderer.setUseFillPaint( true );
                        renderer.setDefaultFillPaint( Color.white );
                        CategoryAxis domainAxis = plot.getDomainAxis();
                        domainAxis.setCategoryLabelPositions( CategoryLabelPositions.UP_45 );
                    } else {

                        /*
                         * Continuous value factor
                         */

                        DefaultXYDataset series = new DefaultXYDataset();
                        series.addSeries( plotname,
                                new double[][] { ArrayUtils.toPrimitive( values.toArray( new Double[] {} ) ),
                                        ArrayUtils.toPrimitive( eigenGene ) } );

                        // don't show x-axis label, which would otherwise be efs.get( efId )
                        chart = ChartFactory
                                .createScatterPlot( title, null, xaxisLabel, series, PlotOrientation.VERTICAL, false,
                                        false, false );
                        XYPlot plot = chart.getXYPlot();
                        plot.setRangeGridlinesVisible( false );
                        plot.setDomainGridlinesVisible( false );

                        XYItemRenderer renderer = plot.getRenderer();
                        renderer.setDefaultPaint( Color.white );
                        renderer.setSeriesShape( 0, new Ellipse2D.Double( 0, 0, 3, 3 ) );
                        float saturationDrop = ( float ) Math.min( 1.0, component * 0.8f / MAX_COMP );
                        renderer.setSeriesPaint( 0, Color.getHSBColor( 0.0f, 1.0f - saturationDrop, 0.7f ) );
                        plot.setRenderer( renderer );
                    }

                    chart.getTitle().setFont( new Font( "SansSerif", Font.BOLD, 12 ) );

                    charts.get( efId ).add( chart );
                }
            }
        }

        /*
         * DATES
         */
        charts.put( -1L, new ArrayList<>() );
        for ( Integer component : dateCorrelations.keySet() ) {
            String xaxisLabel = componentShorthand + ( component + 1 );

            List<Date> dates = svdo.getDates();
            if ( dates.isEmpty() )
                break;

            long secspan = ubic.basecode.util.DateUtil.numberOfSecondsBetweenDates( dates );

            if ( component >= MAX_COMP )
                break;
            Double a = dateCorrelations.get( component );

            if ( a != null && !Double.isNaN( a ) ) {
                Double[] eigenGene = svdo.getvMatrix().getColObj( component );

                /*
                 * Plot eigengene vs values, add correlation to the plot
                 */
                TimeSeries series = new TimeSeries( "Dates vs. eigen" + ( component + 1 ) );
                int i = 0;
                for ( Date d : dates ) {
                    // if span is less than an hour, retain the minute.
                    if ( secspan < 60 * 60 ) {
                        series.addOrUpdate( new Minute( d ), eigenGene[i++] );
                    } else {
                        series.addOrUpdate( new Hour( d ), eigenGene[i++] );
                    }

                }
                TimeSeriesCollection dataset = new TimeSeriesCollection();
                dataset.addSeries( series );

                JFreeChart chart = ChartFactory
                        .createTimeSeriesChart( "Dates: " + xaxisLabel + " " + String.format( "r=%.2f", a ), null,
                                xaxisLabel, dataset, false, false, false );

                XYPlot xyPlot = chart.getXYPlot();

                chart.getTitle().setFont( new Font( "SansSerif", Font.BOLD, 12 ) );

                // standard renderer makes lines.
                XYDotRenderer renderer = new XYDotRenderer();
                renderer.setDefaultFillPaint( Color.white );
                renderer.setDotHeight( 3 );
                renderer.setDotWidth( 3 );
                renderer.setSeriesShape( 0, new Ellipse2D.Double( 0, 0, 3, 3 ) ); // has no effect, need dotheight.
                float saturationDrop = ( float ) Math.min( 1.0, component * 0.8f / MAX_COMP );
                renderer.setSeriesPaint( 0, Color.getHSBColor( 0.0f, 1.0f - saturationDrop, 0.7f ) );
                ValueAxis domainAxis = xyPlot.getDomainAxis();
                domainAxis.setVerticalTickLabels( true );
                xyPlot.setRenderer( renderer );
                xyPlot.setRangeGridlinesVisible( false );
                xyPlot.setDomainGridlinesVisible( false );
                charts.get( -1L ).add( chart );

            }
        }

        /*
         * Plot in a grid, with each factor as a column. FIXME What if we have too many factors to fit on the screen?
         */
        int columns = ( int ) Math.ceil( charts.size() );
        int perChartSize = ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX;
        BufferedImage image = new BufferedImage( columns * perChartSize, MAX_COMP * perChartSize,
                BufferedImage.TYPE_INT_ARGB );
        Graphics2D g2 = image.createGraphics();
        int currentX = 0;
        int currentY = 0;
        for ( Long id : charts.keySet() ) {
            for ( JFreeChart chart : charts.get( id ) ) {
                this.addChartToGraphics( chart, g2, currentX, currentY, perChartSize, perChartSize );
                if ( currentY + perChartSize < MAX_COMP * perChartSize ) {
                    currentY += perChartSize;
                } else {
                    currentY = 0;
                    currentX += perChartSize;
                }
            }
        }

        os.setContentType( MediaType.IMAGE_PNG_VALUE );
        ChartUtils.writeBufferedImageAsPNG( os.getOutputStream(), image );
    }

    /**
     * @param mvr MeanVarianceRelation object to plot
     * @param response  response output stream
     */
    private void writeMeanVariance( MeanVarianceRelation mvr, Double size, HttpServletResponse response ) throws Exception {
        // if number of datapoints > THRESHOLD then alpha = TRANSLUCENT, else alpha = OPAQUE
        final int THRESHOLD = 1000;
        final int TRANSLUCENT = 50;
        final int OPAQUE = 255;

        // Set maximum plot range to Y_MAX + YRANGE * OFFSET to leave some extra white space
        final double OFFSET_FACTOR = 0.05f;

        // set the final image size to be the minimum of MAX_IMAGE_SIZE_PX or size
        final int MAX_IMAGE_SIZE_PX = 5;

        if ( mvr == null ) {
            writePlaceholderImage( response );
            return;
        }

        // get data points
        XYSeriesCollection collection = this.getMeanVariance( mvr );

        if ( collection.getSeries().isEmpty() ) {
            writePlaceholderImage( response );
            return;
        }

        ChartFactory.setChartTheme( StandardChartTheme.createLegacyTheme() );
        JFreeChart chart = ChartFactory
                .createScatterPlot( "", "mean (log2)", "variance (log2)", collection, PlotOrientation.VERTICAL, false,
                        false, false );

        // adjust colors and shapes
        XYRegressionRenderer renderer = new XYRegressionRenderer();
        renderer.setDefaultPaint( Color.white );
        XYSeries series = collection.getSeries( 0 );
        int alpha = series.getItemCount() > THRESHOLD ? TRANSLUCENT : OPAQUE;
        renderer.setSeriesPaint( 0, new Color( 0, 0, 0, alpha ) );
        renderer.setSeriesPaint( 1, Color.red );
        renderer.setSeriesStroke( 1, new BasicStroke( 1 ) );
        renderer.setSeriesShape( 0, new Ellipse2D.Double( 4, 4, 4, 4 ) );
        renderer.setSeriesShapesFilled( 0, false );
        renderer.setSeriesLinesVisible( 0, false );
        renderer.setSeriesLinesVisible( 1, true );
        renderer.setSeriesShapesVisible( 1, false );

        XYPlot plot = chart.getXYPlot();
        plot.setRenderer( renderer );
        plot.setRangeGridlinesVisible( false );
        plot.setDomainGridlinesVisible( false );

        // adjust the chart domain and ranges
        double yRange = series.getMaxY() - series.getMinY();
        double xRange = series.getMaxX() - series.getMinX();
        if ( xRange < 0 ) {
            log.warn( "Min X was greater than Max X: Max=" + series.getMaxY() + " Min= " + series.getMinY() );
            writePlaceholderImage( response );
            return;
        }
        double ybuffer = ( yRange ) * OFFSET_FACTOR;
        double xbuffer = ( xRange ) * OFFSET_FACTOR;
        double newYMin = series.getMinY() - ybuffer;
        double newYMax = series.getMaxY() + ybuffer;
        double newXMin = series.getMinX() - xbuffer;
        double newXMax = series.getMaxX() + xbuffer;

        ValueAxis yAxis = new NumberAxis( "Variance" );
        yAxis.setRange( newYMin, newYMax );
        ValueAxis xAxis = new NumberAxis( "Mean" );
        xAxis.setRange( newXMin, newXMax );
        chart.getXYPlot().setRangeAxis( yAxis );
        chart.getXYPlot().setDomainAxis( xAxis );

        int finalSize = ( int ) Math.min( MAX_IMAGE_SIZE_PX * ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX,
                size * ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX );

        response.setContentType( MediaType.IMAGE_PNG_VALUE );
        ChartUtils.writeChartAsPNG( response.getOutputStream(), chart, finalSize, finalSize );
    }

    /**
     * Remove outliers from the MeanVarianceRelation by removing those points which have: (zscore(mean) > zscoreMax ||
     * zscore(variance) > zscoreMax)
     */
    @SuppressWarnings("unused")
    private MeanVarianceRelation removeMVOutliers( MeanVarianceRelation mvr, double zscoreMax ) {
        MeanVarianceRelation ret = MeanVarianceRelation.Factory.newInstance();
        ByteArrayConverter bac = new ByteArrayConverter();

        DoubleArrayList vars = new DoubleArrayList( bac.byteArrayToDoubles( mvr.getVariances() ) );
        DoubleArrayList means = new DoubleArrayList( bac.byteArrayToDoubles( mvr.getMeans() ) );

        DoubleArrayList filteredMeans = new DoubleArrayList();
        DoubleArrayList filteredVars = new DoubleArrayList();

        DoubleArrayList zVars = this.zscore( vars );
        DoubleArrayList zMeans = this.zscore( means );

        // clip outliers
        for ( int i = 0; i < zMeans.size(); i++ ) {

            if ( Math.abs( zMeans.getQuick( i ) ) > zscoreMax || Math.abs( zVars.getQuick( i ) ) > zscoreMax ) {
                continue;
            }

            filteredMeans.add( means.getQuick( i ) );
            filteredVars.add( vars.getQuick( i ) );
        }

        log.debug( filteredMeans.size() + " (out of " + means.size() + ") MV points had mean or variance zscore < "
                + zscoreMax + ". Max mean,variance is ( " + Descriptive.max( filteredMeans ) + "," + Descriptive
                .max( filteredVars )
                + ")." );

        ret.setVariances( bac.doubleArrayToBytes( filteredVars ) );
        ret.setMeans( bac.doubleArrayToBytes( filteredMeans ) );

        return ret;
    }

    /**
     * @return zscores
     */
    private DoubleArrayList zscore( DoubleArrayList d ) {
        DoubleArrayList z = new DoubleArrayList();
        double mean = Descriptive.mean( d );
        double sd = Descriptive.standardDeviation(
                Descriptive.variance( d.size(), Descriptive.sum( d ), Descriptive.sumOfSquares( d ) ) );
        for ( int i = 0; i < d.size(); i++ ) {
            z.add( Math.abs( d.getQuick( i ) - mean ) / sd );
        }
        assert z.size() == d.size();
        return z;
    }

    /**
     * Visualization of the correlation of principal components with factors or the date samples were run.
     *
     * @param svdo SVD value object
     */
    private void writePCAFactors( ExpressionExperiment ee, SVDValueObject svdo, HttpServletResponse response ) throws Exception {
        Map<Integer, Map<Long, Double>> factorCorrelations = svdo.getFactorCorrelations();
        // Map<Integer, Map<Long, Double>> factorPvalues = svdo.getFactorPvalues();
        Map<Integer, Double> dateCorrelations = svdo.getDateCorrelations();

        assert ee.getId().equals( svdo.getId() );

        if ( factorCorrelations.isEmpty() && dateCorrelations.isEmpty() ) {
            this.writePlaceholderImage( response );
            return;
        }
        ee = expressionExperimentService.thawLite( ee ); // need the experimental design
        int maxWidth = 10;

        Map<Long, String> efs = this.getFactorNames( ee, maxWidth );

        DefaultCategoryDataset series = new DefaultCategoryDataset();

        /*
         * With two groups, or a continuous factor, we get rank correlations
         */
        int MAX_COMP = 3;
        double STUB = 0.05; // always plot a little thing so we know its there.
        for ( Integer component : factorCorrelations.keySet() ) {
            if ( component >= MAX_COMP )
                break;
            for ( Long efId : factorCorrelations.get( component ).keySet() ) {
                Double a = factorCorrelations.get( component ).get( efId );
                String facname = efs.get( efId ) == null ? "?" : efs.get( efId );
                if ( a != null && !Double.isNaN( a ) ) {
                    Double corr = Math.max( STUB, Math.abs( a ) );
                    series.addValue( corr, "PC" + ( component + 1 ), facname );
                }
            }
        }

        for ( Integer component : dateCorrelations.keySet() ) {
            if ( component >= MAX_COMP )
                break;
            Double a = dateCorrelations.get( component );
            if ( a != null && !Double.isNaN( a ) ) {
                Double corr = Math.max( STUB, Math.abs( a ) );
                series.addValue( corr, "PC" + ( component + 1 ), "Date run" );
            }
        }
        ChartFactory.setChartTheme( StandardChartTheme.createLegacyTheme() );
        JFreeChart chart = ChartFactory
                .createBarChart( "", "Factors", "Component assoc.", series, PlotOrientation.VERTICAL, true, false,
                        false );

        chart.getCategoryPlot().getRangeAxis().setRange( 0, 1 );
        BarRenderer renderer = ( BarRenderer ) chart.getCategoryPlot().getRenderer();
        renderer.setDefaultPaint( Color.white );
        renderer.setShadowVisible( false );
        chart.getCategoryPlot().setRangeGridlinesVisible( false );
        chart.getCategoryPlot().setDomainGridlinesVisible( false );
        ChartUtils.applyCurrentTheme( chart );

        CategoryAxis domainAxis = chart.getCategoryPlot().getDomainAxis();
        domainAxis.setCategoryLabelPositions( CategoryLabelPositions.UP_45 );
        for ( int i = 0; i < MAX_COMP; i++ ) {
            /*
             * Hue is straightforward; brightness is set medium to make it muted; saturation we vary from high to low.
             */
            float saturationDrop = ( float ) Math.min( 1.0, i * 1.3f / MAX_COMP );
            renderer.setSeriesPaint( i, Color.getHSBColor( 0.0f, 1.0f - saturationDrop, 0.7f ) );

        }

        /*
         * Give figure more room .. up to a limit
         */
        int width = ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX;
        if ( chart.getCategoryPlot().getCategories().size() > 3 ) {
            width = width + 40 * ( chart.getCategoryPlot().getCategories().size() - 2 );
        }
        int MAX_QC_IMAGE_SIZE_PX = 500;
        width = Math.min( width, MAX_QC_IMAGE_SIZE_PX );
        response.setContentType( MediaType.IMAGE_PNG_VALUE );
        ChartUtils.writeChartAsPNG( response.getOutputStream(), chart, width, ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX );
    }

    private void writePCAScree( SVDValueObject svdo, HttpServletResponse response ) throws Exception {
        /*
         * Make a scree plot.
         */
        CategoryDataset series = this.getPCAScree( svdo );

        if ( series.getColumnCount() == 0 ) {
            return;
        }
        int MAX_COMPONENTS_FOR_SCREE = 10;
        ChartFactory.setChartTheme( StandardChartTheme.createLegacyTheme() );
        JFreeChart chart = ChartFactory
                .createBarChart( "", "Component (up to" + MAX_COMPONENTS_FOR_SCREE + ")", "Fraction of var.", series,
                        PlotOrientation.VERTICAL, false, false, false );

        BarRenderer renderer = ( BarRenderer ) chart.getCategoryPlot().getRenderer();
        renderer.setDefaultPaint( Color.white );
        renderer.setShadowVisible( false );
        chart.getCategoryPlot().setRangeGridlinesVisible( false );
        chart.getCategoryPlot().setDomainGridlinesVisible( false );
        response.setContentType( MediaType.IMAGE_PNG_VALUE );
        ChartUtils.writeChartAsPNG( response.getOutputStream(), chart, ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX,
                ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX );
    }

    /**
     * Write a blank image so user doesn't see the broken icon.
     */
    private void writePlaceholderImage( HttpServletResponse response ) throws IOException {
        int placeholderSize = ( int ) ( ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX * 0.75 );
        BufferedImage buffer = new BufferedImage( placeholderSize, placeholderSize, BufferedImage.TYPE_INT_RGB );
        Graphics g = buffer.createGraphics();
        g.setColor( Color.lightGray );
        g.fillRect( 0, 0, placeholderSize, placeholderSize );
        g.setColor( Color.black );
        g.drawString( "Not available", placeholderSize / 4, placeholderSize / 4 );
        response.setContentType( MediaType.IMAGE_PNG_VALUE );
        ImageIO.write( buffer, "png", response.getOutputStream() );
    }

    /**
     * Write a blank thumbnail image so user doesn't see the broken icon.
     */
    private void writePlaceholderThumbnailImage( HttpServletResponse response, int placeholderSize ) throws IOException {
        // Make the image a bit bigger to account for the empty space around the generated image.
        // If we can find a way to remove this empty space, we don't need to make the chart bigger.
        BufferedImage buffer = new BufferedImage( placeholderSize + 16, placeholderSize + 9,
                BufferedImage.TYPE_INT_RGB );
        Graphics g = buffer.createGraphics();
        g.setColor( Color.white );
        g.fillRect( 0, 0, placeholderSize + 16, placeholderSize + 9 );
        g.setColor( Color.gray );
        g.drawLine( 8, placeholderSize + 5, placeholderSize + 8, placeholderSize + 5 ); // x-axis
        g.drawLine( 8, 5, 8, placeholderSize + 5 ); // y-axis
        g.setColor( Color.black );
        Font font = g.getFont();
        g.setFont( new Font( font.getName(), font.getStyle(), 8 ) );
        g.drawString( "N/A", 9, placeholderSize );
        response.setContentType( MediaType.IMAGE_PNG_VALUE );
        ImageIO.write( buffer, "png", response.getOutputStream() );
    }

    private void writeProbeCorrHistImage( ExpressionExperiment ee, HttpServletResponse response ) throws IOException {
        XYSeries series = this.getCorrelHist( ee );

        if ( series == null || series.getItemCount() == 0 ) {
            writePlaceholderImage( response );
            return;
        }

        ChartFactory.setChartTheme( StandardChartTheme.createLegacyTheme() );
        XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
        xySeriesCollection.addSeries( series );
        JFreeChart chart = ChartFactory
                .createXYLineChart( "", "Correlation", "Frequency", xySeriesCollection, PlotOrientation.VERTICAL, false,
                        false, false );
        chart.getXYPlot().setRangeGridlinesVisible( false );
        chart.getXYPlot().setDomainGridlinesVisible( false );
        XYItemRenderer renderer = chart.getXYPlot().getRenderer();
        renderer.setDefaultPaint( Color.white );

        int size = ( int ) ( ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX * 0.8 );
        response.setContentType( MediaType.IMAGE_PNG_VALUE );
        ChartUtils.writeChartAsPNG( response.getOutputStream(), chart, size, size );
    }

    /**
     * Has to handle the situation where there might be more than one ResultSet.
     */
    private void writePValueHistImage( ExpressionExperiment ee, Long analysisId, Long rsId, String factorName, HttpServletResponse response ) throws IOException {

        XYSeries series = this.getDiffExPvalueHistXYSeries( ee, analysisId, rsId, factorName );

        if ( series == null ) {
            writePlaceholderImage( response );
            return;
        }

        XYSeriesCollection xySeriesCollection = new XYSeriesCollection( series );

        ChartFactory.setChartTheme( StandardChartTheme.createLegacyTheme() );
        JFreeChart chart = ChartFactory
                .createXYLineChart( "", "P-value", "Frequency", xySeriesCollection, PlotOrientation.VERTICAL, false,
                        false, false );
        chart.getXYPlot().setRangeGridlinesVisible( false );
        chart.getXYPlot().setDomainGridlinesVisible( false );
        XYItemRenderer renderer = chart.getXYPlot().getRenderer();
        renderer.setDefaultPaint( Color.white );

        response.setContentType( MediaType.IMAGE_PNG_VALUE );
        ChartUtils.writeChartAsPNG( response.getOutputStream(), chart,
                ( int ) ( ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX * 1.4 ),
                ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX );
    }

    /**
     * Write p-value histogram thumbnail image.
     */
    private void writePValueHistThumbnailImage( ExpressionExperiment ee, Long analysisId, Long rsId, String factorName, int size, HttpServletResponse response ) throws IOException {
        XYSeries series = this.getDiffExPvalueHistXYSeries( ee, analysisId, rsId, factorName );

        if ( series == null ) {
            writePlaceholderThumbnailImage( response, size );
            return;
        }

        series.add( -0.01, 0.0 );

        XYSeriesCollection xySeriesCollection = new XYSeriesCollection( series );

        ChartFactory.setChartTheme( StandardChartTheme.createLegacyTheme() );
        JFreeChart chart = ChartFactory
                .createXYLineChart( "", "", "", xySeriesCollection, PlotOrientation.VERTICAL, false, false, false );

        chart.getXYPlot().setBackgroundPaint( new Color( 230, 230, 230 ) );
        chart.getXYPlot().setRangeGridlinesVisible( false );
        chart.getXYPlot().setDomainGridlinesVisible( false );
        chart.getXYPlot().setOutlineVisible( false ); // around the plot
        chart.getXYPlot().getRangeAxis().setTickMarksVisible( false );
        chart.getXYPlot().getRangeAxis().setTickLabelsVisible( false );
        chart.getXYPlot().getRangeAxis().setAxisLineVisible( false );
        chart.getXYPlot().getDomainAxis().setTickMarksVisible( false );
        chart.getXYPlot().getDomainAxis().setTickLabelsVisible( false );
        chart.getXYPlot().getDomainAxis().setAxisLineVisible( false );
        chart.getXYPlot().getRenderer().setSeriesPaint( 0, Color.RED );
        // chart.getXYPlot().getRenderer().setSeriesStroke( 0, new BasicStroke( 1 ) );

        // Make the chart a bit bigger to account for the empty space around the generated image.
        // If we can find a way to remove this empty space, we don't need to make the chart bigger.
        response.setContentType( MediaType.IMAGE_PNG_VALUE );
        ChartUtils.writeChartAsPNG( response.getOutputStream(), chart, size + 16, size + 9 );
    }

    /**
     * Overrides XYLineAndShapeRenderer such that lines are drawn on top of points.
     */
    private static class XYRegressionRenderer extends XYLineAndShapeRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean isLinePass( int pass ) {
            return pass == 1;
        }

        @Override
        protected boolean isItemPass( int pass ) {
            return pass == 0;
        }
    }
}
