package ubic.gemma.persistence.service.expression.bioAssayData;

import cern.jet.random.engine.MersenneTwister;
import org.apache.commons.math3.distribution.AbstractIntegerDistribution;

public class NegativeBinomialDistribution extends AbstractIntegerDistribution {

    private final int i;
    private final double v;
    private cern.jet.random.NegativeBinomial distribution;

    public NegativeBinomialDistribution( int i, double v ) {
        super( null );
        this.i = i;
        this.v = v;
        this.distribution = new cern.jet.random.NegativeBinomial( i, v, new MersenneTwister() );
    }

    @Override
    public double probability( int x ) {
        return distribution.pdf( x );
    }

    @Override
    public double cumulativeProbability( int x ) {
        return distribution.cdf( x );
    }

    @Override
    public double getNumericalMean() {
        return i * ( 1 - v ) / v;
    }

    @Override
    public double getNumericalVariance() {
        return i * ( 1 - v ) / ( v * v );
    }

    @Override
    public int getSupportLowerBound() {
        return 0;
    }

    @Override
    public int getSupportUpperBound() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isSupportConnected() {
        return true;
    }

    @Override
    public void reseedRandomGenerator( long seed ) {
        this.distribution = new cern.jet.random.NegativeBinomial( i, v, new MersenneTwister( ( int ) seed ) );
    }

    @Override
    public int sample() {
        return distribution.nextInt();
    }
}
