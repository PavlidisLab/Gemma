package ubic.gemma.core.util.math.linalg;

import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ubic.gemma.core.util.math.MatrixStats;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrixReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link JamaSingularValueDecomposition} must be Colt's SVD, bit for bit; the only difference allowed is that it stops
 * when interrupted. Every case compares U, V and the singular values exactly (delta 0) against
 * {@link cern.colt.matrix.linalg.SingularValueDecomposition} on the same input.
 */
public class JamaSingularValueDecompositionTest {

    static Stream<Arguments> matrices() throws IOException {
        Random r = new Random( 42L );
        return Stream.of(
                Arguments.of( "monocyte fixture, double-standardized (200 × 59)", monocyteFixture() ),
                Arguments.of( "Gaussian, tall (300 × 120)", gaussian( r, 300, 120 ) ),
                Arguments.of( "Gaussian, square (80 × 80)", gaussian( r, 80, 80 ) ),
                Arguments.of( "Gaussian, one row more than columns (41 × 40)", gaussian( r, 41, 40 ) ),
                Arguments.of( "rank 5, with duplicated columns (200 × 60)", lowRank( r, 200, 60, 5 ) ),
                Arguments.of( "a zero column (100 × 30)", withZeroColumn( gaussian( r, 100, 30 ), 7 ) ),
                Arguments.of( "tied singular values (60 × 20)", tied( 60, 20 ) ),
                Arguments.of( "one column (50 × 1)", gaussian( r, 50, 1 ) ),
                Arguments.of( "all zeros (10 × 4)", new double[10][4] ),
                Arguments.of( "values spanning 1e-150 to 1e150 (60 × 20)", wideRange( r, 60, 20 ) ) );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("matrices")
    public void testIdenticalToColt( String label, double[][] x ) throws InterruptedException {
        double[][] input = copy( x );
        cern.colt.matrix.linalg.SingularValueDecomposition colt = new cern.colt.matrix.linalg.SingularValueDecomposition(
                new DenseDoubleMatrix2D( x ) );
        JamaSingularValueDecomposition ours = new JamaSingularValueDecomposition( x );

        assertArrayEquals( colt.getSingularValues(), ours.getSingularValues(), 0.0, "singular values" );
        assertMatrixEquals( colt.getU().toArray(), ours.getU(), "U" );
        assertMatrixEquals( colt.getV().toArray(), ours.getV(), "V" );
        assertMatrixEquals( input, x, "input must not be modified" );
    }

    @Test
    public void testRejectsMoreColumnsThanRows() {
        assertThrows( IllegalArgumentException.class, () -> new JamaSingularValueDecomposition( new double[3][4] ) );
    }

    /**
     * The interrupt is checked at the start of each phase step, so a thread interrupted before the call stops before
     * doing any work, and its interrupted status is consumed by the exception.
     */
    @Test
    public void testStopsWhenInterrupted() {
        Thread.currentThread().interrupt();
        assertThrows( InterruptedException.class,
                () -> new JamaSingularValueDecomposition( gaussian( new Random( 1L ), 50, 20 ) ) );
        assertFalse( Thread.interrupted() );
    }

    private static void assertMatrixEquals( double[][] expected, double[][] actual, String what ) {
        assertEquals( expected.length, actual.length, what + " rows" );
        for ( int i = 0; i < expected.length; i++ ) {
            assertArrayEquals( expected[i], actual[i], 0.0, what + " row " + i );
        }
    }

    private static double[][] monocyteFixture() throws IOException {
        DoubleMatrix<String, String> m;
        try ( InputStream is = JamaSingularValueDecompositionTest.class
                .getResourceAsStream( "/data/loader/aov.results-2-monocyte-data-bytime.bypat.data.sort" ) ) {
            m = new DoubleMatrixReader().read( is );
        }
        return MatrixStats.doubleStandardize( m ).getRawMatrix();
    }

    private static double[][] gaussian( Random r, int rows, int columns ) {
        double[][] x = new double[rows][columns];
        for ( double[] row : x ) {
            for ( int j = 0; j < columns; j++ ) {
                row[j] = r.nextGaussian();
            }
        }
        return x;
    }

    private static double[][] lowRank( Random r, int rows, int columns, int rank ) {
        double[][] a = gaussian( r, rows, rank ), b = gaussian( r, rank, columns );
        double[][] x = new double[rows][columns];
        for ( int i = 0; i < rows; i++ ) {
            for ( int j = 0; j < columns; j++ ) {
                for ( int c = 0; c < rank; c++ ) {
                    x[i][j] += a[i][c] * b[c][j];
                }
            }
            x[i][columns - 1] = x[i][0]; // an exact duplicate column
        }
        return x;
    }

    private static double[][] withZeroColumn( double[][] x, int column ) {
        for ( double[] row : x ) {
            row[column] = 0;
        }
        return x;
    }

    /**
     * Orthogonal columns of equal norm: every singular value is the same.
     */
    private static double[][] tied( int rows, int columns ) {
        double[][] x = new double[rows][columns];
        for ( int j = 0; j < columns; j++ ) {
            x[j][j] = 2.0;
            x[j + columns][j] = 2.0;
        }
        return x;
    }

    private static double[][] wideRange( Random r, int rows, int columns ) {
        double[][] x = gaussian( r, rows, columns );
        for ( int j = 0; j < columns; j++ ) {
            double scale = Math.pow( 10, -150 + 300.0 * j / ( columns - 1 ) );
            for ( double[] row : x ) {
                row[j] *= scale;
            }
        }
        return x;
    }

    private static double[][] copy( double[][] x ) {
        double[][] c = new double[x.length][];
        for ( int i = 0; i < x.length; i++ ) {
            c[i] = x[i].clone();
        }
        return c;
    }
}
