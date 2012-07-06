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
package ubic.gemma.model.analysis.expression.coexpression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import ubic.gemma.model.genome.GeneImpl;

/**
 * The coexpressioncollectionValueObject is used for storing all the results of a coexpression search for one query
 * gene, across multiple experiments.
 * 
 * @author jsantos
 * @author klc
 * @version $Id$
 */
public class QueryGeneCoexpression {
    public static final String GENE_IMPL = GeneImpl.class.getSimpleName();

    @Override
    public void finalize() {
        if ( this.eesQueryTestedIn != null ) this.eesQueryTestedIn.clear();
        if ( this.queryProbes != null ) {
            for ( Long l : queryProbes.keySet() ) {
                queryProbes.get( l ).clear();
            }
            this.queryProbes.clear();
        }
        if ( geneCoexpressionData != null ) geneCoexpressionData.finalize();
    }

    private double postProcessSeconds;
    private double dbQuerySeconds;

    private Collection<Long> eesQueryTestedIn = null;

    private String errorState;

    private QueryGeneCoexpressionsDetails geneCoexpressionData;

    private Long queryGene;
    private int queryGeneGoTermCount;

    /**
     * EE -> Probes -> Genes
     */
    private Map<Long, Map<Long, Collection<Long>>> queryProbes;

    private int supportThreshold;

    /**
     * 
     */
    public QueryGeneCoexpression( Long queryGene, int supportThreshold ) {
        this.queryGene = queryGene;
        this.supportThreshold = supportThreshold;
        geneCoexpressionData = new QueryGeneCoexpressionsDetails( queryGene, supportThreshold );
        queryProbes = Collections.synchronizedMap( new HashMap<Long, Map<Long, Collection<Long>>>() );
    }

    public void add( CoexpressedGenePairValueObject vo ) {
        this.geneCoexpressionData.add( vo );
    }

    /**
     * @param coexpressedGeneId
     * @return
     */
    public boolean contains( Long coexpressedGeneId ) {
        return geneCoexpressionData.containsKey( coexpressedGeneId );
    }

    /**
     * @param coexpressedGeneId
     * @return coexpression results pertaining to the given gene, if any; or null otherwise
     */
    public CoexpressedGenePairValueObject get( Long coexpressedGeneId ) {
        if ( !this.contains( coexpressedGeneId ) ) return null;

        CoexpressedGenePairValueObject result = null;

        if ( geneCoexpressionData.containsKey( coexpressedGeneId ) ) {
            result = geneCoexpressionData.get( coexpressedGeneId );
        }

        return result;

    }

    /**
     * @param stringency enter 0 to get everything (entering 1 would have the same effect).
     * @return the standard Genes CoexpressionDataValueObjects for all types of genes, sorted by decreasing support
     */
    public List<CoexpressedGenePairValueObject> getAllGeneCoexpressionData( int stringency ) {
        List<CoexpressedGenePairValueObject> result = new ArrayList<CoexpressedGenePairValueObject>();
        result.addAll( this.geneCoexpressionData.getCoexpressionData( stringency ) );
        Collections.sort( result );
        return result;
    }

    /**
     * Gives the amount of time we had to wait for the database query.
     * 
     * @return
     */
    public double getDbQuerySeconds() {
        return dbQuerySeconds;
    }

    /**
     * @return
     */
    public Collection<Long> getEesQueryTestedIn() {
        return eesQueryTestedIn;
    }

    public String getErrorState() {
        return errorState;
    }

    /**
     * @return
     */
    public Collection<Long> getExpressionExperiments() {
        return this.geneCoexpressionData.getExpressionExperimentIds();
    }

    /**
     * @return the CoexpressedGenesDetails for standard genes
     */
    public QueryGeneCoexpressionsDetails getGeneCoexpression() {
        return this.geneCoexpressionData;
    }

    /**
     * @return the coexpressionData for genes in order of decreasing support
     */
    public List<CoexpressedGenePairValueObject> getGeneCoexpressionData( int s ) {
        return this.geneCoexpressionData.getCoexpressionData( s );
    }

    /**
     * @return the number of Genes coexpressed with the query
     */
    public int getNumGenes() {
        return this.geneCoexpressionData.getNumberOfGenes();
    }

    /**
     * @return the numStringencyGenes - for genes.
     */
    public int getNumStringencyGenes() {
        return this.geneCoexpressionData.getNumberOfGenes();
    }

    public double getPostProcessSeconds() {
        return postProcessSeconds;
    }

    /**
     * @return the queryGene
     */
    public Long getQueryGene() {
        return queryGene;
    }

    public int getQueryGeneGoTermCount() {
        return queryGeneGoTermCount;
    }

