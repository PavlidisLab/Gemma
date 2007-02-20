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
import java.util.HashSet;

import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

/**
 * @author jsantos
 */
public class CoexpressionCollectionValueObject {
    private int linkCount; // the total number of links for this specific coexpression
    private int positiveStringencyLinkCount; // the number of links for this coexpression that passed the stringency
    // requirements
    private int negativeStringencyLinkCount;
    private Collection<ExpressionExperimentValueObject> expressionExperiments; // the expression experiments that were
    
    // the number of actual genes, predicted genes, and probe aligned regions in the query, unfiltered by stringency
    private int numGenes;
    private int numPredictedGenes;
    private int numProbeAlignedRegions;
    
    // the number of actual genes, predicted genes, and probe aligned regions in the query, filtered by stringency
    private int numStringencyGenes;
    private int numStringencyPredictedGenes;
    private int numStringencyProbeAlignedRegions;
    
    // involved in the query
    private Collection<CoexpressionValueObject> coexpressionData;
    private double firstQuerySeconds;
    private double secondQuerySeconds;
    private double postProcessSeconds;
    private double elapsedWallSeconds;
    

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
        expressionExperiments = new HashSet<ExpressionExperimentValueObject>();
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
        return expressionExperiments;
    }

    /**
     * @param expressionExperiments the expressionExperiments to set
     */
    public void setExpressionExperiments( Collection<ExpressionExperimentValueObject> expressionExperiments ) {
        this.expressionExperiments = expressionExperiments;
    }

    /**
     * Add an expression experiment to the list
     * 
     * @param vo
     */
    public void addExpressionExperiment( ExpressionExperimentValueObject vo ) {
        this.expressionExperiments.add( vo );
    }

    /**
     * Add a collection of expression experiment to the list
     * 
     * @param vo
     */
    public void addExpressionExperiment( Collection<ExpressionExperimentValueObject> vos ) {
        this.expressionExperiments.addAll( vos );
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

}
