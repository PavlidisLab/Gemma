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
package ubic.gemma.core.util.math.linearmodels;

import cern.colt.bitvector.BitVector;
import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.jet.math.Functions;
import cern.jet.stat.Descriptive;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.math3.distribution.FDistribution;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrixFactory;
import ubic.gemma.core.util.matrix.MatrixUtil;
import ubic.gemma.core.util.matrix.ObjectMatrix;
import ubic.gemma.core.util.math.Constants;
import ubic.gemma.core.util.math.linalg.QRDecomposition;

import java.util.*;

/**
 * For performing "bulk" linear model fits, but also offers simple methods for simple univariate and multivariate
 * regression for a single vector of dependent variables (data). Has support for ebayes-like shrinkage of variance.
 * <p>
 * Data with missing values is handled but is less memory efficient and somewhat slower. The main cost is that when
 * there are no missing values, a single QR decomposition can be performed.
 *
 * @author paul
 */
public class LeastSquaresFit {

    private static Logger log = LoggerFactory.getLogger(LeastSquaresFit.class);

    /**
     * For ebayes
     */
    boolean hasBeenShrunken = false;

    /**
     * The (raw) design matrix
     */
    private DoubleMatrix2D A;

    /**
     * Lists which factors (terms) are associated with which columns of the design matrix; 0 indicates the intercept.
     * Used for ANOVA
     */
    private List<Integer> assign = new ArrayList<>();

    /**
     * Row-specific assign values, for use when there are missing values; Used for ANOVA
     */
    private List<List<Integer>> assigns = new ArrayList<>();

    /**
     * Independent variables - data
     */
    private DoubleMatrix2D b;

    /**
     * Model fit coefficients (the x in Ax=b)
     */
    private DoubleMatrix2D coefficients = null;

    /**
     * Original design matrix, if provided or generated from constructor arguments.
     */
    private DesignMatrix designMatrix;

    /**
     * For ebayes. Default value is zero
     */
    private double dfPrior = 0;

    /**
     * Fitted values
     */
    private DoubleMatrix2D fitted;

    /**
     * True if model includes intercept
     */
    private boolean hasIntercept = true;

    /**
     * True if data has missing values.
     */
    private boolean hasMissing = false;

    /**
     * QR decomposition of the design matrix; will only be non-null if the QR is the same for all data.
     */
    private QRDecomposition qr = null;

    /**
     * Used if we have missing values so QR might be different for each row. The key
     * is the bitvector representing the values present. DO NOT
     * ACCESS DIRECTLY, use the getQR methods.
     */
    private Map<BitVector, QRDecomposition> qrs = new HashMap<>();

    /**
     * Used if we are using weighted regresion, so QR is different for each row. DO NOT
     * ACCESS DIRECTLY, use the getQR methods.
     */
    private Map<Integer, QRDecomposition> qrsForWeighted = new HashMap<>();

    private int residualDof = -1;

    /**
     * Used if we have missing value so RDOF might be different for each row (we can actually get away without this)
     */
    private List<Integer> residualDofs = new ArrayList<>();

    /**
     * Residuals of the fit
     */
    private DoubleMatrix2D residuals = null;

    /**
     * Optional, but useful
     */
    private List<String> rowNames;

    /**
     * Map of data rows to sdUnscaled (a la limma) Use of treemap: probably not necessary.
     */
    private Map<Integer, DoubleMatrix1D> stdevUnscaled = new TreeMap<>();

    /**
     * Names of the factors (terms)
     */
    private List<String> terms;

    /**
     * Map of row indices to values-present key.
     */
    private Map<Integer, BitVector> valuesPresentMap = new HashMap<>();

    /**
     * ebayes per-item variance estimates (if computed, null otherwise)
     */
    private DoubleMatrix1D varPost = null;

    /**
     * prior variance estimate (if computed, null otherwise)
     */
    private Double varPrior = null;

    /*
     * For weighted regression
     */
    private DoubleMatrix2D weights = null;

    /**
     * Preferred interface if you want control over how the design is set up.
     *
     * @param designMatrix
     * @param data
     */
    public LeastSquaresFit(DesignMatrix designMatrix, DoubleMatrix<String, String> data) {
        this.designMatrix = designMatrix;
        this.A = designMatrix.getDoubleMatrix();
        this.assign = designMatrix.getAssign();
        this.terms = designMatrix.getTerms();

        this.rowNames = data.getRowNames();
        this.b = new DenseDoubleMatrix2D(data.asArray());
        boolean hasInterceptTerm = this.terms.contains( LinearModelSummary.INTERCEPT_COEFFICIENT_NAME);
        this.hasIntercept = designMatrix.hasIntercept();
        assert hasInterceptTerm == this.hasIntercept : diagnosis(null);
        fit();
    }

    /**
     * Weighted least squares fit between two matrices
     *
     * @param designMatrix
     * @param data
     * @param weights      to be used in modifying the influence of the observations in data.
     */
    public LeastSquaresFit(DesignMatrix designMatrix, DoubleMatrix<String, String> data,
                           final DoubleMatrix2D weights) {
        this.designMatrix = designMatrix;
        DoubleMatrix2D X = designMatrix.getDoubleMatrix();
        this.assign = designMatrix.getAssign();
        this.terms = designMatrix.getTerms();
        this.A = X;
        this.rowNames = data.getRowNames();
        this.b = new DenseDoubleMatrix2D(data.asArray());
        boolean hasInterceptTerm = this.terms.contains( LinearModelSummary.INTERCEPT_COEFFICIENT_NAME);
        this.hasIntercept = designMatrix.hasIntercept();
        assert hasInterceptTerm == this.hasIntercept : diagnosis(null);
        this.weights = weights;
        fit();
    }

    /**
     * Preferred interface for weighted least squares fit between two matrices
     *
     * @param designMatrix
     * @param b            the data
     * @param weights      to be used in modifying the influence of the observations in vectorB.
     */
    public LeastSquaresFit(DesignMatrix designMatrix, DoubleMatrix2D b, final DoubleMatrix2D weights) {

        this.designMatrix = designMatrix;
        DoubleMatrix2D X = designMatrix.getDoubleMatrix();
        this.assign = designMatrix.getAssign();
        this.terms = designMatrix.getTerms();
        this.A = X;
        this.b = b;
        boolean hasInterceptTerm = this.terms.contains( LinearModelSummary.INTERCEPT_COEFFICIENT_NAME);
        this.hasIntercept = designMatrix.hasIntercept();
        assert hasInterceptTerm == this.hasIntercept : diagnosis(null);

        this.weights = weights;

        fit();

    }

