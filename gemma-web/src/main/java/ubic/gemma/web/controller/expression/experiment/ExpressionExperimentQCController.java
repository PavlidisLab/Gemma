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
import cern.jet.stat.Descriptive;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartTheme;
import org.jfree.chart.JFreeChart;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.graphics.ColorMatrix;
import ubic.basecode.graphics.MatrixDisplay;
import ubic.basecode.io.writer.MatrixWriter;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.distribution.Histogram;
import ubic.gemma.core.analysis.preprocess.OutlierDetails;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchInfoPopulationHelperServiceImpl;
import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionException;
import ubic.gemma.core.analysis.preprocess.convert.ScaleTypeConversionUtils;
import ubic.gemma.core.analysis.preprocess.svd.SVDResult;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.analysis.service.ExpressionDataFileUtils;
import ubic.gemma.core.datastructure.matrix.io.ExperimentalDesignWriter;
import ubic.gemma.core.datastructure.matrix.io.MeanVarianceWriter;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.visualization.ExpressionDataHeatmap;
import ubic.gemma.core.visualization.SingleCellDataBoxplot;
import ubic.gemma.core.visualization.SingleCellSparsityHeatmap;
import ubic.gemma.corej11.visualization.ChartUtils;
import ubic.gemma.model.analysis.expression.coexpression.CoexpCorrelationDistribution;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeUtils;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.analysis.expression.coexpression.CoexpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSubSetService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;
import ubic.gemma.persistence.util.IdentifiableUtils;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.web.controller.util.EntityNotFoundException;
import ubic.gemma.web.controller.util.MessageUtil;
import ubic.gemma.web.controller.util.view.TextView;
import ubic.gemma.web.util.WebEntityUrlBuilder;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static ubic.gemma.core.analysis.service.ExpressionDataFileUtils.*;
import static ubic.gemma.core.datastructure.matrix.io.ExpressionDataWriterUtils.appendBaseHeader;

/**
 * @author paul
 */
@Controller
@CommonsLog
public class ExpressionExperimentQCController {

    /**
     * Default size for an image, in pixel.
     */
    public static final int DEFAULT_QC_IMAGE_SIZE_PX = 400;

    /**
     * Maximum size for an image, in pixels.
     */
    public static final int MAX_QC_IMAGE_SIZE_PX = 800;

    /**
     * Maximum size of a thumbnail, in pixels.
     */
    private static final int MAX_QC_IMAGE_THUMBNAIL_SIZE_PX = 128;

    private static final double
            MIN_SIZE_FACTOR = 0.5,
            MAX_SIZE_FACTOR = 2.0;

    /**
     * DPI for rendering images.
     */
    private static final float DEFAULT_DPI = 300;

    @Autowired
    protected MessageSource messageSource;
    @Autowired
    protected MessageUtil messageUtil;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private SVDService svdService;
    @Autowired
    private SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService;
    @Autowired
    private OutlierDetectionService outlierDetectionService;
    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    @Autowired
    private ExpressionAnalysisResultSetService expressionAnalysisResultSetService;
    @Autowired
    private CoexpressionAnalysisService coexpressionAnalysisService;
    @Autowired
    private WebEntityUrlBuilder entityUrlBuilder;
    @Autowired
    private BuildInfo buildInfo;
    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;
    @Autowired
    private ExpressionExperimentSubSetService expressionExperimentSubSetService;
    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;
    @Autowired
    private CompositeSequenceService compositeSequenceService;
    @Autowired
    private QuantitationTypeService quantitationTypeService;
    @Autowired
    private ChartTheme chartTheme;

    @Value("${gemma.analysis.dir}")
    private Path analysisStoragePath;

    @RequestMapping(value = "/expressionExperiment/detailedFactorAnalysis.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public void detailedFactorAnalysis(
            @RequestParam("id") Long id,
            @RequestParam(value = "size", defaultValue = "1.0") double sizeFactor,
            HttpServletResponse response ) throws Exception {
        Assert.isTrue( sizeFactor >= MIN_SIZE_FACTOR && sizeFactor <= MAX_SIZE_FACTOR,
                "The size factor factor must be between " + MIN_SIZE_FACTOR + " and " + MAX_SIZE_FACTOR + "." );
        ExpressionExperiment ee = expressionExperimentService.loadAndThawLiteOrFail( id, EntityNotFoundException::new );
        // compute the size of each chart
        // this plot is fairly big, so the actual max is going to be 3 * numFactors * MAX_QC_IMAGE_SIZE_PX
        int perChartSize = ( int ) ( sizeFactor * DEFAULT_QC_IMAGE_SIZE_PX );
        this.writeDetailedFactorAnalysis( ee, perChartSize, response );
    }

    @RequestMapping(value = "/expressionExperiment/outliersRemoved.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView identifyOutliersRemoved( @RequestParam("id") Long id ) throws IOException {
        ExpressionExperiment ee = expressionExperimentService.loadAndThawLiteOrFail( id, EntityNotFoundException::new );

        Collection<BioAssay> bioAssays = new HashSet<>();
        for ( BioAssay assay : ee.getBioAssays() ) {
            if ( assay.getIsOutlier() ) {
                bioAssays.add( assay );
            }
        }

        // and write it out
        StringWriter writer = new StringWriter();
        appendBaseHeader( ee, "Outliers removed", entityUrlBuilder.fromHostUrl().entity( ee ).toUriString(), buildInfo, new Date(), writer );
        new ExperimentalDesignWriter( entityUrlBuilder, buildInfo, false )
                .write( ee, bioAssays, false, writer );

        TextView tv = new TextView( "tab-separated-values" );
        tv.setContentDisposition( "attachment; filename=\"" + FilenameUtils.removeExtension( getDesignFileName( ee ) ) + "\"" );
        return new ModelAndView( tv ).addObject( TextView.TEXT_PARAM, writer.toString() );
    }

    @RequestMapping(value = "/expressionExperiment/possibleOutliers.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView identifyPossibleOutliers( @RequestParam("id") Long id ) throws IOException {
        ExpressionExperiment ee = expressionExperimentService.loadAndThawLiteOrFail( id, EntityNotFoundException::new );

        // identify outliers
        if ( !sampleCoexpressionAnalysisService.hasAnalysis( ee ) ) {
            log.warn( "Experiment doesn't have correlation matrix computed (will not create right now)" );
            return null;
        }

        DoubleMatrix<BioAssay, BioAssay> sampleCorrelationMatrix = sampleCoexpressionAnalysisService
                .loadRegressedMatrix( ee );
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
        appendBaseHeader( ee, "Sample outlier", entityUrlBuilder.fromHostUrl().entity( ee ).toUriString(), buildInfo, new Date(), writer );
        new ExperimentalDesignWriter( entityUrlBuilder, buildInfo, false )
                .write( ee, bioAssays, false, writer );

        TextView tv = new TextView( "tab-separated-values" );
        tv.setContentDisposition( "attachment; filename=\"" + FilenameUtils.removeExtension( getDesignFileName( ee ) ) + "\"" );
        return new ModelAndView( tv )
                .addObject( TextView.TEXT_PARAM, writer.toString() );
    }

