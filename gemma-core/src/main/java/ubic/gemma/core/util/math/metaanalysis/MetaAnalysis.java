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
package ubic.gemma.core.util.math.metaanalysis;

import ubic.gemma.core.util.math.Constants;
import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;
import cern.jet.stat.Probability;

/**
 * Statistics for meta-analysis. Methods from Cooper and Hedges (CH); Hunter and Schmidt (HS).
 * <p>
 * In this class "conditional variance" means the variance for one data set. Unconditional means "between data set", or
 * 
 * @author Paul Pavlidis
 * 
 */
public abstract class MetaAnalysis {

    /**
     * Fisher's method for combining p values. (Cooper and Hedges 15-8)
     * 
     * @param pvals DoubleArrayList
     * @return double upper tail
     */
    public static double fisherCombinePvalues( DoubleArrayList pvals ) {
        double r = 0.0;
        for ( int i = 0, n = pvals.size(); i < n; i++ ) {
            r += Math.log( pvals.getQuick( i ) );
        }
        r *= -2.0;
        // NOTE: dof is first argument.
        return Probability.chiSquareComplemented( 2.0 * pvals.size(), r );
    }

    /**
     * Test for statistical significance of Q.
     * 
     * @param Q - computed using qStatistic
     * @param N - number of studies.
     * @see qStatistic
     * @return The upper tail chi-square probability for Q with N - degrees of freedom.
     */
    public double qTest( double Q, double N ) {
        // NOTE: dof is first argument.
        return Probability.chiSquareComplemented( N - 1, Q );
    }

    /**
     * Fisher's method for combining p values (Cooper and Hedges 15-8)
     * <p>
     * Use for p values that have already been log transformed.
     * 
     * @param pvals DoubleArrayList
     * @return double upper tail
     */
    protected double fisherCombineLogPvalues( DoubleArrayList pvals ) {
        double r = 0.0;
        for ( int i = 0, n = pvals.size(); i < n; i++ ) {
            r += pvals.getQuick( i );
        }
        r *= -2.0;
        // NOTE: dof is first argument.
        return Probability.chiSquareComplemented( 2.0 * pvals.size(), r );
    }

    /**
     * Weights under a fixed effects model. Simply w_i = 1/v_i. CH eqn 18-2.
     * 
     * @param variances
     * @return
     */
    protected DoubleArrayList metaFEWeights( DoubleArrayList variances ) {
        DoubleArrayList w = new DoubleArrayList( variances.size() );

        for ( int i = 0; i < variances.size(); i++ ) {
            double v = variances.getQuick( i );
            if ( v <= Constants.SMALL ) {
                v = Constants.SMALL;
                System.err.println( "Tiny variance    " + v );
            }
            w.add( 1 / v );
        }

        return w;
    }

    /**
     * CH sample variance under random effects model, equation 18-20
     * 
     * @param
     * @return
     */
    protected double metaRESampleVariance( DoubleArrayList effectSizes ) {
        return Descriptive.sampleVariance( effectSizes, Descriptive.mean( effectSizes ) );
    }

    /**
     * CH estimate of between-studies variance, equation 18-22, for random effects model.
     * 
     * @param effectSizes
     * @param variances
     * @return
     */
    // protected double metaREVariance( DoubleArrayList effectSizes,
    // DoubleArrayList variances ) {
    // return Math.max( metaRESampleVariance( effectSizes )
    // - Descriptive.mean( variances ), 0.0 );
    // }
    /**
     * CH equation 18-23. Another estimator of the between-studies variance s<sup>2 </sup> for random effects model.
     * This is non-zero only if Q is larger than expected under the null hypothesis that the variance is zero.
     * 
     * <pre>
     *      s&circ;2 = [Q - ( k - 1 ) ] / c
     * </pre>
     * 
     * where
     * 
     * <pre>
     *      c = Max(sum_i=1&circ;k w_i - [ sum_i&circ;k w_i&circ;2 / sum_i&circ;k w_i ], 0)
     * </pre>
     * 
     * @param effectSizes
     * @param variances
     * @param weights
     * @return
     */
    protected double metaREVariance( DoubleArrayList effectSizes, DoubleArrayList variances, DoubleArrayList weights ) {

        if ( !( effectSizes.size() == weights.size() && weights.size() == variances.size() ) )
            throw new IllegalArgumentException( "Unequal sizes" );

        // the weighted unconditional variance.
        double q = qStatistic( effectSizes, variances, weightedMean( effectSizes, weights ) );

        double c = Descriptive.sum( weights ) - Descriptive.sumOfSquares( weights ) / Descriptive.sum( weights );
        return Math.max( ( q - ( effectSizes.size() - 1 ) ) / c, 0.0 );
    }

