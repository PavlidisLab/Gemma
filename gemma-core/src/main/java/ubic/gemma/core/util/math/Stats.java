/*
 * The baseCode project
 *
 * Copyright (c) 2006-2021 University of British Columbia
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

import java.util.HashSet;
import java.util.Set;

import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;

/**
 * Miscellaneous functions used for statistical analysis. Some are optimized or specialized versions of methods that can
 * be found elsewhere.
 *
 * @author Paul Pavlidis
 * @see <a href="http://hoschek.home.cern.ch/hoschek/colt/V1.0.3/doc/cern/jet/math/package-summary.html">cern.jet.math
 * </a>
 * @see <a href="http://hoschek.home.cern.ch/hoschek/colt/V1.0.3/doc/cern/jet/stat/package-summary.html">cern.jet.stat
 * </a>
 */
public class Stats {


    /**
     * Convert an array into a cumulative density function (CDF). This assumes that the input contains counts
     * representing the distribution in question.
     *
     * @param x The input of counts (i.e. a histogram).
     * @return DoubleArrayList the CDF.
     */
    public static DoubleArrayList cdf(DoubleArrayList x) {
        return cumulateRight(normalize(x));
    }

    /**
     * Convert an array into a cumulative array. Summing is from the left hand side. Use this to make CDFs where the
     * concern is the left tail.
     *
     * @param x DoubleArrayList
     * @return cern.colt.list.DoubleArrayList
     */
    public static DoubleArrayList cumulate(DoubleArrayList x) {
        if (x.size() == 0) {
            return new DoubleArrayList(0);
        }

        DoubleArrayList r = new DoubleArrayList();

        double sum = 0.0;
        for (int i = 0; i < x.size(); i++) {
            sum += x.get(i);
            r.add(sum);
        }
        return r;
    }

    /**
     * Convert an array into a cumulative array. Summing is from the right hand side. This is useful for creating
     * upper-tail cumulative density histograms from count histograms, where the upper tail is expected to have very
     * small numbers that could be lost to rounding.
     *
     * @param x the array of data to be cumulated.
     * @return cern.colt.list.DoubleArrayList
     */
    public static DoubleArrayList cumulateRight(DoubleArrayList x) {
        if (x.size() == 0) {
            return new DoubleArrayList(0);
        }

        DoubleArrayList r = new DoubleArrayList(new double[x.size()]);

        double sum = 0.0;
        for (int i = x.size() - 1; i >= 0; i--) {
            sum += x.get(i);
            r.set(i, sum);
        }
        return r;
    }

    /**
     * Compute the coefficient of variation of an array (standard deviation / mean). If the variance is zero, this
     * returns zero. If the mean is zero, NaN is returned. If the mean is negative, the CV is computed relative to the
     * absolute value of the mean; that is, negative values are treated as magnitudes.
     *
     * @param data DoubleArrayList
     * @return the cv
     * @todo offer a regularized version of this function.
     */
    public static double cv(DoubleArrayList data) {
        double mean = DescriptiveWithMissing.mean(data);

        double sampleVariance = DescriptiveWithMissing.sampleVariance(data, mean);

        if (sampleVariance == 0.0) return 0.0;

        if (mean == 0.0) {
            return 0.0;
        }

        return Math.sqrt(sampleVariance) / Math.abs(mean);
    }

    /**
     * Test whether a value is a valid fractional or probability value.
     *
     * @param value
     * @return true if the value is in the interval 0 to 1.
     */
    public static boolean isValidFraction(double value) {
        if (value > 1.0 || value < 0.0) {
            return false;
        }
        return true;
    }

    /**
     * calculate the mean of the values above (NOT greater or equal to) a particular index rank of an array. Quantile
     * must be a value from 0 to 100.
     *
     * @param index         the rank of the value we wish to average above.
     * @param array         Array for which we want to get the quantile.
     * @param effectiveSize The size of the array, not including NaNs.
     * @return double
     * @see DescriptiveWithMissing#meanAboveQuantile
     */
    public static double meanAboveQuantile(int index, double[] array, int effectiveSize) {

        double[] temp = new double[effectiveSize];
        double median;
        double returnvalue = 0.0;
        int k = 0;

        temp = array;
        median = quantile(index, array, effectiveSize);

        for (int i = 0; i < effectiveSize; i++) {
            if (temp[i] > median) {
                returnvalue += temp[i];
                k++;
            }
        }
        return returnvalue / k;
    }

    /**
     * Adjust the elements of an array so they total to 1.0.
     *
     * @param x Input array.
     * @return Normalized array.
     */
    public static DoubleArrayList normalize(DoubleArrayList x) {
        return normalize(x, Descriptive.sum(x));
    }

