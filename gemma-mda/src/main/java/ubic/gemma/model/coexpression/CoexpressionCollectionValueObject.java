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
package ubic.gemma.model.coexpression;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;

/**
 * @author jsantos, klc The coexpressioncollectionValueObject is used for storing the results of a coexpression search.
 *         The object is thread safe.
 */
public class CoexpressionCollectionValueObject {

    private static Log log = LogFactory.getLog( CoexpressionCollectionValueObject.class.getName() );

    private int linkCount; // the total number of links for this specific coexpression
    private int positiveStringencyLinkCount; // the number of links for this coexpression that passed the stringency
    // requirements
    private int negativeStringencyLinkCount;
    private Map<Long, ExpressionExperimentValueObject> expressionExperiments; // the expression experiments that were used.

    // the number of actual genes, predicted genes, and probe aligned regions in the query, unfiltered by stringency
    private int numGenes;
    private int numPredictedGenes;
    private int numProbeAlignedRegions;

    // the number of actual genes, predicted genes, and probe aligned regions in the query, filtered by stringency
    private int stringencyFilterValue;
    private int numStringencyGenes;
    private int numStringencyPredictedGenes;
    private int numStringencyProbeAlignedRegions;

    // involved in the query
    private Collection<CoexpressionValueObject> coexpressionData;
    private Collection<CoexpressionValueObject> predictedCoexpressionData;
    private Collection<CoexpressionValueObject> alignedCoexpressionData;

    private double firstQuerySeconds;
    private double secondQuerySeconds;
    private double postProcessSeconds;
    private double elapsedWallSeconds;

    // Map<eeID, Map<probeID, Collection<geneID>>>
    private Map<Long, Map<Long, Collection<Long>>> crossHybridizingProbes; // this is raw data before stringincy is
    private Map<Long, Map<Long, Collection<Long>>> crossHybridizingQueryProbes; // This will hold info pertaining to the

    private Gene queryGene;

    /**
     * This gives the amount of time we had to wait for the queries (which can be less than the time per query because
     * of threading)
     * 
     * @return
     */
    public double getElapsedWallSeconds() {
        return elapsedWallSeconds;
    }

    /**
     * Set the amount of time we had to wait for the queries (which can be less than the time per query because
     * 
     * @param elapsedWallTime (in milliseconds)
     */
    public void setElapsedWallTimeElapsed( double elapsedWallMillisSeconds ) {
        this.elapsedWallSeconds = elapsedWallMillisSeconds / 1000.0;
    }

    public CoexpressionCollectionValueObject() {
        linkCount = 0;
        positiveStringencyLinkCount = 0;
        negativeStringencyLinkCount = 0;
        numGenes = 0;
        numPredictedGenes = 0;
        numProbeAlignedRegions = 0;

        numStringencyGenes = 0;
        numStringencyProbeAlignedRegions = 0;
        numStringencyPredictedGenes = 0;        

        coexpressionData = Collections.synchronizedSet( new HashSet<CoexpressionValueObject>() );
        predictedCoexpressionData = Collections.synchronizedSet( new HashSet<CoexpressionValueObject>() );
        alignedCoexpressionData = Collections.synchronizedSet( new HashSet<CoexpressionValueObject>() );

        expressionExperiments = Collections.synchronizedMap( new HashMap<Long, ExpressionExperimentValueObject>() );
        crossHybridizingProbes = Collections.synchronizedMap( new HashMap<Long, Map<Long, Collection<Long>>>() );
        crossHybridizingQueryProbes = Collections.synchronizedMap( new HashMap<Long, Map<Long, Collection<Long>>>() );
    }

    /**
     * @param eeID
     * @param probeID
     * @param geneID
     */
    public void addSpecifityInfo( Long eeID, Long probeID, Long geneID ) {
        if ( crossHybridizingProbes.containsKey( eeID ) ) {
            Map<Long, Collection<Long>> probe2geneMap = crossHybridizingProbes.get( eeID );
            if ( probe2geneMap.containsKey( probeID ) )
                probe2geneMap.get( probeID ).add( geneID );
            else {
                Collection<Long> genes = Collections.synchronizedSet( new HashSet<Long>() );
                genes.add( geneID );
                probe2geneMap.put( probeID, genes );
            }
        } else {
            Map<Long, Collection<Long>> probe2geneMap = Collections
                    .synchronizedMap( new HashMap<Long, Collection<Long>>() );
            Collection<Long> genes = Collections.synchronizedSet( new HashSet<Long>() );
            genes.add( geneID );
            probe2geneMap.put( probeID, genes );
            crossHybridizingProbes.put( eeID, probe2geneMap );

        }
    }

