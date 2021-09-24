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

import lombok.ToString;
import ubic.gemma.persistence.service.ObjectFilterException;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by tesarst on 14/07/17.
 * Provides necessary information to filter a database query by a value of a specific object property.
 */
@ToString
public class ObjectFilter {

    public static final String DAO_EE_ALIAS = "ee";
    public static final String DAO_AD_ALIAS = "ad";
    public static final String DAO_TAXON_ALIAS = "taxon";
    public static final String DAO_PROBE_ALIAS = "probe";
    public static final String DAO_GENE_ALIAS = "gene";
    public static final String DAO_CHARACTERISTIC_ALIAS = "ch";
    public static final String DAO_BIOASSAY_ALIAS = "ba";
    public static final String DAO_DATABASE_ENTRY_ALIAS = "accession";

    public enum Operator {
        is( "=" ),
        isNot( "!=" ),
        like( "like" ),
        lessThan( "<" ),
        greaterThan( ">" ),
        lessOrEq( "<=" ),
        greaterOrEq( ">=" ),
        in( "in" );

        /**
         * Token used when parsing object filter input.
         */
        private final String token;

        Operator( String operator ) {
            this.token = operator;
        }

        public String getToken() {
            return token;
        }

        /**
         * SQL token representing this operator.
         */
        public String getSqlToken() {
            return token;
        }
    }

    private final String objectAlias;
    private final String propertyName;
    private final Class<?> propertyType;
    private final Operator operator;
    private final Object requiredValue;

    public ObjectFilter( String objectAlias, String propertyName, Class<?> propertyType, Operator operator, Object requiredValue ) throws IllegalArgumentException {
        this.objectAlias = objectAlias;
        this.propertyName = propertyName;
        this.propertyType = propertyType;
        this.operator = operator;
        this.requiredValue = requiredValue;
        this.checkTypeCorrect( propertyType );
    }

    /**
     * Creates a new ObjectFilter with a value parsed from a String into a given propertyType.
     *
     * @param objectAlias   alias of the relevant object to use in the final HQL query
     * @param propertyName  property name of the object
     * @param propertyType  the type of the property that will be checked, you can use the {@link #getPropertyType(String, Class)}
     *                      utility to obtain that type conveniently
     * @param operator      operator for this filter, see {@link Operator} for more details about available operations
     * @param requiredValue required value
     */
    public ObjectFilter( String objectAlias, String propertyName, Class<?> propertyType, Operator operator, String requiredValue ) throws ObjectFilterException {
        this( objectAlias, propertyName, propertyType, operator, parseRequiredValue( requiredValue, propertyType ) );
    }

    /**
     * Creates a new ObjectFilter with a value of type that the given requiredValue object is.
     *
     * @param  objectAlias              The alias of the object in the query. See the DAO for the filtered object to see
     *                                  what
     *                                  the alias in the query is. E.g for
     *                                  {@link ubic.gemma.model.expression.experiment.ExpressionExperiment}
     *                                  the alias is 'ee'
     * @param  propertyName             the name of a property that will be checked.
     * @param  operator                 the operator that will be used to compare the value of the object. The
     *                                  requiredValue will
     *                                  be the right operand of the given operator.
     *                                  Demonstration in pseudo-code: if( object.value lessThan requiredValue) return
     *                                  object.
     *                                  The {@link Operator#in} operator means that the given requiredValue is
     *                                  expected to be a
     *                                  {@link Collection}, and the checked property has to be equal to at
     *                                  least one of the
     *                                  values contained within that <i>List</i>.
     * @param  requiredValue            the value that the property will be checked for. Null objects are not allowed
     *                                  for operators "lessThan", "greaterThan" and "in".
     * @throws IllegalArgumentException if the given operator is the "in" operator but the
     *                                  given requiredValue is not an instance of Iterable.
     */
    public ObjectFilter( String objectAlias, String propertyName, Class<?> propertyType, Operator operator, Collection<String> requiredValues ) throws ObjectFilterException {
        this( objectAlias, propertyName, propertyType, operator, parseRequiredValues( requiredValues, propertyType ) );
    }

    /**
     * @param  filter the filter to create the ArrayList with
     * @return an instance of ArrayList&lt;ObjectFilter[]&gt; with only the given filter as the first element of
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
        if ( this.operator.equals( ObjectFilter.Operator.like ) )
            return "%" + requiredValue + "%";
        return requiredValue;
    }

    public Operator getOperator() {
        return operator;
    }

    public String getObjectAlias() {
        return objectAlias;
    }

    private void checkTypeCorrect( Class<?> propertyType ) throws IllegalArgumentException {
        if ( requiredValue == null && ( // Check null for disallowed operators
                operator.equals( ObjectFilter.Operator.greaterThan ) || // gt
                        operator.equals( ObjectFilter.Operator.lessThan ) ) // lt
        ) {
            throw new IllegalArgumentException( "requiredValue for operator " + operator + " can not be null." );
        } else if ( operator.equals( ObjectFilter.Operator.in ) ) { // Check 'in' conditions
            if ( !( requiredValue instanceof Collection<?> ) ) { // Check value is iterable
                throw new IllegalArgumentException(
                        "requiredValue for operator " + operator + " has to be an Collection." );
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
        } else if ( operator.equals( ObjectFilter.Operator.like ) && ( propertyType == null || !String.class
                .isAssignableFrom( propertyType ) ) ) {
            throw new IllegalArgumentException(
                    "requiredValue for operator " + operator + " has to be a non null String." );
        }
    }

    /**
     * Converts the given value to be of the given property type. For primitive number types, the wrapper class is used.
     *
     * @param  rv the Object to be converted into the desired type.
     * @param  pt  the type that the given value should be converted to.
     * @return and Object of requested type, containing the given value converted to the new type.
     */
    private static Object parseRequiredValue( String rv, Class<?> pt ) throws ObjectFilterException {
        try {
            if ( isCollection( rv ) ) {
                // convert individual elements
                return parseCollection( rv ).stream()
                        .map( item -> parseItem( item, pt ) )
                        .collect( Collectors.toList() );
            } else {
                return parseItem( rv, pt );
            }
        } catch ( IllegalArgumentException e ) {
            throw new ObjectFilterException( "Could not parse required value '" + rv + "'.", e );
        }
    }

