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
package ubic.gemma.analysis.expression.coexpression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ubic.basecode.dataStructure.CountingMap;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.util.Settings;

/**
 * @author luke
 * @version $Id$
 */
public class CoexpressionMetaValueObject {

    /**
     * The default maximum number of edges to send to the client.
     */
    public static final int DEFAULT_MAX_EDGES_PER_GRAPH = 2000;

    private static Logger log = LoggerFactory.getLogger( CoexpressionMetaValueObject.class );

    /**
     * The stringency used in the initial query and after 'global' trimming.
     */
    private int queryStringency;

    public int getQueryStringency() {
        return queryStringency;
    }

    public void setQueryStringency( int queryStringency ) {
        this.queryStringency = queryStringency;
    }

    /**
     * Error message for the client.
     */
    private String errorState;

    /**
     * Upper bound on how large we let the graph be from the server side. Can be overridden via the setter.
     */
    private int maxEdges = Settings.getInt( "gemma.cytoscapeweb.maxEdges",
            CoexpressionMetaValueObject.DEFAULT_MAX_EDGES_PER_GRAPH );

    /**
     * This will be greater than zero if the data were trimmed prior to sending to the client.
     */
    // private int nonQueryGeneTrimmedValue = 0;

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
     * Results for coexpression of the query gene with other 'found' genes (which could potentially include other query
     * genes)
     */
    private List<CoexpressionValueObjectExt> results;

    /**
     * The original search settings. FIXME we should not modify this?
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

    /**
     * How many edges total are we allowed to have in the graph? Above this, they can get trimmed.
     * 
     * @return
     */
    public int getMaxEdges() {
        return maxEdges;
    }

    // public int getNonQueryGeneTrimmedValue() {
    // return nonQueryGeneTrimmedValue;
    // }

    public int getNumDatasetsQueried() {
        return numDatasetsQueried;
    }

    public Collection<GeneValueObject> getQueryGenes() {
        return queryGenes;
    }

    /**
     * A sorted list of the results (sorting must be done elsewhere!)
     * 
     * @return
     */
    public List<CoexpressionValueObjectExt> getResults() {
        return results;
    }

    /**
     * The original search settings from the client. FIXME ??? Does not reflect any adjustments that might have been
     * made to the stringency
     * 
     * @return
     */
    public CoexpressionSearchCommand getSearchSettings() {
        return searchSettings;
    }

    public Map<Long, CoexpressionSummaryValueObject> getSummaries() {
        return summaries;
    }

    public boolean isQueryGenesOnly() {
        return queryGenesOnly;
    }

    public void setErrorState( String errorState ) {
        this.errorState = errorState;
    }

    /**
     * Override the value of gemma.cytoscapeweb.maxEdges (or DEFAULT_MAX_EDGES_PER_GRAPH )
     * 
     * @param maxEdges
     */
    public void setMaxEdges( int maxEdges ) {
        this.maxEdges = maxEdges;
    }

    //
    // public void setNonQueryGeneTrimmedValue( int nonQueryGeneTrimmedValue ) {
    // this.nonQueryGeneTrimmedValue = nonQueryGeneTrimmedValue;
    // }

    /**
     * @param numDatasetsQueried the number of data sets which were actually used in the query (a subset of those
     *        requested by the user if they didn't have coexpression analysis done)
     */
    public void setNumDatasetsQueried( int numDatasetsQueried ) {
        this.numDatasetsQueried = numDatasetsQueried;
    }

    public void setQueryGenes( Collection<GeneValueObject> queryGenes ) {
        this.queryGenes = queryGenes;
    }

    public void setQueryGenesOnly( boolean queryGenesOnly ) {
        this.queryGenesOnly = queryGenesOnly;
    }

    /**
     * @param results please be sorted.
     */
    public void setResults( List<CoexpressionValueObjectExt> results ) {
        this.results = results;
    }

    /**
     * @param searchSettings, should be the original settings
     */
    public void setSearchSettings( CoexpressionSearchCommand searchSettings ) {
        this.searchSettings = searchSettings;
    }

    /**
     * @param summary
     */
    public void setSummaries( Map<Long, CoexpressionSummaryValueObject> summary ) {
        this.summaries = summary;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for ( CoexpressionValueObjectExt ecvo : getResults() ) {
            buf.append( ecvo.toString() );
            buf.append( "\n" );
        }
        return buf.toString();
    }

