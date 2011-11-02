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

import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneImpl;
import ubic.gemma.model.genome.PredictedGeneImpl;
import ubic.gemma.model.genome.ProbeAlignedRegionImpl;

/**
 * The coexpressioncollectionValueObject is used for storing all the results of a coexpression search for one query
 * gene.
 * 
 * @author jsantos
 * @author klc
 * @version $Id$
 */
public class CoexpressionCollectionValueObject {
    public static final String GENE_IMPL = GeneImpl.class.getSimpleName();

    // public static final String PREDICTED_GENE_IMPL = PredictedGeneImpl.class.getSimpleName();
    // public static final String PROBE_ALIGNED_REGION_IMPL = ProbeAlignedRegionImpl.class.getSimpleName();

    private double postProcessSeconds;
    private double dbQuerySeconds;

    private Collection<BioAssaySet> eesQueryTestedIn;

    private String errorState;

    private CoexpressedGenesDetails knownGeneCoexpressionData;

    // private CoexpressedGenesDetails predictedCoexpressionData;

    // private CoexpressedGenesDetails probeAlignedRegionCoexpressionData;

    private Gene queryGene;
    private int queryGeneGoTermCount;

    /**
     * EE -> Probes -> Genes
     */
    private Map<Long, Map<Long, Collection<Long>>> queryProbes;

    private int supportThreshold;

    /**
     * 
     */
    public CoexpressionCollectionValueObject( Gene queryGene, int supportThreshold ) {
        this.queryGene = queryGene;
        this.supportThreshold = supportThreshold;
        knownGeneCoexpressionData = new CoexpressedGenesDetails( queryGene, supportThreshold );
        // predictedCoexpressionData = new CoexpressedGenesDetails( queryGene, supportThreshold );
        // probeAlignedRegionCoexpressionData = new CoexpressedGenesDetails( queryGene, supportThreshold );
        queryProbes = Collections.synchronizedMap( new HashMap<Long, Map<Long, Collection<Long>>>() );
        this.eesQueryTestedIn = new HashSet<BioAssaySet>();
    }

    public void add( CoexpressionValueObject vo ) {
        // String geneType = vo.getGeneType();
        // if ( geneType.equals( GENE_IMPL ) )
        this.knownGeneCoexpressionData.add( vo );
        // else if ( geneType.equals( PROBE_ALIGNED_REGION_IMPL ) )
        // this.probeAlignedRegionCoexpressionData.add( vo );
        // else if ( geneType.equals( PREDICTED_GENE_IMPL ) )
        // this.predictedCoexpressionData.add( vo );
        // else
        // throw new IllegalArgumentException( "Unknown gene type" + geneType );
    }

    /**
     * @param geneType
     * @param eevo
     */
    public void addExpressionExperiment( ExpressionExperimentValueObject eevo ) {
        /*
         * Maintain a list of which expression experiments participate in which type of coexpression
         */
        // if ( geneType.equals( GENE_IMPL ) )
        this.knownGeneCoexpressionData.addExpressionExperiment( eevo );
        // else if ( geneType.equals( PROBE_ALIGNED_REGION_IMPL ) )
        // this.probeAlignedRegionCoexpressionData.addExpressionExperiment( eevo );
        // else if ( geneType.equals( PREDICTED_GENE_IMPL ) )
        // this.predictedCoexpressionData.addExpressionExperiment( eevo );
        // else
        // throw new IllegalArgumentException( "Unknown gene type" + geneType );
    }

    /**
     * @param coexpressedGeneId
     * @return
     */
    public boolean contains( Long coexpressedGeneId ) {
        return knownGeneCoexpressionData.containsKey( coexpressedGeneId );
        // || predictedCoexpressionData.containsKey( coexpressedGeneId )
        // || probeAlignedRegionCoexpressionData.containsKey( coexpressedGeneId );
    }

