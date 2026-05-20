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

import cern.colt.list.BooleanArrayList;
import org.apache.commons.math3.special.Gamma;

import cern.colt.function.DoubleFunction;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.jet.math.Functions;
import ubic.gemma.core.util.matrix.MatrixUtil;

// Java 8 only, fix later.
//import net.sourceforge.jdistlib.math.PolyGamma;
import static java.lang.Math.*;

/**
 * Assorted special functions, primarily concerning probability distributions. For cumBinomial use
 * cern.jet.stat.Probability.binomial.
 * <p>
 * Mostly ported from the R source tree (dhyper.c etc.), much due to Catherine Loader.
 *
 * @author Paul Pavlidis
 * @see <a href="http://hoschek.home.cern.ch/hoschek/colt/V1.0.3/doc/cern/jet/stat/Gamma.html">cern.jet.stat.gamma </a>
 * @see <a
 * href=
 * "http://hoschek.home.cern.ch/hoschek/colt/V1.0.3/doc/cern/jet/math/Arithmetic.html">cern.jet.math.arithmetic
 * </a>
 */
public class SpecFunc {


    private static final double SMALL = 1e-8;

    /**
     * See dbinom_raw.
     * <hr>
     *
     * @param x Number of successes
     * @param n Number of trials
     * @param p Probability of success
     * @return
     */
    public static double dbinom(double x, double n, double p) {

        if (p < 0 || p > 1 || n < 0) throw new IllegalArgumentException();

        return dbinom_raw(x, n, p, 1 - p);
    }

    /**
     * Ported from R (Catherine Loader)
     * <p>
     * DESCRIPTION
     * <p>
     * Given a sequence of r successes and b failures, we sample n (\le b+r) items without replacement. The
     * hypergeometric probability is the probability of x successes:
     *
     * <pre>
     *
     *               choose(r, x) * choose(b, n-x)
     * (x; r,b,n) =  -----------------------------  =
     *                     choose(r+b, n)
     *
     *    dbinom(x,r,p) * dbinom(n-x,b,p)
     * = --------------------------------
     *       dbinom(n,r+b,p)
     * </pre>
     * <p>
     * for any p. For numerical stability, we take p=n/(r+b); with this choice, the denominator is not exponentially
     * small.
     */
    public static double dhyper(int x, int r, int b, int n) {
        double p, q, p1, p2, p3;

        if (r < 0 || b < 0 || n < 0 || n > r + b) throw new IllegalArgumentException();

        if (x < 0) return 0.0;

        if (n < x || r < x || n - x > b) return 0;
        if (n == 0) return ((x == 0) ? 1 : 0);

        p = ((double) n) / ((double) (r + b));
        q = ((double) (r + b - n)) / ((double) (r + b));

        p1 = dbinom_raw(x, r, p, q);
        p2 = dbinom_raw(n - x, b, p, q);
        p3 = dbinom_raw(n, r + b, p, q);

        return p1 * p2 / p3;
    }

    /**
     * Ported from R phyper.c
     * <p>
     * Sample of n balls from NR red and NB black ones; x are red
     * <p>
     *
     * @param x         - number of reds retrieved == successes
     * @param NR        - number of reds in the urn. == positives
     * @param NB        - number of blacks in the urn == negatives
     * @param n         - the total number of objects drawn == successes + failures
     * @param lowerTail
     * @return cumulative hypergeometric distribution.
     */
    public static double phyper(int x, int NR, int NB, int n, boolean lowerTail) {

        double d, pd;

        if (NR < 0 || NB < 0 || n < 0 || n > NR + NB) {
            throw new IllegalArgumentException("Need NR>=0, NB>=0,n>=0,n>NR+NB");
        }

        if (x * (NR + NB) > n * NR) {
            /* Swap tails. */
            int oldNB = NB;
            NB = NR;
            NR = oldNB;
            x = n - x - 1;
            lowerTail = !lowerTail;
        }

        if (x < 0) return 0.0;

        d = dhyper(x, NR, NB, n);
        pd = pdhyper(x, NR, NB, n);

        return lowerTail ? d * pd : 1.0 - (d * pd);
    }

