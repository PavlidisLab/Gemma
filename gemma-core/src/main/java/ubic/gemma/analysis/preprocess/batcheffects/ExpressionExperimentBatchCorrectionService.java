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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cern.colt.matrix.DoubleMatrix2D;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrixImpl;
import ubic.gemma.analysis.preprocess.svd.SVDService;
import ubic.gemma.analysis.preprocess.svd.SVDValueObject;
import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisService;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

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
     * Is there a confound problem?
     * 
     * @param ee
     */
    public void checkCorrectability( ExpressionExperiment ee ) {
        ExperimentalFactor batch = getBatchFactor( ee );
        if ( batch == null ) {
            log.warn( "No batch factor found" );
            return;
        }

        Collection<BatchConfoundValueObject> test = BatchConfound.test( ee );
        for ( BatchConfoundValueObject batchConfoundValueObject : test ) {
            if ( batchConfoundValueObject.getP() < 0.01 ) {
                // ...
                log.info( "Batch confound detected: " + ee );
            }
        }
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

        ObjectMatrix<BioMaterial, ExperimentalFactor, Object> design = getDesign( ee, mat );

        ObjectMatrix<BioMaterial, String, Object> designU = convertFactorValuesToStrings( design );

        /*
         * Process
         */
        ComBat<CompositeSequence, BioMaterial> comBat = new ComBat<CompositeSequence, BioMaterial>( mat.getMatrix(),
                designU );

        DoubleMatrix2D results = comBat.run();

        /*
         * Postprocess. Results is a raw matrix/
         */
        DoubleMatrix<CompositeSequence, BioMaterial> resultsM = new DenseDoubleMatrix<CompositeSequence, BioMaterial>(
                results.toArray() );
        resultsM.setRowNames( mat.getMatrix().getRowNames() );
        resultsM.setColumnNames( mat.getMatrix().getColNames() ); 

        return new ExpressionDataDoubleMatrix( mat, resultsM );

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

        ObjectMatrix<BioMaterial, String, Object> designU = new ObjectMatrixImpl<BioMaterial, String, Object>( design
                .rows(), design.columns() );
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
     * @return
     */
    private ObjectMatrix<BioMaterial, ExperimentalFactor, Object> getDesign( ExpressionExperiment ee,
            ExpressionDataDoubleMatrix mat ) {

        List<ExperimentalFactor> factors = new ArrayList<ExperimentalFactor>();
        factors.addAll( ee.getExperimentalDesign().getExperimentalFactors() );
        List<BioMaterial> orderedSamples = ExperimentalDesignUtils.getOrderedSamples( mat, factors );
        ObjectMatrix<BioMaterial, ExperimentalFactor, Object> design = ExperimentalDesignUtils.sampleInfoMatrix(
                factors, orderedSamples, ExperimentalDesignUtils.getBaselineConditions( orderedSamples, factors ) );
        return design;
    }

}
