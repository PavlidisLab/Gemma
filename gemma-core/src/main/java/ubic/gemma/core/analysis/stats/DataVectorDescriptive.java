package ubic.gemma.core.analysis.stats;

import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.gemma.core.analysis.singleCell.SingleCellDescriptive;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssayData.DataVector;

/**
 * Compute descriptive statistics for {@link ubic.gemma.model.expression.bioAssayData.DataVector}.
 * @author poirigui
 * @see SingleCellDescriptive
 * @see ubic.basecode.math.DescriptiveWithMissing
 */
public class DataVectorDescriptive {

    /**
     * Count the number of missing values.
     */
    public static int countMissing( DataVector vector ) {
        int missingValues = 0;
        if ( vector.getQuantitationType().getType() == StandardQuantitationType.PRESENTABSENT ) {
            for ( boolean b : vector.getDataAsBooleans() ) {
                if ( !b ) {
                    missingValues += 1;
                }
            }
            return missingValues;
        }
        switch ( vector.getQuantitationType().getRepresentation() ) {
            case BOOLEAN:
                // unless the type is present/absent, we cannot treat it as a vector of missing values
            case INT:
            case LONG:
                break;
            case CHAR:
                for ( char c : vector.getDataAsChars() ) {
                    if ( c == '\0' ) {
                        missingValues += 1;
                    }
                }
                break;
            case STRING:
                // empty strings and null strings are stored the same and both indicate missing values
                for ( String s : vector.getDataAsStrings() ) {
                    if ( s == null || s.isEmpty() ) {
                        missingValues += 1;
                    }
                }
                break;
            case DOUBLE:
                // NaNs indicate missing values
                for ( double d : vector.getDataAsDoubles() ) {
                    if ( Double.isNaN( d ) ) {
                        missingValues += 1;
                    }
                }
                break;
            default:
                throw new UnsupportedOperationException( "Don't know how to count missing values from a " + vector.getQuantitationType().getRepresentation() + " vector." );
        }
        return missingValues;
    }

    public static double sum( DataVector vector ) {
        return sum( vector.getDataAsDoubles(), vector.getQuantitationType().getScale() );
    }

    public static double sum( double[] data, ScaleType scaleType ) {
        DoubleArrayList vec = new DoubleArrayList( data );
        switch ( scaleType ) {
            case LINEAR:
            case COUNT:
                return DescriptiveWithMissing.sum( vec );
            case LOG2:
            case LN:
            case LOG10:
            case LOGBASEUNKNOWN:
                double s = 0;
                for ( double datum : data ) {
                    if ( Double.isNaN( datum ) ) {
                        continue;
                    }
                    s += Math.exp( datum );
                }
                return Math.log( s );
            case LOG1P:
                double s2 = 0;
                for ( double datum : data ) {
                    if ( Double.isNaN( datum ) ) {
                        continue;
                    }
                    s2 += Math.expm1( datum );
                }
                return Math.log1p( s2 );
            default:
                throw new IllegalArgumentException( "Don't know how to calculate sum for scale type " + scaleType );
        }
    }

    /**
     * Calculate the sum of the data in the vector, but keep the result unscaled.
     */
    public static double sumUnscaled( double[] data, ScaleType scaleType ) {
        DoubleArrayList vec = new DoubleArrayList( data );
        switch ( scaleType ) {
            case LINEAR:
            case COUNT:
                return DescriptiveWithMissing.sum( vec );
            case LOG2:
            case LN:
            case LOG10:
            case LOGBASEUNKNOWN:
                double s = 0;
                for ( double datum : data ) {
                    if ( Double.isNaN( datum ) ) {
                        continue;
                    }
                    s += Math.exp( datum );
                }
                return s;
            case LOG1P:
                double s2 = 0;
                for ( double datum : data ) {
                    if ( Double.isNaN( datum ) ) {
                        continue;
                    }
                    s2 += Math.expm1( datum );
                }
                return s2;
            default:
                throw new IllegalArgumentException( "Don't know how to calculate sum for scale type " + scaleType );
        }
    }

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
