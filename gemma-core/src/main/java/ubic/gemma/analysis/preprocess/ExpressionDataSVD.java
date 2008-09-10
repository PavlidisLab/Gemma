/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.analysis.preprocess;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import edu.emory.mathcs.backport.java.util.Collections;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.math.SingularValueDecomposition;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.designElement.DesignElement;

/**
 * Perform SVD on an expression data matrix, E = U S V'. The rows of the input matrix are probes (genes), following the
 * convention of Alter et al. 2000 (PNAS). Thus the U matrix columns are the <em>eigensamples</em> (eigenarrays) and
 * the V matrix columns are the <em>eigengenes</em>. See also http://genome-www.stanford.edu/SVD/.
 * <p>
 * FIXME this also includes SVD-based normalization algorithms which might best be refactored out.
 * 
 * @author paul
 * @version $Id$
 */
public class ExpressionDataSVD {
    SingularValueDecomposition<DesignElement, Integer> svd;
    private ExpressionDataDoubleMatrix expressionData;
    private boolean normalized = false;
    private double norm1;

    /**
     * @param expressionData
     * @param normalizeMatrix If true, the data matrix will be rescaled and centred to mean zero, variance one, on a
     *        per-column (sample) basis.
     */
    public ExpressionDataSVD( ExpressionDataDoubleMatrix expressionData, boolean normalizeMatrix ) {
        this.expressionData = expressionData;
        this.normalized = normalizeMatrix;
        DoubleMatrix<DesignElement, Integer> matrix = expressionData.getMatrix();
        this.svd = new SingularValueDecomposition<DesignElement, Integer>( matrix );
    }

    /**
     * @return the matrix of singular values, indexed by the eigenarray (row) and eigengene (column) numbers (starting
     *         from 0).
     */
    public DoubleMatrix<Integer, Integer> getS() {
        return svd.getS();
    }

    /**
     * @return the left singular vectors. The column indices are of the eigenarrays (starting from 0).
     */
    public DoubleMatrix<DesignElement, Integer> getU() {
        return svd.getU();
    }

    /**
     * @return the right singular vectors. The column indices are of the eigengenes (starting from 0). The row indices
     *         are of the original samples in the given ExpressionDataDoubleMatrix.
     */
    public DoubleMatrix<Integer, Integer> getV() {
        return svd.getV();
    }

    /**
     * Implements the method described in the SPELL paper. Note that this alters the U matrix of this.
     * <p>
     * We make two assumptions about the method that are not described in the paper: 1) The data are rescaled and
     * centered; 2) the absolute value of the U matrix is used.
     * 
     * @return
     */
    public ExpressionDataDoubleMatrix uMatrixAsExpressionData() {
        /*
         *  
         */
        if ( !normalized ) {
            throw new IllegalStateException( "You must do SVD on the normalized matrix" );
        }

        DoubleMatrix<DesignElement, Integer> rawUMatrix = svd.getU();

        // take the absolute value of the U matrix.
        for ( int i = 0; i < rawUMatrix.rows(); i++ ) {
            for ( int j = 0; j < rawUMatrix.columns(); j++ ) {
                rawUMatrix.set( i, j, Math.abs( rawUMatrix.get( i, j ) ) );
            }
        }

        // use that as the 'expression data'
        return new ExpressionDataDoubleMatrix( this.expressionData, rawUMatrix );
    }

    /**
     * Implements method described in Skillicorn et al., "Strategies for winnowing microarray data" (also section 3.5.5
     * of his book)
     * 
     * @param thresholdQuantile Enter 0.5 for median. Value must be > 0 and < 1.
     * @return a filtered matrix
     */
    public ExpressionDataDoubleMatrix winnow( double thresholdQuantile ) {

        if ( thresholdQuantile <= 0 || thresholdQuantile >= 1 ) {
            throw new IllegalArgumentException( "Threshold quantile should be a value between 0 and 1 exclusive" );
        }

        class O implements Comparable {
            int rowIndex;
            Double norm;

            public int getRowIndex() {
                return rowIndex;
            }

            public O( int rowIndex, Double norm ) {
                super();
                this.rowIndex = rowIndex;
                this.norm = norm;
            }

            public int compareTo( Object o ) {
                return this.norm.compareTo( ( ( O ) o ).norm );
            }

        }

        // order rows by distance from the origin. This is proportional to the 1-norm.
        Algebra a = new Algebra();
        List<O> os = new ArrayList<O>();
        for ( int i = 0; i < this.expressionData.rows(); i++ ) {
            Double[] row = this.expressionData.getRow( i );
            DoubleMatrix1D rom = new DenseDoubleMatrix1D( ArrayUtils.toPrimitive( row ) );
            norm1 = a.norm1( rom );
            os.add( new O( i, norm1 ) );
        }

        Collections.sort( os );

        int quantileLimit = ( int ) Math.floor( this.expressionData.rows() * thresholdQuantile );
        quantileLimit = Math.max( 0, quantileLimit );

        List<DesignElement> keepers = new ArrayList<DesignElement>();
        for ( int i = 0; i < quantileLimit; i++ ) {
            O x = os.get( i );
            DesignElement d = this.expressionData.getDesignElementForRow( x.getRowIndex() );
            keepers.add( d );
        }

        // / remove genes which are near the origin in SVD space.
        return new ExpressionDataDoubleMatrix( this.expressionData, keepers );

    }

    /**
     * Provide a reconstructed matrix removing the first N components (the most significant ones). If the matrix was
     * normalized first, removing the first component replicates the normalization approach taken by Nielsen et al.
     * (Lancet 359, 2002) and Alter et al. (PNAS 2000). Correction by ANOVA would yield similar results if the nuisance
     * variable is known.
     * 
     * @param numComponentsToRemove The number of components to remove, starting from the largest eigenvalue.
     * @return the reconstructed matrix
     */
    public ExpressionDataDoubleMatrix removeHighestComponents( int numComponentsToRemove ) {
        DoubleMatrix<Integer, Integer> copy = svd.getS().copy();

        for ( int i = 0; i < numComponentsToRemove; i++ ) {
            copy.set( i, i, 0.0 );
        }

        double[][] rawU = svd.getU().getRawMatrix();
        double[][] rawS = copy.getRawMatrix();
        double[][] rawV = svd.getV().getRawMatrix();

        DoubleMatrix2D u = new DenseDoubleMatrix2D( rawU );
        DoubleMatrix2D s = new DenseDoubleMatrix2D( rawS );
        DoubleMatrix2D v = new DenseDoubleMatrix2D( rawV );

        Algebra a = new Algebra();
        DoubleMatrix<DesignElement, Integer> reconstructed = new DenseDoubleMatrix<DesignElement, Integer>( a.mult(
                a.mult( u, s ), a.transpose( v ) ).toArray() );
        reconstructed.setRowNames( this.expressionData.getMatrix().getRowNames() );
        reconstructed.setColumnNames( this.expressionData.getMatrix().getColNames() );

        return new ExpressionDataDoubleMatrix( this.expressionData, reconstructed );
    }

}
