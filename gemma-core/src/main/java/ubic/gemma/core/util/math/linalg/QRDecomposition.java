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
package ubic.gemma.core.util.math.linalg;

import cern.colt.list.IntArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.jet.math.Functions;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.Matrices;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.netlib.lapack.Dpotri;
import org.netlib.util.intW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix1D;
import ubic.gemma.core.util.matrix.MatrixUtil;

/**
 * QR with pivoting. See http://www.netlib.org/lapack/lug/node42.html and http://www.netlib.org/lapack/lug/node27.html,
 * and Golub and VanLoan, section 5.5.6+. Designed to mimic the way R does this by default.
 * 
 * @author paul
 */
public class QRDecomposition {

    private static Logger log = LoggerFactory.getLogger( QRDecomposition.class );

    private DenseDoubleMatrix2D chol2inv;

    private int[] jpvt;

    /**
     * Rows in input
     */
    private int n;

    /**
     * Columns in input
     */
    private int p;

    /**
     * If pivoting was used.
     */
    private boolean pivoting = true;

    /**
     * Contains the compact QR: R in the upper triangle, Q is recoverable from the lower part. FIXME if we have to
     * access this as a raw array the DoubleMatrix2D API is inefficient.
     */
    private DoubleMatrix2D QR;

    /**
     * Auxiliary information used to contsruct Q from the economy-sized QR.
     */
    private DoubleMatrix1D qraux;

    /**
     * Rank
     */
    private int rank = 0;

    /**
     * Diagonal of R
     */
    private DoubleMatrix1D Rdiag;

    /**
     * Used to decide when to pivot
     */
    private double tolerance = 1e-7;

    /**
     * 
     */
    private DoubleMatrix2D Qcached = null;

    private DoubleMatrix2D effects;

    /**
     * @param A the matrix to decompose, pivoting will be used.
     */
    public QRDecomposition( final DoubleMatrix2D A ) {
        this( A, true );
    }

    /**
     * Construct the QR decomposition of A. With pivoting = true, this reproduces quite closely the behaviour of R qr().
     * 
     * @param A the matrix to decompose
     * @param pivoting set to false to obtain standard QR behaviour.
     */
    public QRDecomposition( final DoubleMatrix2D A, boolean pivoting ) {
        // Initialize.
        this.QR = A.copy();
        this.n = A.rows();
        this.p = A.columns();
        this.Rdiag = A.like1D( p );
        this.pivoting = pivoting;

        // initialization
        jpvt = new int[p];
        for ( int i = 0; i < p; i++ ) {
            jpvt[i] = i;
        }

        // initialization. We always compute qraux here.
        DoubleMatrix1D originalNorms;
        originalNorms = new DenseDoubleMatrix1D( p ); // "work" in linpack
        qraux = new DenseDoubleMatrix1D( p );
        for ( int i = 0; i < p; i++ ) {
            DoubleMatrix1D col = QR.viewColumn( i );
            double norm2 = 0;
            for ( int r = 0; r < n; r++ ) {
                norm2 = hypot( norm2, col.getQuick( r ) );
            }
            qraux.set( i, norm2 );
            originalNorms.set( i, norm2 == 0.0 ? 1.0 : norm2 );
        }

        // precompute and cache some views to avoid regenerating them time and again
        DoubleMatrix1D[] QRcolumns = new DoubleMatrix1D[p];
        DoubleMatrix1D[] QRcolumnsPart = new DoubleMatrix1D[p];
        for ( int v = 0; v < p; v++ ) {
            QRcolumns[v] = QR.viewColumn( v );
            QRcolumnsPart[v] = QR.viewColumn( v ).viewPart( v, n - v ); // upper triangle.
        }

        rank = p;

        // Main loop.
        for ( int v = 0; v < p; v++ ) {

            /*
             * Rotate columns until we find one with a non-negligible norm. This is the pivoting strategy used in
             * dqrdc2, which puts small columns to the right. See R documentation for qr and
             * https://svn.r-project.org/R/trunk/src/appl/dqrdc2.f
             */
            while ( pivoting && v < rank && qraux.get( v ) < originalNorms.get( v ) * tolerance ) {
                log.debug( "Rotating " + v );
                rotate( QR, originalNorms, v );
            }

            DoubleMatrix1D colv = QRcolumns[v];
            double nrm = 0;
            for ( int i = v; i < n; i++ ) {
                nrm = hypot( nrm, colv.getQuick( i ) );
            }

            /*
             * "Householder reflections can be used to calculate QR decompositions by reflecting first one column of a
             * matrix onto a multiple of a standard basis vector, calculating the transformation matrix, multiplying it
             * with the original matrix and then recursing down the (i, i) minors of that product."
             */
            if ( nrm != 0.0 ) {
                // Form k-th Householder vector: scale and flip
                if ( QR.getQuick( v, v ) < 0 ) nrm = -nrm; // dsign
                QRcolumnsPart[v].assign( Functions.div( nrm ) ); // dscal

                QR.setQuick( v, v, QR.getQuick( v, v ) + 1.0 ); // update diagonal

                // Apply transformation to remaining columns.
                for ( int j = v + 1; j < p; j++ ) {
                    DoubleMatrix1D QRcolj = QR.viewColumn( j ).viewPart( v, n - v );
                    double s = QRcolumnsPart[v].zDotProduct( QRcolj );

                    s = -s / QR.getQuick( v, v );
                    for ( int i = v; i < n; i++ ) {
                        QR.setQuick( i, j, QR.getQuick( i, j ) + s * QR.getQuick( i, v ) );
                    }

                    if ( qraux.getQuick( j ) == 0 ) {
                        continue;
                    }

                    /*
                     * Update the norm of this column. Used even if we are not pivoting.
                     */
                    double tt = QR.getQuick( v, j ) / qraux.getQuick( j );
                    double t = Math.max( 1.0 - Math.pow( tt, 2 ), 0.0 );
                    if ( t < 1e-6 ) {
                        DoubleMatrix1D col = QR.viewColumn( j );
                        double nrmv = 0.0;
                        for ( int r = v + 1; r < n; r++ ) {
                            nrmv = hypot( nrmv, col.getQuick( r ) );
                        }
                        qraux.set( j, nrmv );
                    } else {
                        qraux.set( j, qraux.getQuick( j ) * Math.sqrt( t ) );
                    }
                }
                if ( log.isDebugEnabled() ) log.debug( qraux.toString() );

            }

            // save transformation parts we are done with.
            qraux.set( v, QR.getQuick( v, v ) );
            QR.setQuick( v, v, -nrm );
            Rdiag.setQuick( v, -nrm );
        }
        rank = Math.min( rank, n );
    }

