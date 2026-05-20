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
package ubic.gemma.core.util.math;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.jet.stat.Descriptive;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.exception.OutOfRangeException;

/**
 * Methods for moving averages, loess
 *
 * @author paul
 * @author ptan
 */
public class Smooth {

    /**
     * Simple moving average that sums the points "backwards".
     *
     * @param m
     * @param windowSize
     * @return
     */
    public static DoubleMatrix1D movingAverage(DoubleMatrix1D m, int windowSize) {

        Queue<Double> window = new LinkedList<>();

        double sum = 0.0;

        assert windowSize > 0;

        DoubleMatrix1D result = m.like();
        for (int i = 0; i < m.size(); i++) {

            double num = m.get(i);
            sum += num;
            window.add(num);
            if (window.size() > windowSize) {

                sum -= window.remove();
            }

            if (!window.isEmpty()) {
                // if ( window.size() == windowSize ) {
                result.set(i, sum / window.size());
            } else {
                result.set(i, Double.NaN);
            }
        }

        return result;

    }

    /**
     * Default loess span (This is the default value used by limma-voom)
     */
    static final double BANDWIDTH = 0.5;

    /**
     * Default number of loess robustness iterations; 0 is probably fine.
     */
    static final int ROBUSTNESS_ITERS = 3;

    /**
     * @param xy
     * @return loessFit with default bandwitdh
     */
    public static DoubleMatrix2D loessFit(DoubleMatrix2D xy) {
        return loessFit(xy, BANDWIDTH);
    }

    /**
     * Computes a loess regression line to fit the data
     *
     * @param xy        data to be fit
     * @param bandwidth the span of the smoother (from 2/n to 1 where n is the number of points in xy)
     * @return loessFit (same dimensions as xy) or null if there are less than 3 data points
     */
    public static DoubleMatrix2D loessFit(DoubleMatrix2D xy, double bandwidth) {
        assert xy != null;

        DoubleMatrix1D sx = xy.viewColumn(0);
        DoubleMatrix1D sy = xy.viewColumn(1);
        Map<Double, Double> map = new TreeMap<>();// to enforce monotonicity
        for (int i = 0; i < sx.size(); i++) {
            if (Double.isNaN(sx.get(i)) || Double.isInfinite(sx.get(i)) || Double.isNaN(sy.get(i))
                    || Double.isInfinite(sy.get(i))) {
                continue;
            }
            map.put(sx.get(i), sy.get(i));
        }
        DoubleMatrix2D xyChecked = new DenseDoubleMatrix2D(map.size(), 2);
        xyChecked.viewColumn(0).assign(ArrayUtils.toPrimitive(map.keySet().toArray(new Double[0])));
        xyChecked.viewColumn(1).assign(ArrayUtils.toPrimitive(map.values().toArray(new Double[0])));

        // in R:
        // loess(c(1:5),c(1:5)^2,f=0.5,iter=3)
        // Note: we start to lose some precision here in comparison with R's loess FIXME why? does it matter?
        DoubleMatrix2D loessFit = new DenseDoubleMatrix2D(xyChecked.rows(), xyChecked.columns());

        // fit a loess curve
        LoessInterpolator loessInterpolator = new LoessInterpolator(bandwidth,
                ROBUSTNESS_ITERS);

        double[] loessY = loessInterpolator.smooth(xyChecked.viewColumn(0).toArray(),
                xyChecked.viewColumn(1).toArray());

        loessFit.viewColumn(0).assign(xyChecked.viewColumn(0));
        loessFit.viewColumn(1).assign(loessY);

        return loessFit;
    }



    /**
     * Linearlly interpolate values from a given data set
     *
     * Similar implementation of R's stats.approxfun(..., rule = 2) where values outside the interval ['min(x)',
     * 'max(x)'] get the value at the closest data extreme. Also performs sorting based on xTrain.
     *
     * @param x the training set of x values
     * @param y the training set of y values
     * @param xInterpolate the set of x values to interpolate
     * @return yInterpolate the interpolated set of y values
     */
    public static double[] interpolate( double[] x, double[] y, double[] xInterpolate ) {

        assert x != null;
        assert y != null;
        assert xInterpolate != null;
        assert x.length == y.length;

        double[] yInterpolate = new double[xInterpolate.length];
        LinearInterpolator linearInterpolator = new LinearInterpolator();

        // make sure that x is strictly increasing
        DoubleMatrix2D matrix = new DenseDoubleMatrix2D( x.length, 2 );
        matrix.viewColumn( 0 ).assign( x );
        matrix.viewColumn( 1 ).assign( y );
        matrix = matrix.viewSorted( 0 );
        double[] sortedX = matrix.viewColumn( 0 ).toArray();
        double[] sortedY = matrix.viewColumn( 1 ).toArray();

        // make sure x is within the domain
        DoubleArrayList xList = new DoubleArrayList( sortedX );
        double x3ListMin = Descriptive.min( xList );
        double x3ListMax = Descriptive.max( xList );
        PolynomialSplineFunction fun = linearInterpolator.interpolate( sortedX, sortedY );
        for ( int i = 0; i < xInterpolate.length; i++ ) {
            try {
                // approx(...,rule=2)
                if ( xInterpolate[i] > x3ListMax ) {
                    yInterpolate[i] = fun.value( x3ListMax );
                } else if ( xInterpolate[i] < x3ListMin ) {
                    yInterpolate[i] = fun.value( x3ListMin );
                } else {
                    yInterpolate[i] = fun.value( xInterpolate[i] );
                }
            } catch ( OutOfRangeException e ) {
                // this shouldn't happen anymore
                yInterpolate[i] = Double.NaN;
            }
        }

        return yInterpolate;
    }



}
