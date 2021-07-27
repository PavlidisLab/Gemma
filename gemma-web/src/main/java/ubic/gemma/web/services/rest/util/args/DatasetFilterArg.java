package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.util.ObjectFilter;

import java.util.LinkedList;
import java.util.List;

/**
 * Filter argument for ExpressionExperiment filtering.
 *
 * @author tesarst
 */
public class DatasetFilterArg extends FilterArg {

    private static final String ERROR_INVALID_PROPERTY = "Given filter property does not exist in datasets.";
    private static final String ERROR_PARSE_ERROR = "Filter argument parsing error.";

    private DatasetFilterArg( List<String[]> propertyNames, List<String[]> propertyValues,
            List<String[]> propertyOperators, List<Class[]> propertyTypes ) {
        super( propertyNames, propertyValues, propertyOperators, propertyTypes, ObjectFilter.DAO_EE_ALIAS );
    }

    private DatasetFilterArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request filter string.
     * @return an instance of DatasetFilterArg representing the filtering options in the given string.
     * @throws javax.ws.rs.BadRequestException if the given string is not a well formed filter argument (E.g. is not CNF, or at least
     *                           one of the properties specified in the string does not exist in Expression Experiments.
     */
    @SuppressWarnings("unused")
    public static DatasetFilterArg valueOf( final String s ) {
        if ( Strings.isNullOrEmpty( s ) )
            return new DatasetFilterArg( null, null, null, null );

        List<String[]> propertyNames = new LinkedList<>();
        List<String[]> propertyValues = new LinkedList<>();
        List<String[]> propertyOperators = new LinkedList<>();
        List<Class[]> propertyTypes;

        try {
            parseFilterString( s, propertyNames, propertyValues, propertyOperators );
            propertyTypes = getPropertiesTypes( propertyNames, ExpressionExperiment.class );
        } catch ( IllegalArgumentException e ) {
            return new DatasetFilterArg( ERROR_PARSE_ERROR, e );
        } catch ( NoSuchFieldException e ) {
            return new DatasetFilterArg( ERROR_INVALID_PROPERTY, e );
        }
        return new DatasetFilterArg( propertyNames, propertyValues, propertyOperators, propertyTypes );
    }

}
