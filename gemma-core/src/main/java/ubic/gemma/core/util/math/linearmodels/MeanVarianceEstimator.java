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

import static cern.jet.math.Functions.chain;
import static cern.jet.math.Functions.div;
import static cern.jet.math.Functions.log2;
import static cern.jet.math.Functions.minus;
import static cern.jet.math.Functions.mult;
import static cern.jet.math.Functions.plus;
import static cern.jet.math.Functions.sqrt;

import java.util.List;

import cern.colt.function.IntIntDoubleFunction;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.core.util.math.DescriptiveWithMissing;
import ubic.gemma.core.util.math.MatrixRowStats;
import ubic.gemma.core.util.math.Smooth;
import ubic.gemma.core.util.math.linalg.QRDecomposition;

/**
 * Estimate mean-variance relationship and use this to compute weights for least squares fitting. R's limma.voom()
 * Charity Law and Gordon Smyth. See Law et al.
 * {@see http://genomebiology.biomedcentral.com/articles/10.1186/gb-2014-15-2-r29}
 * <p>
 * Running voom() on data matrices with NaNs is not currently supported.
 *
 * @author ptan
 */
public class MeanVarianceEstimator {

    /**
     * Normalized variables on log2 scale
     */
    private final DoubleMatrix2D E;

    /**
     * Size of each library (column).
     */
    private DoubleMatrix1D librarySize;

    /**
     * Loess fit (x, y)
     */
    private DoubleMatrix2D loess;

    /**
     * Matrix that contains the mean and variance of the data. Matrix is sorted by increasing mean. Useful for plotting.
     * mean <- fit$Amean + mean(log2(lib.size+1)) - log2(1e6) variance <- sqrt(fit$sigma)
     */
    private DoubleMatrix2D meanVariance;

    /**
     * inverse variance weights
     */
    private DoubleMatrix2D weights = null;

    /**
     * Preferred interface if you want control over how the design is set up. Executes voom() to calculate weights.
     *
     * @param designMatrix
     * @param data         expected to be log2cpm, and already filtered
     * @param librarySize  library size (matrix column sum)
     */
    public MeanVarianceEstimator(DesignMatrix designMatrix, DoubleMatrix<String, String> data,
                                 DoubleMatrix1D librarySize) {

        DoubleMatrix2D b = new DenseDoubleMatrix2D(data.asArray());
        this.librarySize = librarySize;
        this.E = b;
        voom(designMatrix.getDoubleMatrix());
    }

    /**
     * Executes voom() to calculate weights.
     *
     * @param designMatrix
     * @param data         a normalized count matrix
     * @param librarySize  library size (matrix column sum)
     */
    public MeanVarianceEstimator(DesignMatrix designMatrix, DoubleMatrix2D data, DoubleMatrix1D librarySize) {

        this.librarySize = librarySize;
        this.E = data;
        voom(designMatrix.getDoubleMatrix());
    }

    /**
     * Generic method for calculating mean and variance, as a data diagnostic.
     * <p>
     * voom() is not executed and therefore no weights
     * are calculated.
     *
     * @param data data to be processed
     */
    public MeanVarianceEstimator(DoubleMatrix2D data) {
        this.E = data;
        mv();
        this.loess = Smooth.loessFit(this.meanVariance);
    }

    /**
     * @return total library size
     */
    public DoubleMatrix1D getLibrarySize() {
        return this.librarySize;
    }

    /**
     * @return the loess fit of the mean-variance relationship
     */
    public DoubleMatrix2D getLoess() {
        return this.loess;
    }

    /**
     * @return the mean and variance of the normalized data, columns 0 and 1 respectively
     */
    public DoubleMatrix2D getMeanVariance() {
        return this.meanVariance;
    }

    /**
     * @return data, as supplied (should be log2cpm)
     */
    public DoubleMatrix2D getNormalizedValue() {
        return this.E;
    }

    /**
     * @return inverse variance weights if voom was applied
     */
    public DoubleMatrix2D getWeights() {
        return this.weights;
    }


