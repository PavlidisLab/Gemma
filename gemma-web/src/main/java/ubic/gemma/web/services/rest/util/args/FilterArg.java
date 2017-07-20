package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.web.services.rest.util.GemmaApiException;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tesarst on 17/07/17.
 * An abstract filter argument implementing methods common for all filter arguments
 */
public abstract class FilterArg extends MalformableArg {

    private List<String[]> propertyNames;
    private List<Object[]> propertyValues;
    private List<String[]> propertyOperators;
    private String objectAlias;

    /**
     * @param propertyNames     names of properties to filter by. <br/>
     *                          Elements in each array will be in a disjunction (OR) with each other.<br/>
     *                          Arrays will then be in a conjunction (AND) with each other.<br/>
     * @param propertyValues    values to compare the given property names to.
     * @param propertyOperators the operation used for comparison of the given value and the value of the object.
     *                          The propertyValues will be the right operand of each given operator.
     *                          <br/>
     *                          E.g: <code>object.propertyName[0] isNot propertyValues[0];</code><br/>
     * @param objectAlias       See {@link ObjectFilter} constructors objectAlias parameter.
     */
    FilterArg( List<String[]> propertyNames, List<Object[]> propertyValues, List<String[]> propertyOperators,
            String objectAlias ) {
        super();
        this.propertyNames = propertyNames;
        this.propertyValues = propertyValues;
        this.propertyOperators = propertyOperators;
        this.objectAlias = objectAlias;
    }

    FilterArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    /**
     * Tries to access the give properties on the given class to check if they exist. Does not check whether these properties
     * are a mapped hibernate property.
     *
     * @param propertyNames the names to be checked.
     * @param cls           the class to check the properties for.
     * @throws NoSuchFieldException if one of the properties did not exist in the given class.
     */
    static void checkProperties( List<String[]> propertyNames, Class cls ) throws NoSuchFieldException {
        for ( String[] array : propertyNames ) {
            for ( String property : array ) {
                checkProperty( property, cls );
            }
        }
    }

    /**
     * Checks if property of given name exists in the given class. If the given string specifies
     * nested properties (E.g. curationDetails.troubled), only the substring before the first dot is evaluated and the
     * rest of the string is processed in new recursion iteration.
     *
     * @param property the property to check for. If the string contains dot characters ('.'), only the part
     *                 before the first dot will be evaluated. Substring after the dot will be checked against the
     *                 type of the field retrieved from the substring before the dot.
     * @param cls      the class to check the property on.
     * @throws NoSuchFieldException if the property did not exist on the given class, or one of the recursive iterations
     *                              thrown this exception.
     */
    private static void checkProperty( String property, Class cls ) throws NoSuchFieldException {
        String[] parts = property.split( "\\.", 2 );
        Field field = cls.getDeclaredField( parts[0] );
        Class<?> subCls = field.getType();
        if ( parts.length > 1 ) {
            checkProperty( parts[1], subCls );
        }
    }

    /**
     * Creates an ArrayList of Object Filter arrays, that can be used as a filter parameter for service value object
     * retrieval.
     *
     * @return an ArrayList of Object Filter arrays, each array represents a disjunction (OR) of filters. Arrays
     * then represent a conjunction (AND) with other arrays in the list.
     */
    public ArrayList<ObjectFilter[]> getObjectFilters() {
        this.checkMalformed();
        if ( propertyNames == null || propertyNames.isEmpty() )
            return null;
        ArrayList<ObjectFilter[]> filterList = new ArrayList<>( propertyNames.size() );

        for ( int i = 0; i < propertyNames.size(); i++ ) {
            try {
                String[] properties = propertyNames.get( i );
                Object[] values = propertyValues.get( i );
                String[] operators = propertyOperators.get( i );

                ObjectFilter[] filterArray = new ObjectFilter[properties.length];
                for ( int j = 0; j < properties.length; j++ ) {
                    String property = properties[j];
                    Object value = values[j];
                    String operator = operators[j];
                    filterArray[j] = new ObjectFilter( property, value, operator, objectAlias );
                }
                filterList.add( filterArray );

            } catch ( IndexOutOfBoundsException e ) {
                throw new GemmaApiException( new WellComposedErrorBody( Response.Status.BAD_REQUEST,
                        "Filter query problem: Amount of properties, operators and values does not match" ) );
            }
        }

        return filterList;
    }

}

