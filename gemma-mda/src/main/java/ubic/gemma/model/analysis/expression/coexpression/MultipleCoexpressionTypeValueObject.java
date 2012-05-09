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
import java.util.List;
import java.util.Map;

import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;

/**
 * @author klc This object is thread safe. Used for storing the information regarding different types of coexpressed
 *         genes. (Genes, Predicted Genes, Probe Aligned regions)
 */
public class MultipleCoexpressionTypeValueObject {

    public static final String GENE_IMPL = "GeneImpl";
    private Map<Long, ExpressionExperimentValueObject> expressionExperiments;

    private Map<Gene, CommonCoexpressionValueObject> geneToQueries;
    private Map<Long, Gene> geneLookup;

    public MultipleCoexpressionTypeValueObject() {

        geneToQueries = Collections.synchronizedMap( new HashMap<Gene, CommonCoexpressionValueObject>() );
        geneLookup = Collections.synchronizedMap( new HashMap<Long, Gene>() );

        expressionExperiments = Collections.synchronizedMap( new HashMap<Long, ExpressionExperimentValueObject>() );
    }

    public void addCoexpressionCollection( Gene queryGene, CoexpressedGenesDetails coexpressionType ) {
        synchronized ( this ) {
            for ( CoexpressionValueObject coexpressed : coexpressionType.getCoexpressionData( 0 ) ) {
                getQueriesForGene( getGene( coexpressed ) ).add(
                        new QueryGeneCoexpressionDataPair( queryGene, coexpressed ) );
            }
            for ( ExpressionExperimentValueObject eevo : coexpressionType.getExpressionExperiments() ) {
                expressionExperiments.put( eevo.getId(), eevo );
            }
        }
    }

    /**
     * @param cutoff the minimum number of query genes to pass the filter
     * @return those coexpressed genes that are common to multiple query genes
     */
    public Collection<CommonCoexpressionValueObject> getCommonCoexpressedGenes( int cutoff ) {
        synchronized ( this ) {
            List<Long> allEEIds = new ArrayList<Long>( getExpressionExperimentIds() );
            Collection<CommonCoexpressionValueObject> coexpressedGenes = new ArrayList<CommonCoexpressionValueObject>();
            for ( CommonCoexpressionValueObject candidate : geneToQueries.values() ) {
                if ( candidate.getCommonCoexpressedQueryGenes().size() >= cutoff ) {
                    coexpressedGenes.add( candidate );
                    candidate.computeExperimentBits( allEEIds );
                }
            }
            return coexpressedGenes;
        }
    }

