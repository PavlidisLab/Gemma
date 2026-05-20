package ubic.gemma.core.util.math.linalg;

/*
    QR_j.java copyright claim:

    This software is based on public domain LINPACK routines.
    It was translated from FORTRAN to Java by a US government employee
    on official time.  Thus this software is also in the public domain.


    The translator's mail address is:

    Steve Verrill
    USDA Forest Products Laboratory
    1 Gifford Pinchot Drive
    Madison, Wisconsin
    53705


    The translator's e-mail address is:

    steve@swst.org


***********************************************************************

DISCLAIMER OF WARRANTIES:

THIS SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND.
THE TRANSLATOR DOES NOT WARRANT, GUARANTEE OR MAKE ANY REPRESENTATIONS
REGARDING THE SOFTWARE OR DOCUMENTATION IN TERMS OF THEIR CORRECTNESS,
RELIABILITY, CURRENTNESS, OR OTHERWISE. THE ENTIRE RISK AS TO
THE RESULTS AND PERFORMANCE OF THE SOFTWARE IS ASSUMED BY YOU.
IN NO CASE WILL ANY PARTY INVOLVED WITH THE CREATION OR DISTRIBUTION
OF THE SOFTWARE BE LIABLE FOR ANY DAMAGE THAT MAY RESULT FROM THE USE
OF THIS SOFTWARE.

Sorry about that.

***********************************************************************


History:

Date        Translator        Changes

2/25/97     Steve Verrill     Translated
6/5/97                        Java/C style indexing

*/

/**
 *
 *
 *
 * This class contains the LINPACK DQRDC (QR decomposition)
 * and DQRSL (QR solve) routines.
 *
 *
 *
 *
 * IMPORTANT: The "_j" suffixes indicate that these routines use
 * Java/C style indexing. For example, you will see
 *
 * for (i = 0; i < n; i++)
 *
 * rather than
 *
 * for (i = 1; i <= n; i++)
 *
 * To use the "_j" routines you will have
 * to fill elements 0 through n - 1 rather than elements 1 through n.
 * Versions of these programs that use FORTYRAN style indexing are
 * also available. They end with the suffix "_f77".
 *
 *
 *
 *
 * This class was translated by a statistician from FORTRAN versions of
 * the LINPACK routines. It is NOT an official translation. When
 * public domain Java numerical analysis routines become available
 * from the people who produce LAPACK, then THE CODE PRODUCED
 * BY THE NUMERICAL ANALYSTS SHOULD BE USED.
 *
 *
 *
 *
 * Meanwhile, if you have suggestions for improving this
 * code, please contact Steve Verrill at sverrill@fs.fed.us.
 *
 * @author Steve Verrill
 * @version .5 --- June 5, 1997
 *
 */
public class Dqrsl extends Object {