    /**
     * Least squares fit between two vectors. Always adds an intercept!
     *
     * @param vectorA Design
     * @param vectorB Data
     */
    public LeastSquaresFit(DoubleMatrix1D vectorA, DoubleMatrix1D vectorB) {
        assert vectorA.size() == vectorB.size();

        this.A = new DenseDoubleMatrix2D(vectorA.size(), 2);
        this.b = new DenseDoubleMatrix2D(1, vectorB.size());

        for (int i = 0; i < vectorA.size(); i++) {
            A.set(i, 0, 1);
            A.set(i, 1, vectorA.get(i));
            b.set(0, i, vectorB.get(i));
        }

        fit();
    }

    /**
     * Stripped-down interface for simple use. Least squares fit between two vectors. Always adds an intercept!
     *
     * @param vectorA Design
     * @param vectorB Data
     * @param weights to be used in modifying the influence of the observations in vectorB.
     */
    public LeastSquaresFit(DoubleMatrix1D vectorA, DoubleMatrix1D vectorB, final DoubleMatrix1D weights) {

        assert vectorA.size() == vectorB.size();
        assert vectorA.size() == weights.size();

        this.A = new DenseDoubleMatrix2D(vectorA.size(), 2);
        this.b = new DenseDoubleMatrix2D(1, vectorB.size());
        this.weights = new DenseDoubleMatrix2D(1, weights.size());

        for (int i = 0; i < vectorA.size(); i++) {
            //   double ws = Math.sqrt( weights.get( i ) );
            A.set(i, 0, 1);
            A.set(i, 1, vectorA.get(i));
            b.set(0, i, vectorB.get(i));
            this.weights.set(0, i, weights.get(i));
        }

        fit();
    }

    /**
     * ANOVA not possible (use the other constructors)
     *
     * @param A Design matrix, which will be used directly in least squares regression
     * @param b Data matrix, containing data in rows.
     */
    public LeastSquaresFit(DoubleMatrix2D A, DoubleMatrix2D b) {
        this.A = A;
        this.b = b;
        fit();
    }

    /**
     * Weighted least squares fit between two matrices
     *
     * @param A       Design
     * @param b       Data
     * @param weights to be used in modifying the influence of the observations in b. If null, will be ignored.
     */
    public LeastSquaresFit(DoubleMatrix2D A, DoubleMatrix2D b, final DoubleMatrix2D weights) {
        assert A != null;
        assert b != null;
        assert A.rows() == b.columns();
        assert weights == null || b.columns() == weights.columns();
        assert weights == null || b.rows() == weights.rows();

        this.A = A;
        this.b = b;
        this.weights = weights;

        fit();

    }

    /**
     * @param sampleInfo information that will be converted to a design matrix; intercept term is added.
     * @param data       Data matrix
     */
    public LeastSquaresFit(ObjectMatrix<String, String, Object> sampleInfo, DenseDoubleMatrix2D data) {

        this.designMatrix = new DesignMatrix(sampleInfo, true);

        this.hasIntercept = true;
        this.A = designMatrix.getDoubleMatrix();
        this.assign = designMatrix.getAssign();
        this.terms = designMatrix.getTerms();

        this.b = data;
        fit();
    }

    /**
     * @param sampleInfo
     * @param data
     * @param interactions add interaction term (two-way only is supported)
     */
    public LeastSquaresFit(ObjectMatrix<String, String, Object> sampleInfo, DenseDoubleMatrix2D data,
                           boolean interactions) {
        this.designMatrix = new DesignMatrix(sampleInfo, true);

        if (interactions) {
            addInteraction();
        }

        this.A = designMatrix.getDoubleMatrix();
        this.assign = designMatrix.getAssign();
        this.terms = designMatrix.getTerms();

        this.b = data;
        fit();
    }

    /**
     * NamedMatrix allows easier handling of the results.
     *
     * @param design information that will be converted to a design matrix; intercept term is added.
     * @param b      Data matrix
     */
    public LeastSquaresFit(ObjectMatrix<String, String, Object> design, DoubleMatrix<String, String> b) {
        this.designMatrix = new DesignMatrix(design, true);

        this.A = designMatrix.getDoubleMatrix();
        this.assign = designMatrix.getAssign();
        this.terms = designMatrix.getTerms();

        this.b = new DenseDoubleMatrix2D(b.asArray());
        this.rowNames = b.getRowNames();
        fit();
    }

    /**
     * NamedMatrix allows easier handling of the results.
     *
     * @param design information that will be converted to a design matrix; intercept term is added.
     * @param data   Data matrix
     */
    public LeastSquaresFit(ObjectMatrix<String, String, Object> design, DoubleMatrix<String, String> data,
                           boolean interactions) {
        this.designMatrix = new DesignMatrix(design, true);

        if (interactions) {
            addInteraction();
        }

        DoubleMatrix2D X = designMatrix.getDoubleMatrix();
        this.assign = designMatrix.getAssign();
        this.terms = designMatrix.getTerms();
        this.A = X;
        this.b = new DenseDoubleMatrix2D(data.asArray());
        fit();
    }

    /**
     * The matrix of coefficients x for Ax = b (parameter estimates). Each column represents one fitted model (e.g., one
     * gene); there is a row for each parameter.
     *
     * @return
     */
    public DoubleMatrix2D getCoefficients() {
        return coefficients;
    }

    public double getDfPrior() {
        return dfPrior;
    }

    public DoubleMatrix2D getFitted() {
        return fitted;
    }

    public int getResidualDof() {
        return residualDof;
    }

    public List<Integer> getResidualDofs() {
        return residualDofs;
    }

    public DoubleMatrix2D getResiduals() {
        return residuals;
    }