    /**
     * @param x
     * @return
     */
    public static double trigammaInverse(double x) {
        return trigammaInverse(new DenseDoubleMatrix1D(new double[]{x})).get(0);
    }

    /**
     * Ported from limma
     *
     * @param x vector to be operated on
     * @return solution for y: trigamma(y) = x
     */
    public static DoubleMatrix1D trigammaInverse(DoubleMatrix1D x) {

        if (x == null || x.size() == 0)
            return null;
        DoubleMatrix1D y = x.copy();

        // very large, small, missing and negative values are handled specially as per limma implementation
        BooleanArrayList ok = MatrixUtil.matchingCriteria(y, a -> !Double.isNaN(a) && a <= 1e7 && a >= 1.0e-6);
        y = MatrixUtil.applyToIndicesMatchingCriteria(y, a -> a < 0, a -> Double.NaN);
        y = MatrixUtil.applyToIndicesMatchingCriteria(y, a -> a > 1e7, a -> 1 / sqrt(a));
        y = MatrixUtil.applyToIndicesMatchingCriteria(y, a -> a < 1.0e-6, a -> 1 / a);

        // The remaining values are subject to iterative search for solution.
        DoubleMatrix1D yok = MatrixUtil.stripNonOK(y, ok);

        // y = 0.5 + 1 /x
        double LB = 0.5;
        DoubleMatrix1D  yokop = yok.copy().assign(Functions.inv).assign(Functions.plus(LB));

        double iter = 0;
        do {

            DoubleMatrix1D tri = yokop.copy().assign(
                    new DoubleFunction() {
                        @Override
                        public final double apply(double a) {
                            return Gamma.trigamma(a);
                        }
                    });

            DoubleMatrix1D tri2 = tri.copy();
            DoubleMatrix1D dif = tri.assign(yok, Functions.div).assign(Functions.neg)
                    .assign(Functions.plus(1.0)).assign(tri2, Functions.mult)
                    .assign(new DenseDoubleMatrix1D(PolyGamma.psigamma(yokop.toArray(), 2)), Functions.div);

            // update y
            yokop.assign(dif, Functions.plus);

            // max(-dif/y)
            double max = yokop.copy().assign(Functions.inv).assign(dif, Functions.mult).assign(Functions.neg)
                    .aggregate(Functions.max, Functions.identity);
            if (max < SMALL) break;
        } while (++iter < 50);

        MatrixUtil.replaceValues(y, ok, yokop);

        return y;

    }

    /**
     * Ported from bd0.c in R source.
     * <p>
     * Evaluates the "deviance part"
     *
     * <pre>
     *    bd0(x,M) :=  M * D0(x/M) = M*[ x/M * log(x/M) + 1 - (x/M) ] =
     *         =  x * log(x/M) + M - x
     * </pre>
     * <p>
     * where M = E[X] = n*p (or = lambda), for x, M &gt; 0
     * <p>
     * in a manner that should be stable (with small relative error) for all x and np. In particular for x/np close to
     * 1, direct evaluation fails, and evaluation is based on the Taylor series of log((1+v)/(1-v)) with v =
     * (x-np)/(x+np).
     *
     * @param x
     * @param np
     * @return
     */
    private static double bd0(double x, double np) {
        double ej, s, s1, v;
        int j;

        if (Math.abs(x - np) < 0.1 * (x + np)) {
            v = (x - np) / (x + np);
            s = (x - np) * v;/* s using v -- change by MM */
            ej = 2 * x * v;
            v = v * v;
            for (j = 1; ; j++) { /* Taylor series */
                ej *= v;
                s1 = s + ej / ((j << 1) + 1);
                if (s1 == s) /* last term was effectively 0 */
                    return (s1);
                s = s1;
            }
        }
        /* else: | x - np | is not too small */
        return (x * Math.log(x / np) + np - x);
    }