    /**
     * @param eeID expressionExperiment ID
     * @return an expressionexperimentValueObject or null if it isn't there
     */
    public ExpressionExperimentValueObject getExpressionExperiment( Long eeID ) {
        return expressionExperiments.get( eeID );
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

    public Integer getLinkCountForEE( Long id ) {

        ExpressionExperimentValueObject eeVo = expressionExperiments.get( id );

        if ( ( eeVo == null ) || ( eeVo.getCoexpressionLinkCount() == null ) ) return 0;

        return eeVo.getCoexpressionLinkCount();

    }

    /**
     * @return the number of genes
     */
    public int getNumberOfGenes() {
        return geneToQueries.keySet().size();
    }

    /**
     * @param gene the Gene of interest
     * @return a subset of the CommonCoexpressionValueObjects that exhibit coexpression with the specified Gene
     */
    public CommonCoexpressionValueObject getQueriesForGene( Gene gene ) {
        synchronized ( this ) {
            CommonCoexpressionValueObject queries = geneToQueries.get( gene );
            if ( queries == null ) {
                queries = new CommonCoexpressionValueObject( gene );
                geneToQueries.put( gene, queries );
            }
            return queries;
        }
    }

    /**
     * @param id
     * @return an int representing the raw number of links a given ee contributed to the coexpression search
     */
    public Integer getRawLinkCountForEE( Long id ) {

        ExpressionExperimentValueObject eeVo = expressionExperiments.get( id );

        if ( ( eeVo == null ) || ( eeVo.getRawCoexpressionLinkCount() == null ) ) return 0;

        return eeVo.getRawCoexpressionLinkCount();
    }

    private Gene getGene( CoexpressionValueObject coexpressed ) {
        // lookup might be unnecessary optimization; whole thing might be unnecessary if we can use load...
        // another option is to just maintain the 4 things we have in a local object until we create the
        // CommonCoexpressionValueObject...
        Gene gene = null;
        synchronized ( geneLookup ) {
            gene = geneLookup.get( coexpressed.getGeneId() );
            if ( gene == null ) {

                gene = Gene.Factory.newInstance();
                gene.setId( coexpressed.getGeneId() );
                gene.setName( coexpressed.getGeneName() );
                gene.setOfficialName( coexpressed.getGeneOfficialName() );
                geneLookup.put( coexpressed.getGeneId(), gene );
            }
        }
        return gene;
    }

    // /**
    // * @return returns map of genes to a collection of expression experiment IDs that contained
    // <strong>specific</strong>
    // * probes (probes that hit only 1 gene) for that gene.
    // * <p>
    // * If an expression exp has two (or more) probes that hit the same gene, and one probe is specific, even if
    // * some of the other(s) are not this EE is considered specific and will still be returned.
    // */
    // public Map<Long, Collection<Long>> getSpecificExpressionExperiments() {
    //
    // Map<Long, Collection<Long>> specificEE = Collections.synchronizedMap( new HashMap<Long, Collection<Long>>() );
    //
    // synchronized ( crossHybridizingProbes ) {
    // for ( Long eeID : crossHybridizingProbes.keySet() ) {
    //
    // // this is a map for ALL the probes from this data set that came up.
    // Map<Long, Collection<Long>> probe2geneMap = crossHybridizingProbes.get( eeID );
    //
    // for ( Long probeID : probe2geneMap.keySet() ) {
    //
    // Collection<Long> genes = probe2geneMap.get( probeID );
    // Integer genecount = genes.size();
    //
    // for ( Long geneId : genes ) {
    //
    // if ( !specificEE.containsKey( geneId ) ) {
    // specificEE.put( geneId, new HashSet<Long>() );
    // }
    //
    // if ( genecount == 1 ) {
    // specificEE.get( geneId ).add( eeID );
    // }
    // }
    //
    // }
    //
    // }
    // }
    //
    // return specificEE;
    // }
    //
    // /**
    // * @param eeID
    // * @returns a collection of Probe IDs for a given expression experiment that hybrydized to more than 1 gene
    // */
    // public Collection<Long> getNonSpecificProbes( Long eeID ) {
    // Collection<Long> nonSpecificProbes = Collections.synchronizedSet( new HashSet<Long>() );
    //
    // Map<Long, Collection<Long>> probe2geneMap = crossHybridizingProbes.get( eeID );
    // synchronized ( probe2geneMap ) {
    // for ( Long probeID : probe2geneMap.keySet() ) {
    // Collection genes = probe2geneMap.get( probeID );
    // if ( genes.size() > 1 ) nonSpecificProbes.add( eeID );
    // }
    // }
    // return nonSpecificProbes;
    // }
    //
    // /**
    // * @param eeID
    // * @param probeID
    // * @return a collection of gene IDs or null if the eeID and probeID were not found
    // */
    // public Collection<Long> getNonSpecificGenes( Long eeID, Long probeID ) {
    //
    // if ( crossHybridizingProbes.containsKey( eeID ) )
    // if ( crossHybridizingProbes.get( eeID ).containsKey( probeID ) )
    // return crossHybridizingProbes.get( eeID ).get( probeID );
    //
    // return null;
    // }
    //
    // /**
    // * @return the coexpressionData
    // */
    // public Collection<CoexpressionValueObject> getCoexpressionData() {
    // return coexpressionData;
    // }
    //
    // /**
    // * @param coexpressionData the coexpressionData to set
    // */
    // public void setCoexpressionData( Collection<CoexpressionValueObject> coexpressionData ) {
    // this.coexpressionData = coexpressionData;
    // }
    //
    //
    // /**
    // * @return the stringencyLinkCount
    // */
    // public int getPositiveStringencyLinkCount() {
    // return positiveStringencyLinkCount;
    // }
    //
    // /**
    // * @param stringencyLinkCount the stringencyLinkCount to set
    // */
    // public void setPositiveStringencyLinkCount( int stringencyLinkCount ) {
    // this.positiveStringencyLinkCount = stringencyLinkCount;
    // }
    //
    // /**
    // * @return the stringencyLinkCount
    // */
    // public int getNegativeStringencyLinkCount() {
    // return negativeStringencyLinkCount;
    // }
    //
    // /**
    // * @param stringencyLinkCount the stringencyLinkCount to set
    // */
    // public void setNegativeStringencyLinkCount( int stringencyLinkCount ) {
    // this.negativeStringencyLinkCount = stringencyLinkCount;
    // }
    //
    // /**
    // * @return the expressionExperiments that were searched for coexpression
    // */
    // public Collection<ExpressionExperimentValueObject> getExpressionExperiments() {
    // return expressionExperiments.values();
    // }
    //
    // /**
    // * @return a collection of expressionExperiment Ids that were searched for coexpression
    // */
    // public Collection<Long> getExpressionExperimentIds() {
    // return expressionExperiments.keySet();
    // }
    //
    // /**
    // * Add an expression experiment to the list
    // *
    // * @param vo
    // */
    // public void addExpressionExperiment( ExpressionExperimentValueObject vo ) {
    // Long id = vo.getId();
    // if ( !expressionExperiments.containsKey( id ) ) this.expressionExperiments.put( id, vo );
    // }
    //
    // /**
    // * @param eeID expressionExperiment ID
    // * @return an expressionexperimentValueObject or null if it isn't there
    // */
    // public ExpressionExperimentValueObject getExpressionExperiment( Long eeID ) {
    //
    // if ( expressionExperiments.containsKey( eeID ) ) return this.expressionExperiments.get( eeID );
    //
    // return null;
    // }
    //
    // /**
    // * Add a collection of expression experiment to the list
    // *
    // * @param vo
    // */
    // public void addExpressionExperiments( Collection<ExpressionExperimentValueObject> vos ) {
    // synchronized ( vos ) {
    // for ( ExpressionExperimentValueObject eeVo : vos )
    // addExpressionExperiment( eeVo );
    // }
    // }
    //
    // /**
    // * @return the number of Genes that met the stringency requirement
    // */
    // public int getNumberOfStringencyGenes() {
    // return this.coexpressionData.size();
    // }
    //
    // /**
    // * @return the number of StringencyGenes
    // */
    // public int getNumberOfGenes() {
    // return numGenes;
    // }
    //
    // /**
    // * @param numGenes the total number of Genes that were coexpressed.
    // */
    // public void setNumberOfGenes( int numGenes ) {
    // this.numGenes = numGenes;
    // }
    //
    // public int getNumberOfUsedExpressonExperiments() {
    // return crossHybridizingProbes.keySet().size();
    //
    // }
    //
    // /**
    // * @param id
    // * @return an int representing the raw number of links a given ee contributed to the coexpression search
    // */
    // public Long getRawLinkCountForEE( Long id ) {
    //
    // ExpressionExperimentValueObject eeVo = expressionExperiments.get( id );
    //
    // if (( eeVo == null) || ( eeVo.getRawCoexpressionLinkCount() == null ))
    // return ( long ) 0;
    // else
    // return eeVo.getRawCoexpressionLinkCount();
    // }
    //
    // public Long getLinkCountForEE( Long id ) {
    //
    // ExpressionExperimentValueObject eeVo = expressionExperiments.get( id );
    //
    // if (( eeVo == null) || (eeVo.getCoexpressionLinkCount() == null ))
    // return ( long ) 0;
    // else
    // return eeVo.getCoexpressionLinkCount();
    //
    // }

}
