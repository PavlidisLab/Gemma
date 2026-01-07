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
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

import javax.annotation.Nullable;
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
public class SearchSettings {

    public enum SearchMode {
        /**
         * Very fast search mode, designed for exact matching of identifiers, etc.
         */
        EXACT,
        /**
         * Fast search mode, designed for autocompletion.
         */
        FAST,
        /**
         * Normal search mode with trade-offs to make it usable.
         */
        BALANCED,
        /**
         * Prefer correctness to speed.
         */
        ACCURATE;

        /**
         * Check if a search mode is at least as thorough as the given one.
         */
        public boolean isAtLeast( SearchMode other ) {
            return this.ordinal() >= other.ordinal();
        }

        /**
         * Check if a search mode is at most as thorough as the given one.
         */
        public boolean isAtMost( SearchMode searchMode ) {
            return this.ordinal() <= searchMode.ordinal();
        }
    }

    /**
     * How many results per result type are allowed. This implies that if you search for multiple types of things, you
     * can get more than this.
     */
    public static final int DEFAULT_MAX_RESULTS_PER_RESULT_TYPE = 5000;

    /**
     * Convenience method to get pre-configured settings.
     *
     * @param query query
     * @return search settings
     */
    public static SearchSettings arrayDesignSearch( String query ) {
        return builder().query( query ).resultType( ArrayDesign.class ).build();
    }

    /**
     * Convenience method to get pre-configured settings.
     *
     * @param query query
     * @return search settings
     */
    public static SearchSettings bibliographicReferenceSearch( String query ) {
        return builder().query( query ).resultType( BibliographicReference.class ).build();
    }

    /**
     * Convenience method to get pre-configured settings.
     *
     * @param query       query
     * @param arrayDesign the array design to limit the search to
     * @return search settings
     */
    public static SearchSettings compositeSequenceSearch( String query, @Nullable ArrayDesign arrayDesign ) {
        return builder().query( query )
                .resultType( CompositeSequence.class )
                .platformConstraint( arrayDesign ) // TODO: check if this was specified in the original code
                .build();
    }

    /**
     * Convenience method to get pre-configured settings.
     *
     * @param query query
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
     * @param query query
     * @param taxon if you want to filter by taxon (can be null)
     * @return search settings
     */
    public static SearchSettings expressionExperimentSearch( String query, @Nullable Taxon taxon ) {
        return builder()
                .query( query )
                .resultType( ExpressionExperiment.class )
                .taxonConstraint( taxon )
                .build();
    }

    /**
     * Convenience method to get pre-configured settings.
     *
     * @param query query
     * @param taxon the taxon to limit the search to (can be null)
     * @return search settings
     */
    public static SearchSettings geneSearch( String query, @Nullable Taxon taxon ) {
        return builder().query( query ).resultType( Gene.class ).taxonConstraint( taxon ).build();
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
    /**
     * Constraint results to be related to the given platform.
     */
    @Nullable
    private ArrayDesign platformConstraint;
    /**
     * Constraint results to be related to the given dataset.
     */
    @Nullable
    private ExpressionExperiment datasetConstraint;
    /**
     * Constraint results to to be related to the given taxon.
     */
    @Nullable
    private Taxon taxonConstraint;

    /* sources */
    /**
     * Use the database for finding results.
     */
    @Builder.Default
    private boolean useDatabase = true;
    /**
     * Use the ontology and ontology inference for finding results.
     * <p>
     * Results from the Gene Ontology (GO) are not included in this setting, use {@link #setUseGeneOntology(boolean)}
     * for that purpose.
     */
    @Builder.Default
    private boolean useOntology = true;
    /**
     * Include results from the Gene Ontology (GO).
     */
    @Builder.Default
    private boolean useGeneOntology = true;
    /**
     * Use the full-text index for finding results.
     */
    @Builder.Default
    private boolean useFullTextIndex = true;

    /**
     * Limit for the number of results per result type.
     * <p>
     * The default is relatively large and given by {@link #DEFAULT_MAX_RESULTS_PER_RESULT_TYPE}. Any value less than
     * one indicate no limit.
     */
    @Builder.Default
    private int maxResults = SearchSettings.DEFAULT_MAX_RESULTS_PER_RESULT_TYPE;

    /**
     * Indicate if result objects (i.e. {@link SearchResult#getResultObject()} should be filled, otherwise only the
     * class and ID will be populated.
     */
    @Builder.Default
    private boolean fillResults = true;

    /**
     * Indicate the search mode to use.
     * <p>
     * Defaults to balanced.
     */
    @Builder.Default
    private SearchMode mode = SearchMode.BALANCED;

    /**
     * Check if this is configured to search a given result type.
     */
    public boolean hasResultType( Class<?> cls ) {
        return resultTypes.contains( cls );
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder( "'" + query + "'" );
        s.append( " in " ).append( resultTypes.stream().map( Class::getSimpleName ).sorted().collect( Collectors.joining( ", " ) ) );
        if ( datasetConstraint != null ) {
            s.append( " " ).append( "[" ).append( datasetConstraint ).append( "]" );
        }
        if ( platformConstraint != null ) {
            s.append( " " ).append( "[" ).append( platformConstraint ).append( "]" );
        }
        if ( taxonConstraint != null ) {
            s.append( " " ).append( "[" ).append( taxonConstraint ).append( "]" );
        }
        if ( useDatabase ) {
            s.append( " [Use Database]" );
        }
        if ( useOntology ) {
            s.append( " [Use Ontology]" );
        }
        if ( useGeneOntology ) {
            s.append( " [Use Gene Ontology]" );
        }
        if ( useFullTextIndex ) {
            s.append( " [Use Full-Text Index]" );
        }
        s.append( " Max Results=" ).append( maxResults );
        if ( fillResults ) {
            s.append( " [Fill Results]" );
        }
        s.append( " Mode=" ).append( mode );
        return s.toString();
    }
}