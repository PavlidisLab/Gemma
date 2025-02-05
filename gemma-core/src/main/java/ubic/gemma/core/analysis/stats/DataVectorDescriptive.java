package ubic.gemma.core.analysis.stats;

import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.gemma.core.analysis.singleCell.SingleCellDescriptive;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssayData.DataVector;

/**
 * Compute descriptive statistics for {@link ubic.gemma.model.expression.bioAssayData.DataVector}.
 * @author poirigui
 * @see SingleCellDescriptive
 * @see ubic.basecode.math.DescriptiveWithMissing
 */
public class DataVectorDescriptive {

    /**
     * Calculate the mean of the data in the vector.
     * <p>
     * If the vector uses a log-scale, a regular mean is used. For counting or linear scales, a geometric mean is used.
     */
    public static double mean( DataVector vector ) {
        return mean( vector.getDataAsDoubles(), vector.getQuantitationType().getScale() );
    }

    public static double mean( double[] data, ScaleType scaleType ) {
        DoubleArrayList vec = new DoubleArrayList( data );
        switch ( scaleType ) {
            case LINEAR:
            case COUNT:
                return DescriptiveWithMissing.geometricMean( vec );
            case LOG2:
            case LN:
            case LOG10:
            case LOGBASEUNKNOWN:
                return DescriptiveWithMissing.mean( vec );
            // TODO:
            // case LOG1P:
            //     return Math.log1p( DescriptiveWithMissing.mean( vec.forEach( Math::expm1 ) ) );
            default:
                throw new IllegalArgumentException( "Don't know how to calculate mean for scale type " + scaleType );
        }
    }

    public static double sampleStandardDeviation( DataVector vector ) {
        return sampleStandardDeviation( vector.getDataAsDoubles(), vector.getQuantitationType().getScale() );
    }

    public static double sampleStandardDeviation( double[] data, ScaleType scaleType ) {
        // FIXME: baseCode does not include the sample size correction
        return Descriptive.sampleStandardDeviation( data.length, sampleVariance( data, scaleType ) );
    }

    public static double sampleVariance( DataVector vector ) {
        return sampleVariance( vector.getDataAsDoubles(), vector.getQuantitationType().getScale() );
    }

    public static double sampleVariance( double[] data, ScaleType scaleType ) {
        DoubleArrayList vec = new DoubleArrayList( data );
        double mean = DescriptiveWithMissing.mean( vec );
        switch ( scaleType ) {
            case LINEAR:
            case COUNT:
                // https://en.wikipedia.org/wiki/Geometric_standard_deviation
                // this is the counterpart of the geometric mean
                // it's been adapted to deal with missing values
                double[] arr = vec.elements();
                int len = arr.length;
                double var = 0;
                for ( double v : arr ) {
                    if ( Double.isNaN( v ) ) {
                        len--;
                        continue;
                    }
                    var += Math.pow( Math.log( v / mean ), 2 );
                }
                return Math.exp( Math.sqrt( var / len ) );
            case LOG2:
            case LN:
            case LOG10:
            case LOGBASEUNKNOWN:
                return DescriptiveWithMissing.sampleVariance( vec, mean );
            // TODO:
            // case LOG1P:
            //     return Math.log1p( DescriptiveWithMissing.mean( vec.forEach( Math::expm1 ) ) );
            default:
                throw new IllegalArgumentException( "Don't know how to calculate mean for scale type " + scaleType );
        }
    }
}
