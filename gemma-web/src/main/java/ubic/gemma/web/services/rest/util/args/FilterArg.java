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

import com.google.common.base.Strings;
import ubic.gemma.persistence.service.FilteringService;
import ubic.gemma.persistence.service.FilteringVoEnabledService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.web.services.rest.util.MalformedArgException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * A base class for filter arguments implementing methods common to all filter arguments.
 *
 * <p>
 * Filtering can be done on any* property or nested property that the ExpressionExperiment/ArrayDesign class has (and is
 * mapped by hibernate ). E.g: 'curationDetails' or 'curationDetails.lastTroubledEvent.date'
 * </p>
 * * Any property of a supported type. Currently supported types are:
 * <ul>
 * <li>String - property of String type, required value can be any String.</li>
 * <li>Number - any Number implementation. Required value must be a string parseable to the specific
 * Number type.</li>
 * <li>Boolean - required value will be parsed to true only if the string matches 'true', ignoring
 * case.</li>
 * </ul>
 * Accepted operator keywords are:
 * <ul>
 * <li>'=' - equality</li>
 * <li>'!=' - non-equality</li>
 * <li>'&lt;' - smaller than</li>
 * <li>'&gt;' - larger than</li>
 * <li>'&lt;=' - smaller or equal</li>
 * <li>'=&gt;' - larger or equal</li>
 * <li>'like' - similar string, effectively means 'contains', translates to the sql 'LIKE' operator
 * (given value will be surrounded by % signs)</li>
 * </ul>
 * <p>
 * Multiple filters can be chained using 'AND' or 'OR' keywords.
 * </p>
 * <p>
 * Leave space between the keywords and the previous/next word!
 * </p>
 * <p>
 * E.g: <code>?filter=property1 &lt; value1 AND property2 like value2</code>
 * </p>
 * <p>
 * If chained filters are mixed conjunctions and disjunctions, the query must be in conjunctive
 * normal
 * form (CNF). Parentheses are not necessary - every AND keyword separates blocks of disjunctions.
 * </p>
 * Example:
 * <code>?filter=p1 = v1 OR p1 != v2 AND p2 &lt;=v2 AND p3 &gt; v3 OR p3 &lt; v4</code>
 * Above query will translate to:
 * <code>(p1 = v1 OR p1 != v2) AND (p2 &lt;=v2) AND (p3 &gt; v3 OR p3 &lt; v4;)</code>
 * <p>
 * Breaking the CNF results in an error.
 * </p>
 * <p>
 * Filter "curationDetails.troubled" will be ignored if user is not an administrator.
 * </p>
 *
 * @author tesarst
 */
public class FilterArg extends AbstractArg<FilterArg.Filter> {

    /**
     * An empty filter singleton.
     * @deprecated don't use that, a missing filter argument should be represented by the 'null' value and an empty
     * string would produce an empty list of {@link ObjectFilter} anyway.
     */
    @Deprecated
    public static final FilterArg EMPTY_FILTER = new FilterArg( null, null, null );

    public static final String ERROR_MSG_MALFORMED_REQUEST = "Entity does not contain the given property, or the provided value can not be converted to the property type.";
    private static final String ERROR_MSG_PARTS_TOO_SHORT = "Provided filter string does not contain at least one of property-operator-value sets.";
    private static final String ERROR_MSG_ILLEGAL_OPERATOR = "Illegal operator: %s is not an accepted operator.";
    private static final String ERROR_MSG_ARGS_MISALIGNED = "Filter query problem: Amount of properties, operators and values does not match";
    private static final String ERROR_PARSE_ERROR = "The filter query is not correctly formed.";
    private static final String ERROR_MALFORMED_REQUEST_BECAUSE_OF_PROPERTY = "The entity cannot be filtered by %s: %s";

    /**
     * @param propertyNames     names of properties to filter by. <br/>
     *                          Elements in each array will be in a disjunction (OR) with each other.<br/>
     *                          Arrays will then be in a conjunction (AND) with each other.<br/>
     * @param propertyValues    values to compare the given property names to.
     * @param propertyOperators the operation used for comparison of the given value and the value of the object.
     *                          The propertyValues will be the right operand of each given operator.
     *                          <br/>
     *                          E.g: <code>object.propertyName[0] isNot propertyValues[0];</code><br/>
     */
    private FilterArg( List<String[]> propertyNames, List<String[]> propertyValues, List<ObjectFilter.Operator[]> propertyOperators ) {
        super( new Filter( propertyNames, propertyValues, propertyOperators ) );
    }

