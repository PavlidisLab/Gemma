package ubic.gemma.persistence.service.expression.bioAssayData;

import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;

/**
 * Shared utilities for generating random data.
 * @see RandomBulkDataUtils
 * @see RandomSingleCellDataUtils
 * @see RandomExpressionDataMatrixUtils
 */
class RandomDataUtils {

    private static final NegativeBinomialDistribution countDistribution = new NegativeBinomialDistribution( 6, 0.5 );
    private static final LogNormalDistribution logNormalDistribution = new LogNormalDistribution( 6, 0.5 );
    private static final UniformRealDistribution uniform100Distribution = new UniformRealDistribution( 0, 100 );

    /**
     * Set the seed used to generate random single-cell vectors.
     */
    public static void setSeed( int seed ) {
        countDistribution.reseedRandomGenerator( seed );
        logNormalDistribution.reseedRandomGenerator( seed );
        uniform100Distribution.reseedRandomGenerator( seed );
    }

    public static double sample( QuantitationType qt ) {
        if ( qt.getScale() == ScaleType.PERCENT ) {
            return uniform100Distribution.sample();
        } else if ( qt.getScale() == ScaleType.PERCENT1 ) {
            return uniform100Distribution.sample() / 100.0;
        } else {
            if ( qt.getType() == StandardQuantitationType.AMOUNT ) {
                return transform( logNormalDistribution.sample(), qt.getScale() );
            } else if ( qt.getType() == StandardQuantitationType.COUNT ) {
                return transform( countDistribution.sample(), qt.getScale() );
            } else {
                throw new IllegalArgumentException( "Don't know how to generate " + qt + " data." );
            }
        }
    }

    public static double[] sample( QuantitationType qt, int n ) {
        if ( qt.getScale() == ScaleType.PERCENT ) {
            return uniform100Distribution.sample( n );
        } else if ( qt.getScale() == ScaleType.PERCENT1 ) {
            double[] vec = uniform100Distribution.sample( n );
            for ( int i = 0; i < vec.length; i++ ) {
                vec[i] /= 100.0;
            }
            return vec;
        } else {
            if ( qt.getType() == StandardQuantitationType.AMOUNT ) {
                return transform( logNormalDistribution.sample( n ), qt.getScale() );
            } else if ( qt.getType() == StandardQuantitationType.COUNT ) {
                return transform( countDistribution.sample( n ), qt.getScale() );
            } else {
                throw new IllegalArgumentException( "Don't know how to generate " + qt + " data." );
            }
        }
    }

    private static double[] transform( double[] val, ScaleType scale ) {
        for ( int i = 0; i < val.length; i++ ) {
            val[i] = transform( val[i], scale );
        }
        return val;
    }

    private static double[] transform( int[] val, ScaleType scale ) {
        double[] newVal = new double[val.length];
        for ( int i = 0; i < val.length; i++ ) {
            newVal[i] = transform( ( double ) val[i], scale );
        }
        return newVal;
    }

    static double transform( double val, ScaleType scale ) {
        switch ( scale ) {
            case LINEAR:
                return val;
            case COUNT:
                return Math.rint( val );
            case LOG2:
                return Math.log( val ) / Math.log( 2 );
            case LN:
                return Math.log( val );
            case LOG10:
                return Math.log10( val );
            case LOG1P:
                return Math.log1p( val );
            default:
                throw new IllegalArgumentException( "Unsupported scale type: " + scale );
        }
    }
}
