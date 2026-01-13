package ubic.gemma.core.analysis.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

@Service
@Transactional(propagation = Propagation.NEVER)
public class ExpressionExperimentDeleterServiceImpl implements ExpressionExperimentDeleterService {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @Override
    public void delete( ExpressionExperiment ee ) {
        expressionExperimentService.remove( ee );
        expressionDataFileService.deleteAllFiles( ee );
    }
}