    private FilterArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    /**
     * Parses the input string into lists of logical disjunctions, that together form a conjunction (CNF).
     *
     * @param s                 the string to be parsed.
     * @param propertyNames     list to be populated with property name arrays for each disjunction.
     * @param propertyValues    list to be populated with property value arrays for each disjunction.
     * @param propertyOperators list to be populated with operator arrays for each disjunction.
     */
    private static void parseFilterString( String s, List<String[]> propertyNames, List<String[]> propertyValues,
            List<ObjectFilter.Operator[]> propertyOperators ) {
        String[] parts = s.split( "\\s*,?\\s+" );

        List<String> propertyNamesDisjunction = new LinkedList<>();
        List<ObjectFilter.Operator> propertyOperatorsDisjunction = new LinkedList<>();
        List<String> propertyValuesDisjunction = new LinkedList<>();
        if ( parts.length < 3 ) {
            throw new IllegalArgumentException( ERROR_MSG_PARTS_TOO_SHORT );
        }

        for ( int i = 0; i < parts.length; ) {
            propertyNamesDisjunction.add( parts[i++] );
            propertyOperatorsDisjunction.add( parseObjectFilterOperator( parts[i++] ) );
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
    }

    /**
     * Parses the string into a valid operator.
     *
     * @param s the string to be parsed.
     * @return a string that is a valid operator.
     * @throws IllegalArgumentException if the
     */
    private static ObjectFilter.Operator parseObjectFilterOperator( String s ) {
        for ( ObjectFilter.Operator op : ObjectFilter.Operator.values() ) {
            if ( op.getToken().equalsIgnoreCase( s ) ) {
                return op;
            }
        }
        throw new IllegalArgumentException( String.format( ERROR_MSG_ILLEGAL_OPERATOR, s ) );
    }

    /**
     * Create a List of ObjectFilter arrays that can be used as a filter parameter for service value object
     * retrieval. If there is an "in" operator, the required value will be converted into a collection of strings.
     *
     * @param service a VO service that can resolve the properties types and relevant object alias to use subsequently
     *                in {@link FilteringVoEnabledService#loadValueObjectsPreFilter(Filters, Sort, int, int)} for example.
     *
     * @return a List of {@link ObjectFilter} arrays, each array represents a disjunction (OR) of filters. Arrays
     * then represent a conjunction (AND) with other arrays in the list, or null if this filter is empty.
     */
    public Filters getObjectFilters( FilteringService service ) throws MalformedArgException {
        Filter filter = getValue();
        if ( filter.propertyNames == null )
            return null;
        Filters filterList = new Filters();

        for ( int i = 0; i < filter.propertyNames.size(); i++ ) {
            try {
                String[] properties = filter.propertyNames.get( i );
                String[] values = filter.propertyValues.get( i );
                ObjectFilter.Operator[] operators = filter.propertyOperators.get( i );
                ObjectFilter[] filterArray = new ObjectFilter[properties.length];
                for ( int j = 0; j < properties.length; j++ ) {
                    try {
                        // these are user-supplied filters, so we need to do basic exception checking
                        filterArray[j] = service.getObjectFilter( properties[j], operators[j], values[j] );
                    } catch ( IllegalArgumentException e ) {
                        throw new MalformedArgException( String.format( ERROR_MALFORMED_REQUEST_BECAUSE_OF_PROPERTY, properties[j], e.getMessage() ), e );
                    }
                }
                filterList.add( filterArray );
            } catch ( IndexOutOfBoundsException e ) {
                throw new MalformedArgException( ERROR_MSG_ARGS_MISALIGNED, e );
            }
        }

        return filterList;
    }

    /**
     * Represents the internal value of a {@link FilterArg}.
     */
    public static class Filter {

        List<String[]> propertyNames;
        List<String[]> propertyValues;
        List<ObjectFilter.Operator[]> propertyOperators;

        public Filter( List<String[]> propertyNames, List<String[]> propertyValues, List<ObjectFilter.Operator[]> propertyOperators ) {
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
     * @throws javax.ws.rs.BadRequestException if the given string is not a well formed filter argument (E.g. is not CNF, or at least
     *                           one of the properties specified in the string does not exist in Expression Experiments.
     */
    @SuppressWarnings("unused")
    public static FilterArg valueOf( final String s ) {
        if ( Strings.isNullOrEmpty( s ) )
            return EMPTY_FILTER;

        List<String[]> propertyNames = new LinkedList<>();
        List<String[]> propertyValues = new LinkedList<>();
        List<ObjectFilter.Operator[]> propertyOperators = new LinkedList<>();
        List<Class[]> propertyTypes;

        try {
            parseFilterString( s, propertyNames, propertyValues, propertyOperators );
            return new FilterArg( propertyNames, propertyValues, propertyOperators );
        } catch ( IllegalArgumentException e ) {
            return new FilterArg( ERROR_PARSE_ERROR, e );
        }
    }
}
