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
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The coexpressioncollectionValueObject is used for storing all the results of a coexpression search for one query
 * gene, across multiple experiments.
 * 
 * @author jsantos
 * @author klc
 * @version $Id$
 */
public class QueryGeneCoexpression {

    private static Logger log = LoggerFactory.getLogger( QueryGeneCoexpression.class );

    private String errorState = null;

    /**
     * EE -> Probes -> Genes for the query gene. This is a map of probes to multiple genes to deal with the case where
     * the probe might be nonspecific; one of the genes 'has to' be this.queryGene.
     */
    private Map<Long, Map<Long, Collection<Long>>> queryProbes;

    /**
     * Details for each gene coexpressed with this one. Map of gene id to vos.
     */
    private Map<Long, CoexpressedGenePairValueObject> coexpressionData;

    /**
     * Map of EE -> Probes -> Genes for the coexpressed genes.
     */
    private Map<Long, Map<Long, Collection<Long>>> expressionExperimentProbe2GeneMaps;

    /**
     * the expression experiments that show up in the results - they contribute links. Value is the number of links.
     */
    private Map<Long, Integer> expressionExperiments;

    /**
     * Number of links that passed the stringency requirements, with negative correlations
     */
    private int negativeStringencyLinkCount;

    /**
     * Number of links that passed the stringency requirements, with positive correlations
     */
    private int positiveStringencyLinkCount;

    /**
     * The gene used to search from.
     */
    private final Long queryGene;

    private final int supportThreshold;

    private boolean warned = false;

    /**
     * Constructor just initializes
     */
    public QueryGeneCoexpression( Long queryGene, int supportThreshold ) {
        this.queryGene = queryGene;
        this.supportThreshold = supportThreshold;
        queryProbes = new HashMap<Long, Map<Long, Collection<Long>>>();

        positiveStringencyLinkCount = 0;
        negativeStringencyLinkCount = 0;

        coexpressionData = new HashMap<Long, CoexpressedGenePairValueObject>();
        expressionExperiments = new HashMap<Long, Integer>();
        expressionExperimentProbe2GeneMaps = new HashMap<Long, Map<Long, Collection<Long>>>();

    }

    /**
     * @param vo
     * @return
     */
    public void add( CoexpressedGenePairValueObject vo ) {
        if ( coexpressionData.containsKey( vo.getCoexpressedGeneId() ) ) {
            // FIXME this seems like it would be an error.
            if ( log.isDebugEnabled() ) log.debug( "Clobbering when adding " + vo );
        }
        coexpressionData.put( vo.getCoexpressedGeneId(), vo );
    }

    /**
     * @param coexpressedGeneId
     * @return
     */
    public boolean contains( Long coexpressedGeneId ) {
        return coexpressionData.containsKey( coexpressedGeneId );
    }

    /**
     * Filter out all results except for the top N, where N = limit. Important: only run this after you've populated the
     * object!
     * 
     * @param limit
     * @param stringency
     */
    public void filter( int limit, int stringency ) {
        // remove from the coexpressionData
        int count = 0;

        // we need to sort this map by the values.
        class Vk implements Comparable<Vk> {
            private Long i;
            private CoexpressedGenePairValueObject v;

            private Vk( Long i, CoexpressedGenePairValueObject v ) {
                this.i = i;
                this.v = v;
            }

            @Override
            public int compareTo( Vk o ) {
                return o.getV().compareTo( this.v );
            }

            /*
             * (non-Javadoc)
             * 
             * @see java.lang.Object#equals(java.lang.Object)
             */
            @Override
            public boolean equals( Object obj ) {
                if ( this == obj ) return true;
                if ( obj == null ) return false;
                if ( getClass() != obj.getClass() ) return false;
                Vk other = ( Vk ) obj;
                if ( v == null ) {
                    if ( other.v != null ) return false;
                } else if ( !v.equals( other.v ) ) return false;
                return true;
            }

            /*
             * (non-Javadoc)
             * 
             * @see java.lang.Object#hashCode()
             */
            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + ( ( v == null ) ? 0 : v.hashCode() );
                return result;
            }

            private Long getI() {
                return i;
            }

            private CoexpressedGenePairValueObject getV() {
                return v;
            }
        }

