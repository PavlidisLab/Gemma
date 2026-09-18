/*
 * The baseCode project
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
package ubic.gemma.core.util.math;

import java.util.BitSet;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.core.util.matrix.datafilter.RowMissingFilter;
import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;

/**
 * @author paul
 * 
 */
public class MatrixNormalizer<R, C> {

    /**
     * Rows with all missing will not be returned. Otherwise, missing values are imputed, used for estimating quantiles,
     * and then replaced with missing values at the end.
     * <p>
     * Note that the Bioconductor implementation deals with missing values differently, and in a much more complex way.
     * Therefore this gives different answers in the missing value case from Bioconductor (normalize.quantiles).
     *
     * @param matrix
     * @return
     */
    public DoubleMatrix<R, C> quantileNormalize( DoubleMatrix<R, C> matrix ) {
        return quantileNormalize( matrix, null );
    }

    /**
     * Quantile-normalize, optionally computing the reference distribution from a subset of the columns.
     * <p>
     * Every column is still mapped onto the reference -- the excluded ones are placed on the same scale as the
     * rest, they just do not get a say in what that scale is.
     * <p>
     * 🛑 Excluding a column is NOT the same as it being missing. {@link #imputeMissing} fills a missing cell with
     * its ROW MEAN, so a column of all-NaN becomes a synthetic average sample and still contributes to the
     * reference -- pulling it toward the centre. That is what an outlier-masked column does today. Passing the
     * outlier columns here instead keeps them out of the reference, which is what masking was meant to achieve.
     *
     * @param includeInReference one flag per column, true to let it define the reference distribution; null means
     *                           every column contributes, which is the historical behaviour
     */
    public DoubleMatrix<R, C> quantileNormalize( DoubleMatrix<R, C> matrix, @Nullable boolean[] includeInReference ) {
        if ( includeInReference != null ) {
            Assert.isTrue( includeInReference.length == matrix.columns(),
                    "includeInReference must have one entry per column." );
            boolean any = false;
            for ( boolean b : includeInReference ) {
                any |= b;
            }
            Assert.isTrue( any, "At least one column must define the reference distribution." );
        }

        RowMissingFilter<DoubleMatrix<R, C>, R, C, Double> f = new RowMissingFilter<>();
        f.setMinPresentCount( 1 );
        DoubleMatrix<R, C> fM = f.filter( matrix );

        BitSet missingValueStatus = imputeMissing( fM );

        // copy() rather than a fresh matrix of the same shape because this method is generic over
        // DoubleMatrix and cannot construct a concrete one; every cell is overwritten immediately below, so
        // what the copy is actually for is the shape and the row and column names.
        DoubleMatrix<R, C> sortedData = fM.copy();

        // One buffer for every column instead of one per column. getColumn() allocates a double[rows] on each
        // call, so the old loop churned columns x rows doubles -- 299 MB on a 34,330 x 1,090 matrix -- to read
        // data that is already in the matrix. DoubleArrayList wraps the array without copying and sort()
        // orders it in place, so the sorted values are readable straight out of the buffer.
        double[] columnBuffer = new double[fM.rows()];
        DoubleArrayList sortedColumn = new DoubleArrayList( columnBuffer );
        for ( int i = 0; i < fM.columns(); i++ ) {
            for ( int j = 0; j < fM.rows(); j++ ) {
                columnBuffer[j] = fM.get( j, i );
            }
            sortedColumn.sort();
            for ( int j = 0; j < fM.rows(); j++ ) {
                sortedData.set( j, i, columnBuffer[j] );
            }
        }

        /*
         * Compute the mean at each rank
         */
        // Same again for the rows: getRow() allocated a double[columns] per row and then a DoubleArrayList
        // around it, twice over, for every one of the rows. The buffer is sized for the widest case and
        // setSize tells Descriptive.mean how much of it counts, which is how the includeInReference subset is
        // expressed without a second allocation.
        DoubleArrayList rowMeans = new DoubleArrayList( sortedData.rows() );
        double[] rowBuffer = new double[sortedData.columns()];
        DoubleArrayList contributing = new DoubleArrayList( rowBuffer );
        for ( int i = 0; i < sortedData.rows(); i++ ) {
            int n = 0;
            for ( int j = 0; j < sortedData.columns(); j++ ) {
                if ( includeInReference == null || includeInReference[j] ) {
                    rowBuffer[n++] = sortedData.get( i, j );
                }
            }
            contributing.setSize( n );
            rowMeans.add( Descriptive.mean( contributing ) );
        }

        for ( int j = 0; j < sortedData.columns(); j++ ) {

            // Ranked here, one column at a time, rather than all of them up front. Every rank column is
            // read by exactly this iteration of j and by nothing else, so holding all of them cost
            // columns x rows doubles for no reuse -- 299 MB on a 34,330 x 1,090 matrix. Same number of
            // rankTransform calls either way.
            // getColumn() here and not the shared buffer: rankTransform's treatment of the list it is
            // handed is not part of its contract, and a buffer it retained would be silently overwritten by
            // the next column. One allocation per column is worth not having to be sure.
            DoubleArrayList ranks = Rank.rankTransform( new DoubleArrayList( fM.getColumn( j ) ) );
            assert ranks != null;

            for ( int i = 0; i < sortedData.rows(); i++ ) {

                // No missing-value test here: imputeMissing filled every NaN in fM with its row mean
                // before sortedData was copied from it, so there is none left to find. The cells that
                // were missing are re-masked from missingValueStatus below, which is the actual record.

                double rank = ranks.get( i ) - 1.0;

                int intrank = ( int ) Math.floor( rank );

                // 🛑 double, not Double. This is the innermost loop of the whole normalization: it runs
                // rows x columns times -- 37.4 million on a 34,330 x 1,090 matrix -- and boxing here
                // allocated a Double object on every one of them, for a value that is read once and
                // discarded. The null check went with it; a primitive cannot be null.
                double value;
                if ( rank - intrank > 0.4 && intrank > 0 ) {
                    // cope with tied ranks. 0.4 is the threshold R uses.
                    value = ( rowMeans.get( intrank ) + rowMeans.get( intrank - 1 ) ) / 2.0;
                } else {
                    value = rowMeans.get( intrank );
                }
                sortedData.set( i, j, value );

            }
        }

        // mask the missing values.
        for ( int k = missingValueStatus.nextSetBit( 0 ); k >= 0; k = missingValueStatus.nextSetBit( k + 1 ) ) {
            sortedData.set( k / sortedData.columns(), k % sortedData.columns(), Double.NaN );
        }

        return sortedData;

    }