    @RequestMapping(value = "/expressionExperiment/pcaFactors.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public void pcaFactors( @RequestParam("id") Long id, @RequestParam(value = "size", defaultValue = "1.0") double sizeFactor, HttpServletResponse response ) throws Exception {
        Assert.isTrue( sizeFactor >= MIN_SIZE_FACTOR && sizeFactor <= MAX_SIZE_FACTOR,
                "The size factor factor must be between " + MIN_SIZE_FACTOR + " and " + MAX_SIZE_FACTOR + "." );
        ExpressionExperiment ee = expressionExperimentService.loadAndThawLiteOrFail( id, EntityNotFoundException::new );

        SVDResult svdo = null;
        try {
            svdo = svdService.getSvdFactorAnalysis( ee );
        } catch ( Exception e ) {
            // if there is no pca
            log.error( e, e );
        }

        if ( svdo != null ) {
            this.writePCAFactors( ee, svdo, sizeFactor, response );
        } else {
            int size = ( int ) ( sizeFactor * DEFAULT_QC_IMAGE_SIZE_PX );
            this.writePlaceholderImage( response, size );
        }
    }

    @RequestMapping(value = "/expressionExperiment/pcaScree.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public void pcaScree(
            @RequestParam("id") Long id,
            @RequestParam(value = "size", defaultValue = "1.0") double sizeFactor,
            HttpServletResponse response ) throws Exception {
        Assert.isTrue( sizeFactor >= MIN_SIZE_FACTOR && sizeFactor <= MAX_SIZE_FACTOR,
                "The size factor factor must be between " + MIN_SIZE_FACTOR + " and " + MAX_SIZE_FACTOR + "." );
        ExpressionExperiment ee = expressionExperimentService.loadOrFail( id, EntityNotFoundException::new );
        SVDResult svdo = svdService.getSvd( ee );
        int size = ( int ) ( sizeFactor * DEFAULT_QC_IMAGE_SIZE_PX );
        if ( svdo != null ) {
            this.writePCAScree( ee, svdo, size, response );
        } else {
            this.writePlaceholderImage( response, size );
        }
    }

