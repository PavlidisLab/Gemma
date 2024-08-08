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
package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.apachecommons.CommonsLog;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.FilteringService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.persistence.util.SubqueryMode;
import ubic.gemma.rest.util.MalformedArgException;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ubic.gemma.persistence.util.FiltersUtils.unnestSubquery;
import static ubic.gemma.rest.util.ArgUtils.decodeCompressedArg;

/**
 * Represent a filter argument designed to generate a {@link Filters} from user input.
 * <p>
 * Filtering can be done on any property or nested property of an entity managed by a {@link FilteringService}. E.g:
 * 'curationDetails' or 'curationDetails.lastTroubledEvent.date'.
 * <p>
 * Any property of a supported type. Currently, supported types are:
 * <dl>
 * <dt>String</dt>
 * <dd>Property of {@link String} type. Strings that contain parenthesis {@code ()}, comma {@code ,} or space ' '
 * characters, or empty strings must be quoted with double-quotes {@code "}. Double-quotes can be escaped with a
 * backslash character: {@code \"}.</dd>
 * <dt>Number</dt>
 * <dd>Any Number implementation (i.e. {@link Float}, {@link Double}, {@link Integer}, {@link Long}, and their
 * corresponding primitive types). Required value must be a string parseable to the specific Number type.</dd>
 * <dt>Boolean</dt>
 * <dd>Required value will be parsed to true only if the string matches {@code true}, ignoring case.</dd>
 * <dt>Date</dt>
 * <dd>Property of {@link java.util.Date}, required value must be an ISO 8601 formatted date or datetime, UTC is assumed of no timezone is supplied</dd>
 * <dt>Collection</dt>
 * <dd>Property of a {@link java.util.Collection} of any type aforementioned, nested collections are not supported</dd>
 * </dl>
 * <p>
 * Accepted operator keywords are:
 * <dl>
 * <dt>=</dt>
 * <dd>Equality</dd>
 * <dt>!=</dt>
 * <dd>Non-equality</dd>
 * <dt>&lt;</dt>
 * <dd>Smaller than (only Number and Date types)</dd>
 * <dt>&gt;</dt>
 * <dd>Larger than (only Number and Date types)</dd>
 * <dt>&lt;=</dt>
 * <dd>Smaller or equal (only Number and Date types)</dd>
 * <dt>&gt;=</dt>
 * <dd>Larger or equal (only Number and Date types)</dd>
 * <dt>like or LIKE</dt>
 * <dd>Similar string, effectively means 'begins with', translates to the SQL {@code LIKE} operator where a '%' is
 * appended to the given value (only String type)</dd>
 * <dt>not like or NOT LIKE</dt>
 * <dd>Similar string, effectively means 'does not begins with', translates to the SQL {@code NOT LIKE} operator where a
 * '%' is appended to the given value (only String type)</dd>
 * <dt>in or IN</dt>
 * <dd>Required value in the given collection with the semantic of the '=' equality operator (only for Collection
 * types)</dd>
 * <dt>not in or NOT IN</dt>
 * <dd>Required value not in the given collection with the semantic of '=' equality operator (only for Collection types)</dd>
 * </dl>
 * <p>
 * See {@link ubic.gemma.persistence.util.Filter.Operator} for more details on the available operators.
 * <p>
 * If the property refers to a collection of entities, an operator can be used to indicate the desired behavior:
 * <dl>
 * <dt>any(predicate)</dt>
 * <dd>True if any element of the collection satisfies the predicate.</dd>
 * <dt>all(predicate)</dt>
 * <dd>True if all elements of the collection satisfies the predicate. Note that this is true for an empty collection.</dd>
 * <dt>none(predicate)</dt>
 * <dd>True if no element of the collection satisfies the predicate. Note that this is true for an empty collection.</dd>
 * </dl>
 * By default, {@code any(predicate)} is used.
 * <p>
 * Properties, operators and required values must be delimited by spaces.
 * <p>
 * Multiple filters can be chained using conjunctions (i.e. {@code AND, and}) or disjunctions (i.e. {@code OR, or, ','}) keywords.
 * Example:<br>
 * {@code property1 < value1 AND property2 like value2}
 * <p>
 * Queries mixing conjunctions and disjunctions are interpreted in <a href="https://en.wikipedia.org/wiki/Conjunctive_normal_form">conjunctive normal form (CNF)</a>
 * without any parentheses. Every {@code AND} conjunctions separates blocks of {@code OR} disjunctions.
 * <p>
 * Example:<br>
 * {@code p1 = v1 OR p1 != v2 AND p2 <=v2 AND p3 > v3, p3 < v4}
 * <p>
 * Above query will translate to:<br>
 * {@code (p1 = v1 OR p1 != v2) AND (p2 <= v2) AND (p3 > v3 OR p3 < v4)}
 * <p>
 * Breaking the CNF results in an error.
 * <p>
 * The format of collection is a sequence of comma-delimited  values surrounded by parenthesis. The values must be
 * compatible with the type contained in the collection.
 * <p>
 * Example:<br>
 * {@code id in (1,2,3,4)}
 * <p>
 * The available filterable properties on an entity can be retrieved from {@link FilteringService#getFilterableProperties()}.
 *
 * @author tesarst
 * @author poirigui
 * @see Filters
 * @see ubic.gemma.persistence.util.Filter
 * @see ubic.gemma.persistence.util.Filter.Operator
 * @see FilteringService
 */
