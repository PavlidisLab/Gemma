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
package ubic.gemma.core.analysis.preprocess.normalize;

import cern.colt.list.DoubleArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import ubic.gemma.core.util.math.MatrixNormalizer;
import ubic.gemma.core.util.math.Rank;
import ubic.gemma.core.util.matrix.DoubleMatrix;

import java.util.BitSet;

/**
 * Perform quantile normalization on a matrix, as described in:
 * <p>
 * Bolstad, B (2001) _Probe Level Quantile Normalization of High Density Oligonucleotide Array Data_. Unpublished
 * manuscript <a href="http://oz.berkeley.edu/~bolstad/stuff/qnorm.pdf">PDF</a>
 * </p>
 * Bolstad, B. M., Irizarry R. A., Astrand, M, and Speed, T. P. (2003) _A Comparison of Normalization Methods for High
 * Density Oligonucleotide Array Data Based on Bias and Variance._ Bioinformatics 19(2) ,pp 185-193. <a
 * href="http://www.stat.berkeley.edu/~bolstad/normalize/normalize.html">web page</a>.
 * However, note that this deals with missing values differently than the Bioconductor implementation.
 *
 * @author pavlidis
 * @see ubic.gemma.core.util.math.MatrixNormalizer
 */
@Slf4j
public class QuantileNormalizer<R, C> {

    public DoubleMatrix<R, C> normalize( DoubleMatrix<R, C> dataMatrix ) {
        return normalize( dataMatrix, null );
    }

    /**
     * Normalize, letting only some columns define the reference distribution.
     *
     * @param includeInReference one flag per column; null means every column contributes
     * @see MatrixNormalizer#quantileNormalize(DoubleMatrix, boolean[])
     */
    public DoubleMatrix<R, C> normalize( DoubleMatrix<R, C> dataMatrix, @Nullable boolean[] includeInReference ) {
        MatrixNormalizer<R, C> m = new MatrixNormalizer<>();
        return m.quantileNormalize( dataMatrix, includeInReference );
    }

