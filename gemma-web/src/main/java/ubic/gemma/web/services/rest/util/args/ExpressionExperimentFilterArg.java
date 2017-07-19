package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.web.services.rest.util.GemmaApiException;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by tesarst on 17/07/17.
 * Filter argument for ExpressionExperiment filtering.
 */
public class ExpressionExperimentFilterArg extends FilterArg {

    private static final String ERROR_INVALID_PROPERTY = "Given filter property does not exist in expression experiments.";
    private static final String ERROR_PARTS_TOO_SHORT = "Provided filter string does not contain at least one of property-operator-value sets.";
    private static final String ERROR_ILLEGAL_OPERATOR = "Illegal operator: %s is not an accepted operator.";
    private static final String ERROR_PARSE_ERROR = "Filter argument parsing error.";

    private ExpressionExperimentFilterArg( List<String[]> propertyNames, List<Object[]> propertyValues,
            List<String[]> propertyOperators ) {
        super( propertyNames, propertyValues, propertyOperators, ObjectFilter.EEDAO_EE_ALIAS );
    }

    private ExpressionExperimentFilterArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request filter string.
     * @return an instance of ExpressionExperimentFilterArg representing the filtering options in the given string.
     * @throws GemmaApiException if the given string is not a well formed filter argument (E.g. is not CNF, or at least
     *                           one of the properties specified in the string does not exist in Expression Experiments.
     */
    @SuppressWarnings("unused")
    public static ExpressionExperimentFilterArg valueOf( final String s ) {
        if( Strings.isNullOrEmpty(s)) return new ExpressionExperimentFilterArg( null, null, null );

        List<String[]> propertyNames = new LinkedList<>();
        List<Object[]> propertyValues = new LinkedList<>();
        List<String[]> propertyOperators = new LinkedList<>();

        try {
            parseFilterString( s, propertyNames, propertyValues, propertyOperators );
            checkProperties( propertyNames, ExpressionExperiment.class );
        } catch ( IllegalArgumentException e ) {
            return new ExpressionExperimentFilterArg( ERROR_PARSE_ERROR, e );
        } catch ( NoSuchFieldException e ) {
            return new ExpressionExperimentFilterArg( ERROR_INVALID_PROPERTY, e );
        }
        return new ExpressionExperimentFilterArg( propertyNames, propertyValues, propertyOperators );
    }

    private static void parseFilterString( String s, List<String[]> propertyNames, List<Object[]> propertyValues,
            List<String[]> propertyOperators ) {
        String[] parts = s.split( "\\s+" );

        List<String> propertyNamesDisjunction = new LinkedList<>();
        List<String> propertyOperatorsDisjunction = new LinkedList<>();
        List<Object> propertyValuesDisjunction = new LinkedList<>();
        if ( parts.length < 3 ) {
            throw new IllegalArgumentException( ERROR_PARTS_TOO_SHORT );
        }

        for ( int i = 0; i < parts.length; ) {
            propertyNamesDisjunction.add( parts[i++] );
            propertyOperatorsDisjunction.add( parseObjectFilterOperator( parts[i++] ) );
            propertyValuesDisjunction.add( parts[i++] );

             if ( i == parts.length || parts[i].toLowerCase().equals( "and" ) ) {
                // We either reached an 'AND', or the end of the string.
                // Add the current disjunction.
                propertyNames.add( propertyNamesDisjunction.toArray( new String[propertyNamesDisjunction.size()] ) );
                propertyOperators
                        .add( propertyOperatorsDisjunction.toArray( new String[propertyOperatorsDisjunction.size()] ) );
                propertyValues.add( propertyValuesDisjunction.toArray( new Object[propertyValuesDisjunction.size()] ) );
                // Start new disjunction lists
                propertyNamesDisjunction = new LinkedList<>();
                propertyOperatorsDisjunction = new LinkedList<>();
                propertyValuesDisjunction = new LinkedList<>();
            } else if ( parts[i].toLowerCase().equals( "or" ) ) {
                 // Skip this part and continue the disjunction
                 i++;
             }
        }
    }

    private static String parseObjectFilterOperator( String s ) {
        if ( s.equals( ObjectFilter.is ) || s.equals( ObjectFilter.isNot ) || s.equals( ObjectFilter.like ) || s
                .equals( ObjectFilter.greaterThan ) || s.equals( ObjectFilter.lessThan ) || s
                .equals( ObjectFilter.greaterOrEq ) || s.equals( ObjectFilter.lessOrEq ) || s
                .equals( ObjectFilter.in ) ) {
            return s;
        }
        throw new IllegalArgumentException( String.format( ERROR_ILLEGAL_OPERATOR, s ) );
    }

}
