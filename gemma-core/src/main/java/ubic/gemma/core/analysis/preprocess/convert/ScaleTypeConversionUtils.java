package ubic.gemma.core.analysis.preprocess.convert;

import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssayData.DataVector;

import javax.annotation.Nullable;

/**
 * Utilities for converting data vectors to different scales and representations.
 * @author poirigui
 */
public class ScaleTypeConversionUtils {

    private static final ThreadLocal<int[]> ONE_INT_VALUE = ThreadLocal.withInitial( () -> new int[1] );
    private static final ThreadLocal<double[]> ONE_DOUBLE_VALUE = ThreadLocal.withInitial( () -> new double[1] );

    /**
     * Convert a single number.
     */
    public static Number convertScalar( Number val, QuantitationType qt, @Nullable ScaleType scaleType ) {
        if ( scaleType == null || qt.getScale() == scaleType ) {
            return val;
        }
        if ( val instanceof Double ) {
            double[] vec = ONE_DOUBLE_VALUE.get();
            vec[0] = val.doubleValue();
            return convertVector( vec, qt, scaleType )[0];
        } else if ( val instanceof Integer ) {
            int[] vec = ONE_INT_VALUE.get();
            vec[0] = val.intValue();
            return convertVector( vec, scaleType )[0];
        } else {
            throw new UnsupportedOperationException( "Cannot convert " + val.getClass().getSimpleName() + " to " + scaleType + " scale." );
        }
    }

    /**
     * Convert a vector to the target scale.
     * @param scaleType the target scale, or null to keep the original scale
     * @throws IllegalArgumentException if the conversion is not possible
     */
    public static double[] convertVector( DataVector vec, @Nullable ScaleType scaleType ) {
        if ( vec.getQuantitationType().getRepresentation().equals( PrimitiveType.INT ) ) {
            return convertVector( vec.getDataAsInts(), scaleType );
        }
        // assume double, any other type will produce an exception
        return convertVector( vec.getDataAsDoubles(), vec.getQuantitationType(), scaleType );
    }

    /**
     * Convert a vector of data to the target scale.
     */
    public static double[] convertVector( double[] vec, QuantitationType quantitationType, @Nullable ScaleType scaleType ) {
        return convertVector( vec, quantitationType.getType(), quantitationType.getScale(), scaleType );
    }

    /**
     * Convert a vector of counting data to the target scale.
     * <p>
     * The type and scale are assumed to be counts.
     */
    public static double[] convertVector( int[] vec, @Nullable ScaleType scaleType ) {
        double[] result = new double[vec.length];
        if ( scaleType == null ) {
            scaleType = ScaleType.COUNT;
        }
        switch ( scaleType ) {
            case LINEAR:
            case COUNT:
                for ( int i = 0; i < vec.length; i++ ) {
                    result[i] = vec[i];
                }
                break;
            case LOG2:
            case LN:
            case LOG10:
                double logBase = getLogBase( scaleType );
                for ( int i = 0; i < vec.length; i++ ) {
                    result[i] = Math.log( vec[i] ) / Math.log( logBase );
                }
                break;
            case LOG1P:
                for ( int i = 0; i < vec.length; i++ ) {
                    result[i] = Math.log1p( vec[i] );
                }
                break;
            default:
                throw new UnsupportedOperationException( "Cannot rescale counting data on to a " + scaleType + " scale." );
        }
        return result;
    }

    public static double[] convertVector( double[] vec, StandardQuantitationType fromType, ScaleType fromScale, @Nullable ScaleType scaleType ) {
        if ( scaleType == null || fromScale == scaleType ) {
            return vec;
        }
        double[] unscaled;
        // first, unscale the data
        switch ( fromScale ) {
            case COUNT:
                if ( scaleType == ScaleType.LINEAR ) {
                    // count -> linear requires no transformation
                    return vec;
                }
            case LINEAR:
                unscaled = new double[vec.length];
                System.arraycopy( vec, 0, unscaled, 0, vec.length );
                break;
            case LOG2:
            case LN:
            case LOG10:
                double logBase = getLogBase( fromScale );
                unscaled = new double[vec.length];
                if ( scaleType == ScaleType.LOG2 || scaleType == ScaleType.LN || scaleType == ScaleType.LOG10 ) {
                    double targetLogBase = getLogBase( scaleType );
                    for ( int i = 0; i < vec.length; i++ ) {
                        unscaled[i] = vec[i] * Math.log( logBase ) / Math.log( targetLogBase );
                    }
                    return unscaled;
                } else {
                    for ( int i = 0; i < vec.length; i++ ) {
                        unscaled[i] = Math.pow( logBase, vec[i] );
                    }
                }
                break;
            case LOG1P:
                unscaled = new double[vec.length];
                for ( int i = 0; i < vec.length; i++ ) {
                    unscaled[i] = Math.expm1( vec[i] );
                }
                break;
            default:
                throw new UnsupportedOperationException( "Cannot unscale data on a " + fromScale + " scale." );
        }

        // then rescale
        switch ( scaleType ) {
            case LINEAR:
                return unscaled;
            case COUNT:
                if ( fromType != StandardQuantitationType.COUNT ) {
                    throw new IllegalArgumentException( "Cannot generate data on a COUNT scale from non-counting data." );
                }
                unscaled = new double[vec.length];
                for ( int i = 0; i < vec.length; i++ ) {
                    unscaled[i] = Math.rint( vec[i] );
                }
                return unscaled;
            case LOG2:
            case LN:
            case LOG10:
                double logBase = getLogBase( scaleType );
                for ( int i = 0; i < vec.length; i++ ) {
                    unscaled[i] = Math.log( vec[i] ) / Math.log( logBase );
                }
                return unscaled;
            case LOG1P:
                for ( int i = 0; i < vec.length; i++ ) {
                    unscaled[i] = Math.log1p( vec[i] );
                }
                return unscaled;
            default:
                throw new UnsupportedOperationException( "Cannot rescale data on to a " + scaleType + " scale." );
        }
    }

    private static double getLogBase( ScaleType scaleType ) {
        switch ( scaleType ) {
            case LOG2:
                return 2;
            case LN:
                return Math.E;
            case LOG10:
                return 10;
            default:
                throw new IllegalArgumentException( "Log base cannot be determined from: " + scaleType + "." );
        }
    }
}
