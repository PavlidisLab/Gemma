/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.persistence.service.analysis.expression.sampleCoexpression;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.math.MatrixStats;
import ubic.basecode.math.linearmodels.DesignMatrix;
import ubic.basecode.math.linearmodels.LeastSquaresFit;
import ubic.basecode.math.linearmodels.MeanVarianceEstimator;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalysisConfig;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalysisUtil;
import ubic.gemma.core.analysis.expression.diff.LinearModelAnalyzer;
import ubic.gemma.core.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.core.analysis.preprocess.svd.SVDServiceHelper;
import ubic.gemma.core.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.core.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrixColumnSort;
import ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manage the "sample correlation/coexpression" matrices.
 *
 * @author paul
 */
@Component
public class SampleCoexpressionAnalysisServiceImpl implements SampleCoexpressionAnalysisService {

    private static final Logger log = LoggerFactory.getLogger( SampleCoexpressionAnalysisServiceImpl.class );
    private static final ByteArrayConverter bac = new ByteArrayConverter();
    private static final String MSG_ERR_NO_VECTORS = "No processed expression vectors available for experiment, can not compute sample correlation matrix.";
    private static final String MSG_ERR_NO_DESIGN = "Can not run factor regression! No experimental factors found.";
    private static final String MSG_ERR_NO_FACTORS = "Can not run factor regression! No factors to include in the regressed matrix.";
    private static final String MSG_ERR_NO_BAS_IN_BAD = "No bioassays in the bioAssayDimension id:%d";
    private static final String MSG_ERR_BIOASSAY_MISMATCH = "Number of bioassays doesn't match length of the best bioAssayDimension. BAs in dimension: %d, rows in cormat: %d";
    private static final String MSG_WARN_NO_REGRESSED_MATRIX = "No regressed matrix for ee %d, returning the full matrix instead.";
    private static final String MSG_INFO_RUNNING_SCM = "Sample Correlations not calculated for ee %d yet, running them now.";
    private static final String MSG_INFO_COMPUTING_SCM = "Computing sample coexpression matrix for ee %d, regressing: %s";
    private static final String MSG_INFO_REGRESSING = "Regressing out covariates";
    private static final String MSG_INFO_BATCH_REMOVED = "Removed 'batch' from the list of significant factors.";
    private static final String MSG_INFO_ANALYSIS_STATUS = " | SAMPLE CORR ANALYSIS | %s\t | full matrix : %s\t | regressed matrix: %s";
    private static final String A_STATUS_AVAILABLE = "Available";
    private static final String A_STATUS_NOT_AVAILABLE = "Not available";
    private static final double IMPORTANCE_THRESHOLD = 0.01;
    private static final String A_STATUS_COMPUTED = "Just computed";
    private static final String A_STATUS_LOADED = "Loaded from db";

    @Autowired
    private ExpressionDataMatrixService expressionDataMatrixService;
    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;
    @Autowired
    private SampleCoexpressionAnalysisDao sampleCoexpressionAnalysisDao;
    @Autowired
    private SVDServiceHelper svdService;
    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;

    @Override
    public DoubleMatrix<BioAssay, BioAssay> loadFullMatrix( ExpressionExperiment ee ) {
        return this.toDoubleMatrix( this.load( ee ).getFullCoexpressionMatrix() );
    }

    @Override
    public DoubleMatrix<BioAssay, BioAssay> loadTryRegressedThenFull( ExpressionExperiment ee ) {
        SampleCoexpressionAnalysis analysis = this.load( ee );
        SampleCoexpressionMatrix matrix = analysis.getRegressedCoexpressionMatrix();
        if ( matrix == null ) {
            SampleCoexpressionAnalysisServiceImpl.log.warn( String
                    .format( SampleCoexpressionAnalysisServiceImpl.MSG_WARN_NO_REGRESSED_MATRIX, ee.getId() ) );
            matrix = analysis.getFullCoexpressionMatrix();
        }
        return this.toDoubleMatrix( matrix );
    }

