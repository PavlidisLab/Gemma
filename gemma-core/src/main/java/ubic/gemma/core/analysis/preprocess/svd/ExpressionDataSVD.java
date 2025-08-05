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
package ubic.gemma.core.analysis.preprocess.svd;

import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import org.apache.commons.lang3.StringUtils;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.MatrixStats;
import ubic.basecode.math.linalg.SingularValueDecomposition;
import ubic.gemma.core.analysis.preprocess.filter.AffyProbeNameFilter;
import ubic.gemma.core.analysis.preprocess.filter.AffyProbeNameFilter.Pattern;
import ubic.gemma.core.analysis.preprocess.filter.LowVarianceFilter;
import ubic.gemma.core.analysis.preprocess.filter.RowMissingValueFilter;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Perform SVD on an expression data matrix, E = U S V'. The rows of the input matrix are probes (genes), following the
 * convention of Alter et al. 2000 (PNAS). Thus the U matrix columns are the <em>eigensamples</em> (eigenarrays) and the
 * V matrix columns are the <em>eigengenes</em>. See also http://genome-www.stanford.edu/SVD/.
 * Because SVD can't be done on a matrix with missing values, values are imputed. Rows with no variance are removed, and
 * rows with too many missing values are also removed (MIN_PRESENT_FRACTION_FOR_ROW)
 *
 * @author paul
 */
@SuppressWarnings({ "WeakerAccess", "unused" }) // Possible external use
public class ExpressionDataSVD {

    private static final double MIN_PRESENT_FRACTION_FOR_ROW = 0.75;
    DenseDoubleMatrix2D missingValueInfo;
    SingularValueDecomposition<CompositeSequence, BioMaterial> svd;
    private ExpressionDataDoubleMatrix expressionData;
    private boolean normalized = false;

    /**
     * Does normalization.
     *
     * @param expressionData expression data
     */
    public ExpressionDataSVD( ExpressionDataDoubleMatrix expressionData ) throws SVDException {
        this( expressionData, true );
    }

    /**
     * @param expressionData  Note that this may be modified!
     * @param normalizeMatrix If true, the data matrix will be rescaled and centred to mean zero, variance one, for both
     *                        rows and columns ("double-standardized")
     */
    public ExpressionDataSVD( ExpressionDataDoubleMatrix expressionData, boolean normalizeMatrix ) throws SVDException {
        this.expressionData = expressionData;

        ArrayDesign arrayDesign = expressionData.getRowElement( 0 ).getDesignElement().getArrayDesign();
        if ( StringUtils.isNotBlank( arrayDesign.getName() ) && arrayDesign.getName().toUpperCase()
                .contains( "AFFYMETRIX" ) ) {
            AffyProbeNameFilter affyProbeNameFilter = new AffyProbeNameFilter( new Pattern[] { Pattern.AFFX } );
            this.expressionData = affyProbeNameFilter.filter( this.expressionData );
        }

        // FIXME Remove any columns which have only missing data. We have to put in dummy values, otherwise things will be quite messed up
        // ColumnMissingValueFilter columnMissingFilter = new ColumnMissingValueFilter();
        // columnMissingFilter.setMinPresentFactrion( 10 );
        // this.expressionData = columnMissingFilter.filter( this.expressionData );

        /*
         * Now filter rows.
         */
        RowMissingValueFilter rowMissingFilter = new RowMissingValueFilter();
        rowMissingFilter.setMinPresentFraction( ExpressionDataSVD.MIN_PRESENT_FRACTION_FOR_ROW );
        this.expressionData = rowMissingFilter.filter( this.expressionData );

        /*
         * Remove rows with no variance
         */
        // the colt SVD method fails to converge? I tried removing just Constant.SMALL but
        // it wasn't enough?
        LowVarianceFilter rlf = new LowVarianceFilter( 0.01 );
        this.expressionData = rlf.filter( this.expressionData );

        if ( this.expressionData.rows() == 0 ) {
            throw new SVDException( "After filtering, matrix has no rows, SVD cannot be computed" );
        }

        if ( this.expressionData.rows() < this.expressionData.columns() ) {
            throw new SVDException(
                    "After filtering, this data set has more samples than rows; SVD not supported." );
        }

        // if we want to filter by expression. Problem: choosing threshold. Filtering by variance (lightly, as above) is okay.
        //        if (!expressionData.getQuantitationTypes().iterator().next().getIsRatio()) {
        //            RowLevelFilter explevelFilt = new RowLevelFilter();
        //            rlf.setMethod( Method.MEAN );
        //            rlf.setLowCut( 0.3, true );
        //        }

        this.normalized = normalizeMatrix;
        DoubleMatrix<CompositeSequence, BioMaterial> matrix = this.expressionData.getMatrix();

        assert matrix.getRowNames().size() > 0;
        assert matrix.getColNames().size() > 0;

        this.imputeMissing( matrix );

        if ( normalizeMatrix ) {
            matrix = MatrixStats.doubleStandardize( matrix );
        }

        this.svd = new SingularValueDecomposition<>( matrix );
    }

