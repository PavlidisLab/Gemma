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
package ubic.gemma.core.analysis.preprocess.batcheffects;

import cern.colt.matrix.DoubleMatrix2D;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrixImpl;
import ubic.basecode.math.MatrixStats;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.analysis.expression.diff.DiffExAnalyzerUtils;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrixColumnSort;
import ubic.gemma.core.visualization.ChartThemeUtils;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeUtils;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.core.analysis.expression.diff.DiffExAnalyzerUtils.buildDesignMatrix;
import static ubic.gemma.core.analysis.preprocess.batcheffects.BatchEffectUtils.getBatchEffectType;

/**
 * Methods for correcting batch effects.
 *
 * @author paul
 */
@Component
public class ExpressionExperimentBatchCorrectionServiceImpl implements ExpressionExperimentBatchCorrectionService {

    private static final Log log = LogFactory.getLog( ExpressionExperimentBatchCorrectionServiceImpl.class );


    // uris checked Aug 2024.
    public static final String COLLECTION_OF_MATERIAL_URI = "http://www.ebi.ac.uk/efo/EFO_0005066";
    public static final String DE_EXCLUDE_URI = "http://gemma.msl.ubc.ca/ont/TGEMO_00014";
    public static final String DE_INCLUDE_URI = "http://gemma.msl.ubc.ca/ont/TGEMO_00013";

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentBatchInformationService eeBatchService;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Override
    public boolean checkCorrectability( ExpressionExperiment ee ) {

        ExperimentalFactor batch = this.getBatchFactor( ee );
        if ( batch == null ) {
            ExpressionExperimentBatchCorrectionServiceImpl.log.info( "No batch factor found: " + ee );
            return false;
        }

        BatchEffectDetails details = eeBatchService.getBatchEffectDetails( ee );
        BatchEffectType bet = getBatchEffectType( details );
        if ( BatchEffectType.NO_BATCH_EFFECT_SUCCESS.equals( bet ) || BatchEffectType.SINGLE_BATCH_SUCCESS.equals( bet ) ) {
            ExpressionExperimentBatchCorrectionServiceImpl.log.info( "Experiment does not require batch correction as " +
                    "batch effect is negligible or it's a single batch: " + ee );
            return false;
        }

        if ( expressionExperimentService.getArrayDesignsUsed( ee ).size() > 1 ) {
            log.info( String.format( "%s cannot be batch-corrected: multiplatform; you must switch/merge first.", ee ) );
            return false;
        }

        String bConf = eeBatchService.getBatchConfoundAsHtmlString( ee );
        if ( bConf != null ) { // we used to let force override this, but that behavior is undesirable: if there is a confound, we don't batch correct
            ExpressionExperimentBatchCorrectionServiceImpl.log
                    .info( "Experiment cannot be batch corrected due to a confound: " + bConf );
            return false;
        }

        /*
         * Make sure we have at least two samples per batch. This generally won't happen if batches were defined by
         * Gemma.
         */
        Map<Long, Integer> batches = new HashMap<>();
        Set<BioMaterial> seen = new HashSet<>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            if ( seen.contains( bm ) )
                continue;
            seen.add( bm );
            for ( FactorValue fv : bm.getAllFactorValues() ) {
                if ( fv.getExperimentalFactor().equals( batch ) ) {
                    Long batchId = fv.getId();
                    if ( !batches.containsKey( batchId ) )
                        batches.put( batchId, 0 );
                    batches.put( batchId, batches.get( batchId ) + 1 );
                }
            }
        }
        for ( Long batchId : batches.keySet() ) {
            if ( batches.get( batchId ) < 2 ) {
                ExpressionExperimentBatchCorrectionServiceImpl.log
                        .info( "Batch with only one sample detected, correction not possible: " + ee + ", batchId="
                                + batchId );
                return false;
            }
        }

        /*
         * Extract data
         */
        Collection<ProcessedExpressionDataVector> vectors = processedExpressionDataVectorService
                .getProcessedDataVectorsAndThaw( ee );
        ExpressionDataDoubleMatrix mat = new ExpressionDataDoubleMatrix( ee, vectors );

