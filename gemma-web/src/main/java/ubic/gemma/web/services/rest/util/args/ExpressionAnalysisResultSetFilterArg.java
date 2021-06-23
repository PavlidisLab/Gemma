package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.persistence.util.ObjectFilter;

import java.util.LinkedList;
import java.util.List;

public class ExpressionAnalysisResultSetFilterArg extends FilterArg {

    private static final String ERROR_INVALID_PROPERTY = "Given filter property does not exist in expression analysis result sets.";
    private static final String ERROR_PARSE_ERROR = "Filter argument parsing error.";

    ExpressionAnalysisResultSetFilterArg( List<String[]> propertyNames, List<String[]> propertyValues, List<String[]> propertyOperators, List<Class[]> propertyTypes ) {
        super( propertyNames, propertyValues, propertyOperators, propertyTypes, ObjectFilter.DAO_EARS_ALIAS );
    }

    public ExpressionAnalysisResultSetFilterArg( String s, Exception e ) {
        super( s, e );
    }

    public static ExpressionAnalysisResultSetFilterArg valueOf( String s ) {
        if ( Strings.isNullOrEmpty( s ) )
            return new ExpressionAnalysisResultSetFilterArg( null, null, null, null );
        List<String[]> propertyNames = new LinkedList<>();
        List<String[]> propertyValues = new LinkedList<>();
        List<String[]> propertyOperators = new LinkedList<>();
        List<Class[]> propertyTypes;
        try {
            parseFilterString( s, propertyNames, propertyValues, propertyOperators );
            propertyTypes = getPropertiesTypes( propertyNames, ExpressionAnalysisResultSet.class );
        } catch ( IllegalArgumentException e ) {
            return new ExpressionAnalysisResultSetFilterArg( ERROR_PARSE_ERROR, e );
        } catch ( NoSuchFieldException e ) {
            return new ExpressionAnalysisResultSetFilterArg( ERROR_INVALID_PROPERTY, e );
        }
        return new ExpressionAnalysisResultSetFilterArg( propertyNames, propertyValues, propertyOperators, propertyTypes );
    }
}
