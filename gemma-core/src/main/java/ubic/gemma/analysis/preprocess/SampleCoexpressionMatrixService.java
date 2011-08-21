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
package ubic.gemma.analysis.preprocess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.math.MatrixStats;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixColumnSort;
import ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisDao;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Manage the "sample correlation/coexpression" matrices.
 * 
 * @author paul
 * @version $Id$
 */
@Service
public class SampleCoexpressionMatrixService {

    @Autowired
    SampleCoexpressionAnalysisDao sampleCoexpressionMatrixDao;

    @Autowired
    private ExpressionDataMatrixService expressionDataMatrixService;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    public boolean hasMatrix( ExpressionExperiment ee ) {
        return sampleCoexpressionMatrixDao.load( ee ) != null;
    }

    /**
     * Retrieve (and if necessary compute) the correlation matrix for the samples.
     * 
     * @param ee
     * @return
     */
    public DoubleMatrix<BioAssay, BioAssay> getSampleCorrelationMatrix( ExpressionExperiment ee ) {
        DoubleMatrix<BioAssay, BioAssay> mat = sampleCoexpressionMatrixDao.load( ee );

        if ( mat == null ) {

            Collection<ProcessedExpressionDataVector> processedVectors = processedExpressionDataVectorService
                    .getProcessedDataVectors( ee );

            if ( processedVectors.isEmpty() ) {
                throw new IllegalArgumentException( "Must have processed vectors created first" );
            }

            mat = getSampleCorrelationMatrix( ee, processedVectors );

        }

        mat = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( mat );
        return mat;

    }

    /**
     * @param processedVectors
     * @return
     */
    public DoubleMatrix<BioAssay, BioAssay> getSampleCorrelationMatrix( ExpressionExperiment ee,
            Collection<ProcessedExpressionDataVector> processedVectors ) {
        FilterConfig fconfig = new FilterConfig();
        fconfig.setIgnoreMinimumRowsThreshold( true );
        fconfig.setIgnoreMinimumSampleThreshold( true );
        ExpressionDataDoubleMatrix datamatrix = expressionDataMatrixService.getFilteredMatrix( ee, fconfig,
                processedVectors );

        DoubleMatrix<BioAssay, BioAssay> mat = getMatrix( datamatrix );
        assert mat != null;

        sampleCoexpressionMatrixDao.create( mat, datamatrix.getBioAssayDimension( datamatrix.getRowElement( 0 )
                .getDesignElement() ), datamatrix.getExpressionExperiment() );

        return mat;
    }

    /**
     * @param matrix
     * @return
     */
    public static DoubleMatrix<BioAssay, BioAssay> getMatrix( ExpressionDataDoubleMatrix matrix ) {
        int cols = matrix.columns();
        double[][] rawcols = new double[cols][];

        /*
         * Transpose the matrix
         */
        List<BioAssay> colElements = new ArrayList<BioAssay>();
        int m = 0;
        for ( int i = 0; i < matrix.columns(); i++ ) {
            Collection<BioAssay> bas = matrix.getBioAssaysForColumn( i );
            colElements.add( bas.iterator().next() );
            Double[] colo = matrix.getColumn( i );
            rawcols[m] = new double[colo.length];
            for ( int j = 0; j < colo.length; j++ ) {
                rawcols[m][j] = colo[j];
            }
            m++;
        }

        DoubleMatrix<BioAssay, Object> columns = new DenseDoubleMatrix<BioAssay, Object>( rawcols );

        columns.setRowNames( colElements );

        DoubleMatrix<BioAssay, BioAssay> mat = MatrixStats.correlationMatrix( columns );

        return mat;
    }

}
