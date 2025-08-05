package ubic.gemma.core.analysis.preprocess.convert;

/**
 * Exception raised when data from a given quantitation type cannot be converted to another quantitation type.
 * @author poirigui
 */
public class UnsupportedQuantitationTypeConversionException extends QuantitationTypeConversionException {

    public UnsupportedQuantitationTypeConversionException( String message ) {
        super( message );
    }
}
