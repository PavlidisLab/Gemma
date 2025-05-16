package ubic.gemma.core.analysis.preprocess.convert;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssayData.DataVector;

import java.util.Collection;
import java.util.function.Function;

/**
 * Convert {@link DataVector} to different {@link ScaleType}.
 * <p>
 * For now, all conversions produce {@link PrimitiveType#DOUBLE}.
 * @author poirigui
 */
public class ScaleTypeConversionUtils {

    private static final ThreadLocal<int[]> ONE_INT_VALUE = ThreadLocal.withInitial( () -> new int[1] );
    private static final ThreadLocal<long[]> ONE_LONG_VALUE = ThreadLocal.withInitial( () -> new long[1] );
    private static final ThreadLocal<float[]> ONE_FLOAT_VALUE = ThreadLocal.withInitial( () -> new float[1] );
    private static final ThreadLocal<double[]> ONE_DOUBLE_VALUE = ThreadLocal.withInitial( () -> new double[1] );

    public static <T extends DataVector> Collection<T> convertVectors( Collection<T> vectors, ScaleType toScale, Class<T> vectorType ) {
        return QuantitationTypeConversionUtils.convertVectors( vectors, qt -> getConvertedQuantitationType( qt, toScale ), ( vec, origVec ) -> vec.setDataAsDoubles( convertData( origVec, toScale ) ), vectorType );
    }

    private static QuantitationType getConvertedQuantitationType( QuantitationType qt, ScaleType toScale ) {
        QuantitationType quantitationType = QuantitationType.Factory.newInstance( qt );
        String description;
        if ( StringUtils.isNotBlank( qt.getDescription() ) ) {
            description = StringUtils.appendIfMissing( StringUtils.strip( qt.getDescription() ), "." ) + " ";
        } else {
            description = "";
        }
        description += "Data was converted from " + qt.getScale() + " to " + toScale + ".";
        quantitationType.setDescription( description );
        quantitationType.setScale( toScale );
        quantitationType.setRepresentation( PrimitiveType.DOUBLE );
        return quantitationType;
    }

    /**
     * Convert a single number.
     * <p>
     * For efficiency, thread-local variables are used. Once you are done converting scalars, make sure to clean-up
     * those variables with {@link #clearScalarConversionThreadLocalStorage()}.
     */
    public static double convertScalar( Number val, QuantitationType qt, ScaleType scaleType ) {
        if ( qt.getScale() == scaleType ) {
            return val.doubleValue();
        }
        if ( val instanceof Float ) {
            float[] vec = ONE_FLOAT_VALUE.get();
            vec[0] = val.floatValue();
            return convertData( vec, qt, scaleType )[0];
        } else if ( val instanceof Double ) {
            double[] vec = ONE_DOUBLE_VALUE.get();
            vec[0] = val.doubleValue();
            return convertData( vec, qt, scaleType )[0];
        } else if ( val instanceof Integer ) {
            int[] vec = ONE_INT_VALUE.get();
            vec[0] = val.intValue();
            return convertData( vec, scaleType )[0];
        } else if ( val instanceof Long ) {
            long[] vec = ONE_LONG_VALUE.get();
            vec[0] = val.longValue();
            return convertData( vec, scaleType )[0];
        } else {
            throw new UnsupportedOperationException( "Cannot convert " + val.getClass().getSimpleName() + " to " + scaleType + " scale." );
        }
    }

    /**
     * Clear the thread-local storage used for converting scalars.
     */
    public static void clearScalarConversionThreadLocalStorage() {
        ONE_FLOAT_VALUE.remove();
        ONE_DOUBLE_VALUE.remove();
        ONE_INT_VALUE.remove();
        ONE_LONG_VALUE.remove();
    }

    /**
     * Convert a vector to the target scale.
     * @param scaleType the target scale, or null to keep the original scale
     * @throws IllegalArgumentException      if the conversion is not possible
     * @throws UnsupportedOperationException if the conversion is not supported
     */
    public static double[] convertData( DataVector vec, ScaleType scaleType ) {
        switch ( vec.getQuantitationType().getRepresentation() ) {
            case FLOAT:
                return convertData( vec.getDataAsFloats(), vec.getQuantitationType(), scaleType );
            case DOUBLE:
                return convertData( vec.getDataAsDoubles(), vec.getQuantitationType(), scaleType );
            case INT:
                return convertData( vec.getDataAsInts(), scaleType );
            case LONG:
                return convertData( vec.getDataAsLongs(), scaleType );
            default:
                throw new UnsupportedOperationException( "Conversion of " + vec.getQuantitationType().getRepresentation() + " is not supported." );
        }
    }

    /**
     * Convert a vector of float data to the target scale.
     */
    public static double[] convertData( float[] vec, QuantitationType quantitationType, ScaleType scaleType ) {
        return convertData( float2double( vec ), quantitationType.getType(), quantitationType.getScale(), scaleType );
    }

    /**
     * Convert a vector of double data to the target scale.
     */
    public static double[] convertData( double[] vec, QuantitationType quantitationType, ScaleType scaleType ) {
        return convertData( vec, quantitationType.getType(), quantitationType.getScale(), scaleType );
    }

    /**
     * Convert a vector of counting data to the target scale.
     * <p>
     * The type and scale are assumed to be counts.
     */
    public static double[] convertData( int[] vec, ScaleType scaleType ) {
        if ( scaleType == ScaleType.LINEAR || scaleType == ScaleType.COUNT ) {
            return int2double( vec );
        }
        double[] result = new double[vec.length];
        switch ( scaleType ) {
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

    /**
     * Convert a vector of counting data to the target scale.
     * <p>
     * The type and scale are assumed to be counts.
     */
    public static double[] convertData( long[] vec, ScaleType scaleType ) {
        if ( scaleType == ScaleType.LINEAR || scaleType == ScaleType.COUNT ) {
            return long2double( vec );
        }
        double[] result = new double[vec.length];
        switch ( scaleType ) {
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

    public static double[] convertData( double[] vec, StandardQuantitationType fromType, ScaleType fromScale, ScaleType scaleType ) {
        if ( fromScale == scaleType ) {
            return vec;
        }
        if ( fromScale == ScaleType.COUNT && scaleType == ScaleType.LINEAR ) {
            // count -> linear requires no transformation
            return vec;
        }
        double[] unscaled;
        // first, unscale the data
        switch ( fromScale ) {
            case COUNT:
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
}
