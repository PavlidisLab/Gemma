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
import java.util.Map;

import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;

/**
 * The details about coexpression for multiple target genes with respect to a query gene. All the genes should be of the
 * same subclass (known genes, predicted genes or probe aligned regions). Thus we usually have three instances of this
 * for each query gene.
 * 
 * @author klc
 * @version $Id$
 */
public class CoexpressedGenesDetails {

    /**
     * Number of links that passed the stringency requirements
     */
    private int positiveStringencyLinkCount;
    private int negativeStringencyLinkCount;

    /**
     * the expression experiments that show up in the results - they contribute raw links.
     */
    private Map<Long, ExpressionExperimentValueObject> expressionExperiments;

    /**
     * Details for each gene. Map of gene id to vos.
     */
    private Map<Long, CoexpressionValueObject> coexpressionData;

    /**
     * The gene used to search from.
     */
    private Gene queryGene;

    /**
     * Map of EE -> Probe -> Genes
     */
    private Map<Long, Map<Long, Collection<Long>>> expressionExperimentProbe2GeneMaps;

    private final int supportThreshold;

    public CoexpressedGenesDetails( Gene queryGene, int supportThreshold ) {

        this.queryGene = queryGene;
        this.supportThreshold = supportThreshold;

        positiveStringencyLinkCount = 0;
        negativeStringencyLinkCount = 0;

        coexpressionData = new HashMap<Long, CoexpressionValueObject>();
        expressionExperiments = new HashMap<Long, ExpressionExperimentValueObject>();
        expressionExperimentProbe2GeneMaps = new HashMap<Long, Map<Long, Collection<Long>>>();
    }

    public CoexpressionValueObject add( CoexpressionValueObject value ) {
        return coexpressionData.put( value.getGeneId(), value );
    }

    /**
     * Add an expression experiment to the list
     * 
     * @param vo
     */
    public void addExpressionExperiment( ExpressionExperimentValueObject vo ) {
        Long id = vo.getId();
        this.expressionExperiments.put( id, vo );
    }

    /**
     * @param eeID
     * @param probeID
     * @param geneID
     */
    public void addSpecificityInfo( Long eeID, Long probeID, Long geneID ) {
        Map<Long, Collection<Long>> probe2geneMap = getOrInitSpecificityMap( eeID, probeID );
        probe2geneMap.get( probeID ).add( geneID );
    }

    public boolean containsKey( Object key ) {
        return coexpressionData.containsKey( key );
    }

    /**
     * @param geneId
     * @return
     */
    public CoexpressionValueObject get( Long geneId ) {
        return this.coexpressionData.get( geneId );
    }

    /**
     * @param supportThreshold
     * @return the coexpressionData
     */
    public Collection<CoexpressionValueObject> getCoexpressionData( int supportThreshold ) {
        Collection<CoexpressionValueObject> result = new HashSet<CoexpressionValueObject>();
        for ( CoexpressionValueObject o : coexpressionData.values() ) {
            if ( o.getNegativeLinkSupport() >= supportThreshold || o.getPositiveLinkSupport() >= supportThreshold ) {
                result.add( o );
            }
        }
        return result;
    }

    /**
     * @param eeID expressionExperiment ID
     * @return an expressionexperimentValueObject or null if it isn't there
     */
    public ExpressionExperimentValueObject getExpressionExperiment( Long eeID ) {
        return this.expressionExperiments.get( eeID );
    }

    /**
     * @return a collection of expressionExperiment Ids that were searched for coexpression
     */
    public Collection<Long> getExpressionExperimentIds() {
        return expressionExperiments.keySet();
    }

    /**
     * @return the expressionExperiments that were searched for coexpression
     */
    public Collection<ExpressionExperimentValueObject> getExpressionExperiments() {
        return expressionExperiments.values();
    }

    /**
     * @param id
     * @return
     */
    public Long getLinkCountForEE( Long id ) {

        ExpressionExperimentValueObject eeVo = expressionExperiments.get( id );

        if ( eeVo == null || eeVo.getCoexpressionLinkCount() == null ) return 0L;

        return eeVo.getCoexpressionLinkCount();

    }

    /**
     * @return the stringencyLinkCount
     */
    public int getNegativeStringencyLinkCount() {
        return negativeStringencyLinkCount;
    }

    /**
     * @param eeID FIXME why is this actually needed?
     * @param probeID
     * @return a collection of gene IDs that the probe is predicted to detect
     */
    public Collection<Long> getGenesForProbe( Long eeID, Long probeID ) {
        return expressionExperimentProbe2GeneMaps.get( eeID ).get( probeID );
    }