    /**
     * Under a random effects model, CH eqn. 18-24, we replace the conditional variance with the sum of the
     * between-sample variance and the conditional variance.
     * 
     * <pre>
     *      v_i&circ;* = sigma-hat_theta&circ;2 + v_i.
     * </pre>
     * 
     * @param variances Conditional variances
     * @param sampleVariance estimated...somehow.
     * @return
     */
    protected DoubleArrayList metaREWeights( DoubleArrayList variances, double sampleVariance ) {
        DoubleArrayList w = new DoubleArrayList( variances.size() );

        for ( int i = 0; i < variances.size(); i++ ) {
            if ( variances.getQuick( i ) <= 0 ) {
                throw new IllegalStateException( "Negative or zero variance" );
            }
            w.add( 1 / ( variances.getQuick( i ) + sampleVariance ) );
        }

        return w;
    }

    /**
     * CH 18-3. Can be used for fixed or random effects model, the variances just have to computed differently.
     * 
     * <pre>
     * 
     *            v_dot = 1/sum_i=1&circ;k ( 1/v_i)
     * </pre>
     * 
     * @param variances
     * @return
     */
    protected double metaVariance( DoubleArrayList variances ) {
        double var = 0.0;
        for ( int i = 0; i < variances.size(); i++ ) {
            var += 1.0 / variances.getQuick( i );
        }
        if ( var == 0.0 ) {
            var = Double.MIN_VALUE;
            // throw new IllegalStateException( "Variance of zero" );
        }
        return 1.0 / var;
    }

    /**
     * CH 18-3 version 2 for quality weighted. ( page 266 ) in Fixed effects model.
     * 
     * <pre>
     *            v_dot = [ sum_i=1&circ;k ( q_i &circ; 2 * w_i) ]/[ sum_i=1&circ;k  q_i * w_i ]&circ;2
     * </pre>
     * 
     * @param variances
     * @return
     */
    protected double metaVariance( DoubleArrayList weights, DoubleArrayList qualityIndices ) {
        double num = 0.0;
        double denom = 0.0;
        for ( int i = 0; i < weights.size(); i++ ) {
            num += Math.pow( weights.getQuick( i ), 2 ) * qualityIndices.getQuick( i );
            denom += Math.pow( weights.getQuick( i ) * qualityIndices.getQuick( i ), 2 );
        }
        if ( denom == 0.0 ) {
            throw new IllegalStateException( "Attempt to divide by zero." );
        }
        return num / denom;
    }

    /**
     * Test statistic for H0: effectSize == 0. CH 18-5. For fixed effects model.
     * 
     * @param metaEffectSize
     * @param metaVariance
     * @return
     */
    protected double metaZscore( double metaEffectSize, double metaVariance ) {
        return Math.abs( metaEffectSize ) / Math.sqrt( metaVariance );
    }

    /**
     * The "Q" statistic used to test homogeneity of effect sizes. (Cooper and Hedges 18-6)
     * 
     * @param effectSizes DoubleArrayList
     * @param variances DoubleArrayList
     * @param globalMean double
     * @return double
     */
    protected double qStatistic( DoubleArrayList effectSizes, DoubleArrayList variances, double globalMean ) {

        if ( !( effectSizes.size() == variances.size() ) ) throw new IllegalArgumentException( "Unequal sizes" );

        double r = 0.0;
        for ( int i = 0, n = effectSizes.size(); i < n; i++ ) {
            r += Math.pow( effectSizes.getQuick( i ) - globalMean, 2.0 ) / variances.getQuick( i );
        }
        return r;
    }

    /**
     * General formula for weighted mean of effect sizes. Cooper and Hedges 18-1, or HS pg. 100.
     * <p>
     * In HS, the weights are simply the sample sizes. For CH, the weights are 1/v for a fixed effect model. Under a
     * random effects model, we would use 1/(v + v_bs) where v_bs is the between-studies variance.
     * 
     * @param effectSizes
     * @param sampleSizes
     * @return
     */
    protected double weightedMean( DoubleArrayList effectSizes, DoubleArrayList weights ) {

        if ( !( effectSizes.size() == weights.size() ) ) throw new IllegalArgumentException( "Unequal sizes" );

        double wm = 0.0;
        for ( int i = 0; i < effectSizes.size(); i++ ) {
            wm += weights.getQuick( i ) * effectSizes.getQuick( i );
        }

        double s = Descriptive.sum( weights );

        if ( s == 0.0 ) return 0.0;
        return wm /= s;
    }

    /**
     * General formula for weighted mean of effect sizes including quality index scores for each value. Cooper and
     * Hedges 18-1, or HS pg. 100.
     * 
     * @param effectSizes
     * @param sampleSizes
     * @param qualityIndices
     * @return
     */
    protected double weightedMean( DoubleArrayList effectSizes, DoubleArrayList weights, DoubleArrayList qualityIndices ) {

        if ( !( effectSizes.size() == weights.size() && weights.size() == qualityIndices.size() ) )
            throw new IllegalArgumentException( "Unequal sizes" );

        double wm = 0.0;
        for ( int i = 0; i < effectSizes.size(); i++ ) {
            wm += weights.getQuick( i ) * effectSizes.getQuick( i ) * qualityIndices.getQuick( i );
        }
        return wm /= Descriptive.sum( weights ) * Descriptive.sum( qualityIndices );
    }
}