    /**
     *
     *
     * This method decomposes an n by p matrix X into a product, QR, of
     * an orthogonal n by n matrix Q and an upper triangular n by p matrix R.
     * For details, see the comments in the code.
     * This method is a translation from FORTRAN to Java of the LINPACK subroutine
     * DQRDC. In the LINPACK listing DQRDC is attributed to G.W. Stewart
     * with a date of 8/14/78.
     *
     * Translated by Steve Verrill, February 25, 1997.
     *
     * @param X The matrix to be decomposed
     * @param n The number of rows of the matrix X
     * @param p The number of columns of the matrix X
     * @param qraux This vector "contains further information required to
     *        recover the orthogonal part of the decomposition."
     * @param jpvt This output vector contains pivoting information.
     * @param work This vector is used as temporary space
     * @param job This value indicates whether column pivoting should be performed
     *
     */
    public static void dqrdc_j( double x[][], int n, int p, double qraux[], int jpvt[], int job ) {

        int j, jj, jp, l, lp1, lup, maxj, pl, pu;
        int jm1, plm1, pum1, lm1, maxjm1;

        double maxnrm, tt;
        double nrmxl, t;
        double fac;
        double work[] = new double[p];

        boolean initial, fin;

        /*
         *
         * Here is a copy of the LINPACK documentation (from the SLATEC version):
         *
         * C***BEGIN PROLOGUE DQRDC
         * C***DATE WRITTEN 780814 (YYMMDD)
         * C***REVISION DATE 861211 (YYMMDD)
         * C***CATEGORY NO. D5
         * C***KEYWORDS LIBRARY=SLATEC(LINPACK),
         * C TYPE=DOUBLE PRECISION(SQRDC-S DQRDC-D CQRDC-C),
         * C LINEAR ALGEBRA,MATRIX,ORTHOGONAL TRIANGULAR,
         * C QR DECOMPOSITION
         * C***AUTHOR STEWART, G. W., (U. OF MARYLAND)
         * C***PURPOSE Uses Householder transformations to compute the QR factori-
         * C zation of N by P matrix X. Column pivoting is optional.
         * C***DESCRIPTION
         * C
         * C DQRDC uses Householder transformations to compute the QR
         * C factorization of an N by P matrix X. Column pivoting
         * C based on the 2-norms of the reduced columns may be
         * C performed at the user's option.
         * C
         * C On Entry
         * C
         * C X DOUBLE PRECISION(LDX,P), where LDX .GE. N.
         * C X contains the matrix whose decomposition is to be
         * C computed.
         * C
         * C LDX INTEGER.
         * C LDX is the leading dimension of the array X.
         * C
         * C N INTEGER.
         * C N is the number of rows of the matrix X.
         * C
         * C P INTEGER.
         * C P is the number of columns of the matrix X.
         * C
         * C JPVT INTEGER(P).
         * C JPVT contains integers that control the selection
         * C of the pivot columns. The K-th column X(K) of X
         * C is placed in one of three classes according to the
         * C value of JPVT(K).
         * C
         * C If JPVT(K) .GT. 0, then X(K) is an initial
         * C column.
         * C
         * C If JPVT(K) .EQ. 0, then X(K) is a free column.
         * C
         * C If JPVT(K) .LT. 0, then X(K) is a final column.
         * C
         * C Before the decomposition is computed, initial columns
         * C are moved to the beginning of the array X and final
         * C columns to the end. Both initial and final columns
         * C are frozen in place during the computation and only
         * C free columns are moved. At the K-th stage of the
         * C reduction, if X(K) is occupied by a free column
         * C it is interchanged with the free column of largest
         * C reduced norm. JPVT is not referenced if
         * C JOB .EQ. 0.
         * C
         * C WORK DOUBLE PRECISION(P).
         * C WORK is a work array. WORK is not referenced if
         * C JOB .EQ. 0.
         * C
         * C JOB INTEGER.
         * C JOB is an integer that initiates column pivoting.
         * C If JOB .EQ. 0, no pivoting is done.
         * C If JOB .NE. 0, pivoting is done.
         * C
         * C On Return
         * C
         * C X X contains in its upper triangle the upper
         * C triangular matrix R of the QR factorization.
         * C Below its diagonal X contains information from
         * C which the orthogonal part of the decomposition
         * C can be recovered. Note that if pivoting has
         * C been requested, the decomposition is not that
         * C of the original matrix X but that of X
         * C with its columns permuted as described by JPVT.
         * C
         * C QRAUX DOUBLE PRECISION(P).
         * C QRAUX contains further information required to recover
         * C the orthogonal part of the decomposition.
         * C
         * C JPVT JPVT(K) contains the index of the column of the
         * C original matrix that has been interchanged into
         * C the K-th column, if pivoting was requested.
         * C
         * C LINPACK. This version dated 08/14/78 .
         * C G. W. Stewart, University of Maryland, Argonne National Lab.
         * C
         * C DQRDC uses the following functions and subprograms.
         * C
         * C BLAS DAXPY,DDOT,DSCAL,DSWAP,DNRM2
         * C Fortran DABS,DMAX1,MIN0,DSQRT
         * C***REFERENCES DONGARRA J.J., BUNCH J.R., MOLER C.B., STEWART G.W.,
         * C *LINPACK USERS GUIDE*, SIAM, 1979.
         * C***ROUTINES CALLED DAXPY,DDOT,DNRM2,DSCAL,DSWAP
         * C***END PROLOGUE DQRDC
         *
         */

        pl = 1;
        plm1 = pl - 1;
        pu = 0;

        if ( job != 0 ) {

            //   Pivoting has been requested.  Rearrange the columns according to pvt.

            for ( j = 1; j <= p; j++ ) {
                jm1 = j - 1;
                initial = ( jpvt[jm1] > 0 );
                fin = ( jpvt[jm1] < 0 );

                jpvt[jm1] = j;
                if ( fin ) jpvt[jm1] = -j;

                if ( initial ) {
                    if ( j != pl ) Blas.colswap_j( n, x, plm1, jm1 );
                    jpvt[jm1] = jpvt[plm1];
                    jpvt[plm1] = j;
                    plm1 = pl;
                    pl++;
                }
            }

            pu = p;
            pum1 = p - 1;

            for ( jj = 1; jj <= p; jj++ ) {

                j = p - jj;

                if ( jpvt[j] < 0 ) {

                    jpvt[j] = -jpvt[j];

                    if ( j != pum1 ) {
                        Blas.colswap_j( n, x, pum1, j );
                        jp = jpvt[pum1];
                        jpvt[pum1] = jpvt[j];
                        jpvt[j] = jp;
                    }

                    pu--;
                    pum1--;

                }
            }
        }

        //   Compute the norms of the free columns. 
        for ( j = pl - 1; j < pu; j++ ) {
            qraux[j] = Blas.colnrm2_j( n, x, 0, j );
            work[j] = qraux[j];
        }

        //   Perform the Householder reduction of X.

        lup = Math.min( n, p );

        for ( l = 1; l <= lup; l++ ) {

            lm1 = l - 1;

            if ( l >= pl && l < pu ) {

                //   Locate the column of greatest norm and bring it into
                //   the pivot position.

                maxnrm = 0.0;
                maxj = l;

                for ( j = l; j <= pu; j++ ) {

                    jm1 = j - 1;

                    if ( qraux[jm1] > maxnrm ) {
                        maxnrm = qraux[jm1];
                        maxj = j;
                    }
                }

                if ( maxj != l ) {
                    maxjm1 = maxj - 1;
                    Blas.colswap_j( n, x, lm1, maxjm1 );
                    qraux[maxjm1] = qraux[lm1];
                    work[maxjm1] = work[lm1];
                    jp = jpvt[maxjm1];
                    jpvt[maxjm1] = jpvt[lm1];
                    jpvt[lm1] = jp;

                }
            }

            qraux[lm1] = 0.0;

            if ( l != n ) {

                //   Compute the Householder transformation for column l.

                nrmxl = Blas.colnrm2_j( n - l + 1, x, lm1, lm1 );

                if ( nrmxl != 0.0 ) {

                    if ( x[lm1][lm1] != 0.0 ) nrmxl = Blas.sign_j( nrmxl, x[lm1][lm1] );

                    Blas.colscal_j( n - l + 1, 1.0 / nrmxl, x, lm1, lm1 );

                    x[lm1][lm1]++;

                    //   Apply the transformation to the remaining columns,
                    //   updating the norms.

                    lp1 = l + 1;

                    for ( j = lp1; j <= p; j++ ) {

                        jm1 = j - 1;

                        t = -Blas.coldot_j( n - l + 1, x, lm1, lm1, jm1 ) / x[lm1][lm1];

                        Blas.colaxpy_j( n - l + 1, t, x, lm1, lm1, jm1 );

                        if ( j >= pl && j <= pu ) {

                            if ( qraux[jm1] != 0.0 ) {

                                fac = Math.abs( x[lm1][jm1] ) / qraux[jm1];
                                tt = 1.0 - fac * fac;
                                tt = Math.max( tt, 0.0 );
                                t = tt;
                                fac = qraux[jm1] / work[jm1];
                                tt = 1.0 + .05 * tt * fac * fac;

                                if ( tt != 1.0 ) {

                                    qraux[jm1] *= Math.sqrt( t );

                                } else {

                                    qraux[jm1] = Blas.colnrm2_j( n - l, x, l, jm1 );
                                    work[jm1] = qraux[jm1];

                                }
                            }
                        }
                    }

                    //   Save the transformation

                    qraux[lm1] = x[lm1][lm1];
                    x[lm1][lm1] = -nrmxl;

                }
            }
        }

        return;

    }

