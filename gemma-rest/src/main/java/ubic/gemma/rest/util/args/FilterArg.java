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
import ubic.gemma.rest.util.MalformedArgException;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represent a filter argument designed to generate a {@link Filters} from user input.
 * <p>
 * Filtering can be done on any property or nested property of an entity managed by a {@link FilteringService}. E.g:
 * 'curationDetails' or 'curationDetails.lastTroubledEvent.date'.
 * <p>
 * Any property of a supported type. Currently, supported types are:
 * <ul>
 * <li>String - property of {@link String} type, required value can be any String.</li>
 * <li>Number - any Number implementation (i.e. {@link Float}, {@link Double}, {@link Integer}, {@link Long}, and their
 * corresponding primitive types). Required value must be a string parseable to the specific Number type.</li>
 * <li>Boolean - required value will be parsed to true only if the string matches 'true', ignoring case.</li>
 * <li>Date - property of {@link java.util.Date}, required value must be an ISO 8601 formatted date or datetime, UTC is assumed of no timezone is supplied</li>
 * <li>Collection - property of a {@link java.util.Collection} of any type aforementioned, nested collections are not
 * supported</li>
 * </ul>
 * <p>
 * Accepted operator keywords are:
 * <ul>
 * <li>'=' - equality</li>
 * <li>'!=' - non-equality</li>
 * <li>'&lt;' - smaller than (only Number and Date types)</li>
 * <li>'&gt;' - larger than (only Number and Date types)</li>
 * <li>'&lt;=' - smaller or equal (only Number and Date types)</li>
 * <li>'&gt;=' - larger or equal (only Number and Date types)</li>
 * <li>'like' - similar string, effectively means 'contains', translates to the SQL {@code LIKE} operator where the
 * given value is surrounded by '%' signs</li>
 * <li>'in' - required value in the given collection with the semantic of the '=' equality operator (only for Collection
 * types)</li>
 * </ul>
 * <p>
 * See {@link ubic.gemma.persistence.util.Filter.Operator} for more details on the available operators.
 * <p>
 * Properties, operators and required values must be delimited by spaces.
 * <p>
 * Multiple filters can be chained using 'AND' or 'OR' keywords.
 * Example:
 * {@code property1 < value1 AND property2 like value2}
 *
 * If chained filters are mixed conjunctions and disjunctions, the query must be in conjunctive normal form (CNF)
 * without any parentheses. Every AND conjunctions separates blocks of OR disjunctions.
 * <p>
 * Example:
 * {@code p1 = v1 OR p1 != v2 AND p2 <=v2 AND p3 > v3 OR p3 < v4}
 * Above query will translate to:
 * {@code (p1 = v1 OR p1 != v2) AND (p2 <=v2) AND (p3 > v3 OR p3 < v4;)}
 *
 * The format of collection is a sequence of comma-delimited  values surrounded by parenthesis. The values must be
 * compatible with the type contained in the collection. No space can be used to separate elements of a collection.
 * <p>
 * Example:
 * {@code id in (1,2,3,4)}
 *
 * Breaking the CNF results in an error.
 * <p>
 * The available properties on an entity can be restricted by the service layer via {@link FilteringService#getFilter(String, ubic.gemma.persistence.util.Filter.Operator, String)}.
 *
 * @author tesarst
 * @see Filters
 * @see ubic.gemma.persistence.util.Filter
 * @see ubic.gemma.persistence.util.Filter.Operator
 */
@CommonsLog
@Schema(type = "string", description = "Filter results by matching the expression. The exact syntax is described in the attached external documentation.",
        externalDocs = @ExternalDocumentation(url = "https://gemma.msl.ubc.ca/resources/apidocs/ubic/gemma/web/services/rest/util/args/FilterArg.html"))
public class FilterArg<O extends Identifiable> extends AbstractArg<FilterArg.Filter> {

    private FilterArg( FilterArgParser.FilterContext filterContext ) {
        super( new Filter( filterContext ) );
    }

    /**
     * Create a {@link Filters} that can be used as a filter parameter for service value object retrieval.
     * <p>
     * This is typically used with {@link FilteringService#loadPreFilter(Filters, Sort, int, int)}
     * for retrieving and filtering entities.
     *
     * @param service a filtering service that can resolve the properties types and relevant object alias to use
     * @return a {@link Filters} structure that is actually an iterable over a sequence of conjunction of disjunction of
     * {@link ubic.gemma.persistence.util.Filter}, or null if the filter is empty.
     * @throws MalformedArgException if the filter cannot be parsed for the given {@link FilteringService}
     */
    public Filters getFilters( FilteringService<O> service ) throws MalformedArgException {
        Filter filter = getValue();
        Filters filterList = Filters.empty();

        for ( FilterArgParser.ClauseContext clause : filter.filterContext.clause() ) {
            Filters.FiltersClauseBuilder disjunction = filterList.and();
            for ( FilterArgParser.SubClauseContext subClause : clause.subClause() ) {
                String property = subClause.PROPERTY().getText();
                ubic.gemma.persistence.util.Filter.Operator operator;
                try {
                    if ( subClause.operator() != null ) {
                        operator = operatorToOperator( subClause.operator() );
                        String requiredValue = scalarToString( subClause.scalar() );
                        disjunction = disjunction.or( service.getFilter( property, operator, requiredValue ) );
                    } else {
                        operator = collectionOperatorToOperator( subClause.collectionOperator() );
                        List<String> requiredValue = subClause.collection().scalar().stream().map( FilterArg::scalarToString ).collect( Collectors.toList() );
                        disjunction = disjunction.or( service.getFilter( property, operator, requiredValue ) );
                    }
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
        }
        throw new IllegalArgumentException( String.format( "Unknown operator: %s", operator.getText() ) );
    }

    private static ubic.gemma.persistence.util.Filter.Operator collectionOperatorToOperator( FilterArgParser.CollectionOperatorContext terminalNode ) {
        if ( terminalNode.IN() != null ) {
            return ubic.gemma.persistence.util.Filter.Operator.in;
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
     *
     * @param s the request filter string.
     * @return an instance of DatasetFilterArg representing the filtering options in the given string.
     */
    @SuppressWarnings("unused")
    public static <O extends Identifiable> FilterArg<O> valueOf( final String s ) {
        LoggingErrorListener lel = new LoggingErrorListener();

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