    /**
     * @return externally studentized residuals (assumes we have only one QR)
     */
    public DoubleMatrix2D getStudentizedResiduals() {
        int dof = this.residualDof - 1; // MINUS for external studentizing!!

        assert dof > 0;

        if (this.hasMissing) {
            throw new UnsupportedOperationException("Studentizing not supported with missing values");
        }

        DoubleMatrix2D result = this.residuals.like();

        /*
         * Diagnonal of the hat matrix at i (hi) is the squared norm of the ith row of Q
         */
        DoubleMatrix2D q = this.getQR(0).getQ();

        DoubleMatrix1D hatdiag = new DenseDoubleMatrix1D(residuals.columns());
        for (int j = 0; j < residuals.columns(); j++) {
            double hj = q.viewRow(j).aggregate(Functions.plus, Functions.square);
            if (1.0 - hj < Constants.TINY) {
                hj = 1.0;
            }
            hatdiag.set(j, hj);
        }

        /*
         * Measure sum of squares of residuals / residualDof
         */
        for (int i = 0; i < residuals.rows(); i++) {

            // these are 'internally studentized'
            // double sdhat = Math.sqrt( residuals.viewRow( i ).aggregate( Functions.plus, Functions.square ) / dof );

            DoubleMatrix1D residualRow = residuals.viewRow(i);

            if (this.weights != null) {
                // use weighted residuals.
                DoubleMatrix1D w = weights.viewRow(i).copy().assign(Functions.sqrt);
                residualRow = residualRow.copy().assign(w, Functions.mult);
            }

            double sum = residualRow.aggregate(Functions.plus, Functions.square);

            for (int j = 0; j < residualRow.size(); j++) {

                double hj = hatdiag.get(j);

                // this is how we externalize...
                double sigma;

                if (hj < 1.0) {
                    sigma = Math.sqrt((sum - Math.pow(residualRow.get(j), 2) / (1.0 - hj)) / dof);
                } else {
                    sigma = Math.sqrt(sum / dof);
                }

                double res = residualRow.getQuick(j);
                double studres = res / (sigma * Math.sqrt(1.0 - hj));

                if (log.isDebugEnabled()) log.debug("sigma=" + sigma + " hj=" + hj + " stres=" + studres);

                result.set(i, j, studres);
            }
        }
        return result;
    }

    public DoubleMatrix1D getVarPost() {
        return varPost;
    }

    public double getVarPrior() {
        return varPrior;
    }

    public DoubleMatrix2D getWeights() {
        return weights;
    }

    public boolean isHasBeenShrunken() {
        return hasBeenShrunken;
    }

    public boolean isHasMissing() {
        return hasMissing;
    }

    /**
     * @return summaries. ANOVA will not be computed. If ebayesUpdate has been run, variance and degrees of freedom
     * estimated using the limma eBayes algorithm will be used.
     */
    public List<LinearModelSummary> summarize() {
        return this.summarize(false);
    }

    /**
     * @param anova if true, ANOVA will be computed
     * @return
     */
    public List<LinearModelSummary> summarize(boolean anova) {
        List<LinearModelSummary> lmsresults = new ArrayList<>();

        List<GenericAnovaResult> anovas = null;
        if (anova) {
            anovas = this.anova();
        }

        StopWatch timer = new StopWatch();
        timer.start();
        log.info("Summarizing");
        for (int i = 0; i < this.coefficients.columns(); i++) {
            LinearModelSummaryImpl lms = summarize(i);
            lms.setAnova(anovas != null ? anovas.get(i) : null);
            lmsresults.add(lms);
            if (timer.getTime() > 10000 && i > 0 && i % 10000 == 0) {
                log.info("Summarized " + i);
            }
        }
        log.info("Summzarized " + this.coefficients.columns() + " results");

        return lmsresults;
    }

    /**
     * @param anova perform ANOVA, otherwise only basic summarization will be done. If ebayesUpdate has been run,
     *              variance and degrees of freedom
     *              estimated using the limma eBayes algorithm will be used.
     * @return
     */
    public Map<String, LinearModelSummary> summarizeByKeys(boolean anova) {
        List<LinearModelSummary> summaries = this.summarize(anova);
        Map<String, LinearModelSummary> result = new LinkedHashMap<>();
        for (LinearModelSummary lms : summaries) {
            if (StringUtils.isBlank(lms.getKey())) {
                /*
                 * Perhaps we should just use an integer.
                 */
                throw new IllegalStateException("Key must not be blank");
            }

            if (result.containsKey(lms.getKey())) {
                throw new IllegalStateException("Duplicate key " + lms.getKey());
            }
            result.put(lms.getKey(), lms);
        }
        return result;
    }

