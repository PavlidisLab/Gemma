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

import com.fasterxml.jackson.databind.util.StdDateFormat;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.GenericConversionService;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.stream.Collectors;

import static ubic.gemma.persistence.util.PropertyMappingUtils.formProperty;

/**
 * Holds the necessary information to filter an entity with a property, operator and right-hand side value.
 * <p>
 * Clauses and sub-clauses are always sorted by objectAlias, propertyName, operator and requiredValue. A clause is
 * sorted by its first element, if any.
 * @author tesarst
 * @author poirigui
 */
@Value
@EqualsAndHashCode(of = { "objectAlias", "propertyName", "operator", "requiredValue" })
public class Filter implements PropertyMapping, Comparable<Filter> {

    /**
     * This is only the date part of the ISO 8601 standard.
     */
    private static final DateFormat DATE_FORMAT = new StdDateFormat();

    /**
     * Provide all the supported type conversion for parsing required values.
     */
    private static final ConfigurableConversionService conversionService = new GenericConversionService();

    private static final Comparator<Filter> comparator = Comparator
            .comparing( Filter::getObjectAlias, Comparator.nullsFirst( Comparator.naturalOrder() ) )
            .thenComparing( Filter::getPropertyName )
            .thenComparing( Filter::getOperator )
            .thenComparing( Filter::getComparableRequiredValue, Comparator.nullsLast( Comparator.naturalOrder() ) );

    @Nullable
    private Comparable<Object> getComparableRequiredValue() {
        Object requiredValue = this.requiredValue;
        if ( operator.requiredType != null && Collection.class.isAssignableFrom( operator.requiredType ) ) {
            // unpack the first element of the collection
            if ( requiredValue != null ) {
                requiredValue = ( ( Collection<?> ) requiredValue ).iterator().next();
            }
        }
        //noinspection unchecked
        return ( Comparable<Object> ) requiredValue;
    }

    private static <T> void addConverter( Class<T> targetClass, Converter<String, T> converter, Converter<T, String> reverseConverter ) {
        conversionService.addConverter( String.class, targetClass, converter );
        conversionService.addConverter( targetClass, String.class, reverseConverter );
    }

    static {
        addConverter( String.class, s -> s, s -> s );
        addConverter( Boolean.class, Boolean::parseBoolean, Object::toString );
        addConverter( Double.class, Double::parseDouble, Object::toString );
        addConverter( Float.class, Float::parseFloat, Object::toString );
        addConverter( Byte.class, Byte::parseByte, Object::toString );
        addConverter( Long.class, Long::parseLong, Object::toString );
        addConverter( Integer.class, Integer::parseInt, Object::toString );
        addConverter( Date.class, s -> {
            try {
                return DATE_FORMAT.parse( s );
            } catch ( ParseException e ) {
                throw new ConversionFailedException( TypeDescriptor.valueOf( Date.class ), TypeDescriptor.valueOf( String.class ), s, e );
            }
        }, DATE_FORMAT::format );
        addConverter( URL.class, s -> {
            try {
                return new URL( s );
            } catch ( MalformedURLException e ) {
                throw new RuntimeException( e );
            }
        }, URL::toString );
        // handle ann enum to string conversion using Enum::name
        //noinspection rawtypes
        conversionService.addConverterFactory( new ConverterFactory<String, Enum>() {

            @Override
            public <T extends Enum> Converter<String, T> getConverter( Class<T> targetType ) {
                //noinspection unchecked
                return source -> ( T ) Enum.valueOf( targetType, source );
            }
        } );
        conversionService.addConverter( Enum.class, String.class, ( Converter<Enum<?>, String> ) Enum::name );
    }

    /**
     * Obtain the conversion service used for parsing values.
     */
    public static ConversionService getConversionService() {
        return conversionService;
    }

    /**
     * Create a new filter.
     * <p>
     * If you need to parse the right-hand side, consider using {@link #parse(String, String, Class, Operator, String, String)}
     * for a scalar or {@link #parse(String, String, Class, Operator, Collection, String)} for a collection type.
     *
     * @param objectAlias      the alias that refers to the entity subject to the filter
     * @param propertyName     the property in the entity
     * @param propertyType     the type of the property
     * @param operator         a valid operator for the property and the requiredValue
     * @param requiredValue    a required value, or null to perform a null-check (i.e. <code>objectAlias.propertyName is null</code>)
     * @param originalProperty the original property name, or null if not applicable
     * @throws IllegalArgumentException if the type of the requiredValue does not match the propertyType
     */
    public static <T> Filter by( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Operator operator, @Nullable T requiredValue, String originalProperty ) {
        return new Filter( objectAlias, propertyName, propertyType, operator, requiredValue, originalProperty );
    }

