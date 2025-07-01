package ubic.gemma.model.common.quantitationtype;

import org.springframework.util.Assert;

import javax.annotation.Nonnull;

public class QuantitationTypeUtils {

    /**
     * Obtain the default to use for a given quantitation type.
     * <p>
     * This can be used as a placeholder for missing values in data vectors.
     * <p>
     * Missing double and float values are represented as NaNs unless they are counts in which case they are
     * represented as zeroes.
     * <p>
     * For counting data represented as double or float, the default value is transformed as per
     * {@link #getDefaultCountValueAsDouble(QuantitationType)} and {@link #getDefaultCountValueAsFloat(QuantitationType)}.
     */
    @Nonnull
    public static Object getDefaultValue( QuantitationType quantitationType ) {
        PrimitiveType pt = quantitationType.getRepresentation();
        switch ( pt ) {
            case DOUBLE:
                if ( quantitationType.getType() == StandardQuantitationType.COUNT ) {
                    return getDefaultCountValueAsDouble( quantitationType );
                }
                return Double.NaN;
            case FLOAT:
                if ( quantitationType.getType() == StandardQuantitationType.COUNT ) {
                    return getDefaultCountValueAsFloat( quantitationType );
                }
                return Float.NaN;
            case STRING:
                return "";
            case CHAR:
                return ( char ) 0;
            case INT:
                return 0;
            case LONG:
                return 0L;
            case BOOLEAN:
                return false;
            default:
                throw new UnsupportedOperationException( "Missing values in data vectors of type " + quantitationType + " is not supported." );
        }
    }

    public static double getDefaultValueAsDouble( QuantitationType qt ) {
        Assert.isTrue( qt.getRepresentation() == PrimitiveType.DOUBLE,
                "Only double representation is supported." );
        if ( qt.getType() == StandardQuantitationType.COUNT ) {
            return getDefaultCountValueAsDouble( qt );
        } else {
            return 0;
        }
    }

    /**
     * Obtain the value that indicates a missing value for counting data.
     * <p>
     * The default count is zero, but if the data is on a log-scale, it will be mapped to {@link Double#NEGATIVE_INFINITY}.
     */
    public static double getDefaultCountValueAsDouble( QuantitationType quantitationType ) {
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

    /**
     * Obtain the value that indicates a missing value for counting data.
     */
    public static float getDefaultCountValueAsFloat( QuantitationType quantitationType ) {
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
}