    /**
     * Used for computing standard errors of parameter estimates for least squares; copies functionality of R chol2inv.
     * 
     * @return
     */
    public DoubleMatrix2D chol2inv() {
        // using "size" in dpotri doesn't work right.
        return dpotri( this.getR().viewPart( 0, 0, this.rank, this.rank ) );
    }

    /**
     * Compute effects matrix Q'y (as in Rb = Q'y).
     * 
     * <p>
     * "Tthe effects are the uncorrelated single-degree-of-freedom values obtained by projecting the data onto the
     * successive orthogonal subspaces generated by the QR decomposition during the fitting process. The first r (the
     * rank of the model) are associated with coefficients and the remainder span the space of residuals (but are not
     * associated with particular residuals)."
     * 
     * @param y vector Missing values are ignored, otherwise assumed to be of the right size
     * @return vector of effects - these are the projections of y into Q column space
     */
    public DoubleMatrix1D effects( DoubleMatrix1D y ) {

        double[] qty = new double[y.size()];
        double[] junk = new double[y.size()];
        ubic.gemma.core.util.math.linalg.Dqrsl.dqrsl_j( QR.toArray(), QR.rows(), QR.columns(), qraux.toArray(), MatrixUtil.removeMissingOrInfinite( y ).toArray(),
                junk, qty,
                junk, junk, junk, 1000 );
        return new DenseDoubleMatrix1D( qty );
    }

    /**
     * Compute effects matrix Q'y (as in Rb = Q'y)
     * 
     * @param y matrix of data, assumed to be of right size, missing values are not supported
     * @return matrix of effects - these are the projections of y's columns into Q column subspace associated with the
     *         parameters,
     *         so values are "effects" each basis vector on the data
     */
    public DoubleMatrix2D effects( DoubleMatrix2D y ) {
        double[][] efa = new double[y.columns()][y.rows()];
        for ( int i = 0; i < y.columns(); i++ ) {
            efa[i] = effects( y.viewColumn( i ) ).toArray();
        }
        return new DenseDoubleMatrix2D( efa ).viewDice();
    }

    /**
     * @return
     */
    public IntArrayList getPivotOrder() {
        return new IntArrayList( jpvt );
    }

