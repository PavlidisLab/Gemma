package ubic.gemma.core.analysis.preprocess.convert;

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

    /**
     * Convert a vector to the target scale.
     * @param scaleType the target scale, or null to keep the original scale
     * @throws IllegalArgumentException if the conversion is not possible
     */
    public static double[] convertVector( DataVector vec, @Nullable ScaleType scaleType ) {
        return convertVector( vec.getDataAsDoubles(), vec.getQuantitationType(), scaleType );
    }

    /**
     * Convert a vector of data to the target scale.
     */
    public static double[] convertVector( double[] vec, QuantitationType quantitationType, @Nullable ScaleType scaleType ) {
        if ( scaleType == null || quantitationType.getScale() == scaleType ) {
            return vec;
        }
        double[] unscaled;
        // first, unscale the data
        switch ( quantitationType.getScale() ) {
            case LINEAR:
                if ( scaleType == ScaleType.LINEAR || scaleType == ScaleType.COUNT ) {
                    return vec;
                }
                unscaled = new double[vec.length];
                System.arraycopy( vec, 0, unscaled, 0, vec.length );
                break;
            case LOG2:
            case LN:
            case LOG10:
                double logBase = getLogBase( quantitationType.getScale() );
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
                throw new UnsupportedOperationException( "Cannot unscale data on a " + quantitationType.getScale() + " scale." );
        }

        // then rescale
        switch ( scaleType ) {
            case LINEAR:
                return unscaled;
            case COUNT:
                if ( quantitationType.getType() != StandardQuantitationType.COUNT ) {
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
