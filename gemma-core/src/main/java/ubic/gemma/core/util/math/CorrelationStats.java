/*
 * The baseCode project
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
package ubic.gemma.core.util.math;

import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.jet.math.Arithmetic;
import cern.jet.stat.Probability;

/**
 * Statistical evaluation and transformation tools for correlations.
 * 
 * @author Paul Pavlidis
 * 
 */
public class CorrelationStats {

    /* for spearman - for n <= this, we compute exact probabilities. */
    final static int n_small = 9;
    private static final double BINSIZE = 0.005; // resolution of correlation.
    // Differences smaller than this
    // are considered meaningless.

    /* Edgeworth coefficients for spearman p-value computation : */
    private static final double c1 = 0.2274, c2 = 0.2531, c3 = 0.1745, c4 = 0.0758, c5 = 0.1033, c6 = 0.3932,
            c7 = 0.0879, c8 = 0.0151, c9 = 0.0072, c10 = 0.0831, c11 = 0.0131, c12 = 4.6e-4;
    private static DoubleMatrix2D correlationPvalLookup;

    private static final int MAXCOUNT = 1000; // maximum number of things.

    private static final double PVALCHOP = 8.0; // value by which log(pvalues)
    // are scaled before storing as
    // bytes. Values less than
    // 10^e-256/PVALCHOP are
    // 'clipped'.

    private static DoubleMatrix2D spearmanPvalLookup;

    /**
     * Monitors for the static p-value lookup caches. {@link SparseDoubleMatrix2D} is not thread-safe, so concurrent
     * {@code setQuick} from multiple correlation-computing threads can corrupt internal state. Each cache has its own
     * monitor (they're independent) to avoid needless contention between Pearson and Spearman call sites.
     */
    private static final Object correlationPvalLookupLock = new Object();
    private static final Object spearmanPvalLookupLock = new Object();

    static {
        int numbins = ( int ) Math.ceil( 1.0 / BINSIZE );
        correlationPvalLookup = new SparseDoubleMatrix2D( numbins, MAXCOUNT + 1 );
        spearmanPvalLookup = new SparseDoubleMatrix2D( numbins, MAXCOUNT + 1 );
    }

    /**
     * @param correlByte int
     * @return double
     */
    public static double byteToCorrel( int correlByte ) {
        return correlByte / 128.0 - 1.0;
    }

    /**
     * @param pvalByte int
     * @return double
     */
    public static double byteToPvalue( int pvalByte ) {
        return Math.pow( 10.0, -( double ) pvalByte / PVALCHOP );
    }

    /**
     * Statistical comparison of two Pearson correlations. Assumes data are bivariate normal. Null hypothesis is that
     * the two correlations are equal. See Zar (Biostatistics)
     * 
     * @param correl1 First correlation
     * @param n1 Number of values used to compute correl1
     * @param correl2 Second correlation
     * @param n2 Number of values used to compute correl2
     * @return double p value.
     */
    public static double compare( double correl1, int n1, double correl2, int n2 ) {

        double Z;
        double sigma;
        double p;

        sigma = Math.sqrt( 1 / ( ( double ) n1 - 3 ) + 1 / ( ( double ) n2 - 3 ) );

        Z = Math.abs( correl1 - correl2 ) / sigma;

        p = Probability.normal( -Z ); // upper tail.

        if ( p > 0.5 ) {
            return 1.0 - p;
        }
        return p;
    }

    /**
     * Compute the Pearson correlation, missing values are permitted.
     * 
     * @param ival
     * @param jval
     * @return
     */
    public static double correl( double[] ival, double[] jval ) {
        /* do it the old fashioned way */
        int numused = 0;
        double sxy = 0.0, sxx = 0.0, syy = 0.0, sx = 0.0, sy = 0.0;
        int length = Math.min( ival.length, jval.length );
        for ( int k = 0; k < length; k++ ) {
            double xj = ival[k];
            double yj = jval[k];
            if ( !Double.isNaN( ival[k] ) && !Double.isNaN( jval[k] ) ) {
                sx += xj;
                sy += yj;
                sxy += xj * yj;
                sxx += xj * xj;
                syy += yj * yj;
                numused++;
            }
        }
        if ( numused < 2 ) return Double.NaN;
        double denom = ( sxx - sx * sx / numused ) * ( syy - sy * sy / numused );
        if ( denom <= 0 ) return Double.NaN;
        double correl = ( sxy - sx * sy / numused ) / Math.sqrt( denom );
        return correl;
    }