    /**
     * Create a new filter with a collection right hand side.
     */
    public static <T> Filter by( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Operator operator, Collection<T> requiredValues, String originalProperty ) {
        return new Filter( objectAlias, propertyName, propertyType, operator, requiredValues, originalProperty );
    }

    public static <T> Filter by( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Operator operator, Subquery requiredValues, String originalProperty ) {
        return new Filter( objectAlias, propertyName, propertyType, operator, requiredValues, originalProperty );
    }

    /**
     * Create a new filter without an original property.
     * @see #by(String, String, Class, Operator, Object, String)
     */
    public static <T> Filter by( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Operator operator, @Nullable T requiredValue ) {
        return new Filter( objectAlias, propertyName, propertyType, operator, requiredValue, null );
    }

    /**
     * Create a new filter without an original property and a collection right hand side.
     * @see #by(String, String, Class, Operator, Object, String)
     */
    public static <T> Filter by( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Operator operator, Collection<T> requiredValues ) {
        return new Filter( objectAlias, propertyName, propertyType, operator, requiredValues, null );
    }

    public static <T> Filter by( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Operator operator, Subquery requiredValues ) {
        return new Filter( objectAlias, propertyName, propertyType, operator, requiredValues, null );
    }

    /**
     * Parse filter where the right-hand side is a scalar.
     *
     * @param requiredValue a right-hand side to be parsed according to the propertyType and operator
     * @throws IllegalArgumentException if the right-hand side cannot be parsed, which is generally caused by a
     *                                  {@link ConversionException} when attempting to convert the requiredValue to the
     *                                  desired propertyType.
     * @see #Filter(String, String, Class, Operator, Object, String)
     */
    public static Filter parse( @Nullable String objectAlias, String propertyName, Class<?> propertyType, Operator operator, String requiredValue, String originalProperty ) throws IllegalArgumentException {
        return new Filter( objectAlias, propertyName, propertyType, operator, parseRequiredValue( requiredValue, propertyType ), originalProperty );
    }

    public static Filter parse( @Nullable String objectAlias, String propertyName, Class<?> propertyType, Operator operator, String requiredValue ) throws IllegalArgumentException {
        return new Filter( objectAlias, propertyName, propertyType, operator, parseRequiredValue( requiredValue, propertyType ), null );
    }

    /**
     * Parse a filter where the right-hand side is a {@link Collection} of scalar right-hand side to be parsed.
     * <p>
     * If you need to parse a collection held in a {@link String} (i.e. <code>"(1,2,3,4)"</code>), you should use
     * {@link #parse(String, String, Class, Operator, String, String)} instead.
     *
     * @param requiredValues a collection of right-hand side to be parsed
     * @throws IllegalArgumentException if the right-hand side cannot be parsed, which is generally caused by a
     *                                  {@link ConversionException} when attempting to convert the requiredValue to the
     *                                  desired propertyType.
     * @see #Filter(String, String, Class, Operator, Object, String)
     */
    public static Filter parse( @Nullable String objectAlias, String propertyName, Class<?> propertyType, Operator operator, Collection<String> requiredValues, String originalProperty ) throws IllegalArgumentException {
        return new Filter( objectAlias, propertyName, propertyType, operator, parseRequiredValues( requiredValues, propertyType ), originalProperty );
    }

    public static Filter parse( @Nullable String objectAlias, String propertyName, Class<?> propertyType, Operator operator, Collection<String> requiredValues ) throws IllegalArgumentException {
        return new Filter( objectAlias, propertyName, propertyType, operator, parseRequiredValues( requiredValues, propertyType ), null );
    }

    public enum Operator {
        eq( "=", false, null ),
        notEq( "!=", false, null ),
        like( "like", true, String.class ),
        lessThan( "<", true, null ),
        greaterThan( ">", true, null ),
        lessOrEq( "<=", true, null ),
        greaterOrEq( ">=", true, null ),
        in( "in", true, Collection.class ),
        inSubquery( "in", true, Subquery.class );

        /**
         * Token used when parsing filter input.
         */
        private final String token;

        /**
         * THe required value must not be null.
         */
        private final boolean nonNullRequired;

        /**
         * The required value must satisfy this type.
         */
        @Nullable
        private final Class<?> requiredType;

        Operator( String operator, boolean isNonNullRequired, @Nullable Class<?> requiredType ) {
            this.token = operator;
            this.nonNullRequired = isNonNullRequired;
            this.requiredType = requiredType;
        }
    }

    @Nullable
    String objectAlias;
    String propertyName;
    Class<?> propertyType;
    Operator operator;
    @Nullable
    Object requiredValue;
    /**
     * The origin, if known.
     */
    @Nullable
    String originalProperty;

