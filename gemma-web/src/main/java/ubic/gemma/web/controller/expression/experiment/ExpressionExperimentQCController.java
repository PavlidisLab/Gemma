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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
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
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.graphics.ColorMatrix;
import ubic.basecode.graphics.MatrixDisplay;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.io.writer.MatrixWriter;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.distribution.Histogram;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionFileUtils;
import ubic.gemma.analysis.preprocess.MeanVarianceService;
import ubic.gemma.analysis.preprocess.OutlierDetails;
import ubic.gemma.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.analysis.preprocess.SampleCoexpressionMatrixService;
import ubic.gemma.analysis.preprocess.svd.SVDService;
import ubic.gemma.analysis.preprocess.svd.SVDValueObject;
import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.PvalueDistribution;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.tasks.analysis.expression.ProcessedExpressionDataVectorCreateTask;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.web.controller.BaseController;
import ubic.gemma.web.view.TextView;
import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.doublealgo.Formatter;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

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
    ProcessedExpressionDataVectorCreateTask processedExpressionDataVectorCreateTask;
    @Autowired
    private MeanVarianceService meanVarianceService;

    /**
     * @param id
     * @param os
     * @throws Exception
     */
    @RequestMapping("/expressionExperiment/detailedFactorAnalysis.html")
    public void detailedFactorAnalysis( Long id, OutputStream os ) throws Exception {
        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            log.warn( "Could not load experiment with id " + id );
            return;
        }

        boolean ok = writeDetailedFactorAnalysis( ee, os );
        if ( !ok ) {
            writePlaceholderImage( os );
        }
    }

    /**
     * @param id
     * @param os
     * @return
     * @throws Exception
     */
    @RequestMapping("/expressionExperiment/pcaFactors.html")
    public ModelAndView pcaFactors( Long id, OutputStream os ) throws Exception {
        if ( id == null ) return null;

        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            log.warn( "Could not load experiment with id " + id ); // or access denied.
            writePlaceholderImage( os );
            return null;
        }

        SVDValueObject svdo = null;
        try {
            svdo = svdService.getSvdFactorAnalysis( ee.getId() );
        } catch ( Exception e ) {
            // if there is no pca
            // log.error( e, e );
        }

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
            writePlaceholderImage( os );
            return null;
        }

        SVDValueObject svdo = svdService.getSvd( ee.getId() );

        if ( svdo != null ) {
            this.writePCAScree( os, svdo );
        } else {
            writePlaceholderImage( os );
        }
        return null;
    }

    /**
     * @param id of experiment
     */
    @RequestMapping("/expressionExperiment/outliers.html")
    public ModelAndView identifyOutliers( Long id ) {

        if ( id == null ) {
            log.warn( "No id!" );
            return null;
        }

        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            log.warn( "Could not load experiment with id " + id );
            return null;
        }

        DoubleMatrix<BioAssay, BioAssay> sampleCorrelationMatrix = sampleCoexpressionMatrixService.findOrCreate( ee );

        Collection<OutlierDetails> outliers = outlierDetectionService.identifyOutliers( ee, sampleCorrelationMatrix,
                15, 0.9 );

        if ( !outliers.isEmpty() ) {
            for ( OutlierDetails details : outliers ) {
                // TODO
                details.getBioAssay();
            }
        }

        return null; // nothing to return;
    }

    @Autowired
    private SampleCoexpressionMatrixService sampleCoexpressionMatrixService;

    @Autowired
    private OutlierDetectionService outlierDetectionService;

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
            Boolean forceShowLabels, OutputStream os ) throws Exception {

        if ( id == null ) {
            log.warn( "No id!" );
            return null;
        }

        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            log.warn( "Could not load experiment with id " + id );
            return null;
        }

        DoubleMatrix<BioAssay, BioAssay> omatrix = sampleCoexpressionMatrixService.findOrCreate( ee );

        List<String> stringNames = new ArrayList<String>();
        for ( BioAssay ba : omatrix.getRowNames() ) {
            stringNames.add( ba.getName() + " ID=" + ba.getId() );
        }
        DoubleMatrix<String, String> matrix = new DenseDoubleMatrix<String, String>( omatrix.getRawMatrix() );
        matrix.setRowNames( stringNames );
        matrix.setColumnNames( stringNames );

        if ( text != null && text ) {
            StringWriter s = new StringWriter();
            MatrixWriter<String, String> mw = new MatrixWriter<String, String>( s, new DecimalFormat( "#.##" ) );
            mw.writeMatrix( matrix, true );
            ModelAndView mav = new ModelAndView( new TextView() );
            mav.addObject( TextView.TEXT_PARAM, s.toString() );
            return mav;
        }

        /*
         * Blank out the diagonal so it doesn't affect the colour scale.
         */
        for ( int i = 0; i < matrix.rows(); i++ ) {
            matrix.set( i, i, Double.NaN );
        }

        ColorMatrix<String, String> cm = new ColorMatrix<String, String>( matrix );

        cleanNames( matrix );

        int row = matrix.rows();
        int cellsize = ( int ) Math.min( MAX_HEATMAP_CELLSIZE, Math.max( 1, size * DEFAULT_QC_IMAGE_SIZE_PX / row ) );

        MatrixDisplay<String, String> writer = new MatrixDisplay<String, String>( cm );

        boolean reallyShowLabels;
        int minimumCellSizeForText = 9;
        if ( forceShowLabels != null && forceShowLabels ) {
            cellsize = Math.min( MAX_HEATMAP_CELLSIZE, minimumCellSizeForText );
            reallyShowLabels = true;
        } else {
            reallyShowLabels = showLabels == null ? false : showLabels && cellsize >= minimumCellSizeForText;
        }

        writer.setCellSize( new Dimension( cellsize, cellsize ) );
        boolean showScalebar = size > 2;
        writer.writeToPng( cm, os, reallyShowLabels, showScalebar );

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
     * @param id of the experiment
     * @param analysisId of the analysis
     * @param rsId resultSet Id
     * @param factorName deprecated, we will use rsId instead. Maintained for backwards compatibility.
     * @param size of the image.
     * @param os stream to write the image to.
     * @return null
     * @throws Exception
     */
    @RequestMapping("/expressionExperiment/visualizePvalueDist.html")
    public ModelAndView visualizePvalueDist( Long id, Long analysisId, Long rsid, String factorName, Integer size,
            OutputStream os ) throws Exception {
        ExpressionExperiment ee = this.expressionExperimentService.load( id );
        if ( ee == null ) {
            this.log.warn( "Could not load experiment with id " + id );
            return null;
        }

        if ( size == null ) {
            if ( !this.writePValueHistImage( os, ee, analysisId, rsid, factorName ) ) {
                writePlaceholderImage( os );
            }
        } else {
            if ( !this.writePValueHistThumbnailImage( os, ee, analysisId, rsid, factorName, size ) ) {
                writePlaceholderThumbnailImage( os, size );
            }
        }

        return null; // nothing to return;
    }

    /**
     * @param eeid
     * @param os
     * @throws IOException
     */
    @RequestMapping("/expressionExperiment/eigenGenes.html")
    public void writeEigenGenes( Long eeid, OutputStream os ) throws IOException {
        ExpressionExperiment ee = expressionExperimentService.load( eeid );
        if ( ee == null ) {
            throw new IllegalArgumentException( "Could not load experiment with id " + eeid ); // or access deined.
        }
        SVDValueObject svdo = svdService.getSvd( ee.getId() );

        DoubleMatrix<Long, Integer> vMatrix = svdo.getvMatrix();

        /*
         * FIXME put the biomaterial names in there instead of the IDs.
         */

        MatrixWriter<Long, Integer> mr = new MatrixWriter<Long, Integer>( os );

        mr.writeMatrix( vMatrix, true );

    }

    /**
     * @param chart
     * @param g2
     * @param x
     * @param y
     * @param width
     * @param height
     */
    private void addChartToGraphics( JFreeChart chart, Graphics2D g2, double x, double y, double width, double height ) {
        chart.draw( g2, new Rectangle2D.Double( x, y, width, height ), null, null );
    }

    /**
     * clean up the names of the correlation matrix rows and columns, so they are not to long and also contain the
     * bioassay id for reference purposes. Note that newly created coexpression matrices already give shorter names, so
     * this is partly for backwards compatibility with the old format.
     * 
     * @param matrix
     */
    private void cleanNames( DoubleMatrix<String, String> matrix ) {
        List<String> rawRowNames = matrix.getRowNames();
        List<String> rowNames = new ArrayList<String>();
        int i = 0;
        Pattern p = Pattern.compile( "^.*?ID=([0-9]+).*$", Pattern.CASE_INSENSITIVE );
        Pattern bipattern = Pattern.compile( "BioAssayImpl Id=[0-9]+ Name=", Pattern.CASE_INSENSITIVE );
        int MAX_BIO_ASSAY_NAME_LEN = 30;

        for ( String rn : rawRowNames ) {
            Matcher matcher = p.matcher( rn );
            Matcher bppat = bipattern.matcher( rn );
            String bioassayid = null;
            if ( matcher.matches() ) {
                bioassayid = matcher.group( 1 );
            }

            String cleanRn = StringUtils.abbreviate( bppat.replaceFirst( "" ), MAX_BIO_ASSAY_NAME_LEN );

            if ( bioassayid != null ) {
                if ( !cleanRn.contains( bioassayid ) )
                    rowNames.add( cleanRn + " ID=" + bioassayid ); // ensure the rows are unique
                else
                    rowNames.add( cleanRn + " " + ++i );
            } else {
                rowNames.add( cleanRn );
            }
        }
        matrix.setRowNames( rowNames );
        matrix.setColumnNames( rowNames );
    }

    /**
     * Support method for writeDetailedFactorAnalysis
     * 
     * @param efIdMap
     * @param efId
     * @param categories map of factor ID to text value. Strings will be unique, but possibly abbreviated and/or munged.
     */
    private void getCategories( Map<Long, ExperimentalFactor> efIdMap, Long efId, Map<Long, String> categories ) {
        ExperimentalFactor ef = efIdMap.get( efId );
        if ( ef == null ) return;
        int maxCategoryLabelLength = 10;

        for ( FactorValue fv : ef.getFactorValues() ) {
            String value = fv.getValue();
            if ( StringUtils.isBlank( value ) || value.equals( "null" ) ) {
                for ( Characteristic c : fv.getCharacteristics() ) {
                    if ( StringUtils.isNotBlank( c.getValue() ) ) {
                        if ( StringUtils.isNotBlank( value ) ) {
                            value = value + "; " + c.getValue();
                        } else {
                            value = c.getValue();
                        }
                    }
                }
            }

            if ( StringUtils.isBlank( value ) ) {
                value = fv.toString() + "--??";
            }

            if ( value.startsWith( ExperimentalDesignUtils.BATCH_FACTOR_NAME_PREFIX ) ) {
                value = value.replaceFirst( ExperimentalDesignUtils.BATCH_FACTOR_NAME_PREFIX, "" );
            } else {
                value = StringUtils.abbreviate( value, maxCategoryLabelLength );
            }

            while ( categories.values().contains( value ) ) {
                value = value + "+";// make unique, kludge, will end up with string of ++++
            }

            categories.put( fv.getId(), value );

        }
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

    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;

    /**
     * @param ee
     * @param analysisId
     * @param rsId
     * @param factorName
     * @return JFreeChart XYSeries representing the histogram for the requested result set
     * @throws FileNotFoundException
     * @throws IOException
     */
    private XYSeries getDiffExPvalueHistXYSeries( ExpressionExperiment ee, Long analysisId, Long rsId, String factorName )
            throws FileNotFoundException, IOException {
        if ( ee == null || analysisId == null || rsId == null ) {
            log.warn( "Got invalid values: " + ee + " " + analysisId + " " + rsId + " " + factorName );
            return null;
        }

        Histogram hist = differentialExpressionResultService.loadPvalueDistribution( rsId );

        XYSeries xySeries = null;

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

        /*
         * Rest of this is for backwards compatibility - read from the file.
         */
        File file = this.locatePvalueDistFile( ee, analysisId );

        // Current format is to have just one file for each analysis.
        if ( file == null ) {
            return null;
        }

        BufferedReader in = new BufferedReader( new FileReader( file ) );
        int factorNameIndex = -1;

        boolean readHeader = false;
        PvalueDistribution pvalueDist = PvalueDistribution.Factory.newInstance();
        DoubleArrayList counts = new DoubleArrayList();
        while ( in.ready() ) {
            String line = in.readLine().trim();
            if ( line.startsWith( "#" ) ) continue;
            String[] split = StringUtils.split( line, "\t" );
            if ( split.length < 2 ) continue;

            if ( !readHeader ) {
                for ( int i = 1; i < split.length; i++ ) {
                    String currFactorName = split[i];

                    // Note that currFactorName may have suffix such as __3
                    // (DifferentialExpressionAnalyzerService.FACTOR_NAME_MANGLING_DELIMITER followed by the ID).
                    if ( currFactorName.length() >= factorName.length()
                            && factorName.equals( currFactorName.substring( 0, factorName.length() ) ) ) {
                        factorNameIndex = i;
                    }
                }
                readHeader = true;
                if ( factorNameIndex < 0 ) {
                    // If factorName cannot be found, don't need to continue reading file.
                    break;
                }
                continue;
            }

            try {
                double x = Double.parseDouble( split[0] );
                double y = Double.parseDouble( split[factorNameIndex] );
                if ( xySeries == null ) {
                    xySeries = new XYSeries( factorName, true, true );
                }
                xySeries.add( x, y );

                /*
                 * Update the result set.
                 */
                counts.add( y );

            } catch ( NumberFormatException e ) {
                // line wasn't useable.. no big deal. Heading is included.
            }
        }

        if ( counts.size() > 0 ) {
            pvalueDistFileToPersistent( file, rsId, pvalueDist, counts );
        }

        return xySeries;
    }

    /**
     * For conversion from legacy system.
     * 
     * @param file
     * @param rsId
     * @param pvalueDist
     * @param counts
     */
    private void pvalueDistFileToPersistent( File file, Long rsId, PvalueDistribution pvalueDist, DoubleArrayList counts ) {
        log.info( "Converting from pvalue distribution file to persistent stored version" );
        ByteArrayConverter bac = new ByteArrayConverter();
        Double[] countArray = ( Double[] ) counts.toList().toArray( new Double[] {} );
        byte[] bytes = bac.doubleArrayToBytes( countArray );
        pvalueDist.setBinCounts( bytes );
        pvalueDist.setNumBins( countArray.length );
        ExpressionAnalysisResultSet resultSet = differentialExpressionResultService.loadAnalysisResultSet( rsId );
        resultSet.setPvalueDistribution( pvalueDist );
        differentialExpressionResultService.update( resultSet );
        if ( file.delete() ) {
            log.info( "Old file deleted" );
        } else {
            log.info( "Old file could not be deleted" );
        }
    }

    /**
     * Get the eigengene for the given component.
     * <p>
     * The values are rescaled so that jfreechart can cope. Small numbers give it fits.
     * 
     * @param svdo
     * @param component
     * @return
     */
    private Double[] getEigenGene( SVDValueObject svdo, Integer component ) {
        DoubleArrayList eigenGeneL = new DoubleArrayList( ArrayUtils.toPrimitive( svdo.getvMatrix().getColObj(
                component ) ) );
        DescriptiveWithMissing.standardize( eigenGeneL );
        Double[] eigenGene = ArrayUtils.toObject( eigenGeneL.elements() );
        return eigenGene;
    }

    /**
     * @param ee
     * @param maxWidth
     * @return
     */
    private Map<Long, String> getFactorNames( ExpressionExperiment ee, int maxWidth ) {
        Collection<ExperimentalFactor> factors = ee.getExperimentalDesign().getExperimentalFactors();

        Map<Long, String> efs = new HashMap<Long, String>();
        for ( ExperimentalFactor ef : factors ) {
            efs.put( ef.getId(), StringUtils.abbreviate( StringUtils.capitalize( ef.getName() ), maxWidth ) );
        }
        return efs;
    }

    /**
     * @param svdo
     * @return
     */
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
        double[] loessX = bac.byteArrayToDoubles( mvr.getLowessX() );
        double[] loessY = bac.byteArrayToDoubles( mvr.getLowessY() );
        if ( means == null || variances == null || loessX == null || loessY == null ) {
            return dataset;
        }

        XYSeries series = new XYSeries( "Mean-variance" );
        for ( int i = 0; i < means.length; i++ ) {
            series.add( means[i], variances[i] );
        }

        // loess fit trend line
        XYSeries loessSeries = new XYSeries( "Loess" );
        for ( int i = 0; i < loessX.length; i++ ) {
            loessSeries.add( loessX[i], loessY[i] );
        }

        dataset.addSeries( series );
        dataset.addSeries( loessSeries );

        return dataset;
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
     * @param analysisId
     * @return
     * @deprecated because we store this in the db now.
     */
    @Deprecated
    private File locatePvalueDistFile( ExpressionExperiment ee, Long analysisId ) {
        File file = null;

        if ( ee != null && analysisId != null ) {
            String shortName = ee.getShortName();
            file = new File( DifferentialExpressionFileUtils.getBaseDifferentialDirectory( shortName ), shortName
                    + ".an" + analysisId + ".pvalues" + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX );

            if ( !file.exists() ) {
                file = null;
            }
        }

        return file;
    }

    /**
     * @param ee
     * @param os
     * @return
     * @throws Exception
     */
    private boolean writeDetailedFactorAnalysis( ExpressionExperiment ee, OutputStream os ) throws Exception {
        SVDValueObject svdo = svdService.getSvdFactorAnalysis( ee.getId() );
        if ( svdo == null ) return false;

        if ( svdo.getFactors().isEmpty() && svdo.getDates().isEmpty() ) {
            return false;
        }
        Map<Integer, Map<Long, Double>> factorCorrelations = svdo.getFactorCorrelations();
        // Map<Integer, Map<Long, Double>> factorPvalues = svdo.getFactorPvalues();
        Map<Integer, Double> dateCorrelations = svdo.getDateCorrelations();

        assert ee.getId().equals( svdo.getId() );

        ee = expressionExperimentService.thawLite( ee ); // need the experimental design
        int maxWidth = 30;
        Map<Long, String> efs = getFactorNames( ee, maxWidth );
        Map<Long, ExperimentalFactor> efIdMap = EntityUtils.getIdMap( ee.getExperimentalDesign()
                .getExperimentalFactors() );
        Collection<Long> continuousFactors = new HashSet<Long>();
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

        Map<Long, List<JFreeChart>> charts = new LinkedHashMap<Long, List<JFreeChart>>();
        ChartFactory.setChartTheme( StandardChartTheme.createLegacyTheme() );
        /*
         * FACTORS
         */
        String componentShorthand = "PC";
        for ( Integer component : factorCorrelations.keySet() ) {

            if ( component >= MAX_COMP ) break;
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

                Map<Long, String> categories = new HashMap<Long, String>();

                if ( isCategorical ) {
                    getCategories( efIdMap, efId, categories );
                }

                if ( !charts.containsKey( efId ) ) {
                    charts.put( efId, new ArrayList<JFreeChart>() );
                }

                Double a = factorCorrelations.get( component ).get( efId );
                String plotname = ( efs.get( efId ) == null ? "?" : efs.get( efId ) ) + " " + xaxisLabel; // unique?

                if ( a != null && !Double.isNaN( a ) ) {
                    Double corr = a;
                    String title = plotname + " " + String.format( "%.2f", corr );
                    List<Double> values = svdo.getFactors().get( efId );
                    Double[] eigenGene = getEigenGene( svdo, component );
                    assert values.size() == eigenGene.length;

                    /*
                     * Plot eigengene vs values, add correlation to the plot
                     */
                    JFreeChart chart = null;
                    if ( isCategorical ) {

                        /*
                         * Categorical factor
                         */

                        // use the absolute value of the correlation, since direction is arbitrary.
                        title = plotname + " " + String.format( "r=%.2f", Math.abs( corr ) );

                        DefaultMultiValueCategoryDataset dataset = new DefaultMultiValueCategoryDataset();

                        /*
                         * What this code does is organize the factor values by the groups.
                         */
                        Map<String, List<Double>> groupedValues = new TreeMap<String, List<Double>>();
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
                                groupedValues.put( fvValue, new ArrayList<Double>() );
                            }

                            groupedValues.get( fvValue ).add( eigenGene[i] );

                            if ( log.isDebugEnabled() ) log.debug( fvValue + " " + values.get( i ) );
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
                        renderer.setBaseFillPaint( Color.white );
                        CategoryAxis domainAxis = plot.getDomainAxis();
                        domainAxis.setCategoryLabelPositions( CategoryLabelPositions.UP_45 );
                    } else {

                        /*
                         * Continous value factor
                         */

                        DefaultXYDataset series = new DefaultXYDataset();
                        series.addSeries( plotname,
                                new double[][] { ArrayUtils.toPrimitive( values.toArray( new Double[] {} ) ),
                                        ArrayUtils.toPrimitive( eigenGene ) } );

                        // don't show x-axis label, which would otherwise be efs.get( efId )
                        chart = ChartFactory.createScatterPlot( title, null, xaxisLabel, series,
                                PlotOrientation.VERTICAL, false, false, false );
                        XYPlot plot = chart.getXYPlot();
                        plot.setRangeGridlinesVisible( false );
                        plot.setDomainGridlinesVisible( false );

                        XYItemRenderer renderer = plot.getRenderer();
                        renderer.setBasePaint( Color.white );
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
        charts.put( -1L, new ArrayList<JFreeChart>() );
        for ( Integer component : dateCorrelations.keySet() ) {
            String xaxisLabel = componentShorthand + ( component + 1 );

            List<Date> dates = svdo.getDates();
            if ( dates.isEmpty() ) break;

            if ( component >= MAX_COMP ) break;
            Double a = dateCorrelations.get( component );

            if ( a != null && !Double.isNaN( a ) ) {
                Double corr = a;
                Double[] eigenGene = svdo.getvMatrix().getColObj( component );

                /*
                 * Plot eigengene vs values, add correlation to the plot
                 */
                TimeSeries series = new TimeSeries( "Dates vs. eigen" + ( component + 1 ) );
                int i = 0;
                for ( Date d : dates ) {
                    series.addOrUpdate( new Hour( d ), eigenGene[i++] );
                }
                TimeSeriesCollection dataset = new TimeSeriesCollection();
                dataset.addSeries( series );

                JFreeChart chart = ChartFactory.createTimeSeriesChart(
                        "Dates: " + xaxisLabel + " " + String.format( "r=%.2f", corr ), null, xaxisLabel, dataset,
                        false, false, false );

                XYPlot xyPlot = chart.getXYPlot();

                chart.getTitle().setFont( new Font( "SansSerif", Font.BOLD, 12 ) );

                // standard renderer makes lines.
                XYDotRenderer renderer = new XYDotRenderer();
                renderer.setBaseFillPaint( Color.white );
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
        int rows = MAX_COMP;
        int columns = ( int ) Math.ceil( charts.size() );
        int perChartSize = DEFAULT_QC_IMAGE_SIZE_PX;
        BufferedImage image = new BufferedImage( columns * perChartSize, rows * perChartSize,
                BufferedImage.TYPE_INT_ARGB );
        Graphics2D g2 = image.createGraphics();
        int currentX = 0;
        int currentY = 0;
        for ( Long id : charts.keySet() ) {
            for ( JFreeChart chart : charts.get( id ) ) {
                addChartToGraphics( chart, g2, currentX, currentY, perChartSize, perChartSize );
                if ( currentY + perChartSize < rows * perChartSize ) {
                    currentY += perChartSize;
                } else {
                    currentY = 0;
                    currentX += perChartSize;
                }
            }
        }

        os.write( ChartUtilities.encodeAsPNG( image ) );
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
        // Map<Integer, Map<Long, Double>> factorPvalues = svdo.getFactorPvalues();
        Map<Integer, Double> dateCorrelations = svdo.getDateCorrelations();

        assert ee.getId().equals( svdo.getId() );

        if ( factorCorrelations.isEmpty() && dateCorrelations.isEmpty() ) {
            writePlaceholderImage( os );
            return;
        }
        ee = expressionExperimentService.thawLite( ee ); // need the experimental design
        int maxWidth = 10;

        Map<Long, String> efs = getFactorNames( ee, maxWidth );

        DefaultCategoryDataset series = new DefaultCategoryDataset();

        /*
         * With two groups, or a continuous factor, we get rank correlations
         */
        int MAX_COMP = 3;
        double STUB = 0.05; // always plot a little thing so we know its there.
        for ( Integer component : factorCorrelations.keySet() ) {
            if ( component >= MAX_COMP ) break;
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
            if ( component >= MAX_COMP ) break;
            Double a = dateCorrelations.get( component );
            if ( a != null && !Double.isNaN( a ) ) {
                Double corr = Math.max( STUB, Math.abs( a ) );
                series.addValue( corr, "PC" + ( component + 1 ), "Date run" );
            }
        }
        ChartFactory.setChartTheme( StandardChartTheme.createLegacyTheme() );
        JFreeChart chart = ChartFactory.createBarChart( "", "Factors", "Component assoc.", series,
                PlotOrientation.VERTICAL, true, false, false );

        chart.getCategoryPlot().getRangeAxis().setRange( 0, 1 );
        BarRenderer renderer = ( BarRenderer ) chart.getCategoryPlot().getRenderer();
        renderer.setBasePaint( Color.white );
        renderer.setShadowVisible( false );
        chart.getCategoryPlot().setRangeGridlinesVisible( false );
        chart.getCategoryPlot().setDomainGridlinesVisible( false );
        ChartUtilities.applyCurrentTheme( chart );

        CategoryAxis domainAxis = chart.getCategoryPlot().getDomainAxis();
        domainAxis.setCategoryLabelPositions( CategoryLabelPositions.UP_45 );
        for ( int i = 0; i < MAX_COMP; i++ ) {
            /*
             * Hue is straightforward; brightness is set medium to make it muted; saturation we vary from high to low.
             */
            float saturationDrop = ( float ) Math.min( 1.0, i * 0.8f / MAX_COMP );
            renderer.setSeriesPaint( i, Color.getHSBColor( 0.0f, 1.0f - saturationDrop, 0.7f ) );

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
        ChartFactory.setChartTheme( StandardChartTheme.createLegacyTheme() );
        JFreeChart chart = ChartFactory.createBarChart( "", "Component (up to" + MAX_COMPONENTS_FOR_SCREE + ")",
                "Fraction of var.", series, PlotOrientation.VERTICAL, false, false, false );

        BarRenderer renderer = ( BarRenderer ) chart.getCategoryPlot().getRenderer();
        renderer.setBasePaint( Color.white );
        renderer.setShadowVisible( false );
        chart.getCategoryPlot().setRangeGridlinesVisible( false );
        chart.getCategoryPlot().setDomainGridlinesVisible( false );
        ChartUtilities.writeChartAsPNG( os, chart, DEFAULT_QC_IMAGE_SIZE_PX, DEFAULT_QC_IMAGE_SIZE_PX );
        return true;
    }

    /**
     * Write a blank image so user doesn't see the broken icon.
     * 
     * @param os
     * @throws IOException
     */
    private void writePlaceholderImage( OutputStream os ) throws IOException {
        int placeholderSize = ( int ) ( DEFAULT_QC_IMAGE_SIZE_PX * 0.75 );
        BufferedImage buffer = new BufferedImage( placeholderSize, placeholderSize, BufferedImage.TYPE_INT_RGB );
        Graphics g = buffer.createGraphics();
        g.setColor( Color.lightGray );
        g.fillRect( 0, 0, placeholderSize, placeholderSize );
        g.setColor( Color.black );
        g.drawString( "Not available", placeholderSize / 4, placeholderSize / 4 );
        ImageIO.write( buffer, "png", os );
    }

    /**
     * Write a blank thumbnail image so user doesn't see the broken icon.
     * 
     * @param os
     * @param placeholderSize
     * @throws IOException
     */
    private void writePlaceholderThumbnailImage( OutputStream os, int placeholderSize ) throws IOException {
        // Make the image a bit bigger to account for the empty space around the generated image.
        // If we can find a way to remove this empty space, we don't need to make the chart bigger.
        BufferedImage buffer = new BufferedImage( placeholderSize + 16, placeholderSize + 9, BufferedImage.TYPE_INT_RGB );
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
        ImageIO.write( buffer, "png", os );
    }

    /**
     * @param id of experiment
     * @param size Multiplier on the cell size. 1 or null for standard small size.
     * @param text if true, output a tabbed file instead of a png
     * @param os response output stream
     * @return ModelAndView object if text is true, otherwise null
     * @throws Exception
     */
    @RequestMapping("/expressionExperiment/visualizeMeanVariance.html")
    public ModelAndView visualizeMeanVariance( Long id, Double size, Boolean text, OutputStream os ) throws Exception {

        if ( id == null ) {
            log.warn( "No id!" );
            return null;
        }

        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            log.warn( "Could not load experiment with id " + id );
            return null;
        }

        MeanVarianceRelation mvr = meanVarianceService.find( ee );

        if ( mvr == null ) {
            return null;
        }

        if ( text != null && text && mvr != null ) {
            final ByteArrayConverter bac = new ByteArrayConverter();

            double[] means = bac.byteArrayToDoubles( mvr.getMeans() );
            double[] variances = bac.byteArrayToDoubles( mvr.getVariances() );

            DoubleMatrix2D matrix = new DenseDoubleMatrix2D( means.length, 2 );
            matrix.viewColumn( 0 ).assign( means );
            matrix.viewColumn( 1 ).assign( variances );

            String matrixString = new Formatter( "%1.2G" ).toTitleString( matrix, null, new String[] { "mean",
                    "variance" }, null, null, null, null );
            ModelAndView mav = new ModelAndView( new TextView() );
            mav.addObject( TextView.TEXT_PARAM, matrixString );

            return mav;
        }

        writeMeanVariance( os, mvr, size );

        return null;
    }

    /**
     * Overrides XYLineAndShapeRenderer such that lines are drawn on top of points.
     */
    private class XYRegressionRenderer extends XYLineAndShapeRenderer {
        private static final long serialVersionUID = 1L;

        protected boolean isLinePass( int pass ) {
            return pass == 1;
        }

        protected boolean isItemPass( int pass ) {
            return pass == 0;
        }
    }

    /**
     * @param os response output stream
     * @param mvr MeanVarianceRelation object to plot
     * @param size
     * @return true if mvr data points were plotted
     */
    private boolean writeMeanVariance( OutputStream os, MeanVarianceRelation mvr, Double size ) throws Exception {
        // if number of datapoints > THRESHOLD then alpha = TRANSLUCENT, else alpha = OPAQUE
        final int THRESHOLD = 1000;
        final int TRANSLUCENT = 50;
        final int OPAQUE = 255;

        // Set maximum plot range to Y_MAX - YSCALE * OFFSET to cut down on excess white space
        final double OFFSET_FACTOR = 0.05f;
        final double Y_SCALE = 10f;

        // set the final image size to be the minimum of MAX_IMAGE_SIZE_PX or size
        final int MAX_IMAGE_SIZE_PX = 5;

        if ( mvr == null ) {
            return false;
        }

        // get data points
        XYSeriesCollection collection = getMeanVariance( mvr );

        if ( collection.getSeries().size() == 0 ) {
            return false;
        }

        ChartFactory.setChartTheme( StandardChartTheme.createLegacyTheme() );
        JFreeChart chart = ChartFactory.createScatterPlot( "", "Mean", "Variance", collection,
                PlotOrientation.VERTICAL, false, false, false );

        // adjust colors and shapes
        XYRegressionRenderer renderer = new XYRegressionRenderer();
        renderer.setBasePaint( Color.white );
        int alpha = collection.getSeries( 0 ).getItemCount() > THRESHOLD ? TRANSLUCENT : OPAQUE;
        renderer.setSeriesPaint( 0, new Color( 0, 0, 0, alpha ) );
        renderer.setSeriesPaint( 1, Color.red );
        renderer.setSeriesStroke( 1, new BasicStroke( 4 ) );
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
        double yRange = collection.getSeries( 0 ).getMaxY() - collection.getSeries( 0 ).getMinY();
        double offset = ( yRange ) * OFFSET_FACTOR;
        double newYMin = collection.getSeries( 0 ).getMinY() - offset;
        double newYMax = collection.getSeries( 0 ).getMaxY() - Y_SCALE * offset; // cut off excess space at the top
        double newXMin = collection.getSeries( 0 ).getMinX() - offset;
        double newXMax = collection.getSeries( 0 ).getMaxX() + offset;
        ValueAxis yAxis = new NumberAxis( "Variance" );
        yAxis.setRange( newYMin, newYMax );
        ValueAxis xAxis = new NumberAxis( "Mean" );
        xAxis.setRange( newXMin, newXMax );
        chart.getXYPlot().setRangeAxis( yAxis );
        chart.getXYPlot().setDomainAxis( xAxis );

        int finalSize = ( int ) Math
                .min( MAX_IMAGE_SIZE_PX * DEFAULT_QC_IMAGE_SIZE_PX, size * DEFAULT_QC_IMAGE_SIZE_PX );
        ChartUtilities.writeChartAsPNG( os, chart, finalSize, finalSize );

        return true;
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
        ChartFactory.setChartTheme( StandardChartTheme.createLegacyTheme() );
        XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
        xySeriesCollection.addSeries( series );
        JFreeChart chart = ChartFactory.createXYLineChart( "", "Correlation", "Frequency", xySeriesCollection,
                PlotOrientation.VERTICAL, false, false, false );
        chart.getXYPlot().setRangeGridlinesVisible( false );
        chart.getXYPlot().setDomainGridlinesVisible( false );
        XYItemRenderer renderer = chart.getXYPlot().getRenderer();
        renderer.setBasePaint( Color.white );

        int size = ( int ) ( DEFAULT_QC_IMAGE_SIZE_PX * 0.8 );
        ChartUtilities.writeChartAsPNG( os, chart, size, size );

        return true;
    }

    /**
     * Has to handle the situation where there might be more than one ResultSet.
     * 
     * @param os
     * @param ee
     * @param analysisId
     * @param rsId
     * @throws IOException
     */
    private boolean writePValueHistImage( OutputStream os, ExpressionExperiment ee, Long analysisId, Long rsId,
            String factorName ) throws IOException {

        XYSeries series = getDiffExPvalueHistXYSeries( ee, analysisId, rsId, factorName );

        if ( series == null ) {
            return false;
        }

        XYSeriesCollection xySeriesCollection = new XYSeriesCollection( series );

        ChartFactory.setChartTheme( StandardChartTheme.createLegacyTheme() );
        JFreeChart chart = ChartFactory.createXYLineChart( "", "P-value", "Frequency", xySeriesCollection,
                PlotOrientation.VERTICAL, false, false, false );
        chart.getXYPlot().setRangeGridlinesVisible( false );
        chart.getXYPlot().setDomainGridlinesVisible( false );
        XYItemRenderer renderer = chart.getXYPlot().getRenderer();
        renderer.setBasePaint( Color.white );

        ChartUtilities
                .writeChartAsPNG( os, chart, ( int ) ( DEFAULT_QC_IMAGE_SIZE_PX * 1.4 ), DEFAULT_QC_IMAGE_SIZE_PX );
        return true;
    }

    /**
     * Write p-value histogram thumbnail image.
     * 
     * @param os
     * @param ee
     * @param analysisId
     * @param rsId
     * @param size
     * @throws IOException
     */
    private boolean writePValueHistThumbnailImage( OutputStream os, ExpressionExperiment ee, Long analysisId,
            Long rsId, String factorName, int size ) throws IOException {
        XYSeries series = getDiffExPvalueHistXYSeries( ee, analysisId, rsId, factorName );

        if ( series == null ) {
            return false;
        }

        series.add( -0.01, 0.0 );

        XYSeriesCollection xySeriesCollection = new XYSeriesCollection( series );

        ChartFactory.setChartTheme( StandardChartTheme.createLegacyTheme() );
        JFreeChart chart = ChartFactory.createXYLineChart( "", "", "", xySeriesCollection, PlotOrientation.VERTICAL,
                false, false, false );

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
        ChartUtilities.writeChartAsPNG( os, chart, size + 16, size + 9 );

        return true;
    }
}
