package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;

import javax.ws.rs.BadRequestException;

@Schema(type = "string", description = "A quantitation type name.")
public class QuantitationTypeByNameArg extends QuantitationTypeArg<String> {

    QuantitationTypeByNameArg( String s ) {
        super( "name", String.class, s );
    }

    @Override
    QuantitationType getEntity( QuantitationTypeService service ) throws BadRequestException {
        throw new UnsupportedOperationException( "A name is insufficient to retrieve a unique quantitation type." );
    }

}
