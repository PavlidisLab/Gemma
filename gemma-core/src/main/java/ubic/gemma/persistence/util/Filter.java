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
import lombok.Getter;
import lombok.Value;
import org.apache.commons.lang3.ClassUtils;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;

import javax.annotation.Nullable;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Holds the necessary information to filter an entity with a property, operator and right-hand side value.
 *
 * @author tesarst
 * @author poirigui
 */
@Value
public class Filter {

    /**
     * This is only the date part of the ISO 8601 standard.
     */
    private static final DateFormat DATE_FORMAT = new StdDateFormat();

    /**
     * Provide all the supported type conversion for parsing required values.
     */
    private static final ConfigurableConversionService conversionService = new GenericConversionService();

    private static <T> void addConverter( Class<?> targetClass, Converter<String, T> converter, Converter<T, String> reverseConverter ) {
        conversionService.addConverter( String.class, targetClass, converter );
        conversionService.addConverter( targetClass, String.class, reverseConverter );
    }

    static {
        addConverter( String.class, s -> s, s -> s );
        addConverter( Boolean.class, Boolean::parseBoolean, Object::toString );
        addConverter( Double.class, Double::parseDouble, Object::toString );
        addConverter( Float.class, Float::parseFloat, Object::toString );
        addConverter( Long.class, Long::parseLong, Object::toString );
        addConverter( Integer.class, Integer::parseInt, Object::toString );
        addConverter( Date.class, s -> {
            try {
                return DATE_FORMAT.parse( s );
            } catch ( ParseException e ) {
                throw new ConversionFailedException( TypeDescriptor.valueOf( Date.class ), TypeDescriptor.valueOf( String.class ), s, e );
            }
        }, DATE_FORMAT::format );
        // FIXME: move these into AbstractFilteringVoEnabledDao
        addConverter( DatabaseType.class, DatabaseType::valueOf, DatabaseType::name );
        addConverter( TechnologyType.class, TechnologyType::valueOf, TechnologyType::name );
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
     * If you need to parse the right-hand side, consider using {@link #parse(String, String, Class, Operator, String)}
     * for a scalar or {@link #parse(String, String, Class, Operator, Collection)} for a collection type.
     *
     * @param objectAlias the alias that refers to the entity subject to the filter
     * @param propertyName  the property in the entity
     * @param propertyType  the type of the property
     * @param operator      a valid operator for the property and the requiredValue
     * @param requiredValue a required value, or null to perform a null-check (i.e. <code>objectAlias.propertyName is null</code>)
     * @throws IllegalArgumentException if the type of the requiredValue does not match the propertyType
     */
    public static Filter by( @Nullable String objectAlias, String propertyName, Class<?> propertyType, Operator operator, @Nullable Object requiredValue ) {
        return new Filter( objectAlias, propertyName, propertyType, operator, requiredValue );
    }


    /**
     * Parse filter where the right-hand side is a scalar.
     *
     * @param requiredValue a right-hand side to be parsed according to the propertyType and operator
     * @throws IllegalArgumentException if the right-hand side cannot be parsed, which is generally caused by a
     *                                  {@link ConversionException} when attempting to convert the requiredValue to the
     *                                  desired propertyType.
     * @see #Filter(String, String, Class, Operator, Object)
     */
    public static Filter parse( @Nullable String objectAlias, String propertyName, Class<?> propertyType, Operator operator, String requiredValue ) throws IllegalArgumentException {
        return new Filter( objectAlias, propertyName, propertyType, operator, parseRequiredValue( requiredValue, propertyType ) );
    }


    /**
     * Parse a filter where the right-hand side is a {@link Collection} of scalar right-hand side to be parsed.
     * <p>
     * If you need to parse a collection held in a {@link String} (i.e. <code>"(1,2,3,4)"</code>), you should use
     * {@link #parse(String, String, Class, Operator, String)} instead.
     *
     * @param requiredValues a collection of right-hand side to be parsed
     * @throws IllegalArgumentException if the right-hand side cannot be parsed, which is generally caused by a
     *                                  {@link ConversionException} when attempting to convert the requiredValue to the
     *                                  desired propertyType.
     * @see #Filter(String, String, Class, Operator, Object)
     */
    public static Filter parse( @Nullable String objectAlias, String propertyName, Class<?> propertyType, Operator operator, Collection<String> requiredValues ) throws IllegalArgumentException {
        return new Filter( objectAlias, propertyName, propertyType, operator, parseRequiredValues( requiredValues, propertyType ) );
    }

    @Getter
    public enum Operator {
        /**
         * Note that in the case of a null requiredValue, the {@link #getSqlToken()} of this operator must be ignored
         * and 'is' must be used instead.
         */
        eq( "=", false, null ),
        /**
         * Same remark for {@link #eq} applies, but with the 'is not' operator.
         */
        notEq( "!=", false, null ),
        like( "like", true, String.class ),
        lessThan( "<", true, null ),
        greaterThan( ">", true, null ),
        lessOrEq( "<=", true, null ),
        greaterOrEq( ">=", true, null ),
        in( "in", true, Collection.class );

        /**
         * Retrieve an {@link Operator} from a given token.
         *
         * @return an {@link Optional} containing the operator matched in a case-insensitive manner if found, otherwise
         * {@link Optional#empty()}.
         */
        public static Optional<Operator> fromToken( String token ) {
            for ( Operator o : values() ) {
                if ( o.token.equalsIgnoreCase( token ) ) {
                    return Optional.of( o );
                }
            }
            return Optional.empty();
        }

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

        /**
         * Token used in SQL/HQL query.
         * <p>
         * This is package-private on purpose and is only meant for{@link FilterQueryUtils#formRestrictionClause(Filters)}.
         */
        String getSqlToken() {
            return token;
        }
    }

    @Nullable
    String objectAlias;
    String propertyName;
    Class<?> propertyType;
    Operator operator;
    @Nullable
    Object requiredValue;

    private Filter( @Nullable String objectAlias, String propertyName, Class<?> propertyType, Operator operator, @Nullable Object requiredValue ) throws IllegalArgumentException {
        this.objectAlias = objectAlias;
        this.propertyName = propertyName;
        this.propertyType = propertyType;
        this.operator = operator;
        this.requiredValue = requiredValue;
        this.checkTypeCorrect();
    }

    @Override
    public String toString() {
        String requiredValueString;
        if ( requiredValue instanceof Collection ) {
            requiredValueString = "(" + ( ( Collection<?> ) requiredValue ).stream()
                    .map( e -> conversionService.convert( e, String.class ) )
                    .collect( Collectors.joining( ", " ) ) + ")";
        } else {
            requiredValueString = conversionService.convert( requiredValue, String.class );
        }
        return String.format( "%s%s %s %s", objectAlias != null ? objectAlias + "." : "", propertyName, operator.getToken(), requiredValueString );
    }

    private void checkTypeCorrect() throws IllegalArgumentException {
        if ( operator.isNonNullRequired() && requiredValue == null ) {
            throw new IllegalArgumentException( "requiredValue for operator " + operator + " cannot be null." );
        }

        // if the operator does not have a specific type requirement, then consider that the propertyType is the type requirement
        Class<?> requiredType = operator.getRequiredType() != null ? operator.getRequiredType() : propertyType;

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
        if ( requiredValue != null && operator.getRequiredType() != null && Collection.class.isAssignableFrom( operator.getRequiredType() ) ) {
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
        if ( isCollection( rv ) ) {
            // convert individual elements
            return parseCollection( rv )
                    .map( item -> parseItem( item, pt ) )
                    .collect( Collectors.toList() );
        } else {
            return parseItem( rv, pt );
        }
    }

    private static Object parseRequiredValues( Collection<String> requiredValues, Class<?> pt ) throws IllegalArgumentException {
        return requiredValues.stream()
                .map( item -> parseItem( item, pt ) )
                .collect( Collectors.toList() );
    }

    private static final Pattern
            COLLECTION_PATTERN = Pattern.compile( "^\\((.+,)*.+\\)$" ),
            COLLECTION_DELIMITER_PATTERN = Pattern.compile( "\\s*,\\s*" );

    private static boolean isCollection( String value ) {
        return COLLECTION_PATTERN.matcher( value ).matches();
    }

    /**
     * Tries to parse the given string value into a collection of strings.
     * @param value the value to be parsed into a collection of strings. This should be a bracketed comma separated list
     *              of strings.
     * @return a collection of strings.
     */
    private static Stream<String> parseCollection( String value ) {
        // these are the parenthesis, thus we skip the first and last non-blank character
        return COLLECTION_DELIMITER_PATTERN.splitAsStream( value.trim().substring( 1, value.length() - 1 ) );
    }

    private static Object parseItem( String rv, Class<?> pt ) throws IllegalArgumentException {
        try {
            return conversionService.convert( rv, pt );
        } catch ( ConversionException e ) {
            throw new IllegalArgumentException( e );
        }
    }
}
