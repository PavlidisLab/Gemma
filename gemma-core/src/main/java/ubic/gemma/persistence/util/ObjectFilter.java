/*
 * The gemma-core project
 * 
 * Copyright (c) 2019 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.persistence.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by tesarst on 14/07/17.
 * Provides necessary information to filter a database query by a value of a specific object property.
 */
public class ObjectFilter {
    public static final String DAO_EE_ALIAS = "ee";
    public static final String DAO_AD_ALIAS = "ad";
    public static final String DAO_TAXON_ALIAS = "taxon";
    public static final String DAO_PROBE_ALIAS = "probe";
    public static final String DAO_GENE_ALIAS = "gene";
    public static final String DAO_GEEQ_ALIAS = "geeq";
    public static final String DAO_CHARACTERISTIC_ALIAS = "ch";
    public static final String DAO_BIOASSAY_ALIAS = "ba";

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
    private final Class<?> propertyType;

    /**
     * Creates a new ObjectFilter with a value parsed from a String into a given propertyType.
     *
     * @param propertyType  the type of the property that will be checked.
     * @param objectAlias   alias of the relevant object to use in the final hql query
     * @param operator      operator the operator for this filter
     * @param propertyName  property name
     * @param requiredValue required value
     * @see                 ObjectFilter#ObjectFilter(String, Object, String, String)
     */
    public ObjectFilter( String propertyName, Class<?> propertyType, Object requiredValue, String operator,
            String objectAlias ) {
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
     * @param  propertyName             the name of a property that will be checked.
     * @param  requiredValue            the value that the property will be checked for. Null objects are not allowed
     *                                  for operators
     *                                  "lessThan", "greaterThan" and "in".
     * @param  operator                 the operator that will be used to compare the value of the object. The
     *                                  requiredValue will
     *                                  be the right operand of the given operator.
     *                                  Demonstration in pseudo-code: if( object.value lessThan requiredValue) return
     *                                  object.
     *                                  The {@link ObjectFilter#in} operator means that the given requiredValue is
     *                                  expected to be a
     *                                  {@link java.util.Collection}, and the checked property has to be equal to at
     *                                  least one of the
     *                                  values contained within that <i>List</i>.
     * @param  objectAlias              The alias of the object in the query. See the DAO for the filtered object to see
     *                                  what
     *                                  the alias in the query is. E.g for
     *                                  {@link ubic.gemma.model.expression.experiment.ExpressionExperiment}
     *                                  the alias is 'ee', as seen in
     *                                  {@link ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDaoImpl#getLoadValueObjectsQueryString(List, String, boolean)}.
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
     * @param  filter the filter to create the ArrayList with
     * @return        an instance of ArrayList&lt;ObjectFilter[]&gt; with only the given filter as the first element of
     *                the
     *                only array in the list.
     */
    public static List<ObjectFilter[]> singleFilter( ObjectFilter filter ) {
        List<ObjectFilter[]> filters = new ArrayList<>();
        filters.add( new ObjectFilter[] { filter } );
        return filters;
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

    /**
     * Converts the given value to be of the given property type. For primitive number types, the wrapper class is used.
     *
     * @param  rv the Object to be converted into the desired type.
     * @param  pt  the type that the given value should be converted to.
     * @return               and Object of requested type, containing the given value converted to the new type.
     */
    private Object convertToParamType( Object rv, Class<?> pt ) {
        if ( Iterable.class.isAssignableFrom( rv.getClass() ) ) {
            // We got a collection
            @SuppressWarnings("unchecked") // Assuming default is string
            Collection<String> reqCol = ( Collection<String> ) rv;
            if ( String.class.isAssignableFrom( pt ) ) {
                return rv;
            }
            Collection<Object> newCol = new ArrayList<>( reqCol.size() );
            for ( String s : reqCol ) {
                newCol.add( convertItem( s, pt ) );
            }
            return newCol;

        }
        return convertItem( rv, pt );

    }

    /**
     * 
     * @param  rv required value
     * @param  pt property type
     * @return    converted object
     */
    private Object convertItem( Object rv, Class<?> pt ) {
        // Assuming default is string
        if ( String.class.isAssignableFrom( pt ) ) {
            return requiredValue;
        } else if ( Boolean.class.isAssignableFrom( pt ) || boolean.class.isAssignableFrom( pt ) ) {
            return Boolean.parseBoolean( ( String ) rv );
        } else if ( Integer.class.isAssignableFrom( pt ) || int.class.isAssignableFrom( pt ) ) {
            return Integer.valueOf( ( String ) rv );
        } else if ( Double.class.isAssignableFrom( pt ) || double.class.isAssignableFrom( pt ) ) {
            return Double.valueOf( ( String ) rv );
        } else if ( Long.class.isAssignableFrom( pt ) || long.class.isAssignableFrom( pt ) ) {
            return Long.valueOf( ( String ) rv );
        } else if ( Short.class.isAssignableFrom( pt ) || short.class.isAssignableFrom( pt ) ) {
            return Short.valueOf( ( String ) rv );
        } else if ( Byte.class.isAssignableFrom( pt ) || byte.class.isAssignableFrom( pt ) ) {
            return Byte.valueOf( ( String ) rv );
        }
        throw new IllegalArgumentException( "Property type not supported (" + pt + ")." );
    }

    private void checkTypeCorrect() {
        if ( requiredValue == null && ( // Check null for disallowed operators
        operator.equals( ObjectFilter.greaterThan ) || // gt
                operator.equals( ObjectFilter.lessThan ) ) // lt
        ) {
            throw new IllegalArgumentException( "requiredValue for operator " + operator + " can not be null." );
        } else if ( operator.equals( ObjectFilter.in ) ) { // Check 'in' conditions
            if ( !( requiredValue instanceof Collection<?> ) ) { // Check value is iterable
                throw new IllegalArgumentException(
                        "requiredValue for operator " + operator + " has to be an Iterable Object." );
            }
        } else if ( propertyType != null && !( requiredValue == null || requiredValue.getClass()
                .isAssignableFrom( propertyType )
                || ( this
                        .isSameOrWrapperType( requiredValue.getClass(), propertyType ) ) ) // Check the type matches
        ) {
            throw new IllegalArgumentException(
                    "requiredValue for property " + propertyName + " has to be assignable from " + propertyType
                            .getName() + " or null, but the requiredValue class is  "
                            + requiredValue.getClass()
                                    .getName()
                            + "." );
        } else if ( operator.equals( ObjectFilter.like ) && ( propertyType == null || !String.class
                .isAssignableFrom( propertyType ) ) ) {
            throw new IllegalArgumentException(
                    "requiredValue for operator " + operator + " has to be a non null String." );
        }
    }

    /**
     * Checks whether the two given classes are representing the same data type, regardless of whether it is a Wrapper
     * class or a primitive type.
     * The types checked are double, integer, float, long, short, boolean and byte.
     *
     * @param  cls1 the first class to compare
     * @param  cls2 the second class to compare
     * @return      true, if the two given classes represent the same number type.
     */
    private boolean isSameOrWrapperType( Class<?> cls1, Class<?> cls2 ) {
        return ( ( Double.class.isAssignableFrom( cls1 ) || double.class.isAssignableFrom( cls1 ) )
                && ( Double.class.isAssignableFrom( cls2 ) || double.class.isAssignableFrom( cls2 ) ) ) // double
                || ( ( Integer.class.isAssignableFrom( cls1 ) || int.class.isAssignableFrom( cls1 ) )
                        && ( Integer.class.isAssignableFrom( cls2 ) || int.class.isAssignableFrom( cls2 ) ) ) // integer
                || ( ( Float.class.isAssignableFrom( cls1 ) || float.class.isAssignableFrom( cls1 ) )
                        && ( Float.class.isAssignableFrom( cls2 ) || float.class.isAssignableFrom( cls2 ) ) ) // float
                || ( ( Long.class.isAssignableFrom( cls1 ) || long.class.isAssignableFrom( cls1 ) )
                        && ( Long.class.isAssignableFrom( cls2 ) || long.class.isAssignableFrom( cls2 ) ) ) // long
                || ( ( Short.class.isAssignableFrom( cls1 ) || short.class.isAssignableFrom( cls1 ) )
                        && ( Short.class.isAssignableFrom( cls2 ) || short.class.isAssignableFrom( cls2 ) ) ) // short
                || ( ( Byte.class.isAssignableFrom( cls1 ) || byte.class.isAssignableFrom( cls1 ) )
                        && ( Byte.class.isAssignableFrom( cls2 ) || byte.class.isAssignableFrom( cls2 ) ) ) // byte
                || ( ( Boolean.class.isAssignableFrom( cls1 ) || boolean.class.isAssignableFrom( cls1 ) )
                        && ( Boolean.class.isAssignableFrom( cls2 ) || boolean.class.isAssignableFrom( cls2 ) ) ); // boolean
    }
}