    @Override
    public SampleCoexpressionAnalysis load( ExpressionExperiment ee ) {
        SampleCoexpressionAnalysis analysis = sampleCoexpressionAnalysisDao.load( ee );

        if ( analysis == null || analysis.getFullCoexpressionMatrix() == null || this.shouldComputeRegressed( ee,
                analysis ) ) {
            SampleCoexpressionAnalysisServiceImpl.log
                    .info( String.format( SampleCoexpressionAnalysisServiceImpl.MSG_INFO_RUNNING_SCM, ee.getId() ) );
            return this.compute( ee );
        }
        this.logCormatStatus( analysis, false );
        return analysis;
    }

    @Override
    public boolean hasAnalysis( ExpressionExperiment ee ) {
        return sampleCoexpressionAnalysisDao.load( ee ) != null;
    }

    @Override
    public SampleCoexpressionAnalysis compute( ExpressionExperiment ee ) {

        // Remove any old data
        this.removeForExperiment( ee );

        // Create new analysis
        Collection<ProcessedExpressionDataVector> vectors = processedExpressionDataVectorService
                .getProcessedDataVectors( ee );
        SampleCoexpressionAnalysis analysis = new SampleCoexpressionAnalysis( ee, // Analyzed experiment
                this.getMatrix( ee, false, vectors ), // Full
                this.getMatrix( ee, true, vectors ) );// Regressed

        // Persist
        this.logCormatStatus( analysis, true );
        return sampleCoexpressionAnalysisDao.create( analysis );
    }

    @Override
    public void removeForExperiment( ExpressionExperiment ee ) {
        this.sampleCoexpressionAnalysisDao.removeForExperiment( ee );
    }

    /**
     * Checks whether the regressed matrix should be computed for the given ee.
     * @param ee the experiment that will be checked for meeting all the conditions to have regressed matrix computed.
     * @param analysis the analysis that will be checked for already having a regressed matrix or not.
     * @return true, if the regression matrix should be run for the given combination of experiment and analysis. False if
     * computing it is not necessary or possible.
     */
    private boolean shouldComputeRegressed( ExpressionExperiment ee, SampleCoexpressionAnalysis analysis ) {
        return analysis.getRegressedCoexpressionMatrix() == null && !this.getImportantFactors( ee ).isEmpty();
    }

    private void logCormatStatus( SampleCoexpressionAnalysis analysis, boolean justComputed ) {
        String full = analysis.getFullCoexpressionMatrix() != null ?
                SampleCoexpressionAnalysisServiceImpl.A_STATUS_AVAILABLE :
                SampleCoexpressionAnalysisServiceImpl.A_STATUS_NOT_AVAILABLE;
        String reg = analysis.getRegressedCoexpressionMatrix() != null ?
                SampleCoexpressionAnalysisServiceImpl.A_STATUS_AVAILABLE :
                SampleCoexpressionAnalysisServiceImpl.A_STATUS_NOT_AVAILABLE;
        String comp = justComputed ?
                SampleCoexpressionAnalysisServiceImpl.A_STATUS_COMPUTED :
                SampleCoexpressionAnalysisServiceImpl.A_STATUS_LOADED;
        SampleCoexpressionAnalysisServiceImpl.log.info( String
                .format( SampleCoexpressionAnalysisServiceImpl.MSG_INFO_ANALYSIS_STATUS, comp, full, reg ) );
    }

    private DoubleMatrix<BioAssay, BioAssay> toDoubleMatrix( SampleCoexpressionMatrix matrix ) {
        if ( matrix == null )
            return null;

        byte[] matrixBytes = matrix.getCoexpressionMatrix();

        final List<BioAssay> bioAssays = matrix.getBioAssayDimension().getBioAssays();
        int numBa = bioAssays.size();

        if ( numBa == 0 ) {
            throw new IllegalArgumentException(
                    String.format( SampleCoexpressionAnalysisServiceImpl.MSG_ERR_NO_BAS_IN_BAD,
                            matrix.getBioAssayDimension().getId() ) );
        }

        try {
            double[][] rawMatrix = SampleCoexpressionAnalysisServiceImpl.bac
                    .byteArrayToDoubleMatrix( matrixBytes, numBa );
            DoubleMatrix<BioAssay, BioAssay> result = new DenseDoubleMatrix<>( rawMatrix );
            result.setRowNames( bioAssays );
            result.setColumnNames( bioAssays );
            result = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( result );
            result = result.subsetRows( result.getColNames() ); // enforce same order on rows.
            return result;
        } catch ( IllegalArgumentException e ) {
            SampleCoexpressionAnalysisServiceImpl.log.error( e.getMessage() );
            e.printStackTrace();
            return null;
        }
    }

