package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.web.services.rest.util.MalformedArgException;

/**
 * Represents an expression analysis result set identifier.
 */
@Schema(type = "integer",
        description = "Represents an expression analysis result set identifier")
public class ExpressionAnalysisResultSetArg extends AbstractEntityArg<Long, ExpressionAnalysisResultSet, ExpressionAnalysisResultSetService> {

    private ExpressionAnalysisResultSetArg( long value ) {
        super( ExpressionAnalysisResultSet.class, value );
    }

    @Override
    public ExpressionAnalysisResultSet getEntity( ExpressionAnalysisResultSetService service ) {
        return checkEntity( service.loadWithExperimentAnalyzed( getValue() ) );
    }

    public ExpressionAnalysisResultSet getEntityWithContrastsAndResults( ExpressionAnalysisResultSetService service ) {
        return checkEntity( service.loadWithResultsAndContrasts( getValue() ) );
    }

    public static ExpressionAnalysisResultSetArg valueOf( String s ) {
        try {
            return new ExpressionAnalysisResultSetArg( Long.parseLong( s ) );
        } catch ( NumberFormatException e ) {
            throw new MalformedArgException( String.format( "Could not parse expression analysis result set identifier %s.", s ), e );
        }
    }
}
