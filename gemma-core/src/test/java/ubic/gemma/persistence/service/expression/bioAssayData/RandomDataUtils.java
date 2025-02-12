package ubic.gemma.persistence.service.expression.bioAssayData;

import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;

import static ubic.gemma.core.analysis.preprocess.convert.ScaleTypeConversionUtils.convertVector;

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
    public static void setSeed( long seed ) {
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
                double val = logNormalDistribution.sample();
                ScaleType scale = qt.getScale();
                return convertVector( new double[] { val }, StandardQuantitationType.AMOUNT, ScaleType.LINEAR, scale )[0];
            } else if ( qt.getType() == StandardQuantitationType.COUNT ) {
                int val = countDistribution.sample();
                ScaleType scale = qt.getScale();
                return convertVector( new int[] { val }, scale )[0];
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
                double[] val = logNormalDistribution.sample( n );
                return convertVector( val, StandardQuantitationType.AMOUNT, ScaleType.LINEAR, qt.getScale() );
            } else if ( qt.getType() == StandardQuantitationType.COUNT ) {
                int[] val = countDistribution.sample( n );
                return convertVector( val, qt.getScale() );
            } else {
                throw new IllegalArgumentException( "Don't know how to generate " + qt + " data." );
            }
        }
    }
}
