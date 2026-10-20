/*
 * The baseCode project
 *
 * Copyright (c) 2008-2019 University of British Columbia
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
package ubic.gemma.core.util.math.linalg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.fommil.netlib.ARPACK;
import no.uib.cipr.matrix.NotConvergedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.netlib.util.doubleW;
import org.netlib.util.intW;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrix;

/**
 * SVD for DoubleMatrix.
 * <p>
 * The constructors compute the full decomposition, with Colt's algorithm ({@link JamaSingularValueDecomposition}).
 * {@link #truncated} computes only the leading components, as the largest eigenpairs of XᵀX found by ARPACK without
 * forming XᵀX, which is much faster when only a few are needed.
 * <p>
 * Both stop when their thread is interrupted, so a cancelled or timed-out decomposition does not keep running.
 *
 * @author  paul
 * 
 */
public class SingularValueDecomposition<R, C> {

    private static final Log log = LogFactory.getLog( SingularValueDecomposition.class );

    private static final int MAX_COMPUTE_TIME = 60 * 1000 * 120; // millis

    /**
     * How long a timed-out full decomposition may take to notice the interrupt before we give up waiting for it; it
     * checks once per step, which takes well under a second even for tens of thousands of samples.
     */
    private static final long STOP_WAIT_SECONDS = 60;

    /**
     * Implicit restarts ARPACK may take before {@link #truncated} reports non-convergence.
     */
    private static final int MAX_ARPACK_ITERATIONS = 1000;

    static final String WORKER_THREAD_PREFIX = "gemma-svd-";

    private static final AtomicInteger workerThreadCount = new AtomicInteger();

    private List<C> columnNames;
    private List<R> rowNames;
    private DoubleMatrix<Integer, Integer> sMatrix;

    private DoubleMatrix<R, Integer> uMatrix;

    private DoubleMatrix<C, Integer> vMatrix;

    private double[] singularValues;

    private double totalSumOfSquares;

    private boolean truncated;

    /**
     * @param matrix
     * @throws TimeoutException if the decomposition does not finish within two hours
     */
    public SingularValueDecomposition( DoubleMatrix<R, C> matrix ) throws TimeoutException {
        this( matrix, MAX_COMPUTE_TIME );
    }

    /**
     * @param matrix
     * @param maxComputeTimeMillis how long the decomposition may run before it is stopped
     * @throws TimeoutException if the decomposition does not finish within {@code maxComputeTimeMillis}
     */
    public SingularValueDecomposition( DoubleMatrix<R, C> matrix, long maxComputeTimeMillis ) throws TimeoutException {
        this.rowNames = matrix.getRowNames();
        this.columnNames = matrix.getColNames();

        double[][] mat = matrix.getRawMatrix();
        JamaSingularValueDecomposition svd = computeSVD( mat, maxComputeTimeMillis );

        List<Integer> componentIds = new ArrayList<>();

        if ( rowNames.size() == 0 ) { // sanity check
            throw new IllegalStateException( "No row names!" );
        }

        for ( int i = 0; i < matrix.columns(); i++ ) {
            componentIds.add( i );
        }

        this.uMatrix = new DenseDoubleMatrix<>( svd.getU() );
        uMatrix.setRowNames( this.rowNames );
        uMatrix.setColumnNames( componentIds );

        this.vMatrix = new DenseDoubleMatrix<>( svd.getV() );
        vMatrix.setRowNames( this.columnNames );
        vMatrix.setColumnNames( componentIds );

        this.singularValues = svd.getSingularValues();
        int n = matrix.columns();
        double[][] s = new double[n][n];
        for ( int i = 0; i < n; i++ ) {
            s[i][i] = singularValues[i];
        }
        this.sMatrix = new DenseDoubleMatrix<>( s );
        sMatrix.setRowNames( componentIds );
        sMatrix.setColumnNames( componentIds );

        for ( double d : singularValues ) {
            this.totalSumOfSquares += d * d;
        }
    }

    private SingularValueDecomposition() {
    }

