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

    public ExpressionAnalysisResultSet getEntityWithContrastsAndResults( ExpressionAnalysisResultSetArg analysisResultSet ) {
        return service.loadWithResultsAndContrasts( analysisResultSet.getValue() );
    }

    public ExpressionAnalysisResultSet getEntityWithContrastsAndResults( ExpressionAnalysisResultSetArg analysisResultSet, int offset, int limit ) {
        return service.loadWithResultsAndContrasts( analysisResultSet.getValue(), offset, limit );
    }

    public ExpressionAnalysisResultSet getEntityWithContrastsAndResults( ExpressionAnalysisResultSetArg analysisResultSet, double threshold, int offset, int limit ) {
        return service.loadWithResultsAndContrasts( analysisResultSet.getValue(), threshold, offset, limit );
    }
}