    /**
     * @param coexpressedGeneId
     * @return coexpression results pertaining to the given gene, if any; or null otherwise
     */
    public CoexpressionValueObject get( Long coexpressedGeneId ) {
        if ( !this.contains( coexpressedGeneId ) ) return null;

        CoexpressionValueObject result = null;

        if ( knownGeneCoexpressionData.containsKey( coexpressedGeneId ) ) {
            result = knownGeneCoexpressionData.get( coexpressedGeneId );
        }

        // if ( predictedCoexpressionData.containsKey( coexpressedGeneId ) ) {
        // assert result == null;
        // result = predictedCoexpressionData.get( coexpressedGeneId );
        // }
        //
        // if ( probeAlignedRegionCoexpressionData.containsKey( coexpressedGeneId ) ) {
        // assert result == null;
        // result = probeAlignedRegionCoexpressionData.get( coexpressedGeneId );
        //
        // }
        return result;

    }

    /**
     * @param stringency enter 0 to get everything (entering 1 would have the same effect).
     * @return the standard Genes CoexpressionDataValueObjects for all types of genes, sorted by decreasing support
     */
    public List<CoexpressionValueObject> getAllGeneCoexpressionData( int stringency ) {
        List<CoexpressionValueObject> result = new ArrayList<CoexpressionValueObject>();
        result.addAll( this.knownGeneCoexpressionData.getCoexpressionData( stringency ) );
        // result.addAll( this.predictedCoexpressionData.getCoexpressionData( stringency ) );
        // result.addAll( this.probeAlignedRegionCoexpressionData.getCoexpressionData( stringency ) );
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
    public Collection<BioAssaySet> getEesQueryTestedIn() {
        return eesQueryTestedIn;
    }

    public String getErrorState() {
        return errorState;
    }

    /**
     * Only searches the given geneType for the specified EE. if not found return null.
     */
    public ExpressionExperimentValueObject getExpressionExperiment( Long eeID ) {
        // if ( geneType.equals( GENE_IMPL ) )
        return knownGeneCoexpressionData.getExpressionExperiment( eeID );
        // else if ( geneType.equals( PROBE_ALIGNED_REGION_IMPL ) )
        // return probeAlignedRegionCoexpressionData.getExpressionExperiment( eeID );
        // else if ( geneType.equals( PREDICTED_GENE_IMPL ) )
        // return predictedCoexpressionData.getExpressionExperiment( eeID );

        // return null;

    }

    /**
     * @return
     */
    public Collection<ExpressionExperimentValueObject> getExpressionExperiments() {
        Collection<ExpressionExperimentValueObject> all = new HashSet<ExpressionExperimentValueObject>();
        all.addAll( this.knownGeneCoexpressionData.getExpressionExperiments() );
        // all.addAll( this.probeAlignedRegionCoexpressionData.getExpressionExperiments() );
        // all.addAll( this.predictedCoexpressionData.getExpressionExperiments() );
        return all;

    }

    /**
     * @return the CoexpressedGenesDetails for standard genes
     */
    public CoexpressedGenesDetails getKnownGeneCoexpression() {
        return this.knownGeneCoexpressionData;
    }

    /**
     * @return the coexpressionData for known genes in order of decreasing support
     */
    @SuppressWarnings("hiding")
    public List<CoexpressionValueObject> getKnownGeneCoexpressionData( int supportThreshold ) {
        return this.knownGeneCoexpressionData.getCoexpressionData( supportThreshold );
    }

    /**
     * @return the number of Known Genes coexpressed with the query
     */
    public int getNumKnownGenes() {
        return this.knownGeneCoexpressionData.getNumberOfGenes();
    }

    //
    // /**
    // * @return the numPredictedGenes coexpressed with the query
    // */
    // public int getNumPredictedGenes() {
    // return this.predictedCoexpressionData.getNumberOfGenes();
    // }
    //
    // /**
    // * @return the numProbeAlignedRegions coexpressed with the query
    // */
    // public int getNumProbeAlignedRegions() {
    // return this.probeAlignedRegionCoexpressionData.getNumberOfGenes();
    // }

    /**
     * @return the numStringencyGenes - for known genes.
     */
    public int getNumStringencyGenes() {
        return getNumStringencyKnownGenes();
    }

    /**
     * @return the numStringencyGenes - for known genes.
     */
    public int getNumStringencyKnownGenes() {
        return this.knownGeneCoexpressionData.getNumberOfGenes();
    }

    // /**
    // * @return the numStringencyPredictedGenes
    // */
    // public int getNumStringencyPredictedGenes() {
    // return this.predictedCoexpressionData.getNumberOfGenes();
    // }
    //
    // /**
    // * @return the numStringencyProbeAlignedRegions
    // */
    // public int getNumStringencyProbeAlignedRegions() {
    // return this.probeAlignedRegionCoexpressionData.getNumberOfGenes();
    // }

    public double getPostProcessSeconds() {
        return postProcessSeconds;
    }

    // /**
    // * @return the predicted genes CoexpressionDataValueobjects in order of decreasing support
    // */
    // @SuppressWarnings("hiding")
    // public List<CoexpressionValueObject> getPredictedCoexpressionData( int supportThreshold ) {
    // return this.predictedCoexpressionData.getCoexpressionData( supportThreshold );
    // }

    // /**
    // * @return the CoexpressedGenesDetails for predicted Genes
    // */
    // public CoexpressedGenesDetails getPredictedGeneCoexpression() {
    // return this.predictedCoexpressionData;
    // }

    // /**
    // * @return the probe aligned genes CoexpressionDataValueObjects in order of decreasing support
    // */
    // @SuppressWarnings("hiding")
    // public List<CoexpressionValueObject> getProbeAlignedCoexpressionData( int supportThreshold ) {
    // return this.probeAlignedRegionCoexpressionData.getCoexpressionData( supportThreshold );
    // }

    // /**
    // * @return the CoexpressedGenesDetails for probe aligned regions
    // */
    // public CoexpressedGenesDetails getProbeAlignedRegionCoexpression() {
    // return this.probeAlignedRegionCoexpressionData;
    // }

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
        List<CoexpressionValueObject> data = this.getAllGeneCoexpressionData( 0 );
        for ( CoexpressionValueObject coVo : data ) {
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
    public void setEesQueryGeneTestedIn( Collection<BioAssaySet> eesQueryTestedIn ) {
        this.eesQueryTestedIn = eesQueryTestedIn;
    }

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
     * specificty of the queried gene to be determined on a expression experiment level.
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
                if ( genes.size() == 1 && genes.iterator().next().equals( queryGene.getId() ) ) {

                    /*
                     * This is a high-level record.
                     */
                    // if ( this.predictedCoexpressionData.getExpressionExperiment( eeID ) != null )
                    // this.predictedCoexpressionData.getExpressionExperiment( eeID ).setHasProbeSpecificForQueryGene(
                    // true );

                    if ( this.knownGeneCoexpressionData.getExpressionExperiment( eeID ) != null )
                        this.knownGeneCoexpressionData.getExpressionExperiment( eeID ).setHasProbeSpecificForQueryGene(
                                true );
                    //
                    // if ( this.probeAlignedRegionCoexpressionData.getExpressionExperiment( eeID ) != null )
                    // this.probeAlignedRegionCoexpressionData.getExpressionExperiment( eeID )
                    // .setHasProbeSpecificForQueryGene( true );

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
        if ( this.knownGeneCoexpressionData.getExpressionExperiment( eeID ) != null )
            this.knownGeneCoexpressionData.addQuerySpecificityInfo( eeID, probe2GeneMap );

        // if ( this.predictedCoexpressionData.getExpressionExperiment( eeID ) != null )
        // this.predictedCoexpressionData.addQuerySpecificityInfo( eeID, probe2GeneMap );
        //
        // if ( this.probeAlignedRegionCoexpressionData.getExpressionExperiment( eeID ) != null )
        // this.probeAlignedRegionCoexpressionData.addQuerySpecificityInfo( eeID, probe2GeneMap );
    }

    private void addTargetSpecificityData( Long eeID, Map<Long, Collection<Long>> probe2GeneMap ) {
        if ( this.knownGeneCoexpressionData.getExpressionExperiment( eeID ) != null )
            this.knownGeneCoexpressionData.addTargetSpecificityInfo( eeID, probe2GeneMap );

        // if ( this.predictedCoexpressionData.getExpressionExperiment( eeID ) != null )
        // this.predictedCoexpressionData.addTargetSpecificityInfo( eeID, probe2GeneMap );
        //
        // if ( this.probeAlignedRegionCoexpressionData.getExpressionExperiment( eeID ) != null )
        // this.probeAlignedRegionCoexpressionData.addTargetSpecificityInfo( eeID, probe2GeneMap );
    }

}
