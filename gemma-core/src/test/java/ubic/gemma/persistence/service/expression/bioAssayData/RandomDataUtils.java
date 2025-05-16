package ubic.gemma.persistence.service.expression.bioAssayData;

import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.springframework.util.Assert;
import ubic.gemma.core.analysis.preprocess.convert.ScaleTypeConversionUtils;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;

import static ubic.gemma.core.analysis.preprocess.convert.ScaleTypeConversionUtils.convertData;

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

    public static float sampleFloat( QuantitationType qt ) {
        Assert.isTrue( qt.getRepresentation() == PrimitiveType.FLOAT );
        return ( float ) sample( qt );
    }

    public static double sampleDouble( QuantitationType qt ) {
        Assert.isTrue( qt.getRepresentation() == PrimitiveType.DOUBLE );
        return sample( qt );
    }

    private static double sample( QuantitationType qt ) {
        if ( qt.getScale() == ScaleType.PERCENT ) {
            return uniform100Distribution.sample();
        } else if ( qt.getScale() == ScaleType.PERCENT1 ) {
            return uniform100Distribution.sample() / 100.0;
        } else {
            if ( qt.getType() == StandardQuantitationType.AMOUNT ) {
                double val = logNormalDistribution.sample();
                ScaleType scale = qt.getScale();
                return convertData( new double[] { val }, StandardQuantitationType.AMOUNT, ScaleType.LINEAR, scale )[0];
            } else if ( qt.getType() == StandardQuantitationType.COUNT ) {
                int val = countDistribution.sample();
                ScaleType scale = qt.getScale();
                return ScaleTypeConversionUtils.convertData( new int[] { val }, scale )[0];
            } else {
                throw new IllegalArgumentException( "Don't know how to generate " + qt + " data." );
            }
        }
    }

    public static float[] sampleFloats( QuantitationType qt, int n ) {
        Assert.isTrue( qt.getRepresentation() == PrimitiveType.FLOAT );
        double[] vec = sample( qt, n );
        float[] vecAsFloats = new float[vec.length];
        for ( int i = 0; i < vec.length; i++ ) {
            vecAsFloats[i] = ( float ) vec[i];
        }
        return vecAsFloats;
    }


    public static double[] sampleDoubles( QuantitationType qt, int n ) {
        Assert.isTrue( qt.getRepresentation() == PrimitiveType.DOUBLE );
        return sample( qt, n );
    }

    private static double[] sample( QuantitationType qt, int n ) {
        if ( n == 0 ) {
            return new double[0];
        }
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
                return convertData( val, StandardQuantitationType.AMOUNT, ScaleType.LINEAR, qt.getScale() );
            } else if ( qt.getType() == StandardQuantitationType.COUNT ) {
                int[] val = countDistribution.sample( n );
                return ScaleTypeConversionUtils.convertData( val, qt.getScale() );
            } else {
                throw new IllegalArgumentException( "Don't know how to generate " + qt + " data." );
            }
        }
    }

    public static int sampleInt( QuantitationType qt ) {
        Assert.isTrue( qt.getType() == StandardQuantitationType.COUNT );
        Assert.isTrue( qt.getScale() == ScaleType.COUNT );
        Assert.isTrue( qt.getRepresentation() == PrimitiveType.INT );
        return countDistribution.sample();
    }

    public static int[] sampleInts( QuantitationType qt, int n ) {
        Assert.isTrue( qt.getType() == StandardQuantitationType.COUNT );
        Assert.isTrue( qt.getScale() == ScaleType.COUNT );
        Assert.isTrue( qt.getRepresentation() == PrimitiveType.INT );
        return countDistribution.sample( n );
    }

    public static long sampleLong( QuantitationType qt ) {
        Assert.isTrue( qt.getType() == StandardQuantitationType.COUNT );
        Assert.isTrue( qt.getScale() == ScaleType.COUNT );
        Assert.isTrue( qt.getRepresentation() == PrimitiveType.LONG );
        return countDistribution.sample();
    }

    public static long[] sampleLongs( QuantitationType qt, int n ) {
        Assert.isTrue( qt.getScale() == ScaleType.COUNT );
        Assert.isTrue( qt.getRepresentation() == PrimitiveType.LONG );
        int[] vec = countDistribution.sample( n );
        long[] vecAsLongs = new long[vec.length];
        for ( int i = 0; i < vec.length; i++ ) {
            vecAsLongs[i] = vec[i];
        }
        return vecAsLongs;
    }
}
