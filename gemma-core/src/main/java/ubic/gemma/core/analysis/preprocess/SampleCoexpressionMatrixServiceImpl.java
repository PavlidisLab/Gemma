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
package ubic.gemma.core.analysis.preprocess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.math.MatrixStats;
import ubic.gemma.core.analysis.expression.diff.DiffExAnalyzer;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalysisConfig;
import ubic.gemma.core.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.core.analysis.preprocess.svd.SVDServiceHelper;
import ubic.gemma.core.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrixColumnSort;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Manage the "sample correlation/coexpression" matrices.
 *
 * @author paul
 */
@Component
public class SampleCoexpressionMatrixServiceImpl implements SampleCoexpressionMatrixService {

    private static final Logger log = LoggerFactory.getLogger( SampleCoexpressionMatrixServiceImpl.class );
    @Autowired
    private ExpressionDataMatrixService expressionDataMatrixService;
    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;
    @Autowired
    private SampleCoexpressionMatrixHelperService sampleCoexpressionMatrixHelperService;
    @Autowired
    private DiffExAnalyzer lma;
    @Autowired
    private SVDServiceHelper svdService;

    private static DoubleMatrix<BioAssay, BioAssay> getMatrix( ExpressionDataDoubleMatrix matrix ) {

        DoubleMatrix<BioMaterial, CompositeSequence> transposeR = matrix.getMatrix().transpose();

        DoubleMatrix<BioAssay, CompositeSequence> transpose = new DenseDoubleMatrix<>( transposeR.getRawMatrix() );
        transpose.setColumnNames( transposeR.getColNames() );
        for ( int i = 0; i < transpose.rows(); i++ ) {
            BioAssay s = transposeR.getRowName( i ).getBioAssaysUsedIn().iterator().next();
            transpose.setRowName( s, i );
        }

        return MatrixStats.correlationMatrix( transpose );
    }

    @Override
    public DoubleMatrix<BioAssay, BioAssay> findOrCreate( ExpressionExperiment ee ) {
        return this.findOrCreate( ee, true, true );
    }

    @Override
    public DoubleMatrix<BioAssay, BioAssay> findOrCreate( ExpressionExperiment ee, boolean useRegression,
            boolean removeOutliers ) {
        DoubleMatrix<BioAssay, BioAssay> mat = sampleCoexpressionMatrixHelperService.load( ee );

        if ( mat == null ) {
            SampleCoexpressionMatrixServiceImpl.log.info( "Computing sample coexpression" );
            return this.create( ee, useRegression, removeOutliers );
        }
        return mat;
    }

    @Override
    public boolean hasMatrix( ExpressionExperiment ee ) {
        return sampleCoexpressionMatrixHelperService.load( ee ) != null;
    }

    @Override
    public void delete( ExpressionExperiment ee ) {
        sampleCoexpressionMatrixHelperService.removeForExperiment( ee );
    }

    @Override
    public DoubleMatrix<BioAssay, BioAssay> create( ExpressionExperiment ee ) {
        return this.create( ee, true, true );
    }

    @Override
    public DoubleMatrix<BioAssay, BioAssay> create( ExpressionExperiment ee, boolean useRegression,
            boolean removeOutliers ) {

        // Load data and create matrix
        ExpressionDataDoubleMatrix mat = this.loadDataMatrix( ee, useRegression, this.loadVectors( ee ) );
        DoubleMatrix<BioAssay, BioAssay> cormat = this.loadCorMat( removeOutliers, mat );

        // Check consistency
        BioAssayDimension bestBioAssayDimension = mat.getBestBioAssayDimension();
        if ( cormat.rows() != bestBioAssayDimension.getBioAssays().size() ) {
            throw new IllegalStateException(
                    "Number of bioassays doesn't match length of the best bioAssayDimension. BAs in dimension: "
                            + bestBioAssayDimension.getBioAssays().size() + ", rows in cormat: " + cormat.rows() );
        }

        // Persist
        sampleCoexpressionMatrixHelperService.create( cormat, bestBioAssayDimension, mat.getExpressionExperiment() );
        return cormat;
    }

    private DoubleMatrix<BioAssay, BioAssay> reformat( DoubleMatrix<BioAssay, BioAssay> cormat ) {
        try {
            cormat = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( cormat );
            cormat = cormat.subsetRows( cormat.getColNames() ); // enforce same order on rows.
        } catch ( Exception e ) {
            SampleCoexpressionMatrixServiceImpl.log.error( "Could not reformat the sample correlation matrix! " );
            e.printStackTrace();
        }
        return cormat;
    }