    /**
     * Implements the method described in the SPELL paper, alternative interpretation as related by Q. Morris. Set all
     * components to have equal weight (set all singular values to 1)
     *
     * @return the reconstructed matrix; values that were missing before are re-masked.
     */
    public ExpressionDataDoubleMatrix equalize() {
        DoubleMatrix<Integer, Integer> copy = svd.getS().copy();

        for ( int i = 0; i < copy.columns(); i++ ) {
            copy.set( i, i, 1.0 );
        }

        double[][] rawU = svd.getU().getRawMatrix();
        double[][] rawS = copy.getRawMatrix();
        double[][] rawV = svd.getV().getRawMatrix();

        DoubleMatrix2D u = new DenseDoubleMatrix2D( rawU );
        DoubleMatrix2D s = new DenseDoubleMatrix2D( rawS );
        DoubleMatrix2D v = new DenseDoubleMatrix2D( rawV );

        Algebra a = new Algebra();
        DoubleMatrix<CompositeSequence, BioMaterial> reconstructed = new DenseDoubleMatrix<>(
                a.mult( a.mult( u, s ), a.transpose( v ) ).toArray() );

        reconstructed.setRowNames( this.expressionData.getMatrix().getRowNames() );
        reconstructed.setColumnNames( this.expressionData.getMatrix().getColNames() );

        // re-mask the missing values.
        for ( int i = 0; i < reconstructed.rows(); i++ ) {
            for ( int j = 0; j < reconstructed.columns(); j++ ) {
                if ( Double.isNaN( this.missingValueInfo.get( i, j ) ) ) {
                    reconstructed.set( i, j, Double.NaN );
                }
            }
        }

        return new ExpressionDataDoubleMatrix( this.expressionData, reconstructed, this.expressionData.getQuantitationTypes() );
    }

    /**
     * @param  i which eigengene
     * @return the ith eigengene (column of V)
     */
    public Double[] getEigenGene( int i ) {
        return this.getV().getColObj( i );
    }

    /**
     * @param  i which eigensample
     * @return the ith eigensample (column of U)
     */
    public Double[] getEigenSample( int i ) {
        return this.getU().getColObj( i );
    }

    /**
     * @return the square roots of the singular values.
     */
    public double[] getEigenvalues() {
        double[] singularValues = this.getSingularValues();
        double[] eigenvalues = new double[singularValues.length];
        for ( int i = 0; i < singularValues.length; i++ ) {
            double d = Math.pow( singularValues[i], 2 ) / ( this.getNumVariables() - 1 );
            eigenvalues[i] = d;
        }
        return eigenvalues;
    }

    /**
     * @return how many rows the U matrix has.
     */
    public int getNumVariables() {
        return this.svd.getU().rows();
    }

