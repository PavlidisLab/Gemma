package ubic.gemma.core.analysis.stats;

import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;
import org.springframework.util.Assert;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.gemma.core.analysis.singleCell.SingleCellDescriptive;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
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
     * Count the number of values in a vector.
     */
    public static int count( DataVector vector ) {
        if ( vector.getQuantitationType().getType() == StandardQuantitationType.PRESENTABSENT ) {
            return countPresentAbsent( vector.getDataAsBooleans() );
        }
        if ( vector.getQuantitationType().getType() == StandardQuantitationType.COUNT ) {
            switch ( vector.getQuantitationType().getRepresentation() ) {
                case INT:
                    return countCount( vector.getDataAsInts() );
                case LONG:
                    return countCount( vector.getDataAsLongs() );
                case FLOAT:
                    return countCount( vector.getDataAsFloats(), getMissingFloatCountValue( vector.getQuantitationType() ) );
                case DOUBLE:
                    return countCount( vector.getDataAsDoubles(), getMissingCountValue( vector.getQuantitationType() ) );
                default:
                    throw new UnsupportedOperationException( "Counting data represented as " + vector.getQuantitationType().getRepresentation() + " is not supported." );
            }
        }
        switch ( vector.getQuantitationType().getRepresentation() ) {
            case BOOLEAN:
                // unless the type is present/absent, we cannot treat it as a vector of missing values
            case INT:
            case LONG:
                // fixed-length encoding, all the values are deemed to be present
                return vector.getData().length / vector.getQuantitationType().getRepresentation().getSizeInBytes();
            case FLOAT:
                return count( vector.getDataAsFloats() );
            case DOUBLE:
                return count( vector.getDataAsDoubles() );
            case CHAR:
                return count( vector.getDataAsChars() );
            case STRING:
                return count( vector.getDataAsStrings() );
            default:
                throw new UnsupportedOperationException( "Don't know how to count values from a " + vector.getQuantitationType().getRepresentation() + " vector." );
        }
    }

    private static int countPresentAbsent( boolean[] data ) {
        return data.length - countMissingPresentAbsent( data );
    }

    private static int count( char[] data ) {
        return data.length - countMissing( data );
    }

    private static int count( String[] data ) {
        return data.length - countMissing( data );
    }

    private static int count( float[] data ) {
        return data.length - countMissing( data );
    }

    private static int count( double[] data ) {
        return data.length - countMissing( data );
    }

    private static int countCount( long[] data ) {
        return data.length - countCountMissing( data );
    }

    private static int countCount( int[] data ) {
        return data.length - countCountMissing( data );
    }

    private static int countCount( float[] data, float missingFloatCountValue ) {
        return data.length - countCountMissing( data, missingFloatCountValue );
    }

    private static int countCount( double[] data, double missingValueForScaleType ) {
        return data.length - countCountMissing( data, missingValueForScaleType );
    }

    /**
     * Count the number of missing values.
     */
    public static int countMissing( DataVector vector ) {
        if ( vector.getQuantitationType().getType() == StandardQuantitationType.PRESENTABSENT ) {
            return countMissingPresentAbsent( vector.getDataAsBooleans() );
        }
        if ( vector.getQuantitationType().getType() == StandardQuantitationType.COUNT ) {
            switch ( vector.getQuantitationType().getRepresentation() ) {
                case INT:
                    return countCountMissing( vector.getDataAsInts() );
                case LONG:
                    return countCountMissing( vector.getDataAsLongs() );
                case DOUBLE:
                    return countCountMissing( vector.getDataAsDoubles(), getMissingCountValue( vector.getQuantitationType() ) );
                default:
                    throw new UnsupportedOperationException( "Counting data represented as " + vector.getQuantitationType().getRepresentation() + " is not supported." );
            }
        }
        switch ( vector.getQuantitationType().getRepresentation() ) {
            case BOOLEAN:
                // unless the type is present/absent, we cannot treat it as a vector of missing values
            case INT:
            case LONG:
                return 0;
            case CHAR:
                return countMissing( vector.getDataAsChars() );
            case STRING:
                // empty strings indicate missing values, there is no way to encode it as null
                return countMissing( vector.getDataAsStrings() );
            case DOUBLE:
                // NaNs indicate missing values
                return countMissing( vector.getDataAsDoubles() );
            default:
                throw new UnsupportedOperationException( "Don't know how to count missing values from a " + vector.getQuantitationType().getRepresentation() + " vector." );
        }
    }

    /**
     * For present/absent data, missing values are counted as missing.
     */
    private static int countMissingPresentAbsent( boolean[] data ) {
        int missingValues = 0;
        for ( boolean b : data ) {
            if ( !b ) {
                missingValues += 1;
            }
        }
        return missingValues;

    }

    /**
     * A null char is always considered missing.
     */
    private static int countMissing( char[] data ) {
        int missingValues = 0;
        for ( long l : data ) {
            if ( l == '\0' ) {
                missingValues++;
            }
        }
        return missingValues;
    }

    /**
     * An empty string is always considered missing.
     */
    private static int countMissing( String[] data ) {
        int missingValues = 0;
        for ( String l : data ) {
            if ( l.isEmpty() ) {
                missingValues++;
            }
        }
        return missingValues;
    }

    /**
     * For floats, {@link Float#NaN} is always considered missing.
     */
    private static int countMissing( float[] data ) {
        int missingValues = 0;
        for ( float d : data ) {
            if ( Float.isNaN( d ) ) {
                missingValues++;
            }
        }
        return missingValues;
    }

    /**
     * For doubles, {@link Double#NaN} is always considered missing.
     */
    private static int countMissing( double[] data ) {
        int missingValues = 0;
        for ( double d : data ) {
            if ( Double.isNaN( d ) ) {
                missingValues++;
            }
        }
        return missingValues;
    }

    private static int countCountMissing( long[] data ) {
        int missingValues = 0;
        for ( long l : data ) {
            if ( l == 0L ) {
                missingValues++;
            }
        }
        return missingValues;
    }

    private static int countCountMissing( int[] data ) {
        int missingValues = 0;
        for ( long l : data ) {
            if ( l == 0 ) {
                missingValues++;
            }
        }
        return missingValues;
    }

    private static int countCountMissing( float[] data, float defaultValue ) {
        int missingValues = 0;
        for ( float d : data ) {
            if ( Float.isNaN( d ) || d == defaultValue ) {
                missingValues++;
            }
        }
        return missingValues;
    }

    private static int countCountMissing( double[] data, double defaultValue ) {
        int missingValues = 0;
        for ( double d : data ) {
            if ( Double.isNaN( d ) || d == defaultValue ) {
                missingValues++;
            }
        }
        return missingValues;
    }

    public static double sum( DataVector vector ) {
        switch ( vector.getQuantitationType().getRepresentation() ) {
            case FLOAT:
                return sum( vector.getDataAsFloats(), vector.getQuantitationType().getScale() );
            case DOUBLE:
                return sum( vector.getDataAsDoubles(), vector.getQuantitationType().getScale() );
            case INT:
                return sum( vector.getDataAsInts(), vector.getQuantitationType().getScale() );
            case LONG:
                return sum( vector.getDataAsLongs(), vector.getQuantitationType().getScale() );
            default:
                throw new UnsupportedOperationException();
        }
    }

    public static double sum( float[] data, ScaleType scaleType ) {
        return sum( float2double( data ), scaleType );
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

    public static int sum( int[] data, ScaleType scaleType ) {
        Assert.isTrue( scaleType == ScaleType.COUNT );
        int s = 0;
        for ( int d : data ) {
            s += d;
        }
        return s;
    }

    public static long sum( long[] data, ScaleType scaleType ) {
        Assert.isTrue( scaleType == ScaleType.COUNT );
        long s = 0;
        for ( long d : data ) {
            s += d;
        }
        return s;
    }

    public static double sumUnscaled( float[] data, ScaleType scaleType ) {
        return sumUnscaled( float2double( data ), scaleType );
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

    public static double sumUnscaled( int[] data, ScaleType scaleType ) {
        return sum( data, scaleType );
    }

    public static double sumUnscaled( long[] data, ScaleType scaleType ) {
        return sum( data, scaleType );
    }

    /**
     * Calculate the mean of the data in the vector.
     * <p>
     * If the vector uses a log-scale, a regular mean is used. For counting or linear scales, a geometric mean is used.
     */
    public static double mean( DataVector vector ) {
        return mean( vector.getDataAsDoubles(), vector.getQuantitationType().getScale() );
    }

    public static double mean( float[] data, ScaleType scaleType ) {
        return mean( float2double( data ), scaleType );
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

    public static double mean( int[] data, ScaleType scaleType ) {
        return ( double ) sum( data, scaleType ) / ( double ) data.length;
    }

    public static double mean( long[] data, ScaleType scaleType ) {
        return ( double ) sum( data, scaleType ) / ( double ) data.length;
    }

    public static double sampleStandardDeviation( DataVector vector ) {
        PrimitiveType representation = vector.getQuantitationType().getRepresentation();
        switch ( representation ) {
            case FLOAT:
                return sampleStandardDeviation( vector.getDataAsFloats(), vector.getQuantitationType().getScale() );
            case DOUBLE:
                return sampleStandardDeviation( vector.getDataAsDoubles(), vector.getQuantitationType().getScale() );
            case INT:
                return sampleStandardDeviation( vector.getDataAsInts(), vector.getQuantitationType().getScale() );
            case LONG:
                return sampleStandardDeviation( vector.getDataAsLongs(), vector.getQuantitationType().getScale() );
            default:
                throw unsupportedRepresentation( representation, "sampleStandardDeviation" );
        }
    }

    public static double sampleStandardDeviation( float[] data, ScaleType scaleType ) {
        // FIXME: baseCode does not include the sample size correction
        return Descriptive.sampleStandardDeviation( data.length, sampleVariance( data, scaleType ) );
    }

    public static double sampleStandardDeviation( double[] data, ScaleType scaleType ) {
        // FIXME: baseCode does not include the sample size correction
        return Descriptive.sampleStandardDeviation( data.length, sampleVariance( data, scaleType ) );
    }

    public static double sampleStandardDeviation( int[] data, ScaleType scaleType ) {
        // FIXME: baseCode does not include the sample size correction
        return Descriptive.sampleStandardDeviation( data.length, sampleVariance( data, scaleType ) );
    }

    public static double sampleStandardDeviation( long[] data, ScaleType scaleType ) {
        // FIXME: baseCode does not include the sample size correction
        return Descriptive.sampleStandardDeviation( data.length, sampleVariance( data, scaleType ) );
    }

    public static double sampleVariance( DataVector vector ) {
        return sampleVariance( vector.getDataAsDoubles(), vector.getQuantitationType().getScale() );
    }

    public static double sampleVariance( float[] data, ScaleType scaleType ) {
        return sampleVariance( float2double( data ), scaleType );
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
                int len = vec.size();
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

    public static double sampleVariance( int[] data, ScaleType scaleType ) {
        Assert.isTrue( scaleType == ScaleType.COUNT );
        DoubleArrayList d = new DoubleArrayList( int2double( data ) );
        // no need to use DescriptiveWithMissing for integer data
        return Descriptive.sampleVariance( d, Descriptive.mean( d ) );
    }

    public static double sampleVariance( long[] data, ScaleType scaleType ) {
        Assert.isTrue( scaleType == ScaleType.COUNT );
        DoubleArrayList d = new DoubleArrayList( long2double( data ) );
        // no need to use DescriptiveWithMissing for long data
        return Descriptive.sampleVariance( d, Descriptive.mean( d ) );
    }

    /**
     * Obtain the value that indicates a missing value for counting data.
     */
    public static float getMissingFloatCountValue( QuantitationType quantitationType ) {
        Assert.isTrue( quantitationType.getType() == StandardQuantitationType.COUNT,
                "Only counting data can be supplied." );
        Assert.isTrue( quantitationType.getRepresentation() == PrimitiveType.FLOAT,
                "Only float representation is supported." );
        switch ( quantitationType.getScale() ) {
            case LOG2:
            case LN:
            case LOG10:
            case LOGBASEUNKNOWN:
                return Float.NEGATIVE_INFINITY;
            case LOG1P: // in log1p, 0 is mapped back to 0
            default:
                return 0;
        }
    }

    /**
     * Obtain the value that indicates a missing value for counting data.
     */
    public static double getMissingCountValue( QuantitationType quantitationType ) {
        Assert.isTrue( quantitationType.getType() == StandardQuantitationType.COUNT,
                "Only counting data can be supplied." );
        Assert.isTrue( quantitationType.getRepresentation() == PrimitiveType.DOUBLE,
                "Only double representation is supported." );
        switch ( quantitationType.getScale() ) {
            case LOG2:
            case LN:
            case LOG10:
            case LOGBASEUNKNOWN:
                return Double.NEGATIVE_INFINITY;
            case LOG1P: // in log1p, 0 is mapped back to 0
            default:
                return 0;
        }
    }

    private static double[] float2double( float[] data ) {
        double[] dataAsDoubles = new double[data.length];
        for ( int i = 0; i < data.length; i++ ) {
            dataAsDoubles[i] = data[i];
        }
        return dataAsDoubles;
    }

    private static double[] int2double( int[] data ) {
        double[] dataAsDoubles = new double[data.length];
        for ( int i = 0; i < data.length; i++ ) {
            dataAsDoubles[i] = data[i];
        }
        return dataAsDoubles;
    }

    private static double[] long2double( long[] data ) {
        double[] dataAsDoubles = new double[data.length];
        for ( int i = 0; i < data.length; i++ ) {
            dataAsDoubles[i] = data[i];
        }
        return dataAsDoubles;
    }

    private static UnsupportedOperationException unsupportedRepresentation( PrimitiveType representation, String operation ) {
        return new UnsupportedOperationException( "Unsupported representation " + representation + " for " + operation + "." );
    }
}
