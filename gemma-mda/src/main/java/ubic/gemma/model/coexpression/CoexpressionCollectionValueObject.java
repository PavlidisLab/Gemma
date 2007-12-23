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
 * The coexpressioncollectionValueObject is used for storing the results of a coexpression search. The object is thread
 * safe
 * 
 * @author jsantos
 * @author klc
 * @version $Id$
 */
public class CoexpressionCollectionValueObject {

    public static final String PREDICTED_GENE_IMPL = "PredictedGeneImpl";
    public static final String PROBE_ALIGNED_REGION_IMPL = "ProbeAlignedRegionImpl";
    public static final String GENE_IMPL = "GeneImpl";

    private static Log log = LogFactory.getLog( CoexpressionCollectionValueObject.class.getName() );

    // the number of actual genes, predicted genes, and probe aligned regions in the query, filtered by stringency
    private int stringencyFilterValue;
    private Gene queryGene;
    private Map<Long, Map<Long, Collection<Long>>> crossHybridizingQueryProbes; // This will hold info pertaining to the

    // involved in the query
    private CoexpressionTypeValueObject geneCoexpressionData;
    private CoexpressionTypeValueObject predictedCoexpressionData;
    private CoexpressionTypeValueObject alignedCoexpressionData;

    private double firstQuerySeconds;
    private double postProcessSeconds;
    private double elapsedWallSeconds;
    private GeneCoexpressionResults geneMap;

    /**
     * 
     */
    public CoexpressionCollectionValueObject() {
        geneCoexpressionData = new CoexpressionTypeValueObject();
        predictedCoexpressionData = new CoexpressionTypeValueObject();
        alignedCoexpressionData = new CoexpressionTypeValueObject();
        crossHybridizingQueryProbes = Collections.synchronizedMap( new HashMap<Long, Map<Long, Collection<Long>>>() );
    }

    /**
     * @param geneType
     * @param eevo
     */
    public void addExpressionExperiment( String geneType, ExpressionExperimentValueObject eevo ) {

        if ( geneType.equalsIgnoreCase( GENE_IMPL ) )
            this.geneCoexpressionData.addExpressionExperiment( eevo );
        else if ( geneType.equalsIgnoreCase( PROBE_ALIGNED_REGION_IMPL ) )
            this.alignedCoexpressionData.addExpressionExperiment( eevo );
        else if ( geneType.equalsIgnoreCase( PREDICTED_GENE_IMPL ) )
            this.predictedCoexpressionData.addExpressionExperiment( eevo );
        else
            throw new IllegalArgumentException( "Unknown gene type" + geneType );
    }

