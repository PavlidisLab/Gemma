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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVectorSub;
import no.uib.cipr.matrix.NotConvergedException;
import no.uib.cipr.matrix.sparse.ArpackSym;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrix;

/**
 * SVD for DoubleMatrix.
 * <p>
 * The constructors compute the full decomposition with Colt. {@link #truncated} computes only the leading components,
 * from the largest eigenpairs of XᵀX (MTJ's {@link ArpackSym}), which is much faster when only a few are needed.
 *
 * @author  paul
 * 
 */
public class SingularValueDecomposition<R, C> {

    private static final int MAX_COMPUTE_TIME = 60 * 1000 * 120; // millis
    private List<C> columnNames;
    private List<R> rowNames;
    private DoubleMatrix<Integer, Integer> sMatrix;

    /**
     * The full Colt decomposition, or null if this one is {@link #isTruncated() truncated}.
     */
    private cern.colt.matrix.linalg.SingularValueDecomposition svd;

    private DoubleMatrix<R, Integer> uMatrix;

    private DoubleMatrix<C, Integer> vMatrix;

    private double[] singularValues;

    private double totalSumOfSquares;

    /**
     * @param matrix
     * @throws TimeoutException if the decomposition does not finish within two hours
     */
    public SingularValueDecomposition( DoubleMatrix<R, C> matrix ) throws TimeoutException {
        this( matrix, MAX_COMPUTE_TIME );
    }

    /**
     * @param matrix
     * @param maxComputeTimeMillis how long the decomposition may run before it is cancelled
     * @throws TimeoutException if the decomposition does not finish within {@code maxComputeTimeMillis}
     */
    public SingularValueDecomposition( DoubleMatrix<R, C> matrix, long maxComputeTimeMillis ) throws TimeoutException {
        double[][] mat = matrix.getRawMatrix();
        final DoubleMatrix2D dm = new DenseDoubleMatrix2D( mat );
        this.rowNames = matrix.getRowNames();
        this.columnNames = matrix.getColNames();

        computeSVD( dm, maxComputeTimeMillis );

        List<Integer> componentIds = new ArrayList<>();

        if ( rowNames.size() == 0 ) { // sanity check
            throw new IllegalStateException( "No row names!" );
        }

        for ( int i = 0; i < matrix.columns(); i++ ) {
            componentIds.add( i );
        }

        this.uMatrix = new DenseDoubleMatrix<>( svd.getU().toArray() );
        uMatrix.setRowNames( this.rowNames );
        uMatrix.setColumnNames( componentIds );

        this.vMatrix = new DenseDoubleMatrix<>( svd.getV().toArray() );
        vMatrix.setRowNames( this.columnNames );
        vMatrix.setColumnNames( componentIds );

        this.sMatrix = new DenseDoubleMatrix<>( svd.getS().toArray() );
        sMatrix.setRowNames( componentIds );
        sMatrix.setColumnNames( componentIds );

        this.singularValues = svd.getSingularValues();
        for ( double d : singularValues ) {
            this.totalSumOfSquares += d * d;
        }
    }

    private SingularValueDecomposition() {
    }

    /**
     * Compute only the {@code numComponents} leading components, with the default two-hour limit.
     *
     * @see #truncated(DoubleMatrix, int, long)
     */
    public static <R, C> SingularValueDecomposition<R, C> truncated( DoubleMatrix<R, C> matrix, int numComponents )
            throws TimeoutException, NotConvergedException {
        return truncated( matrix, numComponents, MAX_COMPUTE_TIME );
    }