    /**
     * Compute row-wise mean (x) and variance (y) on the given data. Nothing is regressed out and no loess is computed.
     */
    private void mv() {
        assert this.E != null;

        // mean-variance
        DoubleMatrix1D Amean = new DenseDoubleMatrix1D(E.rows());
        DoubleMatrix1D variance = Amean.like();
        for (int i = 0; i < Amean.size(); i++) {
            DoubleArrayList row = new DoubleArrayList(E.viewRow(i).toArray());
            double rowMean = DescriptiveWithMissing.mean(row);
            double rowVar = DescriptiveWithMissing.variance(row);
            Amean.set(i, rowMean);
            variance.set(i, rowVar);

        }
        this.meanVariance = new DenseDoubleMatrix2D(E.rows(), 2);
        this.meanVariance.viewColumn(0).assign(Amean);
        this.meanVariance.viewColumn(1).assign(variance);
    }

    /**
     * Performs the heavy duty work of calculating the weights. See Law et al.
     * {@see http://genomebiology.biomedcentral.com/articles/10.1186/gb-2014-15-2-r29}. Assumes E is already log2cpm.
     *
     * @param designMatrix of factors that will be regressed out for the purpose of getting the mv relation
     * @throws IllegalArgumentException if there are missing values.
     */
    private void voom(DoubleMatrix2D designMatrix) {
        assert designMatrix != null;
        assert this.E != null;
        assert this.librarySize != null;

        Algebra solver = new Algebra();

        DoubleMatrix2D A = designMatrix;
        weights = new DenseDoubleMatrix2D(E.rows(), E.columns());

        // perform a linear fit to obtain the mean-variance relationship
        // fit3<-lm(t(yCpm) ~ as.matrix(design.matrix[,2]))
        // or gFit <- lmFit(yCpm, design=design.matrix)
        // as per voom, "Fit linear model to log2-counts-per-million"
        LeastSquaresFit lsf = new LeastSquaresFit(A, E);


        // calculate fit$Amean by doing rowSums(CPM) (see limma.getEAWP())
        DoubleMatrix1D Amean = MatrixRowStats.means(E);

        // "Fit lowess trend to sqrt-standard-deviations by log-count-size" (so we have to convert back)
        // sx <- fit$Amean + mean(log2(lib.size+1)) - log2(1e6)
        DoubleMatrix1D sx = Amean.copy();
        double meanLog2LibrarySize = librarySize.copy().assign(chain(log2, plus(1))).zSum() / librarySize.size();
        sx.assign(plus(meanLog2LibrarySize));
        sx.assign(minus(Math.log(Math.pow(10, 6)) / Math.log(2)));

        DoubleMatrix1D sy = quarterRootVariance(lsf);

        // only accepts array in strictly increasing order (drop duplicates)
        // so combine sx and sy and sort
        DoubleMatrix2D voomXY = new DenseDoubleMatrix2D(sx.size(), 2);
        voomXY.viewColumn(0).assign(sx);
        voomXY.viewColumn(1).assign(sy);
        DoubleMatrix2D fit = Smooth.loessFit(voomXY);
        this.meanVariance = voomXY;
        this.loess = fit;

        // quarterroot fitted counts
        DoubleMatrix2D fittedValues = null;
        QRDecomposition qr = new QRDecomposition(A);
        DoubleMatrix2D coeff = lsf.getCoefficients();

        if (qr.getRank() < A.columns()) {
            // j <- fit$pivot[1:fit$rank]
            // fitted.values <- fit$coef[,j,drop=F] %*% t(fit$design[,j,drop=F]);
            IntArrayList pivot = qr.getPivotOrder();

            IntArrayList subindices = (IntArrayList) pivot.partFromTo(0, qr.getRank() - 1);
            int[] coeffAllCols = new int[coeff.columns()];
            int[] desAllRows = new int[A.rows()];
            for (int i = 0; i < coeffAllCols.length; i++) {
                coeffAllCols[i] = i;
            }
            for (int i = 0; i < desAllRows.length; i++) {
                desAllRows[i] = i;
            }
            DoubleMatrix2D coeffSlice = coeff.viewSelection(subindices.elements(), coeffAllCols);
            DoubleMatrix2D ASlice = A.viewSelection(desAllRows, subindices.elements());
            fittedValues = solver.mult(coeffSlice.viewDice(), ASlice.viewDice());
        } else {
            // fitted.values <- fit$coef %*% t(fit$design)
            fittedValues = solver.mult(coeff.viewDice(), A.viewDice());
        }

        // back-compute the values we want
        // fitted.cpm <- 2^fitted.values
        // fitted.count <- 1e-6 * t(t(fitted.cpm)*(lib.size+1))
        // fitted.logcount <- log2(fitted.count)
        DoubleMatrix2D fittedCpm = fittedValues.copy().forEachNonZero(new IntIntDoubleFunction() {
            @Override
            public double apply(int row, int column, double third) {
                return Math.pow(2, third);
            }
        });
        DoubleMatrix2D fittedCount = fittedCpm.copy();
        DoubleMatrix1D libSizePlusOne = librarySize.assign(plus(1));
        for (int i = 0; i < fittedCount.rows(); i++) {
            fittedCount.viewRow(i).assign(libSizePlusOne, mult);
            fittedCount.viewRow(i).assign(mult(Math.pow(10, -6)));
        }
        DoubleMatrix2D fittedLogCount = fittedCount.copy().assign(log2);

        // to here we are *very* close to limma::voom

        // interpolate points using the loess curve
        // f <- approxfun(l, rule=2)
        // 2D to 1D FIXME this unrolling seems kind of unnecessary
        double[] xInterpolate = new double[fittedLogCount.rows() * fittedLogCount.columns()];
        int idx = 0;
        for (int col = 0; col < fittedLogCount.columns(); col++) {
            for (int row = 0; row < fittedLogCount.rows(); row++) {
                xInterpolate[idx] = fittedLogCount.get(row, col);
                idx++;
            }
        }
        // apply trend to individual observations
        // w <- 1/f(fitted.logcount)^4
        assert fit != null;
        double[] yInterpolate = Smooth.interpolate(fit.viewColumn(0).toArray(),
                fit.viewColumn(1).toArray(), xInterpolate);

        // 1D to 2D
        idx = 0;
        for (int col = 0; col < weights.columns(); col++) {
            for (int row = 0; row < weights.rows(); row++) {
                weights.set(row, col, (1.0 / Math.pow(yInterpolate[idx], 4)));
                idx++;
            }
        }

    }