    private DoubleMatrix<BioAssay, BioAssay> loadCorMat( boolean removeOutliers, ExpressionDataDoubleMatrix mat ) {
        DoubleMatrix<BioAssay, BioAssay> cormat = SampleCoexpressionMatrixServiceImpl.getMatrix( mat );
        if ( removeOutliers ) {
            SampleCoexpressionMatrixServiceImpl.log.info( "Processing cormat for outliers" );
            cormat = this.removeKnownOutliers( cormat );
        }
        return this.reformat( cormat );
    }

    private ExpressionDataDoubleMatrix loadDataMatrix( ExpressionExperiment ee, boolean useRegression,
            Collection<ProcessedExpressionDataVector> vectors ) {
        ExpressionDataDoubleMatrix mat;
        if ( useRegression ) {
            mat = this.loadFilteredMatrix( ee, vectors, false );
            if ( ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
                SampleCoexpressionMatrixServiceImpl.log
                        .error( "No experimental factors found! Can not regress major factors." );
            } else {
                mat = this.regressMajorFactors( ee, mat );
            }
        } else {
            mat = this.loadFilteredMatrix( ee, vectors, true );
        }
        return mat;
    }

    private Collection<ProcessedExpressionDataVector> loadVectors( ExpressionExperiment ee ) {
        Collection<ProcessedExpressionDataVector> vectors = processedExpressionDataVectorService
                .getProcessedDataVectors( ee );
        if ( vectors.isEmpty() ) {
            throw new IllegalArgumentException( "Must have processed vectors created first" );
        }
        return vectors;
    }

    private ExpressionDataDoubleMatrix loadFilteredMatrix( ExpressionExperiment ee,
            Collection<ProcessedExpressionDataVector> vectors, boolean requireSequences ) {
        FilterConfig fConfig = new FilterConfig();
        fConfig.setIgnoreMinimumRowsThreshold( true );
        fConfig.setIgnoreMinimumSampleThreshold( true );
        fConfig.setRequireSequences(
                requireSequences ); // not sure if this is the best thing to do. Some tests will fail.
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
        double importanceThreshold = 0.01;
        Set<ExperimentalFactor> importantFactors = svdService
                .getImportantFactors( ee, ee.getExperimentalDesign().getExperimentalFactors(), importanceThreshold );
        /* Remove 'batch' from important factors */
        ExperimentalFactor batch = null;
        for ( ExperimentalFactor factor : importantFactors ) {
            if ( factor.getName().toLowerCase().equals( "batch" ) )
                batch = factor;
        }
        if ( batch != null ) {
            importantFactors.remove( batch );
            SampleCoexpressionMatrixServiceImpl.log.info( "Removed 'batch' from the list of significant factors." );
        }
        if ( !importantFactors.isEmpty() ) {
            SampleCoexpressionMatrixServiceImpl.log.info( "Regressing out covariates" );
            DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
            config.setFactorsToInclude( importantFactors );
            mat = lma.regressionResiduals( mat, config, true );
        }
        return mat;
    }

    /**
     * Removes known outliers from the given correlation matrix.
     *
     * @param cormat the correlation matrix to strip the outliers from.
     * @return outlier-stripped correlation matrix.
     */
    private DoubleMatrix<BioAssay, BioAssay> removeKnownOutliers( DoubleMatrix<BioAssay, BioAssay> cormat ) {
        int col = 0;
        while ( col < cormat.columns() ) {
            if ( cormat.getColName( col ).getIsOutlier() ) {
                SampleCoexpressionMatrixServiceImpl.log.info( "Removing existing outlier " + cormat.getColName( col ) );
                List<BioAssay> colNames = this.getRemainingColumns( cormat, cormat.getColName( col ) );
                cormat = cormat.subsetRows( colNames );
                cormat = cormat.subsetColumns( colNames );
            } else
                col++; // increment only if sample is not an outlier so as not to skip columns
        }
        return cormat;
    }

    private List<BioAssay> getRemainingColumns( DoubleMatrix<BioAssay, BioAssay> cormat, BioAssay outlier ) {
        List<BioAssay> bas = new ArrayList<>();
        for ( int i = 0; i < cormat.columns(); i++ ) {
            if ( cormat.getColName( i ) != outlier )
                bas.add( cormat.getColName( i ) );
        }
        return bas;
    }
}
