package ubic.gemma.persistence.util;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by tesarst on 14/07/17.
 * Provides necessary information to filter a database query by a value of a specific object property.
 */
public class ObjectFilter {
    // FIXME these might be better placed in their respective DAOs, since they are used mostly there.
    public static final String DAO_EE_ALIAS = "ee";
    public static final String DAO_AD_ALIAS = "ad";
    public static final String DAO_TAXON_ALIAS = "taxon";
    public static final String DAO_PROBE_ALIAS = "probe";
    public static final String DAO_GENE_ALIAS = "gene";
    public static final String DAO_GEEQ_ALIAS = "geeq";

    public static final String is = "=";
    public static final String isNot = "!=";
    public static final String like = "like";
    public static final String lessThan = "<";
    public static final String greaterThan = ">";
    public static final String lessOrEq = "<=";
    public static final String greaterOrEq = ">=";
    public static final String in = "in";

    private final String propertyName;
    private final Object requiredValue;
    private final String operator;
    private final String objectAlias;
    private final Class propertyType;

    /**
     * Creates a new ObjectFilter with a value parsed from a String into a given propertyType.
     *
     * @param propertyType the type of the property that will be checked.
     * @throws ParseException in case the given requiredValue could not be parsed into the propertyType.
     * @see ObjectFilter#ObjectFilter(String, Object, String, String)
     */
    public ObjectFilter( String propertyName, Class propertyType, Object requiredValue, String operator,
            String objectAlias ) throws ParseException {
        this.propertyName = propertyName;
        this.propertyType = propertyType;
        this.requiredValue = this.convertToParamType( requiredValue, propertyType );
        this.operator = operator;
        this.objectAlias = objectAlias;

        this.checkTypeCorrect();
    }

    /**
     * Creates a new ObjectFilter with a value of type that the given requiredValue object is.
     *
     * @param propertyName  the name of a property that will be checked.
     * @param requiredValue the value that the property will be checked for. Null objects are not allowed for operators
     *                      "lessThan", "greaterThan" and "in".
     * @param operator      the operator that will be used to compare the value of the object. The requiredValue will
     *                      be the right operand of the given operator.
     *                      Demonstration in pseudo-code: if( object.value lessThan requiredValue) return object.
     *                      The {@link ObjectFilter#in} operator means that the given requiredValue is expected to be a
     *                      {@link java.util.Collection}, and the checked property has to be equal to at least one of the
     *                      values contained within that <i>List</i>.
     * @param objectAlias   The alias of the object in the query. See the DAO for the filtered object to see what
     *                      the alias in the query is. E.g for {@link ubic.gemma.model.expression.experiment.ExpressionExperiment}
     *                      the alias is 'ee', as seen in
     *                      {@link ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDaoImpl#getLoadValueObjectsQueryString(ArrayList, String, boolean)}.
     * @throws IllegalArgumentException if the given operator is the "in" operator but the
     *                                  given requiredValue is not an instance of Iterable.
     */
    public ObjectFilter( String propertyName, Object requiredValue, String operator, String objectAlias ) {
        this.propertyName = propertyName;
        this.propertyType = requiredValue.getClass();
        this.requiredValue = requiredValue;
        this.operator = operator;
        this.objectAlias = objectAlias;
        this.checkTypeCorrect();
    }

    /**
     * @param filter the filter to create the ArrayList with
     * @return an instance of ArrayList&lt;ObjectFilter[]&gt; with only the given filter as the first element of the
     * only array in the list.
     */
    public static ArrayList<ObjectFilter[]> singleFilter( ObjectFilter filter ) {
        ArrayList<ObjectFilter[]> filters = new ArrayList<>();
        filters.add( new ObjectFilter[] { filter } );
        return filters;
    }