        List<Vk> vks = new ArrayList<Vk>();

        int revisedPosLinks = 0;
        int revisedNegLinks = 0;

        for ( Long l : coexpressionData.keySet() ) {
            CoexpressedGenePairValueObject link = coexpressionData.get( l );

            vks.add( new Vk( l, link ) );
        }

        Collections.sort( vks );
        Collections.reverse( vks );

        for ( Vk vk : vks ) {
            if ( count > limit ) {
                coexpressionData.remove( vk.getI() );
            } else {
                if ( vk.getV().getPositiveLinkSupport() >= stringency ) {
                    revisedPosLinks++;
                }
                if ( vk.getV().getNegativeLinkSupport() >= stringency ) {
                    revisedNegLinks++;
                }
            }
            count++;
        }

        this.setPositiveStringencyLinkCount( revisedPosLinks );
        this.setNegativeStringencyLinkCount( revisedNegLinks );

        /*
         * Note that it is possible for the sum to be greater than the limit, as links can have both + and - correlation
         * support > threshold.
         */
        assert revisedPosLinks <= limit : "Got " + revisedPosLinks + " pos links but limit was " + limit;
        assert revisedNegLinks <= limit : "Got " + revisedNegLinks + " neg links but limit was " + limit;

    }

    @Override
    public void finalize() {
        if ( this.queryProbes != null ) {
            for ( Long l : queryProbes.keySet() ) {
                queryProbes.get( l ).clear();
            }
            this.queryProbes.clear();
        }
        expressionExperiments.clear();

        for ( Long l : coexpressionData.keySet() ) {
            coexpressionData.get( l ).finalize();
        }

        for ( Long l : expressionExperimentProbe2GeneMaps.keySet() ) {
            expressionExperimentProbe2GeneMaps.get( l ).clear();
        }

        this.expressionExperimentProbe2GeneMaps.clear();
        this.coexpressionData.clear();
    }

    /**
     * @param coexpressedGeneId
     * @return coexpression results pertaining to the given gene, if any; or null otherwise
     */
    public CoexpressedGenePairValueObject get( Long coexpressedGeneId ) {
        if ( !this.contains( coexpressedGeneId ) ) return null;

        return this.coexpressionData.get( coexpressedGeneId );

    }

    /**
     * @param stringency enter 0 to get everything (entering 1 would have the same effect).
     * @return the standard Genes CoexpressionDataValueObjects for all types of genes.
     */
    public List<CoexpressedGenePairValueObject> getAllGeneCoexpressionData( int stringency ) {
        return this.getCoexpressionData( stringency );
    }

    /**
     * @param supportThreshold
     * @return the coexpressionData
     */
    public List<CoexpressedGenePairValueObject> getCoexpressionData( int threshold ) {
        List<CoexpressedGenePairValueObject> result = new ArrayList<CoexpressedGenePairValueObject>();
        for ( CoexpressedGenePairValueObject o : coexpressionData.values() ) {
            if ( o.getNegativeLinkSupport() >= threshold || o.getPositiveLinkSupport() >= threshold ) {
                result.add( o );
            }
        }
        return result;
    }

    public String getErrorState() {
        return errorState;
    }

    /**
     * @return
     */
    public Collection<Long> getExpressionExperiments() {
        return expressionExperiments.keySet();
    }

    /**
     * @param id
     * @return
     */
    public Integer getLinkCountForEE( Long id ) {

        Integer eeVo = expressionExperiments.get( id );

        if ( eeVo == null ) return 0;

        return eeVo;

    }

    /**
     * @return the stringencyLinkCount
     */
    public int getNegativeStringencyLinkCount() {
        return negativeStringencyLinkCount;
    }

    /**
     * @return the number of StringencyGenes
     */
    public int getNumberOfGenes() {
        return this.coexpressionData.size();
    }

    public int getNumDatasetsQueryGeneTestedIn() {
        return this.getExpressionExperiments().size();
    }

    /**
     * @return the stringencyLinkCount
     */
    public int getPositiveStringencyLinkCount() {
        return positiveStringencyLinkCount;
    }

    /**
     * @return the queryGene
     */
    public Long getQueryGene() {
        return queryGene;
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
     * This computes the support for each link from a single query gene, as well as updating information on specificity
     * of probes. Terminology:
     * <ul>
     * <li>Query gene: the gene that was used to initate the search</li>
     * <li>Coexpressed gene: 'answer' to the query</li>
     * <li>Query probe: a probe that provided evidence for the query gene</li>
     * <li>Coexpressed probe: probe that provided evidence for the coexpressed gene</li>
     * <li>'hyb' is shorthand for 'hybridizes'. Really we don't know what hybridizes to what: 'hyb' means that sequence
     * similarity was above some threshold during sequence analysis.</li>
     * </ul>
     * <p>
     * For any query probe - coexpressed probe combination, computing specificity requires considering several cases:
     * </p>
     * <ol>
     * <li>query probe and the coexpressed probe are both specific for separate query and coexpressed genes. This is the
     * 'easy' case.</li>
     * <li>query probe is non-specific. There are two subcases (which can co-occur)
     * <ol>
     * <li>query probe hybs to target gene</li>
     * <li>query probe hybs to other gene(s).</li>
     * </ol>
     * </li>
     * <li>target probe is non-specific. There are two subcases (which can co-occur)
     * <ol>
     * <li>target probe hybs to the query gene</li>
     * <li>target probe hybs to some other gene(s).</li>
     * </ol>
     * </li>
     * <li>Both the target and query probe are non-specific, in which case there are four combinations of the above
     * subcases. These cases aren't handled separately.</li>
     * </ol>
     * <p>
     * The current policy (please see the code for details) is that if there is potential cross-hybridization between
     * ANY gene that the query probe picks up and ANY gene that the coexpressed probe picks up, then we remove the link
     * entirely. For all other cases we just flag the probe as non-specific. This lets downstream analysis know that the
     * "query gene" might really be something else; likewise for the coexpressed gene. In other words, for most of the
     * above cases the concern is that the issue is not kept track of. Only two seriously problematic cases (2.1 and
     * 3.1) result in data removal.
     */
    public void postProcess() {
        StopWatch watch = new StopWatch();
        watch.start();

        /*
         * Believe it or not, this is some of the trickiest and most important code in Gemma. Don't modify it without
         * DUE care. Case 1 does not require any special handling. Case 2.1 is partly handled earlier, when flags are
         * added to the coexpression objects for non-specific query gene probes.
         */

        int positiveLinkCount = 0;
        int negativeLinkCount = 0;

        Set<CoexpressedGenePairValueObject> toRemove = new HashSet<CoexpressedGenePairValueObject>();

        /*
         * Iterate over all the coexpression data (per target gene)
         */
        coexp: for ( CoexpressedGenePairValueObject coExValObj : getCoexpressionData( 0 ) ) {

            Map<Long, Collection<ProbePair>> links = coExValObj.getLinks();

            Map<Long, Collection<Long>> queryProbeInfo = coExValObj.getQueryProbeInfo();

            if ( log.isDebugEnabled() ) log.debug( "Gene: " + coExValObj );

            // Fill in information about the other genes these probes hybridize to, if any.

            // For each experiment with coexpression data...
            for ( Long eeID : coExValObj.getExpressionExperiments() ) {

                /*
                 * Fill in 'experiment has specific probes' information.
                 */
                processLinksForSpecificity( coExValObj, links, eeID );

                /*
                 * Look for probes that cross-hybridize to the query. These are completely removed.
                 */
                Collection<Long> qp = queryProbeInfo.get( eeID );

                if ( log.isDebugEnabled() )
                    log.debug( "Query probes in " + eeID + ": " + StringUtils.join( qp, "," ) );

                // For each probe ...
                Collection<Long> coexpressedProbes = coExValObj.getProbes( eeID );
                probe: for ( Long coexpressedProbe : coexpressedProbes ) {

                    Collection<Long> genesForProbe = getGenesForQueryProbe( eeID, coexpressedProbe );
                    if ( genesForProbe == null || genesForProbe.isEmpty() ) {
                        // This can happen if we removed the probe in the inner loop.
                        continue;
                    }

                    /*
                     * Each probe can hybridize to multiple genes. There are two cases (which can co-occur): 1) The
                     * 'other' gene is the same as the query gene, in which case we remove the result; 2) otherwise we
                     * just make a record of the crosshyb.
                     */
                    for ( Long geneID : genesForProbe ) {

                        /*
                         * Check for query probe that hits the coexpressed gene. This is case 2.1
                         */
                        for ( Long queryProbe : qp ) {
                            Collection<Long> genesForQueryProbe = getGenesForProbe( eeID, queryProbe );
                            if ( genesForQueryProbe != null && genesForQueryProbe.contains( geneID ) ) {
                                /*
                                 * Note: the coexpressedProbe is often the same as the queryID
                                 */
                                this.removeProbeDataForEE( eeID, coexpressedProbe );
                                toRemove.add( coExValObj );
                                continue probe;
                            }
                        }

                        if ( log.isDebugEnabled() ) {
                            log.debug( "EEID=" + eeID + " Probe=" + coexpressedProbe + " Gene=" + geneID );
                        }

                        if ( geneID.equals( queryGene ) ) {
                            /*
                             * Case 3.1
                             */
                            if ( log.isDebugEnabled() ) {
                                log.debug( "Crosshyb with query gene id=" + queryGene + ": ee=" + eeID + " , probe="
                                        + coexpressedProbe + " hits genes=" + StringUtils.join( genesForProbe, "," ) );
                            }

                            /*
                             * If this probe also hybridizes with the query gene's probe, then we chuck it.
                             */
                            boolean hasAnyEvidenceLeft = coExValObj.removeProbeEvidence( coexpressedProbe, eeID );

                            /*
                             * This call modifies the map we are iterating over, so we have to check that there is still
                             * anything left.
                             */
                            this.removeProbeDataForEE( eeID, coexpressedProbe );

                            if ( !hasAnyEvidenceLeft ) {
                                // We have to remove this coExValObj from the resultset.
                                if ( log.isDebugEnabled() )
                                    log.debug( "No evidence left : ee=" + eeID + " , crosshyb probe="
                                            + coexpressedProbe + " to genes "
                                            + org.apache.commons.lang.StringUtils.join( genesForProbe, ',' ) );
                                toRemove.add( coExValObj );
                                continue coexp;
                            }
                            continue probe;
                        }
                        /*
                         * Otherwise we add a crosshybridizing gene. Case 3.2.
                         */
                        coExValObj.addCrossHybridizingGene( geneID );
                    }
                }
            }

            if ( toRemove.contains( coExValObj ) ) continue;

            boolean keep = false;
            if ( coExValObj.getPositiveLinkSupport() != 0
                    && coExValObj.getPositiveLinkSupport() >= this.supportThreshold ) {
                positiveLinkCount++;
                incrementEEContributions( coExValObj.getEEContributing2PositiveLinks() );
                keep = true;
            }

            if ( coExValObj.getNegativeLinkSupport() != 0
                    && coExValObj.getNegativeLinkSupport() >= this.supportThreshold ) {
                negativeLinkCount++;
                incrementEEContributions( coExValObj.getEEContributing2NegativeLinks() );
                keep = true;
            }

            if ( !keep ) {
                toRemove.add( coExValObj );
            }

            assert coExValObj.getExpressionExperiments().size() <= coExValObj.getPositiveLinkSupport()
                    + coExValObj.getNegativeLinkSupport() : "got " + coExValObj.getExpressionExperiments().size()
                    + " expected " + ( coExValObj.getPositiveLinkSupport() + coExValObj.getNegativeLinkSupport() );

        }

        for ( CoexpressedGenePairValueObject cvo : toRemove ) {
            if ( log.isDebugEnabled() ) log.debug( "Removing gene: " + cvo.getCoexpressedGeneId() );
            this.coexpressionData.remove( cvo.getCoexpressedGeneId() );
        }

        // add count of pruned matches to coexpression data
        setPositiveStringencyLinkCount( positiveLinkCount );
        setNegativeStringencyLinkCount( negativeLinkCount );

        watch.stop();
        Long elapsed = watch.getTime();

        if ( elapsed > 250 ) log.info( "Specificity check: " + elapsed + "ms." );
    }

    /**
     * @param errorState
     */
    public void setErrorState( String errorState ) {
        this.errorState = errorState;
    }

    /**
     * @return a unique collection of probeIDs that the queried gene was expressed from
     */
    public Collection<Long> getQueryGeneProbes() {
        Collection<Long> results = new HashSet<Long>();

        for ( Long eeID : queryProbes.keySet() ) {
            results.addAll( queryProbes.get( eeID ).keySet() );
        }

        return results;
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

            }
        }
    }

    public void setTargetGeneSpecificityInfo( Map<Long, Collection<Long>> probe2GeneMap ) {
        for ( Long eeID : queryProbes.keySet() ) {
            addTargetSpecificityData( eeID, probe2GeneMap );
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append( this.coexpressionData.size() + " coexpressions with " + this.queryGene + ":\n" );

        for ( CoexpressedGenePairValueObject cvo : this.coexpressionData.values() ) {
            buf.append( cvo.toString() + "\n-------------------\n" );
        }
        return buf.toString();
    }

    /**
     * Populate information about probe -> gene relationships for a single probe, for the query gene's probe.
     * 
     * @param eeID
     * @param probe2geneMap populated from cs2gene query.
     */
    void addQuerySpecificityInfo( Long eeID, Map<Long, Collection<Long>> probe2geneMap ) {
        this.queryProbes.put( eeID, probe2geneMap );
    }

    /**
     * Populate information about probe -> gene relationships for a single probe, for the coexpressed (target) gene's
     * probe.
     * 
     * @param eeID
     * @param probe2geneMap
     */
    void addTargetSpecificityInfo( Long eeID, Map<Long, Collection<Long>> probe2geneMap ) {
        if ( !this.expressionExperimentProbe2GeneMaps.containsKey( eeID ) ) {
            this.expressionExperimentProbe2GeneMaps.put( eeID, new HashMap<Long, Collection<Long>>() );
        }
        this.expressionExperimentProbe2GeneMaps.get( eeID ).putAll( probe2geneMap );
    }

    boolean hasExpressionExperiment( Long eeID ) {
        return this.expressionExperiments.containsKey( eeID );
    }

    private void addQuerySpecificityData( Long eeID, Map<Long, Collection<Long>> probe2GeneMap ) {
        this.addQuerySpecificityInfo( eeID, probe2GeneMap );
    }

    private void addTargetSpecificityData( Long eeID, Map<Long, Collection<Long>> probe2GeneMap ) {
        this.addTargetSpecificityInfo( eeID, probe2GeneMap );
    }

    /**
     * @param eeID
     * @param probeID
     * @return a collection of gene IDs that the probe is predicted to detect
     */
    private Collection<Long> getGenesForProbe( Long eeID, Long probeID ) {
        Map<Long, Collection<Long>> map = expressionExperimentProbe2GeneMaps.get( eeID );
        if ( map == null ) {
            return new HashSet<Long>();
        }
        return map.get( probeID );
    }

    /**
     * @param eeID
     * @param probeID
     * @return
     */
    private Collection<Long> getGenesForQueryProbe( Long eeID, Long probeID ) {
        return queryProbes.get( eeID ).get( probeID );
    }

    /**
     * Counting up how many support-threshold exceeding links each data set contributed.
     * 
     * @param contributingEEs
     */
    private void incrementEEContributions( Collection<Long> contributingEEs ) {

        for ( Long eeID : contributingEEs ) {

            if ( !expressionExperiments.containsKey( eeID ) ) {
                expressionExperiments.put( eeID, 1 );
            } else {
                expressionExperiments.put( eeID, expressionExperiments.get( eeID ) + 1 );
            }

        }
    }

    /**
     * Fill in specificity information for this experiment, based on the pairs.
     * 
     * @param coExValObj
     * @param links
     * @param eeID
     */
    private void processLinksForSpecificity( CoexpressedGenePairValueObject coExValObj,
            Map<Long, Collection<ProbePair>> links, Long eeID ) {
        Collection<ProbePair> rawLinks = links.get( eeID );

        if ( rawLinks == null || rawLinks.size() == 0 ) {
            throw new IllegalStateException();
        }

        boolean isSpecific = false;
        for ( ProbePair probePair : rawLinks ) {

            Long queryProbeId = probePair.getQueryProbeId();
            Collection<Long> genesForQueryProbe = getGenesForQueryProbe( eeID, queryProbeId );
            if ( genesForQueryProbe == null ) {
                /*
                 * There could be probes for which no gene mapping exists any more.
                 */
                if ( !warned ) {
                    log.warn( "No genes for query probe=" + queryProbeId + " in ee=" + eeID + " [" + this.queryGene
                            + "] " + " (Any additional warnings for this query gene will be at DEBUG level only)" );
                } else if ( log.isDebugEnabled() ) {
                    log.debug( "No genes for query probe=" + queryProbeId + " in ee=" + eeID );
                }
                warned = true;
                continue;
            }
            int numQueryGenesHit = genesForQueryProbe.size();

            Long targetProbeId = probePair.getTargetProbeId();
            Collection<Long> genesForProbe = getGenesForProbe( eeID, targetProbeId );
            if ( genesForProbe == null ) {
                if ( !warned ) {
                    log.warn( "No genes for target probe=" + targetProbeId + " in ee=" + eeID
                            + " (Any additional warnings for this link will be at DEBUG level only)" );
                } else if ( log.isDebugEnabled() ) {
                    log.debug( "No genes for target probe=" + targetProbeId + " in ee=" + eeID );
                }
                warned = true;
                continue;
            }
            int numTargetGenesHit = genesForProbe.size();

            /*
             * If a _single_ link is 'specific', then we count the ee. as 'specific'.
             */
            if ( numQueryGenesHit == 1 && numTargetGenesHit == 1 ) {
                isSpecific = true;
            }
        }

        if ( !isSpecific ) {
            coExValObj.getNonspecificEE().add( eeID );
        }
    }

    /**
     * Remove a probe.
     * 
     * @param eeID
     * @param probeID
     */
    private void removeProbeDataForEE( Long eeID, Long probeID ) {
        Map<Long, Collection<Long>> eeData = this.expressionExperimentProbe2GeneMaps.get( eeID );
        if ( eeData != null ) {
            eeData.remove( probeID );
            if ( eeData.size() == 0 ) {
                /*
                 * This includes 'low-stringency' data so usually this doesn't really happen.
                 */
                this.expressionExperimentProbe2GeneMaps.remove( eeID );
                this.expressionExperiments.remove( eeID );
            }
        }
    }

    /**
     * @param stringencyLinkCount the stringencyLinkCount to set
     */
    private void setNegativeStringencyLinkCount( int stringencyLinkCount ) {
        this.negativeStringencyLinkCount = stringencyLinkCount;
    }

    /**
     * @param stringencyLinkCount the stringencyLinkCount to set
     */
    private void setPositiveStringencyLinkCount( int stringencyLinkCount ) {
        this.positiveStringencyLinkCount = stringencyLinkCount;
    }

}
