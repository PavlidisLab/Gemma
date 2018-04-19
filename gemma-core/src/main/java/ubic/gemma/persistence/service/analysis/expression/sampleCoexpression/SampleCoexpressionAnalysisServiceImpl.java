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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.math.MatrixStats;
import ubic.gemma.core.analysis.expression.diff.DiffExAnalyzer;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalysisConfig;
import ubic.gemma.core.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.core.analysis.preprocess.svd.SVDServiceHelper;
import ubic.gemma.core.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrixColumnSort;
import ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionMatrix;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;

import java.util.Collection;
import java.util.List;
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
    private static final String MSG_ERR_NO_DESIGN = "No experimental factors found! Can not regress major factors.";
    private static final String MSG_ERR_REFORMAT = "Could not reformat the sample correlation matrix!";
    private static final String MSG_ERR_BIOASSAY_MISMATCH = "Number of bioassays doesn't match length of the best bioAssayDimension. BAs in dimension: %d, rows in cormat: %d";
    private static final String MSG_ERR_NO_BAS_IN_BAD = "No bioassays in the bioAssayDimension id:%d";
    private static final String MSG_INFO_RUNNING_SCM = "Sample Correlations not calculated for ee %d yet, running them now.";
    private static final String MSG_INFO_COMPUTING_SCM = "Computing sample coexpression matrix for ee %d, regressing: %s";
    private static final String MSG_INFO_REGRESSING = "Regressing out covariates";
    private static final String MSG_INFO_BATCH_REMOVED = "Removed 'batch' from the list of significant factors.";
    private static final double IMPORTANCE_THRESHOLD = 0.01;

    @Autowired
    private ExpressionDataMatrixService expressionDataMatrixService;
    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;
    @Autowired
    private SampleCoexpressionAnalysisDao sampleCoexpressionAnalysisDao;
    @Autowired
    private DiffExAnalyzer lma;
    @Autowired
    private SVDServiceHelper svdService;

    @Override
    public DoubleMatrix<BioAssay, BioAssay> loadFullMatrix( ExpressionExperiment ee ) {
        return this.toDoubleMatrix( this.load( ee ).getFullCoexpressionMatrix() );
    }

    @Override
    public DoubleMatrix<BioAssay, BioAssay> loadRegressedMatrix( ExpressionExperiment ee ) {
        return this.toDoubleMatrix( this.load( ee ).getRegressedCoexpressionMatrix() );
    }

    @Override
    public SampleCoexpressionAnalysis load( ExpressionExperiment ee ) {
        SampleCoexpressionAnalysis mat = sampleCoexpressionAnalysisDao.load( ee );

        if ( mat == null || mat.getFullCoexpressionMatrix() == null || mat.getRegressedCoexpressionMatrix() == null ) {
            SampleCoexpressionAnalysisServiceImpl.log
                    .info( String.format( SampleCoexpressionAnalysisServiceImpl.MSG_INFO_RUNNING_SCM, ee.getId() ) );
            return this.compute( ee );
        }
        return mat;
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
        SampleCoexpressionAnalysis analysis = new SampleCoexpressionAnalysis( ee, // Analyzed experiment
                this.getMatrix( ee, false ), // Full
                this.getMatrix( ee, true ) );// Regressed

        // Persist
        return sampleCoexpressionAnalysisDao.create( analysis );
    }

    @Override
    public void removeForExperiment( ExpressionExperiment ee ) {
        this.sampleCoexpressionAnalysisDao.removeForExperiment( ee );
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
            return result;
        } catch ( IllegalArgumentException e ) {
            SampleCoexpressionAnalysisServiceImpl.log.error( e.getMessage() );
            e.printStackTrace();
            return null;
        }
    }

    private SampleCoexpressionMatrix getMatrix( ExpressionExperiment ee, boolean regress ) {
        SampleCoexpressionAnalysisServiceImpl.log.info( String
                .format( SampleCoexpressionAnalysisServiceImpl.MSG_INFO_COMPUTING_SCM, ee.getId(), regress ) );

        ExpressionDataDoubleMatrix mat = this
                .loadDataMatrix( ee, regress, processedExpressionDataVectorService.getProcessedDataVectors( ee ) );
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

        return this.reformatCorMat( MatrixStats.correlationMatrix( transpose ) );
    }

    private DoubleMatrix<BioAssay, BioAssay> reformatCorMat( DoubleMatrix<BioAssay, BioAssay> cormat ) {
        try {
            cormat = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( cormat );
            cormat = cormat.subsetRows( cormat.getColNames() ); // enforce same order on rows.
        } catch ( Exception e ) {
            SampleCoexpressionAnalysisServiceImpl.log.error( SampleCoexpressionAnalysisServiceImpl.MSG_ERR_REFORMAT );
            e.printStackTrace();
        }
        return cormat;
    }

    private ExpressionDataDoubleMatrix loadDataMatrix( ExpressionExperiment ee, boolean useRegression,
            Collection<ProcessedExpressionDataVector> vectors ) {
        ExpressionDataDoubleMatrix mat;
        if ( vectors == null || vectors.isEmpty() ) {
            SampleCoexpressionAnalysisServiceImpl.log.error( SampleCoexpressionAnalysisServiceImpl.MSG_ERR_NO_VECTORS );
        }
        if ( useRegression ) {
            mat = this.loadFilteredDataMatrix( ee, vectors, false );
            if ( ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
                SampleCoexpressionAnalysisServiceImpl.log
                        .error( SampleCoexpressionAnalysisServiceImpl.MSG_ERR_NO_DESIGN );
                return null;
            } else {
                mat = this.regressMajorFactors( ee, mat );
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
     * @param ee  the experiment to load the factors from
     * @param mat the double matrix of processed vectors to regress
     * @return regressed double matrix
     */
    private ExpressionDataDoubleMatrix regressMajorFactors( ExpressionExperiment ee, ExpressionDataDoubleMatrix mat ) {
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
        if ( !importantFactors.isEmpty() ) {
            SampleCoexpressionAnalysisServiceImpl.log.info( SampleCoexpressionAnalysisServiceImpl.MSG_INFO_REGRESSING );
            DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
            config.setFactorsToInclude( importantFactors );
            mat = lma.regressionResiduals( mat, config, true );
        }
        return mat;
    }
}