        /*
        Get the experimental design matrix we would use for batch correction. If this is not full rank we can't proceed.
         */
        ObjectMatrix<BioMaterial, ExperimentalFactor, Object> design = this.getDesign( ee, mat );
        ObjectMatrix<BioMaterial, String, Object> designU = this.convertFactorValuesToStrings( design );
        try {
            new ComBat<>( designU ); // without data, just to check
        } catch ( ComBatException c ) { // probably because it's not full rank.
            log.info( c.getMessage() );
            return false;
        }
        return true;

    }

    @Override
    public ExpressionDataDoubleMatrix comBat( ExpressionExperiment ee ) {
        /*
         * is there a batch to use?
         */
        ExperimentalFactor batch = this.getBatchFactor( ee );
        if ( batch == null ) {
            ExpressionExperimentBatchCorrectionServiceImpl.log.warn( "No batch factor found" );
            return null;
        }

        /*
         * Extract data
         */
        Collection<ProcessedExpressionDataVector> vectors = processedExpressionDataVectorService
                .getProcessedDataVectorsAndThaw( ee );
        ExpressionDataDoubleMatrix mat = new ExpressionDataDoubleMatrix( ee, vectors );

        return this.comBat( ee, mat );
    }

    @Override
    public ExpressionDataDoubleMatrix comBat( ExpressionExperiment ee, ExpressionDataDoubleMatrix originalDataMatrix ) {
        /*
         * is there a batch to use?
         */
        ExperimentalFactor batch = this.getBatchFactor( ee );
        if ( batch == null ) {
            ExpressionExperimentBatchCorrectionServiceImpl.log.warn( "No batch factor found" );
            return null;
        }

        ExpressionDataDoubleMatrix finalMatrix = removeOutliers( originalDataMatrix );

        ObjectMatrix<BioMaterial, ExperimentalFactor, Object> design = this.getDesign( ee, finalMatrix );

        ExpressionDataDoubleMatrix correctedMatrix = this.doComBat( ee, finalMatrix, design );

        return correctedMatrix != null ? restoreOutliers( originalDataMatrix, correctedMatrix ) : null;
    }

    /**
     * Restore the outliers by basically overwriting the original matrix with the corrected values, leaving outlier samples as they were.
     * This is a lot easier than starting over with a new matrix.
     *
     * @return the originalDataMatrix with the corrected values now plugged in, or, if no outliers were present, the correctedMatrix because why not.s
     */
    private ExpressionDataDoubleMatrix restoreOutliers( ExpressionDataDoubleMatrix originalDataMatrix, ExpressionDataDoubleMatrix correctedMatrix ) {
        if ( originalDataMatrix.getBioAssayDimension().getBioAssays().size() == correctedMatrix.columns() ) {
            return correctedMatrix;
        }

        Set<Integer> outlierColumns = new HashSet<>();
        for ( int j = 0; j < originalDataMatrix.columns(); j++ ) {
            if ( originalDataMatrix.getBioAssayForColumn( j ).getIsOutlier() ) {
                outlierColumns.add( j );
            }
        }

        if ( outlierColumns.isEmpty() ) {
            throw new IllegalStateException( "Was expecting some outliers to be present since the corrected matrix is smaller than the original matrix" );
        }

        log.info( "Restoring " + outlierColumns.size() + " outlier columns" );

        /*
        Iterate over the rows and columns of the original matrix and copy the values from the corrected matrix.
        If the column is an outlier in the original matrix, just skip it.
         */
        DoubleMatrix<CompositeSequence, BioMaterial> dmatrix = originalDataMatrix.asDoubleMatrix();
        for ( int i = 0; i < dmatrix.rows(); i++ ) {
            int skip = 0;
            for ( int j = 0; j < dmatrix.columns(); j++ ) {
                if ( outlierColumns.contains( j ) ) {
                    skip++;
                    continue; // leave it alone; normally this will be an NaN.
                }
                dmatrix.set( i, j, correctedMatrix.getAsDouble( i, j - skip ) );
            }
        }

        return originalDataMatrix.withMatrix( dmatrix );
    }

    /**
     * Remove outlier samples from the data matrix, based on outliers that were flagged in the experiment (not just candidate outliers)
     *
     * @return the original matrix, or if outliers were present, a new matrix with the outliers removed
     */
    private ExpressionDataDoubleMatrix removeOutliers( ExpressionDataDoubleMatrix originalDataMatrix ) {
        List<BioMaterial> columnsToKeep = new ArrayList<>();
        for ( BioAssay ba : originalDataMatrix.getBioAssayDimension().getBioAssays() ) {
            if ( !ba.getIsOutlier() ) {
               /*
               Find the index of this bioassay in the originalDataMatrix
                */
                columnsToKeep.add( ba.getSampleUsed() );

            } else {
                log.info( "Dropping outlier sample: " + ba + " from batch correction" );
            }
        }
        ExpressionDataDoubleMatrix finalMatrix = originalDataMatrix;
        if ( columnsToKeep.size() < originalDataMatrix.columns() ) {
            finalMatrix = originalDataMatrix.sliceColumns( columnsToKeep, DiffExAnalyzerUtils.createBADMap( columnsToKeep ) );
        }
        return finalMatrix;
    }

    /**
     * For convenience of some testing classes
     *
     * @param ee the experiment to get the batch factor for
     * @return the batch factor of the experiment, or null, if experiment has no batch factor
     */
    @Nullable
    private ExperimentalFactor getBatchFactor( ExpressionExperiment ee ) {
        if ( ee.getExperimentalDesign() == null ) {
            log.warn( ee + " does not have an experimental design, cannot get the batch factor." );
            return null;
        }
        return ee.getExperimentalDesign().getExperimentalFactors().stream()
                .filter( ExperimentFactorUtils::isBatchFactor )
                .findFirst()
                .orElse( null );
    }

    /**
     * I really don't want ComBat to know about our expression APIs, so I redo the design without the ExperimentalFactor
     * type. But this is a bit stupid and causes other problems.
     */
    private ObjectMatrix<BioMaterial, String, Object> convertFactorValuesToStrings(
            ObjectMatrix<BioMaterial, ExperimentalFactor, Object> design ) {

        ObjectMatrix<BioMaterial, String, Object> designU = new ObjectMatrixImpl<>( design.rows(), design.columns() );
        designU.setRowNames( design.getRowNames() );
        List<String> colNames = new ArrayList<>();
        for ( int i = 0; i < design.rows(); i++ ) {
            for ( int j = 0; j < design.columns(); j++ ) {
                designU.set( i, j, design.get( i, j ) );
                if ( i == 0 ) {
                    // Address possibility of duplicate column names.
                    String colname = design.getColName( j ).getName();
                    if ( colNames.contains( colname ) ) {  // yes inefficient but this is a small matrix
                        colname = colname + "_" + j; // this is just to make sure we don't have duplicates so the addend doesn't matter
                    }
                    colNames.add( colname );
                }
            }
        }
        designU.setColumnNames( colNames );
        return designU;
    }

    @Nullable
    private ExpressionDataDoubleMatrix doComBat( ExpressionExperiment ee, ExpressionDataDoubleMatrix
                    originalDataMatrix,
            ObjectMatrix<BioMaterial, ExperimentalFactor, Object> design ) {
        ObjectMatrix<BioMaterial, String, Object> designU = this.convertFactorValuesToStrings( design );
        DoubleMatrix<CompositeSequence, BioMaterial> matrix = originalDataMatrix.asDoubleMatrix();

        designU = this.orderMatrix( matrix, designU );

        ScaleType scale = originalDataMatrix.getQuantitationTypes().iterator().next().getScale();

        boolean transformed = false;
        if ( !( scale.equals( ScaleType.LOG2 ) || scale.equals( ScaleType.LOG10 ) || scale
                .equals( ScaleType.LOGBASEUNKNOWN ) || scale.equals( ScaleType.LN ) ) ) {
            ExpressionExperimentBatchCorrectionServiceImpl.log.info( " *** COMBAT: LOG TRANSFORMING ***" );
            transformed = true;
            MatrixStats.logTransform( matrix );
        }

        /*
         * Process
         */
        ComBat<CompositeSequence, BioMaterial> comBat = new ComBat<>( matrix, designU );
        comBat.setChartTheme( ChartThemeUtils.getGemmaChartTheme( "Arial" ) );

        DoubleMatrix2D results;

        try {
            results = comBat.run( true ); // false: NONPARAMETRIC
        } catch ( ComBatException e ) {
            log.error( "ComBat on " + ee + " failed.", e );
            return null;
        }

        // note these plots always reflect the parametric setup.
        comBat.plot( ee.getId() + "." + FileTools.cleanForFileName( ee.getShortName() ) ); // TEMPORARY?

        /*
         * Postprocess. Results is a raw matrix/
         */
        DoubleMatrix<CompositeSequence, BioMaterial> correctedDataMatrix = new DenseDoubleMatrix<>( results.toArray() );
        correctedDataMatrix.setRowNames( matrix.getRowNames() );
        correctedDataMatrix.setColumnNames( matrix.getColNames() );

        if ( transformed ) {
            MatrixStats.unLogTransform( correctedDataMatrix );
        }

        /*
         * It is easier if we make a new quantitationtype.
         */
        Map<QuantitationType, QuantitationType> newQts = originalDataMatrix.getQuantitationTypes().stream()
                .collect( Collectors.toMap( qt -> qt, this::makeNewQuantitationType ) );
        ExpressionDataDoubleMatrix correctedExpressionDataMatrix = originalDataMatrix.withMatrix(
                correctedDataMatrix, newQts );

        // Sanity check...
        for ( int i = 0; i < correctedExpressionDataMatrix.columns(); i++ ) {
            assert correctedExpressionDataMatrix.getBioMaterialForColumn( i )
                    .equals( originalDataMatrix.getBioMaterialForColumn( i ) );
        }

        return correctedExpressionDataMatrix;
    }

    /**
     * Extract sample information, format into something ComBat can use.
     * <p>
     * Certain factors are removed at this stage, notably "DE_Exclude/Include" factors.
     *
     * @param mat only used to get sample ordering?
     * @return design matrix
     */
    private ObjectMatrix<BioMaterial, ExperimentalFactor, Object> getDesign( ExpressionExperiment ee,
            ExpressionDataDoubleMatrix mat ) {
        Assert.notNull( ee.getExperimentalDesign(), ee + " does not have an experimental design." );

        Collection<ExperimentalFactor> experimentalFactors = ee.getExperimentalDesign().getExperimentalFactors();

        /* remove experimental factors that are for DE_Exclude */
        List<ExperimentalFactor> retainedFactors = experimentalFactors.stream().filter( this::retainForBatchCorrection ).collect( Collectors.toList() );

        List<BioMaterial> biomaterials = new ArrayList<>();
        for ( int i = 0; i < mat.columns(); i++ ) {
            biomaterials.add( mat.getBioMaterialForColumn( i ) );
        }
        List<BioMaterial> orderedSamples = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( biomaterials, retainedFactors, null );
        return buildDesignMatrix( retainedFactors, orderedSamples, true );
    }

    /**
     * Test whether the experimental factor should be used for batch correction. If the factor is for DE_Exclude/Include, then it should not be used.
     *
     * @param ef experimental factor
     * @return true if the factor should be used in the model for batch correction
     */
    private boolean retainForBatchCorrection( ExperimentalFactor ef ) {
        if ( ef.getCategory() != null && COLLECTION_OF_MATERIAL_URI.equals( ef.getCategory().getCategoryUri() ) ) {
            for ( FactorValue fv : ef.getFactorValues() ) {
                for ( Characteristic c : fv.getCharacteristics() ) {
                    if ( c.getValueUri() != null && ( c.getValueUri().equals( DE_EXCLUDE_URI ) || c.getValueUri().equals( DE_INCLUDE_URI ) ) ) {
                        log.info( "Dropping factor " + ef.getName() + " from batch correction model because it is for DE_Exclude/Include" );
                        return false;
                    }
                }
            }
        }
        log.info( "Retaining factor " + ef.getName() + " for batch correction model" );
        return true;
    }

    private QuantitationType makeNewQuantitationType( QuantitationType oldQt ) {
        QuantitationType newQt = QuantitationType.Factory.newInstance( oldQt );
        QuantitationTypeUtils.appendToDescription( newQt, "Batch corrected with ComBat." );
        newQt.setIsBatchCorrected( true );
        return newQt;
    }

    /**
     * Reorder the design matrix so its rows are in the same order as the columns of the data matrix.
     *
     * @return updated designU
     */
    private ObjectMatrix<BioMaterial, String, Object> orderMatrix( DoubleMatrix<CompositeSequence, BioMaterial> matrix, ObjectMatrix<BioMaterial, String, Object> designU ) {
        ObjectMatrix<BioMaterial, String, Object> result = new ObjectMatrixImpl<>( designU.rows(), designU.columns() );
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
}
