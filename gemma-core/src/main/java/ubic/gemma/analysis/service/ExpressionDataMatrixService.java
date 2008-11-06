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
package ubic.gemma.analysis.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.gemma.analysis.preprocess.filter.ExpressionExperimentFilter;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;

/**
 * Tools for easily getting data matrices for analysis in a consistent way.
 * 
 * @spring.bean id="expressionDataMatrixService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="processedExpressionDataVectorService" ref="processedExpressionDataVectorService"
 * @spring.property name="dedvService" ref="designElementDataVectorService"
 * @author keshav
 * @version $Id$
 */
public class ExpressionDataMatrixService {

    ExpressionExperimentService expressionExperimentService;

    ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    DesignElementDataVectorService dedvService;

    /**
     * Provide a filtered expression data matrix.
     * 
     * @param ee
     * @param filterConfig
     * @param dataVectors
     * @return
     */
    public ExpressionDataDoubleMatrix getFilteredMatrix( ExpressionExperiment ee, FilterConfig filterConfig,
            Collection<ProcessedExpressionDataVector> dataVectors ) {
        Collection<ArrayDesign> arrayDesignsUsed = expressionExperimentService.getArrayDesignsUsed( ee );
        ExpressionExperimentFilter filter = new ExpressionExperimentFilter( ee, arrayDesignsUsed, filterConfig );
        ExpressionDataDoubleMatrix eeDoubleMatrix = filter.getFilteredMatrix( dataVectors );
        return eeDoubleMatrix;
    }

    /**
     * Provide a filtered expression data matrix.
     * 
     * @param ee
     * @param filterConfig
     * @return
     */
    public ExpressionDataDoubleMatrix getFilteredMatrix( ExpressionExperiment ee, FilterConfig filterConfig ) {
        Collection<ProcessedExpressionDataVector> dataVectors = processedExpressionDataVectorService
                .getProcessedDataVectors( ee );
        return this.getFilteredMatrix( ee, filterConfig, dataVectors );
    }

    /**
     * @param ee
     * @return matrix of preferred data, with all missing values masked. If the ProcessedExpressionDataVectors are
     *         missing, this will throw an exception.
     */
    public ExpressionDataDoubleMatrix getProcessedExpressionDataMatrix( ExpressionExperiment ee ) {
        Collection<ProcessedExpressionDataVector> dataVectors = this.processedExpressionDataVectorService
                .getProcessedDataVectors( ee );
        if ( dataVectors.isEmpty() ) {
            throw new IllegalArgumentException( "There are no ProcessedExpressionDataVectors for " + ee
                    + ", they must be created first" );
        }
        return new ExpressionDataDoubleMatrix( dataVectors );
    }

    /**
     * @param ee
     * @return
     */
    public Collection<ProcessedExpressionDataVector> getProcessedExpressionDataVectors( ExpressionExperiment ee ) {
        Collection<ProcessedExpressionDataVector> dataVectors = this.processedExpressionDataVectorService
                .getProcessedDataVectors( ee ); // these are already thawed.
        return dataVectors;
    }

    @SuppressWarnings("unchecked")
    public DenseDoubleMatrix getRankMatrix( Collection<Gene> genes, Collection<ExpressionExperiment> ees,
            ProcessedExpressionDataVectorDao.RankMethod method ) {
        DenseDoubleMatrix<Gene, ExpressionExperiment> matrix = new DenseDoubleMatrix( genes.size(), ees.size() );

        Map<ExpressionExperiment, Map<Gene, Collection<Double>>> ranks = processedExpressionDataVectorService.getRanks(
                ees, genes, method );

        matrix.setRowNames( new ArrayList<Gene>( genes ) );
        matrix.setColumnNames( new ArrayList<ExpressionExperiment>( ees ) );
        for ( int i = 0; i < matrix.rows(); i++ ) {
            for ( int j = 0; j < matrix.columns(); j++ ) {
                matrix.setByKeys( matrix.getRowName( i ), matrix.getColName( j ), Double.NaN );
            }
        }

        for ( Gene g : matrix.getRowNames() ) {
            for ( ExpressionExperiment e : matrix.getColNames() ) {
                if ( ranks.containsKey( e ) ) {
                    Collection<Double> r = ranks.get( e ).get( g );

                    // compute median of collection.
                    Double[] ar = new Double[r.size()];
                    r.toArray( ar );
                    double[] dar = ArrayUtils.toPrimitive( ar );
                    double medianRank = Descriptive.median( new DoubleArrayList( dar ) );

                    matrix.setByKeys( g, e, medianRank );
                }
            }
        }

        return matrix;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setProcessedExpressionDataVectorService( ProcessedExpressionDataVectorService vectorService ) {
        this.processedExpressionDataVectorService = vectorService;
    }

    public void setDedvService( DesignElementDataVectorService dedvService ) {
        this.dedvService = dedvService;
    }

}