    /**
     * Generates and returns the (economy-sized - first <tt>p</tt> columns only) orthogonal factor <tt>Q</tt>.
     * 
     * @return first <tt>p</tt> columns of <tt>Q</tt>
     */
    public DoubleMatrix2D getQ() {

        // For efficienty we do this... but really we should avoid directly getting Q.
        if ( this.Qcached != null ) return Qcached;

        DoubleMatrix2D Q = QR.like();

        for ( int i = 0; i < Q.columns(); i++ ) {
            Q.set( i, i, 1 );
        }

        for ( int jy = 0; jy < p; jy++ ) {

            DoubleMatrix1D y = Q.viewColumn( jy );

            for ( int jj = 1; jj <= p; jj++ ) {
                int j = p - jj;

                if ( qraux.get( j ) == 0.0 ) {
                    continue;
                }

                double temp = QR.get( j, j );
                QR.set( j, j, qraux.get( j ) );
                DoubleMatrix1D QRcolv = QR.viewColumn( j ).viewPart( j, n - j );
                DoubleMatrix1D Qcolj = y.viewPart( j, n - j );
                double s = QRcolv.zDotProduct( Qcolj );
                s = -s / QR.getQuick( j, j );

                Qcolj.assign( QRcolv, Functions.plusMult( s ) );

                QR.set( j, j, temp );
            }
        }
        this.Qcached = Q;
        return Q;

    }

    /**
     * @return
     */
    public DoubleMatrix1D getQraux() {
        return qraux;
    }

    /**
     * Returns the upper triangular factor, <tt>R</tt>.
     * 
     * @return <tt>R</tt>
     */
    public DoubleMatrix2D getR() {
        DoubleMatrix2D R = QR.like( p, p );
        for ( int i = 0; i < p; i++ ) {
            for ( int j = 0; j < p; j++ ) {
                if ( i < j )
                    R.setQuick( i, j, QR.getQuick( i, j ) );
                else if ( i == j )
                    R.setQuick( i, j, Rdiag.getQuick( i ) );
                else
                    R.setQuick( i, j, 0 );
            }
        }
        return R;
    }

    /**
     * @return rank
     */
    public int getRank() {
        return rank;
    }

    public double getTolerance() {
        return tolerance;
    }

    /**
     * Returns whether the matrix <tt>A</tt> has full rank.
     * 
     * @return true if <tt>R</tt>, and hence <tt>A</tt>, has full rank.
     */
    public boolean hasFullRank() {
        return rank == p;
    }

    /**
     * @return true if pivoting was used (just whether it was set; not whether any actual pivoting happened)
     */
    public boolean isPivoting() {
        return pivoting;
    }

    /**
     * Least squares solution of <tt>A*X = y</tt>; <tt>returns X</tt> using the stored QR decomposition of A.
     * 
     * @param y A matrix with as many rows as <tt>A</tt> and any number of columns. Least squares is fit for each column
     *        of y.
     * @return <tt>X</tt> that minimizes the two norm of <tt>Q*R*X - B</tt>.
     * @exception IllegalArgumentException if <tt>y.rows() != A.rows()</tt>.
     * @exception IllegalArgumentException if <tt>!this.hasFullRank()</tt> (<tt>A</tt> is rank deficient). However,
     *            rank-deficient cases are handled by pivoting, so if you are using pivoting you should not see this
     *            happening.
     */
    public DoubleMatrix2D solve( DoubleMatrix2D y ) {
        if ( y.rows() != n ) {
            throw new IllegalArgumentException( "Matrix row dimensions must agree." );
        }

        if ( !pivoting && !this.hasFullRank() ) {
            throw new IllegalArgumentException( "Matrix is rank deficient; try using pivoting" );
        }

        DoubleMatrix2D qTy = effects( y ); // FIXME we use this again later, but we recompute it. Try to cache it.

        // Solve R*X = Y => X = RinvY; backsubstitution
        for ( int k1 = rank - 1; k1 >= 0; k1-- ) {

            for ( int j = 0; j < y.columns(); j++ ) {
                qTy.setQuick( k1, j, qTy.getQuick( k1, j ) / Rdiag.getQuick( k1 ) );
            }
            for ( int i = 0; i < k1; i++ ) {
                // sum up to the parameter we've done.
                for ( int j = 0; j < y.columns(); j++ ) {
                    qTy.setQuick( i, j, qTy.getQuick( i, j ) - qTy.getQuick( k1, j ) * QR.getQuick( i, k1 ) );
                }
            }
        }

        /*
         * Drop coefficients we couldn't estimate. These will always be at the end, even if we pivoted ??????
         */
        if ( this.rank < this.p ) {
            for ( int i = rank; i < this.p; i++ ) {
                qTy.viewRow( i ).assign( Double.NaN );
            }
        }

        /*
         * Pad r1 back out to the full length p, and (if pivoted) in the right original order using jpvt
         */
        DoubleMatrix2D coeff = qTy.like( p, y.columns() );
        coeff.assign( Double.NaN );
        for ( int i = 0; i < this.rank; i++ ) {
            int piv = jpvt[i]; // where the value should go.
            for ( int j = 0; j < qTy.columns(); j++ ) {
                coeff.setQuick( piv, j, qTy.getQuick( i, j ) );
            }
        }

        return coeff;

    }

