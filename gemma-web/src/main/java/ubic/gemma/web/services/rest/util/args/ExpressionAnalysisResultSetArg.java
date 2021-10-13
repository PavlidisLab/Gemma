package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;

/**
 * Represents an expression analysis result set identifier.
 */
@Schema(type = "integer",
        description = "Represents an expression analysis result set identifier")
public class ExpressionAnalysisResultSetArg extends AbstractEntityArg<Long, ExpressionAnalysisResultSet, ExpressionAnalysisResultSetService> {

    private ExpressionAnalysisResultSetArg( long value ) {
        super( ExpressionAnalysisResultSet.class, value );
    }

    public ExpressionAnalysisResultSetArg( String message, Throwable cause ) {
        super( ExpressionAnalysisResultSet.class, message, cause );
    }

    @Override
    public ExpressionAnalysisResultSet getEntity( ExpressionAnalysisResultSetService service ) {
        return service.load( getValue() );
    }

    public ExpressionAnalysisResultSet getEntityWithContrastsAndResults( ExpressionAnalysisResultSetService service ) {
        return service.thawWithResultsAndContrasts( getValue() );
    }

    public static ExpressionAnalysisResultSetArg valueOf( String s ) {
        try {
            return new ExpressionAnalysisResultSetArg( Long.parseLong( s ) );
        } catch ( NumberFormatException e ) {
            return new ExpressionAnalysisResultSetArg( "Could not parse expression analysis result set identifier.", e );
        }
    }
}