    /**
     * Simple imputation method. Generally (but not always), missing values correspond to "low expression". Therefore
     * imputed values of zero are defensible. However, because at this point the matrix has probably already been
     * filtered, the row mean is better.
     * <p>
     * FIXME this should be factored out
     * <p>
     * The record of which cells were missing is a {@link BitSet} over {@code row * columns + column}, not a
     * matrix: it is one bit of information per cell and a same-shaped {@code DenseDoubleMatrix} charged 64 bits
     * for it. On a 34,330 x 1,090 matrix that is 4.7 MB rather than 299 MB, and on data with no missing values
     * at all -- the common case -- it is an empty set rather than 37.4 million stored 1.0s.
     *
     * @param matrix modified in place: every missing cell is filled with its row mean
     * @return the positions that were missing, as {@code row * matrix.columns() + column}
     */
    private BitSet imputeMissing( DoubleMatrix<R, C> matrix ) {
        /*
         * keep track of the missing values so they can be re-masked later.
         */
        BitSet missingValueInfo = new BitSet( matrix.rows() * matrix.columns() );
        for ( int i = 0; i < matrix.rows(); i++ ) {
            DoubleArrayList v = new DoubleArrayList( matrix.getRow( i ) );
            double m = DescriptiveWithMissing.mean( v );
            for ( int j = 0; j < matrix.columns(); j++ ) {
                double d = matrix.get( i, j );
                if ( Double.isNaN( d ) ) {
                    missingValueInfo.set( i * matrix.columns() + j );
                    matrix.set( i, j, m );
                }
            }
        }
        return missingValueInfo;
    }
}
