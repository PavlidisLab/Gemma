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

import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix2D;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.math.MatrixRowStats;
import ubic.basecode.math.MatrixStats;
import ubic.basecode.math.linearmodels.DesignMatrix;
import ubic.basecode.math.linearmodels.LeastSquaresFit;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalysisConfig;
import ubic.gemma.core.analysis.expression.diff.LinearModelAnalyzerUtils;
import ubic.gemma.core.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.core.analysis.preprocess.svd.SVDServiceHelper;
import ubic.gemma.core.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.model.expression.experiment.ExperimentalDesignUtils;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrixColumnSort;
import ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionMatrix;
import ubic.gemma.model.common.auditAndSecurity.eventType.SampleCorrelationAnalysisEvent;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.*;

/**
 * Manage the "sample correlation/coexpression" matrices.
 *
 * @author paul
 */
@Service
@CommonsLog
public class SampleCoexpressionAnalysisServiceImpl implements SampleCoexpressionAnalysisService {

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
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private AuditTrailService auditTrailService;

    @Override
    @Transactional(readOnly = true)
    public DoubleMatrix<BioAssay, BioAssay> loadFullMatrix( ExpressionExperiment ee ) {
        SampleCoexpressionAnalysis analysis = sampleCoexpressionAnalysisDao.load( ee );
        return analysis != null ? toDoubleMatrix( analysis.getFullCoexpressionMatrix() ) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public DoubleMatrix<BioAssay, BioAssay> loadRegressedMatrix( ExpressionExperiment ee ) {
        SampleCoexpressionAnalysis analysis = sampleCoexpressionAnalysisDao.load( ee );
        return analysis != null && analysis.getRegressedCoexpressionMatrix() != null ? toDoubleMatrix( analysis.getRegressedCoexpressionMatrix() ) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public DoubleMatrix<BioAssay, BioAssay> loadBestMatrix( ExpressionExperiment ee ) {
        SampleCoexpressionAnalysis analysis = sampleCoexpressionAnalysisDao.load( ee );
        return analysis != null ? toDoubleMatrix( analysis.getBestCoexpressionMatrix() ) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public DoubleMatrix<BioAssay, BioAssay> retrieveExisting( ExpressionExperiment ee ) {
        ExpressionExperiment thawedee = this.expressionExperimentService.thawLite( ee );
        SampleCoexpressionAnalysis analysis = sampleCoexpressionAnalysisDao.load( thawedee );
        if ( analysis == null || analysis.getFullCoexpressionMatrix() == null || this.shouldComputeRegressed( thawedee, analysis ) ) {
            SampleCoexpressionAnalysisServiceImpl.log
                    .info( String.format( SampleCoexpressionAnalysisServiceImpl.MSG_INFO_RUNNING_SCM, thawedee.getId() ) );
            return null;
        } else {
            this.logCormatStatus( analysis, false );
            return toDoubleMatrix( analysis.getBestCoexpressionMatrix() );
        }
    }


    @Override
    @Transactional(readOnly = true)
    public PreparedCoexMatrices prepare( ExpressionExperiment ee ) {
        // Create new analysis
        Collection<ProcessedExpressionDataVector> vectors = processedExpressionDataVectorService
                .getProcessedDataVectors( ee );
        SampleCoexpressionMatrix matrix = this.getMatrix( ee, false, vectors );
        SampleCoexpressionMatrix regressedMatrix = this.getMatrix( ee, true, vectors );
        return new PreparedCoexMatrices( matrix, regressedMatrix );
    }


    /**
     * Unfortunately, this method breaks under high contention (see <a href="https://github.com/PavlidisLab/Gemma/issues/400">#400</a>,
     * so we need to fully lock the database while undergoing using {@link Isolation#SERIALIZABLE} transaction isolation
     * level. This annotation also has to be appled to other methods of this class that call compute() directly or
     * indirectly.
     *
     * PP changed this to do more of the processing in a read-only transaction
     */
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public DoubleMatrix<BioAssay, BioAssay> compute( ExpressionExperiment ee, PreparedCoexMatrices matrices ) {
        SampleCoexpressionMatrix matrix = matrices.matrix;

        if ( matrix == null ) {
            throw new RuntimeException( "Full coexpression matrix could not be computed." );
        }

        ExpressionExperiment thawedee = this.expressionExperimentService.thawLite( ee );

        // Remove any old data
        this.removeForExperiment( thawedee );

        SampleCoexpressionMatrix regressedMatrix = matrices.regressedMatrix; // this one is optional

        // this one is optional
        if ( regressedMatrix == null ) {
            log.warn( "Regressed coexpression matrix could not be computed, review experimental design? Experiment " + thawedee );
        }

        SampleCoexpressionAnalysis analysis = new SampleCoexpressionAnalysis( thawedee, // Analyzed experiment
                matrix, // Full
                regressedMatrix );// Regressed

        // Persist
        this.logCormatStatus( analysis, true );
        analysis = sampleCoexpressionAnalysisDao.create( analysis );

        auditTrailService.addUpdateEvent( ee, SampleCorrelationAnalysisEvent.class, "Sample correlation has been computed." );

        return toDoubleMatrix( analysis.getBestCoexpressionMatrix() );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAnalysis( ExpressionExperiment ee ) {
        return sampleCoexpressionAnalysisDao.existsByExperiment( ee );
    }

    @Override
    @Transactional
    public void removeForExperiment( ExpressionExperiment ee ) {
        this.sampleCoexpressionAnalysisDao.remove( this.sampleCoexpressionAnalysisDao.findByExperiment( ee ) );
    }

    /**
     * Checks whether the regressed matrix should be computed for the given ee.
     *
     * @param ee the experiment that will be checked for meeting all the conditions to have regressed matrix computed.
     * @param analysis the analysis that will be checked for already having a regressed matrix or not.
     * @return true, if the regression matrix should be run for the given combination of experiment and analysis. False
     *         if
     *         computing it is not necessary or possible.
     */
    private boolean shouldComputeRegressed( ExpressionExperiment ee, SampleCoexpressionAnalysis analysis ) {
        return analysis.getRegressedCoexpressionMatrix() == null && !this.getImportantFactors( ee ).isEmpty();
    }

    private void logCormatStatus( SampleCoexpressionAnalysis analysis, boolean justComputed ) {
        String full = analysis.getFullCoexpressionMatrix() != null ? SampleCoexpressionAnalysisServiceImpl.A_STATUS_AVAILABLE
                : SampleCoexpressionAnalysisServiceImpl.A_STATUS_NOT_AVAILABLE;
        String reg = analysis.getRegressedCoexpressionMatrix() != null ? SampleCoexpressionAnalysisServiceImpl.A_STATUS_AVAILABLE
                : SampleCoexpressionAnalysisServiceImpl.A_STATUS_NOT_AVAILABLE;
        String comp = justComputed ? SampleCoexpressionAnalysisServiceImpl.A_STATUS_COMPUTED : SampleCoexpressionAnalysisServiceImpl.A_STATUS_LOADED;
        SampleCoexpressionAnalysisServiceImpl.log.info( String
                .format( SampleCoexpressionAnalysisServiceImpl.MSG_INFO_ANALYSIS_STATUS, comp, full, reg ) );
    }

    private DoubleMatrix<BioAssay, BioAssay> toDoubleMatrix( SampleCoexpressionMatrix matrix ) {
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
            SampleCoexpressionAnalysisServiceImpl.log.error( e.getMessage(), e );
            return null;
        }
    }

    private SampleCoexpressionMatrix getMatrix( ExpressionExperiment ee, boolean regress,
            Collection<ProcessedExpressionDataVector> vectors ) {
        SampleCoexpressionAnalysisServiceImpl.log.info( String
                .format( SampleCoexpressionAnalysisServiceImpl.MSG_INFO_COMPUTING_SCM, ee.getId(), regress ) );

        ExpressionDataDoubleMatrix mat = this.loadDataMatrix( ee, regress, vectors );
        if ( mat == null ) {
            log.warn( "Could not get data matrix for " + ee );
            return null;
        }

        DoubleMatrix<BioAssay, BioAssay> cormat = this.dataToDoubleMat( mat );
        // Check consistency
        BioAssayDimension bestBioAssayDimension = mat.getBestBioAssayDimension();
        if ( cormat.rows() != bestBioAssayDimension.getBioAssays().size() ) {
            throw new RuntimeException( String.format( SampleCoexpressionAnalysisServiceImpl.MSG_ERR_BIOASSAY_MISMATCH,
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
        if ( vectors.isEmpty() ) {
            SampleCoexpressionAnalysisServiceImpl.log.warn( SampleCoexpressionAnalysisServiceImpl.MSG_ERR_NO_VECTORS );
            return null;
        }

        ExpressionDataDoubleMatrix mat;
        if ( useRegression ) {
            if ( ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
                SampleCoexpressionAnalysisServiceImpl.log
                        .warn( SampleCoexpressionAnalysisServiceImpl.MSG_ERR_NO_DESIGN );
                return null;
            }
            mat = this.regressMajorFactors( ee, this.loadFilteredDataMatrix( ee, vectors, false ) );

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
                .getImportantFactors( ee, ee.getExperimentalDesign().getExperimentalFactors(), // FIXME lazy-init of expfactors
                        SampleCoexpressionAnalysisServiceImpl.IMPORTANCE_THRESHOLD );
        /* Remove 'batch' from important factors */
        ExperimentalFactor batch = null;
        for ( ExperimentalFactor factor : importantFactors ) {
            if ( factor.getName().equalsIgnoreCase( "batch" ) )
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
         * We always want to use all the samples. There is no need to create a new bioAssayDimension.
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

        ObjectMatrix<String, String, Object> designMatrix;
        try {
            /*
             * A failure here can mean that the design matrix could not be built because of missing values; see #664
             */
            designMatrix = ExperimentalDesignUtils
                    .buildDesignMatrix( factors, samplesUsed, baselineConditions );
        } catch ( Exception e ) {
            return null;
        }
        DesignMatrix properDesignMatrix = new DesignMatrix( designMatrix, true );

        ExpressionDataDoubleMatrix dmatrix = new ExpressionDataDoubleMatrix( matrix, samplesUsed, bad );
        DoubleMatrix<CompositeSequence, BioMaterial> namedMatrix = dmatrix.getMatrix();
        DoubleMatrix<String, String> sNamedMatrix = LinearModelAnalyzerUtils.makeDataMatrix( designMatrix, namedMatrix );

        LeastSquaresFit fit = new LeastSquaresFit( properDesignMatrix, sNamedMatrix );

        DoubleMatrix2D residuals = fit.getResiduals();

        DoubleMatrix<CompositeSequence, BioMaterial> f = new DenseDoubleMatrix<>( residuals.toArray() );
        f.setRowNames( dmatrix.getMatrix().getRowNames() );
        f.setColumnNames( dmatrix.getMatrix().getColNames() );

        DoubleArrayList rowmeans = MatrixRowStats.means( sNamedMatrix );
        for ( int i = 0; i < f.rows(); i++ ) {
            double rowmean = rowmeans.get( i );
            for ( int j = 0; j < f.columns(); j++ ) {
                f.set( i, j, f.get( i, j ) + rowmean );
            }
        }
        return new ExpressionDataDoubleMatrix( dmatrix, f, dmatrix.getQuantitationTypes() );
    }
}


