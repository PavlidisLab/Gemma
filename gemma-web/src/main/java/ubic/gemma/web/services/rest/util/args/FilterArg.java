/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.persistence.service.FilteringService;
import ubic.gemma.persistence.service.FilteringVoEnabledService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.web.services.rest.util.MalformedArgException;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

/**
 * Represent a filter argument designed to generate a {@link Filters} from user input.
 *
 * Filtering can be done on any property or nested property of an entity managed by a {@link FilteringService}. E.g:
 * 'curationDetails' or 'curationDetails.lastTroubledEvent.date'.
 *
 * Any property of a supported type. Currently, supported types are:
 * <ul>
 * <li>String - property of {@link String} type, required value can be any String.</li>
 * <li>Number - any Number implementation (i.e. {@link Float}, {@link Double}, {@link Integer}, {@link Long}, and their
 * corresponding primitive types). Required value must be a string parseable to the specific Number type.</li>
 * <li>Boolean - required value will be parsed to true only if the string matches 'true', ignoring case.</li>
 * <li>Date - property of {@link java.util.Date}, required value must be an ISO 8601 formatted date or date-time with
 * UTC timezone</li>
 * <li>Collection - property of a {@link java.util.Collection} of any type aforementioned, nested collections are not
 * supported</li>
 * </ul>
 *
 * Accepted operator keywords are:
 * <ul>
 * <li>'=' - equality</li>
 * <li>'!=' - non-equality</li>
 * <li>'&lt;' - smaller than (only Number and Date types)</li>
 * <li>'&gt;' - larger than (only Number and Date types)</li>
 * <li>'&lt;=' - smaller or equal (only Number and Date types)</li>
 * <li>'&gt;=' - larger or equal (only Number and Date types)</li>
 * <li>'like' - similar string, effectively means 'contains', translates to the SQL <code>LIKE</code> operator where the
 * given value is surrounded by '%' signs</li>
 * <li>'in' - required value in the given collection with the semantic of the '=' equality operator (only for Collection
 * types)</li>
 * </ul>
 *
 * See {@link ObjectFilter.Operator} for more details on the available operators.
 *
 * Properties, operators and required values must be delimited by spaces.
 *
 * Multiple filters can be chained using 'AND' or 'OR' keywords.
 * Example:
 * <code>property1 &lt; value1 AND property2 like value2</code>
 *
 * If chained filters are mixed conjunctions and disjunctions, the query must be in conjunctive normal form (CNF)
 * without any parentheses. Every AND conjunctions separates blocks of OR disjunctions.
 *
 * Example:
 * <code>p1 = v1 OR p1 != v2 AND p2 &lt;=v2 AND p3 &gt; v3 OR p3 &lt; v4</code>
 * Above query will translate to:
 * <code>(p1 = v1 OR p1 != v2) AND (p2 &lt;=v2) AND (p3 &gt; v3 OR p3 &lt; v4;)</code>
 *
 * The format of collection is a sequence of comma-delimited  values surrounded by parenthesis. The values must be
 * compatible with the type contained in the collection. No space can be used to separate elements of a collection.
 *
 * Example:
 * <code>id in (1,2,3,4)</code>
 *
 * Breaking the CNF results in an error.
 *
 * The available properties on an entity can be restricted by the service layer via {@link FilteringService#getObjectFilter(String, ObjectFilter.Operator, String)}.
 *
 * @author tesarst
 * @see Filters
 * @see ObjectFilter
 * @see ObjectFilter.Operator
 */
@Schema(type = "string", description = "Filter results by matching the expression. The exact syntax is described in the attached external documentation.",
        externalDocs = @ExternalDocumentation(url = "https://gemma.msl.ubc.ca/resources/apidocs/ubic/gemma/web/services/rest/util/args/FilterArg.html"))
public class FilterArg extends AbstractArg<FilterArg.Filter> {

    /**
     * @param propertyNames     names of properties to filter by. <br> Elements in each array will be in a disjunction
     *                          (OR) with each other.<br> Arrays will then be in a conjunction (AND) with each
     *                          other.<br>
     * @param propertyValues    values to compare the given property names to.
     * @param propertyOperators the operation used for comparison of the given value and the value of the object. The
     *                          propertyValues will be the right operand of each given operator. <br> E.g:
     *                          <code>object.propertyName[0] isNot propertyValues[0];</code><br>
     */
    private FilterArg( List<String[]> propertyNames, List<String[]> propertyValues, List<ObjectFilter.Operator[]> propertyOperators ) {
        super( new Filter( propertyNames, propertyValues, propertyOperators ) );
    }

