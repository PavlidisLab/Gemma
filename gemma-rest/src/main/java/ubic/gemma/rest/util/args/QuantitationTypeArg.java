package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

@Schema(oneOf = { QuantitationTypeByIdArg.class, QuantitationTypeByNameArg.class })
public abstract class QuantitationTypeArg<T> extends AbstractEntityArg<T, QuantitationType, QuantitationTypeService> {

    protected QuantitationTypeArg( T value ) {
        super( QuantitationType.class, value );
    }

    public abstract QuantitationType getEntityForExpressionExperimentAndDataVectorType( ExpressionExperiment ee, Class<? extends DesignElementDataVector> dataVectorType, QuantitationTypeService service );

    public static QuantitationTypeArg<?> valueOf( String s ) {
        try {
            return new QuantitationTypeByIdArg( Long.parseLong( s ) );
        } catch ( NumberFormatException e ) {

            return new QuantitationTypeByNameArg( s );
        }
    }
}
