package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;

import javax.ws.rs.BadRequestException;

@Schema(type = "string", description = "A quantitation type numerical identifier.")
public class QuantitationTypeByNameArg extends QuantitationTypeArg<String> {

    QuantitationTypeByNameArg( String s ) {
        super( s );
    }

    @Override
    public QuantitationType getEntity( QuantitationTypeService service ) throws BadRequestException {
        throw new UnsupportedOperationException( "A name is insufficient to retrieve a unique quantitation type." );
    }

    @Override
    public QuantitationType getEntityForExpressionExperimentAndDataVectorType( ExpressionExperiment ee, Class<? extends DesignElementDataVector> dataVectorType, QuantitationTypeService service ) {
        return checkEntity( service.findByNameAndDataVectorType( ee, getValue(), dataVectorType ) );
    }
}