    /**
     * @return the matrix of singular values, indexed by the eigenarray (row) and eigengene (column) numbers (starting
     *         from 0).
     */
    public DoubleMatrix<Integer, Integer> getS() {
        return svd.getS();
    }

    public double[] getSingularValues() {
        return this.svd.getSingularValues();
    }

    /**
     * @return the left singular vectors. The column indices are of the eigenarrays (starting from 0).
     */
    public DoubleMatrix<CompositeSequence, Integer> getU() {
        return svd.getU();
    }

    /**
     * @return the right singular vectors. The column indices are of the eigengenes (starting from 0). The row indices
     *         are of the original samples in the given ExpressionDataDoubleMatrix.
     */
    public DoubleMatrix<Integer, BioMaterial> getV() {
        return svd.getV();
    }

    /**
     * @return fractions of the variance for each singular vector.
     */
    public Double[] getVarianceFractions() {
        double[] singularValues = svd.getSingularValues();
        // d should be be square roots of the eigenvalues scaled by number of variables: check

        int numVariables = this.getNumVariables();

        double sum = 0;
        for ( double d : singularValues ) {

            double d2 = d * d / ( numVariables - 1 );
            sum += d2;
        }
        Double[] answer = new Double[singularValues.length];
        for ( int i = 0; i < singularValues.length; i++ ) {
            answer[i] = singularValues[i] * singularValues[i] / sum;
        }
        return answer;
    }

    /**
     * Provide a reconstructed matrix removing the first N components (the most significant ones). If the matrix was
     * normalized first, removing the first component replicates the normalization approach taken by Nielsen et al.
     * (Lancet 359, 2002) and Alter et al. (PNAS 2000). Correction by ANOVA would yield similar results if the nuisance
     * variable is known.
     *
     * @param  numComponentsToRemove The number of components to remove, starting from the largest eigenvalue.
     * @return the reconstructed matrix; values that were missing before are re-masked.
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
        DoubleMatrix<CompositeSequence, BioMaterial> reconstructed = new DenseDoubleMatrix<>(
                a.mult( a.mult( u, s ), a.transpose( v ) ).toArray() );

        reconstructed.setRowNames( this.expressionData.getMatrix().getRowNames() );
        reconstructed.setColumnNames( this.expressionData.getMatrix().getColNames() );

        // re-mask the missing values.
        for ( int i = 0; i < reconstructed.rows(); i++ ) {
            for ( int j = 0; j < reconstructed.columns(); j++ ) {
                if ( Double.isNaN( this.missingValueInfo.get( i, j ) ) ) {
                    reconstructed.set( i, j, Double.NaN );
                }
            }
        }

        return new ExpressionDataDoubleMatrix( this.expressionData, reconstructed, this.expressionData.getQuantitationTypes() );
    }

    /**
     * @return Implements the method described in the SPELL paper. Note that this alters the U matrix of this.
     *         <p>
     *         We make two assumptions about the method that are not described in the paper: 1) The data are rescaled
     *         and
     *         centered; 2) the absolute value of the U matrix is used. Note that unlike the original data, the
     *         transformed data
     *         will have no missing values.
     *         </p>
     */
    public ExpressionDataDoubleMatrix uMatrixAsExpressionData() {

        if ( !normalized ) {
            throw new IllegalStateException( "You must do SVD on the normalized matrix" );
        }

        DoubleMatrix<CompositeSequence, Integer> rawUMatrix = svd.getU();

        DoubleMatrix<CompositeSequence, BioMaterial> result = new DenseDoubleMatrix<>( rawUMatrix.rows(),
                rawUMatrix.columns() );

        // take the absolute value of the U matrix.
        for ( int i = 0; i < rawUMatrix.rows(); i++ ) {
            for ( int j = 0; j < rawUMatrix.columns(); j++ ) {
                result.set( i, j, Math.abs( rawUMatrix.get( i, j ) ) );
            }
        }
        List<BioMaterial> colNames = svd.getV().getColNames();

        result.setColumnNames( colNames );
        result.setRowNames( rawUMatrix.getRowNames() );

        // use that as the 'expression data'
        return new ExpressionDataDoubleMatrix( this.expressionData, result, this.expressionData.getQuantitationTypes() );
    }