    /**
     * This method just removes low stringency results until it goes below the limit, regardless of whether the genes
     * involved were query genes or not. This means, in effect, the original query could have been done at this higher
     * stringency.
     * <p>
     * The other trim method only removes non-query gene edges, so it is suitable for large 'query genes only'.
     */
    public void trim() {

        // sorted.
        List<CoexpressionValueObjectExt> geneResults = this.getResults();

        if ( geneResults == null ) {
            return;
        }

        if ( geneResults.size() <= this.getMaxEdges() ) return;

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

        log.info( "Original results size: " + geneResults.size() + " trimmed results size: "
                + strippedGeneResults.size() + "  Total results removed: "
                + ( geneResults.size() - strippedGeneResults.size() ) );

        Collections.sort( strippedGeneResults );
        this.setResults( strippedGeneResults );
        this.setQueryStringency( initialTrimStringency );

        if ( this.searchSettings != null ) this.searchSettings.setStringency( initialTrimStringency );

        trimUnusedSummaries();

    }

    // /**
    // * Reduce the size of the graph by preferentially removing links that don't involve the given genes. Used when
    // * running "query genes only" searches.
    // * <p>
    // * FIXME I don't see the main point of this.
    // *
    // * @param queryGeneIds
    // */
    // public void trim( Set<Long> queryGeneIds ) {
    //
    // // sorted in decreasing order of support.
    // List<CoexpressionValueObjectExt> geneResults = this.getResults();
    //
    // if ( geneResults.size() <= this.getMaxEdges() ) return;
    //
    // int startStringency = this.getSearchSettings().getStringency();
    //
    // /*
    // * Pick stringency that doesn't go over the limit.
    // */
    // CountingMap<Integer> supportDistribution = CoexpressionUtils.getSupportDistribution( geneResults );
    // int initialTrimStringency = findTrimStringency( startStringency, supportDistribution );
    //
    // // Map<Long, Integer> nodeDegreeDistribution = CoexpressionUtils.getNodeDegreeDistribution( geneResults );
    //
    // List<Integer> supports = new ArrayList<>( supportDistribution.keySet() );
    // Collections.sort( supports );
    //
    // /*
    // * haircut
    // */
    // // if ( geneResults.size() > this.getMaxEdges() ) {
    // // Set<Long> singletons = getGenesWithNodeDegree( nodeDegreeDistribution, 1 );
    // //
    // // /*
    // // * from the lowest stringency up
    // // *
    // // * FIXME involvement of query genes might not be a good criterion.
    // // */
    // // int tc = 0;
    // // int supportToTrim = supports.get( 0 );
    // // for ( int i = 0; i < supports.size(); i++ ) {
    // //
    // // supportToTrim = supports.get( i );
    // //
    // // for ( Iterator<CoexpressionValueObjectExt> iterator = geneResults.iterator(); iterator.hasNext(); ) {
    // // CoexpressionValueObjectExt cvoe = iterator.next();
    // // if ( cvoe.getSupport() > supportToTrim ) continue;
    // // if ( cvoe.involvesAny( queryGeneIds ) ) continue;
    // // if ( cvoe.involvesAny( singletons ) ) {
    // // iterator.remove();
    // // tc++;
    // // }
    // // }
    // //
    // // if ( geneResults.size() < this.getMaxEdges() ) break;
    // // }
    // //
    // // if ( tc > 0 ) {
    // // log.info( "Trimmed " + tc + " singletons at " + supportToTrim );
    // // }
    // //
    // // if ( supportToTrim > startStringency ) {
    // // trimStringency = supportToTrim;
    // // }
    // // }
    //
    // if ( initialTrimStringency > startStringency ) {
    // log.info( "Trim stringency will be " + initialTrimStringency + ", instead of " + startStringency );
    // } else {
    // // no trimming will happen, so we can bail. But we shouldn't get here.
    // // assert geneResults.size() <= this.getMaxEdges();
    // log.info( "No trimming required: " + geneResults.size() + " results." );
    // return;
    // }
    //
    // /*
    // * Now trim.
    // */
    // // for ( CoexpressionValueObjectExt cvoe : geneResults ) {
    // //
    // // if ( cvoe.getSupport() < trimStringency ) continue;
    // //
    // // Long g1 = cvoe.getQueryGene().getId();
    // // Long g2 = cvoe.getFoundGene().getId();
    // // boolean f = queryGeneIds.contains( g2 );
    // // boolean q = queryGeneIds.contains( g1 );
    // //
    // // if ( f || q ) {
    // // strippedGeneResults.add( cvoe );
    // //
    // // // get the non-query genes.
    // // if ( !f ) geneIds.add( g1 );
    // // if ( !q ) geneIds.add( g2 );
    // //
    // // } else {
    // // maybe.add( cvoe );
    // // }
    // //
    // // }
    //
    // /*
    // * First pass: Favor links that involve the query genes, until we hit the limit.
    // */
    // List<CoexpressionValueObjectExt> strippedGeneResults = new ArrayList<>();
    // Set<Long> geneIds = new HashSet<>();
    // Collection<CoexpressionValueObjectExt> maybe = new HashSet<>();
    // // int trimStringency = startStringency;
    // for ( CoexpressionValueObjectExt cvoe : geneResults ) {
    //
    // Long g1 = cvoe.getQueryGene().getId();
    // Long g2 = cvoe.getFoundGene().getId();
    // boolean f = queryGeneIds.contains( g2 );
    // boolean q = queryGeneIds.contains( g1 );
    //
    // if ( f || q ) {
    // // always keep the links that involve query genes.
    // strippedGeneResults.add( cvoe );
    //
    // /*
    // * Keep the gene as one to add more links to if it's above the trimStringency.
    // */
    // if ( f && q ) {
    // geneIds.add( g1 );
    // geneIds.add( g2 );
    // } else if ( f ) {
    // geneIds.add( g1 );
    // if ( cvoe.getSupport() >= initialTrimStringency ) {
    // geneIds.add( g2 );
    // }
    // } else if ( q ) {
    // geneIds.add( g2 );
    // if ( cvoe.getSupport() >= initialTrimStringency ) {
    // geneIds.add( g1 );
    // }
    // }
    // } else {
    // maybe.add( cvoe );
    // }
    //
    // // check if we identified the stringency threshold we want to use; we only set this once. Say the start
    // // stringency is 2. If we end up with enough results at stringency 10, we get the rest of the results for
    // // that stringency, but no more.
    // if ( initialTrimStringency == startStringency && strippedGeneResults.size() >= this.getMaxEdges() ) {
    // initialTrimStringency = cvoe.getSupport();
    // log.info( "Trim stringency raised to " + initialTrimStringency );
    // }
    // }
    //
    // /*
    // * Retain links that involve the genes included above, at the trim stringency.
    // */
    // for ( CoexpressionValueObjectExt cvoe : maybe ) {
    // if ( cvoe.getSupport() >= initialTrimStringency && geneIds.contains( cvoe.getFoundGene().getId() )
    // && geneIds.contains( cvoe.getQueryGene().getId() ) ) {
    // strippedGeneResults.add( cvoe );
    // }
    // }
    //
    // log.info( "Original results size: " + this.getResults().size() + " trimmed size: " + strippedGeneResults.size()
    // + "  Total results removed: " + ( this.getResults().size() - strippedGeneResults.size() ) );
    //
    // Collections.sort( strippedGeneResults );
    // this.setResults( strippedGeneResults );
    // // this.setTrimStringency( initialTrimStringency );
    //
    // trimUnusedSummaries();
    // }

    /**
     * 
     */
    private void trimUnusedSummaries() {
        if ( summaries == null ) return;

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

    // /**
    // * Find stringency that doesn't keep too many links.
    // *
    // * @param startStringency
    // * @param stringencyDist
    // * @return
    // */
    // private int findTrimStringency( int startStringency, CountingMap<Integer> stringencyDist ) {
    // int initTrimStringency = startStringency;
    // List<Integer> s = new ArrayList<>( stringencyDist.keySet() );
    // Collections.sort( s );
    // int total = 0;
    // // double SLOP = 1.2; // let us go over the limit a little.
    // for ( int i = s.size() - 1; i >= 0; i-- ) {
    //
    // int stringency = s.get( i );
    // int count = stringencyDist.get( stringency );
    //
    // int oldtot = total;
    // total += count;
    // if ( log.isDebugEnabled() )
    // log.debug( "Testing number of edges at stringency " + stringency + " = " + total );
    //
    // // if we're over, use the previous limit. Remember we're going to add more edges.
    // if ( oldtot > 0 && total > this.getMaxEdges() ) {
    // initTrimStringency = stringency + 1;
    // assert initTrimStringency >= startStringency;
    // break;
    // }
    //
    // }
    // return initTrimStringency;
    // }

}