    /**
     * Ported from R dbinom.c
     * <p>
     * Due to Catherine Loader, catherine@research.bell-labs.com.
     * <p>
     * To compute the binomial probability, call dbinom(x,n,p). This checks for argument validity, and calls
     * dbinom_raw().
     * <p>
     * dbinom_raw() does the actual computation; note this is called by other functions in addition to dbinom()).
     * <ol>
     * <li>dbinom_raw() has both p and q arguments, when one may be represented more accurately than the other (in
     * particular, in df()).
     * <li>dbinom_raw() does NOT check that inputs x and n are integers. This should be done in the calling function,
     * where necessary.
     * <li>Also does not check for 0 <= p <= 1 and 0 <= q <= 1 or NaN's. Do this in the calling function.
     * </ol>
     * <hr>
     *
     * @param x Number of successes
     * @param n Number of trials
     * @param p Probability of success
     * @param q 1 - p
     */
    private static double dbinom_raw(double x, double n, double p, double q) {
        double f, lc;

        if (p == 0) return ((x == 0) ? 1 : 0);
        if (q == 0) return ((x == n) ? 1 : 0);

        if (x == 0) {
            if (n == 0) return 1;
            lc = (p < 0.1) ? -bd0(n, n * q) - n * p : n * Math.log(q);
            return (Math.exp(lc));
        }
        if (x == n) {
            lc = (q < 0.1) ? -bd0(n, n * p) - n * q : n * Math.log(p);
            return (Math.exp(lc));
        }
        if (x < 0 || x > n) return (0);

        lc = stirlerr(n) - stirlerr(x) - stirlerr(n - x) - bd0(x, n * p) - bd0(n - x, n * q);
        f = (2 * Math.PI * x * (n - x)) / n;

        return Math.exp(lc) / Math.sqrt(f);
    }

    /**
     * Ported from R phyper.c
     * <p>
     * Calculate
     *
     * <pre>
     *       phyper (x, NR, NB, n, TRUE, FALSE)
     * [log]  ----------------------------------
     *         dhyper (x, NR, NB, n, FALSE)
     * </pre>
     * <p>
     * without actually calling phyper. This assumes that
     *
     * <pre>
     * x * ( NR + NB ) &lt;= n * NR
     * </pre>
     *
     * <hr>
     *
     * @param x  - number of reds retrieved == successes
     * @param NR - number of reds in the urn. == positives
     * @param NB - number of blacks in the urn == negatives
     * @param n  - the total number of objects drawn == successes + failures
     */
    private static double pdhyper(int x, int NR, int NB, int n) {
        double sum = 0.0;
        double term = 1.0;

        while (x > 0.0 && term >= Double.MIN_VALUE * sum) {
            term *= (double) x * (NB - n + x) / (n + 1 - x) / (NR + 1 - x);
            sum += term;
            x--;
        }

        return 1.0 + sum;
    }