    /**
     * Implements method described in Skillicorn et al., "Strategies for winnowing microarray data" (also section 3.5.5
     * of his book)
     *
     * @param  thresholdQuantile Enter 0.5 for median. Value must be &gt; 0 and &lt; 1.
     * @return a filtered matrix
     */
    public ExpressionDataDoubleMatrix winnow( double thresholdQuantile ) {

        if ( thresholdQuantile <= 0 || thresholdQuantile >= 1 ) {
            throw new IllegalArgumentException( "Threshold quantile should be a value between 0 and 1 exclusive" );
        }

        class NormCmp implements Comparable<NormCmp> {
            private Double norm;
            private int rowIndex;

            private NormCmp( int rowIndex, Double norm ) {
                super();
                this.rowIndex = rowIndex;
                this.norm = norm;
            }

            @Override
            public int compareTo( NormCmp o ) {
                return this.norm.compareTo( o.norm );
            }

            public int getRowIndex() {
                return rowIndex;
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + ( ( norm == null ) ? 0 : norm.hashCode() );
                return result;
            }

            @Override
            public boolean equals( Object obj ) {
                if ( this == obj )
                    return true;
                if ( obj == null )
                    return false;
                if ( this.getClass() != obj.getClass() )
                    return false;
                NormCmp other = ( NormCmp ) obj;
                if ( norm == null ) {
                    return other.norm == null;
                }
                return norm.equals( other.norm );
            }

        }

        // order rows by distance from the origin. This is proportional to the 1-norm.
        Algebra a = new Algebra();
        List<NormCmp> os = new ArrayList<>();
        for ( int i = 0; i < this.expressionData.rows(); i++ ) {
            double[] row = this.getU().getRow( i );
            DoubleMatrix1D rom = new DenseDoubleMatrix1D( row );
            double norm1 = a.norm1( rom );
            os.add( new NormCmp( i, norm1 ) );
        }

        Collections.sort( os );

        int quantileLimit = ( int ) Math.floor( this.expressionData.rows() * thresholdQuantile );
        quantileLimit = Math.max( 0, quantileLimit );

        List<CompositeSequence> keepers = new ArrayList<>();
        for ( int i = 0; i < quantileLimit; i++ ) {
            NormCmp x = os.get( i );
            CompositeSequence d = this.expressionData.getDesignElementForRow( x.getRowIndex() );
            keepers.add( d );
        }

        // remove genes which are near the origin in SVD space. FIXME: make sure the missing values are still masked.
        return this.expressionData.sliceRows( keepers );

    }

    /**
     * Simple imputation method. Generally (but not always), missing values correspond to "low expression". Therefore
     * imputed values of zero are defensible. However, because at this point the matrix has probably already been
     * filtered, the row mean is better.
     */
    private void imputeMissing( DoubleMatrix<CompositeSequence, BioMaterial> matrix ) {
        /*
         * keep track of the missing values so they can be re-masked later.
         */
        missingValueInfo = new DenseDoubleMatrix2D( matrix.rows(), matrix.columns() );
        for ( int i = 0; i < matrix.rows(); i++ ) {
            DoubleArrayList v = new DoubleArrayList( matrix.getRow( i ) );
            double m = DescriptiveWithMissing.mean( v );
            for ( int j = 0; j < matrix.columns(); j++ ) {
                double d = matrix.get( i, j );
                if ( Double.isNaN( d ) ) {
                    missingValueInfo.set( i, j, Double.NaN );
                    matrix.set( i, j, m );
                } else {
                    missingValueInfo.set( i, j, 1.0 );
                }
            }
        }
    }
}
