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

/**
 * Alternative distance and similarity metrics for vectors.
 * 
 * @author Paul Pavlidis
 * 
 */
public class Distance {

    /**
     * Highly optimized implementation of the Pearson correlation. The inputs must be standardized - mean zero, variance
     * one, without any missing values.
     * 
     * @param xe A standardized vector
     * @param ye A standardized vector
     * @return Pearson correlation coefficient.
     */
    public static double correlationOfStandardized( double[] xe, double[] ye ) {
        double sxy = 0.0;
        for ( int i = 0, n = xe.length; i < n; i++ ) {
            double xj = xe[i];
            double yj = ye[i];
            sxy += xj * yj;
        }

        return sxy / ( xe.length - 1 );
    }

    /**
     * Like correlationofNormedFast, but takes DoubleArrayLists as inputs, handles missing values correctly, and does
     * more error checking. Assumes the data has been converted to z scores already.
     * 
     * @param x A standardized vector
     * @param y A standardized vector
     * @return The Pearson correlation between x and y.
     */
    public static double correlationOfStandardized( DoubleArrayList x, DoubleArrayList y ) {

        if ( x.size() != y.size() ) {
            throw new IllegalArgumentException( "Array lengths must be the same" );
        }

        double[] xe = x.elements();
        double[] ye = y.elements();
        double sxy = 0.0;
        int length = 0;
        for ( int i = 0, n = x.size(); i < n; i++ ) {
            double xj = xe[i];
            double yj = ye[i];

            if ( Double.isNaN( xj ) || Double.isNaN( yj ) ) {
                continue;
            }

            sxy += xj * yj;
            length++;
        }

        if ( length == 0 ) {
            return -2.0; // flag of illegal value.
        }
        return sxy / ( length - 1 );
    }

    /**
     * Calculate the Euclidean distance between two vectors.
     * 
     * @param x DoubleArrayList
     * @param y DoubleArrayList
     * @return Euclidean distance between x and y
     */
    public static double euclDistance( DoubleArrayList x, DoubleArrayList y ) {
        int j;
        double sum;
        sum = 0.0;
        if ( x.size() != y.size() ) {
            throw new ArithmeticException();
        }

        int length = x.size();

        for ( j = 0; j < length; j++ ) {
            if ( !Double.isNaN( x.elements()[j] ) && !Double.isNaN( y.elements()[j] ) ) {
                sum += Math.pow( ( x.elements()[j] - y.elements()[j] ), 2 );
            }
        }
        if ( sum == 0.0 ) {
            return 0.0;
        }
        return Math.sqrt( sum );
    }

    /**
     * Calculate the Manhattan distance between two vectors.
     * 
     * @param x DoubleArrayList
     * @param y DoubleArrayList
     * @return Manhattan distance between x and y
     */
    public static double manhattanDistance( DoubleArrayList x, DoubleArrayList y ) {
        int j;
        double sum = 0.0;

        if ( x.size() != y.size() ) {
            throw new ArithmeticException();
        }

        int length = x.size();
        for ( j = 0; j < length; j++ ) {
            if ( !Double.isNaN( x.elements()[j] ) && !Double.isNaN( y.elements()[j] ) ) {
                sum += Math.abs( x.elements()[j] - y.elements()[j] );
            }
        }
        return sum;
    }

    /**
     * Convenience function to compute the rank correlation when we just want to know if the values are "in order".
     * Values in perfect ascending order are a correlation of 1, descending is -1.
     * 
     * @param x
     * @return
     */
    public static double spearmanRankCorrelation( DoubleArrayList x ) {
        DoubleArrayList ranks = Rank.rankTransform( x );

        double s = 0.0;
        int n = ranks.size();
        for ( int i = 0; i < n; i++ ) {
            double r = ranks.get( i ); // 1 is the minimum.
            double d = Math.pow( r - ( i + 1 ), 2 );
            s += d;
        }

        return 1.0 - ( 6.0 * s ) / ( n * ( Math.pow( n, 2 ) - 1 ) );
    }

    //
    // public static double mutualInformation(DoubleMatrix1D x, DoubleMatrix1D y) {
    // double h = 0.1; // toto: estimate this. Tricky.
    //
    //
    //
    // }

    /**
     * Spearman Rank Correlation. This does the rank transformation of the data. Only mutually non-NaN values are used.
     * 
     * @param x DoubleArrayList
     * @param y DoubleArrayList
     * @return Spearman's rank correlation between x and y or NaN if it could not be computed.
     */
    public static double spearmanRankCorrelation( DoubleArrayList x, DoubleArrayList y ) {

        if ( x.size() != y.size() ) {
            throw new IllegalArgumentException( "x and y must have equal sizes." );
        }

        DoubleArrayList mx = new DoubleArrayList();
        DoubleArrayList my = new DoubleArrayList();

        for ( int i = 0; i < x.size(); i++ ) {
            if ( Double.isNaN( x.get( i ) ) || Double.isNaN( y.get( i ) ) ) {
                continue;
            }
            mx.add( x.get( i ) );
            my.add( y.get( i ) );
        }

        assert mx.size() == my.size();
        if ( mx.size() < 2 ) return Double.NaN;

        DoubleArrayList rx = Rank.rankTransform( mx );
        DoubleArrayList ry = Rank.rankTransform( my );

        return DescriptiveWithMissing.correlation( rx, ry );
    }
}