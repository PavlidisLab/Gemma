package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;

@Schema(oneOf = { QuantitationTypeByIdArg.class, QuantitationTypeByNameArg.class })
public abstract class QuantitationTypeArg<T> extends AbstractEntityArg<T, QuantitationType, QuantitationTypeService> {

    protected QuantitationTypeArg( String propertyName, Class<T> propertyType, T value ) {
        super( propertyName, propertyType, value );
    }

    public static QuantitationTypeArg<?> valueOf( String s ) {
        try {
            return new QuantitationTypeByIdArg( Long.parseLong( s ) );
        } catch ( NumberFormatException e ) {

            return new QuantitationTypeByNameArg( s );
        }
    }
}