    /**
     * This method "applies the output of DQRDC to compute coordinate
     * transformations, projections, and least squares solutions."
     * For details, see the comments in the code.
     * This method is a translation from FORTRAN to Java of the LINPACK subroutine
     * DQRSL. In the LINPACK listing DQRSL is attributed to G.W. Stewart
     * with a date of 8/14/78.
     *
     * Translated by Steve Verrill, February 27, 1997.
     *
     * @param X This n by p matrix contains most of the output from DQRDC
     * @param n The number of rows of X
     * @param k k <= min(n,p) where p is the number of columns of X
     * @param qraux This vector "contains further information required to
     *        recover the orthogonal part of the decomposition"
     * @param y This n by 1 vector will be manipulated by DQRSL
     * @param qy On output, this vector contains Qy if it has been requested
     * @param qty On output, this vector contains transpose(Q)y if it has been requested
     * @param b Parameter estimates
     * @param rsd Residuals
     * @param xb Predicted values
     * @param job Specifies what is to be computed (see the code for details)
     *
     */
    public static int dqrsl_j( double x[][], int n, int k, double qraux[],
            double y[], double qy[], double qty[],
            double b[], double rsd[], double xb[],
            int job ) {

        /*
         *
         * Here is a copy of the LINPACK documentation (from the SLATEC version):
         *
         * c***begin prologue dqrsl
         * c***date written 780814 (yymmdd)
         * c***revision date 861211 (yymmdd)
         * c***category no. d9,d2a1
         * c***keywords library=slatec(linpack),
         * c type=double precision(sqrsl-s dqrsl-d cqrsl-c),
         * c linear algebra,matrix,orthogonal triangular,solve
         * c***author stewart, g. w., (u. of maryland)
         * c***purpose applies the output of dqrdc to compute coordinate
         * c transformations, projections, and least squares solutions.
         * c***description
         * c
         * c dqrsl applies the output of dqrdc to compute coordinate
         * c transformations, projections, and least squares solutions.
         * c for k .le. min(n,p), let xk be the matrix
         * c
         * c xk = (x(jpvt(1)),x(jpvt(2)), ... ,x(jpvt(k)))
         * c
         * c formed from columnns jpvt(1), ... ,jpvt(k) of the original
         * c n x p matrix x that was input to dqrdc (if no pivoting was
         * c done, xk consists of the first k columns of x in their
         * c original order). dqrdc produces a factored orthogonal matrix q
         * c and an upper triangular matrix r such that
         * c
         * c xk = q * (r)
         * c (0)
         * c
         * c this information is contained in coded form in the arrays
         * c x and qraux.
         * c
         * c on entry
         * c
         * c x double precision(ldx,p).
         * c x contains the output of dqrdc.
         * c
         * c ldx integer.
         * c ldx is the leading dimension of the array x.
         * c
         * c n integer.
         * c n is the number of rows of the matrix xk. it must
         * c have the same value as n in dqrdc.
         * c
         * c k integer.
         * c k is the number of columns of the matrix xk. k
         * c must not be greater than min(n,p), where p is the
         * c same as in the calling sequence to dqrdc.
         * c
         * c qraux double precision(p).
         * c qraux contains the auxiliary output from dqrdc.
         * c
         * c y double precision(n)
         * c y contains an n-vector that is to be manipulated
         * c by dqrsl.
         * c
         * c job integer.
         * c job specifies what is to be computed. job has
         * c the decimal expansion abcde, with the following
         * c meaning.
         * c
         * c if a .ne. 0, compute qy.
         * c if b,c,d, or e .ne. 0, compute qty.
         * c if c .ne. 0, compute b.
         * c if d .ne. 0, compute rsd.
         * c if e .ne. 0, compute xb.
         * c
         * c note that a request to compute b, rsd, or xb
         * c automatically triggers the computation of qty, for
         * c which an array must be provided in the calling
         * c sequence.
         * c
         * c on return
         * c
         * c qy double precision(n).
         * c qy contains q*y, if its computation has been
         * c requested.
         * c
         * c qty double precision(n).
         * c qty contains trans(q)*y, if its computation has
         * c been requested. here trans(q) is the
         * c transpose of the matrix q.
         * c
         * c b double precision(k)
         * c b contains the solution of the least squares problem
         * c
         * c minimize norm2(y - xk*b),
         * c
         * c if its computation has been requested. (note that
         * c if pivoting was requested in dqrdc, the j-th
         * c component of b will be associated with column jpvt(j)
         * c of the original matrix x that was input into dqrdc.)
         * c
         * c rsd double precision(n).
         * c rsd contains the least squares residual y - xk*b,
         * c if its computation has been requested. rsd is
         * c also the orthogonal projection of y onto the
         * c orthogonal complement of the column space of xk.
         * c
         * c xb double precision(n).
         * c xb contains the least squares approximation xk*b,
         * c if its computation has been requested. xb is also
         * c the orthogonal projection of y onto the column space
         * c of x.
         * c
         * c info integer.
         * c info is zero unless the computation of b has
         * c been requested and r is exactly singular. in
         * c this case, info is the index of the first zero
         * c diagonal element of r and b is left unaltered.
         *
         *
         * For the Java version, info
         * is the return value of the the dqrsl_j method.
         *
         *
         * c
         * c the parameters qy, qty, b, rsd, and xb are not referenced
         * c if their computation is not requested and in this case
         * c can be replaced by dummy variables in the calling program.
         * c to save storage, the user may in some cases use the same
         * c array for different parameters in the calling sequence. a
         * c frequently occuring example is when one wishes to compute
         * c any of b, rsd, or xb and does not need y or qty. in this
         * c case one may identify y, qty, and one of b, rsd, or xb, while
         * c providing separate arrays for anything else that is to be
         * c computed. thus the calling sequence
         * c
         * c call dqrsl(x,ldx,n,k,qraux,y,dum,y,b,y,dum,110,info)
         * c
         * c will result in the computation of b and rsd, with rsd
         * c overwriting y. more generally, each item in the following
         * c list contains groups of permissible identifications for
         * c a single calling sequence.
         * c
         * c 1. (y,qty,b) (rsd) (xb) (qy)
         * c
         * c 2. (y,qty,rsd) (b) (xb) (qy)
         * c
         * c 3. (y,qty,xb) (b) (rsd) (qy)
         * c
         * c 4. (y,qy) (qty,b) (rsd) (xb)
         * c
         * c 5. (y,qy) (qty,rsd) (b) (xb)
         * c
         * c 6. (y,qy) (qty,xb) (b) (rsd)
         * c
         * c in any group the value returned in the array allocated to
         * c the group corresponds to the last member of the group.
         * c
         * c linpack. this version dated 08/14/78 .
         * c g. w. stewart, university of maryland, argonne national lab.
         * c
         * c dqrsl uses the following functions and subprograms.
         * c
         * c blas daxpy,dcopy,ddot
         * c fortran dabs,min0,mod
         * c***references dongarra j.j., bunch j.r., moler c.b., stewart g.w.,
         * c *linpack users guide*, siam, 1979.
         * c***routines called daxpy,dcopy,ddot
         * c***end prologue dqrsl
         *
         */

        /*
         * Input, integer JOB, specifies what is to be computed. JOB has
         * the decimal expansion ABCDE, with the following meaning:
         * if A != 0, compute QY.
         * if B != 0, compute QTY.
         * if C != 0, compute QTY and B.
         * if D != 0, compute QTY and RSD.
         * if E != 0, compute QTY and AB.
         */

        int i, j, jj, ju, info;
        int jm1;
        double t, temp;
        boolean cb, cqy, cqty, cr, cxb;

        //   set info flag

        info = 0;

        //   determine what is to be computed

        cqy = ( job / 10000 != 0 );
        cqty = ( ( job % 10000 ) != 0 );
        cb = ( ( job % 1000 ) / 100 != 0 );
        cr = ( ( job % 100 ) / 10 != 0 );
        cxb = ( ( job % 10 ) != 0 );

        ju = Math.min( k, n - 1 );

        //   special action when n = 1

        if ( ju == 0 ) {

            if ( cqy ) qy[0] = y[0];
            if ( cqty ) qty[0] = y[0];
            if ( cxb ) xb[0] = y[0];

            if ( cb ) {

                if ( x[0][0] == 0.0 ) {

                    info = 1;

                } else {

                    b[0] = y[0] / x[0][0];

                }

            }

            if ( cr ) rsd[0] = 0.0;

            return info;

        }

        //   set up to compute qy or transpose(q)y

        if ( cqy ) Blas.dcopy_j( n, y, 1, qy, 1 );
        if ( cqty ) Blas.dcopy_j( n, y, 1, qty, 1 );

        if ( cqy ) {

            //   compute qy

            for ( jj = 1; jj <= ju; jj++ ) {

                j = ju - jj;

                if ( qraux[j] != 0.0 ) {

                    temp = x[j][j];
                    x[j][j] = qraux[j];
                    t = -Blas.colvdot_j( n - j, x, qy, j, j ) / x[j][j];
                    Blas.colvaxpy_j( n - j, t, x, qy, j, j );
                    x[j][j] = temp;

                }

            }

        }

        if ( cqty ) {

            //   compute transpose(q)y

            for ( j = 0; j < ju; j++ ) {
                if ( qraux[j] != 0.0 ) {
                    temp = x[j][j];
                    x[j][j] = qraux[j];
                    t = -Blas.colvdot_j( n - j, x, qty, j, j ) / x[j][j];
                    Blas.colvaxpy_j( n - j, t, x, qty, j, j );
                    x[j][j] = temp;
                }
            }
        }

        //   set up to compute b, rsd, or xb

        if ( cb ) Blas.dcopy_j( k, qty, 1, b, 1 );

        if ( cxb ) Blas.dcopy_j( k, qty, 1, xb, 1 );

        if ( cr && ( k < n ) ) Blas.dcopyp_j( n - k, qty, rsd, k );

        if ( cxb ) {
            for ( i = k; i < n; i++ ) {
                xb[i] = 0.0;
            }
        }

        if ( cr ) {
            for ( i = 0; i < k; i++ ) {
                rsd[i] = 0.0;
            }
        }

        if ( cb ) {

            //   compute b

            for ( jj = 1; jj <= k; jj++ ) {

                jm1 = k - jj;
                if ( x[jm1][jm1] == 0.0 ) {
                    info = jm1 + 1;
                    break;
                }

                b[jm1] = b[jm1] / x[jm1][jm1];

                if ( jm1 != 0 ) {
                    t = -b[jm1];
                    Blas.colvaxpy_j( jm1, t, x, b, 0, jm1 );

                }
            }
        }

        if ( cr || cxb ) {
            //   compute rsd or xb as required
            for ( jj = 1; jj <= ju; jj++ ) {
                j = ju - jj;

                if ( qraux[j] != 0.0 ) {
                    temp = x[j][j];
                    x[j][j] = qraux[j];

                    if ( cr ) {
                        t = -Blas.colvdot_j( n - j, x, rsd, j, j ) / x[j][j];
                        Blas.colvaxpy_j( n - j, t, x, rsd, j, j );
                    }

                    if ( cxb ) {
                        t = -Blas.colvdot_j( n - j, x, xb, j, j ) / x[j][j];
                        Blas.colvaxpy_j( n - j, t, x, xb, j, j );

                    }

                    x[j][j] = temp;

                }
            }

        }

        return info;

    }

}