    /**
     * @return a unique collection of probeIDs that the queried gene was expressed from
     */
    public Collection<Long> getQueryGeneProbes() {
        Collection<Long> results = new HashSet<Long>();
        synchronized ( queryProbes ) {
            for ( Long eeID : queryProbes.keySet() ) {
                results.addAll( queryProbes.get( eeID ).keySet() );
            }
        }

        return results;
    }

    /**
     * @return the stringency Filter Value
     */
    public int getStringency() {
        return supportThreshold;
    }

    /**
     * @return all the probes for all the target genes.
     */
    public Collection<Long> getTargetGeneProbes() {
        Collection<Long> result = new HashSet<Long>();
        List<CoexpressedGenePairValueObject> data = this.getAllGeneCoexpressionData( 0 );
        for ( CoexpressedGenePairValueObject coVo : data ) {
            for ( Long ee : coVo.getExpressionExperiments() ) {
                result.addAll( coVo.getProbes( ee ) );
            }
        }
        return result;
    }

    /**
     * Merely initializes the data structure.
     * 
     * @param eeID the expressionexperiment id
     * @param probeID the probe ID for the query gene.
     */
    public void initializeSpecificityDataStructure( Long eeID, Long probeID ) {
        if ( !queryProbes.containsKey( eeID ) ) {
            queryProbes.put( eeID, new HashMap<Long, Collection<Long>>() );
        }
        Map<Long, Collection<Long>> probe2geneMap = queryProbes.get( eeID );
        if ( !probe2geneMap.containsKey( probeID ) ) {
            probe2geneMap.put( probeID, new HashSet<Long>() );
        }
    }

    /**
     * Set the amount of time we had to wait for the query (mostly database access time)
     * 
     * @param elapsedWallTime (in milliseconds)
     */
    public void setDbQuerySeconds( double elapsedWallMillisSeconds ) {
        this.dbQuerySeconds = elapsedWallMillisSeconds / 1000.0;
    }

    /**
     * @param eesQueryTestedIn
     */
    public void setEesQueryGeneTestedIn( Collection<Long> eesQueryTestedIn ) {
        this.eesQueryTestedIn = eesQueryTestedIn;
    }

    /**
     * @param errorState
     */
    public void setErrorState( String errorState ) {
        this.errorState = errorState;
    }

    /**
     * @param elapsed
     */
    public void setPostProcessTime( Long elapsed ) {
        this.postProcessSeconds = elapsed / 1000.0;

    }

    public void setQueryGeneGoTermCount( int queryGeneGoTermCount ) {
        this.queryGeneGoTermCount = queryGeneGoTermCount;
    }

    /**
     * Given a list of probes to genes adds this information to the crossHybridizingQueryProbes allowing for the
     * specificity of the queried gene to be determined on a expression experiment level.
     * 
     * @param probe2GeneMap
     */
    public void setQueryGeneSpecifityInfo( Map<Long, Collection<Long>> probe2GeneMap ) {

        /*
         * For each experiment, for each probe
         */
        for ( Long eeID : queryProbes.keySet() ) {

            /*
             * Store the details.
             */
            addQuerySpecificityData( eeID, probe2GeneMap );

            Map<Long, Collection<Long>> probe2genes = queryProbes.get( eeID );
            for ( Long probeID : probe2genes.keySet() ) {
                Collection<Long> genes = probe2genes.get( probeID );

                Collection<Long> genesForProbe = probe2GeneMap.get( probeID );

                // defensive; shouldn't happen.
                if ( genesForProbe == null ) continue;

                for ( Long g : genesForProbe ) {
                    genes.add( g );
                }

                /*
                 * If the probe maps to just one gene, then we're okay. Note that this check is a little paranoid: if
                 * there is only one gene, then it had better be the query gene!
                 */
                if ( genes.size() == 1 && genes.iterator().next().equals( queryGene ) ) {

                    if ( this.geneCoexpressionData.hasExpressionExperiment( eeID ) )
                        this.geneCoexpressionData.setHasProbeSpecificForQueryGene( eeID );

                }
            }

        }
    }

    public void setTargetGeneSpecificityInfo( Map<Long, Collection<Long>> probe2GeneMap ) {
        for ( Long eeID : queryProbes.keySet() ) {
            addTargetSpecificityData( eeID, probe2GeneMap );
        }
    }

    private void addQuerySpecificityData( Long eeID, Map<Long, Collection<Long>> probe2GeneMap ) {
        // if ( this.geneCoexpressionData.hasExpressionExperiment( eeID ) ) {
        this.geneCoexpressionData.addQuerySpecificityInfo( eeID, probe2GeneMap );
        // }
    }

    private void addTargetSpecificityData( Long eeID, Map<Long, Collection<Long>> probe2GeneMap ) {
        // if ( this.geneCoexpressionData.hasExpressionExperiment( eeID ) ) {
        this.geneCoexpressionData.addTargetSpecificityInfo( eeID, probe2GeneMap );
        // }
    }

}