    /**
     * Normalize rows of data in place, producing exactly the values of {@link #normalize(DoubleMatrix, boolean[])}.
     * <p>
     * The matrix path holds the input matrix, a filtered copy of it and a sorted copy of that, three full-size
     * {@code rows x columns} copies on top of the caller's own data. This one works on the caller's row arrays and
     * needs two {@code double[rows]} buffers, a {@code double[rows]} reference distribution and one bit per cell of
     * the rows that have missing values.
     * <p>
     * Every step is the same computation in the same order, which is what makes the results identical rather than
     * merely close: rows with no value at all are left alone (the matrix path drops them with
     * {@code RowMissingFilter}), a missing cell is imputed with its row mean for the ranking, each column is sorted
     * with {@link DoubleArrayList#sort()}, the reference at each rank is the mean over the contributing columns summed
     * in column order, ranks come from {@link Rank#rankTransform(DoubleArrayList)} with the same tie handling, and
     * the missing cells are masked again at the end.
     *
     * @param data               one array per row, all of the same length; rows that have at least one value are
     *                           overwritten with their normalized values, rows with none are left as they are
     * @param includeInReference one flag per column; null means every column contributes
     * @see MatrixNormalizer#quantileNormalize(DoubleMatrix, boolean[])
     */
    public static void normalizeInPlace( double[][] data, @Nullable boolean[] includeInReference ) {
        if ( data.length == 0 ) {
            return;
        }
        int cols = data[0].length;
        for ( double[] row : data ) {
            Assert.isTrue( row.length == cols, "All rows must have " + cols + " values." );
        }
        int contributingColumns = cols;
        if ( includeInReference != null ) {
            Assert.isTrue( includeInReference.length == cols, "includeInReference must have one entry per column." );
            contributingColumns = 0;
            for ( boolean b : includeInReference ) {
                if ( b ) {
                    contributingColumns++;
                }
            }
            Assert.isTrue( contributingColumns > 0, "At least one column must define the reference distribution." );
        }

        // rows with at least one value, as in RowMissingFilter with a minimum present count of 1
        int[] activeRows = new int[data.length];
        int rows = 0;
        for ( int i = 0; i < data.length; i++ ) {
            for ( double v : data[i] ) {
                if ( !Double.isNaN( v ) ) {
                    activeRows[rows++] = i;
                    break;
                }
            }
        }
        if ( rows == 0 ) {
            return;
        }
        if ( rows < data.length ) {
            log.info( String.format( "%d rows have no values and are left out of the quantile normalization.", data.length - rows ) );
        }

        // Record the missing cells and impute them with the row mean, which is what MatrixNormalizer.imputeMissing
        // does. One BitSet per row that has any, rather than one over row * columns, which overflows an int past
        // 2^31 cells.
        BitSet[] missing = new BitSet[rows];
        for ( int ri = 0; ri < rows; ri++ ) {
            double[] row = data[activeRows[ri]];
            double sum = 0;
            int n = 0;
            for ( double v : row ) {
                if ( !Double.isNaN( v ) ) {
                    sum += v;
                    n++;
                }
            }
            if ( n == cols ) {
                continue;
            }
            double mean = sum / n;
            BitSet m = new BitSet( cols );
            for ( int j = 0; j < cols; j++ ) {
                if ( Double.isNaN( row[j] ) ) {
                    m.set( j );
                    row[j] = mean;
                }
            }
            missing[ri] = m;
        }

        // Reference distribution: the mean, at each rank, of the sorted contributing columns. Accumulating one
        // column at a time adds the same values in the same order as Descriptive.mean over a row of the sorted
        // matrix.
        double[] columnBuffer = new double[rows];
        DoubleArrayList sortedColumn = new DoubleArrayList( columnBuffer );
        double[] reference = new double[rows];
        for ( int j = 0; j < cols; j++ ) {
            if ( includeInReference != null && !includeInReference[j] ) {
                continue;
            }
            for ( int ri = 0; ri < rows; ri++ ) {
                columnBuffer[ri] = data[activeRows[ri]][j];
            }
            sortedColumn.sort();
            for ( int ri = 0; ri < rows; ri++ ) {
                reference[ri] += columnBuffer[ri];
            }
        }
        for ( int ri = 0; ri < rows; ri++ ) {
            reference[ri] /= contributingColumns;
        }

        // Map every column, contributing or not, onto the reference by rank.
        for ( int j = 0; j < cols; j++ ) {
            for ( int ri = 0; ri < rows; ri++ ) {
                columnBuffer[ri] = data[activeRows[ri]][j];
            }
            // a fresh list: the buffer is refilled for the next column, and rankTransform's treatment of the list
            // it is handed is not part of its contract
            DoubleArrayList ranks = Rank.rankTransform( new DoubleArrayList( columnBuffer.clone() ) );
            assert ranks != null;
            for ( int ri = 0; ri < rows; ri++ ) {
                double rank = ranks.get( ri ) - 1.0;
                int intrank = ( int ) Math.floor( rank );
                double value;
                if ( rank - intrank > 0.4 && intrank > 0 ) {
                    // tied ranks, same as MatrixNormalizer: averaged with the rank BELOW. 0.4 is the threshold R uses.
                    value = ( reference[intrank] + reference[intrank - 1] ) / 2.0;
                } else {
                    value = reference[intrank];
                }
                data[activeRows[ri]][j] = value;
            }
        }

        // mask the missing values again
        for ( int ri = 0; ri < rows; ri++ ) {
            BitSet m = missing[ri];
            if ( m == null ) {
                continue;
            }
            double[] row = data[activeRows[ri]];
            for ( int j = m.nextSetBit( 0 ); j >= 0; j = m.nextSetBit( j + 1 ) ) {
                row[j] = Double.NaN;
            }
        }
    }
}
