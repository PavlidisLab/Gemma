package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

@Schema(type = "string", description = "A quantitation type name.")
public class QuantitationTypeByIdArg extends QuantitationTypeArg<Long> {
    QuantitationTypeByIdArg( long id ) {
        super( id );
    }

    @Override
    protected String getPropertyName( QuantitationTypeService service ) {
        return service.getIdentifierPropertyName();
    }

    @Override
    public QuantitationType getEntity( QuantitationTypeService service ) throws NotFoundException, BadRequestException {
        return checkEntity( service, service.load( getValue() ) );
    }

    @Override
    public QuantitationType getEntityForExpressionExperimentAndDataVectorType( ExpressionExperiment ee, Class<? extends DesignElementDataVector> dataVectorType, QuantitationTypeService service ) {
        return checkEntity( service, service.findByIdAndDataVectorType( ee, getValue(), dataVectorType ) );
    }
}