    private Filter( @Nullable String objectAlias, String propertyName, Class<?> propertyType, Operator operator, @Nullable Object requiredValue, @Nullable String originalProperty ) throws IllegalArgumentException {
        this.objectAlias = objectAlias;
        this.propertyName = propertyName;
        this.propertyType = propertyType;
        this.operator = operator;
        this.requiredValue = requiredValue;
        this.originalProperty = originalProperty;
        this.checkTypeCorrect();
    }

    @Override
    public int compareTo( Filter filter ) {
        return comparator.compare( this, filter );
    }

    @Override
    public String toString() {
        return toString( false );
    }

    @Override
    public String toOriginalString() {
        return toString( true );
    }

    private String toString( boolean withOriginalProperties ) {
        String requiredValueString;
        if ( requiredValue instanceof Subquery ) {
            return ( ( Subquery ) requiredValue ).getFilter().toString( withOriginalProperties );
        } else if ( requiredValue instanceof Collection ) {
            requiredValueString = "(" + ( ( Collection<?> ) requiredValue ).stream()
                    .map( e -> conversionService.convert( e, String.class ) )
                    .map( Filter::quoteIfNecessary )
                    .collect( Collectors.joining( ", " ) ) + ")";
        } else {
            requiredValueString = conversionService.convert( requiredValue, String.class );
            requiredValueString = quoteIfNecessary( requiredValueString );
        }
        return String.format( "%s %s %s",
                withOriginalProperties && originalProperty != null ? originalProperty : formProperty( this ),
                operator.token,
                requiredValueString );
    }

    private static String quoteIfNecessary( String s ) {
        // FIXME: this is incomplete, a full solution would be based on the FilterArg grammar
        char[] RESERVED_CHARS = { '(', ')', ',', ' ' };
        if ( StringUtils.containsAny( s, RESERVED_CHARS ) || s.isEmpty() ) {
            return "\"" + s.replace( "\"", "\\\"" ) + "\"";
        } else {
            return s;
        }
    }

    private void checkTypeCorrect() throws IllegalArgumentException {
        if ( operator.nonNullRequired && requiredValue == null ) {
            throw new IllegalArgumentException( "requiredValue for operator " + operator + " cannot be null." );
        }

        // if the operator does not have a specific type requirement, then consider that the propertyType is the type requirement
        Class<?> requiredType = operator.requiredType != null ? operator.requiredType : propertyType;

        // if the required type is a primitive, convert it to the corresponding wrapper type because required value can
        // never be a primitive
        if ( requiredType.isPrimitive() ) {
            requiredType = ClassUtils.primitiveToWrapper( requiredType );
        }

        if ( requiredValue != null && !requiredType.isAssignableFrom( requiredValue.getClass() ) ) {
            throw new IllegalArgumentException( "requiredValue " + requiredValue + " of type " + requiredValue.getClass().getName() + " for operator " + operator + " must be assignable from " + requiredType.getName() + "." );
        }

        // if an operator expects a collection as RHS, then the type of the elements in that collection must absolutely
        // match the propertyType
        // for example, ad.id in ("a", 1, NULL) must be invalid if id is of type Long
        if ( requiredValue != null && operator.requiredType != null && Collection.class.isAssignableFrom( operator.requiredType ) ) {
            Collection<?> requiredCollection = ( Collection<?> ) requiredValue;
            if ( !requiredCollection.stream().allMatch( rv -> rv != null && propertyType.isAssignableFrom( rv.getClass() ) ) ) {
                throw new IllegalArgumentException( String.format( "All elements in requiredValue %s must be assignable from %s.", requiredType, propertyType.getName() ) );
            }
        }
    }

    /**
     * Converts the given value to be of the given property type. For primitive number types, the wrapper class is used.
     *
     * @param rv the Object to be converted into the desired type.
     * @param pt the type that the given value should be converted to.
     * @return and Object of requested type, containing the given value converted to the new type.
     */
    private static Object parseRequiredValue( String rv, Class<?> pt ) throws IllegalArgumentException {
        return parseItem( rv, pt );
    }

    private static Object parseRequiredValues( Collection<String> requiredValues, Class<?> pt ) throws IllegalArgumentException {
        return requiredValues.stream()
                .map( item -> parseItem( item, pt ) )
                .collect( Collectors.toList() );
    }

    private static Object parseItem( String rv, Class<?> pt ) throws IllegalArgumentException {
        try {
            return conversionService.convert( rv, pt );
        } catch ( ConversionException e ) {
            throw new IllegalArgumentException( e );
        }
    }
}
