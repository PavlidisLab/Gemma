package ubic.gemma.core.analysis.preprocess;

import org.junit.jupiter.api.Test;
import org.springframework.lang.Nullable;
import ubic.gemma.core.analysis.preprocess.normalize.QuantileNormalizer;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrixReader;

import java.util.Arrays;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;

/**
 * {@link QuantileNormalizer#normalizeInPlace(double[][], boolean[])} must give exactly the values of
 * {@link QuantileNormalizer#normalize(DoubleMatrix, boolean[])}, with and without reference columns and with missing
 * values.
 * <p>
 * Exactly means bit for bit: the arrays are compared with {@link java.util.Arrays#deepEquals}, which compares doubles
 * as {@link Double#equals}, so a NaN must be a NaN, and {@code -0.0} and {@code 0.0} are different values. The in-place
 * implementation performs the same floating-point operations in the same order as the matrix one, so there is no
 * tolerance to justify.
 */
public class QuantileNormalizeInPlaceTest {

    @Test
    public void matchesTheMatrixNormalizerOnTheFixture() throws Exception {
        double[][] data = readFixture();
        assertMatchesMatrixNormalizer( data, null );
        // the value QuantileNormalizerTest pins, reached through the in-place path
        double[][] normalized = deepCopy( data );
        QuantileNormalizer.normalizeInPlace( normalized, null );
        assertThat( normalized[0][9] ).isCloseTo( -0.525, offset( 0.001 ) );
    }

    @Test
    public void matchesTheMatrixNormalizerOnTheFixtureWithReferenceColumns() throws Exception {
        double[][] data = readFixture();
        boolean[] reference = new boolean[data[0].length];
        for ( int j = 0; j < reference.length; j++ ) {
            reference[j] = j % 3 != 1;
        }
        assertMatchesMatrixNormalizer( data, reference );
    }

    /**
     * The outlier case the processed-vector pipeline produces: the outlier columns are masked (all NaN) and excluded
     * from the reference, other cells are missing here and there, and one row has no value at all.
     */
    @Test
    public void matchesTheMatrixNormalizerOnTheFixtureWithMaskedOutliersAndMissingValues() throws Exception {
        double[][] data = readFixture();
        int cols = data[0].length;
        boolean[] reference = new boolean[cols];
        Arrays.fill( reference, true );
        for ( int outlier : new int[] { 2, 7 } ) {
            reference[outlier] = false;
            for ( double[] row : data ) {
                row[outlier] = Double.NaN;
            }
        }
        data[3][0] = Double.NaN;
        data[5][4] = Double.NaN;
        data[5][10] = Double.NaN;
        Arrays.fill( data[8], Double.NaN );
        assertMatchesMatrixNormalizer( data, reference );
        assertMatchesMatrixNormalizer( data, null );
    }

    @Test
    public void restoresMissingValuesAfterImputingThemForTheRanking() {
        double[][] data = {
                { 1.0, 4.0, 7.0 },
                { 2.0, Double.NaN, 8.0 },
                { 3.0, 6.0, 9.0 },
                { 4.0, 5.0, 10.0 },
        };
        assertMatchesMatrixNormalizer( data, null );
        assertMatchesMatrixNormalizer( data, new boolean[] { true, false, true } );
        double[][] normalized = deepCopy( data );
        QuantileNormalizer.normalizeInPlace( normalized, null );
        assertThat( normalized[1][1] ).isNaN();
    }

    /**
     * The matrix path drops a row with no value from its result, and the caller used to leave such a vector as it
     * was. The in-place path must not touch it either.
     */
    @Test
    public void leavesRowsWithNoValueUntouched() {
        double[] empty = { Double.NaN, Double.NaN, Double.NaN };
        double[][] data = {
                { 1.0, 4.0, 7.0 },
                empty,
                { 3.0, 6.0, 9.0 },
                { 4.0, 5.0, 10.0 },
        };
        QuantileNormalizer.normalizeInPlace( data, null );
        assertThat( data[1] ).isSameAs( empty );
        assertThat( empty ).containsOnly( Double.NaN );

        double[][] allEmpty = { { Double.NaN, Double.NaN }, { Double.NaN, Double.NaN } };
        QuantileNormalizer.normalizeInPlace( allEmpty, null );
        assertThat( allEmpty ).isEqualTo( new double[][] { { Double.NaN, Double.NaN }, { Double.NaN, Double.NaN } } );
    }

    /**
     * Master's version of this test pinned a divergence on columns mixing {@code -0.0} and {@code 0.0}, which came
     * from sorting with {@link java.util.Arrays#sort(double[])} (which orders {@code -0.0} first) rather than with
     * colt's sort. Sorting the same way as the matrix path removes it.
     */
    @Test
    public void matchesTheMatrixNormalizerOnSignedZeros() {
        int rows = 5000;
        double[][] data = new double[rows][2];
        Random rng = new Random( 42L );
        for ( int i = 0; i < rows; i++ ) {
            data[i][0] = rng.nextGaussian();
            if ( i % 7 == 0 ) {
                data[i][1] = ( i % 2 == 0 ) ? 0.0 : -0.0;
            } else {
                data[i][1] = rng.nextGaussian();
            }
        }
        assertMatchesMatrixNormalizer( data, null );
        assertMatchesMatrixNormalizer( data, new boolean[] { false, true } );
    }