    /**
     * Compute ANOVA based on the model fit (Type I SSQ, sequential)
     * <p>
     * The idea is to add up the sums of squares (and dof) for all parameters associated with a particular factor.
     * <p>
     * This code is more or less ported from R summary.aov.
     *
     * @return
     */
    protected List<GenericAnovaResult> anova() {

        DoubleMatrix1D ones = new DenseDoubleMatrix1D(residuals.columns());
        ones.assign(1.0);

        /*
         * For ebayes, instead of this value (divided by rdof), we'll use the moderated sigma^2
         */
        DoubleMatrix1D residualSumsOfSquares;

        if (this.weights == null) {
            residualSumsOfSquares = MatrixUtil.multWithMissing(residuals.copy().assign(Functions.square),
                    ones);
        } else {
            residualSumsOfSquares = MatrixUtil.multWithMissing(
                    residuals.copy().assign(this.weights.copy().assign(Functions.sqrt), Functions.mult).assign(Functions.square),
                    ones);
        }

        DoubleMatrix2D effects = null;
        if (this.hasMissing || this.weights != null) {
            effects = new DenseDoubleMatrix2D(this.b.rows(), this.A.columns());
            effects.assign(Double.NaN);
            for (int i = 0; i < this.b.rows(); i++) {
                QRDecomposition qrd = this.getQR(i);
                if (qrd == null) {
                    // means we did not get a fit
                    for (int j = 0; j < effects.columns(); j++) {
                        effects.set(i, j, Double.NaN);
                    }
                    continue;
                }

                /*
                 * Compute Qty for the specific y, dealing with missing values.
                 */
                DoubleMatrix1D brow = b.viewRow(i);
                DoubleMatrix1D browWithoutMissing = MatrixUtil.removeMissingOrInfinite(brow);

                DoubleMatrix1D tqty;
                if (weights != null) {
                    DoubleMatrix1D w = MatrixUtil.removeMissingOrInfinite(brow, this.weights.viewRow(i).copy().assign(Functions.sqrt));
                    assert w.size() == browWithoutMissing.size();
                    DoubleMatrix1D bw = browWithoutMissing.copy().assign(w, Functions.mult);
                    tqty = qrd.effects(bw);
                } else {
                    tqty = qrd.effects(browWithoutMissing);
                }

                // view just part we need; put values back so missingness is restored.
                for (int j = 0; j < qrd.getRank(); j++) {
                    effects.set(i, j, tqty.get(j));
                }
            }

        } else {
            assert this.qr != null;
            effects = qr.effects(this.b.viewDice().copy()).viewDice();
        }

        /* this is t(Qfty), the effects associated with the parameters only! We already have the residuals. */
        effects.assign(Functions.square); // because we're going to compute sums of squares.

        /*
         * Add up the ssr for the columns within each factor.
         */
        Set<Integer> facs = new TreeSet<>();
        facs.addAll(assign);

        DoubleMatrix2D ssq = new DenseDoubleMatrix2D(effects.rows(), facs.size() + 1);
        DoubleMatrix2D dof = new DenseDoubleMatrix2D(effects.rows(), facs.size() + 1);
        dof.assign(0.0);
        ssq.assign(0.0);
        List<Integer> assignToUse = assign; // if has missing values, this will get swapped.

        for (int i = 0; i < ssq.rows(); i++) {

            ssq.set(i, facs.size(), residualSumsOfSquares.get(i));
            int rdof;
            if (this.residualDofs.isEmpty()) {
                rdof = this.residualDof;
            } else {
                rdof = this.residualDofs.get(i);
            }
            /*
             * Store residual DOF in the last column.
             */
            dof.set(i, facs.size(), rdof);

            if (!assigns.isEmpty()) {
                // when missing values are present we end up here.
                assignToUse = assigns.get(i);
            }

            // these have been squared.
            DoubleMatrix1D effectsForRow = effects.viewRow(i);

            if (assignToUse.size() != effectsForRow.size()) {
                /*
                 * Effects will have NaNs, just so you know.
                 */
                log.debug("Check me: effects has missing values");
            }

            for (int j = 0; j < assignToUse.size(); j++) {

                double valueToAdd = effectsForRow.get(j);
                int col = assignToUse.get(j);
                if (col > 0 && !this.hasIntercept) {
                    col = col - 1;
                }

                /*
                 * Accumulate the sums for the different parameters associated with the same factor. When the data is
                 * "constant" you can end up with a tiny but non-zero coefficient,
                 * but it's bogus. See bug 3177. Ignore missing values.
                 */
                if (!Double.isNaN(valueToAdd) && valueToAdd > Constants.SMALL) {
                    ssq.set(i, col, ssq.get(i, col) + valueToAdd);
                    dof.set(i, col, dof.get(i, col) + 1);
                }
            }
        }

        DoubleMatrix1D denominator;
        if (this.hasBeenShrunken) {
            denominator = this.varPost.copy();
        } else {
            if (this.residualDofs.isEmpty()) {
                // when there's just one value...
                denominator = residualSumsOfSquares.copy().assign(Functions.div(residualDof));
            } else {
                denominator = new DenseDoubleMatrix1D(residualSumsOfSquares.size());
                for (int i = 0; i < residualSumsOfSquares.size(); i++) {
                    denominator.set(i, residualSumsOfSquares.get(i) / residualDofs.get(i));
                }
            }
        }

        // Fstats and pvalues will go here. Just initializing.
        DoubleMatrix2D fStats = ssq.copy().assign(dof, Functions.div); // this is the numerator.
        DoubleMatrix2D pvalues = fStats.like();
        computeStats(dof, fStats, denominator, pvalues);

        return summarizeAnova(ssq, dof, fStats, pvalues);
    }

    /**
     * Provide results of limma eBayes algorithm. These will be used next time summarize is called on this.
     *
     * @param d  dfPrior
     * @param v  varPrior
     * @param vp varPost
     */
    protected void ebayesUpdate(double d, double v, DoubleMatrix1D vp) {
        this.dfPrior = d;
        this.varPrior = v; // somewhat confusingly, this is sd.prior in limma; var.prior gets used for B stat.
        this.varPost = vp; // also called s2.post; without ebayes this is the same as sigma^2 = rssq/rdof
        this.hasBeenShrunken = true;
    }