    /**
     * Ported from stirlerr.c (Catherine Loader).
     * <p>
     * Note that this is the same functionality as colt's Arithemetic.stirlingCorrection. I am keeping this version for
     * compatibility with R.
     *
     * <pre>
     *         stirlerr(n) = log(n!) - log( sqrt(2*pi*n)*(n/e)&circ;n )
     *                    = log Gamma(n+1) - 1/2 * [log(2*pi) + log(n)] - n*[log(n) - 1]
     *                    = log Gamma(n+1) - (n + 1/2) * log(n) + n - log(2*pi)/2
     * </pre>
     */
    private static double stirlerr(double n) {

        double S0 = 0.083333333333333333333; /* 1/12 */
        double S1 = 0.00277777777777777777778; /* 1/360 */
        double S2 = 0.00079365079365079365079365; /* 1/1260 */
        double S3 = 0.000595238095238095238095238;/* 1/1680 */
        double S4 = 0.0008417508417508417508417508;/* 1/1188 */

        /*
         * error for 0, 0.5, 1.0, 1.5, ..., 14.5, 15.0.
         */
        double[] sferr_halves = new double[]{0.0, /* n=0 - wrong, place holder only */
                0.1534264097200273452913848, /* 0.5 */
                0.0810614667953272582196702, /* 1.0 */
                0.0548141210519176538961390, /* 1.5 */
                0.0413406959554092940938221, /* 2.0 */
                0.03316287351993628748511048, /* 2.5 */
                0.02767792568499833914878929, /* 3.0 */
                0.02374616365629749597132920, /* 3.5 */
                0.02079067210376509311152277, /* 4.0 */
                0.01848845053267318523077934, /* 4.5 */
                0.01664469118982119216319487, /* 5.0 */
                0.01513497322191737887351255, /* 5.5 */
                0.01387612882307074799874573, /* 6.0 */
                0.01281046524292022692424986, /* 6.5 */
                0.01189670994589177009505572, /* 7.0 */
                0.01110455975820691732662991, /* 7.5 */
                0.010411265261972096497478567, /* 8.0 */
                0.009799416126158803298389475, /* 8.5 */
                0.009255462182712732917728637, /* 9.0 */
                0.008768700134139385462952823, /* 9.5 */
                0.008330563433362871256469318, /* 10.0 */
                0.007934114564314020547248100, /* 10.5 */
                0.007573675487951840794972024, /* 11.0 */
                0.007244554301320383179543912, /* 11.5 */
                0.006942840107209529865664152, /* 12.0 */
                0.006665247032707682442354394, /* 12.5 */
                0.006408994188004207068439631, /* 13.0 */
                0.006171712263039457647532867, /* 13.5 */
                0.005951370112758847735624416, /* 14.0 */
                0.005746216513010115682023589, /* 14.5 */
                0.005554733551962801371038690
                /* 15.0 */
        };

        double nn;

        if (n <= 15.0) {
            nn = n + n;
            if (nn == (int) nn) return (sferr_halves[(int) nn]);
            return (Gamma.logGamma(n + 1.) - (n + 0.5) * Math.log(n) + n - Constants.M_LN_SQRT_2PI);
        }

        nn = n * n;
        if (n > 500) return ((S0 - S1 / nn) / n);
        if (n > 80) return ((S0 - (S1 - S2 / nn) / nn) / n);
        if (n > 35) return ((S0 - (S1 - (S2 - S3 / nn) / nn) / nn) / n);
        /* 15 < n <= 35 : */
        return ((S0 - (S1 - (S2 - (S3 - S4 / nn) / nn) / nn) / nn) / n);
    }

}

// Temporary.
class PolyGamma {
    private static final double klog10Of2 = log10(2),
            kDefaultWDTol = max(pow(2, -53), 0.5e-18);
    private static final int kMaxValue = 100,
            DBL_MANT_DIG = 53,
            DBL_MIN_EXP = -1021;
    private static final String sErrorDomain = "Math Error: DOMAIN"; //$NON-NLS-1$

    // Bernoulli Numbers
    static private double bvalues[] = {
            1.00000000000000000e+00,
            -5.00000000000000000e-01,
            1.66666666666666667e-01,
            -3.33333333333333333e-02,
            2.38095238095238095e-02,
            -3.33333333333333333e-02,
            7.57575757575757576e-02,
            -2.53113553113553114e-01,
            1.16666666666666667e+00,
            -7.09215686274509804e+00,
            5.49711779448621554e+01,
            -5.29124242424242424e+02,
            6.19212318840579710e+03,
            -8.65802531135531136e+04,
            1.42551716666666667e+06,
            -2.72982310678160920e+07,
            6.01580873900642368e+08,
            -1.51163157670921569e+10,
            4.29614643061166667e+11,
            -1.37116552050883328e+13,
            4.88332318973593167e+14,
            -1.92965793419400681e+16
    };