@CommonsLog
@Schema(type = "string", description = "Filter results by matching the expression. The exact syntax is described in the attached external documentation. The filter value may be compressed with gzip and encoded with base64.",
        externalDocs = @ExternalDocumentation(url = "https://gemma.msl.ubc.ca/resources/apidocs/ubic/gemma/rest/util/args/FilterArg.html"))
public class FilterArg<O extends Identifiable> extends AbstractArg<FilterArg.Filter> {

    /**
     * Maximum number of clauses that can appear in a filter expression.
     */
    public static final int MAX_CLAUSES = 50;

    private FilterArg( FilterArgParser.FilterContext filterContext ) {
        super( new Filter( filterContext ) );
    }

    /**
     * Create a {@link Filters} that can be used as a filter parameter for service value object retrieval.
     * <p>
     * This is typically used with {@link FilteringService#load(Filters, Sort, int, int)}
     * for retrieving and filtering entities.
     *
     * @param service a filtering service that can resolve the properties types and relevant object alias to use
     * @return a {@link Filters} structure that is actually an iterable over a sequence of conjunction of disjunction of
     * {@link ubic.gemma.persistence.util.Filter}, or null if the filter is empty.
     * @throws MalformedArgException if the filter cannot be parsed for the given {@link FilteringService}
     */
    Filters getFilters( FilteringService<O> service ) throws MalformedArgException {
        Filter filter = getValue();

        int numClauses = filter.filterContext.clause().stream()
                .map( FilterArgParser.ClauseContext::subClause )
                .mapToInt( Collection::size ).sum();
        if ( numClauses > MAX_CLAUSES ) {
            throw new MalformedArgException( String.format( "The number of clauses cannot exceed %d.", MAX_CLAUSES ) );
        }

        Filters filterList = Filters.empty();
        for ( FilterArgParser.ClauseContext clause : filter.filterContext.clause() ) {
            Filters.FiltersClauseBuilder disjunction = filterList.and();
            for ( FilterArgParser.SubClauseContext subClause : clause.subClause() ) {
                SubqueryMode subqueryMode;
                if ( subClause.quantifier() != null ) {
                    if ( subClause.quantifier().NONE() != null ) {
                        subqueryMode = SubqueryMode.NONE;
                    } else if ( subClause.quantifier().ANY() != null ) {
                        subqueryMode = SubqueryMode.ANY;
                    } else if ( subClause.quantifier().ALL() != null ) {
                        subqueryMode = SubqueryMode.ALL;
                    } else {
                        throw new IllegalArgumentException( "Unsupported sub-clause quantifier " + subClause.quantifier().getText() + "." );
                    }
                } else {
                    subqueryMode = null;
                }
                FilterArgParser.PredicateContext predicate = subClause.predicate();
                String property = predicate.PROPERTY().getText();
                ubic.gemma.persistence.util.Filter.Operator operator;
                try {
                    if ( !service.getFilterableProperties().contains( property ) ) {
                        throw new IllegalArgumentException( String.format( "The property of %s is unknown", property ) );
                    }
                    ubic.gemma.persistence.util.Filter f;
                    if ( predicate.operator() != null ) {
                        operator = operatorToOperator( predicate.operator() );
                        String requiredValue = scalarToString( predicate.scalar() );
                        if ( subqueryMode != null ) {
                            f = service.getFilter( property, operator, requiredValue, subqueryMode );
                        } else {
                            f = service.getFilter( property, operator, requiredValue );
                        }
                    } else {
                        operator = collectionOperatorToOperator( predicate.collectionOperator() );
                        List<String> requiredValue = predicate.collection().scalar().stream().map( FilterArg::scalarToString ).collect( Collectors.toList() );
                        if ( subqueryMode != null ) {
                            f = service.getFilter( property, operator, requiredValue, subqueryMode );
                        } else {
                            f = service.getFilter( property, operator, requiredValue );
                        }
                        // the rhs might be a subquery, unnest it
                        ubic.gemma.persistence.util.Filter filterToValidate = unnestSubquery( f );
                        if ( filterToValidate.getRequiredValue() == null || ( ( Collection<?> ) filterToValidate.getRequiredValue() ).isEmpty() ) {
                            // collections must be non-empty
                            throw new MalformedArgException( String.format( "The right hand side collection in '%s' must be non-empty.",
                                    filterToValidate ) );
                        }
                    }
                    disjunction = disjunction.or( f );
                } catch ( IllegalArgumentException e ) {
                    throw new MalformedArgException( String.format( "The entity cannot be filtered by %s: %s", property, e.getMessage() ), e );
                }
            }
            disjunction.build();
        }

        return filterList;
    }