    /**
     * @param correl double
     * @return int
     */
    public static int correlAsByte( double correl ) {
        if ( correl == -1.0 ) {
            return 0;
        }
        return ( int ) ( Math.ceil( ( correl + 1.0 ) * 128 ) - 1 );
    }

    /**
     * Find the approximate Pearson correlation required to meet a particular pvalue. If the pvalue is <=0 or >= 1,
     * returns 1 and 0 respectively.
     * 
     * @param pval double
     * @param count int
     * @return double
     */
    public static double correlationForPvalue( double pval, int count ) {

        if ( pval >= 1.0 ) {
            return 0.0;
        }

        if ( pval <= 0.0 ) {
            return 1.0;
        }

        if ( count < 3 ) {
            return 0.0; // warn?
        }

        double z = Math.abs( Probability.normalInverse( pval ) );

        double v = z / Math.sqrt( count - 3.0 );

        double corrguess = unFisherTransform( v );
        assert Math.abs( corrguess ) <= 1.1 : "Invalid correlation: " + corrguess; // sanity.
        // double goback = pvalue( corrguess, count );

        // System.err.println( "est corr=" + corrguess + " with pvalue " + goback + " ==? original p=" + pval );

        return Math.min( corrguess, 1.0 );
    }

    /**
     * Compute the t-statistic associated with a Pearson correlation.
     * 
     * @param correl Pearson correlation
     * @param dof Degrees of freedom (n - 2)
     * @return double
     */
    public static double correlationTstat( double correl, int dof ) {
        return correl / Math.sqrt( ( 1.0 - correl * correl ) / dof );
    }

    /**
     * Compute Pearson correlation when there are no missing values.
     * 
     * @param ival
     * @param jval
     * @param meani
     * @param meanj
     * @param sqrti
     * @param sqrtj
     * @return
     */
    public static double correlFast( double[] ival, double[] jval, double meani, double meanj, double sqrti,
            double sqrtj ) {
        double sxy = 0.0;
        for ( int k = 0, n = ival.length; k < n; k++ ) {
            sxy += ( ival[k] - meani ) * ( jval[k] - meanj );
        }
        return sxy / ( sqrti * sqrtj );
    }

    /**
     * Compute the Fisher z transform of the Pearson correlation.
     * 
     * @param r Correlation coefficient.
     * @return Fisher transform of the Correlation.
     */
    public static double fisherTransform( double r ) {

        if ( Math.abs( r ) == 1.0 ) {
            return Double.POSITIVE_INFINITY;
        }

        if ( r == 0.0 ) return 0.0;

        if ( !isValidPearsonCorrelation( r ) ) {
            throw new IllegalArgumentException( "Invalid correlation " + r );
        }

        return 0.5 * Math.log( ( 1.0 + r ) / ( 1.0 - r ) );
    }

    /**
     * Fisher-transform a list of Pearson correlations.
     * 
     * @param e
     * @return
     */
    public static DoubleArrayList fisherTransform( DoubleArrayList e ) {
        DoubleArrayList r = new DoubleArrayList( e.size() );
        for ( int i = 0; i < e.size(); i++ ) {
            r.add( CorrelationStats.fisherTransform( e.getQuick( i ) ) );
        }
        return r;
    }

    /**
     * Test if a value is a reasonable Pearson correlation (in the range -1 to 1; values outside of this range are
     * acceptable within a small roundoff.
     * 
     * @param r
     * @return
     */
    public static boolean isValidPearsonCorrelation( double r ) {
        return r + Constants.SMALL >= -1.0 && r - Constants.SMALL <= 1.0;
    }

