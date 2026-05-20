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

import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;
import cern.jet.stat.Probability;

/**
 * Meta-analysis methods from chapter 18 of Cooper and Hedges, sections 2.1 and 3.1
 * <p>
 * These methods use the standardized mean difference statistic d:
 * 
 * <pre>
 * d_i = ( X_i &circ; t - X_i &circ; c ) / s_i
 * </pre>
 * 
 * where X <sub>i </sub> <sup>t </sup> is the mean of the treatment group in the ith study, X <sub>i </sub> <sup>ct
 * </sup> is the mean of the control group in the treatment group in the ith study, and s <sub>i </sub> is the pooled
 * standard deviation of the two groups. Essentially this is a t statistic.
 * 
 * @author pavlidis
 * 
 */
public class MeanDifferenceMetaAnalysis extends MetaAnalysis {

    private double bsv; // between-studies variance component;

    private double e; // unconditional effect;
    private boolean fixed = true;
    private double n; // total sample size
    private double p; // probability
    private double q; // q-score;
    private double v; // unconditional variance;
    private double z; // z score

    /**
     * @param b
     */
    public MeanDifferenceMetaAnalysis( boolean fixed ) {
        this.fixed = fixed;
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
     * @param effects
     * @param cvar Conditional variances.
     * @return
     */
    public double run( DoubleArrayList effects, DoubleArrayList cvar ) {
        DoubleArrayList weights;
        DoubleArrayList conditionalVariances;
        // this.n = Descriptive.sum( controlSizes ) + Descriptive.sum( testSizes );

        conditionalVariances = cvar.copy();
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
        this.v = super.metaVariance( conditionalVariances );
        this.z = Math.abs( e ) / Math.sqrt( v );
        this.p = Probability.errorFunctionComplemented( z );
        return p;
    }

    public double run( DoubleArrayList effects, DoubleArrayList controlSizes, DoubleArrayList testSizes ) {
        DoubleArrayList weights;
        DoubleArrayList conditionalVariances;
        this.n = Descriptive.sum( controlSizes ) + Descriptive.sum( testSizes );

        conditionalVariances = samplingVariances( effects, controlSizes, testSizes );
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
        this.v = super.metaVariance( conditionalVariances );
        this.z = Math.abs( e ) / Math.sqrt( v );
        this.p = Probability.errorFunctionComplemented( z );
        return p;
    }

    /**
     * CH eqn 18-7
     * 
     * @param d effect size
     * @param nC number of samples in control group
     * @param nT number of samples in test group
     * @return
     */
    public double samplingVariance( double d, double nC, double nT ) {
        return ( nT + nC ) / ( nT * nC ) + d * d / 2 * ( nT + nC );
    }

    /**
     * Run eqn 18-7 on a set of effect sizes.
     * 
     * @param effects
     * @param controlSizes
     * @param testSizes
     * @return
     */
    public DoubleArrayList samplingVariances( DoubleArrayList effects, DoubleArrayList controlSizes,
            DoubleArrayList testSizes ) {
        if ( effects.size() != controlSizes.size() || controlSizes.size() != testSizes.size() )
            throw new IllegalArgumentException( "Unequal sample sizes." );

        DoubleArrayList answer = new DoubleArrayList( controlSizes.size() );
        for ( int i = 0; i < controlSizes.size(); i++ ) {
            answer.add( samplingVariance( effects.getQuick( i ), controlSizes.getQuick( i ), testSizes.getQuick( i ) ) );
        }
        return answer;
    }

}