    private SampleCoexpressionMatrix getMatrix( ExpressionExperiment ee, boolean regress,
            Collection<ProcessedExpressionDataVector> vectors ) {
        SampleCoexpressionAnalysisServiceImpl.log.info( String
                .format( SampleCoexpressionAnalysisServiceImpl.MSG_INFO_COMPUTING_SCM, ee.getId(), regress ) );

        ExpressionDataDoubleMatrix mat = this.loadDataMatrix( ee, regress, vectors );
        if ( mat == null ) {
            return null;
        }

        DoubleMatrix<BioAssay, BioAssay> cormat = this.dataToDoubleMat( mat );
        // Check consistency
        BioAssayDimension bestBioAssayDimension = mat.getBestBioAssayDimension();
        if ( cormat.rows() != bestBioAssayDimension.getBioAssays().size() ) {
            throw new IllegalStateException(
                    String.format( SampleCoexpressionAnalysisServiceImpl.MSG_ERR_BIOASSAY_MISMATCH,
                            bestBioAssayDimension.getBioAssays().size(), cormat.rows() ) );
        }

        return new SampleCoexpressionMatrix( bestBioAssayDimension,
                SampleCoexpressionAnalysisServiceImpl.bac.doubleMatrixToBytes( cormat.getRawMatrix() ) );
    }

    private DoubleMatrix<BioAssay, BioAssay> dataToDoubleMat( ExpressionDataDoubleMatrix matrix ) {

        DoubleMatrix<BioMaterial, CompositeSequence> transposeR = matrix.getMatrix().transpose();

        DoubleMatrix<BioAssay, CompositeSequence> transpose = new DenseDoubleMatrix<>( transposeR.getRawMatrix() );
        transpose.setColumnNames( transposeR.getColNames() );
        for ( int i = 0; i < transpose.rows(); i++ ) {
            BioAssay s = transposeR.getRowName( i ).getBioAssaysUsedIn().iterator().next();
            transpose.setRowName( s, i );
        }

        return MatrixStats.correlationMatrix( transpose );
    }

    private ExpressionDataDoubleMatrix loadDataMatrix( ExpressionExperiment ee, boolean useRegression,
            Collection<ProcessedExpressionDataVector> vectors ) {
        if ( vectors == null || vectors.isEmpty() ) {
            SampleCoexpressionAnalysisServiceImpl.log.error( SampleCoexpressionAnalysisServiceImpl.MSG_ERR_NO_VECTORS );
            return null;
        }

        ExpressionDataDoubleMatrix mat;
        if ( useRegression ) {
            if ( ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
                SampleCoexpressionAnalysisServiceImpl.log
                        .error( SampleCoexpressionAnalysisServiceImpl.MSG_ERR_NO_DESIGN );
                return null;
            } else {
                mat = this.regressMajorFactors( ee, this.loadFilteredDataMatrix( ee, vectors, false ) );
            }
        } else {
            mat = this.loadFilteredDataMatrix( ee, vectors, true );
        }

        return mat;
    }

    private ExpressionDataDoubleMatrix loadFilteredDataMatrix( ExpressionExperiment ee,
            Collection<ProcessedExpressionDataVector> vectors, boolean requireSequences ) {
        FilterConfig fConfig = new FilterConfig();
        fConfig.setIgnoreMinimumRowsThreshold( true );
        fConfig.setIgnoreMinimumSampleThreshold( true );
        fConfig.setRequireSequences( requireSequences );
        // Loads using new array designs will fail. So we allow special case where there are no sequences.
        return expressionDataMatrixService.getFilteredMatrix( ee, fConfig, vectors );
    }

