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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

    private int supportThreshold;

    private Gene queryGene;

    private int queryGeneGoTermCount;

    private Map<Long, Map<Long, Collection<Long>>> crossHybridizingQueryProbes;

    private CoexpressedGenesDetails knownGeneCoexpressionData;
    private CoexpressedGenesDetails predictedCoexpressionData;
    private CoexpressedGenesDetails probeAlignedRegionCoexpressionData;

    private double postProcessSeconds;
    private double dbQuerySeconds;
    private Collection eesQueryTestedIn;

    // private GeneCoexpressionResults geneMap;

    /**
     * 
     */
    public CoexpressionCollectionValueObject( Gene queryGene, int supportThreshold ) {
        this.queryGene = queryGene;
        this.supportThreshold = supportThreshold;
        knownGeneCoexpressionData = new CoexpressedGenesDetails( queryGene, supportThreshold );
        predictedCoexpressionData = new CoexpressedGenesDetails( queryGene, supportThreshold );
        probeAlignedRegionCoexpressionData = new CoexpressedGenesDetails( queryGene, supportThreshold );
        crossHybridizingQueryProbes = Collections.synchronizedMap( new HashMap<Long, Map<Long, Collection<Long>>>() );
    }

    public void add( CoexpressionValueObject vo ) {
        String geneType = vo.getGeneType();
        if ( geneType.equalsIgnoreCase( GENE_IMPL ) )
            this.knownGeneCoexpressionData.add( vo );
        else if ( geneType.equalsIgnoreCase( PROBE_ALIGNED_REGION_IMPL ) )
            this.probeAlignedRegionCoexpressionData.add( vo );
        else if ( geneType.equalsIgnoreCase( PREDICTED_GENE_IMPL ) )
            this.predictedCoexpressionData.add( vo );
        else
            throw new IllegalArgumentException( "Unknown gene type" + geneType );
    }

    /**
     * @param geneType
     * @param eevo
     */
    public void addExpressionExperiment( String geneType, ExpressionExperimentValueObject eevo ) {
        /*
         * Maintain a list of which expression experiments participate in which type of coexpression
         */
        if ( geneType.equalsIgnoreCase( GENE_IMPL ) )
            this.knownGeneCoexpressionData.addExpressionExperiment( eevo );
        else if ( geneType.equalsIgnoreCase( PROBE_ALIGNED_REGION_IMPL ) )
            this.probeAlignedRegionCoexpressionData.addExpressionExperiment( eevo );
        else if ( geneType.equalsIgnoreCase( PREDICTED_GENE_IMPL ) )
            this.predictedCoexpressionData.addExpressionExperiment( eevo );
        else
            throw new IllegalArgumentException( "Unknown gene type" + geneType );
    }

    /**
     * Given a list of probes to genes adds this information to the crossHybridizingQueryProbes allowing for the
     * specificty of the queried gene to be determined on a expression experiment level.
     * 
     * @param probe2GeneMap
     */
    public void addQueryGeneSpecifityInfo( Map<Long, Collection<Long>> probe2GeneMap ) {

        /*
         * For each experiment, for each probe
         */
        for ( Long eeID : crossHybridizingQueryProbes.keySet() ) {
            Map<Long, Collection<Long>> probe2genes = crossHybridizingQueryProbes.get( eeID );
            for ( Long probeID : probe2genes.keySet() ) {
                Collection<Long> genes = probe2genes.get( probeID );

                for ( Long g : probe2GeneMap.get( probeID ) ) {
                    genes.add( g );
                }

                if ( ( genes.size() == 1 ) && ( genes.iterator().next().equals( queryGene.getId() ) ) ) {

                    if ( this.predictedCoexpressionData.getExpressionExperiment( eeID ) != null )
                        this.predictedCoexpressionData.getExpressionExperiment( eeID ).setProbeSpecificForQueryGene(
                                true );

                    if ( this.knownGeneCoexpressionData.getExpressionExperiment( eeID ) != null )
                        this.knownGeneCoexpressionData.getExpressionExperiment( eeID ).setProbeSpecificForQueryGene(
                                true );

                    if ( this.probeAlignedRegionCoexpressionData.getExpressionExperiment( eeID ) != null )
                        this.probeAlignedRegionCoexpressionData.getExpressionExperiment( eeID )
                                .setProbeSpecificForQueryGene( true );

                }
            }

        }
    }

    /**
     * @param id
     * @return
     */
    public boolean contains( Long id ) {
        return knownGeneCoexpressionData.containsKey( id ) || predictedCoexpressionData.containsKey( id )
                || probeAlignedRegionCoexpressionData.containsKey( id );
    }

    /**
     * @param geneId
     * @return
     */
    public CoexpressionValueObject get( Long geneId ) {
        if ( !this.contains( geneId ) ) return null;

        CoexpressionValueObject result = null;

        if ( knownGeneCoexpressionData.containsKey( geneId ) ) {
            result = knownGeneCoexpressionData.get( geneId );
        }

        if ( predictedCoexpressionData.containsKey( geneId ) ) {
            assert result == null;
            result = predictedCoexpressionData.get( geneId );
        }

        if ( probeAlignedRegionCoexpressionData.containsKey( geneId ) ) {
            assert result == null;
            result = probeAlignedRegionCoexpressionData.get( geneId );

        }
        return result;

    }

    /**
     * @return the standard Genes CoexpressionDataValueObjects for all types of genes, sorted by decreasing support
     */
    public List<CoexpressionValueObject> getAllGeneCoexpressionData() {
        List<CoexpressionValueObject> result = new ArrayList<CoexpressionValueObject>();
        result.addAll( this.knownGeneCoexpressionData.getCoexpressionData( 0 ) );
        result.addAll( this.predictedCoexpressionData.getCoexpressionData( 0 ) );
        result.addAll( this.probeAlignedRegionCoexpressionData.getCoexpressionData( 0 ) );
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
    public Collection getEesQueryTestedIn() {
        return eesQueryTestedIn;
    }

    /**
     * Only searches the given geneType for the specified EE. if not found return null.
     */
    public ExpressionExperimentValueObject getExpressionExperiment( String geneType, Long eeID ) {

        if ( geneType.equalsIgnoreCase( GENE_IMPL ) )
            return knownGeneCoexpressionData.getExpressionExperiment( eeID );
        else if ( geneType.equalsIgnoreCase( PROBE_ALIGNED_REGION_IMPL ) )
            return probeAlignedRegionCoexpressionData.getExpressionExperiment( eeID );
        else if ( geneType.equalsIgnoreCase( PREDICTED_GENE_IMPL ) )
            return predictedCoexpressionData.getExpressionExperiment( eeID );

        return null;

    }

    /**
     * @return
     */
    public Collection<ExpressionExperimentValueObject> getExpressionExperiments() {
        Collection<ExpressionExperimentValueObject> all = new HashSet<ExpressionExperimentValueObject>();
        all.addAll( this.knownGeneCoexpressionData.getExpressionExperiments() );
        all.addAll( this.probeAlignedRegionCoexpressionData.getExpressionExperiments() );
        all.addAll( this.predictedCoexpressionData.getExpressionExperiments() );
        return all;

    }

    /**
     * @return the CoexpressonTypeValueObject for standard genes
     */
    public CoexpressedGenesDetails getKnownGeneCoexpression() {
        return this.knownGeneCoexpressionData;
    }

    /**
     * @return the coexpressionData for known genes in order of decreasing support
     */
    public List<CoexpressionValueObject> getKnownGeneCoexpressionData( int supportThreshold ) {
        return this.knownGeneCoexpressionData.getCoexpressionData( supportThreshold );
    }

    /**
     * @return the number of Genes
     */
    public int getNumKnownGenes() {
        return this.knownGeneCoexpressionData.getNumberOfGenes();
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
        return this.probeAlignedRegionCoexpressionData.getNumberOfGenes();
    }

    /**
     * @return the numStringencyGenes
     */
    public int getNumStringencyGenes() {
        return this.knownGeneCoexpressionData.getNumberOfGenes();
    }

    /**
     * @return the numStringencyPredictedGenes
     */
    public int getNumStringencyPredictedGenes() {
        return this.predictedCoexpressionData.getNumberOfGenes();
    }

    /**
     * @return the numStringencyProbeAlignedRegions
     */
    public int getNumStringencyProbeAlignedRegions() {
        return this.probeAlignedRegionCoexpressionData.getNumberOfGenes();
    }

    public double getPostProcessSeconds() {
        return postProcessSeconds;
    }

    /**
     * @return the predicted genes CoexpressionDataValueobjects in order of decreasing support
     */
    public List<CoexpressionValueObject> getPredictedCoexpressionData( int supportThreshold ) {
        return this.predictedCoexpressionData.getCoexpressionData( supportThreshold );
    }

    /**
     * @return the CoexpressionTypeValueObject for predicted Genes
     */
    public CoexpressedGenesDetails getPredictedCoexpressionType() {
        return this.predictedCoexpressionData;
    }

    /**
     * @return the probe aligned genes CoexpressionDataValueObjects in order of decreasing support
     */
    public List<CoexpressionValueObject> getProbeAlignedCoexpressionData( int supportThreshold ) {
        return this.probeAlignedRegionCoexpressionData.getCoexpressionData( supportThreshold );
    }

    /**
     * @return the CoexpressonTypeValueObject for probe aligned regions
     */
    public CoexpressedGenesDetails getProbeAlignedCoexpressionType() {
        return this.probeAlignedRegionCoexpressionData;
    }

    /**
     * @return the queryGene
     */
    public Gene getQueryGene() {
        return queryGene;
    }

    public int getQueryGeneGoTermCount() {
        return queryGeneGoTermCount;
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
     * @return a collection of expression experiment IDs that have <strong>at least one</strong> probes that are
     *         predicted to be <strong>specific</strong> for the query gene. Thus, expression experiments that have
     *         both specific and non-specific probes for the query gene will be included.
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
        return supportThreshold;
    }

    /**
     * Merely initializes the data structure.
     * 
     * @param eeID the expressionexperiment id
     * @param probeID the probe ID
     */

    public void initializeSpecificityDataStructure( Long eeID, Long probeID ) {
        if ( !crossHybridizingQueryProbes.containsKey( eeID ) ) {
            crossHybridizingQueryProbes.put( eeID, new HashMap<Long, Collection<Long>>() );
        }
        Map<Long, Collection<Long>> probe2geneMap = crossHybridizingQueryProbes.get( eeID );
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
    public void setEesQueryGeneTestedIn( Collection eesQueryTestedIn ) {
        this.eesQueryTestedIn = eesQueryTestedIn;
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
}