    /**
     * Divide the elements of an array by a given factor.
     *
     * @param x          Input array.
     * @param normfactor double
     * @return Normalized array.
     */
    public static DoubleArrayList normalize(DoubleArrayList x, double normfactor) {
        if (x.size() == 0) {
            return new DoubleArrayList(0);
        }

        DoubleArrayList r = new DoubleArrayList();

        for (int i = 0; i < x.size(); i++) {
            r.add(x.get(i) / normfactor);
        }
        return r;

    }

    /**
     * @param array input data
     * @param tolerance a small constant
     * @return number of distinct values in the array, within tolerance. Double.NaN is counted as a distinct
     * value.
     */
    public static Integer numberofDistinctValues(DoubleArrayList array, double tolerance) {

        Set<Double> distinct = new HashSet<>();
        int r = 1;
        if (tolerance > 0.0) {
            r = (int) Math.ceil(1.0 / tolerance);
        }
        for (int i = 0; i < array.size(); i++) {
            double v = array.get(i);
            if (tolerance > 0) {
                // this might not be foolproof
                distinct.add((double) Math.round(v * r) / r);
            } else {
                distinct.add(v);
            }
        }
        return Math.max(0, distinct.size());

    }


    /**
     * @param tolerance a small constant
     * @return number of distinct values in the array, within tolerance. Double.NaN is ignored entirely
     */
    public static Integer numberofDistinctValuesNonNA(DoubleArrayList array, double tolerance) {

        Set<Double> distinct = new HashSet<>();
        int r = 1;
        if (tolerance > 0.0) {
            r = (int) Math.ceil(1.0 / tolerance);
        }
        for (int i = 0; i < array.size(); i++) {
            double v = array.get(i);
            if (Double.isNaN(v)) {
                continue;
            }
            if (tolerance > 0) {
                // this might not be foolproof
                distinct.add((double) Math.round(v * r) / r);
            } else {
                distinct.add(v);
            }
        }
        return Math.max(0, distinct.size());

    }

    /**
     * Compute the fraction of values which are distinct. NaNs are ignored entirely. If the data are all NaN, 0.0 is returned.
     *
     * @param array input data
     * @param tolerance a small constant to define the difference that is "distinct"
     * @return
     */
    public static Double fractionDistinctValuesNonNA(DoubleArrayList array, double tolerance) {
        double numNonNA = (double) numNonMissing(array);
        if (numNonNA == 0) return 0.0;
        return (double) numberofDistinctValuesNonNA(array, tolerance) / numNonNA;
    }

    private static Integer numNonMissing(DoubleArrayList array) {
        int nm = 0;
        for (int i = 0; i < array.size(); i++) {
            if (Double.isNaN(array.get(i))) continue;
            nm++;
        }
        return nm;
    }


    /**
     * Given a double array, calculate the quantile requested. Note that no interpolation is done and missing values are ignored.
     *
     * @param index         - the rank of the value we wish to get. Thus if we have 200 items in the array, and want the median,
     *                      we should enter 100.
     * @param values        double[] - array of data we want quantile of
     * @param effectiveSize int the effective size of the array
     * @return double the value at the requested quantile
     * @see DescriptiveWithMissing#quantile
     */
    public static double quantile(int index, double[] values, int effectiveSize) {
        double pivot = -1.0;
        if (index == 0) {
            double ans = values[0];
            for (int i = 1; i < effectiveSize; i++) {
                if (ans > values[i]) {
                    ans = values[i];
                }
            }
            return ans;
        }

        double[] temp = new double[effectiveSize];

        for (int i = 0; i < effectiveSize; i++) {
            temp[i] = values[i];
        }

        pivot = temp[0];

        double[] smaller = new double[effectiveSize];
        double[] bigger = new double[effectiveSize];
        int itrSm = 0;
        int itrBg = 0;
        for (int i = 1; i < effectiveSize; i++) {
            if (temp[i] <= pivot) {
                smaller[itrSm] = temp[i];
                itrSm++;
            } else if (temp[i] > pivot) {
                bigger[itrBg] = temp[i];
                itrBg++;
            }
        }
        if (itrSm > index) { // quantile must be in the 'smaller' array
            return quantile(index, smaller, itrSm);
        } else if (itrSm < index) { // quantile is in the 'bigger' array
            return quantile(index - itrSm - 1, bigger, itrBg);
        } else {
            return pivot;
        }

    }

    /**
     * Compute the range of an array. Missing values are ignored.
     *
     * @param data DoubleArrayList
     * @return double
     */
    public static double range(DoubleArrayList data) {
        return DescriptiveWithMissing.max(data) - DescriptiveWithMissing.min(data);
    }

    private Stats() { /* block instantiation */
    }

}