    /*
     * Given a list of probes to genes adds this information to the crossHybridizingQueryProbes allowing for the
     * specificty of the queried gene to be determined on a expression experiment level.
     */
    public void addQueryGeneSpecifityInfo( Map<Long, Collection<Long>> probe2GeneMap ) {

        synchronized ( crossHybridizingQueryProbes ) {
            for ( Long eeID : crossHybridizingQueryProbes.keySet() ) {
                Map<Long, Collection<Long>> probe2genes = crossHybridizingQueryProbes.get( eeID );
                for ( Long probeID : probe2genes.keySet() ) {
                    Collection<Long> genes = probe2genes.get( probeID );
                    assert probe2GeneMap.containsKey( probeID );
                    for ( Long g : probe2GeneMap.get( probeID ) )
                        genes.add( g );

                    if ( ( genes.size() == 1 ) && ( genes.iterator().next().equals( queryGene.getId() ) ) ) {

                        if ( this.predictedCoexpressionData.getExpressionExperiment( eeID ) != null )
                            this.predictedCoexpressionData.getExpressionExperiment( eeID ).setSpecific( true );

                        if ( this.geneCoexpressionData.getExpressionExperiment( eeID ) != null )
                            this.geneCoexpressionData.getExpressionExperiment( eeID ).setSpecific( true );

                        if ( this.alignedCoexpressionData.getExpressionExperiment( eeID ) != null )
                            this.alignedCoexpressionData.getExpressionExperiment( eeID ).setSpecific( true );

                    }
                }
            }
        }
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

    /**
     * @return the coexpressionData for genes
     */
    public Collection<CoexpressionValueObject> getCoexpressionData() {
        return this.geneCoexpressionData.getCoexpressionData();
    }

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
     * Only searches the given geneType for the specified EE. if not found return null.
     */
    public ExpressionExperimentValueObject getExpressionExperiment( String geneType, Long eeID ) {

        if ( geneType.equalsIgnoreCase( GENE_IMPL ) )
            return geneCoexpressionData.getExpressionExperiment( eeID );
        else if ( geneType.equalsIgnoreCase( PROBE_ALIGNED_REGION_IMPL ) )
            return alignedCoexpressionData.getExpressionExperiment( eeID );
        else if ( geneType.equalsIgnoreCase( PREDICTED_GENE_IMPL ) )
            return predictedCoexpressionData.getExpressionExperiment( eeID );

        return null;

    }

    /**
     * @return
     */
    public Collection<ExpressionExperimentValueObject> getExpressionExperiments() {
        Collection<ExpressionExperimentValueObject> all = new HashSet<ExpressionExperimentValueObject>();
        all.addAll( this.geneCoexpressionData.getExpressionExperiments() );
        all.addAll( this.alignedCoexpressionData.getExpressionExperiments() );
        all.addAll( this.predictedCoexpressionData.getExpressionExperiments() );
        return all;

    }

    public double getFirstQuerySeconds() {
        return firstQuerySeconds;
    }

    /**
     * @return the standard Genes CoexpressionDataValueObjects
     */
    public Collection<CoexpressionValueObject> getGeneCoexpressionData() {
        return this.geneCoexpressionData.getCoexpressionData();
    }

    /**
     * @return the CoexpressonTypeValueObject for standard genes
     */
    public CoexpressionTypeValueObject getGeneCoexpressionType() {
        return this.geneCoexpressionData;
    }

    public GeneCoexpressionResults getGeneMap() {
        return geneMap;
    }

    /**
     * @return the number of Genes
     */
    public int getNumGenes() {
        return this.geneCoexpressionData.getNumberOfGenes();
    }

    /**
     * @return the numPredictedGenes
     */
    public int getNumPredictedGenes() {
        return this.predictedCoexpressionData.getNumberOfGenes();
    }

    /**
     * @return the numProbeAlignedRegions
     */
    public int getNumProbeAlignedRegions() {
        return this.alignedCoexpressionData.getNumberOfGenes();
    }

    /**
     * @return the numStringencyGenes
     */
    public int getNumStringencyGenes() {
        return this.geneCoexpressionData.getNumberOfStringencyGenes();
    }

    /**
     * @return the numStringencyPredictedGenes
     */
    public int getNumStringencyPredictedGenes() {
        return this.predictedCoexpressionData.getNumberOfStringencyGenes();
    }

    /**
     * @return the numStringencyProbeAlignedRegions
     */
    public int getNumStringencyProbeAlignedRegions() {
        return this.alignedCoexpressionData.getNumberOfStringencyGenes();
    }

    public double getPostProcessSeconds() {
        return postProcessSeconds;
    }

    /**
     * @return the predicted genes CoexpressionDataValueobjects
     */
    public Collection<CoexpressionValueObject> getPredictedCoexpressionData() {
        return this.predictedCoexpressionData.getCoexpressionData();
    }

    /**
     * @return the CoexpressionTypeValueObject for predicted Genes
     */
    public CoexpressionTypeValueObject getPredictedCoexpressionType() {
        return this.predictedCoexpressionData;
    }

    /**
     * @return the probe aligned genes CoexpressionDataValueObjects
     */
    public Collection<CoexpressionValueObject> getProbeAlignedCoexpressionData() {
        return this.alignedCoexpressionData.getCoexpressionData();
    }

    /**
     * @return the CoexpressonTypeValueObject for probe aligned regions
     */
    public CoexpressionTypeValueObject getProbeAlignedCoexpressionType() {
        return this.alignedCoexpressionData;
    }

    /**
     * @return the queryGene
     */
    public Gene getQueryGene() {
        return queryGene;
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

                    if ( ( genes.size() == 1 ) && ( genes.iterator().next().equals( queryGene.getId() ) ) ) {
                        log.debug( "Expression Experiment: + " + eeID + " is specific" );
                        specificEE.add( eeID );
                    }
                }

            }
        }
        return specificEE;
    }

    /**
     * @return the stringency Filter Value
     */
    public int getStringency() {
        return stringencyFilterValue;
    }

    /**
     * Set the amount of time we had to wait for the queries (which can be less than the time per query because
     * 
     * @param elapsedWallTime (in milliseconds)
     */
    public void setElapsedWallTimeElapsed( double elapsedWallMillisSeconds ) {
        this.elapsedWallSeconds = elapsedWallMillisSeconds / 1000.0;
    }

    public void setFirstQueryElapsedTime( Long elapsed ) {
        this.firstQuerySeconds = elapsed / 1000.0;

    }

    public void setGeneCoexpressionResults( GeneCoexpressionResults geneMap ) {
        this.geneMap = geneMap;
    }

    public void setPostProcessTime( Long elapsed ) {
        this.postProcessSeconds = elapsed / 1000.0;

    }

    /**
     * @param queryGene the queryGene to set
     */
    public void setQueryGene( Gene queryGene ) {
        this.queryGene = queryGene;
    }

    /**
     * @param sets the stringency Value
     */
    public void setStringency( int stringency ) {
        this.stringencyFilterValue = stringency;
    }

}