    /**
     * @param id of experiment
     * @param sizeFactor Multiplier on the cell size. 1 or null for standard small size.
     * @param text if true, output a tabbed file instead of a png
     * @param showLabels if the row and column labels of the matrix should be shown.
     * @param forceShowLabels forces the display of labels in the picture
     * @param reg uses the regressed matrix (if available).
     */
    @RequestMapping(value = "/expressionExperiment/visualizeCorrMat.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public void visualizeCorrMat(
            @RequestParam("id") Long id,
            @RequestParam(value = "size", defaultValue = "1.0") double sizeFactor,
            @RequestParam(value = "text", defaultValue = "false") boolean text,
            @RequestParam(value = "showLabels", defaultValue = "false") boolean showLabels,
            @RequestParam(value = "forceShowLabels", defaultValue = "false") boolean forceShowLabels,
            @RequestParam(value = "reg", defaultValue = "false") boolean reg,
            HttpServletResponse response ) throws Exception {
        Assert.isTrue( sizeFactor >= MIN_SIZE_FACTOR && sizeFactor <= MAX_SIZE_FACTOR,
                "The size factor factor must be between " + MIN_SIZE_FACTOR + " and " + MAX_SIZE_FACTOR + "." );
        ExpressionExperiment ee = expressionExperimentService.loadAndThawLiterOrFail( id, EntityNotFoundException::new );
        DoubleMatrix<BioAssay, BioAssay> omatrix = reg ? sampleCoexpressionAnalysisService.loadBestMatrix( ee )
                : sampleCoexpressionAnalysisService.loadFullMatrix( ee );
        if ( omatrix == null ) {
            throw new EntityNotFoundException( "No correlation matrix for ee " + id );
        }

        List<String> stringNames = new ArrayList<>();
        for ( BioAssay ba : omatrix.getRowNames() ) {
            stringNames.add( ba.getName() + " ID=" + ba.getId() );
        }
        DoubleMatrix<String, String> matrix = new DenseDoubleMatrix<>( omatrix.getRawMatrix() );
        matrix.setRowNames( stringNames );
        matrix.setColumnNames( stringNames );

        if ( text ) {
            StringWriter s = new StringWriter();
            MatrixWriter<String, String> mw = new MatrixWriter<>( s, new DecimalFormat( "#.##" ) );
            mw.writeMatrix( matrix, true );
            // This does not solve the root issue, but I wasted too much time on it
            response.setContentType( "text/tab-separated-values" );
            response.setHeader( "Content-Disposition", "attachment; filename=\"" + ExpressionDataFileUtils.getCoexpressionDataFilename( ee ) + "\"" );
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

        boolean showScalebar = sizeFactor >= 2;
        // account for the labels if we intend to show them
        // some of those numbers are in MatrixDisplay
        int labelSize = ( showLabels || forceShowLabels ) ? 80 : 0;
        int scalebarSize = showScalebar ? 40 : 0;
        int cellsize = ( int ) ( sizeFactor * ( DEFAULT_QC_IMAGE_SIZE_PX - labelSize - scalebarSize ) / row );
        // at least 1 pixel per cell
        cellsize = Math.max( cellsize, 1 );

        MatrixDisplay<String, String> writer = new MatrixDisplay<>( cm );

        boolean reallyShowLabels;
        int minimumCellSizeForText = 9;
        if ( forceShowLabels ) {
            // make sure that the labels are readable
            cellsize = Math.max( cellsize, minimumCellSizeForText );
            reallyShowLabels = true;
        } else if ( showLabels && cellsize >= minimumCellSizeForText ) {
            reallyShowLabels = true;
        } else if ( showLabels ) {
            log.warn( "Asking for labels, but the cell size (" + cellsize + ") is too small to display them." );
            reallyShowLabels = false;
        } else {
            reallyShowLabels = false;
        }

        writer.setCellSize( new Dimension( cellsize, cellsize ) );
        response.setContentType( MediaType.IMAGE_PNG_VALUE );
        writer.writeToPng( cm, response.getOutputStream(), reallyShowLabels, showScalebar );
    }

    /**
     * @param id of experiment
     * @param sizeFactor Multiplier on the cell size. 1 or null for standard small size.
     * @param text if true, output a tabbed file instead of a png
     */
    @RequestMapping(value = "/expressionExperiment/visualizeMeanVariance.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public void visualizeMeanVariance(
            @RequestParam("id") Long id,
            @RequestParam(value = "size", defaultValue = "1.0") double sizeFactor,
            @RequestParam(value = "text", defaultValue = "false") boolean text,
            HttpServletResponse response ) throws Exception {
        Assert.isTrue( sizeFactor >= MIN_SIZE_FACTOR && sizeFactor <= MAX_SIZE_FACTOR,
                "The size factor factor must be between " + MIN_SIZE_FACTOR + " and " + MAX_SIZE_FACTOR + "." );
        ExpressionExperiment ee = expressionExperimentService.loadWithMeanVarianceRelation( id );
        if ( ee == null ) {
            throw new EntityNotFoundException( "Could not load experiment with id " + id );
        }

        QuantitationType quantitationType = expressionExperimentService.getProcessedQuantitationType( ee )
                .orElseThrow( () -> new EntityNotFoundException( ee.getShortName() + " does not have processed data vectors." ) );

        MeanVarianceRelation mvr = ee.getMeanVarianceRelation();

        if ( mvr == null ) {
            log.warn( "EE " + id + " does not have a mean-variance relation." );
            return;
        }

        if ( text ) {
            MeanVarianceWriter writer = new MeanVarianceWriter( buildInfo, entityUrlBuilder );
            response.setContentType( "text/tab-separated-values" );
            response.setHeader( "Content-Disposition", "attachment; filename=\"" + FilenameUtils.removeExtension( getMeanVarianceRelationFilename( ee ) ) + "\"" );
            writer.write( ee, quantitationType, response.getWriter() );
        } else {
            int size = ( int ) ( sizeFactor * DEFAULT_QC_IMAGE_SIZE_PX );
            // FIXME might be something better to do
            response.setContentType( MediaType.IMAGE_PNG_VALUE );
            writeMeanVariance( ee, quantitationType, mvr, size, response );
        }
    }

    @RequestMapping(value = "/expressionExperiment/visualizeProbeCorrDist.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public void visualizeProbeCorrDist(
            @RequestParam("id") Long id,
            @RequestParam(value = "size", defaultValue = "1.0") double sizeFactor,
            HttpServletResponse response ) throws Exception {
        Assert.isTrue( sizeFactor >= MIN_SIZE_FACTOR && sizeFactor <= MAX_SIZE_FACTOR,
                "The size factor factor must be between " + MIN_SIZE_FACTOR + " and " + MAX_SIZE_FACTOR + "." );
        ExpressionExperiment ee = expressionExperimentService.loadOrFail( id, EntityNotFoundException::new );
        int size = ( int ) ( sizeFactor * DEFAULT_QC_IMAGE_SIZE_PX );
        writeProbeCorrHistImage( ee, size, response );
    }

    /**
     * @param id of the experiment
     * @param analysisId of the analysis
     * @param rsid resultSet Id
     * @param size of the image.
     * @param response stream to write the image to.
     */
    @RequestMapping(value = "/expressionExperiment/visualizePvalueDist.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public void visualizePvalueDist(
            @RequestParam("id") Long id,
            @RequestParam("analysisId") Long analysisId,
            @RequestParam("rsid") Long rsid,
            @RequestParam(value = "size", required = false) @Nullable Integer size,
            @RequestParam(value = "sizeFactor", defaultValue = "1.0") double sizeFactor,
            HttpServletResponse response ) throws Exception {
        Assert.isTrue( sizeFactor >= MIN_SIZE_FACTOR && sizeFactor <= MAX_SIZE_FACTOR,
                "The size factor factor must be between " + MIN_SIZE_FACTOR + " and " + MAX_SIZE_FACTOR + "." );
        ExpressionExperiment ee = expressionExperimentService.loadOrFail( id, EntityNotFoundException::new );
        DifferentialExpressionAnalysis analysis = differentialExpressionAnalysisService.loadWithExperimentAnalyzed( analysisId );
        if ( analysis == null ) {
            throw new EntityNotFoundException( "No analysis with ID " + analysisId );
        }
        // check the associated experiment
        if ( !getExpressionExperiment( analysis ).equals( ee ) ) {
            throw new IllegalArgumentException( "Analysis with ID " + analysisId + " does not belong to experiment with ID " + id );
        }
        ExpressionAnalysisResultSet rs = expressionAnalysisResultSetService.loadWithAnalysis( rsid );
        if ( rs == null ) {
            throw new EntityNotFoundException( "No result set with ID " + rsid + "." );
        }
        if ( !rs.getAnalysis().equals( analysis ) ) {
            throw new IllegalArgumentException( "Result set with ID " + id + " does not belong to analysis with ID " + analysisId );
        }
        if ( size == null ) {
            int finalSize = ( int ) ( sizeFactor * DEFAULT_QC_IMAGE_SIZE_PX );
            writePValueHistImage( ee, rs, finalSize, response );
        } else {
            writePValueHistThumbnailImage( ee, rs, size, response );
        }
    }

    private ExpressionExperiment getExpressionExperiment( DifferentialExpressionAnalysis analysis ) {
        if ( analysis.getExperimentAnalyzed() instanceof ExpressionExperimentSubSet ) {
            return ( ( ExpressionExperimentSubSet ) analysis.getExperimentAnalyzed() ).getSourceExperiment();
        } else if ( analysis.getExperimentAnalyzed() instanceof ExpressionExperiment ) {
            return ( ExpressionExperiment ) analysis.getExperimentAnalyzed();
        } else {
            throw new UnsupportedOperationException( "Unsupported type of analyzed experiment for analysis with ID " + analysis.getId() );
        }
    }

    @RequestMapping(value = "/expressionExperiment/eigenGenes.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView writeEigenGenes( @RequestParam("eeid") Long eeid ) throws IOException {
        ExpressionExperiment ee = expressionExperimentService.loadOrFail( eeid,
                EntityNotFoundException::new, "Could not load experiment with id " + eeid );// or access deined.
        SVDResult svdo = svdService.getSvd( ee );

        if ( svdo == null ) {
            throw new EntityNotFoundException( ee.getShortName() + " does not have a SVD." );
        }

        DoubleMatrix<BioMaterial, Integer> vMatrix = svdo.getVMatrix();

        /*
         * FIXME put the biomaterial names in there instead of the IDs.
         */
        /*
         * new DenseDoubleMatrix<String, String>() DoubleMatrix<String, String> matrix = new DenseDoubleMatrix<String,
         * String>( omatrix.getRawMatrix() ); matrix.setRowNames( stringNames ); matrix.setColumnNames( stringNames );
         */
        StringWriter s = new StringWriter();
        MatrixWriter<BioMaterial, Integer> mw = new MatrixWriter<>( s, new DecimalFormat( "#.######" ) );
        mw.writeMatrix( vMatrix, true );
        TextView tv = new TextView( "tab-separated-values" );
        tv.setContentDisposition( "attachment; filename=\"" + FilenameUtils.removeExtension( getEigenGenesFilename( ee ) ) + "\"" );
        return new ModelAndView( tv )
                .addObject( TextView.TEXT_PARAM, s.toString() );
    }

    @RequestMapping(value = "/expressionExperiment/visualizeSingleCellSparsityHeatmap.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public void visualizeSingleCellSparsityHeatmap(
            @RequestParam("id") Long id,
            @RequestParam("type") String type,
            @RequestParam(value = "cellSize", required = false) @Nullable Integer cellSize,
            @RequestParam(value = "transpose", defaultValue = "false") boolean transpose,
            HttpServletResponse response ) throws IOException {
        Assert.isTrue( cellSize == null || cellSize > 0 );
        ExpressionExperiment ee = expressionExperimentService.loadAndThawLiteOrFail( id, EntityNotFoundException::new, "No dataset with ID " + id + "." );
        SingleCellDimension singleCellDimension = singleCellExpressionExperimentService.getPreferredSingleCellDimensionWithoutCellIds( ee )
                .orElseThrow( () -> new EntityNotFoundException( ee.getShortName() + " does not have a preferred single-cell dimension." ) );
        QuantitationType qt = expressionExperimentService.getProcessedQuantitationType( ee )
                .orElseThrow( () -> new EntityNotFoundException( "No processed quantitation type found for " + ee.getShortName() + "." ) );
        BioAssayDimension dimension = expressionExperimentService.getBioAssayDimension( ee, qt, ProcessedExpressionDataVector.class );
        if ( dimension == null ) {
            throw new EntityNotFoundException( "No dimension found for " + qt + "." );
        }
        Collection<ExpressionExperimentSubSet> subSets = expressionExperimentService.getSubSetsWithBioAssays( ee, dimension );
        Map<BioAssay, Long> designElementsPerSample = expressionExperimentService.getNumberOfDesignElementsPerSample( ee );
        SingleCellSparsityHeatmap heatmap = new SingleCellSparsityHeatmap( ee, singleCellDimension, dimension, subSets, designElementsPerSample, SingleCellSparsityHeatmap.SingleCellHeatmapType.valueOf( type.toUpperCase() ) );
        if ( cellSize != null ) {
            heatmap.setCellSize( cellSize );
        }
        heatmap.setTranspose( transpose );
        BufferedImage image = heatmap.createImage();
        response.setContentType( MediaType.IMAGE_PNG_VALUE );
        ChartUtils.writeBufferedImageAsPNG( response.getOutputStream(), image, DEFAULT_DPI, "Single-cell sparsity heatmap for " + ee.getShortName(), buildInfo );
    }

    @RequestMapping(value = "/expressionExperiment/visualizeHeatmap.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public void visualizeHeatmap(
            @RequestParam("id") Long id,
            @RequestParam(value = "dimension", required = false) Long dimensionId,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "cellSize", required = false) @Nullable Integer cellSize,
            @RequestParam(value = "transpose", defaultValue = "false") boolean transpose,
            HttpServletResponse response ) throws IOException {
        Assert.isTrue( cellSize == null || cellSize > 0 );
        Assert.isTrue( limit > 0 && limit <= 100 );
        ExpressionExperiment ee = expressionExperimentService.loadAndThawLiteOrFail( id, EntityNotFoundException::new, "" );
        BioAssayDimension dimension;
        if ( dimensionId != null ) {
            dimension = expressionExperimentService.getBioAssayDimensionById( ee, dimensionId, ProcessedExpressionDataVector.class );
            if ( dimension == null ) {
                throw new EntityNotFoundException( ee.getShortName() + " does not have a dimension with ID " + dimensionId + "." );
            }
        } else {
            QuantitationType preferredQt = expressionExperimentService.getProcessedQuantitationType( ee )
                    .orElseThrow( () -> new EntityNotFoundException( ee.getShortName() + " does not have a set of processed vectors." ) );
            dimension = expressionExperimentService.getBioAssayDimension( ee, preferredQt, ProcessedExpressionDataVector.class );
            if ( dimension == null ) {
                throw new EntityNotFoundException( preferredQt + " does not have any associated dimension." );
            }
        }
        Slice<ProcessedExpressionDataVector> vectors = processedExpressionDataVectorService.getProcessedDataVectors( ee, dimension, offset, limit );
        ExpressionDataHeatmap heatmap = ExpressionDataHeatmap.fromVectors( ee, dimension, vectors, null );
        if ( cellSize != null ) {
            heatmap.setCellSize( cellSize );
        }
        heatmap.setTranspose( transpose );
        BufferedImage image = heatmap.createImage();
        response.setContentType( MediaType.IMAGE_PNG_VALUE );
        ChartUtils.writeBufferedImageAsPNG( response.getOutputStream(), image, DEFAULT_DPI, "Expression data heatmap for " + ee.getShortName(), buildInfo );
    }

