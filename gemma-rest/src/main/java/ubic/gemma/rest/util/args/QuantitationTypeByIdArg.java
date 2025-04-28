package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;

import javax.annotation.Nullable;

@Schema(type = "integer", format = "int64", description = "A quantitation type ID.")
public class QuantitationTypeByIdArg extends QuantitationTypeArg<Long> {
    QuantitationTypeByIdArg( long id ) {
        super( "id", Long.class, id );
    }

    @Override
    QuantitationType getEntity( QuantitationTypeService service ) {
        return service.load( getValue() );
    }

    @Nullable
    @Override
    QuantitationType getEntity( ExpressionExperiment ee, QuantitationTypeService service ) {
        return service.loadById( getValue(), ee );
    }

    @Nullable
    @Override
    QuantitationType getEntity( ExpressionExperiment ee, QuantitationTypeService service, Class<? extends DesignElementDataVector> dataVectorType ) {
        return service.loadByIdAndVectorType( getValue(), ee, dataVectorType );
    }
}