    private static Object parseRequiredValues( Collection<String> requiredValues, Class<?> pt ) throws ObjectFilterException {
        try {
            return requiredValues.stream()
                    .map( item -> parseItem( item, pt ) )
                    .collect( Collectors.toList() );
        } catch ( IllegalArgumentException e ) {
            throw new ObjectFilterException( "Could not parse one or more required values.", e );
        }
    }

    private static boolean isCollection( String value ) {
        return value.trim().matches( "^\\((.+,)*.+\\)$" );
    }

    /**
     * Tries to parse the given string value into a collection of strings.
     * @param value the value to be parsed into a collection of strings. This should be a bracketed comma separated list
     *              of strings.
     * @return a collection of strings.
     */
    private static Collection<String> parseCollection( String value ) {
        return Arrays.asList( value
                .trim()
                .substring( 1, value.length() - 1 ) // these are the parenthesis
                .split( "\\s*,\\s*" ) );
    }

    /**
     *
     * @param  rv required value
     * @param  pt property type
     * @return converted object
     * @throws IllegalArgumentException if the type is not supported
     */
    private static Object parseItem( String rv, Class<?> pt ) throws IllegalArgumentException {
        if ( String.class.isAssignableFrom( pt ) )
            return rv;

        if ( Boolean.class.isAssignableFrom( pt ) || boolean.class.isAssignableFrom( pt ) )
            return Boolean.parseBoolean( rv );

        if ( Double.class.isAssignableFrom( pt ) || double.class.isAssignableFrom( pt ) )
            return Double.valueOf( rv );

        if ( Float.class.isAssignableFrom( pt ) || float.class.isAssignableFrom( pt ) )
            return Float.valueOf( rv );

        if ( Long.class.isAssignableFrom( pt ) || long.class.isAssignableFrom( pt ) )
            return Long.valueOf( rv );

        if ( Integer.class.isAssignableFrom( pt ) || int.class.isAssignableFrom( pt ) )
            return Integer.valueOf( rv );

        if ( Short.class.isAssignableFrom( pt ) || short.class.isAssignableFrom( pt ) )
            return Short.valueOf( rv );

        if ( Byte.class.isAssignableFrom( pt ) || byte.class.isAssignableFrom( pt ) )
            return Byte.valueOf( rv );

        throw new IllegalArgumentException( "Property type not supported (" + pt + ")." );
    }

    /**
     * Checks whether the two given classes are representing the same data type, regardless of whether it is a Wrapper
     * class or a primitive type.
     * The types checked are double, integer, float, long, short, boolean and byte.
     *
     * @param  cls1 the first class to compare
     * @param  cls2 the second class to compare
     * @return true, if the two given classes represent the same number type.
     */
    private static boolean isSameOrWrapperType( Class<?> cls1, Class<?> cls2 ) {
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

    /**
     * Checks if property of given name exists in the given class. If the given string specifies
     * nested properties (E.g. curationDetails.troubled), only the substring before the first dot is evaluated and the
     * rest of the string is processed in a new recursive iteration.
     *
     * @param property the property to check for. If the string contains dot characters ('.'), only the part
     *                 before the first dot will be evaluated. Substring after the dot will be checked against the
     *                 type of the field retrieved from the substring before the dot.
     * @param cls      the class to check the property on.
     * @return the class of the property last in the line of nesting.
     */
    public static Class getPropertyType( String property, Class cls ) throws ObjectFilterException {
        String[] parts = property.split( "\\.", 2 );
        Field field = checkFieldExists( cls, parts[0] );
        Class<?> subCls = field.getType();

        if ( Collection.class.isAssignableFrom( subCls ) ) {
            ParameterizedType pt = ( ParameterizedType ) field.getGenericType();
            for ( Type type : pt.getActualTypeArguments() ) {
                if ( type instanceof Class ) {
                    subCls = ( Class<?> ) type;
                    break;
                }
            }
        }

        if ( parts.length > 1 ) {
            return getPropertyType( parts[1], subCls );
        } else {
            return subCls;
        }
    }

    private static Field checkFieldExists( Class<?> cls, String field ) throws ObjectFilterException {
        List<Field> fields = new ArrayList<>();
        for ( Class<?> c = cls; c != null; c = c.getSuperclass() ) {
            fields.addAll( Arrays.asList( c.getDeclaredFields() ) );
        }

        for ( Field f : fields ) {
            if ( f.getName().equals( field ) )
                return f;
        }
        throw new ObjectFilterException( "Class " + cls + " does not contain field '" + field + "'." );
    }
}
