/*
 * Copyright (c) 1999 CERN - European Organization for Nuclear Research.
 *
 * Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose is hereby
 * granted without fee, provided that the above copyright notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting documentation. CERN makes no representations about the
 * suitability of this software for any purpose. It is provided "as is" without expressed or implied warranty.
 *
 * Adapted from cern.colt.matrix.linalg.SingularValueDecomposition (Colt 1.2.0), itself a port of the public-domain
 * JAMA SingularValueDecomposition (MathWorks and NIST).
 */
package ubic.gemma.core.util.math.linalg;

/**
 * Colt's singular value decomposition (Golub-Kahan bidiagonalization followed by implicit-shift QR), with one change:
 * it stops with an {@link InterruptedException} when its thread is interrupted.
 * <p>
 * Colt's version never checks, so cancelling a timed-out decomposition left it running to completion on its worker
 * thread, with the result thrown away. The arithmetic is Colt's, statement for statement, so the results are
 * identical; {@code JamaSingularValueDecompositionTest} holds it to that.
 * <p>
 * Requires rows ≥ columns, as Colt does.
 */
final class JamaSingularValueDecomposition {

    /**
     * Rows × min(rows, columns) left singular vectors.
     */
    private final double[][] u;

    /**
     * Columns × columns right singular vectors.
     */
    private final double[][] v;

    /**
     * Singular values, largest first; min(rows + 1, columns) of them.
     */
    private final double[] s;

