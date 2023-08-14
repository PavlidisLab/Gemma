package ubic.gemma.rest.util.args;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;

@Service
public class ExpressionAnalysisResultSetArgService extends AbstractEntityArgService<ExpressionAnalysisResultSet, ExpressionAnalysisResultSetService> {

    @Autowired
    public ExpressionAnalysisResultSetArgService( ExpressionAnalysisResultSetService service ) {
        super( service );
    }
}
