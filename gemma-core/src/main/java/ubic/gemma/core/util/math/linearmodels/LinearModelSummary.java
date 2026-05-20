package ubic.gemma.core.util.math.linearmodels;

import ubic.gemma.core.util.matrix.DoubleMatrix;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface LinearModelSummary extends Serializable {

    String INTERCEPT_COEFFICIENT_NAME = "(Intercept)";

    /**
     * @return may be null if ANOVA was not run.
     */
    @Nullable
    GenericAnovaResult getAnova();

    double[] getCoefficients();

    /**
     * @return The contrast coefficients and associated statistics for all tested contrasts.
     *         <p>
     *         Row names are the contrasts, for example for a model with one
     *         factor "f" with two
     *         levels "a" and "b": {"(Intercept)", "fb"}. columns are always {"Estimate" ,"Std. Error", "t value",
     *         "Pr(>|t|)"}
     *
     */
    DoubleMatrix<String, String> getContrastCoefficients();

    Map<String, Double> getContrastCoefficients( String factorName );

    /**
     * For the requested factor, return the standard errors associated with the contrast coefficient estimates.
     */
    Map<String, Double> getContrastCoefficientStderr( String factorName );

    /**
     * @return Map of pvalues for the given factor. For continuous factors or factors with only one level, there will be
     *         just one value. For factors with N>2 levels, there will be N-1 values, one for each contrast
     *         (since we compute treatment contrasts to the baseline)
     */
    Map<String, Double> getContrastPValues( String factorName );

    /**
     * @return Map of T statistics for the given factor. For continuous factors or factors with only one level, there
     *         will be just one value. For factors with N>2 levels, there will be N-1 values, one for each contrast
     *         (since we compute treatment contrasts to the baseline)
     */
    Map<String, Double> getContrastTStats( String factorName );

    double[] getEffects();

    /**
     * @return F statistic for overall model fit.
     */
    double getFStat();

    /**
     * @return the factorNames
     */
    List<String> getFactorNames();

    double getInterceptCoefficient();

    double getInterceptPValue();

    double getInterceptTStat();

    String getKey();

    double getNumeratorDof();

    /**
     * Overall p value for F stat of model fit (upper tail probability)
     *
     * @return value or NaN if it can't be computed for some reason
     */
    double getOverallPValue();

    /**
     * @return the priorDof
     */
    double getPriorDof();

    double getResidualsDof();

    /**
     * @return the residuals
     */
    double[] getResiduals();

    /**
     * @return the rSquared
     */
    double getRSquared();

    /**
     * @return the adjRSquared
     */
    double getAdjRSquared();

    /**
     * Residual standard deviation
     */
    double getSigma();

    /**
     * Unscaled standard deviations for the coefficient estimators in same order as coefficients. The standard errors
     * are given by stdev.unscaled * sigma (a la limma)
     */
    double[] getStdevUnscaled();

    boolean isBaseline( String factorValueName );

    /**
     * Whether this is the result of emprical bayes shrinkage of variance estimates
     */
    boolean isShrunken();
}
