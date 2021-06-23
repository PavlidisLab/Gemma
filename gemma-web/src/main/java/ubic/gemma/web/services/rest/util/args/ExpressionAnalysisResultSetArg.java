package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSetValueObject;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;

/**
 * Represents an expression analysis result set identifier.
 */
public class ExpressionAnalysisResultSetArg extends MutableArg<Long, ExpressionAnalysisResultSet, ExpressionAnalysisResultSetValueObject, ExpressionAnalysisResultSetService> {

    public ExpressionAnalysisResultSetArg( long value ) {
        this.value = value;
    }

    @Override
    public ExpressionAnalysisResultSet getPersistentObject( ExpressionAnalysisResultSetService service ) {
        return service.load( value );
    }

    @Override
    public String getPropertyName( ExpressionAnalysisResultSetService service ) {
        return null;
    }

    public static ExpressionAnalysisResultSetArg valueOf( String s ) {
        return new ExpressionAnalysisResultSetArg( Long.parseLong( s ) );
    }
}