    /**
     * Compute and organize the various summary statistics for a fit.
     * <p>
     * If ebayes has been run, variance and degrees of freedom
     * estimated using the limma eBayes algorithm will be used.
     * <p>
     * Does not populate the ANOVA.
     *
     * @param i index of the fit to summarize
     * @return
     */
    LinearModelSummaryImpl summarize(int i) {

        String key = null;
        if (this.rowNames != null) {
            key = this.rowNames.get(i);
            if (key == null) log.warn("Key null at " + i);
        }

        QRDecomposition qrd = null;
        qrd = this.getQR(i);

        if (qrd == null) {
            log.debug("QR was null for item " + i);
            return new LinearModelSummaryImpl(key);
        }

        int rdf;
        if (this.residualDofs.isEmpty()) {
            rdf = this.residualDof; // no missing values, so it's global
        } else {
            rdf = this.residualDofs.get(i); // row-specific
        }
        assert !Double.isNaN(rdf);

        if (rdf == 0) {
            return new LinearModelSummaryImpl(key);
        }

        DoubleMatrix1D resid = MatrixUtil.removeMissingOrInfinite(this.residuals.viewRow(i));
        DoubleMatrix1D f = MatrixUtil.removeMissingOrInfinite(fitted.viewRow(i));

        DoubleMatrix1D rweights = null;
        DoubleMatrix1D sqrtweights = null;
        if (this.weights != null) {
            rweights = MatrixUtil.removeMissingOrInfinite(fitted.viewRow(i), this.weights.viewRow(i).copy());
            sqrtweights = rweights.copy().assign(Functions.sqrt);
        } else {
            rweights = new DenseDoubleMatrix1D(f.size()).assign(1.0);
            sqrtweights = rweights.copy();
        }

        DoubleMatrix1D allCoef = coefficients.viewColumn(i); // has NA for unestimated parameters.
        DoubleMatrix1D estCoef = MatrixUtil.removeMissingOrInfinite(allCoef); // estimated parameters.

        if (estCoef.size() == 0) {
            log.warn("No coefficients estimated for row " + i + this.diagnosis(qrd));
            log.info("Data for this row:\n" + this.b.viewRow(i));
            return new LinearModelSummaryImpl(key);
        }

        int rank = qrd.getRank();
        int n = qrd.getQ().rows();
        assert rdf == n - rank : "Rank was not correct, expected " + rdf + " but got Q rows=" + n + ", #Coef=" + rank
                + diagnosis(qrd);

        //        if (is.null(w)) {
        //            mss <- if (attr(z$terms, "intercept"))
        //                sum((f - mean(f))^2) else sum(f^2)
        //            rss <- sum(r^2)
        //        } else {
        //            mss <- if (attr(z$terms, "intercept")) {
        //                m <- sum(w * f /sum(w))
        //                sum(w * (f - m)^2)
        //            } else sum(w * f^2)
        //            rss <- sum(w * r^2)
        //            r <- sqrt(w) * r
        //        }
        double mss;

        if (weights != null) {

            if (hasIntercept) {
                //  m <- sum(w * f /sum(w))
                double m = f.copy().assign(Functions.div(rweights.zSum())).assign(rweights, Functions.mult).zSum();

                mss = f.copy().assign(Functions.minus(m)).assign(Functions.square).assign(rweights, Functions.mult).zSum();
            } else {
                mss = f.copy().assign(Functions.square).assign(rweights, Functions.mult).zSum();
            }

            assert resid.size() == rweights.size();
        } else {
            if (hasIntercept) {
                mss = f.copy().assign(Functions.minus(Descriptive.mean(new DoubleArrayList(f.toArray()))))
                        .assign(Functions.square).zSum();
            } else {
                mss = f.copy().assign(Functions.square).zSum();
            }
        }

        double rss = resid.copy().assign(Functions.square).assign(rweights, Functions.mult).zSum();
        if (weights != null) resid = resid.copy().assign(sqrtweights, Functions.mult);

        double resvar = rss / rdf; // sqrt of this is sigma.

        // matrix to hold the summary information.
        DoubleMatrix<String, String> summaryTable = DoubleMatrixFactory.dense(allCoef.size(), 4);
        summaryTable.assign(Double.NaN);
        summaryTable
                .setColumnNames(Arrays.asList(new String[]{"Estimate", "Std. Error", "t value", "Pr(>|t|)"}));

        // XtXi is (X'X)^-1; in R limma this is fit$cov.coefficients: "unscaled covariance matrix of the estimable coefficients"

        DoubleMatrix2D XtXi = qrd.chol2inv();

        // the diagonal has the (unscaled) variances s; NEGATIVE VALUES can occur when not of full rank...
        DoubleMatrix1D sdUnscaled = MatrixUtil.diagonal(XtXi).assign(Functions.sqrt);

        // in contrast the stdev.unscaled uses the gene-specific QR
        // //  stdev.unscaled[i,est] <- sqrt(diag(chol2inv(out$qr$qr,size=out$rank)))

        this.stdevUnscaled.put(i, sdUnscaled);

        DoubleMatrix1D sdScaled = MatrixUtil
                .removeMissingOrInfinite(MatrixUtil.diagonal(XtXi).assign(Functions.mult(resvar))
                        .assign(Functions.sqrt)); // wasteful...

        // AKA Qty

        DoubleMatrix1D effects = qrd.effects(MatrixUtil.removeMissingOrInfinite(this.b.viewRow(i).copy()).assign(sqrtweights, Functions.mult));

        // sigma is the estimated sd of the parameters. In limma, fit$sigma <- sqrt(mean(fit$effects[-(1:fit$rank)]^2)
        // in lm.series, it's same: sigma[i] <- sqrt(mean(out$effects[-(1:out$rank)]^2))
        // first p elements are associated with the coefficients; same as residuals (QQty) / resid dof.
        //        double sigma = Math
        //                .sqrt( resid.copy().assign( Functions.square ).aggregate( Functions.plus, Functions.identity )
        //                        / ( resid.size() - rank ) );

        // Based on effects
        double sigma = Math.sqrt(
                effects.copy().viewPart(rank, effects.size() - rank).aggregate(Functions.plus, Functions.square) / (effects.size() - rank));

        /*
         * Finally ready to compute t-stats and finish up.
         */

        DoubleMatrix1D tstats;
        TDistribution tdist;
        if (this.hasBeenShrunken) {
            /*
             * moderated t-statistic
             * out$t <- coefficients / stdev.unscaled / sqrt(out$s2.post)
             */
            tstats = estCoef.copy().assign(sdUnscaled, Functions.div).assign(
                    Functions.div(Math.sqrt(this.varPost.get(i))));

            /*
             * df.total <- df.residual + out$df.prior
             * df.pooled <- sum(df.residual,na.rm=TRUE)
             * df.total <- pmin(df.total,df.pooled)
             * out$df.total <- df.total
             * out$p.value <- 2*pt(-abs(out$t),df=df.total
             */

            double dfTotal = rdf + this.dfPrior;

            assert !Double.isNaN(dfTotal);
            tdist = new TDistribution(dfTotal);
        } else {
            /*
             * Or we could get these from
             * tstat.ord <- coefficients/ stdev.unscaled/ sigma
             * And not have to store the sdScaled.
             */
            tstats = estCoef.copy().assign(sdScaled, Functions.div);
            tdist = new TDistribution(rdf);
        }

        int j = 0;
        for (int ti = 0; ti < allCoef.size(); ti++) {
            double c = allCoef.get(ti);
            assert this.designMatrix != null;
            List<String> colNames = this.designMatrix.getMatrix().getColNames();

            String dmcolname;
            if (colNames == null) {
                dmcolname = "Column_" + ti;
            } else {
                dmcolname = colNames.get(ti);
            }

            summaryTable.addRowName(dmcolname);
            if (Double.isNaN(c)) {
                continue;
            }

            summaryTable.set(ti, 0, estCoef.get(j));
            summaryTable.set(ti, 1, sdUnscaled.get(j));
            summaryTable.set(ti, 2, tstats.get(j));

            double pval = 2.0 * (1.0 - tdist.cumulativeProbability(Math.abs(tstats.get(j))));
            summaryTable.set(ti, 3, pval);

            j++;

        }

        double rsquared = 0.0;
        double adjRsquared = 0.0;
        double fstatistic = 0.0;
        int numdf = 0;
        int dendf = 0;

        if (terms.size() > 1 || !hasIntercept) {
            int dfint = hasIntercept ? 1 : 0;
            rsquared = mss / (mss + rss);
            adjRsquared = 1 - (1 - rsquared) * ((n - dfint) / (double) rdf);

            fstatistic = mss / (rank - dfint) / resvar;

            // This doesn't get set otherwise??
            numdf = rank - dfint;
            dendf = rdf;

        } else {
            // intercept only, apparently.
            rsquared = 0.0;
            adjRsquared = 0.0;
        }

        // NOTE that not all the information stored in the summary is likely to be important/used,
        // while other information is probably still needed.
        LinearModelSummaryImpl lms = new LinearModelSummaryImpl( key, allCoef.toArray(), resid.toArray(), terms,
            summaryTable, effects.toArray(), sdUnscaled.toArray(), rsquared, adjRsquared, fstatistic, numdf, dendf,
            null, sigma, this.hasBeenShrunken, this.dfPrior );

        return lms;
    }

