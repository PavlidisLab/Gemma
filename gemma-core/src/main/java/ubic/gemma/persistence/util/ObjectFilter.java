package ubic.gemma.persistence.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Created by tesarst on 14/07/17.
 * Provides necessary information to filter a database query by a value of a specific object property.
 */
public class ObjectFilter {

    public static String EEDAO_EE_ALIAS = "ee";
    public static String EEDAO_AD_ALIAS = "AD";

    public static String is = "=";
    public static String isNot = "!=";
    public static String like = "like";
    public static String lessThan = "<";
    public static String greaterThan = ">";
    public static String lessOrEq = "<=";
    public static String greaterOrEq = ">=";
    public static String in = "in";

    private String propertyName;
    private Object requiredValue;
    private String operator;
    private String objectAlias;

    /**
     * Creates a new ObjectFilter.
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
        this.requiredValue = requiredValue;
        this.operator = operator;
        this.objectAlias = objectAlias;

        if ( ( requiredValue == null && ( // Check null for disallowed operators
                operator.equals( greaterThan ) || // gt
                        operator.equals( lessThan ) ) // lt
        ) || ( operator.equals( in ) &&  // Check 'in' conditions
                ( requiredValue == null || !(requiredValue instanceof Collection<?>)) ) // Check value is iterable
                ) {
            throw new IllegalArgumentException(
                    "requiredValue for operator " + operator + " has to be an Iterable Object" );
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
}
