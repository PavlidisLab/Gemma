package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.NonUniqueQuantitationTypeByNameException;
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

    @Override
    QuantitationType getEntity( ExpressionExperiment ee, QuantitationTypeService service, Class<? extends DesignElementDataVector> dataVectorType ) {
        try {
            return service.findByNameAndVectorType( ee, getValue(), dataVectorType );
        } catch ( NonUniqueQuantitationTypeByNameException e ) {
            throw new BadRequestException( "More than one quantitation type uses the given name. Use a numerical ID instead.", e );
        }
    }
}
