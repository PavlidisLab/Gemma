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
package ubic.gemma.analysis.preprocess.batcheffects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cern.colt.matrix.DoubleMatrix2D;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrixImpl;
import ubic.basecode.math.MatrixStats;
import ubic.gemma.analysis.preprocess.svd.SVDService;
import ubic.gemma.analysis.preprocess.svd.SVDValueObject;
import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisService;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * Methods for correcting batch effects.
 * 
 * @author paul
 * @version $Id$
 */
@Service
public class ExpressionExperimentBatchCorrectionService {

    private static Log log = LogFactory.getLog( ExpressionExperimentBatchCorrectionService.class );

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    private PrincipalComponentAnalysisService principalComponentAnalysisService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private SVDService svdService;

    /**
     * @param ee
     */
    public void checkBatchEffectSeverity( ExpressionExperiment ee ) {
        ExperimentalFactor batch = getBatchFactor( ee );
        if ( batch == null ) {
            log.warn( "No batch factor found" );
            return;
        }

        if ( principalComponentAnalysisService.loadForExperiment( ee ) == null ) {
            svdService.svd( ee );
        }

        SVDValueObject svdFactorAnalysis = svdService.svdFactorAnalysis( ee );
        Double pc1rsq = Math.abs( svdFactorAnalysis.getFactorCorrelations().get( 0 ).get( batch.getId() ) );
        Double pc2rsq = Math.abs( svdFactorAnalysis.getFactorCorrelations().get( 1 ).get( batch.getId() ) );
        Double pc3rsq = Math.abs( svdFactorAnalysis.getFactorCorrelations().get( 2 ).get( batch.getId() ) );

        if ( pc1rsq > 0.4 || pc2rsq > 0.4 || pc3rsq > 0.4 ) {
            // ...
            log.info( "Batch effect detected: " + ee );
        } else {
            // ...
        }
    }