    /**
     * @return returns map of genes to a collection of expression experiment IDs that contained <strong>specific</strong>
     *         probes (probes that hit only 1 gene) for that gene.
     *         <p>
     *         If an expression exp has two (or more) probes that hit the same gene, and one probe is specific, even if
     *         some of the other(s) are not this EE is considered specific and will still be returned.
     */
    public Map<Long, Collection<Long>> getSpecificExpressionExperiments() {

        Map<Long, Collection<Long>> specificEE = Collections.synchronizedMap( new HashMap<Long, Collection<Long>>() );

        synchronized ( crossHybridizingProbes ) {
            for ( Long eeID : crossHybridizingProbes.keySet() ) {

                // this is a map for ALL the probes from this data set that came up.
                Map<Long, Collection<Long>> probe2geneMap = crossHybridizingProbes.get( eeID );

                for ( Long probeID : probe2geneMap.keySet() ) {

                    Collection<Long> genes = probe2geneMap.get( probeID );
                    Integer genecount = genes.size();

                    for ( Long geneId : genes ) {

                        if ( !specificEE.containsKey( geneId ) ) {
                            specificEE.put( geneId, new HashSet<Long>() );
                        }

                        if ( genecount == 1 ) {
                            specificEE.get( geneId ).add( eeID );
                        }
                    }

                }

            }
        }

        return specificEE;
    }

    /**
     * @param geneIDs The gene IDs to consider.
     * @return returns a collection of expression experiment IDs that have <strong>specific</strong> probes (probes
     *         that hit only 1 gene) for the query gene.
     *         <p>
     *         If an expression exp has two (or more) probes that hit the same gene, and one probe is specific, even if
     *         some of the other(s) are not this EE is considered specific and will still be returned.
     */
    public Collection<Long> getQueryGeneSpecificExpressionExperiments() {

        Collection<Long> specificEE = Collections.synchronizedSet( new HashSet<Long>() );

        if ( queryGene == null ) return null;

        synchronized ( crossHybridizingQueryProbes ) {
            for ( Long eeID : crossHybridizingQueryProbes.keySet() ) {

                // this is a map for ALL the probes from this data set that came up.
                Map<Long, Collection<Long>> probe2geneMap = crossHybridizingQueryProbes.get( eeID );

                for ( Long probeID : probe2geneMap.keySet() ) {

                    Collection<Long> genes = probe2geneMap.get( probeID );

                    if ( ( genes.size() == 1 ) && ( genes.iterator().next() == queryGene.getId() ) ) {
                        log.info( "Expression Experiment: + " + eeID + " is specific" );
                        specificEE.add( eeID );
                    }
                }

            }
        }
        return specificEE;
    }

    /**
     * @param eeID
     * @returns a collection of Probe IDs for a given expression experiment that hybrydized to more than 1 gene
     */
    public Collection<Long> getNonSpecificProbes( Long eeID ) {
        Collection<Long> nonSpecificProbes = Collections.synchronizedSet( new HashSet<Long>() );

        Map<Long, Collection<Long>> probe2geneMap = crossHybridizingProbes.get( eeID );
        synchronized ( probe2geneMap ) {
            for ( Long probeID : probe2geneMap.keySet() ) {
                Collection genes = probe2geneMap.get( probeID );
                if ( genes.size() > 1 ) nonSpecificProbes.add( eeID );
            }
        }
        return nonSpecificProbes;
    }

    /**
     * @param eeID
     * @param probeID
     * @return a collection of gene IDs or null if the eeID and probeID were not found
     */
    public Collection<Long> getNonSpecificGenes( Long eeID, Long probeID ) {

        if ( crossHybridizingProbes.containsKey( eeID ) )
            if ( crossHybridizingProbes.get( eeID ).containsKey( probeID ) )
                return crossHybridizingProbes.get( eeID ).get( probeID );

        return null;
    }

    /**
     * @return the coexpressionData
     */
    public Collection<CoexpressionValueObject> getCoexpressionData() {
        return coexpressionData;
    }

    /**
     * @param coexpressionData the coexpressionData to set
     */
    public void setCoexpressionData( Collection<CoexpressionValueObject> coexpressionData ) {
        this.coexpressionData = coexpressionData;
    }

    /**
     * @return the linkCount
     */
    public int getLinkCount() {
        return linkCount;
    }

    /**
     * @param linkCount the linkCount to set
     */
    public void setLinkCount( int linkCount ) {
        this.linkCount = linkCount;
    }

    /**
     * @return the stringencyLinkCount
     */
    public int getPositiveStringencyLinkCount() {
        return positiveStringencyLinkCount;
    }