    /**
     * Compute only the {@code numComponents} leading components.
     * <p>
     * With X = U S Vᵀ, XᵀX = V S² Vᵀ: the k largest eigenpairs of XᵀX give the leading singular values and the columns
     * of V, and u_i = X v_i / s_i. U is rows × k, V is columns × k and S is k × k. The leading components agree with
     * the full decomposition to rounding; the last one or two of the k converge last and are slightly less accurate
     * (around 1e-8 relative), so ask for a few more than you need.
     * <p>
     * {@link #getTotalSumOfSquares()} is ‖X‖_F², the sum of all s², so variance fractions of the leading components
     * stay exact.
     *
     * @param numComponents how many components to compute, at least 1 and fewer than the number of columns
     * @throws TimeoutException     if the decomposition does not finish within {@code maxComputeTimeMillis}
     * @throws NotConvergedException if ARPACK does not converge, or returns fewer than {@code numComponents}
     *                              distinct eigenvalues (it reports eigenpairs keyed by eigenvalue, so tied ones would
     *                              be lost)
     */
    public static <R, C> SingularValueDecomposition<R, C> truncated( DoubleMatrix<R, C> matrix, int numComponents,
            long maxComputeTimeMillis ) throws TimeoutException, NotConvergedException {
        if ( numComponents < 1 || numComponents >= matrix.columns() ) {
            throw new IllegalArgumentException( "numComponents must be between 1 and " + ( matrix.columns() - 1 )
                    + " for a matrix with " + matrix.columns() + " columns, got " + numComponents + "." );
        }
        if ( matrix.getRowNames().isEmpty() ) { // sanity check
            throw new IllegalStateException( "No row names!" );
        }
        double[][] x = matrix.getRawMatrix();
        SingularValueDecomposition<R, C> result = new SingularValueDecomposition<>();
        result.rowNames = matrix.getRowNames();
        result.columnNames = matrix.getColNames();
        double[][][] usv = runWithTimeout( () -> computeTopK( x, numComponents ), maxComputeTimeMillis );

        List<Integer> componentIds = new ArrayList<>();
        for ( int i = 0; i < numComponents; i++ ) {
            componentIds.add( i );
        }

        result.uMatrix = new DenseDoubleMatrix<>( usv[0] );
        result.uMatrix.setRowNames( result.rowNames );
        result.uMatrix.setColumnNames( componentIds );

        result.singularValues = usv[1][0];
        double[][] s = new double[numComponents][numComponents];
        for ( int i = 0; i < numComponents; i++ ) {
            s[i][i] = result.singularValues[i];
        }
        result.sMatrix = new DenseDoubleMatrix<>( s );
        result.sMatrix.setRowNames( componentIds );
        result.sMatrix.setColumnNames( componentIds );

        result.vMatrix = new DenseDoubleMatrix<>( usv[2] );
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
     * @see    cern.colt.matrix.linalg.SingularValueDecomposition#cond()
     */
    public double cond() {
        return requireFull( "cond" ).cond();
    }

    /**
     * @return
     * @see    cern.colt.matrix.linalg.SingularValueDecomposition#getS()
     */
    public DoubleMatrix<Integer, Integer> getS() {
        return this.sMatrix;
    }

    /**
     * @return the singular values, largest first; only the leading ones if the decomposition is truncated
     * @see    cern.colt.matrix.linalg.SingularValueDecomposition#getSingularValues()
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
        return svd == null;
    }

    /**
     * @return
     * @see    cern.colt.matrix.linalg.SingularValueDecomposition#getU()
     */
    public DoubleMatrix<R, Integer> getU() {
        return this.uMatrix;

    }

    /**
     * @return
     * @see    cern.colt.matrix.linalg.SingularValueDecomposition#getV()
     */
    public DoubleMatrix<C, Integer> getV() {
        return this.vMatrix;
    }

    /**
     * @return
     * @see    cern.colt.matrix.linalg.SingularValueDecomposition#norm2()
     */
    public double norm2() {
        return singularValues[0];
    }

    /**
     * @return
     * @throws UnsupportedOperationException if the decomposition is truncated
     * @see    cern.colt.matrix.linalg.SingularValueDecomposition#rank()
     */
    public int rank() {
        return requireFull( "rank" ).rank();
    }

    /**
     * @return
     * @see    cern.colt.matrix.linalg.SingularValueDecomposition#toString()
     */
    @Override
    public String toString() {
        if ( svd == null ) {
            return "Truncated SVD of a " + rowNames.size() + " × " + columnNames.size() + " matrix, "
                    + singularValues.length + " components";
        }
        return svd.toString();
    }

    private cern.colt.matrix.linalg.SingularValueDecomposition requireFull( String what ) {
        if ( svd == null ) {
            throw new UnsupportedOperationException( what + " needs every singular value; this decomposition is truncated." );
        }
        return svd;
    }

    /**
     * @param dm
     * @param maxComputeTimeMillis
     */
    private void computeSVD( final DoubleMatrix2D dm, long maxComputeTimeMillis ) throws TimeoutException {
        /*
         * Colt's SVD occasionally fails to converge on degenerate matrices; run it on a worker thread so we can bail
         * via a wall-clock timeout.
         */
        try {
            this.svd = runWithTimeout( () -> new cern.colt.matrix.linalg.SingularValueDecomposition( dm ),
                    maxComputeTimeMillis );
        } catch ( NotConvergedException e ) {
            throw new IllegalStateException( "Colt does not throw NotConvergedException", e );
        }

        assert this.svd != null;
    }

    /**
     * Run a decomposition on a worker thread so a wall-clock limit can stop waiting for it. The executor MUST be shut
     * down on every path (success, timeout, exception) — an earlier implementation leaked a thread per call because it
     * never invoked shutdown().
     */
    private static <T> T runWithTimeout( Callable<T> task, long maxComputeTimeMillis )
            throws TimeoutException, NotConvergedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<T> future = executor.submit( task );
            try {
                return future.get( maxComputeTimeMillis, TimeUnit.MILLISECONDS );
            } catch ( TimeoutException te ) {
                future.cancel( true );
                throw new TimeoutException( "SVD did not finish within " + maxComputeTimeMillis + " ms" );
            } catch ( InterruptedException e ) {
                future.cancel( true );
                Thread.currentThread().interrupt();
                throw new RuntimeException( e );
            } catch ( ExecutionException e ) {
                if ( e.getCause() instanceof NotConvergedException ) {
                    throw ( NotConvergedException ) e.getCause();
                }
                throw new RuntimeException( e );
            }
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * @return U (rows × k), the singular values (as the single row of a 1 × k array) and V (columns × k)
     */
    private static double[][][] computeTopK( double[][] x, int k ) throws NotConvergedException {
        int rows = x.length, columns = x[0].length;
        DenseMatrix xm = new DenseMatrix( x );
        DenseMatrix gram = new DenseMatrix( columns, columns );
        xm.transAmult( xm, gram );
        // ArpackSym rejects a matrix that is not exactly symmetric, and an optimized BLAS need not compute both
        // triangles of XᵀX identically
        for ( int i = 0; i < columns; i++ ) {
            for ( int j = 0; j < i; j++ ) {
                gram.set( i, j, gram.get( j, i ) );
            }
        }

        Map<Double, DenseVectorSub> eig;
        try {
            eig = new ArpackSym( gram ).solve( k, ArpackSym.Ritz.LA );
        } catch ( IllegalStateException e ) {
            // ArpackSym reports a nonzero ARPACK status this way
            throw new NotConvergedException( NotConvergedException.Reason.Iterations, e.getMessage() );
        }
        if ( eig.size() < k ) {
            throw new NotConvergedException( NotConvergedException.Reason.Breakdown, "ARPACK returned " + eig.size()
                    + " distinct eigenvalues of the " + k + " requested; tied eigenvalues cannot be told apart." );
        }
        List<Double> lambdas = new ArrayList<>( eig.keySet() );
        lambdas.sort( Comparator.reverseOrder() );

        double[] s = new double[k];
        double[][] v = new double[columns][k];
        for ( int c = 0; c < k; c++ ) {
            double lambda = lambdas.get( c );
            s[c] = Math.sqrt( Math.max( lambda, 0 ) );
            DenseVectorSub vec = eig.get( lambda );
            for ( int j = 0; j < columns; j++ ) {
                v[j][c] = vec.get( j );
            }
        }
        double[][] u = new double[rows][k];
        for ( int g = 0; g < rows; g++ ) {
            for ( int c = 0; c < k; c++ ) {
                if ( s[c] == 0 ) {
                    continue; // a null direction of X; its left singular vector is undetermined
                }
                double acc = 0;
                for ( int j = 0; j < columns; j++ ) {
                    acc += x[g][j] * v[j][c];
                }
                u[g][c] = acc / s[c];
            }
        }
        return new double[][][] { u, { s }, v };
    }

}
