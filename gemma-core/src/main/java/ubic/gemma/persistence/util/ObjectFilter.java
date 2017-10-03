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
        this.requiredValue = convertToParamType( requiredValue, propertyType );
        this.operator = operator;
        this.objectAlias = objectAlias;

        checkTypeCorrect();
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
        checkTypeCorrect();
    }

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
        } else if ( Number.class.isAssignableFrom( propertyType ) ) {
            return NumberFormat.getInstance().parse( ( String ) requiredValue );
        } else if ( Boolean.class.isAssignableFrom( propertyType ) ) {
            return Boolean.parseBoolean( ( String ) requiredValue );
        }
        throw new IllegalArgumentException( "Property type not supported (" + propertyType + ")." );
    }

    private void checkTypeCorrect() {
        if ( requiredValue == null && ( // Check null for disallowed operators
                operator.equals( greaterThan ) || // gt
                        operator.equals( lessThan ) ) // lt
                ) {
            throw new IllegalArgumentException( "requiredValue for operator " + operator + " can not be null." );
        } else if ( operator.equals( in ) ){ // Check 'in' conditions
            if( requiredValue == null || !( requiredValue instanceof Collection<?> ) ) { // Check value is iterable
                throw new IllegalArgumentException( "requiredValue for operator " + operator + " has to be an Iterable Object." );
            }
        } else if ( propertyType != null && !( requiredValue == null || requiredValue.getClass()
                .isAssignableFrom( propertyType ) ) ) { // Check the type matches
            throw new IllegalArgumentException(
                    "requiredValue for property " + propertyName + " has to be assignable from " + propertyType
                            .getName() + " or null." );
        } else if ( operator.equals( like ) && ( propertyType == null || !String.class
                .isAssignableFrom( propertyType ) ) ) {
            throw new IllegalArgumentException(
                    "requiredValue for operator " + operator + " has to be a non null String." );
        }
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Object getRequiredValue() {
        if ( this.operator.equals( like ) )
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
}