    /**
     * Returns a String with (propertyName, propertyValue) pairs. Useful for debugging or to quickly get the rough
     * picture.
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        String unknown = "Illegal operation or error: ";

        buf.append( "-----------------------------------------------------------------\n" );
        buf.append( "QRDecomposition(A) \n" );
        buf.append( "-----------------------------------------------------------------\n" );

        buf.append( "rank = " + rank );

        buf.append( "\n\nQ = " );
        try {
            buf.append( String.valueOf( this.getQ() ) );
        } catch ( IllegalArgumentException exc ) {
            buf.append( unknown + exc.getMessage() );
        }

        buf.append( "\n\nR = " );
        try {
            buf.append( String.valueOf( this.getR() ) );
        } catch ( IllegalArgumentException exc ) {
            buf.append( unknown + exc.getMessage() );
        }

        buf.append( "\n\nQRaux = " + this.qraux );

        return buf.toString();
    }

    protected String diagnose() {
        StringBuilder buf = new StringBuilder();
        buf.append( "Rank = " + rank + "\n" );
        //  buf.append( "Work: " + originalNorms + "\n" );
        buf.append( "Qraux: " + qraux + "\n" );
        buf.append( "Pivot indices: " + StringUtils.join( ArrayUtils.toObject( jpvt ), "  " ) + "\n" );
        return buf.toString();
    }

    /**
     * For testing.
     * 
     * @return
     */
    protected DoubleMatrix2D getQR() {
        return QR;
    }

    /**
     * Mimics functionality of chol2inv from R (which just calls LAPACK::dpotri)
     *
     * @param x upper triangular matrix (from qr)
     * @return symmetric matrix X'X^-1
     */
    private DoubleMatrix2D dpotri( DoubleMatrix2D x ) {

        if ( this.chol2inv != null ) return this.chol2inv;

        DenseMatrix denseMatrix = new DenseMatrix( x.copy().toArray() );
        intW status = new intW( 0 );
        Dpotri.dpotri( "U", x.columns(), denseMatrix.getData(), 0, x.columns(), status );
        if ( status.val != 0 ) {
            throw new IllegalStateException( "Could not invert matrix" );
        }

        this.chol2inv = new DenseDoubleMatrix2D( Matrices.getArray( denseMatrix ) );
        return this.chol2inv;
    }

    /**
     * Returns sqrt(a^2 + b^2) without under/overflow (from Colt)
     */
    private double hypot( double a, double b ) {
        double r;
        if ( Math.abs( a ) > Math.abs( b ) ) {
            r = b / a;
            r = Math.abs( a ) * Math.sqrt( 1 + r * r );
        } else if ( b != 0 ) {
            r = a / b;
            r = Math.abs( b ) * Math.sqrt( 1 + r * r );
        } else {
            r = 0.0;
        }
        return r;
    }

    /**
     * @param x
     * @param work
     * @param v
     */
    private void rotate( DoubleMatrix2D x, DoubleMatrix1D work, int v ) {
        for ( int i = 0; i < n; i++ ) {
            double t = x.get( i, v );
            for ( int j = v; j < p - 1; j++ ) {
                x.set( i, j, x.get( i, j + 1 ) );
            }
            x.set( i, p - 1, t );
        }

        // do the same rotation to our helpers
        int i = jpvt[v];
        double t = qraux.get( v );
        double w0 = work.get( v );

        for ( int j = v; j < p - 1; j++ ) {
            jpvt[j] = jpvt[j + 1];
            qraux.set( j, qraux.get( j + 1 ) );
            work.set( j, work.get( j + 1 ) );
        }
        jpvt[p - 1] = i;
        qraux.set( p - 1, t );
        work.set( p - 1, w0 );
        rank = rank - 1;
    }

}
