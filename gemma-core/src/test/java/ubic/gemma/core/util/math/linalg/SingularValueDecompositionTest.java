package ubic.gemma.core.util.math.linalg;

import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.util.math.MatrixStats;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class SingularValueDecompositionTest {

    private static final Log log = LogFactory.getLog( SingularValueDecompositionTest.class );

    /**
     * A decomposition that runs past its limit is reported as a {@link TimeoutException}, which callers can handle,
     * rather than an unchecked exception that escapes diagnostics error handling.
     */
    @Test
    public void testTimeoutIsReportedAsTimeoutException() {
        // a zero limit expires before any decomposition of this size can finish
        TimeoutException e = assertThrows( TimeoutException.class,
                () -> new SingularValueDecomposition<>( randomMatrix( 400, 200, 42L ), 0 ) );
        assertEquals( "SVD did not finish within 0 ms", e.getMessage() );
        assertNoSvdWorkerAlive();
    }

    /**
     * A timed-out decomposition stops instead of running to completion on its worker thread. Colt's SVD of this
     * matrix takes about two minutes on one core; stopped, the call returns within a fraction of a second of the
     * limit, and no worker is left behind.
     */
    @Test
    public void testTimedOutDecompositionStops() {
        DoubleMatrix<Integer, Integer> m = randomMatrix( 1500, 800, 7L );
        long start = System.nanoTime();
        assertThrows( TimeoutException.class, () -> new SingularValueDecomposition<>( m, 200 ) );
        long elapsedMs = ( System.nanoTime() - start ) / 1_000_000;
        assertTrue( elapsedMs < 30_000, "the timed-out SVD took " + elapsedMs + " ms to stop" );
        assertNoSvdWorkerAlive();
    }

    /**
     * The truncated path checks for an interrupt each time ARPACK hands back control, so it stops within one step.
     */
    @Test
    public void testTruncatedStopsWithinOneStepOfAnInterrupt() {
        double[][] x = randomMatrix( 300, 100, 3L ).getRawMatrix();
        SingularValueDecomposition.GramOperator gram = new SingularValueDecomposition.GramOperator( x );
        AtomicInteger calls = new AtomicInteger();
        SingularValueDecomposition.SymmetricOperator interruptingOnThirdCall = new SingularValueDecomposition.SymmetricOperator() {
            @Override
            public int size() {
                return gram.size();
            }

            @Override
            public void apply( double[] in, int inOffset, double[] out, int outOffset ) {
                gram.apply( in, inOffset, out, outOffset );
                if ( calls.incrementAndGet() == 3 ) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        assertThrows( InterruptedException.class,
                () -> SingularValueDecomposition.largestEigenpairs( interruptingOnThirdCall, 10 ) );
        assertEquals( 3, calls.get() );
        assertFalse( Thread.interrupted() );
    }

    @Test
    public void testTruncatedStopsWhenAlreadyInterrupted() {
        DoubleMatrix<Integer, Integer> m = randomMatrix( 100, 40, 5L );
        Thread.currentThread().interrupt();
        assertThrows( InterruptedException.class, () -> SingularValueDecomposition.truncated( m, 5 ) );
        assertFalse( Thread.interrupted() );
    }

    /**
     * On a diagonal operator the eigenpairs are known exactly: the largest values on the diagonal, and unit vectors.
     */
    @Test
    public void testLargestEigenpairsOfAKnownSpectrum() throws Exception {
        int n = 200, k = 10;
        double[] diagonal = new double[n];
        for ( int i = 0; i < n; i++ ) {
            diagonal[i] = ( i * 37 ) % n + 1; // 1 … n, shuffled
        }
        SingularValueDecomposition.Eigenpairs eig = SingularValueDecomposition.largestEigenpairs( diagonalOperator( diagonal ), k );
        for ( int c = 0; c < k; c++ ) {
            double expected = n - c;
            assertEquals( expected, eig.values[c], 1e-10 * n, "eigenvalue " + ( c + 1 ) );
            int where = indexOf( diagonal, expected );
            for ( int j = 0; j < n; j++ ) {
                assertEquals( j == where ? 1.0 : 0.0, Math.abs( eig.vectors[j][c] ), 1e-10,
                        "eigenvector " + ( c + 1 ) + ", entry " + j );
            }
        }
    }

    /**
     * Tied eigenvalues are all returned, with orthonormal eigenvectors spanning their eigenspace. (MTJ's ArpackSym
     * returned its eigenpairs in a map keyed by eigenvalue, so ties would have collapsed into one.)
     */
    @Test
    public void testLargestEigenpairsWithTiedEigenvalues() throws Exception {
        int n = 100;
        double[] diagonal = new double[n];
        for ( int i = 0; i < n; i++ ) {
            diagonal[i] = i < 3 ? 50.0 : 40.0 * ( n - i ) / n;
        }
        SingularValueDecomposition.Eigenpairs eig = SingularValueDecomposition.largestEigenpairs( diagonalOperator( diagonal ), 4 );
        for ( int c = 0; c < 3; c++ ) {
            assertEquals( 50.0, eig.values[c], 1e-10 * 50, "tied eigenvalue " + ( c + 1 ) );
            double inSpan = 0;
            for ( int j = 0; j < 3; j++ ) {
                inSpan += eig.vectors[j][c] * eig.vectors[j][c];
            }
            assertEquals( 1.0, inSpan, 1e-10, "eigenvector " + ( c + 1 ) + " lies in the tied eigenspace" );
            for ( int d = 0; d < 3; d++ ) {
                double dot = 0;
                for ( int j = 0; j < n; j++ ) {
                    dot += eig.vectors[j][c] * eig.vectors[j][d];
                }
                assertEquals( c == d ? 1.0 : 0.0, dot, 1e-10, "eigenvectors " + ( c + 1 ) + " and " + ( d + 1 ) );
            }
        }
        assertEquals( 40.0 * ( n - 3 ) / n, eig.values[3], 1e-10 * 50 );
    }

    /**
     * The truncated decomposition against Colt's full SVD itself. With ARPACK's tolerance at machine precision, every
     * one of the k components matches, not only the leading ones: singular values to 1e-12 relative, singular vectors
     * (up to sign) to 1e-9, even for noise, where neighbouring singular values are within 0.5% of each other.
     */
    @Test
    public void testTruncatedMatchesColt() throws Exception {
        for ( long seed : new long[] { 11L, 12L } ) {
            DoubleMatrix<Integer, Integer> x = MatrixStats.doubleStandardize( randomMatrix( 400, 100, seed ) );
            int k = 10;
            cern.colt.matrix.linalg.SingularValueDecomposition colt = new cern.colt.matrix.linalg.SingularValueDecomposition(
                    new DenseDoubleMatrix2D( x.getRawMatrix() ) );
            double[] sFull = colt.getSingularValues();
            double[][] uFull = colt.getU().toArray(), vFull = colt.getV().toArray();
            SingularValueDecomposition<Integer, Integer> top = SingularValueDecomposition.truncated( x, k );
            double[] s = top.getSingularValues();
            double[][] u = top.getU().getRawMatrix(), v = top.getV().getRawMatrix();
            double totalSq = 0;
            for ( double d : sFull ) {
                totalSq += d * d;
            }
            assertEquals( totalSq, top.getTotalSumOfSquares(), 1e-12 * totalSq );
            for ( int c = 0; c < k; c++ ) {
                double vDot = 0, uDot = 0;
                for ( int j = 0; j < v.length; j++ ) {
                    vDot += v[j][c] * vFull[j][c];
                }
                for ( int g = 0; g < u.length; g++ ) {
                    uDot += u[g][c] * uFull[g][c];
                }
                double sErr = Math.abs( s[c] - sFull[c] ) / sFull[c];
                log.info( String.format( "seed %d, PC %2d: relative gap %.2e, |Δs|/s = %.2e, 1 - |v·v'| = %.2e, 1 - |u·u'| = %.2e",
                        seed, c + 1, ( sFull[c] - sFull[c + 1] ) / sFull[c], sErr, 1 - Math.abs( vDot ), 1 - Math.abs( uDot ) ) );
                assertEquals( 0, sErr, 1e-12, "singular value " + ( c + 1 ) );
                assertEquals( 1, Math.abs( vDot ), 1e-9, "V column " + ( c + 1 ) );
                assertEquals( 1, Math.abs( uDot ), 1e-9, "U column " + ( c + 1 ) );
            }
        }
    }

    private static void assertNoSvdWorkerAlive() {
        for ( Thread t : Thread.getAllStackTraces().keySet() ) {
            assertFalse( t.isAlive() && t.getName().startsWith( SingularValueDecomposition.WORKER_THREAD_PREFIX ),
                    "SVD worker " + t.getName() + " is still running" );
        }
    }

    private static SingularValueDecomposition.SymmetricOperator diagonalOperator( double[] diagonal ) {
        return new SingularValueDecomposition.SymmetricOperator() {
            @Override
            public int size() {
                return diagonal.length;
            }

            @Override
            public void apply( double[] in, int inOffset, double[] out, int outOffset ) {
                for ( int j = 0; j < diagonal.length; j++ ) {
                    out[outOffset + j] = diagonal[j] * in[inOffset + j];
                }
            }
        };
    }

    private static int indexOf( double[] a, double value ) {
        for ( int i = 0; i < a.length; i++ ) {
            if ( a[i] == value ) {
                return i;
            }
        }
        throw new IllegalArgumentException( value + " not found" );
    }

    private static DoubleMatrix<Integer, Integer> randomMatrix( int rows, int columns, long seed ) {
        Random r = new Random( seed );
        DoubleMatrix<Integer, Integer> m = new DenseDoubleMatrix<>( rows, columns );
        List<Integer> rowNames = new ArrayList<>(), colNames = new ArrayList<>();
        for ( int i = 0; i < rows; i++ ) {
            rowNames.add( i );
            for ( int j = 0; j < columns; j++ ) {
                m.set( i, j, r.nextGaussian() );
            }
        }
        for ( int j = 0; j < columns; j++ ) {
            colNames.add( j );
        }
        m.setRowNames( rowNames );
        m.setColumnNames( colNames );
        return m;
    }
}