    /**
     * @param eeID
     * @returns a collection of Probe IDs for a given expression experiment that hybrydized to more than 1 gene
     */
    public Collection<Long> getNonSpecificProbes( Long eeID ) {
        Collection<Long> nonSpecificProbes = Collections.synchronizedSet( new HashSet<Long>() );

        Map<Long, Collection<Long>> probe2geneMap = expressionExperimentProbe2GeneMaps.get( eeID );
        synchronized ( probe2geneMap ) {
            for ( Long probeID : probe2geneMap.keySet() ) {
                Collection genes = probe2geneMap.get( probeID );
                if ( genes.size() > 1 ) nonSpecificProbes.add( eeID );
            }
        }
        return nonSpecificProbes;
    }

    /**
     * @return the number of StringencyGenes
     */
    public int getNumberOfGenes() {
        return this.coexpressionData.size();
    }

    /**
     * @return
     */
    public int getNumberOfUsedExpressonExperiments() {
        return expressionExperimentProbe2GeneMaps.keySet().size();

    }

    /**
     * @return the stringencyLinkCount
     */
    public int getPositiveStringencyLinkCount() {
        return positiveStringencyLinkCount;
    }

    /**
     * @param id
     * @return an int representing the raw number of links a given ee contributed to the coexpression search
     */
    public Long getRawLinkCountForEE( Long id ) {

        ExpressionExperimentValueObject eeVo = expressionExperiments.get( id );

        if ( eeVo == null || eeVo.getRawCoexpressionLinkCount() == null ) {
            return ( long ) 0;
        }

        return eeVo.getRawCoexpressionLinkCount();
    }

    /**
     * @return map of genes to a collection of expression experiment IDs that contained <strong>specific</strong>
     *         probes (probes that hit only 1 gene) for that gene.
     *         <p>
     *         If an EE has two (or more) probes that hit the same gene, and one probe is specific, even if some of the
     *         other(s) are not, the EE is considered specific and will still be included.
     */
    public Map<Long, Collection<Long>> getSpecificExpressionExperiments() {

        Map<Long, Collection<Long>> result = Collections.synchronizedMap( new HashMap<Long, Collection<Long>>() );
        for ( Long eeID : expressionExperimentProbe2GeneMaps.keySet() ) {

            // this is a map for ALL the probes from this data set that came up.
            Map<Long, Collection<Long>> probe2geneMap = expressionExperimentProbe2GeneMaps.get( eeID );

            for ( Long probeID : probe2geneMap.keySet() ) {

                Collection<Long> genes = probe2geneMap.get( probeID );
                Integer genecount = genes.size();

                for ( Long geneId : genes ) {

                    if ( !result.containsKey( geneId ) ) {
                        result.put( geneId, new HashSet<Long>() );
                    }

                    if ( genecount == 1 ) {
                        result.get( geneId ).add( eeID );
                    }
                }

            }
        }

        return result;
    }

    /**
     * @param querySpecificEE Expression experiments that have probes specific for the query gene.
     */
    public void postProcess( Collection<Long> querySpecificEEs ) {
        Map<Long, Collection<Long>> allSpecificEE = this.getSpecificExpressionExperiments();

        int positiveLinkCount = 0;
        int negativeLinkCount = 0;

        for ( CoexpressionValueObject coExValObj : getCoexpressionData( 0 ) ) {
            coExValObj.computeExperimentBits( new ArrayList<Long>( getExpressionExperimentIds() ) );

            Collection<Long> nonspecificEE = fillInNonspecificEEs( allSpecificEE, querySpecificEEs, coExValObj );

            // Fill in information about the other genes these probes hybridize to, if any.
            for ( Long eeID : nonspecificEE ) {

                for ( Long probeID : coExValObj.getProbes( eeID ) ) {

                    for ( Long geneID : getGenesForProbe( eeID, probeID ) ) {

                        if ( geneID.equals( queryGene.getId() ) ) {
                            coExValObj.setHybridizesWithQueryGene( true );
                        } else {
                            coExValObj.addCrossHybridizingGene( geneID );
                        }

                    }
                }

            }

            incrementRawEEContributions( coExValObj.getExpressionExperiments() );

            if ( coExValObj.getPositiveLinkSupport() != 0 ) {
                if ( coExValObj.getPositiveLinkSupport() >= this.supportThreshold ) {
                    positiveLinkCount++;
                }

                incrementEEContributions( coExValObj.getEEContributing2PositiveLinks() );
            }

            if ( coExValObj.getNegativeLinkSupport() != 0 ) {
                if ( coExValObj.getNegativeLinkSupport() >= this.supportThreshold ) {
                    negativeLinkCount++;
                }
                incrementEEContributions( coExValObj.getEEContributing2NegativeLinks() );
            }
        }

        // add count of pruned matches to coexpression data
        setPositiveStringencyLinkCount( positiveLinkCount );
        setNegativeStringencyLinkCount( negativeLinkCount );
    }