    /**
     * Compute pvalue for the pearson correlation, using the t distribution method.
     * 
     * @param correl Pearson correlation.
     * @param count Number of items used to calculate the correlation. NOT the degrees of freedom.
     * @return double one-side pvalue
     */
    public static double pvalue( double correl, int count ) {

        double acorrel = Math.abs( correl );

        if ( acorrel == 1.0 ) {
            return 0.0;
        }

        if ( acorrel == 0.0 ) {
            return 1.0;
        }

        int dof = count - 2;

        if ( dof <= 0 ) {
            return 1.0;
        }

        int bin = ( int ) Math.ceil( acorrel / BINSIZE );
        if ( count <= MAXCOUNT ) {
            synchronized ( correlationPvalLookupLock ) {
                double cached = correlationPvalLookup.getQuick( bin, dof );
                if ( cached != 0.0 ) {
                    return cached;
                }
            }
        }
        double t = correlationTstat( acorrel, dof );
        double p = Probability.studentT( dof, -t );

        /*
         * Alternate method: Fisher's transform.
         */
        // double rr = 0.5 * Math.log( ( 1 + correl ) / ( 1 - correl ) );
        // double z = rr * Math.sqrt( count - 3.0 );
        // double altp = 1.0 - Probability.normal( z );
        // System.err.println( "P for " + correl + ", n=" + count + " =" + String.format( "%.2g", p ) + " alt: "
        // + String.format( "%.2g", altp ) );

        if ( count < MAXCOUNT ) {
            synchronized ( correlationPvalLookupLock ) {
                correlationPvalLookup.setQuick( bin, dof, p );
            }
        }

        return p;

    }

    /**
     * Convert a p value into a value between 0 and 255 inclusive. This is done by taking the log, multiplying it by a
     * fixed value (currently 8). This means that pvalues less than 10^-32 are rounded to 10^-32.
     * 
     * @param correl double
     * @param count int
     * @return int
     */
    public static int pvalueAsByte( double correl, int count ) {
        int p = -( int ) Math.floor( PVALCHOP * Arithmetic.log10( pvalue( correl, count ) ) );

        if ( p < 0 ) {
            return 0;
        } else if ( p > 255 ) {
            return 255;
        }
        return p;
    }

    /**
     * @param correl Spearman's correlation
     * @param count
     * @return pvalue, two-tailed pvalue, computed using t distribution for large sample sizes or exact computation for
     *         small sample sizes.
     */
    public static double spearmanPvalue( double correl, int count ) {
        double acorrel = Math.abs( correl );
        int dof = count - 2;

        if ( dof <= 0 ) {
            return 1.0;
        }
        int bin = ( int ) Math.ceil( acorrel / BINSIZE );
        if ( count <= MAXCOUNT ) {
            synchronized ( spearmanPvalLookupLock ) {
                double cached = spearmanPvalLookup.getQuick( bin, dof );
                if ( cached != 0.0 ) {
                    return cached;
                }
            }
        }

        double p;
        if ( count > 1290 ) { // this is the threshold used by R (cor.test.R), to avoid overflows.
            double t = correlationTstat( acorrel, dof );
            p = Probability.studentT( dof, -t );
            // studentT returns the lower tail so we use -t to effectively get the
            // upper tail.
        } else {
            p = spearmanPvalueSmallSample( acorrel, count ); // returns the upper tail for rho>0.
        }

        assert p <= 0.5 : "Pvalue was " + p + " for correl=" + correl + ", count=" + count
                + ", expected value less than 0.5";
        p = Math.min( 1.0, 2.0 * p ); // two-tailed.
        if ( count < MAXCOUNT ) {
            synchronized ( spearmanPvalLookupLock ) {
                spearmanPvalLookup.setQuick( bin, dof, p );
            }
        }
        return p;
    }

    /**
     * Reverse the Fisher z-transform of Pearson correlations
     * 
     * @param z
     * @return r
     */
    public static double unFisherTransform( double z ) {
        if ( Math.abs( z ) < Constants.SMALLISH ) {
            return 0.0;
        }

        if ( Math.abs( z ) > 20.0 ) { // ridiculously large
            return 1.0;
        }

        return ( Math.exp( 2.0 * z ) - 1.0 ) / ( Math.exp( 2.0 * z ) + 1.0 );
    }

