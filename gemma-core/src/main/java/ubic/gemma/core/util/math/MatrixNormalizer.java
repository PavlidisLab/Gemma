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

import java.util.LinkedHashMap;
import java.util.Map;

import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
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

        DoubleMatrix<R, C> missingValueStatus = imputeMissing( fM );

        /*
         * Compute ranks of each column. Missing values are wherever they end up, which is a bit odd.
         */
        Map<Integer, DoubleArrayList> ranks = new LinkedHashMap<>();

        DoubleMatrix<R, C> sortedData = fM.copy();
        for ( int i = 0; i < fM.columns(); i++ ) {
            DoubleArrayList dataColumn = new DoubleArrayList( fM.getColumn( i ) );

            DoubleArrayList sortedColumn = dataColumn.copy();
            sortedColumn.sort();
            for ( int j = 0; j < sortedColumn.size(); j++ ) {
                sortedData.set( j, i, sortedColumn.get( j ) );
            }

            DoubleArrayList r = Rank.rankTransform( dataColumn );
            assert r != null;
            ranks.put( i, r );
        }

        /*
         * Compute the mean at each rank
         */
        DoubleArrayList rowMeans = new DoubleArrayList( sortedData.rows() );
        for ( int i = 0; i < sortedData.rows(); i++ ) {
            double[] row = sortedData.getRow( i );
            DoubleArrayList contributing;
            if ( includeInReference == null ) {
                contributing = new DoubleArrayList( row );
            } else {
                contributing = new DoubleArrayList( row.length );
                for ( int j = 0; j < row.length; j++ ) {
                    if ( includeInReference[j] ) {
                        contributing.add( row[j] );
                    }
                }
            }
            rowMeans.add( Descriptive.mean( contributing ) );
        }

        for ( int j = 0; j < sortedData.columns(); j++ ) {

            for ( int i = 0; i < sortedData.rows(); i++ ) {

                if ( Double.isNaN( fM.get( i, j ) ) ) {
                    sortedData.set( i, j, Double.NaN );
                    continue;
                }

                double rank = ranks.get( j ).get( i ) - 1.0;

                int intrank = ( int ) Math.floor( rank );

                Double value = null;
                if ( rank - intrank > 0.4 && intrank > 0 ) {
                    // cope with tied ranks. 0.4 is the threshold R uses.
                    value = ( rowMeans.get( intrank ) + rowMeans.get( intrank - 1 ) ) / 2.0;
                } else {
                    value = rowMeans.get( intrank );
                }
                assert value != null : "No mean value for rank=" + rank;
                sortedData.set( i, j, value );

            }
        }

        assert missingValueStatus.rows() == sortedData.rows() && missingValueStatus.columns() == sortedData.columns();

        // mask the missing values.
        for ( int i = 0; i < missingValueStatus.rows(); i++ ) {
            for ( int j = 0; j < missingValueStatus.columns(); j++ ) {
                if ( Double.isNaN( missingValueStatus.get( i, j ) ) ) {
                    sortedData.set( i, j, Double.NaN );
                }
            }
        }

        return sortedData;

    }

    /**
     * Simple imputation method. Generally (but not always), missing values correspond to "low expression". Therefore
     * imputed values of zero are defensible. However, because at this point the matrix has probably already been
     * filtered, the row mean is better.
     * <p>
     * FIXME this should be factored out
     *
     * @param matrix
     * @return missing value status
     */
    private DoubleMatrix<R, C> imputeMissing( DoubleMatrix<R, C> matrix ) {
        /*
         * keep track of the missing values so they can be re-masked later.
         */
        DoubleMatrix<R, C> missingValueInfo = new DenseDoubleMatrix<>( matrix.rows(), matrix.columns() );
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
        return missingValueInfo;
    }
}