    public static final double[] dpsifn(double x, int n, int kode, int m) {
        double ans[] = new double[n + 1];
        int i, j, k, mm, mx, nn, np, nx, fn;
        double arg, den, elim, eps, fln, fx, rln, rxsq;
        double s, slope, t, ta, tk, tol, tols, tss, tst;
        double tt, t1, t2, xdmln, xdmy = 0, xinc = 0, xln = 0, xm, xmin;
        double xq, yint;
        double trm[] = new double[23], trmr[] = new double[kMaxValue + 1];
        boolean flag1 = false;

        if (n < 0 || kode < 1 || kode > 2 || m < 1)
            return null;

        if (x <= 0.) {
            /*
             * use Abramowitz & Stegun 6.4.7 "Reflection Formula"
             * psi(k, x) = (-1)^k psi(k, 1-x) - pi^{n+1} (d/dx)^n cot(x)
             */
            if (x == (long) x) {
                /* non-positive integer : +Inf or NaN depends on n */
                for (j = 0; j < m; j++) /* k = j + n : */
                    ans[j] = ((j + n) % 2 == 1) ? Double.POSITIVE_INFINITY : Double.NaN;
                return ans;
            }
            dpsifn(1. - x, n, 1, m);
            /*
             * ans[j] == (-1)^(k+1) / gamma(k+1) * psi(k, 1 - x)
             * for j = 0:(m-1) , k = n + j
             */

            /* Cheat for now: only work for m = 1, n in {0,1,2,3} : */
            if (m > 1 || n > 3) /* doesn't happen for digamma() .. pentagamma() */
                return null;
            x *= PI; /* pi * x */
            if (n == 0)
                tt = cos(x) / sin(x);
            else if (n == 1)
                tt = -1 / pow(sin(x), 2);
            else if (n == 2)
                tt = 2 * cos(x) / pow(sin(x), 3);
            else if (n == 3)
                tt = -2 * (2 * pow(cos(x), 2) + 1) / pow(sin(x), 4);
            else /* can not happen! */
                tt = Double.NaN;
            /* end cheat */

            s = (n % 2 == 1) ? -1. : 1.;/* s = (-1)^n */
            /*
             * t := pi^(n+1) * d_n(x) / gamma(n+1) , where
             * d_n(x) := (d/dx)^n cot(x)
             */
            t1 = t2 = s = 1.;
            for (k = 0, j = k - n; j < m; k++, j++, s = -s) {
                /* k == n+j , s = (-1)^k */
                t1 *= PI;/* t1 == pi^(k+1) */
                if (k >= 2)
                    t2 *= k;/* t2 == k! == gamma(k+1) */
                if (j >= 0) /* by cheat above, tt === d_k(x) */
                    ans[j] = s * (ans[j] + t1 / t2 * tt);
            }
            if (n == 0 && kode == 2)
                ans[0] += xln;
            return ans;
        } /* x <= 0 */

        //nz = 0;
        mm = m;
        nx = -DBL_MIN_EXP; //min(-DBL_MIN_EXP, DBL_MAX_EXP);
        //r1m5 = klog10Of2;
        //r1m4 = pow(FLT_RADIX, 1-DBL_MANT_DIG) * 0.5;
        //wdtol = kDefaultWDTol; //max(pow(FLT_RADIX, 1-DBL_MANT_DIG) * 0.5, 0.5e-18);

        /* elim = approximate exponential over and underflow limit */

        elim = 2.302 * (nx * klog10Of2 - 3.0);
        xln = log(x);
        xdmln = xln;
        for (; ; ) {
            nn = n + mm - 1;
            fn = nn;
            t = (fn + 1) * xln;

            /* overflow and underflow test for small and large x */

            /* !* if (fabs(t) > elim) { *! */
            if (abs(t) > elim) {
                if (t <= 0.0)
                    return null;
            } else {
                if (x < kDefaultWDTol) {
                    ans[0] = pow(x, -n - 1.0);
                    if (mm != 1) {
                        for (k = 1; k < mm; k++)
                            ans[k] = ans[k - 1] / x;
                    }
                    if (n == 0 && kode == 2)
                        ans[0] += xln;
                    return ans;
                }

                /* compute xmin and the number of terms of the series, fln+1 */

                rln = klog10Of2 * DBL_MANT_DIG;
                rln = min(rln, 18.06);
                /* !* fln = fmax2(rln, 3.0) - 3.0; *! */
                fln = max(rln, 3.0) - 3.0;
                yint = 3.50 + 0.40 * fln;
                slope = 0.21 + fln * (0.0006038 * fln + 0.008677);
                xm = yint + slope * fn;
                mx = (int) xm + 1;
                xmin = mx;
                if (n != 0) {
                    xm = -2.302 * rln - min(0.0, xln);
                    arg = xm / n;
                    arg = min(0.0, arg);
                    eps = exp(arg);
                    xm = 1.0 - eps;
                    if (abs(arg) < 1.0e-3)
                        xm = -arg;
                    fln = x * xm / eps;
                    xm = xmin - x;
                    if (xm > 7.0 && fln < 15.0)
                        break;
                }
                xdmy = x;
                xdmln = xln;
                xinc = 0.0;
                if (x < xmin) {
                    nx = (int) x;
                    xinc = xmin - nx;
                    xdmy = x + xinc;
                    xdmln = log(xdmy);
                }

                /* generate w(n+mm-1, x) by the asymptotic expansion */

                t = fn * xdmln;
                t1 = xdmln + xdmln;
                t2 = t + xdmln;
                /* !* tk = fmax2(fabs(t), fmax2(fabs(t1), fabs(t2))); *! */
                tk = max(abs(t), max(abs(t1), abs(t2)));
                if (tk <= elim) {
                    flag1 = true;
                    break;
                }
            }

            //nz++;
            mm--;
            ans[mm] = 0.0;
            if (mm == 0)
                return ans;
        } // end for(;;;)

        if (!flag1) {
            nn = (int) fln + 1;
            np = n + 1;
            t1 = (n + 1) * xln;
            t = exp(-t1);
            s = t;
            den = x;
            for (i = 1; i <= nn; i++) {
                den = den + 1.0;
                trm[i] = pow(den, -np);
                s += trm[i];
            }
            ans[0] = s;
            if (n == 0 && kode == 2)
                ans[0] = s + xln;

            if (mm != 1) {
                /* generate higher derivatives, j > n */
                tol = kDefaultWDTol / 5.0;
                for (j = 1; j < mm; j++) {
                    t = t / x;
                    s = t;
                    tols = t * tol;
                    den = x;
                    for (i = 1; i <= nn; i++) {
                        den += 1.0;
                        trm[i] /= den;
                        s += trm[i];
                        if (trm[i] < tols)
                            break;
                    }
                    ans[j] = s;
                }
            }
            return ans;
        }

        tss = exp(-t);
        tt = 0.5 / xdmy;
        t1 = tt;
        tst = kDefaultWDTol * tt;
        if (nn != 0)
            t1 = tt + 1.0 / fn;
        rxsq = 1.0 / (xdmy * xdmy);
        ta = 0.5 * rxsq;
        t = (fn + 1) * ta;
        s = t * bvalues[2];
        /* !* if (fabs(s) >= tst) { *! */
        if (abs(s) >= tst) {
            tk = 2.0;
            for (k = 4; k <= 22; k++) {
                t = t * ((tk + fn + 1) / (tk + 1.0)) * ((tk + fn) / (tk + 2.0)) * rxsq;
                trm[k] = t * bvalues[k - 1];
                /* !* if (fabs(trm[k]) < tst) *! */
                if (abs(trm[k]) < tst)
                    break;
                s += trm[k];
                tk += 2.0;
            }
        }
        s = (s + t1) * tss;
        if (xinc != 0.0) {
            /* backward recur from xdmy to x */
            nx = (int) xinc;
            np = nn + 1;
            if (nx > kMaxValue)
                return null;
            if (nn == 0) {
                for (i = 1; i <= nx; i++)
                    s += 1.0 / (x + nx - i);

                if (kode != 2)
                    ans[0] = s - xdmln;
                else if (xdmy != x) {
                    xq = xdmy / x;
                    ans[0] = s - log(xq);
                }
                return ans;
            }
            xm = xinc - 1.0;
            fx = x + xm;

            /* this loop should not be changed. fx is accurate when x is small */

            for (i = 1; i <= nx; i++) {
                trmr[i] = pow(fx, -np);
                s += trmr[i];
                xm -= 1.0;
                fx = x + xm;
            }
        }
        ans[mm - 1] = s;
        if (fn == 0) {
            if (kode != 2)
                ans[0] = s - xdmln;
            else if (xdmy != x) {
                xq = xdmy / x;
                ans[0] = s - log(xq);
            }
            return ans;
        }

        /* generate lower derivatives, j < n+mm-1 */

        for (j = 2; j <= mm; j++) {
            fn--;
            tss *= xdmy;
            t1 = tt;
            if (fn != 0)
                t1 = tt + 1.0 / fn;
            t = (fn + 1) * ta;
            s = t * bvalues[2];
            if (abs(s) >= tst) {
                tk = 4 + fn;
                for (k = 4; k <= 22; k++) {
                    trm[k] = trm[k] * (fn + 1) / tk;
                    if (abs(trm[k]) < tst)
                        break;
                    s += trm[k];
                    tk += 2.0;
                }
            }
            s = (s + t1) * tss;

            if (xinc != 0.0) {
                if (fn == 0) {
                    for (i = 1; i <= nx; i++)
                        s += 1.0 / (x + nx - i);

                    if (kode != 2)
                        ans[0] = s - xdmln;
                    else if (xdmy != x) {
                        xq = xdmy / x;
                        ans[0] = s - log(xq);
                    }
                }
                xm = xinc - 1.0;
                fx = x + xm;
                for (i = 1; i <= nx; i++) {
                    trmr[i] = trmr[i] * fx;
                    s += trmr[i];
                    xm -= 1.0;
                    fx = x + xm;
                }
            }
            ans[mm - j] = s;
            if (fn == 0) {
                if (kode != 2)
                    ans[0] = s - xdmln;
                else if (xdmy != x) {
                    xq = xdmy / x;
                    ans[0] = s - log(xq);
                }
                return ans;
            }
        } // end for(j=2 ; j<=mm ; j++)
        return ans;
    }

