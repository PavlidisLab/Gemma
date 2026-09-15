/*
 * The baseCode project
 *
 * Copyright (c) 2017 University of British Columbia
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

package ubic.gemma.core.util.math.linearmodels;

import java.util.List;

import cern.colt.function.DoubleFunction;
import cern.colt.list.BooleanArrayList;
import cern.colt.list.DoubleArrayList;
import org.apache.commons.math3.special.Gamma;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.jet.math.Functions;
import ubic.gemma.core.util.matrix.MatrixUtil;
import ubic.gemma.core.util.math.DescriptiveWithMissing;
import ubic.gemma.core.util.math.SpecFunc;

/**
 * Implements methods described in
 * <p>
 * Smyth, G. K. (2004). Linear models and empirical Bayes methods for assessing differential expression in microarray
 * experiments. Statistical Applications in Genetics and Molecular Biology Volume 3, Issue 1, Article 3.
 * <p>
 * R code snippets in comments are from squeezeVar.R in the limma source code.
 *
 * @author paul
 */
public class ModeratedTstat {

    private final static DoubleFunction digamma = new DoubleFunction() {
        @Override
        public double apply(double v) {
            return Gamma.digamma(v);
        }
    };
    private final static DoubleFunction trigammainverse = new DoubleFunction() {
        @Override
        public double apply(double v) {
            return SpecFunc.trigammaInverse(v);
        }
    };
    private final static DoubleFunction trigamma = new DoubleFunction() {
        @Override
        public double apply(double v) {
            return Gamma.trigamma(v);
        }
    };
    public static final double TOOSMALL = Math.pow(10, -15);

    /**
     * Does essentially the same thing as limma::ebayes
     *
     * @param fit which will be modified
     */
    public static void ebayes(LeastSquaresFit fit) {
        List<LinearModelSummary> summaries = fit.summarize();
        DoubleMatrix1D sigmas = new DenseDoubleMatrix1D(new double[summaries.size()]);
        DoubleMatrix1D dofs = new DenseDoubleMatrix1D(new double[summaries.size()]);
        int i = 0;
        // corner case can get nulls, example: GSE10778
        for ( LinearModelSummary lms : summaries) {
            if (Double.isNaN(lms.getSigma())) {
                sigmas.set(i, Double.NaN);
                dofs.set(i, Double.NaN);
            } else {
                sigmas.set(i, lms.getSigma());
                dofs.set(i, lms.getResidualsDof());  // collect vector of dofs instead of assuming fixed value.
            }
            i++;

        }
        squeezeVar(sigmas.copy().assign(Functions.square), dofs, fit);
    }

    /**
     * defining 'ok' as non-missing and non-infinite and not very close to zero as per limma implementation
     *
     * @param x
     * @return
     */
    private static final BooleanArrayList okVars(DoubleMatrix1D x) {
        assert x.size() > 0;
        BooleanArrayList answer = new BooleanArrayList(x.size());
        for (int i = 0; i < x.size(); i++) {
            double a = x.getQuick(i);
            answer.add(!(Double.isNaN(a) || Double.isInfinite(a) || a < -1e-15));
        }
        return answer;
    }

    private static final BooleanArrayList okDfs(DoubleMatrix1D x) {
        assert x.size() > 0;
        BooleanArrayList answer = new BooleanArrayList(x.size());

        for (int i = 0; i < x.size(); i++) {
            double a = x.getQuick(i);
            answer.add(!(Double.isNaN(a) || Double.isInfinite(a) || a < TOOSMALL));
        }
        return answer;
    }