    @RequestMapping(value = "/expressionExperiment/visualizeSubSetHeatmap.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public void visualizeSubSetHeatmap(
            @RequestParam("id") Long id,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "cellSize", required = false) @Nullable Integer cellSize,
            @RequestParam(value = "transpose", defaultValue = "false") boolean transpose,
            HttpServletResponse response ) throws IOException {
        Assert.isTrue( cellSize == null || cellSize > 0 );
        Assert.isTrue( limit > 0 && limit <= 100 );
        ExpressionExperimentSubSet subSet = expressionExperimentSubSetService.loadWithBioAssays( id );
        if ( subSet == null ) {
            throw new EntityNotFoundException( "No subset with ID " + id );
        }
        ExpressionExperiment ee = subSet.getSourceExperiment();
        QuantitationType preferredQt = expressionExperimentService.getProcessedQuantitationType( ee )
                .orElseThrow( () -> new EntityNotFoundException( ee.getShortName() + " does not have a set of processed vectors." ) );
        BioAssayDimension dimension = expressionExperimentService.getBioAssayDimension( ee, preferredQt, ProcessedExpressionDataVector.class );
        if ( dimension == null ) {
            throw new EntityNotFoundException( preferredQt + " does not have any associated dimension." );
        }
        Slice<ProcessedExpressionDataVector> vectors = processedExpressionDataVectorService.getProcessedDataVectors( ee, dimension, offset, limit );
        ExpressionDataHeatmap heatmap = ExpressionDataHeatmap.fromVectors( subSet, dimension, vectors, null );
        if ( cellSize != null ) {
            heatmap.setCellSize( cellSize );
        }
        heatmap.setTranspose( transpose );
        BufferedImage image = heatmap.createImage();
        response.setContentType( MediaType.IMAGE_PNG_VALUE );
        ChartUtils.writeBufferedImageAsPNG( response.getOutputStream(), image, DEFAULT_DPI, "Expression data heatmap for " + subSet.getName(), buildInfo );
    }

    @RequestMapping(value = "/expressionExperiment/visualizeSingleCellDataBoxplot.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public void visualizeSingleCellDataBoxplot(
            @RequestParam("id") Long id,
            @RequestParam(value = "quantitationType", required = false) @Nullable Long quantitationTypeId,
            @RequestParam("designElement") Long designElementId,
            @RequestParam(value = "assays", required = false) @Nullable Long[] assayIds,
            @RequestParam(value = "cellTypeAssignment", required = false) @Nullable String ctaName,
            @RequestParam(value = "cellLevelCharacteristics", required = false) @Nullable Long clcId,
            @RequestParam(value = "focusedCharacteristic", required = false) @Nullable Long focusedCharacteristicId,
            @RequestParam(value = "size", defaultValue = "1.0") double sizeFactor,
            HttpServletResponse response ) throws IOException {
        if ( clcId != null && ctaName != null ) {
            throw new IllegalArgumentException( "Cannot provide both 'cellTypeAssignment' and 'cellLevelCharacteristics' at the same time." );
        }
        Assert.isTrue( sizeFactor >= MIN_SIZE_FACTOR && sizeFactor <= MAX_SIZE_FACTOR,
                "The size factor factor must be between " + MIN_SIZE_FACTOR + " and " + MAX_SIZE_FACTOR + "." );
        ExpressionExperiment ee = expressionExperimentService.loadOrFail( id, EntityNotFoundException::new );
        QuantitationType qt;
        if ( quantitationTypeId != null ) {
            qt = quantitationTypeService.loadByIdAndVectorType( quantitationTypeId, ee, SingleCellExpressionDataVector.class );
            if ( qt == null ) {
                throw new EntityNotFoundException( ee + " does not have a quantitation type with ID " + quantitationTypeId + "." );
            }
        } else {
            qt = singleCellExpressionExperimentService.getPreferredSingleCellQuantitationType( ee )
                    .orElseThrow( () -> new EntityNotFoundException( ee + " does not have a preferred single-cell quantitation type." ) );
        }
        CompositeSequence designElement = compositeSequenceService.loadOrFail( designElementId, EntityNotFoundException::new );
        Collection<Gene> genes;
        genes = compositeSequenceService.getGenes( designElement );
        Gene gene;
        if ( genes.isEmpty() ) {
            log.warn( "No gene mapped by " + designElement );
            gene = null;
        } else if ( genes.size() > 1 ) {
            log.warn( "More than one gene mapped by " + designElement + "." );
            gene = null;
        } else {
            gene = genes.iterator().next();
        }
        SingleCellExpressionDataVector vector = singleCellExpressionExperimentService.getSingleCellDataVectorWithoutCellIds( ee, qt, designElement );
        if ( vector == null ) {
            throw new EntityNotFoundException( "No vector for design element with ID " + designElementId + "." );
        }
        // TODO: cpm normalization
        try {
            vector = ScaleTypeConversionUtils.convertVectors( Collections.singletonList( vector ), ScaleType.LOG10, SingleCellExpressionDataVector.class ).iterator().next();
        } catch ( QuantitationTypeConversionException e ) {
            throw new RuntimeException( e );
        }
        SingleCellDataBoxplot dataset = new SingleCellDataBoxplot( vector );
        dataset.setShowMean( false );
        if ( assayIds != null ) {
            Map<Long, BioAssay> baMap = IdentifiableUtils.getIdMap( vector.getSingleCellDimension().getBioAssays() );
            List<BioAssay> assays = Arrays.stream( assayIds )
                    .map( baId -> requireNonNull( baMap.get( baId ), "BioAssay with ID " + baId + " does not belong to " + ee.getShortName() + "." ) )
                    .collect( Collectors.toList() );
            dataset.setBioAssays( assays );
        }
        if ( ctaName != null ) {
            CellTypeAssignment cta = requireNonNull( singleCellExpressionExperimentService.getCellTypeAssignment( ee, qt, ctaName ) );
            dataset.setCellLevelCharacteristics( cta );
            if ( focusedCharacteristicId != null ) {
                dataset.setFocusedCharacteristic( cta.getCellTypes().stream().filter( cl -> cl.getId().equals( focusedCharacteristicId ) ).findFirst().orElseThrow( IllegalArgumentException::new ) );
            }
        }
        if ( clcId != null ) {
            CellLevelCharacteristics clc = requireNonNull( singleCellExpressionExperimentService.getCellLevelCharacteristics( ee, qt, clcId ) );
            dataset.setCellLevelCharacteristics( clc );
            if ( focusedCharacteristicId != null ) {
                dataset.setFocusedCharacteristic( clc.getCharacteristics().stream().filter( cl -> cl.getId().equals( focusedCharacteristicId ) ).findFirst().orElseThrow( IllegalArgumentException::new ) );
            }
        }
        ChartFactory.setChartTheme( chartTheme );
        JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(
                "Single-cell expression data for " + ( gene != null ? gene.getOfficialSymbol() : designElement.getName() ) + " in " + ee.getShortName(),
                "Assay", "Expression data (log10)", dataset.createDataset(), ctaName != null || clcId != null );
        if ( ( ctaName == null && clcId == null ) || focusedCharacteristicId != null ) {
            // TODO: add per-row annotation, this does not appear to be supported by JFreeChart
            dataset.createAnnotations().forEach( chart.getCategoryPlot()::addAnnotation );
        }
        response.setContentType( MediaType.IMAGE_PNG_VALUE );
        response.setHeader( "Cache-Control", "no-cache" );
        ChartUtils.writeChartAsPNG( response.getOutputStream(), chart,
                ( int ) ( sizeFactor * dataset.getNumberOfBoxplots() * 150 ),
                ( int ) ( sizeFactor * DEFAULT_QC_IMAGE_SIZE_PX ), DEFAULT_DPI,
                "Single-cell data boxplot for " + ee.getShortName(), buildInfo );
    }

    /**
     * Support method for writeDetailedFactorAnalysis
     *
     * @param categories map of factor ID to text value. Strings will be unique, but possibly abbreviated and/or munged.
     */
    private void getCategories( ExperimentalFactor ef, Map<Long, String> categories ) {
        if ( ef == null )
            return;
        int maxCategoryLabelLength = 10;

        for ( FactorValue fv : ef.getFactorValues() ) {
            String value = FactorValueUtils.getSummaryString( fv, "; " );

            if ( StringUtils.isBlank( value ) ) {
                value = fv + "--??";
            }

            if ( value.startsWith( BatchInfoPopulationHelperServiceImpl.BATCH_FACTOR_NAME_PREFIX ) ) {
                value = value.replaceFirst( BatchInfoPopulationHelperServiceImpl.BATCH_FACTOR_NAME_PREFIX, "" );
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

        double[] binCounts = coexpCorrelationDistribution.getBinCounts();
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

        Path file = this.locateProbeCorrFile( ee );

        // Current format is to have just one file for each analysis.
        if ( !Files.isReadable( file ) ) {
            return null;
        }

        try ( BufferedReader in = Files.newBufferedReader( file ) ) {
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
     * Get the eigengene for the given component.
     * The values are rescaled so that jfreechart can cope. Small numbers give it fits.
     */
    private Double[] getEigenGene( SVDResult svdo, Integer component ) {
        DoubleArrayList eigenGeneL = new DoubleArrayList(
                ArrayUtils.toPrimitive( svdo.getVMatrix().getColObj( component ) ) );
        DescriptiveWithMissing.standardize( eigenGeneL );
        return ArrayUtils.toObject( eigenGeneL.elements() );
    }

    private Map<ExperimentalFactor, String> getFactorNames( ExpressionExperiment ee, int maxWidth ) {
        if ( ee.getExperimentalDesign() == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment does not have an experimental design." );
        }
        Collection<ExperimentalFactor> factors = ee.getExperimentalDesign().getExperimentalFactors();
        Map<ExperimentalFactor, String> efs = new HashMap<>();
        for ( ExperimentalFactor ef : factors ) {
            efs.put( ef, StringUtils.abbreviate( StringUtils.capitalize( ef.getName() ), maxWidth ) );
        }
        return efs;
    }

    /**
     * @param mvr MeanVarianceRelation object that contains the datapoints to plot
     * @return XYSeriesCollection which contains the Mean-variance and Loess series
     */
    private XYSeriesCollection getMeanVariance( MeanVarianceRelation mvr ) {

        XYSeriesCollection dataset = new XYSeriesCollection();

        if ( mvr == null ) {
            return dataset;
        }

        double[] means = mvr.getMeans();
        double[] variances = mvr.getVariances();

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

    private CategoryDataset getPCAScree( SVDResult svdo ) {
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
    private Path locateProbeCorrFile( ExpressionExperiment ee ) {
        String shortName = ee.getShortName();
        String suffix = ".correlDist.txt";
        return analysisStoragePath.resolve( shortName + suffix );
    }

    /**
     * For conversion from legacy system.
     */
    private void corrDistFileToPersistent( Path file, ExpressionExperiment ee, DoubleArrayList counts ) {
        log.info( "Converting from pvalue distribution file to persistent stored version" );

        CoexpCorrelationDistribution coexpd = CoexpCorrelationDistribution.Factory.newInstance();
        coexpd.setNumBins( counts.size() );
        coexpd.setBinCounts( Arrays.copyOf( counts.elements(), counts.size() ) );

        try {
            coexpressionAnalysisService.addCoexpCorrelationDistribution( ee, coexpd );

            if ( Files.deleteIfExists( file ) ) {
                log.info( "Old file deleted" );
            } else {
                log.info( "Old file could not be deleted" );
            }
        } catch ( Exception e ) {
            log.info( "Could not save the corr dist: " + e.getMessage() );
        }
    }

    private void writeDetailedFactorAnalysis( ExpressionExperiment ee, int perChartSize, HttpServletResponse os ) throws Exception {
        SVDResult svdo = svdService.getSvdFactorAnalysis( ee );
        if ( svdo == null ) {
            writePlaceholderImage( os, perChartSize );
            return;
        }

        if ( svdo.getFactors().isEmpty() && svdo.getDates().isEmpty() ) {
            writePlaceholderImage( os, perChartSize );
            return;
        }
        Map<Integer, Map<ExperimentalFactor, Double>> factorCorrelations = svdo.getFactorCorrelations();
        // Map<Integer, Map<Long, Double>> factorPvalues = svdo.getFactorPvalues();
        Map<Integer, Double> dateCorrelations = svdo.getDateCorrelations();

        assert ee.equals( svdo.getExperimentAnalyzed() );

        int maxWidth = 30;
        Map<ExperimentalFactor, String> efs = this.getFactorNames( ee, maxWidth );
        Collection<ExperimentalFactor> continuousFactors = new HashSet<>();
        if ( ee.getExperimentalDesign() == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment does not have an experimental design." );
        }
        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            boolean isContinous = ef.getType().equals( FactorType.CONTINUOUS );
            if ( isContinous ) {
                continuousFactors.add( ef );
            }
        }

        /*
         * Make plots of the dates vs. PCs, factors vs. PCs.
         */
        int MAX_COMP = 3;

        Map<Long, List<JFreeChart>> charts = new LinkedHashMap<>();
        /*
         * FACTORS
         */
        String componentShorthand = "PC";
        for ( Integer component : factorCorrelations.keySet() ) {

            if ( component >= MAX_COMP )
                break;
            String xaxisLabel = componentShorthand + ( component + 1 );

            for ( ExperimentalFactor efId : factorCorrelations.get( component ).keySet() ) {

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
                    this.getCategories( efId, categories );
                }

                Double a = factorCorrelations.get( component ).get( efId );
                String plotname = ( efs.get( efId ) == null ? "?" : efs.get( efId ) ) + " " + xaxisLabel; // unique?

                if ( a != null && !Double.isNaN( a ) ) {
                    String title = plotname + " " + String.format( "%.2f", a );
                    List<Number> values = svdo.getFactors().get( efId );
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
                        ChartFactory.setChartTheme( chartTheme );
                        chart = ChartFactory
                                .createScatterPlot( title, null, xaxisLabel, series, PlotOrientation.VERTICAL, false,
                                        false, false );
                        XYPlot plot = chart.getXYPlot();
                        plot.setRangeGridlinesVisible( false );
                        plot.setDomainGridlinesVisible( false );

                        XYItemRenderer renderer = plot.getRenderer();
                        renderer.setSeriesShape( 0, new Ellipse2D.Double( 0, 0, 3, 3 ) );
                        float saturationDrop = ( float ) Math.min( 1.0, component * 0.8f / MAX_COMP );
                        renderer.setSeriesPaint( 0, Color.getHSBColor( 0.0f, 1.0f - saturationDrop, 0.7f ) );
                        plot.setRenderer( renderer );
                    }

                    charts.computeIfAbsent( efId.getId(), k -> new ArrayList<>( MAX_COMP ) ).add( chart );
                }
            }
        }

        /*
         * DATES
         */
        for ( Integer component : dateCorrelations.keySet() ) {
            String xaxisLabel = componentShorthand + ( component + 1 );

            List<Date> dates = svdo.getDates();

            List<Date> nonNullDates = dates.stream()
                    .filter( Objects::nonNull )
                    .collect( Collectors.toList() );

            if ( nonNullDates.isEmpty() )
                break;

            long secspan = ubic.basecode.util.DateUtil.numberOfSecondsBetweenDates( nonNullDates );

            if ( component >= MAX_COMP )
                break;
            Double a = dateCorrelations.get( component );

            if ( a != null && !Double.isNaN( a ) ) {
                Double[] eigenGene = svdo.getVMatrix().getColObj( component );

                /*
                 * Plot eigengene vs values, add correlation to the plot
                 */
                TimeSeries series = new TimeSeries( "Dates vs. eigen" + ( component + 1 ) );
                for ( int i = 0; i < dates.size(); i++ ) {
                    Date d = dates.get( i );
                    if ( d == null ) {
                        continue;
                    }
                    // if span is less than an hour, retain the minute.
                    if ( secspan < 60 * 60 ) {
                        series.addOrUpdate( new Minute( d ), eigenGene[i] );
                    } else {
                        series.addOrUpdate( new Hour( d ), eigenGene[i] );
                    }

                }
                TimeSeriesCollection dataset = new TimeSeriesCollection();
                dataset.addSeries( series );

                ChartFactory.setChartTheme( chartTheme );
                JFreeChart chart = ChartFactory
                        .createTimeSeriesChart( "Dates: " + xaxisLabel + " " + String.format( "r=%.2f", a ), null,
                                xaxisLabel, dataset, false, false, false );

                XYPlot xyPlot = chart.getXYPlot();

                // standard renderer makes lines.
                XYDotRenderer renderer = new XYDotRenderer();
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
                charts.computeIfAbsent( -1L, k -> new ArrayList<>( MAX_COMP ) ).add( chart );
            }
        }

        /*
         * Plot in a grid, with each factor as a column. FIXME What if we have too many factors to fit on the screen?
         */
        int rows = charts.size();
        BufferedImage image = new BufferedImage( MAX_COMP * perChartSize, rows * perChartSize,
                BufferedImage.TYPE_INT_ARGB );
        Graphics2D g2 = image.createGraphics();
        int currentY = 0;
        for ( Long id : charts.keySet() ) {
            int currentX = 0;
            for ( JFreeChart chart : charts.get( id ) ) {
                chartTheme.apply( chart );
                chart.draw( g2, new Rectangle2D.Double( currentX, currentY, perChartSize, perChartSize ), null, null );
                currentX += perChartSize;
            }
            currentY += perChartSize;
        }

        os.setContentType( MediaType.IMAGE_PNG_VALUE );
        ChartUtils.writeBufferedImageAsPNG( os.getOutputStream(), image, DEFAULT_DPI,
                "Detailed factor analysis for " + ee.getShortName(), buildInfo );
    }

    private void writeMeanVariance( ExpressionExperiment ee, QuantitationType quantitationType, MeanVarianceRelation mvr, int size, HttpServletResponse response ) throws Exception {
        // if number of datapoints > THRESHOLD then alpha = TRANSLUCENT, else alpha = OPAQUE
        final int THRESHOLD = 1000;
        final int TRANSLUCENT = 50;
        final int OPAQUE = 255;

        // Set maximum plot range to Y_MAX + YRANGE * OFFSET to leave some extra white space
        final double OFFSET_FACTOR = 0.05f;

        if ( mvr == null ) {
            writePlaceholderImage( response, size );
            return;
        }

        // get data points
        XYSeriesCollection collection = this.getMeanVariance( mvr );

        if ( collection.getSeries().isEmpty() ) {
            writePlaceholderImage( response, size );
            return;
        }

        String unit = QuantitationTypeUtils.getUnit( quantitationType );
        String xAxisLabel = "Mean (" + unit + ")";
        String yAxisLabel = "Variance (" + unit + "²)";

        ChartFactory.setChartTheme( chartTheme );
        JFreeChart chart = ChartFactory
                .createScatterPlot( "", xAxisLabel, yAxisLabel, collection, PlotOrientation.VERTICAL, false,
                        false, false );

        // adjust colors and shapes
        XYRegressionRenderer renderer = new XYRegressionRenderer();
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
            writePlaceholderImage( response, size );
            return;
        }
        double ybuffer = ( yRange ) * OFFSET_FACTOR;
        double xbuffer = ( xRange ) * OFFSET_FACTOR;
        double newYMin = series.getMinY() - ybuffer;
        double newYMax = series.getMaxY() + ybuffer;
        double newXMin = series.getMinX() - xbuffer;
        double newXMax = series.getMaxX() + xbuffer;
        chart.getXYPlot().getRangeAxis().setRange( newYMin, newYMax );
        chart.getXYPlot().getDomainAxis().setRange( newXMin, newXMax );

        response.setContentType( MediaType.IMAGE_PNG_VALUE );
        response.setHeader( "Cache-Control", "no-cache" );
        response.setHeader( "Cache-Control", "no-cache" );
        ChartUtils.writeChartAsPNG( response.getOutputStream(), chart, size, size, DEFAULT_DPI,
                "Mean-variance plot for " + ee.getShortName(), buildInfo );
    }

    /**
     * Remove outliers from the MeanVarianceRelation by removing those points which have: (zscore(mean) > zscoreMax ||
     * zscore(variance) > zscoreMax)
     */
    @SuppressWarnings("unused")
    private MeanVarianceRelation removeMVOutliers( MeanVarianceRelation mvr, double zscoreMax ) {
        MeanVarianceRelation ret = MeanVarianceRelation.Factory.newInstance();

        DoubleArrayList vars = new DoubleArrayList( mvr.getVariances() );
        DoubleArrayList means = new DoubleArrayList( mvr.getMeans() );

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

        filteredVars.trimToSize();
        ret.setVariances( filteredVars.elements() );
        filteredMeans.trimToSize();
        ret.setMeans( filteredMeans.elements() );

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
    private void writePCAFactors( ExpressionExperiment ee, SVDResult svdo, double sizeFactor, HttpServletResponse response ) throws Exception {
        Map<Integer, Map<ExperimentalFactor, Double>> factorCorrelations = svdo.getFactorCorrelations();
        // Map<Integer, Map<Long, Double>> factorPvalues = svdo.getFactorPvalues();
        Map<Integer, Double> dateCorrelations = svdo.getDateCorrelations();

        assert ee.equals( svdo.getExperimentAnalyzed() );

        if ( factorCorrelations.isEmpty() && dateCorrelations.isEmpty() ) {
            int size = ( int ) ( sizeFactor * DEFAULT_QC_IMAGE_SIZE_PX );
            this.writePlaceholderImage( response, size );
            return;
        }
        int maxWidth = 10;

        Map<ExperimentalFactor, String> efs = this.getFactorNames( ee, maxWidth );

        DefaultCategoryDataset series = new DefaultCategoryDataset();

        /*
         * With two groups, or a continuous factor, we get rank correlations
         */
        int MAX_COMP = 3;
        double STUB = 0.05; // always plot a little thing so we know its there.
        for ( Integer component : factorCorrelations.keySet() ) {
            if ( component >= MAX_COMP )
                break;
            for ( ExperimentalFactor efId : factorCorrelations.get( component ).keySet() ) {
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
        ChartFactory.setChartTheme( chartTheme );
        JFreeChart chart = ChartFactory
                .createBarChart( "", "Factors", "Component assoc.", series, PlotOrientation.VERTICAL, true, false,
                        false );

        chart.getCategoryPlot().getRangeAxis().setRange( 0, 1 );
        BarRenderer renderer = ( BarRenderer ) chart.getCategoryPlot().getRenderer();
        renderer.setShadowVisible( false );
        chart.getCategoryPlot().setRangeGridlinesVisible( false );
        chart.getCategoryPlot().setDomainGridlinesVisible( false );

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
         * Give figure more room... up to a limit
         */
        int width = ( int ) ( sizeFactor * Math.min( chart.getCategoryPlot().getCategories().size() * MAX_COMP * 120, DEFAULT_QC_IMAGE_SIZE_PX ) );
        response.setContentType( MediaType.IMAGE_PNG_VALUE );
        response.setHeader( "Cache-Control", "no-cache" );
        ChartUtils.writeChartAsPNG( response.getOutputStream(), chart, width,
                ( int ) ( sizeFactor * DEFAULT_QC_IMAGE_SIZE_PX ), DEFAULT_DPI,
                "PCA factors for " + ee.getShortName(), buildInfo );
    }

    private void writePCAScree( ExpressionExperiment ee, SVDResult svdo, int size, HttpServletResponse response ) throws Exception {
        /*
         * Make a scree plot.
         */
        CategoryDataset series = this.getPCAScree( svdo );

        if ( series.getColumnCount() == 0 ) {
            return;
        }
        int MAX_COMPONENTS_FOR_SCREE = 10;
        ChartFactory.setChartTheme( chartTheme );
        JFreeChart chart = ChartFactory
                .createBarChart( "", "Component (up to " + MAX_COMPONENTS_FOR_SCREE + ")", "Fraction of var.", series,
                        PlotOrientation.VERTICAL, false, false, false );

        BarRenderer renderer = ( BarRenderer ) chart.getCategoryPlot().getRenderer();
        renderer.setShadowVisible( false );
        chart.getCategoryPlot().setRangeGridlinesVisible( false );
        chart.getCategoryPlot().setDomainGridlinesVisible( false );
        response.setContentType( MediaType.IMAGE_PNG_VALUE );
        response.setHeader( "Cache-Control", "no-cache" );
        ChartUtils.writeChartAsPNG( response.getOutputStream(), chart, size, size, DEFAULT_DPI, "PCA scree for " + ee.getShortName(), buildInfo );
    }

    /**
     * Write a blank image so user doesn't see the broken icon.
     */
    private void writePlaceholderImage( HttpServletResponse response, int size ) throws IOException {
        BufferedImage buffer = new BufferedImage( size, size, BufferedImage.TYPE_INT_RGB );
        Graphics g = buffer.createGraphics();
        g.setColor( Color.lightGray );
        g.fillRect( 0, 0, size, size );
        g.setColor( Color.black );
        g.drawString( "Not available", size / 4, size / 4 );
        response.setContentType( MediaType.IMAGE_PNG_VALUE );
        ChartUtils.writeBufferedImageAsPNG( response.getOutputStream(), buffer, DEFAULT_DPI, "Placeholder image", buildInfo );
    }

    /**
     * Write a blank thumbnail image so user doesn't see the broken icon.
     */
    private void writePlaceholderThumbnailImage( HttpServletResponse response, int size ) throws IOException {
        size = Math.min( size, MAX_QC_IMAGE_THUMBNAIL_SIZE_PX );
        // Make the image a bit bigger to account for the empty space around the generated image.
        // If we can find a way to remove this empty space, we don't need to make the chart bigger.
        BufferedImage buffer = new BufferedImage( ( int ) ( 1.4 * size ), size,
                BufferedImage.TYPE_INT_RGB );
        Graphics g = buffer.createGraphics();
        g.setColor( Color.white );
        g.fillRect( 0, 0, ( int ) ( 1.4 * size ), size );
        g.setColor( Color.gray );
        g.drawLine( 8, size + 5, size + 8, size + 5 ); // x-axis
        g.drawLine( 8, 5, 8, size + 5 ); // y-axis
        g.setColor( Color.black );
        g.drawString( "N/A", 9, size );
        response.setContentType( MediaType.IMAGE_PNG_VALUE );
        ChartUtils.writeBufferedImageAsPNG( response.getOutputStream(), buffer, DEFAULT_DPI, "Placeholder thumbnail", buildInfo );
    }

    private void writeProbeCorrHistImage( ExpressionExperiment ee, int size, HttpServletResponse response ) throws IOException {
        XYSeries series = this.getCorrelHist( ee );

        if ( series == null || series.getItemCount() == 0 ) {
            writePlaceholderImage( response, size );
            return;
        }

        XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
        xySeriesCollection.addSeries( series );
        ChartFactory.setChartTheme( chartTheme );
        JFreeChart chart = ChartFactory
                .createXYLineChart( "", "Correlation", "Frequency", xySeriesCollection, PlotOrientation.VERTICAL, false,
                        false, false );
        chart.getXYPlot().setRangeGridlinesVisible( false );
        chart.getXYPlot().setDomainGridlinesVisible( false );

        response.setContentType( MediaType.IMAGE_PNG_VALUE );
        response.setHeader( "Cache-Control", "no-cache" );
        ChartUtils.writeChartAsPNG( response.getOutputStream(), chart, size, size, DEFAULT_DPI,
                "Probe correlation histogram for " + ee.getShortName(), buildInfo );
    }

    /**
     * Has to handle the situation where there might be more than one ResultSet.
     */
    private void writePValueHistImage( ExpressionExperiment ee, ExpressionAnalysisResultSet rs, int finalSize, HttpServletResponse response ) throws IOException {
        XYSeries series = this.getDiffExPvalueHistXYSeries( rs );

        if ( series == null ) {
            writePlaceholderImage( response, finalSize );
            return;
        }

        XYSeriesCollection xySeriesCollection = new XYSeriesCollection( series );

        ChartFactory.setChartTheme( chartTheme );
        JFreeChart chart = ChartFactory
                .createXYLineChart( "", "P-value", "Frequency", xySeriesCollection, PlotOrientation.VERTICAL, false,
                        false, false );
        chart.getXYPlot().setRangeGridlinesVisible( false );
        chart.getXYPlot().setDomainGridlinesVisible( false );
        // allows for seeing the edges of the P-value histogram
        chart.getXYPlot().getDomainAxis().setRange( -0.01, 1.01 );

        response.setContentType( MediaType.IMAGE_PNG_VALUE );
        response.setHeader( "Cache-Control", "no-cache" );
        ChartUtils.writeChartAsPNG( response.getOutputStream(), chart, ( int ) ( 1.4 * finalSize ), finalSize, DEFAULT_DPI, "P-value histogram for " + ee.getShortName(), buildInfo );
    }

    /**
     * Write p-value histogram thumbnail image.
     */
    private void writePValueHistThumbnailImage( ExpressionExperiment ee, ExpressionAnalysisResultSet rs, int size, HttpServletResponse response ) throws IOException {
        XYSeries series = this.getDiffExPvalueHistXYSeries( rs );

        if ( series == null ) {
            writePlaceholderThumbnailImage( response, size );
            return;
        }

        XYSeriesCollection xySeriesCollection = new XYSeriesCollection( series );

        ChartFactory.setChartTheme( chartTheme );
        JFreeChart chart = ChartFactory
                .createXYLineChart( "", "", "", xySeriesCollection, PlotOrientation.VERTICAL, false, false, false );

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
        int finalSize = Math.min( size, MAX_QC_IMAGE_THUMBNAIL_SIZE_PX );
        response.setHeader( "Cache-Control", "no-cache" );
        ChartUtils.writeChartAsPNG( response.getOutputStream(), chart, ( int ) ( 1.4 * finalSize ), finalSize, DEFAULT_DPI, "P-value histogram thumbnail for " + ee.getShortName(), buildInfo );
    }

    /**
     * @return JFreeChart XYSeries representing the histogram for the requested result set
     */
    private XYSeries getDiffExPvalueHistXYSeries( ExpressionAnalysisResultSet rs ) {
        Histogram hist = expressionAnalysisResultSetService.loadPvalueDistribution( rs );
        if ( hist != null ) {
            XYSeries xySeries;
            xySeries = new XYSeries( rs.getId(), true, true );
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