    public static final double psigamma(double x, int n) {
        /* n-th derivative of psi(x); e.g., psigamma(x,0) == digamma(x) */
        double[] ans;

        //int n = (int) rint(deriv);
        //if(n > kMaxValue) return Double.NaN;
        ans = dpsifn(x, n, 1, 1);
        if (ans == null)
            return Double.NaN;
        /* ans == A := (-1)^(n+1) / gamma(n+1) * psi(n, x) */
        double result = -ans[0]; /* = (-1)^(0+1) * gamma(0+1) * A */
        for (int k = 1; k <= n; k++)
            result *= (-k);/* = (-1)^(k+1) * gamma(k+1) * A */
        return result;/* = psi(n, x) */
    }

    public static final double digamma(double x) {
        double ans[] = dpsifn(x, 0, 1, 1);
        if (ans == null)
            throw new ArithmeticException(sErrorDomain);
        return -ans[0];
    }

    public static final double trigamma(double x) {
        double ans[] = dpsifn(x, 1, 1, 1);
        if (ans == null)
            throw new ArithmeticException(sErrorDomain);
        return ans[0];
    }

    public static final double tetragamma(double x) {
        double ans[] = dpsifn(x, 2, 1, 1);
        if (ans == null)
            throw new ArithmeticException(sErrorDomain);
        return -2.0 * ans[0];
    }

