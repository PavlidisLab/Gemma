/*
 * The Gemma project
 *
 * Copyright (c) 2008-2010 University of British Columbia
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
package ubic.gemma.core.analysis.expression.coexpression;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.core.config.Settings;

import java.io.Serializable;
import java.util.*;

/**
 * @author luke
 */
@CommonsLog
@SuppressWarnings({ "unused", "WeakerAccess" }) // Frontend use
public class CoexpressionMetaValueObject implements Serializable {

    /**
     * The default maximum number of edges to send to the client.
     */
    private static final int DEFAULT_MAX_EDGES_PER_GRAPH = 2000;

    /**
     * Error message for the client.
     */
    private String errorState;

    /**
     * Upper bound on how large we let the graph be from the server side. Can be overridden via the setter.
     */
    private int maxEdges = Settings
            .getInt( "gemma.cytoscapeweb.maxEdges", CoexpressionMetaValueObject.DEFAULT_MAX_EDGES_PER_GRAPH );

    /**
     * How many data sets were actually used in the query; that is, which had coexpression analysis done.
     */
    private int numDatasetsQueried = 0;

    private Collection<GeneValueObject> queryGenes;

    /**
     * if this was a "query genes only" search
     */
    private boolean queryGenesOnly = false;

    /**
     * The stringency used in the initial query and after 'global' trimming.
     */
    private int queryStringency;

    /**
     * Results for coexpression of the query gene with other 'found' genes (which could potentially include other query
     * genes)
     */
    private List<CoexpressionValueObjectExt> results;

    /**
     * The original search settings.
     */
    private CoexpressionSearchCommand searchSettings;

    /**
     * Summaries of results for each gene (map of gene ids to CoexpressionSummaryValueObject)
     */
    private Map<Long, CoexpressionSummaryValueObject> summaries;

    public CoexpressionMetaValueObject() {
        super();
    }

    public String getErrorState() {
        return errorState;
    }

    public void setErrorState( String errorState ) {
        this.errorState = errorState;
    }

    /**
     * @return How many edges total are we allowed to have in the graph? Above this, they can get trimmed.
     */
    public int getMaxEdges() {
        return maxEdges;
    }

    /**
     * Override the value of gemma.cytoscapeweb.maxEdges (or DEFAULT_MAX_EDGES_PER_GRAPH )
     *
     * @param maxEdges max edges
     */
    public void setMaxEdges( int maxEdges ) {
        this.maxEdges = maxEdges;
    }

    public int getNumDatasetsQueried() {
        return numDatasetsQueried;
    }

    /**
     * @param numDatasetsQueried the number of data sets which were actually used in the query (a subset of those
     *                           requested by the user if they didn't have coexpression analysis done)
     */
    public void setNumDatasetsQueried( int numDatasetsQueried ) {
        this.numDatasetsQueried = numDatasetsQueried;
    }

    public Collection<GeneValueObject> getQueryGenes() {
        return queryGenes;
    }

    public void setQueryGenes( Collection<GeneValueObject> queryGenes ) {
        this.queryGenes = queryGenes;
    }

    public int getQueryStringency() {
        return queryStringency;
    }

    public void setQueryStringency( int queryStringency ) {
        this.queryStringency = queryStringency;
    }

    /**
     * A sorted list of the results (sorting must be done elsewhere!)
     *
     * @return coexp VOs
     */
    public List<CoexpressionValueObjectExt> getResults() {
        return results;
    }

    /**
     * @param results please be sorted.
     */
    public void setResults( List<CoexpressionValueObjectExt> results ) {
        this.results = results;
    }

    /**
     * The original search settings from the client.
     * made to the stringency
     *
     * @return coexp search command
     */
    public CoexpressionSearchCommand getSearchSettings() {
        return searchSettings;
    }

    /**
     * @param searchSettings, should be the original settings
     */
    public void setSearchSettings( CoexpressionSearchCommand searchSettings ) {
        this.searchSettings = searchSettings;
    }

    public Map<Long, CoexpressionSummaryValueObject> getSummaries() {
        return summaries;
    }

    public void setSummaries( Map<Long, CoexpressionSummaryValueObject> summary ) {
        this.summaries = summary;
    }

    public boolean isQueryGenesOnly() {
        return queryGenesOnly;
    }

    public void setQueryGenesOnly( boolean queryGenesOnly ) {
        this.queryGenesOnly = queryGenesOnly;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for ( CoexpressionValueObjectExt ecvo : this.getResults() ) {
            buf.append( ecvo.toString() );
            buf.append( "\n" );
        }
        return buf.toString();
    }

    /**
     * This method just removes low stringency results until it goes below the limit, regardless of whether the genes
     * involved were query genes or not. This means, in effect, the original query could have been done at this higher
     * stringency.
     * The other trim method only removes non-query gene edges, so it is suitable for large 'query genes only'.
     * Warning, this method can remove results that user may have expected.
     *
     * @deprecated because this is too ad hoc and messy
     */
    @Deprecated
    public void trim() {

        // sorted.
        List<CoexpressionValueObjectExt> geneResults = this.getResults();

        if ( geneResults == null ) {
            return;
        }

        if ( geneResults.size() <= this.getMaxEdges() )
            return;

        int startStringency = this.queryStringency;
        int initialTrimStringency = startStringency;

        List<CoexpressionValueObjectExt> strippedGeneResults = new ArrayList<>();

        for ( CoexpressionValueObjectExt cvoe : geneResults ) {
            if ( cvoe.getSupport() >= initialTrimStringency ) {
                strippedGeneResults.add( cvoe );
            }

            // check if we identified the stringency threshold we want to use; we only set this once. Say the start
            // stringency is 2. If we end up with enough results at stringency 10, we get the rest of the results for
            // that stringency, but no more. Unfortunately this means we can get too many results, still.
            if ( initialTrimStringency == startStringency && strippedGeneResults.size() >= this.getMaxEdges() ) {
                initialTrimStringency = cvoe.getSupport();
            }
        }

        assert initialTrimStringency >= startStringency;

        CoexpressionMetaValueObject.log
                .info( "Original results size: " + geneResults.size() + " trimmed results size: " + strippedGeneResults
                        .size() + "  Total results removed: " + ( geneResults.size() - strippedGeneResults.size() ) );

        Collections.sort( strippedGeneResults );
        this.setResults( strippedGeneResults );
        this.setQueryStringency( initialTrimStringency );

        if ( this.searchSettings != null )
            this.searchSettings.setStringency( initialTrimStringency );

        this.trimUnusedSummaries();

    }

    private void trimUnusedSummaries() {
        if ( summaries == null )
            return;

        /*
         * remove irrelevant gene summaries.
         */
        Set<Long> usedGenes = new HashSet<>();
        for ( CoexpressionValueObjectExt r : this.getResults() ) {
            usedGenes.add( r.getQueryGene().getId() );
        }

        Set<Long> unusedGenes = new HashSet<>();
        for ( Long g : summaries.keySet() ) {
            if ( !usedGenes.contains( g ) ) {
                unusedGenes.add( g );
            }
        }

        for ( Long g : unusedGenes ) {
            summaries.remove( g );
        }
    }

}
