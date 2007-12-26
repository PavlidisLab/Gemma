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

import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

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
     * the expression experiments that were used. FIXME does this mean the EEs that were queried, or that show up in the
     * results?
     */
    private Map<Long, ExpressionExperimentValueObject> expressionExperiments;

    /**
     * Details for each gene. Map of gene id to vos.
     */
    private Map<Long, CoexpressionValueObject> coexpressionData;

    /**
     * Map of EE -> Probe -> Genes
     */
    private Map<Long, Map<Long, Collection<Long>>> expressionExperimentProbe2GeneMaps;

    public CoexpressedGenesDetails() {

        positiveStringencyLinkCount = 0;
        negativeStringencyLinkCount = 0;

        coexpressionData = new HashMap<Long, CoexpressionValueObject>();
        expressionExperiments = new HashMap<Long, ExpressionExperimentValueObject>();
        expressionExperimentProbe2GeneMaps = new HashMap<Long, Map<Long, Collection<Long>>>();
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

    /**
     * @param geneId
     * @return
     */
    public CoexpressionValueObject get( Long geneId ) {
        return this.coexpressionData.get( geneId );
    }

    /**
     * @return the coexpressionData
     */
    public Collection<CoexpressionValueObject> getCoexpressionData() {
        return coexpressionData.values();
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

        if ( ( eeVo == null ) || ( eeVo.getCoexpressionLinkCount() == null ) ) return ( long ) 0;

        return eeVo.getCoexpressionLinkCount();

    }

    /**
     * @return the stringencyLinkCount
     */
    public int getNegativeStringencyLinkCount() {
        return negativeStringencyLinkCount;
    }

    /**
     * @param eeID
     * @param probeID
     * @return a collection of gene IDs or null if the eeID and probeID were not found
     */
    public Collection<Long> getNonSpecificGenes( Long eeID, Long probeID ) {

        if ( expressionExperimentProbe2GeneMaps.containsKey( eeID ) )
            if ( expressionExperimentProbe2GeneMaps.get( eeID ).containsKey( probeID ) )
                return expressionExperimentProbe2GeneMaps.get( eeID ).get( probeID );

        return null;
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

        if ( ( eeVo == null ) || ( eeVo.getRawCoexpressionLinkCount() == null ) ) return ( long ) 0;

        return eeVo.getRawCoexpressionLinkCount();
    }

    /**
     * @return map of genes to a collection of expression experiment IDs that contained <strong>specific</strong>
     *         probes (probes that hit only 1 gene) for that gene.
     *         <p>
     *         If an EE has two (or more) probes that hit the same gene, and one probe is specific, even if some of the
     *         other(s) are not, the EE is considered specific and will still be returned.
     */
    public Map<Long, Collection<Long>> getSpecificExpressionExperiments() {

        Map<Long, Collection<Long>> result = Collections.synchronizedMap( new HashMap<Long, Collection<Long>>() );

        synchronized ( expressionExperimentProbe2GeneMaps ) {
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
        }

        return result;
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

    public boolean containsKey( Object key ) {
        return coexpressionData.containsKey( key );
    }

    public CoexpressionValueObject add( CoexpressionValueObject value ) {
        return coexpressionData.put( value.getGeneId(), value );
    }

}