    /**
     * Create a {@link Filters} that can be used as a filter parameter for service value object retrieval.
     *
     * This is typically used with {@link FilteringVoEnabledService#loadValueObjectsPreFilter(Filters, Sort, int, int)}
     * for retrieving and filtering entities.
     *
     * @param service a filtering service that can resolve the properties types and relevant object alias to use
     * @return a {@link Filters} structure that is actually an iterable over a sequence of conjunction of disjunction of
     * {@link ObjectFilter}, or null if the filter is empty.
     * @throws MalformedArgException if the filter cannot be parsed for the given {@link FilteringService}
     */
    @Nullable
    public Filters getObjectFilters( FilteringService service ) throws MalformedArgException {
        Filter filter = getValue();
        if ( filter.propertyNames == null )
            return null;
        Filters filterList = new Filters();

        for ( int i = 0; i < filter.propertyNames.size(); i++ ) {
            String[] properties = filter.propertyNames.get( i );
            ObjectFilter.Operator[] operators = filter.propertyOperators.get( i );
            String[] values = filter.propertyValues.get( i );
            // this is guaranteed by how the filter array is constructed below
            assert properties.length == operators.length;
            assert properties.length == values.length;
            ObjectFilter[] filterArray = new ObjectFilter[properties.length];
            for ( int j = 0; j < properties.length; j++ ) {
                try {
                    // these are user-supplied filters, so we need to do basic exception checking
                    filterArray[j] = service.getObjectFilter( properties[j], operators[j], values[j] );
                } catch ( IllegalArgumentException e ) {
                    throw new MalformedArgException( String.format( "The entity cannot be filtered by %s: %s", properties[j], e.getMessage() ), e );
                }
            }
            filterList.add( filterArray );
        }

        return filterList;
    }

    /**
     * Represents the internal value of a {@link FilterArg}.
     */
    public static class Filter {

        private final List<String[]> propertyNames;
        private final List<String[]> propertyValues;
        private final List<ObjectFilter.Operator[]> propertyOperators;

        private Filter( List<String[]> propertyNames, List<String[]> propertyValues, List<ObjectFilter.Operator[]> propertyOperators ) {
            this.propertyNames = propertyNames;
            this.propertyValues = propertyValues;
            this.propertyOperators = propertyOperators;
        }
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request filter string.
     * @return an instance of DatasetFilterArg representing the filtering options in the given string.
     */
    @SuppressWarnings("unused")
    public static FilterArg valueOf( final String s ) {
        try {
            return parseFilterString( s );
        } catch ( FilterArgParseException e ) {
            throw new MalformedArgException( String.format( "The filter query '%s' is not correctly formed.", s ), e );
        }
    }

    /**
     * Parses the input string into lists of logical disjunctions, that together form a conjunction (CNF).
     *
     * @param s the string to be parsed.
     */
    private static FilterArg parseFilterString( String s ) throws FilterArgParseException {
        List<String[]> propertyNames = new LinkedList<>();
        List<String[]> propertyValues = new LinkedList<>();
        List<ObjectFilter.Operator[]> propertyOperators = new LinkedList<>();

        // TODO: have a nicer way to tokenize the filter
        String[] parts = StringUtils.isBlank( s ) ? new String[0] : s.split( "\\s*,?\\s+" );

        List<String> propertyNamesDisjunction = new LinkedList<>();
        List<ObjectFilter.Operator> propertyOperatorsDisjunction = new LinkedList<>();
        List<String> propertyValuesDisjunction = new LinkedList<>();

        int i = 0;
        while ( i < parts.length ) {
            if ( i + 2 >= parts.length ) {
                throw new FilterArgParseException( "Not enough parts to parse an property name, operator and required value from here.", i );
            }

            // parse property name
            propertyNamesDisjunction.add( parts[i++] );

            // parse operator
            final int j = i++;
            propertyOperatorsDisjunction.add( ObjectFilter.Operator.fromToken( parts[j] )
                    .orElseThrow( () -> new FilterArgParseException( String.format( "%s is not an accepted operator.", parts[j] ), j ) ) );

            // parse required value
            propertyValuesDisjunction.add( parts[i++] );

            if ( i == parts.length || parts[i].equalsIgnoreCase( "and" ) ) {
                // We either reached an 'AND', or the end of the string.
                // Add the current disjunction.
                propertyNames.add( propertyNamesDisjunction.toArray( new String[0] ) );
                propertyOperators.add( propertyOperatorsDisjunction.toArray( new ObjectFilter.Operator[0] ) );
                propertyValues.add( propertyValuesDisjunction.toArray( new String[0] ) );
                // Start new disjunction lists
                propertyNamesDisjunction = new LinkedList<>();
                propertyOperatorsDisjunction = new LinkedList<>();
                propertyValuesDisjunction = new LinkedList<>();
                i++;
            } else if ( parts[i].equalsIgnoreCase( "or" ) ) {
                // Skip this part and continue the disjunction
                i++;
            }
        }

        return new FilterArg( propertyNames, propertyValues, propertyOperators );
    }

}
