package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

@Schema(type = "integer", format = "int64", description = "A quantitation type ID.")
public class QuantitationTypeByIdArg extends QuantitationTypeArg<Long> {
    QuantitationTypeByIdArg( long id ) {
        super( "id", Long.class, id );
    }

    @Override
    QuantitationType getEntity( QuantitationTypeService service ) throws NotFoundException, BadRequestException {
        return service.load( getValue() );
    }

}
