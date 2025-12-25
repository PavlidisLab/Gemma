package ubic.gemma.model.common.measurement;

import org.springframework.util.Assert;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;

import java.util.Comparator;
import java.util.Objects;

/**
 * Utilities for {@link Measurement}.
 *
 * @author poirigui
 */
public class MeasurementUtils {

    /**
     * Compare two measurements.
     *
     * @throws IllegalArgumentException if the measurements have different units or if one is numerical and the other is
     *                                  not as per {@link #isNumerical(Measurement)}.
     */
    public static int compare( Measurement a, Measurement b ) {
        Assert.isTrue( Objects.equals( a.getUnit(), b.getUnit() ), "Cannot compare measurements with different units." );
        Assert.isTrue( isNumerical( a ) == isNumerical( b ), "Cannot compare numerical and non-numerical measurements." );
        if ( isNumerical( a ) && isNumerical( b ) ) {
            return Double.compare( measurement2double( a ), measurement2double( b ) );
        } else {
            return Comparator.nullsLast( String.CASE_INSENSITIVE_ORDER ).compare( a.getValue(), b.getValue() );
        }
    }

    /**
     * Check if a measurement is numerical.
     */
    public static boolean isNumerical( Measurement measurement ) {
        return measurement.getRepresentation() == PrimitiveType.FLOAT
                || measurement.getRepresentation() == PrimitiveType.DOUBLE
                || measurement.getRepresentation() == PrimitiveType.INT
                || measurement.getRepresentation() == PrimitiveType.LONG;
    }

    /**
     * Convert a measurement to a double. Missing values are treated as NaNs.
     *
     * @throws UnsupportedOperationException if the measurement representation is not supported, you can test this with
     *                                       {@link #isNumerical(Measurement)}.
     */
    public static double measurement2double( Measurement measurement ) {
        if ( measurement.getValue() == null ) {
            return Double.NaN;
        }
        switch ( measurement.getRepresentation() ) {
            case FLOAT:
                return measurement.getValueAsFloat();
            case DOUBLE:
                return measurement.getValueAsDouble();
            case INT:
                return measurement.getValueAsInt();
            case LONG:
                return measurement.getValueAsLong();
            default:
                throw new UnsupportedOperationException( "Unsupported measurement type: " + measurement.getRepresentation() );
        }
    }
}