    /**
     * Many small matrices of varied shape, tie density, missing-value density and reference subsets, including masked
     * columns, rows with no value and signed zeros.
     */
    @Test
    public void matchesTheMatrixNormalizerOnRandomInputs() {
        Random rng = new Random( 0xC0FFEEBEEFL );
        for ( int trial = 0; trial < 1000; trial++ ) {
            int rows = 3 + rng.nextInt( 40 );
            int cols = 2 + rng.nextInt( 12 );
            int regime = trial % 5;
            double nanProb = regime == 1 ? 0.1 : regime == 3 ? 0.3 : 0.0;
            boolean injectTies = regime == 2 || regime == 3;
            boolean injectSignedZeros = regime == 4;
            double[][] data = generateMatrix( rng, rows, cols, nanProb, injectTies, injectSignedZeros );

            boolean[] reference = null;
            if ( trial % 2 == 1 ) {
                reference = new boolean[cols];
                boolean any = false;
                for ( int j = 0; j < cols; j++ ) {
                    reference[j] = rng.nextDouble() < 0.7;
                    any |= reference[j];
                }
                if ( !any ) {
                    reference[rng.nextInt( cols )] = true;
                }
                // a masked outlier: all NaN and excluded
                if ( trial % 6 == 1 ) {
                    for ( int j = 0; j < cols; j++ ) {
                        if ( !reference[j] ) {
                            for ( double[] row : data ) {
                                row[j] = Double.NaN;
                            }
                            break;
                        }
                    }
                }
            }
            // an occasional row with no value
            if ( trial % 7 == 0 ) {
                Arrays.fill( data[rng.nextInt( rows )], Double.NaN );
            }

            if ( countRowsWithAValue( data ) == 0 ) {
                continue;
            }
            assertMatchesMatrixNormalizer( data, reference );
        }
    }

    @Test
    public void rejectsTheSameReferenceColumnsAsTheMatrixNormalizer() {
        double[][] data = { { 1.0, 2.0 }, { 3.0, 4.0 } };
        assertThatThrownBy( () -> QuantileNormalizer.normalizeInPlace( deepCopy( data ), new boolean[] { false, false } ) )
                .isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> QuantileNormalizer.normalizeInPlace( deepCopy( data ), new boolean[] { true } ) )
                .isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> QuantileNormalizer.normalizeInPlace( new double[][] { { 1.0, 2.0 }, { 3.0 } }, null ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    /**
     * Normalize a copy of the data both ways and require identical results. Rows the matrix path drops (no value at
     * all) must come out of the in-place path unchanged.
     */
    private static void assertMatchesMatrixNormalizer( double[][] data, @Nullable boolean[] reference ) {
        DoubleMatrix<Integer, Integer> matrix = new DenseDoubleMatrix<>( data.length, data[0].length );
        for ( int i = 0; i < data.length; i++ ) {
            matrix.setRowName( i, i );
            for ( int j = 0; j < data[i].length; j++ ) {
                matrix.set( i, j, data[i][j] );
            }
        }
        for ( int j = 0; j < data[0].length; j++ ) {
            matrix.setColumnName( j, j );
        }
        DoubleMatrix<Integer, Integer> normalizedMatrix = new QuantileNormalizer<Integer, Integer>()
                .normalize( matrix, reference );

        // what the pipeline did with the matrix result: overwrite the rows it returned, keep the others
        double[][] expected = deepCopy( data );
        for ( int i = 0; i < normalizedMatrix.rows(); i++ ) {
            double[] row = expected[normalizedMatrix.getRowName( i )];
            for ( int j = 0; j < row.length; j++ ) {
                row[j] = normalizedMatrix.get( i, j );
            }
        }

        double[][] actual = deepCopy( data );
        QuantileNormalizer.normalizeInPlace( actual, reference );

        assertThat( actual ).isEqualTo( expected );
    }

    private static int countRowsWithAValue( double[][] data ) {
        int n = 0;
        for ( double[] row : data ) {
            for ( double v : row ) {
                if ( !Double.isNaN( v ) ) {
                    n++;
                    break;
                }
            }
        }
        return n;
    }

    private double[][] readFixture() throws Exception {
        DoubleMatrix<String, String> tester = new DoubleMatrixReader()
                .read( getClass().getResourceAsStream( "/data/testdata.txt" ) );
        double[][] out = new double[tester.rows()][tester.columns()];
        for ( int i = 0; i < tester.rows(); i++ ) {
            for ( int j = 0; j < tester.columns(); j++ ) {
                out[i][j] = tester.get( i, j );
            }
        }
        return out;
    }

    private static double[][] generateMatrix( Random rng, int rows, int cols, double nanProb, boolean injectTies, boolean injectSignedZeros ) {
        double[][] m = new double[rows][cols];
        double[] tiePool = null;
        if ( injectTies ) {
            int pool = 1 + rng.nextInt( Math.max( 1, ( rows * cols ) / 8 ) );
            tiePool = new double[pool];
            for ( int t = 0; t < pool; t++ ) {
                tiePool[t] = rng.nextGaussian() * 5.0;
            }
        }
        for ( int i = 0; i < rows; i++ ) {
            for ( int j = 0; j < cols; j++ ) {
                if ( rng.nextDouble() < nanProb ) {
                    m[i][j] = Double.NaN;
                } else if ( injectTies && rng.nextDouble() < 0.3 ) {
                    m[i][j] = tiePool[rng.nextInt( tiePool.length )];
                } else if ( injectSignedZeros && rng.nextDouble() < 0.1 ) {
                    m[i][j] = rng.nextBoolean() ? 0.0 : -0.0;
                } else {
                    m[i][j] = rng.nextGaussian() * 10.0;
                }
            }
        }
        return m;
    }

    private static double[][] deepCopy( double[][] src ) {
        double[][] out = new double[src.length][];
        for ( int i = 0; i < src.length; i++ ) {
            out[i] = src[i].clone();
        }
        return out;
    }
}
