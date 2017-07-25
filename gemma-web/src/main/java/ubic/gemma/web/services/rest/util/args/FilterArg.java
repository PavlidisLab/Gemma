package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.web.services.rest.util.GemmaApiException;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by tesarst on 17/07/17.
 * An abstract filter argument implementing methods common for all filter arguments
 */
public abstract class FilterArg extends MalformableArg {

    private static final String ERROR_PARTS_TOO_SHORT = "Provided filter string does not contain at least one of property-operator-value sets.";
    private static final String ERROR_ILLEGAL_OPERATOR = "Illegal operator: %s is not an accepted operator.";

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

    /**
     * Parses the input string into lists of logical disjunctions, that together form a conjunction (CNF).
     * @param s the string to be parsed.
     * @param propertyNames list to be populated with property name arrays for each disjunction.
     * @param propertyValues list to be populated with property value arrays for each disjunction.
     * @param propertyOperators list to be populated with operator arrays for each disjunction.
     */
    static void parseFilterString( String s, List<String[]> propertyNames, List<Object[]> propertyValues,
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
                i++;
            } else if ( parts[i].toLowerCase().equals( "or" ) ) {
                // Skip this part and continue the disjunction
                i++;
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
        System.out.println("checking class "+cls+" for property "+parts[0]);
        Field field = checkAllFields(cls, parts[0] );
        Class<?> subCls = field.getType();
        if ( parts.length > 1 ) {
            checkProperty( parts[1], subCls );
        }
    }

    private static Field checkAllFields(Class<?> cls, String field) throws NoSuchFieldException {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            fields.addAll( Arrays.asList(c.getDeclaredFields()));
        }

        for(Field f : fields){
            if(f.getName().equals( field )) return f;
        }
        throw new NoSuchFieldException( "Class "+cls+" does not contain field '"+field+"'." );
    }

    /**
     * Parses the string into a valid operator.
     * @param s the string to be parsed.
     * @return a string that is a valid operator.
     * @throws IllegalArgumentException if the
     */
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