    /**
     *
     */
    private void addInteraction() {
        if (designMatrix.getTerms().size() == 1) {
            throw new IllegalArgumentException("Need at least two factors for interactions");
        }
        if (designMatrix.getTerms().size() != 2) {
            throw new UnsupportedOperationException("Interactions not supported for more than two factors");
        }
        this.designMatrix.addInteraction(designMatrix.getTerms().get(0), designMatrix.getTerms().get(1));
    }

    /**
     * Cache a QR. Only important if missing values are present or if using weights, otherwise we use the "global" QR.
     *
     * @param row           cannot be null; indicates the index into the datamatrix rows.
     * @param valuesPresent if null, this is taken to mean the row wasn't usable.
     * @param newQR         can be null, if valuePresent is null
     */
    private void addQR(Integer row, BitVector valuesPresent, QRDecomposition newQR) {

        /*
         * Use of weights takes precedence over missing values, in terms of how we store QRs. If we only have missing
         * values, often we can get away with a small number of distinct QRs. With weights, we assume they are different
         * for each data row.
         */
        if (this.weights != null) {
            this.qrsForWeighted.put(row, newQR);
            return;
        }

        assert row != null;

        if (valuesPresent == null) {
            valuesPresentMap.put(row, null);
        }

        QRDecomposition cachedQr = qrs.get(valuesPresent);
        if (cachedQr == null) {
            qrs.put(valuesPresent, newQR);
        }
        valuesPresentMap.put(row, valuesPresent);
    }

    /**
     *
     */
    private void checkForMissingValues() {
        for (int i = 0; i < b.rows(); i++) {
            for (int j = 0; j < b.columns(); j++) {
                double v = b.get(i, j);
                if (Double.isNaN(v) || Double.isInfinite(v)) {
                    this.hasMissing = true;
                    log.info("Data has missing values (at row=" + (i + 1) + " column=" + (j + 1));
                    break;
                }
            }
            if (this.hasMissing) break;
        }
    }

    /**
     * Drop, and track, redundant or constant columns (not counting the intercept, if present). This is only used if we
     * have missing values which would require changing the design depending on what is missing. Otherwise the model is
     * assumed to be clean. Note that this does not check the model for singularity, but does help avoid some obvious
     * causes of singularity.
     * <p>
     * NOTE Probably slow if we have to run this often; should cache re-used values.
     *
     * @param design
     * @param ypsize
     * @param droppedColumns populated by this call
     * @return
     */
    private DoubleMatrix2D cleanDesign(final DoubleMatrix2D design, int ypsize, List<Integer> droppedColumns) {

        /*
         * Drop constant columns or columns which are the same as another column.
         */
        for (int j = 0; j < design.columns(); j++) {
            if (j == 0 && this.hasIntercept) continue;
            double lastValue = Double.NaN;
            boolean constant = true;
            for (int i = 0; i < design.rows(); i++) {
                double thisvalue = design.get(i, j);
                if (i > 0 && thisvalue != lastValue) {
                    constant = false;
                    break;
                }
                lastValue = thisvalue;
            }
            if (constant) {
                log.debug("Dropping constant column " + j);
                droppedColumns.add(j);
                continue;
            }

            DoubleMatrix1D col = design.viewColumn(j);

            for (int p = 0; p < j; p++) {
                boolean redundant = true;
                DoubleMatrix1D otherCol = design.viewColumn(p);
                for (int v = 0; v < col.size(); v++) {
                    if (col.get(v) != otherCol.get(v)) {
                        redundant = false;
                        break;
                    }
                }
                if (redundant) {
                    log.debug("Dropping redundant column " + j);
                    droppedColumns.add(j);
                    break;
                }
            }

        }

        DoubleMatrix2D returnValue = MatrixUtil.dropColumns(design, droppedColumns);

        return returnValue;
    }

    /**
     * ANOVA f statistics etc.
     *
     * @param dof         raw degrees of freedom
     * @param fStats      results will be stored here
     * @param denominator residual sums of squares / rdof
     * @param pvalues     results will be stored here
     */
    private void computeStats(DoubleMatrix2D dof, DoubleMatrix2D fStats, DoubleMatrix1D denominator,
                              DoubleMatrix2D pvalues) {
        pvalues.assign(Double.NaN);
        int timesWarned = 0;
        for (int i = 0; i < fStats.rows(); i++) {

            int rdof;
            if (this.residualDofs.isEmpty()) {
                rdof = residualDof;
            } else {
                rdof = this.residualDofs.get(i);
            }

            for (int j = 0; j < fStats.columns(); j++) {

                double ndof = dof.get(i, j);

                if (ndof <= 0 || rdof <= 0) {
                    pvalues.set(i, j, Double.NaN);
                    fStats.set(i, j, Double.NaN);
                    continue;
                }

                if (j == fStats.columns() - 1) {
                    // don't fill in f & p values for the residual...
                    pvalues.set(i, j, Double.NaN);
                    fStats.set(i, j, Double.NaN);
                    continue;
                }

                /*
                 * Taking ratios of two very small values is not meaningful; happens if the data are ~constant.
                 */
                if (fStats.get(i, j) < Constants.SMALLISH && denominator.get(i) < Constants.SMALLISH) {
                    pvalues.set(i, j, Double.NaN);
                    fStats.set(i, j, Double.NaN);
                    continue;
                }

                fStats.set(i, j, fStats.get(i, j) / denominator.get(i));
                try {
                    FDistribution pf = new FDistribution(ndof, rdof + this.dfPrior);
                    pvalues.set(i, j, 1.0 - pf.cumulativeProbability(fStats.get(i, j)));
                } catch (NotStrictlyPositiveException e) {
                    if (timesWarned < 10) {
                        log.warn("Pvalue could not be computed for F=" + fStats.get(i, j) + "; denominator was="
                                + denominator.get(i) + "; Error: " + e.getMessage()
                                + " (limited warnings of this type will be given)");
                        timesWarned++;
                    }
                    pvalues.set(i, j, Double.NaN);
                }

            }
        }
    }

    /**
     * @param qrd
     * @return
     */
    private String diagnosis(QRDecomposition qrd) {
        StringBuilder buf = new StringBuilder();
        buf.append("\n--------\nLM State\n--------\n");
        buf.append("hasMissing=" + this.hasMissing + "\n");
        buf.append("hasIntercept=" + this.hasIntercept + "\n");
        buf.append("Design: " + this.designMatrix + "\n");
        if (this.b.rows() < 5) {
            buf.append("Data matrix: " + this.b + "\n");
        } else {
            buf.append("Data (first few rows): " + this.b.viewSelection(new int[]{0, 1, 2, 3, 4}, null) + "\n");

        }
        buf.append("Current QR:" + qrd + "\n");
        return buf.toString();
    }

