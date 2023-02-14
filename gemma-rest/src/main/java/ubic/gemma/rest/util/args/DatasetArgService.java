package ubic.gemma.rest.util.args;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

@Service
public class DatasetArgService extends AbstractEntityArgService<ExpressionExperiment, ExpressionExperimentService> {

    @Autowired
    public DatasetArgService( ExpressionExperimentService service ) {
        super( service );
    }
}