    /**
     * @param stringencyLinkCount the stringencyLinkCount to set
     */
    public void setPositiveStringencyLinkCount( int stringencyLinkCount ) {
        this.positiveStringencyLinkCount = stringencyLinkCount;
    }

    /**
     * @return the stringencyLinkCount
     */
    public int getNegativeStringencyLinkCount() {
        return negativeStringencyLinkCount;
    }

    /**
     * @param stringencyLinkCount the stringencyLinkCount to set
     */
    public void setNegativeStringencyLinkCount( int stringencyLinkCount ) {
        this.negativeStringencyLinkCount = stringencyLinkCount;
    }

    /**
     * @return the expressionExperiments that were searched for coexpression
     */
    public Collection<ExpressionExperimentValueObject> getExpressionExperiments() {
        return expressionExperiments.values();
    }

    /**
     * Add an expression experiment to the list
     * 
     * @param vo
     */
    public void addExpressionExperiment( ExpressionExperimentValueObject vo ) {
        Long id = vo.getId();
        if ( !expressionExperiments.containsKey( id ) ) this.expressionExperiments.put( id, vo );
    }

    /**
     * @param eeID expressionExperiment ID
     * @return an expressionexperimentValueObject or null if it isn't there
     */
    public ExpressionExperimentValueObject getExpressionExperiment( Long eeID ) {

        if ( expressionExperiments.containsKey( eeID ) ) return this.expressionExperiments.get( eeID );

        return null;
    }

    /**
     * Add a collection of expression experiment to the list
     * 
     * @param vo
     */
    public void addExpressionExperiments( Collection<ExpressionExperimentValueObject> vos ) {
        synchronized ( vos ) {
            for ( ExpressionExperimentValueObject eeVo : vos )
                addExpressionExperiment( eeVo );
        }
    }

    public void setFirstQueryElapsedTime( Long elapsed ) {
        this.firstQuerySeconds = elapsed / 1000.0;

    }

    public void setSecondQueryElapsedTime( Long elapsed ) {
        this.secondQuerySeconds = elapsed / 1000.0;

    }

    public void setPostProcessTime( Long elapsed ) {
        this.postProcessSeconds = elapsed / 1000.0;

    }

    public double getFirstQuerySeconds() {
        return firstQuerySeconds;
    }

    public double getPostProcessSeconds() {
        return postProcessSeconds;
    }

    public double getSecondQuerySeconds() {
        return secondQuerySeconds;
    }

    /**
     * @return the numGenes
     */
    public int getNumGenes() {
        return numGenes;
    }

    /**
     * @param numGenes the numGenes to set
     */
    public void setNumGenes( int numGenes ) {
        this.numGenes = numGenes;
    }

    /**
     * @return the numPredictedGenes
     */
    public int getNumPredictedGenes() {
        return numPredictedGenes;
    }

    /**
     * @param numPredictedGenes the numPredictedGenes to set
     */
    public void setNumPredictedGenes( int numPredictedGenes ) {
        this.numPredictedGenes = numPredictedGenes;
    }

    /**
     * @return the numProbeAlignedRegions
     */
    public int getNumProbeAlignedRegions() {
        return numProbeAlignedRegions;
    }

    /**
     * @param numProbeAlignedRegions the numProbeAlignedRegions to set
     */
    public void setNumProbeAlignedRegions( int numProbeAlignedRegions ) {
        this.numProbeAlignedRegions = numProbeAlignedRegions;
    }

    /**
     * @return the numStringencyGenes
     */
    public int getNumStringencyGenes() {
        return numStringencyGenes;
    }

    /**
     * @param numStringencyGenes the numStringencyGenes to set
     */
    public void setNumStringencyGenes( int numStringencyGenes ) {
        this.numStringencyGenes = numStringencyGenes;
    }

    /**
     * @return the numStringencyPredictedGenes
     */
    public int getNumStringencyPredictedGenes() {
        return numStringencyPredictedGenes;
    }

    /**
     * @param numStringencyPredictedGenes the numStringencyPredictedGenes to set
     */
    public void setNumStringencyPredictedGenes( int numStringencyPredictedGenes ) {
        this.numStringencyPredictedGenes = numStringencyPredictedGenes;
    }

    /**
     * @return the numStringencyProbeAlignedRegions
     */
    public int getNumStringencyProbeAlignedRegions() {
        return numStringencyProbeAlignedRegions;
    }

    /**
     * @param numStringencyProbeAlignedRegions the numStringencyProbeAlignedRegions to set
     */
    public void setNumStringencyProbeAlignedRegions( int numStringencyProbeAlignedRegions ) {
        this.numStringencyProbeAlignedRegions = numStringencyProbeAlignedRegions;
    }

    public int getNumberOfUsedExpressonExperiments() {
        return crossHybridizingProbes.keySet().size();

    }

