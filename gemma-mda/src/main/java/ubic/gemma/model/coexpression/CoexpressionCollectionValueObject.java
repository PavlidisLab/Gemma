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

/**
 * @author jsantos
 */
public class CoexpressionCollectionValueObject {

    private static Log log = LogFactory.getLog( CoexpressionCollectionValueObject.class.getName() );

    private int linkCount; // the total number of links for this specific coexpression
    private int positiveStringencyLinkCount; // the number of links for this coexpression that passed the stringency
    // requirements
    private int negativeStringencyLinkCount;
    private Map<Long, ExpressionExperimentValueObject> expressionExperiments; // the expression experiments that were

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
    private double firstQuerySeconds;
    private double secondQuerySeconds;
    private double postProcessSeconds;
    private double elapsedWallSeconds;

    private Map<Long, Map<Long, Collection<Long>>> crossHybridizingProbes; // this is raw data before stringincy is

    // applied

    // Map <expressionExperimentID, Map<probeId,<collection<geneID>>

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

        coexpressionData = new HashSet<CoexpressionValueObject>();
        expressionExperiments = new HashMap<Long, ExpressionExperimentValueObject>();
        crossHybridizingProbes = Collections.synchronizedMap( new HashMap<Long, Map<Long, Collection<Long>>>() );
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
     * @return returns a collection expression experiment IDs that contained non-specific probes (probes that hit more
     *         than 1 gene) Note: if a expression exp has two probes that hit the same gene. One probe is specific and
     *         the other is not, this EE is considered specific and won't be returned in the list
     */
    public Collection<Long> getNonSpecificExpressionExperiments( Long geneID ) {

        Collection<Long> nonSpecificEE = new HashSet<Long>();

        for ( Long eeID : crossHybridizingProbes.keySet() ) {
            Map<Long, Collection<Long>> probe2geneMap = crossHybridizingProbes.get( eeID );
            // log.info( "Non-specifity validaton for EE: " + eeID + " and gene: " + geneID);

            for ( Long probeID : probe2geneMap.keySet() ) {
                Collection genes = probe2geneMap.get( probeID );

                if ( !genes.contains( geneID ) ) continue;

                if ( ( genes.size() == 1 ) ) {
                    nonSpecificEE.remove( eeID );
                    // log.info( "EE has specific probe: " + probeID + " for Gene: " + genes );
                    break;
                }

                nonSpecificEE.add( eeID );
                // log.info( "EE has NON-specific probe: " + probeID + " for Gene: " + geneID + " in " + genes );
            }

        }

        return nonSpecificEE;
    }

    /**
     * @param eeID
     * @returns a collection of Probe IDs for a given expression experiment that hybrydized to more than 1 gene
     */
    public Collection<Long> getNonSpecificProbes( Long eeID ) {
        Collection<Long> nonSpecificProbes = new HashSet<Long>();

        Map<Long, Collection<Long>> probe2geneMap = crossHybridizingProbes.get( eeID );

        for ( Long probeID : probe2geneMap.keySet() ) {
            Collection genes = probe2geneMap.get( probeID );
            if ( genes.size() > 1 ) nonSpecificProbes.add( eeID );
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
        Long id = Long.parseLong( vo.getId() );
        if (!expressionExperiments.containsKey( id ))
            this.expressionExperiments.put( id , vo );
    }
    
    /**
     * @param eeID expressionExperiment ID
     * @return an expressionexperimentValueObject or null if it isn't there
     */
    public ExpressionExperimentValueObject getExpressionExperiment(Long eeID){

        if (expressionExperiments.containsKey( eeID ))
            return this.expressionExperiments.get( eeID );
        
        return null;
    }

    /**
     * Add a collection of expression experiment to the list
     * 
     * @param vo
     */
    public void addExpressionExperiments( Collection<ExpressionExperimentValueObject> vos ) {
        for ( ExpressionExperimentValueObject eeVo : vos )
            addExpressionExperiment( eeVo );
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
}
