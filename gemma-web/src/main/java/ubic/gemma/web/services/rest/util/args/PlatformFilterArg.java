package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.web.services.rest.util.GemmaApiException;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by tesarst on 17/07/17.
 * Filter argument for ExpressionExperiment filtering.
 */
public class PlatformFilterArg extends FilterArg {

    private static final String ERROR_INVALID_PROPERTY = "Given filter property does not exist in platforms.";
    private static final String ERROR_PARSE_ERROR = "Filter argument parsing error.";

    private PlatformFilterArg( List<String[]> propertyNames, List<String[]> propertyValues,
            List<String[]> propertyOperators, List<Class[]> propertyTypes ) {
        super( propertyNames, propertyValues, propertyOperators, propertyTypes, ObjectFilter.DAO_AD_ALIAS );
    }

    private PlatformFilterArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request filter string.
     * @return an instance of DatasetFilterArg representing the filtering options in the given string.
     * @throws GemmaApiException if the given string is not a well formed filter argument (E.g. is not CNF, or at least
     *                           one of the properties specified in the string does not exist in Array Designs.
     */
    @SuppressWarnings("unused")
    public static PlatformFilterArg valueOf( final String s ) {
        if( Strings.isNullOrEmpty(s)) return new PlatformFilterArg( null, null, null, null );

        List<String[]> propertyNames = new LinkedList<>();
        List<String[]> propertyValues = new LinkedList<>();
        List<String[]> propertyOperators = new LinkedList<>();
        List<Class[]> propertyTypes;

        try {
            parseFilterString( s, propertyNames, propertyValues, propertyOperators );
            propertyTypes = getPropertiesTypes( propertyNames, ArrayDesign.class );
        } catch ( IllegalArgumentException e ) {
            return new PlatformFilterArg( ERROR_PARSE_ERROR, e );
        } catch ( NoSuchFieldException e ) {
            return new PlatformFilterArg( ERROR_INVALID_PROPERTY, e );
        }
        return new PlatformFilterArg( propertyNames, propertyValues, propertyOperators, propertyTypes );
    }

}
