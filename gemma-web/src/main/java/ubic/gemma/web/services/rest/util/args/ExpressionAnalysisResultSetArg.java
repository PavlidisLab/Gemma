package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;

/**
 * Represents an expression analysis result set identifier.
 */
@Schema(description = "Represents an expression analysis result set identifier",
        implementation = Integer.class)
public class ExpressionAnalysisResultSetArg extends AbstractEntityArg<Long, ExpressionAnalysisResultSet, ExpressionAnalysisResultSetService> {

    public ExpressionAnalysisResultSetArg( long value ) {
        super( value );
    }

    @Override
    public ExpressionAnalysisResultSet getEntity( ExpressionAnalysisResultSetService service ) {
        return service.load( getValue() );
    }

    @Override
    public String getPropertyName() {
        return null;
    }

    @Override
    public String getEntityName() {
        return "ExpressionAnalysisResultSet";
    }

    public static ExpressionAnalysisResultSetArg valueOf( String s ) {
        return new ExpressionAnalysisResultSetArg( Long.parseLong( s ) );
    }

}