    /**
     * @param matrix rows × columns, rows ≥ columns; not modified
     * @throws InterruptedException if the current thread is interrupted; it is checked once per step of each phase
     */
    JamaSingularValueDecomposition( double[][] matrix ) throws InterruptedException {
        int m = matrix.length;
        int n = m == 0 ? 0 : matrix[0].length;
        if ( m < n ) {
            throw new IllegalArgumentException( "Matrix must be rectangular: " + m + " x " + n );
        }
        double[][] a = new double[m][];
        for ( int i = 0; i < m; i++ ) {
            a[i] = matrix[i].clone();
        }

        int nu = Math.min( m, n );
        s = new double[Math.min( m + 1, n )];
        u = new double[m][nu];
        v = new double[n][n];
        double[] e = new double[n];
        double[] work = new double[m];

        // Reduce A to bidiagonal form, storing the diagonal elements in s and the super-diagonal elements in e.
        int nct = Math.min( m - 1, n );
        int nrt = Math.max( 0, Math.min( n - 2, m ) );
        for ( int k = 0; k < Math.max( nct, nrt ); k++ ) {
            checkInterrupted();
            if ( k < nct ) {
                // Compute the transformation for the k-th column and place the k-th diagonal in s[k].
                s[k] = 0;
                for ( int i = k; i < m; i++ ) {
                    s[k] = hypot( s[k], a[i][k] );
                }
                if ( s[k] != 0.0 ) {
                    if ( a[k][k] < 0.0 ) {
                        s[k] = -s[k];
                    }
                    for ( int i = k; i < m; i++ ) {
                        a[i][k] /= s[k];
                    }
                    a[k][k] += 1.0;
                }
                s[k] = -s[k];
            }
            for ( int j = k + 1; j < n; j++ ) {
                if ( k < nct & s[k] != 0.0 ) {
                    // Apply the transformation.
                    double t = 0;
                    for ( int i = k; i < m; i++ ) {
                        t += a[i][k] * a[i][j];
                    }
                    t = -t / a[k][k];
                    for ( int i = k; i < m; i++ ) {
                        a[i][j] += t * a[i][k];
                    }
                }
                // Place the k-th row of A into e for the subsequent calculation of the row transformation.
                e[j] = a[k][j];
            }
            if ( k < nct ) {
                // Place the transformation in U for subsequent back multiplication.
                for ( int i = k; i < m; i++ ) {
                    u[i][k] = a[i][k];
                }
            }
            if ( k < nrt ) {
                // Compute the k-th row transformation and place the k-th super-diagonal in e[k].
                e[k] = 0;
                for ( int i = k + 1; i < n; i++ ) {
                    e[k] = hypot( e[k], e[i] );
                }
                if ( e[k] != 0.0 ) {
                    if ( e[k + 1] < 0.0 ) {
                        e[k] = -e[k];
                    }
                    for ( int i = k + 1; i < n; i++ ) {
                        e[i] /= e[k];
                    }
                    e[k + 1] += 1.0;
                }
                e[k] = -e[k];
                if ( k + 1 < m & e[k] != 0.0 ) {
                    // Apply the transformation.
                    for ( int i = k + 1; i < m; i++ ) {
                        work[i] = 0.0;
                    }
                    for ( int j = k + 1; j < n; j++ ) {
                        for ( int i = k + 1; i < m; i++ ) {
                            work[i] += e[j] * a[i][j];
                        }
                    }
                    for ( int j = k + 1; j < n; j++ ) {
                        double t = -e[j] / e[k + 1];
                        for ( int i = k + 1; i < m; i++ ) {
                            a[i][j] += t * work[i];
                        }
                    }
                }
                // Place the transformation in V for subsequent back multiplication.
                for ( int i = k + 1; i < n; i++ ) {
                    v[i][k] = e[i];
                }
            }
        }

        // Set up the final bidiagonal matrix of order p.
        int p = Math.min( n, m + 1 );
        if ( nct < n ) {
            s[nct] = a[nct][nct];
        }
        if ( m < p ) {
            s[p - 1] = 0.0;
        }
        if ( nrt + 1 < p ) {
            e[nrt] = a[nrt][p - 1];
        }
        e[p - 1] = 0.0;

        // Generate U.
        for ( int j = nct; j < nu; j++ ) {
            for ( int i = 0; i < m; i++ ) {
                u[i][j] = 0.0;
            }
            u[j][j] = 1.0;
        }
        for ( int k = nct - 1; k >= 0; k-- ) {
            checkInterrupted();
            if ( s[k] != 0.0 ) {
                for ( int j = k + 1; j < nu; j++ ) {
                    double t = 0;
                    for ( int i = k; i < m; i++ ) {
                        t += u[i][k] * u[i][j];
                    }
                    t = -t / u[k][k];
                    for ( int i = k; i < m; i++ ) {
                        u[i][j] += t * u[i][k];
                    }
                }
                for ( int i = k; i < m; i++ ) {
                    u[i][k] = -u[i][k];
                }
                u[k][k] = 1.0 + u[k][k];
                for ( int i = 0; i < k - 1; i++ ) {
                    u[i][k] = 0.0;
                }
            } else {
                for ( int i = 0; i < m; i++ ) {
                    u[i][k] = 0.0;
                }
                u[k][k] = 1.0;
            }
        }

        // Generate V.
        for ( int k = n - 1; k >= 0; k-- ) {
            checkInterrupted();
            if ( k < nrt & e[k] != 0.0 ) {
                for ( int j = k + 1; j < nu; j++ ) {
                    double t = 0;
                    for ( int i = k + 1; i < n; i++ ) {
                        t += v[i][k] * v[i][j];
                    }
                    t = -t / v[k + 1][k];
                    for ( int i = k + 1; i < n; i++ ) {
                        v[i][j] += t * v[i][k];
                    }
                }
            }
            for ( int i = 0; i < n; i++ ) {
                v[i][k] = 0.0;
            }
            v[k][k] = 1.0;
        }

        // Main iteration loop for the singular values.
        int pp = p - 1;
        double eps = Math.pow( 2.0, -52.0 );
        while ( p > 0 ) {
            checkInterrupted();
            int k, kase;

            // This section of the program inspects for negligible elements in the s and e arrays. On completion the
            // variables kase and k are set as follows.
            //
            // kase = 1 if s(p) and e[k-1] are negligible and k<p
            // kase = 2 if s(k) is negligible and k<p
            // kase = 3 if e[k-1] is negligible, k<p, and s(k), ..., s(p) are not negligible (qr step).
            // kase = 4 if e(p-1) is negligible (convergence).
            for ( k = p - 2; k >= -1; k-- ) {
                if ( k == -1 ) {
                    break;
                }
                if ( Math.abs( e[k] ) <= eps * ( Math.abs( s[k] ) + Math.abs( s[k + 1] ) ) ) {
                    e[k] = 0.0;
                    break;
                }
            }
            if ( k == p - 2 ) {
                kase = 4;
            } else {
                int ks;
                for ( ks = p - 1; ks >= k; ks-- ) {
                    if ( ks == k ) {
                        break;
                    }
                    double t = ( ks != p ? Math.abs( e[ks] ) : 0. ) + ( ks != k + 1 ? Math.abs( e[ks - 1] ) : 0. );
                    if ( Math.abs( s[ks] ) <= eps * t ) {
                        s[ks] = 0.0;
                        break;
                    }
                }
                if ( ks == k ) {
                    kase = 3;
                } else if ( ks == p - 1 ) {
                    kase = 1;
                } else {
                    kase = 2;
                    k = ks;
                }
            }
            k++;

            // Perform the task indicated by kase.
            switch ( kase ) {

                // Deflate negligible s(p).
                case 1: {
                    double f = e[p - 2];
                    e[p - 2] = 0.0;
                    for ( int j = p - 2; j >= k; j-- ) {
                        double t = hypot( s[j], f );
                        double cs = s[j] / t;
                        double sn = f / t;
                        s[j] = t;
                        if ( j != k ) {
                            f = -sn * e[j - 1];
                            e[j - 1] = cs * e[j - 1];
                        }
                        for ( int i = 0; i < n; i++ ) {
                            t = cs * v[i][j] + sn * v[i][p - 1];
                            v[i][p - 1] = -sn * v[i][j] + cs * v[i][p - 1];
                            v[i][j] = t;
                        }
                    }
                }
                break;

                // Split at negligible s(k).
                case 2: {
                    double f = e[k - 1];
                    e[k - 1] = 0.0;
                    for ( int j = k; j < p; j++ ) {
                        double t = hypot( s[j], f );
                        double cs = s[j] / t;
                        double sn = f / t;
                        s[j] = t;
                        f = -sn * e[j];
                        e[j] = cs * e[j];
                        for ( int i = 0; i < m; i++ ) {
                            t = cs * u[i][j] + sn * u[i][k - 1];
                            u[i][k - 1] = -sn * u[i][j] + cs * u[i][k - 1];
                            u[i][j] = t;
                        }
                    }
                }
                break;

                // Perform one qr step.
                case 3: {
                    // Calculate the shift.
                    double scale = Math.max( Math.max( Math.max( Math.max(
                            Math.abs( s[p - 1] ), Math.abs( s[p - 2] ) ), Math.abs( e[p - 2] ) ),
                            Math.abs( s[k] ) ), Math.abs( e[k] ) );
                    double sp = s[p - 1] / scale;
                    double spm1 = s[p - 2] / scale;
                    double epm1 = e[p - 2] / scale;
                    double sk = s[k] / scale;
                    double ek = e[k] / scale;
                    double b = ( ( spm1 + sp ) * ( spm1 - sp ) + epm1 * epm1 ) / 2.0;
                    double c = ( sp * epm1 ) * ( sp * epm1 );
                    double shift = 0.0;
                    if ( b != 0.0 | c != 0.0 ) {
                        shift = Math.sqrt( b * b + c );
                        if ( b < 0.0 ) {
                            shift = -shift;
                        }
                        shift = c / ( b + shift );
                    }
                    double f = ( sk + sp ) * ( sk - sp ) + shift;
                    double g = sk * ek;

                    // Chase zeros.
                    for ( int j = k; j < p - 1; j++ ) {
                        double t = hypot( f, g );
                        double cs = f / t;
                        double sn = g / t;
                        if ( j != k ) {
                            e[j - 1] = t;
                        }
                        f = cs * s[j] + sn * e[j];
                        e[j] = cs * e[j] - sn * s[j];
                        g = sn * s[j + 1];
                        s[j + 1] = cs * s[j + 1];
                        for ( int i = 0; i < n; i++ ) {
                            t = cs * v[i][j] + sn * v[i][j + 1];
                            v[i][j + 1] = -sn * v[i][j] + cs * v[i][j + 1];
                            v[i][j] = t;
                        }
                        t = hypot( f, g );
                        cs = f / t;
                        sn = g / t;
                        s[j] = t;
                        f = cs * e[j] + sn * s[j + 1];
                        s[j + 1] = -sn * e[j] + cs * s[j + 1];
                        g = sn * e[j + 1];
                        e[j + 1] = cs * e[j + 1];
                        if ( j < m - 1 ) {
                            for ( int i = 0; i < m; i++ ) {
                                t = cs * u[i][j] + sn * u[i][j + 1];
                                u[i][j + 1] = -sn * u[i][j] + cs * u[i][j + 1];
                                u[i][j] = t;
                            }
                        }
                    }
                    e[p - 2] = f;
                }
                break;

                // Convergence.
                case 4: {
                    // Make the singular values positive.
                    if ( s[k] <= 0.0 ) {
                        s[k] = ( s[k] < 0.0 ? -s[k] : 0.0 );
                        for ( int i = 0; i <= pp; i++ ) {
                            v[i][k] = -v[i][k];
                        }
                    }
                    // Order the singular values.
                    while ( k < pp ) {
                        if ( s[k] >= s[k + 1] ) {
                            break;
                        }
                        double t = s[k];
                        s[k] = s[k + 1];
                        s[k + 1] = t;
                        if ( k < n - 1 ) {
                            for ( int i = 0; i < n; i++ ) {
                                t = v[i][k + 1];
                                v[i][k + 1] = v[i][k];
                                v[i][k] = t;
                            }
                        }
                        if ( k < m - 1 ) {
                            for ( int i = 0; i < m; i++ ) {
                                t = u[i][k + 1];
                                u[i][k + 1] = u[i][k];
                                u[i][k] = t;
                            }
                        }
                        k++;
                    }
                    p--;
                }
                break;
            }
        }
    }

    /**
     * @return rows × min(rows, columns); the array itself, not a copy
     */
    double[][] getU() {
        return u;
    }

    /**
     * @return columns × columns; the array itself, not a copy
     */
    double[][] getV() {
        return v;
    }

    /**
     * @return the singular values, largest first; the array itself, not a copy
     */
    double[] getSingularValues() {
        return s;
    }

    /**
     * sqrt(a² + b²) without under/overflow, as Colt's {@code Algebra.hypot} computes it.
     */
    private static double hypot( double a, double b ) {
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

    private static void checkInterrupted() throws InterruptedException {
        if ( Thread.interrupted() ) {
            throw new InterruptedException( "SVD interrupted" );
        }
    }
}