    public static final double pentagamma(double x) {
        double ans[] = dpsifn(x, 3, 1, 1);
        if (ans == null)
            throw new ArithmeticException(sErrorDomain);
        return 6.0 * ans[0];
    }

    public static final double[] psigamma(double[] x, int deriv) {
        int n = x.length;
        double[] r = new double[n];
        for (int i = 0; i < n; i++)
            r[i] = psigamma(x[i], deriv);
        return r;
    }

    public static final double[] digamma(double[] x) {
        return psigamma(x, 0);
    }

    public static final double[] trigamma(double[] x) {
        return psigamma(x, 1);
    }

    public static final double[] tetragamma(double[] x) {
        return psigamma(x, 2);
    }

    public static final double[] pentagamma(double[] x) {
        return psigamma(x, 3);
    }

    /**
     * Log of multivariate psigamma function
     * By: Roby Joehanes
     *
     * @param a
     * @param p     the dimension or order
     * @param deriv digamma = 0, trigamma = 1, ... etc.
     * @return log multivariate psigamma
     */
    public static final double lmvpsigammafn(double a, int p, int deriv) {
        double sum = 0;
        for (int j = 1; j <= p; j++)
            sum += log(psigamma(a + (1 - j) / 2.0, deriv));
        return sum;
    }

}