    /**
     *
     */
    private void fit() {
        if (this.weights == null) {
            lsf();
            return;
        }
        wlsf();
    }

    /**
     * @param valuesPresent - only if we have unweighted regression
     * @return appropriate cached QR, or null
     */
    private QRDecomposition getQR(BitVector valuesPresent) {
        assert this.weights == null;
        return qrs.get(valuesPresent);
    }

    /**
     * Get the QR decomposition to use for data row given. If it has not yet been computed/cached return null.
     *
     * @param row
     * @return QR or null if the row wasn't usable. If there are no missing values and weights aren't used, this
     * returns
     * the global qr. If there are only missing values, this returns the QR that matches the pattern of
     * missing
     * values. If there are weights, a row-specific QR is returned.
     */
    private QRDecomposition getQR(Integer row) {
        if (!this.hasMissing && this.weights == null) {
            return this.qr;
        }

        if (this.weights != null)
            return qrsForWeighted.get(row);

        assert this.hasMissing;
        BitVector key = valuesPresentMap.get(row);
        if (key == null) return null;
        return qrs.get(key);

    }

    /**
     * Internal function that does the hard work in unweighted case.
     */
    private void lsf() {

        assert this.weights == null;

        checkForMissingValues();
        Algebra solver = new Algebra();

        if (this.hasMissing) {
            double[][] rawResult = new double[b.rows()][];
            for (int i = 0; i < b.rows(); i++) {

                DoubleMatrix1D row = b.viewRow(i);
                if (row.size() < 3) { // don't bother.
                    rawResult[i] = new double[A.columns()];
                    continue;
                }
                DoubleMatrix1D withoutMissing = lsfWmissing(i, row, A);
                if (withoutMissing == null) {
                    rawResult[i] = new double[A.columns()];
                } else {
                    rawResult[i] = withoutMissing.toArray();
                }
            }
            this.coefficients = new DenseDoubleMatrix2D(rawResult).viewDice();

        } else {

            this.qr = new QRDecomposition(A);
            this.coefficients = qr.solve(solver.transpose(b));
            this.residualDof = b.columns() - qr.getRank();
            if (residualDof <= 0) {
                throw new IllegalArgumentException(
                        "No residual degrees of freedom to fit the model" + diagnosis(qr));
            }

        }
        assert this.assign.isEmpty() || this.assign.size() == this.coefficients.rows() : assign.size()
                + " != # coefficients " + this.coefficients.rows();

        assert this.coefficients.rows() == A.columns();

        // It is somewhat wasteful to hold on to this.
        this.fitted = solver.transpose(MatrixUtil.multWithMissing(A, coefficients));

        if (this.hasMissing) {
            MatrixUtil.maskMissing(b, fitted);
        }

        this.residuals = b.copy().assign(fitted, Functions.minus);
    }

    /**
     * Perform OLS when there might be missing values, for a single vector of data y. If y doesn't have any missing
     * values this works normally.
     * <p>
     * Has side effect of filling in this.qrs and this.residualDofs, so run this "in order".
     *
     * @param row
     * @param y   the data to fit. For weighted ls, you must supply y*w
     * @param des the design matrix. For weighted ls, you must supply des*w.
     * @return the coefficients (a.k.a. x)
     */
    private DoubleMatrix1D lsfWmissing(Integer row, DoubleMatrix1D y, DoubleMatrix2D des) {
        Algebra solver = new Algebra();
        // This can potentially be improved by getting the indices of non-missing values and using that to make slices.

        List<Double> ywithoutMissingList = new ArrayList<>(y.size());
        int size = y.size();
        boolean hasAssign = !this.assign.isEmpty();
        int countNonMissing = 0;
        for (int i = 0; i < size; i++) {
            double v = y.getQuick(i);
            if (!Double.isNaN(v) && !Double.isInfinite(v)) {
                countNonMissing++;
            }
        }

        if (countNonMissing < 3) {
            /*
             * return nothing.
             */
            DoubleMatrix1D re = new DenseDoubleMatrix1D(des.columns());
            re.assign(Double.NaN);
            log.debug("Not enough non-missing values");
            this.addQR(row, null, null);
            this.residualDofs.add(countNonMissing - des.columns());
            if (hasAssign) this.assigns.add(new ArrayList<Integer>());
            return re;
        }

        double[][] rawDesignWithoutMissing = new double[countNonMissing][];
        int index = 0;
        boolean missing = false;

        BitVector bv = new BitVector(size);
        for (int i = 0; i < size; i++) {
            double yi = y.getQuick(i);
            if (Double.isNaN(yi) || Double.isInfinite(yi)) {
                missing = true;
                continue;
            }
            ywithoutMissingList.add(yi);
            bv.set(i);
            rawDesignWithoutMissing[index++] = des.viewRow(i).toArray();
        }
        double[] yWithoutMissing = ArrayUtils.toPrimitive(ywithoutMissingList.toArray(new Double[]{}));
        DenseDoubleMatrix2D yWithoutMissingAsMatrix = new DenseDoubleMatrix2D(new double[][]{yWithoutMissing});

        DoubleMatrix2D designWithoutMissing = new DenseDoubleMatrix2D(rawDesignWithoutMissing);

        boolean fail = false;
        List<Integer> droppedColumns = new ArrayList<>();
        designWithoutMissing = this.cleanDesign(designWithoutMissing, yWithoutMissingAsMatrix.size(), droppedColumns);

        if (designWithoutMissing.columns() == 0 || designWithoutMissing.columns() > designWithoutMissing.rows()) {
            fail = true;
        }

        if (fail) {
            DoubleMatrix1D re = new DenseDoubleMatrix1D(des.columns());
            re.assign(Double.NaN);
            this.addQR(row, null, null);
            this.residualDofs.add(countNonMissing - des.columns());
            if (hasAssign) this.assigns.add(new ArrayList<Integer>());
            return re;
        }

        QRDecomposition rqr = null;
        if (this.weights != null) {
            rqr = new QRDecomposition(designWithoutMissing);
            addQR(row, null, rqr);
        } else if (missing) {
            rqr = this.getQR(bv);
            if (rqr == null) {
                rqr = new QRDecomposition(designWithoutMissing);
                addQR(row, bv, rqr);
            }
        } else {
            // in the case of weighted least squares, the Design matrix has different weights
            // for every row observation, so recompute qr everytime.
            if (this.qr == null) {
                rqr = new QRDecomposition(des);
            } else {
                // presumably not weighted.Why would this be set already, though? Is this ever reached?
                rqr = this.qr;
            }
        }

        this.addQR(row, bv, rqr);

        int pivots = rqr.getRank();

        int rdof = yWithoutMissingAsMatrix.size() - pivots;
        this.residualDofs.add(rdof);

        DoubleMatrix2D coefs = rqr.solve(solver.transpose(yWithoutMissingAsMatrix));

        /*
         * Put NaNs in for missing coefficients that were dropped from our estimation.
         */
        if (designWithoutMissing.columns() < des.columns()) {
            DoubleMatrix1D col = coefs.viewColumn(0);
            DoubleMatrix1D result = new DenseDoubleMatrix1D(des.columns());
            result.assign(Double.NaN);
            int k = 0;
            List<Integer> assignForRow = new ArrayList<>();
            for (int i = 0; i < des.columns(); i++) {
                if (droppedColumns.contains(i)) {
                    // leave it as NaN.
                    continue;
                }

                if (hasAssign) assignForRow.add(this.assign.get(i));
                assert k < col.size();
                result.set(i, col.get(k));
                k++;
            }
            if (hasAssign) assigns.add(assignForRow);
            return result;
        }
        if (hasAssign) assigns.add(this.assign);
        return coefs.viewColumn(0);

    }

