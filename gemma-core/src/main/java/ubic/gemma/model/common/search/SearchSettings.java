/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.model.common.search;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.With;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import ubic.gemma.core.search.Highlighter;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Configuration options for searching.
 *
 * @author paul
 */
@Data
@Builder
@With
public class SearchSettings implements Serializable {

    public static final char
            WILDCARD_CHAR = '*',
            SINGLE_WILDCARD_CHAR = '?';


    public enum SearchMode {
        /**
         * Prefer correctness over speed.
         */
        ACCURATE,
        /**
         * Normal search mode with trade-offs to make it usable.
         */
        BALANCED,
        /**
         * Fast search mode, designed for autocompletion.
         */
        FAST
    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -982243911532743661L;

    /**
     * How many results per result type are allowed. This implies that if you search for multiple types of things, you
     * can get more than this.
     */
    public static final int DEFAULT_MAX_RESULTS_PER_RESULT_TYPE = 5000;

    /**
     * Convenience method to get pre-configured settings.
     *
     * @param  query query
     * @return search settings
     */
    public static SearchSettings arrayDesignSearch( String query ) {
        return builder().query( query ).resultType( ArrayDesign.class ).build();
    }

    /**
     * Convenience method to get pre-configured settings.
     *
     * @param  query query
     * @return search settings
     */
    public static SearchSettings bibliographicReferenceSearch( String query ) {
        return builder().query( query ).resultType( BibliographicReference.class ).build();
    }

    /**
     * Convenience method to get pre-configured settings.
     *
     * @param  query       query
     * @param  arrayDesign the array design to limit the search to
     * @return search settings
     */
    public static SearchSettings compositeSequenceSearch( String query, ArrayDesign arrayDesign ) {
        return builder().query( query )
                .resultType( CompositeSequence.class )
                .resultType( ArrayDesign.class )
                .platformConstraint( arrayDesign ) // TODO: check if this was specified in the original code
                .build();
    }

    /**
     * Convenience method to get pre-configured settings.
     *
     * @param  query query
     * @return search settings
     */
    public static SearchSettings expressionExperimentSearch( String query ) {
        return builder()
                .query( query )
                .resultType( ExpressionExperiment.class )
                .build();
    }

    /**
     * Convenience method to get pre-configured settings.
     *
     * @param  query query
     * @param taxon if you want to filter by taxon (can be null)
     * @return search settings
     */
    public static SearchSettings expressionExperimentSearch( String query, Taxon taxon ) {
        return builder()
                .query( query )
                .resultType( ExpressionExperiment.class )
                .taxon( taxon )
                .build();
    }

    /**
     * Convenience method to get pre-configured settings.
     *
     * @param  query query
     * @param  taxon the taxon to limit the search to (can be null)
     * @return search settings
     */
    public static SearchSettings geneSearch( String query, Taxon taxon ) {
        return builder().query( query ).resultType( Gene.class ).taxon( taxon ).build();
    }

    /**
     * Processed query for performing a search.
     */
    private String query;

    /**
     * Entities to retrieve.
     */
    @Singular
    private Set<Class<? extends Identifiable>> resultTypes;

    /* optional search constraints */
    @Nullable
    private ArrayDesign platformConstraint;
    @Nullable
    private Taxon taxon;

    /* sources */
    @Builder.Default
    private boolean useCharacteristics = true;
    @Builder.Default
    private boolean useDatabase = true;
    @Builder.Default
    private boolean useGo = true;
    @Builder.Default
    private boolean useIndices = true;

    /**
     * Limit for the number of results per result type in {@link ubic.gemma.core.search.SearchService.SearchResultMap}.
     * <p>
     * The default is relatively large and given by {@link #DEFAULT_MAX_RESULTS_PER_RESULT_TYPE}. Any value less than
     * one indicate no limit.
     */
    @Builder.Default
    private int maxResults = SearchSettings.DEFAULT_MAX_RESULTS_PER_RESULT_TYPE;

    /**
     * Indicate if results should be filled.
     */
    @Builder.Default
    private boolean fillResults = true;

    /**
     * Fast mode, return quickly.
     */
    @Builder.Default
    private SearchMode mode = SearchMode.BALANCED;

    /**
     * A custom highlighter.
     */
    @Nullable
    private transient Highlighter highlighter;

    /**
     * Get this query, trimmed.
     */
    public String getQuery() {
        return query == null ? null : query.trim();
    }

    /**
     * Get the original query that was set by {@link #setQuery(String)}, untrimmed.
     */
    @SuppressWarnings("unused")
    public String getRawQuery() {
        return this.query;
    }

    /**
     * Indicate if the query refers to an ontology term.
     * <p>
     * This is done by checking if this query starts with 'http://' for now, but there could be fancier checks performed
     * in the future.
     */
    public boolean isTermQuery() {
        return getQuery() != null && getQuery().startsWith( "http://" );
    }

    /**
     * Obtain the term URI.
     *
     * @deprecated use {@link #getQuery()} and {@link #isTermQuery()} instead.
     *
     * @return the term URI if this is a term query, otherwise null
     */
    @Deprecated
    public String getTermUri() {
        return isTermQuery() ? getQuery() : null;
    }

    /**
     * Set this term URI.
     *
     * @deprecated URI can be set with {@link #setQuery(String)} instead.
     *
     * @param termUri a valid term URI, or null or a blank string
     */
    @Deprecated
    public void setTermUri( String termUri ) {
        if ( StringUtils.isNotBlank( termUri ) && !termUri.startsWith( "http://" ) ) {
            throw new IllegalArgumentException( "The term URI must be a valid URI." );
        }
        setQuery( termUri );
    }

    /**
     * Check if the query is a wildcard query.
     */
    public boolean isWildcard() {
        return query.contains( String.valueOf( WILDCARD_CHAR ) ) || query.contains( String.valueOf( SINGLE_WILDCARD_CHAR ) );
    }

    /**
     * Check if this is configured to search a given result type.
     */
    public boolean hasResultType( Class<?> cls ) {
        return resultTypes.contains( cls );
    }

    /**
     * Highlight a given ontology term.
     * <p>
     * This is a shorthand for {@link #getHighlighter()} and {@link Highlighter#highlightTerm(String, String, String)}
     * that deals with a potentially null highlighter.
     * @see #setHighlighter(Highlighter)
     * @return a highlight, or null if no provider is set or the provider returns null
     */
    @Nullable
    public Map<String, String> highlightTerm( String termUri, String termLabel, String field ) {
        return highlighter != null ? highlighter.highlightTerm( termUri, termLabel, field ) : null;
    }

    @Nullable
    public Map<String, String> highlightDocument( Document document, org.apache.lucene.search.highlight.Highlighter luceneHighlighter, Analyzer analyzer, Set<String> fields ) {
        return highlighter != null ? highlighter.highlightDocument( document, luceneHighlighter, analyzer, fields ) : null;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder( "'" + query + "'" );
        s.append( " in " ).append( resultTypes.stream().map( Class::getSimpleName ).sorted().collect( Collectors.joining( ", " ) ) );
        if ( platformConstraint != null ) {
            s.append( " " ).append( "[" ).append( platformConstraint ).append( "]" );
        }
        if ( taxon != null ) {
            s.append( " " ).append( "[" ).append( taxon ).append( "]" );
        }
        return s.toString();
    }
}