    /**
     * Regress out any 'major' factors, work with residuals only
     *
     * @param ee the experiment to load the factors from
     * @param mat the double matrix of processed vectors to regress
     * @return regressed double matrix
     */
    private ExpressionDataDoubleMatrix regressMajorFactors( ExpressionExperiment ee, ExpressionDataDoubleMatrix mat ) {
        Set<ExperimentalFactor> importantFactors = this.getImportantFactors( ee );
        if ( !importantFactors.isEmpty() ) {
            SampleCoexpressionAnalysisServiceImpl.log.info( SampleCoexpressionAnalysisServiceImpl.MSG_INFO_REGRESSING );
            DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
            config.setFactorsToInclude( importantFactors );
            mat = this.regressionResiduals( mat, config );
        }
        return mat;
    }

    private Set<ExperimentalFactor> getImportantFactors( ExpressionExperiment ee ) {
        Set<ExperimentalFactor> importantFactors = svdService
                .getImportantFactors( ee, ee.getExperimentalDesign().getExperimentalFactors(),
                        SampleCoexpressionAnalysisServiceImpl.IMPORTANCE_THRESHOLD );
        /* Remove 'batch' from important factors */
        ExperimentalFactor batch = null;
        for ( ExperimentalFactor factor : importantFactors ) {
            if ( factor.getName().toLowerCase().equals( "batch" ) )
                batch = factor;
        }
        if ( batch != null ) {
            importantFactors.remove( batch );
            SampleCoexpressionAnalysisServiceImpl.log
                    .info( SampleCoexpressionAnalysisServiceImpl.MSG_INFO_BATCH_REMOVED );
        }
        return importantFactors;
    }

    /**
     * @param matrix on which to perform regression.
     * @param config containing configuration of factors to include. Any interactions or subset configuration is
     *        ignored. Data are <em>NOT</em> log transformed unless they come in that way. (the qValueThreshold will be
     *        ignored)
     * @return residuals from the regression.
     */
    private ExpressionDataDoubleMatrix regressionResiduals( ExpressionDataDoubleMatrix matrix,
            DifferentialExpressionAnalysisConfig config ) {

        if ( config.getFactorsToInclude().isEmpty() ) {
            SampleCoexpressionAnalysisServiceImpl.log.error( SampleCoexpressionAnalysisServiceImpl.MSG_ERR_NO_FACTORS );
            return null;
        }

        List<ExperimentalFactor> factors = config.getFactorsToInclude();

        /*
         * Using ordered samples isn't necessary, it doesn't matter so long as the design matrix is in the same order.
         * We always want to use all the samples. There is no need to create a new bioassaydimension.
         */
        BioAssayDimension bad = matrix.getBestBioAssayDimension(); // this is what we do for the non-regressed version.
        assert bad.getId() != null;

        List<BioMaterial> samplesUsed = new ArrayList<>();
        for ( BioAssay ba : bad.getBioAssays() ) {
            samplesUsed.add( ba.getSampleUsed() );
        }

        // set up design matrix
        Map<ExperimentalFactor, FactorValue> baselineConditions = ExperimentalDesignUtils
                .getBaselineConditions( samplesUsed, factors );
        ObjectMatrix<String, String, Object> designMatrix = ExperimentalDesignUtils
                .buildDesignMatrix( factors, samplesUsed, baselineConditions );
        DesignMatrix properDesignMatrix = new DesignMatrix( designMatrix, true );

        ExpressionDataDoubleMatrix dmatrix = new ExpressionDataDoubleMatrix( matrix, samplesUsed, bad );
        DoubleMatrix<CompositeSequence, BioMaterial> namedMatrix = dmatrix.getMatrix();
        DoubleMatrix<String, String> sNamedMatrix = LinearModelAnalyzer.makeDataMatrix( designMatrix, namedMatrix );

        LeastSquaresFit fit = new LeastSquaresFit( properDesignMatrix, sNamedMatrix );

        DoubleMatrix2D residuals = fit.getResiduals();

        DoubleMatrix<CompositeSequence, BioMaterial> f = new DenseDoubleMatrix<>( residuals.toArray() );
        f.setRowNames( dmatrix.getMatrix().getRowNames() );
        f.setColumnNames( dmatrix.getMatrix().getColNames() );
        return new ExpressionDataDoubleMatrix( dmatrix, f );
    }
}