    /**
     * Compute only the {@code numComponents} leading components.
     * <p>
     * With X = U S Vᵀ, XᵀX = V S² Vᵀ: the k largest eigenpairs of XᵀX give the leading singular values and the columns
     * of V, and u_i = X v_i / s_i. ARPACK only ever asks for XᵀX times a vector, which is computed as Xᵀ(Xv), so XᵀX
     * is never formed: each step costs 2 × rows × columns, and memory beyond X is a few vectors. U is rows × k, V is
     * columns × k and S is k × k.
     * <p>
     * {@link #getTotalSumOfSquares()} is ‖X‖_F², the sum of all s², so variance fractions of the leading components
     * stay exact.
     * <p>
     * Runs on the calling thread with no time limit: ARPACK either converges or gives up after a bounded number of
     * iterations. Interrupting the thread stops it within one step.
     *
     * @param numComponents how many components to compute, at least 1 and fewer than the number of columns
     * @throws NotConvergedException if ARPACK does not converge, or reports an error
     * @throws InterruptedException  if the calling thread is interrupted
     */
    public static <R, C> SingularValueDecomposition<R, C> truncated( DoubleMatrix<R, C> matrix, int numComponents )
            throws NotConvergedException, InterruptedException {
        if ( numComponents < 1 || numComponents >= matrix.columns() ) {
            throw new IllegalArgumentException( "numComponents must be between 1 and " + ( matrix.columns() - 1 )
                    + " for a matrix with " + matrix.columns() + " columns, got " + numComponents + "." );
        }
        if ( matrix.getRowNames().isEmpty() ) { // sanity check
            throw new IllegalStateException( "No row names!" );
        }
        double[][] x = matrix.getRawMatrix();
        int rows = x.length, columns = x[0].length;

        Eigenpairs eig = largestEigenpairs( new GramOperator( x ), numComponents );

        double[] s = new double[numComponents];
        for ( int c = 0; c < numComponents; c++ ) {
            s[c] = Math.sqrt( Math.max( eig.values[c], 0 ) );
        }
        double[][] u = new double[rows][numComponents];
        for ( int g = 0; g < rows; g++ ) {
            for ( int c = 0; c < numComponents; c++ ) {
                if ( s[c] == 0 ) {
                    continue; // a null direction of X; its left singular vector is undetermined
                }
                double acc = 0;
                for ( int j = 0; j < columns; j++ ) {
                    acc += x[g][j] * eig.vectors[j][c];
                }
                u[g][c] = acc / s[c];
            }
        }

        SingularValueDecomposition<R, C> result = new SingularValueDecomposition<>();
        result.truncated = true;
        result.rowNames = matrix.getRowNames();
        result.columnNames = matrix.getColNames();

        List<Integer> componentIds = new ArrayList<>();
        for ( int i = 0; i < numComponents; i++ ) {
            componentIds.add( i );
        }

        result.uMatrix = new DenseDoubleMatrix<>( u );
        result.uMatrix.setRowNames( result.rowNames );
        result.uMatrix.setColumnNames( componentIds );

        result.singularValues = s;
        double[][] sm = new double[numComponents][numComponents];
        for ( int i = 0; i < numComponents; i++ ) {
            sm[i][i] = s[i];
        }
        result.sMatrix = new DenseDoubleMatrix<>( sm );
        result.sMatrix.setRowNames( componentIds );
        result.sMatrix.setColumnNames( componentIds );

        result.vMatrix = new DenseDoubleMatrix<>( eig.vectors );
        result.vMatrix.setRowNames( result.columnNames );
        result.vMatrix.setColumnNames( componentIds );

        for ( double[] row : x ) {
            for ( double d : row ) {
                result.totalSumOfSquares += d * d;
            }
        }
        return result;
    }

    /**
     * @return the condition number of the matrix
     * @throws UnsupportedOperationException if the decomposition is truncated
     */
    public double cond() {
        requireFull( "cond" );
        return singularValues[0] / singularValues[Math.min( rowNames.size(), columnNames.size() ) - 1];
    }

    /**
     * @return
     */
    public DoubleMatrix<Integer, Integer> getS() {
        return this.sMatrix;
    }

    /**
     * @return the singular values, largest first; only the leading ones if the decomposition is truncated
     */
    public double[] getSingularValues() {
        return singularValues;
    }

    /**
     * @return the sum of all squared singular values, which is ‖X‖_F², whether or not the decomposition is truncated
     */
    public double getTotalSumOfSquares() {
        return totalSumOfSquares;
    }

    /**
     * @return true if only the leading components were computed
     */
    public boolean isTruncated() {
        return truncated;
    }

    /**
     * @return
     */
    public DoubleMatrix<R, Integer> getU() {
        return this.uMatrix;

    }

