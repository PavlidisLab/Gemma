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
import ubic.gemma.core.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.core.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrixColumnSort;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;

import java.util.Collection;

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

    public static DoubleMatrix<BioAssay, BioAssay> getMatrix( ExpressionDataDoubleMatrix matrix ) {

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
    public DoubleMatrix<BioAssay, BioAssay> create( ExpressionExperiment ee, boolean forceRecompute ) {
        DoubleMatrix<BioAssay, BioAssay> mat = sampleCoexpressionMatrixHelperService.load( ee );

        if ( forceRecompute || mat == null ) {
            log.info( "Computing sample coexpression" );
            Collection<ProcessedExpressionDataVector> processedVectors = processedExpressionDataVectorService
                    .getProcessedDataVectors( ee );

            if ( processedVectors.isEmpty() ) {
                throw new IllegalArgumentException( "Must have processed vectors created first" );
            }

            mat = create( ee, processedVectors );
        }

        try {
            mat = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( mat );
            mat = mat.subsetRows( mat.getColNames() ); // enforce same order on rows.
            return mat;
        } catch ( Exception e ) {
            log.error( "Could not reformat the sample correlation matrix: " + e.getMessage() );
            return mat;
        }

    }

    @Override
    public DoubleMatrix<BioAssay, BioAssay> create( ExpressionExperiment ee,
            Collection<ProcessedExpressionDataVector> processedVectors ) {

        FilterConfig fconfig = new FilterConfig();
        fconfig.setIgnoreMinimumRowsThreshold( true );
        fconfig.setIgnoreMinimumSampleThreshold( true );

        fconfig.setRequireSequences( true ); // not sure if this is the best thing to do. Some tests will fail.
        // Loads using new array designs will fail. So we allow special case where there are no sequences.

        ExpressionDataDoubleMatrix datamatrix = expressionDataMatrixService
                .getFilteredMatrix( ee, fconfig, processedVectors );

        DoubleMatrix<BioAssay, BioAssay> mat = getMatrix( datamatrix );
        assert mat != null;

        BioAssayDimension bestBioAssayDimension = datamatrix.getBestBioAssayDimension();

        if ( mat.rows() != bestBioAssayDimension.getBioAssays().size() ) {
            throw new IllegalStateException( "Number of bioassays doesn't match length of the bioassayDimension" );
        }

        sampleCoexpressionMatrixHelperService
                .create( mat, bestBioAssayDimension, datamatrix.getExpressionExperiment() );

        return mat;
    }

    @Override
    public void delete( ExpressionExperiment ee ) {
        sampleCoexpressionMatrixHelperService.removeForExperiment( ee );
    }

    @Override
    public DoubleMatrix<BioAssay, BioAssay> findOrCreate( ExpressionExperiment expressionExperiment ) {
        return create( expressionExperiment, false );
    }

    @Override
    public boolean hasMatrix( ExpressionExperiment ee ) {
        return sampleCoexpressionMatrixHelperService.load( ee ) != null;
    }
}