    /**
     * Converts the given value to be of the given property type. For primitive number types, the wrapper class is used.
     *
     * @param requiredValue the Object to be converted into the desired type.
     * @param propertyType  the type that the given value should be converted to.
     * @return and Object of requested type, containing the given value converted to the new type.
     * @throws ParseException when there was a problem converting the given value into the requested type.
     */
    private Object convertToParamType( Object requiredValue, Class propertyType ) throws ParseException {
        if ( Iterable.class.isAssignableFrom( requiredValue.getClass() ) ) {
            // We got a collection
            @SuppressWarnings("unchecked") // Assuming default is string
                    Collection<String> reqCol = ( Collection<String> ) requiredValue;
            if ( String.class.isAssignableFrom( propertyType ) ) {
                return requiredValue;
            } else if ( Number.class.isAssignableFrom( propertyType ) ) {
                Collection<Number> newCol = new ArrayList<>( reqCol.size() );
                for ( String s : reqCol ) {
                    newCol.add( NumberFormat.getInstance().parse( s ) );
                }
                return newCol;
            } else if ( Boolean.class.isAssignableFrom( propertyType ) ) {
                Collection<Boolean> newCol = new ArrayList<>( reqCol.size() );
                for ( String s : reqCol ) {
                    newCol.add( Boolean.parseBoolean( s ) );
                }
                return newCol;
            }
        } else if ( String.class.isAssignableFrom( propertyType ) ) {
            return requiredValue;
        } else if ( Boolean.class.isAssignableFrom( propertyType ) ) {
            return Boolean.parseBoolean( ( String ) requiredValue );
        } else if ( Integer.class.isAssignableFrom( propertyType ) || int.class.isAssignableFrom( propertyType ) ) {
            return Integer.valueOf( ( String ) requiredValue );
        } else if ( Double.class.isAssignableFrom( propertyType ) || double.class.isAssignableFrom( propertyType ) ) {
            return Double.valueOf( ( String ) requiredValue );
        } else if ( Long.class.isAssignableFrom( propertyType ) || long.class.isAssignableFrom( propertyType ) ) {
            return Long.valueOf( ( String ) requiredValue );
        } else if ( Short.class.isAssignableFrom( propertyType ) || short.class.isAssignableFrom( propertyType ) ) {
            return Short.valueOf( ( String ) requiredValue );
        } else if ( Byte.class.isAssignableFrom( propertyType ) || byte.class.isAssignableFrom( propertyType ) ) {
            return Byte.valueOf( ( String ) requiredValue );
        }
        throw new IllegalArgumentException( "Property type not supported (" + propertyType + ")." );
    }

    private void checkTypeCorrect() {
        if ( requiredValue == null && ( // Check null for disallowed operators
                operator.equals( ObjectFilter.greaterThan ) || // gt
                        operator.equals( ObjectFilter.lessThan ) ) // lt
                ) {
            throw new IllegalArgumentException( "requiredValue for operator " + operator + " can not be null." );
        } else if ( operator.equals( ObjectFilter.in ) ) { // Check 'in' conditions
            if ( requiredValue == null || !( requiredValue instanceof Collection<?> ) ) { // Check value is iterable
                throw new IllegalArgumentException(
                        "requiredValue for operator " + operator + " has to be an Iterable Object." );
            }
        } else if ( propertyType != null && !( requiredValue == null || requiredValue.getClass()
                .isAssignableFrom( propertyType ) || ( this.isSameNumberType( requiredValue.getClass(),
                propertyType ) ) ) // Check the type matches, special case for numbers
                ) {
            throw new IllegalArgumentException(
                    "requiredValue for property " + propertyName + " has to be assignable from " + propertyType
                            .getName() + " or null, but the requiredValue class is  " + requiredValue.getClass()
                            .getName() + "." );
        } else if ( operator.equals( ObjectFilter.like ) && ( propertyType == null || !String.class
                .isAssignableFrom( propertyType ) ) ) {
            throw new IllegalArgumentException(
                    "requiredValue for operator " + operator + " has to be a non null String." );
        }
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Object getRequiredValue() {
        if ( this.operator.equals( ObjectFilter.like ) )
            return "%" + requiredValue + "%";
        return requiredValue;
    }

    public String getOperator() {
        return operator;
    }

    public String getObjectAlias() {
        return objectAlias;
    }

    public Class getPropertyType() {
        return propertyType;
    }

    /**
     * Checks whether the two given classes are representing the same number type, regardless of whether it is a Wrapper class or a primitive type.
     * The types checked are double, integer, float, long, short and byte.
     *
     * @param cls1 the first class to compare
     * @param cls2 the second class to compare
     * @return true, if the two given classes represent the same number type.
     */
    private boolean isSameNumberType( Class cls1, Class cls2 ) {
        return ( Double.class.isAssignableFrom( cls1 ) || double.class.isAssignableFrom( cls1 ) ) && (
                Double.class.isAssignableFrom( cls2 ) || double.class.isAssignableFrom( cls2 ) )  // double
                || ( Integer.class.isAssignableFrom( cls1 ) || int.class.isAssignableFrom( cls1 ) ) && (
                Integer.class.isAssignableFrom( cls2 ) || int.class.isAssignableFrom( cls2 ) )  // integer
                || ( Float.class.isAssignableFrom( cls1 ) || float.class.isAssignableFrom( cls1 ) ) && (
                Float.class.isAssignableFrom( cls2 ) || float.class.isAssignableFrom( cls2 ) )  // float
                || ( Long.class.isAssignableFrom( cls1 ) || long.class.isAssignableFrom( cls1 ) ) && (
                Long.class.isAssignableFrom( cls2 ) || long.class.isAssignableFrom( cls2 ) )  // long
                || ( Short.class.isAssignableFrom( cls1 ) || short.class.isAssignableFrom( cls1 ) ) && (
                Short.class.isAssignableFrom( cls2 ) || short.class.isAssignableFrom( cls2 ) )  // short
                || ( Byte.class.isAssignableFrom( cls1 ) || byte.class.isAssignableFrom( cls1 ) ) && (
                Byte.class.isAssignableFrom( cls2 ) || byte.class.isAssignableFrom( cls2 ) );  // byte
    }
}
