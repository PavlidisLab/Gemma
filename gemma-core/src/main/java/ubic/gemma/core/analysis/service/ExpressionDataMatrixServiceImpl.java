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
import ubic.gemma.core.analysis.preprocess.filter.ExpressionExperimentFilter;
import ubic.gemma.core.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorDao;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.*;

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

    @Override
    @Transactional(readOnly = true)
    public ExpressionDataDoubleMatrix getFilteredMatrix( ExpressionExperiment ee, FilterConfig filterConfig ) throws FilteringException {
        Collection<ProcessedExpressionDataVector> dataVectors = processedExpressionDataVectorService
                .getProcessedDataVectors( ee );
        if ( dataVectors.isEmpty() ) {
            throw new IllegalStateException( "There are no processed vectors for " + ee + ", they must be created first." );
        }
        return this.getFilteredMatrix( ee, filterConfig, dataVectors );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionDataDoubleMatrix getFilteredMatrix( ExpressionExperiment ee, FilterConfig filterConfig,
            Collection<ProcessedExpressionDataVector> dataVectors ) throws FilteringException {
        Collection<ArrayDesign> arrayDesignsUsed = expressionExperimentService.getArrayDesignsUsed( ee );
        return this.getFilteredMatrix( filterConfig, dataVectors, arrayDesignsUsed );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionDataDoubleMatrix getFilteredMatrix( String arrayDesignName, FilterConfig filterConfig,
            Collection<ProcessedExpressionDataVector> dataVectors ) throws FilteringException {
        ArrayDesign ad = arrayDesignService.findByShortName( arrayDesignName );
        if ( ad == null ) {
            throw new IllegalArgumentException( "No platform named '" + arrayDesignName + "'" );
        }
        Collection<ArrayDesign> arrayDesignsUsed = new HashSet<>();
        arrayDesignsUsed.add( ad );
        return this.getFilteredMatrix( filterConfig, dataVectors, arrayDesignsUsed );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionDataDoubleMatrix getProcessedExpressionDataMatrix( ExpressionExperiment ee ) {
        Collection<ProcessedExpressionDataVector> dataVectors = expressionExperimentService.getProcessedDataVectors( ee )
                .orElseThrow( () -> new IllegalStateException( "There are no processed vectors for " + ee + ", they must be created first." ) );
        return new ExpressionDataDoubleMatrix( dataVectors );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionDataDoubleMatrix getProcessedExpressionDataMatrix( ExpressionExperiment ee, List<BioAssay> samples ) {
        Collection<ProcessedExpressionDataVector> dataVectors = expressionExperimentService.getProcessedDataVectors( ee, samples )
                .orElseThrow( () -> new IllegalStateException( "There are no processed vectors for " + ee + ", they must be created first." ) );
        return new ExpressionDataDoubleMatrix( dataVectors );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionDataDoubleMatrix getRawExpressionDataMatrix( ExpressionExperiment ee, QuantitationType quantitationType ) {
        Collection<RawExpressionDataVector> vectors = expressionExperimentService.getRawDataVectors( ee, quantitationType );
        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( ee + " does not have any raw data vectors for " + quantitationType + "." );
        }
        return new ExpressionDataDoubleMatrix( vectors );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionDataDoubleMatrix getRawExpressionDataMatrix( ExpressionExperiment ee, List<BioAssay> samples, QuantitationType quantitationType ) {
        Collection<RawExpressionDataVector> vectors = expressionExperimentService.getRawDataVectors( ee, samples, quantitationType );
        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( ee + " does not have any raw data vectors for " + quantitationType + "." );
        }
        return new ExpressionDataDoubleMatrix( vectors );
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

    private ExpressionDataDoubleMatrix getFilteredMatrix( FilterConfig filterConfig,
            Collection<ProcessedExpressionDataVector> dataVectors, Collection<ArrayDesign> arrayDesignsUsed ) throws FilteringException {
        if ( dataVectors.isEmpty() )
            throw new IllegalArgumentException( "Vectors must be provided" );
        ExpressionExperimentFilter filter = new ExpressionExperimentFilter( arrayDesignsUsed, filterConfig );
        dataVectors = this.processedExpressionDataVectorService.thaw( dataVectors );
        return filter.getFilteredMatrix( dataVectors );
    }
}
