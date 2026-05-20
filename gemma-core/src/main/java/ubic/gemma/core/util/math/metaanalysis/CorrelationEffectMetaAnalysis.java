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

import ubic.gemma.core.util.math.CorrelationStats;
import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;
import cern.jet.stat.Probability;

/**
 * Implementation of meta-analysis of correlations along the lines of chapter 18 of Cooper and Hedges, "Handbook of
 * Research Synthesis". Both fixed and random effects models are supported, with z-transformed or untransformed
 * correlations.
 * 
 * @author pavlidis
 * 
 */
public class CorrelationEffectMetaAnalysis extends MetaAnalysis {

    /**
     * Equation 18-10 from CH. For untransformed correlations.
     * <p>
     * 
     * <pre>
     * v_i = ( 1 - r_i &circ; 2 ) &circ; 2 / ( n_i - 1 )
     * </pre>
     * <p>
     * I added a regularization to this, so that we don't get ridiculous variances when correlations are close to 1
     * (this happens). If the correlation is very close to 1 (or -1), we fudge it to be a value less close to 1 (e.g.,
     * 0.999)
     * </p>
     * 
     * @param r
     * @param n
     * @return
     */
    protected static double samplingVariance( double r, double numsamples ) {

        if ( numsamples <= 0 ) throw new IllegalArgumentException( "N must be greater than 0" );

        if ( !CorrelationStats.isValidPearsonCorrelation( r ) )
            throw new IllegalArgumentException( "r=" + r + " is not a valid Pearson correlation" );

        double FUDGE = 0.001;
        if ( 1.0 - Math.abs( r ) < FUDGE ) {
            r = Math.abs( r ) - FUDGE; // don't care about sign. any more.
        }

        if ( numsamples < 2 ) {
            return Double.NaN;
        }
        double k = 1.0 - r * r;
        double var = k * k / ( numsamples - 1 );

        return var;
    }

    private double bsv; // between-studies variance component;

    private double e; // unconditional effect;
    private boolean fixed = true;
    private double n; // total sample size
    private double p; // probability
    private double q; // q-score;
    private boolean transform = false;
    private double v; // unconditional variance;

    private double z; // z score

    public CorrelationEffectMetaAnalysis() {
    }

    public CorrelationEffectMetaAnalysis( boolean fixed, boolean transform ) {
        this.fixed = fixed;
        this.transform = transform;
    }

    public double getBsv() {
        return bsv;
    }

    public double getE() {
        return e;
    }

    public double getN() {
        return n;
    }

    public double getP() {
        return p;
    }

    public double getQ() {
        return q;
    }

    public double getV() {
        return v;
    }

    public double getZ() {
        return z;
    }

    /**
     * Following CH section 2.2.
     * <p>
     * There are four possible cases (for now):
     * <ol>
     * <li>Fixed effects, Z-transformed. Weights are computed using CH eqns 18-8 and 18-3
     * <li>Fixed effects, untransformed. Weights are computed using CH eqns 18-10 and 18-3
     * <li>Random effects, Z-transformed. Weights are computed using CH eqns 18-20, 18-8 with 18-3
     * <li>Random effects, untransformed. Weights are computed using CH eqns 18-10, 18-20, 18-24.
     * </ol>
     * The default is untransformed, fixed effects.
     * 
     * @param correlations - NOT fisher transformed. This routine takes care of that.
     * @param sampleSizes
     * @return p-value. The p-value is also stored in the field p.
     */
    public double run( DoubleArrayList effects, DoubleArrayList sampleSizes ) {

        DoubleArrayList weights;
        DoubleArrayList conditionalVariances;
        this.n = Descriptive.sum( sampleSizes );

        if ( transform ) {
            DoubleArrayList fzte = CorrelationStats.fisherTransform( effects );

            // initial values.
            conditionalVariances = fisherTransformedSamplingVariances( sampleSizes );
            weights = metaFEWeights( conditionalVariances );
            this.q = super.qStatistic( fzte, conditionalVariances, super.weightedMean( fzte, weights ) );

            if ( !fixed ) { // adjust the conditional variances and weights.
                this.bsv = metaREVariance( fzte, conditionalVariances, weights );

                for ( int i = 0; i < conditionalVariances.size(); i++ ) {
                    conditionalVariances.setQuick( i, conditionalVariances.getQuick( i ) + bsv );
                }
                weights = metaFEWeights( conditionalVariances );
            }

            this.e = super.weightedMean( fzte, weights );
        } else {

            conditionalVariances = samplingVariances( effects, sampleSizes );
            weights = metaFEWeights( conditionalVariances );
            this.q = super.qStatistic( effects, conditionalVariances, super.weightedMean( effects, weights ) );

            if ( !fixed ) { // adjust the conditional variances and weights.
                this.bsv = metaREVariance( effects, conditionalVariances, weights );
                for ( int i = 0; i < conditionalVariances.size(); i++ ) {
                    conditionalVariances.setQuick( i, conditionalVariances.getQuick( i ) + bsv );
                }

                weights = metaFEWeights( conditionalVariances );
            }

            this.e = super.weightedMean( effects, weights );
        }
        this.v = super.metaVariance( conditionalVariances );
        this.z = Math.abs( e ) / Math.sqrt( v );
        this.p = Probability.errorFunctionComplemented( z );

        // System.err.println(effects);

        // if ( qTest( q, effects.size() ) < 0.05 ) {
        // System.err.println("Q was significant: " + qTest( q, effects.size() ) );
        // }

        return p;
    }

    public void setFixed( boolean fixed ) {
        this.fixed = fixed;
    }

    public void setTransform( boolean transform ) {
        this.transform = transform;
    }

    /**
     * Equation 18-8 from CH. For z-transformed correlations.
     * 
     * <pre>
     * v_i = 1 / ( n_i - 3 )
     * </pre>
     * 
     * @param n
     * @return
     */
    protected double fisherTransformedSamplingVariance( double sampleSize ) {

        if ( sampleSize <= 3.0 ) throw new IllegalStateException( "N is too small" );

        return 1.0 / ( sampleSize - 3.0 );
    }

    /**
     * Run equation CH 18-8 on a list of sample sizes.
     * 
     * @param sampleSizes
     * @return
     */
    protected DoubleArrayList fisherTransformedSamplingVariances( DoubleArrayList sampleSizes ) {

        DoubleArrayList answer = new DoubleArrayList( sampleSizes.size() );
        for ( int i = 0; i < sampleSizes.size(); i++ ) {
            answer.add( fisherTransformedSamplingVariance( sampleSizes.getQuick( i ) ) );
        }
        return answer;
    }

    /**
     * Run equation CH 18-10 on a list of sample sizes and effects.
     * 
     * @param effectSizes
     * @param sampleSizes
     * @see samplingVariance
     * @return
     */
    protected DoubleArrayList samplingVariances( DoubleArrayList effectSizes, DoubleArrayList sampleSizes ) {

        if ( effectSizes.size() != sampleSizes.size() ) throw new IllegalArgumentException( "Unequal sample sizes." );

        DoubleArrayList answer = new DoubleArrayList( sampleSizes.size() );
        for ( int i = 0; i < sampleSizes.size(); i++ ) {

            double ef = effectSizes.getQuick( i );

            if ( Double.isNaN( ef ) ) {
                answer.add( Double.NaN );
            } else {
                answer.add( samplingVariance( ef, sampleSizes.getQuick( i ) ) );
            }
        }
        return answer;
    }
}