    /*
     * @param vars variances
     * @param df1s vector of degrees of freedom
     * @return the scale (aka s2.prior or s20 in limma) and df2 (aka df.prior) in a double array of length 2
     */
    protected static double[] fitFDist(final DoubleMatrix1D vars, final DoubleMatrix1D df1s) {

        BooleanArrayList ok = MatrixUtil.conjunction(okVars(vars), okDfs(df1s));

        DoubleMatrix1D x = MatrixUtil.stripNonOK(vars, ok);
        DoubleMatrix1D df1 = MatrixUtil.stripNonOK(df1s, ok);

        if (x.size() == 0) {
            throw new IllegalStateException("There were no valid values of variance to perform eBayes parameter estimation");
        }

        // stay away from zero variance
        x = x.assign(Functions.max(1e-5 * DescriptiveWithMissing.median(new DoubleArrayList(vars.toArray()))));

        // z <- log(x)
        DoubleMatrix1D z = x.copy().assign(Functions.log);

        // e <- z-digamma(df1/2)+log(df1/2)
        DoubleMatrix1D e1 = df1.copy().assign(Functions.div(2.0)).assign(digamma);
        DoubleMatrix1D e2 = df1.copy().assign(Functions.div(2.0)).assign(Functions.log);
        DoubleMatrix1D e = z.copy().assign(e1, Functions.minus).assign(e2, Functions.plus);

        int n = x.size();
        if (n < 2) {
            throw new IllegalStateException("Too few valid variance values to do eBayes parameter estimation (require at least 2)");
        }

        //  emean <- mean(e)
        double emean = e.zSum() / n;
        // evar <- sum((e-emean)^2)/(n-1)
        double evar = e.copy().assign(Functions.minus(emean)).aggregate(Functions.plus, Functions.square)
                / (n - 1);

        // evar <- evar - mean(trigamma(df1/2))
        evar = evar - df1.copy().assign(Functions.div(2.0)).assign(trigamma).zSum() / df1.size();
        double df2;
        double s20;
        if (evar > 0.0) {
            // df2 <- 2*trigammaInverse(evar)
            df2 = 2 * SpecFunc.trigammaInverse(evar);

            // s20 <- exp(emean+digamma(df2/2)-log(df2/2))
            s20 = Math.exp(emean + Gamma.digamma(df2 / 2.0) - Math.log(df2 / 2.0));

        } else {
            df2 = Double.POSITIVE_INFINITY;
            // s20 <- mean(x)
            s20 = x.copy().aggregate(Functions.plus, Functions.identity) / x.size();
        }

        return new double[]{s20, df2};
    }


    /*
     * Return the scale and df2. Original implementation, does not handle missing values, kept here for posterity/comparison/debugging.
     */
    @Deprecated
    private static double[] fitFDistNoMissing(DoubleMatrix1D x, double df1) {

        // stay away from zero valuess
        x = x.copy().assign(Functions.max(1e-5));
        // z <- log(x)
        // e <- z-digamma(df1/2)+log(df1/2)
        DoubleMatrix1D e = x.copy().assign(Functions.log).assign(Functions.minus(Gamma.digamma(df1 / 2.0)))
                .assign(Functions.plus(Math.log(df1 / 2.0)));

        int nok = x.size(); // number of oks - need to implement checks
        //  emean <- mean(e)
        double emean = e.copy().zSum() / nok;
        // evar <- sum((e-emean)^2)/(nok-1L)
        double evar = e.copy().assign(Functions.minus(emean)).aggregate(Functions.plus, Functions.square)
                / (nok - 1);

        evar = evar - Gamma.trigamma(df1 / 2.0);

        double df2;
        double s20;
        if (evar > 0) {
            // df2 <- 2*trigammaInverse(evar)
            df2 = 2 * SpecFunc.trigammaInverse(evar);

            // s20 <- exp(emean+digamma(df2/2)-log(df2/2))
            s20 = Math.exp(emean + Gamma.digamma(df2 / 2.0) - Math.log(df2 / 2.0));

        } else {
            df2 = Double.POSITIVE_INFINITY;
            // s20 <- mean(x)
            s20 = x.copy().zSum() / x.size();
        }

        return new double[]{s20, df2};
    }

    /**
     * Ignoring robust and covariate for now
     *
     * @param var initial values of estimated residual variance = sigma^2 = rssq/rdof; this will be moderated
     * @param df  vector of degrees of freedom
     * @param fit will be updated with new info; call fit.summarize() to get updated pvalues etc.
     * @return varPost for testing mostly
     */
    protected static DoubleMatrix1D squeezeVar(DoubleMatrix1D var, DoubleMatrix1D df, LeastSquaresFit fit) {

        double[] ffit = fitFDist(var, df);
        double dfPrior = ffit[1];

        DoubleMatrix1D varPost = squeezeVariances(var, df, ffit);

        if (fit != null)
            fit.ebayesUpdate(dfPrior, ffit[0], varPost);

        return varPost;
    }

    /**
     * @param var vector of estimated residual variances from original model fit
     * @param df  vector of dfs
     * @param fit result of fitFDist() (s2.prior and dfPrior)
     * @return vector of squeezed variances (varPost or s2.post)
     */
    protected static DoubleMatrix1D squeezeVariances(DoubleMatrix1D var, DoubleMatrix1D df, double[] fit) {
        double varPrior = fit[0];
        double dfPrior = fit[1];

        //   out$var.post <- (df*var + out$df.prior*out$var.prior) / df.total

        DoubleMatrix1D dfTotal = df.copy().assign(Functions.plus(dfPrior));

        return var.copy().assign(df, Functions.mult)
                .assign(Functions.plus(dfPrior * varPrior)).assign(dfTotal, Functions.div);

    }

}
