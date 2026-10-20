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
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrix;

/**
 * SVD for DoubleMatrix.
 *
 * @author  paul
 * 
 */
public class SingularValueDecomposition<R, C> {

    private static final int MAX_COMPUTE_TIME = 60 * 1000 * 120; // millis
    private List<C> columnNames;
    private List<R> rowNames;
    private DoubleMatrix<Integer, Integer> sMatrix;

    private cern.colt.matrix.linalg.SingularValueDecomposition svd;

    private DoubleMatrix<R, Integer> uMatrix;

    private DoubleMatrix<Integer, C> vMatrix;

    /**
     * @param matrix
     */
    public SingularValueDecomposition( DoubleMatrix<R, C> matrix ) {
        double[][] mat = matrix.getRawMatrix();
        final DoubleMatrix2D dm = new DenseDoubleMatrix2D( mat );
        this.rowNames = matrix.getRowNames();
        this.columnNames = matrix.getColNames();

        computeSVD( dm );

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
        vMatrix.setRowNames( componentIds );
        vMatrix.setColumnNames( this.columnNames );

        this.sMatrix = new DenseDoubleMatrix<>( svd.getS().toArray() );
        sMatrix.setRowNames( componentIds );
        sMatrix.setColumnNames( componentIds );
    }

    /**
     * @return the condition number of the matrix
     * @see    cern.colt.matrix.linalg.SingularValueDecomposition#cond()
     */
    public double cond() {
        return svd.cond();
    }

    /**
     * @return
     * @see    cern.colt.matrix.linalg.SingularValueDecomposition#getS()
     */
    public DoubleMatrix<Integer, Integer> getS() {
        return this.sMatrix;
    }

    /**
     * @return
     * @see    cern.colt.matrix.linalg.SingularValueDecomposition#getSingularValues()
     */
    public double[] getSingularValues() {
        return svd.getSingularValues();
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
    public DoubleMatrix<Integer, C> getV() {
        return this.vMatrix;
    }

    /**
     * @return
     * @see    cern.colt.matrix.linalg.SingularValueDecomposition#norm2()
     */
    public double norm2() {
        return svd.norm2();
    }

    /**
     * @return
     * @see    cern.colt.matrix.linalg.SingularValueDecomposition#rank()
     */
    public int rank() {
        return svd.rank();
    }

    /**
     * @return
     * @see    cern.colt.matrix.linalg.SingularValueDecomposition#toString()
     */
    @Override
    public String toString() {
        return svd.toString();
    }

    /**
     * @param dm
     */
    private void computeSVD( final DoubleMatrix2D dm ) {
        /*
         * Colt's SVD occasionally fails to converge on degenerate matrices; run it on a worker thread so we can bail
         * via a wall-clock timeout. The executor MUST be shut down on every path (success, timeout, exception) — the
         * previous implementation leaked a thread per call because it never invoked shutdown().
         */
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<cern.colt.matrix.linalg.SingularValueDecomposition> svdFuture = executor.submit(
                    () -> new cern.colt.matrix.linalg.SingularValueDecomposition( dm ) );
            try {
                this.svd = svdFuture.get( MAX_COMPUTE_TIME, TimeUnit.MILLISECONDS );
            } catch ( TimeoutException te ) {
                svdFuture.cancel( true );
                throw new RuntimeException( "SVD failed to converge within " + MAX_COMPUTE_TIME + "ms, bailing", te );
            } catch ( InterruptedException e ) {
                svdFuture.cancel( true );
                Thread.currentThread().interrupt();
                throw new RuntimeException( e );
            } catch ( ExecutionException e ) {
                throw new RuntimeException( e );
            }
        } finally {
            executor.shutdownNow();
        }

        assert this.svd != null;
    }

}