    /**
     * @param ssq
     * @param dof
     * @param fStats
     * @param pvalues
     * @return
     */
    private List<GenericAnovaResult> summarizeAnova(DoubleMatrix2D ssq, DoubleMatrix2D dof, DoubleMatrix2D fStats,
                                                    DoubleMatrix2D pvalues) {

        assert ssq != null;
        assert dof != null;
        assert fStats != null;
        assert pvalues != null;

        List<GenericAnovaResult> results = new ArrayList<>();
        for (int i = 0; i < fStats.rows(); i++) {
            Collection<AnovaEffect> efs = new ArrayList<>();

            /*
             * Don't put in ftest results for the residual thus the -1.
             */
            for (int j = 0; j < fStats.columns() - 1; j++) {
                String effectName = terms.get(j);
                assert effectName != null;
                AnovaEffect ae = new AnovaEffect(effectName, pvalues.get(i, j), fStats.get(i, j), dof.get(
                    i, j ), ssq.get( i, j ), effectName.contains( ":" ), false );
                efs.add(ae);
            }

            /*
             * Add residual
             */
            int residCol = fStats.columns() - 1;
            AnovaEffect ae = new AnovaEffect( "Residual", Double.NaN, Double.NaN, dof.get( i, residCol ) + this.dfPrior, ssq.get( i,
                residCol ), false, true );
            efs.add(ae);

            GenericAnovaResultImpl ao = new GenericAnovaResultImpl( this.rowNames != null ? this.rowNames.get( i ) : String.valueOf( i ), efs );
            results.add(ao);
        }
        return results;
    }

    /**
     * The weighted version which works like 'lm.wfit()' in R.
     */
    private void wlsf() {

        assert this.weights != null;

        checkForMissingValues();
        Algebra solver = new Algebra();

        /*
         * weight A and b: wts <- sqrt(w) A * wts, row * wts
         */
        List<DoubleMatrix2D> AwList = new ArrayList<>(b.rows());
        List<DoubleMatrix1D> bList = new ArrayList<>(b.rows());

        /*
         * Implemented like R::stats::lm.wfit : z <- .Call(C_Cdqrls, x * wts, y * wts, tol, FALSE), but we're doing each
         * y (gene) separately rather than in bulk since weights are different for each y.
         *
         *
         * Limma uses this approach in lm.series.
         */
        for (int i = 0; i < b.rows(); i++) {
            DoubleMatrix1D wts = this.weights.viewRow(i).copy().assign(Functions.sqrt);
            DoubleMatrix1D bw = b.viewRow(i).copy().assign(wts, Functions.mult);
            DoubleMatrix2D Aw = A.copy();
            for (int j = 0; j < Aw.columns(); j++) {
                Aw.viewColumn(j).assign(wts, Functions.mult);
            }
            AwList.add(Aw);
            bList.add(bw);
        }

        double[][] rawResult = new double[b.rows()][];

        if (this.hasMissing) {
            /*
             * Have to drop missing values from the design matrix, so invoke special code.
             */
            for (int i = 0; i < b.rows(); i++) {
                DoubleMatrix1D bw = bList.get(i);
                DoubleMatrix2D Aw = AwList.get(i);
                DoubleMatrix1D withoutMissing = lsfWmissing(i, bw, Aw);
                if (withoutMissing == null) {
                    rawResult[i] = new double[A.columns()];
                } else {
                    rawResult[i] = withoutMissing.toArray();
                }
            }

        } else {

            // do QR for each row because A is scaled by different row weights
            // see lm.series() in limma; calls lm.wfit.
            for (int i = 0; i < b.rows(); i++) {
                DoubleMatrix1D bw = bList.get(i);
                DoubleMatrix2D Aw = AwList.get(i);
                DoubleMatrix2D bw2D = new DenseDoubleMatrix2D(1, bw.size());
                bw2D.viewRow(0).assign(bw);
                QRDecomposition wqr = new QRDecomposition(Aw);

                // We keep all the QRs for later use.
                this.addQR(i, null, wqr);

                rawResult[i] = wqr.solve(solver.transpose(bw2D)).viewColumn(0).toArray();
                this.residualDof = bw.size() - wqr.getRank();
                assert this.residualDof >= 0;
                if (residualDof == 0) {
                    throw new IllegalArgumentException("No residual degrees of freedom to fit the model"
                            + diagnosis(wqr));
                }
            }
        }

        this.coefficients = solver.transpose(new DenseDoubleMatrix2D(rawResult));

        assert this.assign.isEmpty() || this.assign.size() == this.coefficients.rows() : assign.size()
                + " != # coefficients " + this.coefficients.rows();
        assert this.coefficients.rows() == A.columns();

        this.fitted = solver.transpose(MatrixUtil.multWithMissing(A, coefficients));

        if (this.hasMissing) {
            MatrixUtil.maskMissing(b, fitted);
        }

        this.residuals = b.copy().assign(fitted, Functions.minus);
    }

}
