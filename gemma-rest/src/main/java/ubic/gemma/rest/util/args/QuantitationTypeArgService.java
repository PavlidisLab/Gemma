package ubic.gemma.rest.util.args;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;

@Service
public class QuantitationTypeArgService extends AbstractEntityArgService<QuantitationType, QuantitationTypeService> {

    @Autowired
    public QuantitationTypeArgService( QuantitationTypeService service ) {
        super( service );
    }

    public QuantitationType getEntity( QuantitationTypeArg<?> quantitationTypeArg, ExpressionExperiment ee ) {
        return checkEntity( quantitationTypeArg, quantitationTypeArg.getEntity( ee, service ) );
    }

    public QuantitationType getEntity( QuantitationTypeArg<?> quantitationTypeArg, ExpressionExperiment ee, Class<? extends DesignElementDataVector> vectorType ) {
        return checkEntity( quantitationTypeArg, quantitationTypeArg.getEntity( ee, service, vectorType ) );
    }
}