    protected DoubleMatrix1D quarterRootVariance(LeastSquaresFit lsf) {

        // help("MArrayLM-class")
        // fit$sigma <- sqrt(sum(out$residuals^2)/out$df.residual)
        // sy <- sqrt(fit$sigma)
        DoubleMatrix2D residuals = lsf.getResiduals();
        DoubleMatrix1D sy = new DenseDoubleMatrix1D(residuals.rows());
        // sum squared residuals
        for (int row = 0; row < residuals.rows(); row++) {
            double sum = 0;
            for (int column = 0; column < residuals.columns(); column++) {
                Double val = residuals.get(row, column);
                if (!Double.isNaN(val)) {
                    sum += val * val;
                }
            }
            sy.set(row, sum);
        }
        // sigma (ssq/n)
        // if you have missing values in the expression matrix
        // you'll get a residual dof of 0
        if (lsf.isHasMissing()) {
            // calculate it per row
            List<Integer> dofs = lsf.getResidualDofs();
            assert dofs.size() == sy.size();
            for (int i = 0; i < sy.size(); i++) {
                sy.set(i, Math.sqrt(sy.get(i) / dofs.get(i)));
            }
        } else {
            int dof = lsf.getResidualDof();
            assert dof != 0;
            sy.assign(chain(sqrt, div(dof)));
        }
        // we're fitting the quarter-root variances.
        sy.assign(sqrt);
        return sy;
    }
}