    /**
     * @param id
     * @return an int representing the raw number of links a given ee contributed to the coexpression search
     */
    public Long getRawLinkCountForEE( Long id ) {

        ExpressionExperimentValueObject eeVo = expressionExperiments.get( id );

        if ( eeVo.getRawCoexpressionLinkCount() == null )
            return ( long ) 0;
        else
            return eeVo.getRawCoexpressionLinkCount();
    }

    public Long getLinkCountForEE( Long id ) {

        ExpressionExperimentValueObject eeVo = expressionExperiments.get( id );

        if ( eeVo.getCoexpressionLinkCount() == null )
            return ( long ) 0;
        else
            return eeVo.getCoexpressionLinkCount();

    }

    /**
     * @return the stringencyFilterValue
     */
    public int getStringency() {
        return stringencyFilterValue;
    }

    /**
     * @param stringencyFilterValue the stringencyFilterValue to set
     */
    public void setStringency( int stringency ) {
        this.stringencyFilterValue = stringency;
    }

    /**
     * @return the predictedCoexpressionData
     */
    public Collection<CoexpressionValueObject> getPredictedCoexpressionData() {
        return predictedCoexpressionData;
    }

    /**
     * @param predictedCoexpressionData the predictedCoexpressionData to set
     */
    public void setPredictedCoexpressionData( Collection<CoexpressionValueObject> predictedCoexpressionData ) {
        this.predictedCoexpressionData = predictedCoexpressionData;
    }

    /**
     * @return the alignedCoexpressionData
     */
    public Collection<CoexpressionValueObject> getAlignedCoexpressionData() {
        return alignedCoexpressionData;
    }

    /**
     * @param alignedCoexpressionData the alignedCoexpressionData to set
     */
    public void setAlignedCoexpressionData( Collection<CoexpressionValueObject> alignedCoexpressionData ) {
        this.alignedCoexpressionData = alignedCoexpressionData;
    }

    /**
     * @param eeID the expressionexperiment id
     * @param probeID the probe ID Keeps track of which probe (CS) from what EE was responsible for expressing the
     *        queried gene
     */

    public void addQuerySpecifityInfo( Long eeID, Long probeID ) {

        if ( crossHybridizingQueryProbes.containsKey( eeID ) ) {
            Map<Long, Collection<Long>> probe2geneMap = crossHybridizingQueryProbes.get( eeID );
            if ( !probe2geneMap.containsKey( probeID ) ) {
                Collection<Long> genes = Collections.synchronizedSet( new HashSet<Long>() );
                probe2geneMap.put( probeID, genes );
            }
        } else {
            Map<Long, Collection<Long>> probe2geneMap = Collections
                    .synchronizedMap( new HashMap<Long, Collection<Long>>() );
            Collection<Long> genes = Collections.synchronizedSet( new HashSet<Long>() );
            probe2geneMap.put( probeID, genes );
            crossHybridizingQueryProbes.put( eeID, probe2geneMap );
        }
    }

    /*
     * Given a list of probes to genes adds this information to the crossHybridizingQueryProbes allowing for the
     * specificty of the queried gene to be determined on a expression experiment level.
     */
    public void addQueryGeneSpecifityInfo( Map<Long, Collection<Gene>> probe2GeneMap ) {

        synchronized ( crossHybridizingQueryProbes ) {
            for ( Long eeID : crossHybridizingQueryProbes.keySet() ) {
                Map<Long, Collection<Long>> probe2genes = crossHybridizingQueryProbes.get( eeID );
                for ( Long probeID : probe2genes.keySet() ) {
                    Collection<Long> genes = probe2genes.get( probeID );
                    for ( Gene g : probe2GeneMap.get( probeID ) )
                        genes.add( g.getId() );

                    if ( ( genes.size() == 1 ) && ( genes.iterator().next() == queryGene.getId() ) ) {
                        getExpressionExperiment( eeID ).setSpecific( true );
                    }
                }
            }
        }
    }

    /**
     * @return a unique collection of probeIDs that the queried gene was expressed from
     */
    public Collection<Long> getQueryGeneProbes() {

        Collection<Long> results = Collections.synchronizedSet( new HashSet<Long>() );
        synchronized ( crossHybridizingQueryProbes ) {
            for ( Long eeID : crossHybridizingQueryProbes.keySet() )
                results.addAll( crossHybridizingQueryProbes.get( eeID ).keySet() );
        }

        return results;
    }

    /**
     * @return the queryGene
     */
    public Gene getQueryGene() {
        return queryGene;
    }

    /**
     * @param queryGene the queryGene to set
     */
    public void setQueryGene( Gene queryGene ) {
        this.queryGene = queryGene;
    }
}
