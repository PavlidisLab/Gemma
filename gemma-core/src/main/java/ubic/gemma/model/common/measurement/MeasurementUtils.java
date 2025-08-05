package ubic.gemma.model.common.measurement;

/**
 * Utilities for {@link Measurement}.
 * @author poirigui
 */
public class MeasurementUtils {

    /**
     * Convert a measurement to a double. Missing values are treated as NaNs.
     * @throws UnsupportedOperationException if the measurement representation is not supported
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