    /**
     * Ported from R prho.c and cor.test.R which is in turn a port of a Fortran method (AS 89, Best and Roberts, Applied
     * Statistics 1975 p 377-379). We compute exact probabilities for very small values (< 9) and use a special
     * algorithm for larger values. At very large values the t-distribution can be used, this method will be slow.
     * <p>
     * The pvalues returned are based on the assumption of no tied ranks.
     * 
     * @param rho Spearman rank correlation
     * @param n number of samples -- NOT degrees of freedom. If there were missing values, you should compute n as the
     *        number of complete cases.
     * @return one-sided pvalue. This is the upper tail if rho is positive, lower tail if rho is negative (this is
     *         potentially confusing, basically we always return a value <=0.5)
     */
    private static double spearmanPvalueSmallSample( double rho, int n ) {

        /*
         * In R, sStat (is) is the S statistic, and gets computed in cor.test.R and passed into prho(). It's the sum
         * squared error of the ranks (the usual spearman test stat). We backcompute it here. As in the R version we
         * round it off, as properly S should be an integer.
         */
        int sStat = ( int ) Math.round( ( Math.pow( n, 3 ) - n ) * ( 1.0 - Math.abs( rho ) ) / 6.0 );

        // invaluable for debugging!
        // System.out.println( "rho=" + rho + "; N=" + n + "; S=" + sStat );

        if ( n <= 1 ) {
            throw new IllegalArgumentException();
        }

        if ( sStat <= 0.0 ) {
            if ( rho > 0 ) {
                return 0.0;
            }
            return 1.0;
        }

        double pv = 1.0;

        /*
         * Exact evaluation of probability for very small n
         */
        if ( n <= n_small ) {
            int[] ar = new int[n_small];
            int ifr;

            int n3 = n;
            n3 *= ( n3 * n3 - 1 ) / 3.0;/* = (n^3 - n)/3 */
            if ( sStat > n3 ) { /* larger than maximal value */
                return 0.0; // best possible pvalue...
            }

            int nfac = 1;
            for ( int i = 1; i <= n; ++i ) {
                nfac *= i;
                ar[i - 1] = i;
            }

            if ( sStat == n3 ) {
                ifr = 1;
            } else {
                int n1, mt;
                ifr = 0;
                for ( int m = 0; m < nfac; ++m ) {
                    int ise = 0;
                    for ( int i = 0; i < n; ++i ) {
                        n1 = i + 1 - ar[i];
                        ise += n1 * n1;
                    }
                    if ( sStat <= ise ) {
                        ++ifr;
                    }

                    n1 = n;
                    do {
                        mt = ar[0];
                        for ( int i = 1; i < n1; ++i ) {
                            ar[i - 1] = ar[i];
                        }
                        --n1;
                        ar[n1] = mt;
                    } while ( mt == n1 + 1 && n1 > 1 );
                }
            }
            pv = ifr / ( double ) nfac;

        } else {
            /*
             * Evaluation by Edgeworth series expansion. For most sample-wise genomics data sets this is what is used,
             * as N is typically on the order of magnitude 10-100.
             */

            double b = 1.0 / n;

            /*
             * This wasteful backcomputation of rho is designed to mimic the behaviour of R's implementation
             */
            double x = ( 6.0 * ( sStat - 1 ) * b / ( n * n - 1 ) - 1 ) * Math.sqrt( n - 1 );

            double y = x * x;
            double u = x
                    * b
                    * ( c1 + b * ( c2 + c3 * b ) + y
                            * ( -c4 + b * ( c5 + c6 * b ) - y * b
                                    * ( c7 + c8 * b - y * ( c9 - c10 * b + y * b * ( c11 - c12 * y ) ) ) ) );
            y = u / Math.exp( y / 2.0 );

            double pp = 1.0 - Probability.normal( x ); // mean 0, variance 1.
            pv = y + pp;
        }

        if ( pv > 0.5 ) pv = 1.0 - pv; // here we enforce our possibly confusing tail rule.
        if ( pv < 0.0 ) pv = 0.0;
        if ( pv > 1.0 ) pv = 1.0;
        return pv;

    }

}