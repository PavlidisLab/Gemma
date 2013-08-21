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
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.gemma.analysis.preprocess.filter.ExpressionExperimentFilter;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import cern.colt.list.DoubleArrayList;

/**
 * Tools for easily getting data matrices for analysis in a consistent way.
 * 
 * @author keshav
 * @version $Id$
 */
@Component
public class ExpressionDataMatrixServiceImpl implements ExpressionDataMatrixService {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.service.ExpressionDataMatrixService#getFilteredMatrix(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment, ubic.gemma.analysis.preprocess.filter.FilterConfig)
     */
    @Override
    public ExpressionDataDoubleMatrix getFilteredMatrix( ExpressionExperiment ee, FilterConfig filterConfig ) {
        Collection<ProcessedExpressionDataVector> dataVectors = processedExpressionDataVectorService
                .getProcessedDataVectors( ee );
        return this.getFilteredMatrix( ee, filterConfig, dataVectors );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.service.ExpressionDataMatrixService#getFilteredMatrix(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment, ubic.gemma.analysis.preprocess.filter.FilterConfig, java.util.Collection)
     */
    @Override
    public ExpressionDataDoubleMatrix getFilteredMatrix( ExpressionExperiment ee, FilterConfig filterConfig,
            Collection<ProcessedExpressionDataVector> dataVectors ) {
        Collection<ArrayDesign> arrayDesignsUsed = expressionExperimentService.getArrayDesignsUsed( ee );
        return getFilteredMatrix( filterConfig, dataVectors, arrayDesignsUsed );
    }

    /**
     * @param filterConfig
     * @param dataVectors
     * @param arrayDesignsUsed
     * @return
     */
    private ExpressionDataDoubleMatrix getFilteredMatrix( FilterConfig filterConfig,
            Collection<ProcessedExpressionDataVector> dataVectors, Collection<ArrayDesign> arrayDesignsUsed ) {
        ExpressionExperimentFilter filter = new ExpressionExperimentFilter( arrayDesignsUsed, filterConfig );
        assert !dataVectors.isEmpty();
        this.processedExpressionDataVectorService.thaw( dataVectors );
        ExpressionDataDoubleMatrix eeDoubleMatrix = filter.getFilteredMatrix( dataVectors );
        return eeDoubleMatrix;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.service.ExpressionDataMatrixService#getFilteredMatrix(java.lang.String,
     * ubic.gemma.analysis.preprocess.filter.FilterConfig, java.util.Collection)
     */
    @Override
    public ExpressionDataDoubleMatrix getFilteredMatrix( String arrayDesignName, FilterConfig filterConfig,
            Collection<ProcessedExpressionDataVector> dataVectors ) {
        ArrayDesign ad = arrayDesignService.findByShortName( arrayDesignName );
        if ( ad == null ) {
            throw new IllegalArgumentException( "No platform named '" + arrayDesignName + "'" );
        }
        Collection<ArrayDesign> arrayDesignsUsed = new HashSet<ArrayDesign>();
        arrayDesignsUsed.add( ad );
        return this.getFilteredMatrix( filterConfig, dataVectors, arrayDesignsUsed );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.service.ExpressionDataMatrixService#getProcessedExpressionDataMatrix(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @Override
    public ExpressionDataDoubleMatrix getProcessedExpressionDataMatrix( ExpressionExperiment ee ) {
        Collection<ProcessedExpressionDataVector> dataVectors = this.processedExpressionDataVectorService
                .getProcessedDataVectors( ee );
        if ( dataVectors.isEmpty() ) {
            throw new IllegalArgumentException( "There are no ProcessedExpressionDataVectors for " + ee
                    + ", they must be created first" );
        }
        this.processedExpressionDataVectorService.thaw( dataVectors );
        return new ExpressionDataDoubleMatrix( dataVectors );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.service.ExpressionDataMatrixService#getProcessedExpressionDataVectors(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @Override
    public Collection<ProcessedExpressionDataVector> getProcessedExpressionDataVectors( ExpressionExperiment ee ) {
        Collection<ProcessedExpressionDataVector> dataVectors = this.processedExpressionDataVectorService
                .getProcessedDataVectors( ee ); // these are already thawed.
        return dataVectors;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.service.ExpressionDataMatrixService#getRankMatrix(java.util.Collection,
     * java.util.Collection, ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao.RankMethod)
     */
    @Override
    public DoubleMatrix<Gene, ExpressionExperiment> getRankMatrix( Collection<Gene> genes,
            Collection<ExpressionExperiment> ees, ProcessedExpressionDataVectorDao.RankMethod method ) {

        DoubleMatrix<Gene, ExpressionExperiment> matrix = new DenseDoubleMatrix<Gene, ExpressionExperiment>(
                genes.size(), ees.size() );

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
                    Double[] ar = r.toArray( new Double[r.size()] );

                    // compute median of collection.
                    double[] dar = ArrayUtils.toPrimitive( ar );
                    double medianRank = DescriptiveWithMissing.median( new DoubleArrayList( dar ) );
                    matrix.setByKeys( g, e, medianRank );

                }
            }
        }

        return matrix;
    }

}