    /**
     * Is there a confound problem? Do we have at least two samples per batch?
     * 
     * @param ee
     */
    public boolean checkCorrectability( ExpressionExperiment ee ) {
        ExperimentalFactor batch = getBatchFactor( ee );
        if ( batch == null ) {
            log.warn( "No batch factor found" );
            return false;
        }

        Collection<BatchConfoundValueObject> test = BatchConfound.test( ee );

        for ( BatchConfoundValueObject batchConfoundValueObject : test ) {
            if ( batchConfoundValueObject.getP() < 0.01 ) {
                log.info( "Batch confound detected: " + ee );
                /*
                 * How bad is it ...
                 */
            }
        }

        /*
         * Make sure we have at least two samples per batch.
         */

        Map<Long, Integer> batches = new HashMap<Long, Integer>();
        Set<BioMaterial> seen = new HashSet<BioMaterial>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                if ( seen.contains( bm ) ) continue;
                seen.add( bm );
                for ( FactorValue fv : bm.getFactorValues() ) {
                    if ( fv.getExperimentalFactor().equals( batch ) ) {
                        Long batchId = fv.getId();
                        if ( !batches.containsKey( batchId ) ) batches.put( batchId, 0 );

                        batches.put( batchId, batches.get( batchId ) + 1 );

                    }
                }
            }
        }

        /*
         * TODO consider merging batches.
         */

        for ( Long batchId : batches.keySet() ) {
            if ( batches.get( batchId ) < 2 ) {
                return false;
            }
        }

        return true;

    }

    /**
     * @param ee
     * @return
     */
    public ExperimentalFactor getBatchFactor( ExpressionExperiment ee ) {

        ExperimentalFactor batch = null;
        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            if ( ExperimentalDesignUtils.isBatch( ef ) ) {
                batch = ef;
                break;
            }
        }
        return batch;
    }

    /**
     * @param ee
     * @return
     */
    public ExpressionDataDoubleMatrix comBat( ExpressionExperiment ee ) {
        /*
         * is there a batch to use?
         */
        ExperimentalFactor batch = getBatchFactor( ee );
        if ( batch == null ) {
            log.warn( "No batch factor found" );
            return null;
        }

        /*
         * Extract data
         */
        Collection<ProcessedExpressionDataVector> vectos = processedExpressionDataVectorService
                .getProcessedDataVectors( ee );
        ExpressionDataDoubleMatrix mat = new ExpressionDataDoubleMatrix( vectos );

        return comBat( mat );

    }

    /**
     * Run ComBat using default settings (parametric)
     * 
     * @param ee
     * @param mat
     * @return
     */
    public ExpressionDataDoubleMatrix comBat( ExpressionDataDoubleMatrix mat ) {
        return this.comBat( mat, true, null );
    }

    /**
     * @param ee
     * @param originalDataMatrix
     * @param parametric if false, the non-parametric (slow) ComBat estimation will be used.
     * @param importanceThreshold a p-value threshold used to select covariates. Covariates which are not associated
     *        with one of the first three principal components of the data at this level of significance will be removed
     *        from the correction model fitting.
     * @return corrected data.
     */
    public ExpressionDataDoubleMatrix comBat( ExpressionDataDoubleMatrix originalDataMatrix, boolean parametric,
            Double importanceThreshold ) {

        ExpressionExperiment ee = originalDataMatrix.getExpressionExperiment();

        ee = this.expressionExperimentService.thawLite( ee );

        /*
         * is there a batch to use?
         */
        ExperimentalFactor batch = getBatchFactor( ee );
        if ( batch == null ) {
            log.warn( "No batch factor found" );
            return null;
        }

        ObjectMatrix<BioMaterial, ExperimentalFactor, Object> design = getDesign( ee, originalDataMatrix,
                importanceThreshold );

        return doComBat( ee, originalDataMatrix, design, parametric );

    }

    /**
     * @param ee
     * @param originalDataMatrix
     * @param design
     * @param parametric
     * @return
     */
    private ExpressionDataDoubleMatrix doComBat( ExpressionExperiment ee,
            ExpressionDataDoubleMatrix originalDataMatrix,
            ObjectMatrix<BioMaterial, ExperimentalFactor, Object> design, boolean parametric ) {
        ObjectMatrix<BioMaterial, String, Object> designU = convertFactorValuesToStrings( design );
        DoubleMatrix<CompositeSequence, BioMaterial> matrix = originalDataMatrix.getMatrix();

        designU = orderMatrix( matrix, designU );

        ScaleType scale = originalDataMatrix.getQuantitationTypes().iterator().next().getScale();

        boolean transformed = false;
        if ( scale.equals( ScaleType.LOG2 ) || scale.equals( ScaleType.LOG10 )
                || scale.equals( ScaleType.LOGBASEUNKNOWN ) || scale.equals( ScaleType.LN ) ) {
            // ok, already on a log scale.
        } else {
            // log transform it.... hope for the best.
            log.info( " *** COMBAT: LOG TRANSFORMING ***" );
            transformed = true;
            MatrixStats.logTransform( matrix );
        }

        /*
         * Process
         */

        ComBat<CompositeSequence, BioMaterial> comBat = new ComBat<CompositeSequence, BioMaterial>( matrix, designU );

        DoubleMatrix2D results = comBat.run( parametric ); // false: NONPARAMETRIC

        // note these plots always reflect the parametric setup.
        comBat.plot( ee.getId() + "." + ee.getShortName().replaceAll( "[\\W\\s]+", "_" ) ); // TEMPORARY?

        /*
         * Postprocess. Results is a raw matrix/
         */
        DoubleMatrix<CompositeSequence, BioMaterial> correctedDataMatrix = new DenseDoubleMatrix<CompositeSequence, BioMaterial>(
                results.toArray() );
        correctedDataMatrix.setRowNames( matrix.getRowNames() );
        correctedDataMatrix.setColumnNames( matrix.getColNames() );

        if ( transformed ) {
            MatrixStats.unLogTransform( correctedDataMatrix );
        }

        ExpressionDataDoubleMatrix correctedExpressionDataMatrix = new ExpressionDataDoubleMatrix( originalDataMatrix,
                correctedDataMatrix );

        // Sanity check...
        for ( int i = 0; i < correctedExpressionDataMatrix.columns(); i++ ) {
            assert correctedExpressionDataMatrix.getBioMaterialForColumn( i ).equals(
                    originalDataMatrix.getBioMaterialForColumn( i ) );
        }

        return correctedExpressionDataMatrix;
    }

    /**
     * Reorder the design matrix so its rows are in the same order as the columns of the data matrix.
     * 
     * @param matrix
     * @param designU
     * @return updated designU
     */
    private ObjectMatrix<BioMaterial, String, Object> orderMatrix( DoubleMatrix<CompositeSequence, BioMaterial> matrix,
            ObjectMatrix<BioMaterial, String, Object> designU ) {

        ObjectMatrix<BioMaterial, String, Object> result = new ObjectMatrixImpl<BioMaterial, String, Object>(
                designU.rows(), designU.columns() );

        List<BioMaterial> rowNames = matrix.getColNames();

        for ( int j = 0; j < designU.columns(); j++ ) {
            for ( int i = 0; i < designU.rows(); i++ ) {
                result.set( i, j, designU.get( designU.getRowIndexByName( rowNames.get( i ) ), j ) );
            }
        }
        result.setRowNames( matrix.getColNames() );
        result.setColumnNames( designU.getColNames() );
        return result;

    }

    /**
     * I really don't want ComBat to know about our expression APIs, so I redo the design without the ExperimentalFactor
     * type. But this is a bit stupid and causes other problems.
     * 
     * @param design
     * @return
     */
    private ObjectMatrix<BioMaterial, String, Object> convertFactorValuesToStrings(
            ObjectMatrix<BioMaterial, ExperimentalFactor, Object> design ) {

        ObjectMatrix<BioMaterial, String, Object> designU = new ObjectMatrixImpl<BioMaterial, String, Object>(
                design.rows(), design.columns() );
        designU.setRowNames( design.getRowNames() );
        List<String> colNames = new ArrayList<String>();
        for ( int i = 0; i < design.rows(); i++ ) {
            for ( int j = 0; j < design.columns(); j++ ) {
                designU.set( i, j, design.get( i, j ) );
                if ( i == 0 ) {
                    // WARNING we _can_ have duplicates.
                    colNames.add( design.getColName( j ).getName() );
                }
            }
        }
        designU.setColumnNames( colNames );
        return designU;
    }

    /**
     * Extract sample information, format into something ComBat can use. Which covariates should we use??
     * 
     * @param ee
     * @param mat
     * @param importanceThreshold
     * @return
     */
    private ObjectMatrix<BioMaterial, ExperimentalFactor, Object> getDesign( ExpressionExperiment ee,
            ExpressionDataDoubleMatrix mat, Double importanceThreshold ) {

        List<ExperimentalFactor> factors = new ArrayList<ExperimentalFactor>();

        Collection<ExperimentalFactor> experimentalFactors = ee.getExperimentalDesign().getExperimentalFactors();

        if ( importanceThreshold != null ) {
            Set<ExperimentalFactor> importantFactors = svdService.getImportantFactors( ee, experimentalFactors,
                    importanceThreshold );
            importantFactors.remove( getBatchFactor( ee ) );

            log.info( importantFactors.size() + " covariates out of " + ( experimentalFactors.size() - 1 )
                    + " considered important to include in batch correction" );
            ExperimentalFactor batch = getBatchFactor( ee );
            factors.add( batch );
            factors.addAll( importantFactors );
        } else {
            factors.addAll( experimentalFactors );
        }

        List<BioMaterial> orderedSamples = ExperimentalDesignUtils.getOrderedSamples( mat, factors );
        ObjectMatrix<BioMaterial, ExperimentalFactor, Object> design = ExperimentalDesignUtils.sampleInfoMatrix(
                factors, orderedSamples, ExperimentalDesignUtils.getBaselineConditions( orderedSamples, factors ) );

        return design;
    }

}