    private static ubic.gemma.persistence.util.Filter.Operator operatorToOperator( FilterArgParser.OperatorContext operator ) {
        switch ( operator.getStart().getType() ) {
            case FilterArgLexer.EQ:
                return ubic.gemma.persistence.util.Filter.Operator.eq;
            case FilterArgLexer.NEQ:
                return ubic.gemma.persistence.util.Filter.Operator.notEq;
            case FilterArgLexer.LE:
                return ubic.gemma.persistence.util.Filter.Operator.lessThan;
            case FilterArgLexer.LEQ:
                return ubic.gemma.persistence.util.Filter.Operator.lessOrEq;
            case FilterArgLexer.GE:
                return ubic.gemma.persistence.util.Filter.Operator.greaterThan;
            case FilterArgLexer.GEQ:
                return ubic.gemma.persistence.util.Filter.Operator.greaterOrEq;
            case FilterArgLexer.LIKE:
                return ubic.gemma.persistence.util.Filter.Operator.like;
            case FilterArgLexer.NOT_LIKE:
                return ubic.gemma.persistence.util.Filter.Operator.notLike;
        }
        throw new IllegalArgumentException( String.format( "Unknown operator: %s", operator.getText() ) );
    }

    private static ubic.gemma.persistence.util.Filter.Operator collectionOperatorToOperator( FilterArgParser.CollectionOperatorContext terminalNode ) {
        if ( terminalNode.IN() != null ) {
            return ubic.gemma.persistence.util.Filter.Operator.in;
        }
        if ( terminalNode.NOT_IN() != null ) {
            return ubic.gemma.persistence.util.Filter.Operator.notIn;
        }
        throw new IllegalArgumentException( String.format( "Unknown operator: %s", terminalNode.getText() ) );
    }

    private static String scalarToString( FilterArgParser.ScalarContext scalar ) {
        if ( scalar.QUOTED_STRING() != null ) {
            // replace escaped quotes
            return scalar.getText()
                    .substring( 1, scalar.getText().length() - 1 )
                    .replaceAll( Pattern.quote( "\\\"" ), "\"" );
        } else {
            return scalar.getText();
        }
    }

    /**
     * Represents the internal value of a {@link FilterArg}.
     */
    public static class Filter {

        private final FilterArgParser.FilterContext filterContext;

        private Filter( FilterArgParser.FilterContext filterContext ) {
            this.filterContext = filterContext;
        }
    }

    /**
     * Used by RS to parse value of request parameters.
     * <p>
     * The filter string may be compressed and base64-encoded.
     *
     * @param s the request filter string.
     * @return an instance of DatasetFilterArg representing the filtering options in the given string.
     */
    @SuppressWarnings("unused")
    public static <O extends Identifiable> FilterArg<O> valueOf( String s ) {
        LoggingErrorListener lel = new LoggingErrorListener();

        try {
            s = decodeCompressedArg( s );
        } catch ( IllegalArgumentException e ) {
            throw new MalformedArgException( e );
        }

        FilterArgLexer lexer = new FilterArgLexer( CharStreams.fromString( s ) ) {
            @Override
            public void recover( RecognitionException re ) {
                throw new ParseCancellationException( re );
            }

            @Override
            public void recover( LexerNoViableAltException e ) {
                throw new ParseCancellationException( e );
            }
        };
        lexer.removeErrorListeners();
        lexer.addErrorListener( lel );

        FilterArgParser parser = new FilterArgParser( new BufferedTokenStream( lexer ) );
        parser.setErrorHandler( new BailErrorStrategy() );
        parser.removeErrorListeners();
        parser.addErrorListener( lel );

        try {
            FilterArgParser.FilterContext f = parser.filter();
            return new FilterArg<>( f );
        } catch ( ParseCancellationException e ) {
            throw new MalformedArgException( String.format( "The filter query '%s' is not correctly formed.", s ),
                    e.getCause() );
        }
    }

    private static class LoggingErrorListener extends BaseErrorListener {
        @Override
        public void syntaxError( Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e ) {
            log.debug( msg, e );
        }
    }
}