    /**
     * @return
     */
    public DoubleMatrix<C, Integer> getV() {
        return this.vMatrix;
    }

    /**
     * @return the largest singular value
     */
    public double norm2() {
        return singularValues[0];
    }

    /**
     * @return the numerical rank, as Colt computes it
     * @throws UnsupportedOperationException if the decomposition is truncated
     */
    public int rank() {
        requireFull( "rank" );
        double tol = Math.max( rowNames.size(), columnNames.size() ) * singularValues[0] * Math.pow( 2.0, -52.0 );
        int r = 0;
        for ( double d : singularValues ) {
            if ( d > tol ) {
                r++;
            }
        }
        return r;
    }

    @Override
    public String toString() {
        return ( truncated ? "Truncated SVD" : "SVD" ) + " of a " + rowNames.size() + " × " + columnNames.size()
                + " matrix, " + singularValues.length + " components";
    }

    private void requireFull( String what ) {
        if ( truncated ) {
            throw new UnsupportedOperationException( what + " needs every singular value; this decomposition is truncated." );
        }
    }

    /**
     * Run the full decomposition on a worker thread so a wall-clock limit applies. On timeout the worker is
     * interrupted and, since the decomposition checks for that, stops; we wait for it so it does not outlive the
     * call. The executor MUST be shut down on every path (success, timeout, exception) — an earlier implementation
     * leaked a thread per call because it never invoked shutdown().
     */
    private static JamaSingularValueDecomposition computeSVD( double[][] mat, long maxComputeTimeMillis )
            throws TimeoutException {
        ExecutorService executor = Executors.newSingleThreadExecutor( r -> {
            Thread t = new Thread( r, WORKER_THREAD_PREFIX + workerThreadCount.incrementAndGet() );
            t.setDaemon( true );
            return t;
        } );
        try {
            Future<JamaSingularValueDecomposition> future = executor.submit(
                    ( Callable<JamaSingularValueDecomposition> ) () -> new JamaSingularValueDecomposition( mat ) );
            try {
                return future.get( maxComputeTimeMillis, TimeUnit.MILLISECONDS );
            } catch ( TimeoutException te ) {
                future.cancel( true );
                awaitStop( executor );
                throw new TimeoutException( "SVD did not finish within " + maxComputeTimeMillis + " ms" );
            } catch ( InterruptedException e ) {
                future.cancel( true );
                Thread.currentThread().interrupt();
                throw new RuntimeException( e );
            } catch ( ExecutionException e ) {
                throw new RuntimeException( e );
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static void awaitStop( ExecutorService executor ) {
        executor.shutdownNow();
        try {
            if ( !executor.awaitTermination( STOP_WAIT_SECONDS, TimeUnit.SECONDS ) ) {
                log.warn( "Timed-out SVD did not stop within " + STOP_WAIT_SECONDS + " s of being interrupted." );
            }
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * y = A x for a symmetric n × n matrix A that need not be stored.
     */
    interface SymmetricOperator {

        int size();

        /**
         * Write A·in[inOffset … inOffset + n) to out[outOffset … outOffset + n); the two ranges do not overlap.
         */
        void apply( double[] in, int inOffset, double[] out, int outOffset );
    }

    /**
     * XᵀX applied as Xᵀ(Xv), for X stored by rows.
     */
    static final class GramOperator implements SymmetricOperator {

        private final double[][] x;
        private final double[] xv;

        GramOperator( double[][] x ) {
            this.x = x;
            this.xv = new double[x.length];
        }

        @Override
        public int size() {
            return x[0].length;
        }

        @Override
        public void apply( double[] in, int inOffset, double[] out, int outOffset ) {
            int n = size();
            for ( int g = 0; g < x.length; g++ ) {
                double[] row = x[g];
                double acc = 0;
                for ( int j = 0; j < n; j++ ) {
                    acc += row[j] * in[inOffset + j];
                }
                xv[g] = acc;
            }
            Arrays.fill( out, outOffset, outOffset + n, 0.0 );
            for ( int g = 0; g < x.length; g++ ) {
                double[] row = x[g];
                double t = xv[g];
                for ( int j = 0; j < n; j++ ) {
                    out[outOffset + j] += t * row[j];
                }
            }
        }
    }

    /**
     * The k largest eigenpairs of a symmetric operator, largest first.
     */
    static final class Eigenpairs {
        final double[] values;
        /**
         * n × k, one eigenvector per column.
         */
        final double[][] vectors;

        private Eigenpairs( double[] values, double[][] vectors ) {
            this.values = values;
            this.vectors = vectors;
        }
    }

    /**
     * The {@code k} largest eigenpairs of {@code op}, by ARPACK's implicitly restarted Lanczos method (dsaupd/dseupd)
     * in reverse communication: ARPACK returns to this loop whenever it needs the operator applied to a vector. That
     * is where the interrupt is checked.
     * <p>
     * The tolerance is machine precision, so every requested eigenpair converges fully. The starting vector is
     * ARPACK's own, generated from a fixed seed, so results are reproducible.
     *
     * @throws NotConvergedException if ARPACK reaches {@link #MAX_ARPACK_ITERATIONS}, converges fewer than {@code k}
     *                               eigenpairs, or reports an error
     * @throws InterruptedException  if the calling thread is interrupted
     */
    static Eigenpairs largestEigenpairs( SymmetricOperator op, int k ) throws NotConvergedException, InterruptedException {
        int n = op.size();
        if ( k < 1 || k >= n ) {
            throw new IllegalArgumentException( "k must be between 1 and " + ( n - 1 ) + ", got " + k + "." );
        }
        ARPACK arpack = ARPACK.getInstance();
        int ncv = Math.min( Math.max( 2 * k + 1, 20 ), n );
        intW ido = new intW( 0 );
        intW info = new intW( 0 );
        doubleW tol = new doubleW( 0.0 ); // machine precision
        double[] resid = new double[n];
        double[] v = new double[n * ncv];
        double[] workd = new double[3 * n];
        int lworkl = ncv * ( ncv + 8 );
        double[] workl = new double[lworkl];
        int[] iparam = new int[11];
        iparam[0] = 1; // exact shifts
        iparam[2] = MAX_ARPACK_ITERATIONS;
        iparam[6] = 1; // mode 1: standard eigenproblem A x = λ x
        int[] ipntr = new int[11];

        while ( true ) {
            if ( Thread.interrupted() ) {
                throw new InterruptedException( "SVD interrupted" );
            }
            arpack.dsaupd( ido, "I", n, "LA", k, tol, resid, ncv, v, n, iparam, ipntr, workd, workl, lworkl, info );
            if ( ido.val != -1 && ido.val != 1 ) {
                break;
            }
            op.apply( workd, ipntr[0] - 1, workd, ipntr[1] - 1 );
        }
        if ( info.val == 1 ) {
            throw new NotConvergedException( NotConvergedException.Reason.Iterations, "ARPACK did not converge within "
                    + MAX_ARPACK_ITERATIONS + " iterations; " + iparam[4] + " of " + k + " eigenpairs converged." );
        } else if ( info.val != 0 ) {
            throw new NotConvergedException( NotConvergedException.Reason.Breakdown, "ARPACK dsaupd failed, info = " + info.val + "." );
        }
        if ( iparam[4] < k ) {
            throw new NotConvergedException( NotConvergedException.Reason.Iterations,
                    "ARPACK converged " + iparam[4] + " of " + k + " eigenpairs." );
        }

        double[] d = new double[k];
        double[] z = new double[n * k];
        arpack.dseupd( true, "A", new boolean[ncv], d, z, n, 0.0, "I", n, "LA", new intW( k ), tol.val, resid, ncv, v,
                n, iparam, ipntr, workd, workl, lworkl, info );
        if ( info.val != 0 ) {
            throw new NotConvergedException( NotConvergedException.Reason.Breakdown, "ARPACK dseupd failed, info = " + info.val + "." );
        }

        // dseupd returns the eigenvalues in ascending order, each vector a column of z (column-major, n rows)
        Integer[] order = new Integer[k];
        for ( int i = 0; i < k; i++ ) {
            order[i] = i;
        }
        Arrays.sort( order, Comparator.comparingDouble( ( Integer i ) -> d[i] ).reversed() );
        double[] values = new double[k];
        double[][] vectors = new double[n][k];
        for ( int c = 0; c < k; c++ ) {
            int src = order[c];
            values[c] = d[src];
            for ( int j = 0; j < n; j++ ) {
                vectors[j][c] = z[src * n + j];
            }
        }
        return new Eigenpairs( values, vectors );
    }
}
