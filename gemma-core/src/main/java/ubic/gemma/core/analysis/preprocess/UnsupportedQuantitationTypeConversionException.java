package ubic.gemma.core.analysis.preprocess;

import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;

/**
 * Exception raised when a quantitation type conversion is unsupported.
 * <p>
 * The common case is converting {@link StandardQuantitationType#ZSCORE} to {@link StandardQuantitationType#AMOUNT},
 * which only works in one direction.
 *
 * @author poirigui
 */
public class UnsupportedQuantitationTypeConversionException extends QuantitationTypeConversionException {

    public UnsupportedQuantitationTypeConversionException( StandardQuantitationType from, StandardQuantitationType to ) {
        super( String.format( "Converting %s to %s is not supported.", from, to ) );
    }
}
