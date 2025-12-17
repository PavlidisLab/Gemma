/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.core.analysis.service;

import cern.colt.list.DoubleArrayList;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.MatrixStats;
import ubic.gemma.core.analysis.preprocess.VectorMergingService;
import ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionUtils;
import ubic.gemma.core.analysis.preprocess.filter.*;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeUtils;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorDao;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Thaws;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tools for easily getting data matrices for analysis in a consistent way.
 *
 * @author keshav
 */
@Component
@CommonsLog
public class ExpressionDataMatrixServiceImpl implements ExpressionDataMatrixService {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private VectorMergingService vectorMergingService;

    @Override
    @Transactional(readOnly = true)
    public ExpressionDataDoubleMatrix getFilteredMatrix( ExpressionExperiment ee, ExpressionExperimentFilterConfig filterConfig ) throws FilteringException {
        Collection<ProcessedExpressionDataVector> dataVectors = processedExpressionDataVectorService
                .getProcessedDataVectors( ee );
        if ( dataVectors.isEmpty() ) {
            throw new IllegalStateException( "There are no processed vectors for " + ee + ", they must be created first." );
        }
        return this.getFilteredMatrix( ee, dataVectors, filterConfig, false );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionDataDoubleMatrix getFilteredMatrix( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> dataVectors, ExpressionExperimentFilterConfig filterConfig, boolean logTransform ) throws FilteringException {
        Collection<ArrayDesign> arrayDesignsUsed = expressionExperimentService.getArrayDesignsUsed( ee );
        return this.getFilteredMatrix( ee, dataVectors, arrayDesignsUsed, filterConfig, logTransform );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionDataDoubleMatrix getFilteredMatrix( Collection<ProcessedExpressionDataVector> dataVectors, ArrayDesign arrayDesign, ExpressionExperimentFilterConfig filterConfig,
            boolean logTransform ) throws FilteringException {
        return this.getFilteredMatrix( null, dataVectors, Collections.singleton( arrayDesign ), filterConfig, logTransform );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionDataDoubleMatrix getProcessedExpressionDataMatrix( ExpressionExperiment ee ) {
        return getProcessedExpressionDataMatrix( ee, false );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionDataDoubleMatrix getProcessedExpressionDataMatrix( ExpressionExperiment ee, boolean thawAssays ) {
        Collection<ProcessedExpressionDataVector> dataVectors = expressionExperimentService.getProcessedDataVectors( ee )
                .orElseThrow( () -> new IllegalStateException( "There are no processed vectors for " + ee + ", they must be created first." ) );
        if ( thawAssays ) {
            dataVectors.stream()
                    .map( ProcessedExpressionDataVector::getBioAssayDimension )
                    .distinct()
                    .forEach( Thaws::thawBioAssayDimension );
        }
        return new ExpressionDataDoubleMatrix( ee, dataVectors );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionDataDoubleMatrix getProcessedExpressionDataMatrix( ExpressionExperiment ee, List<BioAssay> samples ) {
        Collection<ProcessedExpressionDataVector> dataVectors = expressionExperimentService.getProcessedDataVectors( ee, samples )
                .orElseThrow( () -> new IllegalStateException( "There are no processed vectors for " + ee + ", they must be created first." ) );
        return new ExpressionDataDoubleMatrix( ee, dataVectors );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionDataDoubleMatrix getRawExpressionDataMatrix( ExpressionExperiment ee, QuantitationType quantitationType ) {
        Collection<RawExpressionDataVector> vectors = expressionExperimentService.getRawDataVectors( ee, quantitationType );
        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( ee + " does not have any raw data vectors for " + quantitationType + "." );
        }
        return new ExpressionDataDoubleMatrix( ee, vectors );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionDataDoubleMatrix getRawExpressionDataMatrix( ExpressionExperiment ee, List<BioAssay> samples, QuantitationType quantitationType ) {
        Collection<RawExpressionDataVector> vectors = expressionExperimentService.getRawDataVectors( ee, samples, quantitationType );
        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( ee + " does not have any raw data vectors for " + quantitationType + "." );
        }
        return new ExpressionDataDoubleMatrix( ee, vectors );
    }

    @Override
    @Transactional(readOnly = true)
    public DoubleMatrix<Gene, ExpressionExperiment> getRankMatrix( Collection<Gene> genes,
            Collection<ExpressionExperiment> ees, ProcessedExpressionDataVectorDao.RankMethod method ) {

        DoubleMatrix<Gene, ExpressionExperiment> matrix = new DenseDoubleMatrix<>( genes.size(), ees.size() );

        Map<ExpressionExperiment, Map<Gene, Collection<Double>>> ranks = processedExpressionDataVectorService
                .getRanks( ees, genes, method );

        matrix.setRowNames( new ArrayList<>( genes ) );
        matrix.setColumnNames( new ArrayList<>( ees ) );
        for ( int i = 0; i < matrix.rows(); i++ ) {
            for ( int j = 0; j < matrix.columns(); j++ ) {
                matrix.setByKeys( matrix.getRowName( i ), matrix.getColName( j ), Double.NaN );
            }
        }

        for ( Gene g : matrix.getRowNames() ) {
            for ( ExpressionExperiment e : matrix.getColNames() ) {
                if ( ranks.containsKey( e ) ) {
                    Collection<Double> r = ranks.get( e ).get( g );

                    if ( r == null ) {
                        continue;
                    }

                    Double[] ar = r.toArray( new Double[0] );

                    // compute median of collection.
                    double[] dar = ArrayUtils.toPrimitive( ar );
                    double medianRank = DescriptiveWithMissing.median( new DoubleArrayList( dar ) );
                    matrix.setByKeys( g, e, medianRank );

                }
            }
        }

        return matrix;
    }

    /**
     * Provides a ready-to-use expression data matrix that is transformed and filtered. The processes that are applied,
     * in this order:
     * <ol>
     * <li>Log transform, if requested and not already done
     * <li>Use the missing value data to mask the preferred data (ratiometric data only)
     * <li>Remove rows that don't have biosequences (always applied)
     * <li>Remove Affymetrix control probes (Affymetrix only)
     * <li>Remove rows that have too many missing values (as configured)
     * <li>Remove rows with low variance (ratiometric) or CV (one-color) (as configured)
     * <li>Remove rows with very high or low expression (as configured)
     * </ol>
     *
     * @param dataVectors  data vectors
     * @param logTransform whether to log-transform the data if not already done
     * @return filtered matrix
     * @throws NoDesignElementsException if filtering results in no row left in the expression matrix
     */
    private ExpressionDataDoubleMatrix getFilteredMatrix( @Nullable ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> dataVectors,
            Collection<ArrayDesign> arrayDesignsUsed, ExpressionExperimentFilterConfig filterConfig, boolean logTransform
    ) throws FilteringException {
        if ( dataVectors.isEmpty() )
            throw new IllegalArgumentException( "Vectors must be provided" );
        dataVectors = this.processedExpressionDataVectorService.thaw( dataVectors );
        ExpressionDataDoubleMatrix eeDoubleMatrix = new ExpressionDataDoubleMatrix( ee, dataVectors );
        if ( logTransform ) {
            eeDoubleMatrix = logTransform( eeDoubleMatrix, arrayDesignsUsed );
        }
        return new ExpressionExperimentFilter( filterConfig ).filter( eeDoubleMatrix, arrayDesignsUsed, new ExpressionExperimentFilterResult() );
    }

    /**
     * Transform the data as configured to a log scale (e.g., take log2) -- which could mean no action
     * <p>
     * TODO: move that logic to {@link ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionUtils}
     *       and {@link ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionUtils} for the parts that
     *       detect log-transformed data and two-color arrays
     */
    private ExpressionDataDoubleMatrix logTransform( ExpressionDataDoubleMatrix datamatrix, Collection<ArrayDesign> arrayDesignsUsed ) {
        boolean alreadyLogged = this.isLogTransformed( datamatrix, arrayDesignsUsed );
        if ( alreadyLogged ) {
            return datamatrix;
        }
        DoubleMatrix<CompositeSequence, BioMaterial> matrix = datamatrix.asDoubleMatrix();
        // this is a log2
        MatrixStats.logTransform( matrix );
        Map<QuantitationType, QuantitationType> qts = datamatrix.getQuantitationTypes().stream()
                .collect( Collectors.toMap( qt -> qt, qt -> {
                    qt = QuantitationType.Factory.newInstance( qt );
                    qt.setScale( ScaleType.LOG2 );
                    return qt;
                } ) );
        return datamatrix.withMatrix( matrix, qts );
    }

    /**
     * @param eeDoubleMatrix the matrix
     * @return true if the data looks like it is already log transformed, false otherwise. This is based on the
     * quantitation types and, as a check, looking at the data itself.
     */
    private boolean isLogTransformed( ExpressionDataDoubleMatrix eeDoubleMatrix, Collection<ArrayDesign> arrayDesignsUsed ) {
        Collection<QuantitationType> quantitationTypes = eeDoubleMatrix.getQuantitationTypes();
        for ( QuantitationType qt : quantitationTypes ) {
            if ( QuantitationTypeUtils.isLogTransformed( qt ) ) {
                log.info( "Quantitation type says the data is already log transformed" );
                return true;
            }
        }

        // assume based on the platform
        if ( isTwoColor( arrayDesignsUsed ) ) {
            log.info( "Data is from a two-color array, assuming it is log transformed" );
            return true;
        }

        // detect based on the data itself
        QuantitationType inferredQt = QuantitationTypeDetectionUtils.inferQuantitationType( eeDoubleMatrix );
        if ( QuantitationTypeUtils.isLogTransformed( inferredQt ) ) {
            log.info( "Data looks log-transformed, but not sure...assuming it is" );
            return true;
        } else {
            log.info( "Data has large values, doesn't look log transformed" );
            return false;
        }
    }

    /**
     * Determine if the expression experiment uses two-color arrays.
     *
     * @throws UnsupportedOperationException if the ee uses both two color and one-color technologies.
     */
    private boolean isTwoColor( Collection<ArrayDesign> arrayDesignsUsed ) {
        if ( arrayDesignsUsed.isEmpty() ) {
            throw new IllegalStateException();
        }
        Boolean answer = null;
        for ( ArrayDesign arrayDesign : arrayDesignsUsed ) {
            TechnologyType techType = arrayDesign.getTechnologyType();
            boolean isTwoC = techType.equals( TechnologyType.TWOCOLOR ) || techType.equals( TechnologyType.DUALMODE );
            if ( answer != null && !answer.equals( isTwoC ) ) {
                throw new UnsupportedOperationException(
                        "Gemma cannot handle experiments that mix one- and two-color arrays" );
            }
            answer = isTwoC;
        }
        return answer;
    }
}
