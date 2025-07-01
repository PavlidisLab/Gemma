package ubic.gemma.core.analysis.preprocess.convert;

import ubic.gemma.model.common.quantitationtype.PrimitiveType;

/**
 * Exception raised when data in a given representation cannot be converted to another representation.
 * @author poirigui
 */
public class UnsupportedQuantitationRepresentationConversionException extends UnsupportedQuantitationTypeConversionException {

    public UnsupportedQuantitationRepresentationConversionException( PrimitiveType from, PrimitiveType to ) {
        super( "Converting data from " + from + " to " + to + " is not supported." );
    }
}
