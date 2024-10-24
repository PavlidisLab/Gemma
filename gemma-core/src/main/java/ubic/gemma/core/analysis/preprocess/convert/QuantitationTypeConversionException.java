package ubic.gemma.core.analysis.preprocess.convert;

/**
 * Base class for representing problematic {@link ubic.gemma.model.common.quantitationtype.QuantitationType} conversion.
 * @author poirigui
 */
public class QuantitationTypeConversionException extends Exception {

    protected QuantitationTypeConversionException( String message ) {
        super( message );
    }
}