    /**
     * Update a coexpression value object with data on which expression experiments lacked probes specific for both the
     * query and target genes. The ids of expression experiments that were 'non-specific' are returned.
     * 
     * @param allSpecificEE Map of genes to EEs that had at least one specific probe for that gene.
     * @param querySpecificEEs IDs of EEs which had probes specific for the query gene.
     * @param coExValObj results for a single gene
     * @return ids of expression experiments that had only non-specific probes to the given gene. An ee is considered
     *         specific iff the ee has probes specific for both the query gene and the target gene.
     */
    private Collection<Long> fillInNonspecificEEs( Map<Long, Collection<Long>> allSpecificEE,
            Collection<Long> querySpecificEEs, CoexpressionValueObject coExValObj ) {

        Collection<Long> result = new HashSet<Long>();
        for ( Long eeId : coExValObj.getExpressionExperiments() ) {
            if ( allSpecificEE.get( coExValObj.getGeneId() ).contains( eeId ) && querySpecificEEs.contains( eeId ) ) {
                // then it is specific for this gene pair.
                continue;
            }
            result.add( eeId );
        }

        coExValObj.setNonspecificEE( result );
        return result;
    }

    /**
     * Counting up how many support-threshold exceeding links each data set contributed.
     * 
     * @param contributingEEs
     * @param coexpressions
     */
    private void incrementEEContributions( Collection<Long> contributingEEs ) {

        for ( Long eeID : contributingEEs ) {
            ExpressionExperimentValueObject eeVo = getExpressionExperiment( eeID );

            assert eeVo != null;

            if ( eeVo.getCoexpressionLinkCount() == null ) {
                eeVo.setCoexpressionLinkCount( new Long( 1 ) );
            } else {
                eeVo.setCoexpressionLinkCount( eeVo.getCoexpressionLinkCount() + 1 );
            }

        }
    }

    /**
     * Counting up how many links each data set contributed (including links that did not meet the stringency
     * threshold).
     * 
     * @param contributingEEs
     * @param coexpressions
     */
    private void incrementRawEEContributions( Collection<Long> contributingEEs ) {
        for ( Long eeID : contributingEEs ) {

            ExpressionExperimentValueObject eeVo = getExpressionExperiment( eeID );

            assert eeVo != null;

            if ( eeVo.getRawCoexpressionLinkCount() == null ) {
                eeVo.setRawCoexpressionLinkCount( new Long( 1 ) );
            } else {
                eeVo.setRawCoexpressionLinkCount( eeVo.getRawCoexpressionLinkCount() + 1 );
            }
        }
    }

    /**
     * @param stringencyLinkCount the stringencyLinkCount to set
     */
    public void setNegativeStringencyLinkCount( int stringencyLinkCount ) {
        this.negativeStringencyLinkCount = stringencyLinkCount;
    }

    /**
     * @param stringencyLinkCount the stringencyLinkCount to set
     */
    public void setPositiveStringencyLinkCount( int stringencyLinkCount ) {
        this.positiveStringencyLinkCount = stringencyLinkCount;
    }

    /**
     * @param eeID
     * @param probeID
     * @return
     */
    private Map<Long, Collection<Long>> getOrInitSpecificityMap( Long eeID, Long probeID ) {
        if ( !expressionExperimentProbe2GeneMaps.containsKey( eeID ) ) {
            Map<Long, Collection<Long>> probe2geneMap = Collections
                    .synchronizedMap( new HashMap<Long, Collection<Long>>() );
            expressionExperimentProbe2GeneMaps.put( eeID, probe2geneMap );
        }

        Map<Long, Collection<Long>> probe2geneMap = expressionExperimentProbe2GeneMaps.get( eeID );
        if ( !probe2geneMap.containsKey( probeID ) ) {
            Collection<Long> genes = Collections.synchronizedSet( new HashSet<Long>() );
            probe2geneMap.put( probeID, genes );
        }
        return probe2geneMap;
    }

}
