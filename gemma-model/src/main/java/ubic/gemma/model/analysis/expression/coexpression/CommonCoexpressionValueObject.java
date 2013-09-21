/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ubic.gemma.model.genome.Gene;

/**
 * @author luke The CommonCoexpressionValueObject
 */
public class CommonCoexpressionValueObject {

    // private String geneName;
    // private Long geneId;
    // private String geneOfficialName;
    // private String geneType;

    private Gene gene;

    private Collection<Long> commonCoexpressedQueryGenes;
    private Collection<Long> commonPositiveCoexpressedQueryGenes;
    private Collection<Long> commonNegativeCoexpressedQueryGenes;

    private Collection<Long> positiveExperimentIds;
    private Collection<Long> negativeExperimentIds;

    private List<Integer> experimentBitList = new ArrayList<Integer>();

    /**
     * @param gene
     * @param coexpressed
     */
    public CommonCoexpressionValueObject( Gene gene ) {
        this.gene = gene;

        commonCoexpressedQueryGenes = Collections.synchronizedSet( new HashSet<Long>() );
        commonPositiveCoexpressedQueryGenes = Collections.synchronizedSet( new HashSet<Long>() );
        commonNegativeCoexpressedQueryGenes = Collections.synchronizedSet( new HashSet<Long>() );

        positiveExperimentIds = Collections.synchronizedSet( new HashSet<Long>() );
        negativeExperimentIds = Collections.synchronizedSet( new HashSet<Long>() );

        experimentBitList = new ArrayList<Integer>();
        // commonCoexpressionData = new ArrayList<QueryGeneCoexpressionDataPair>();
    }

    public void add( QueryGeneCoexpressionDataPair coexpressed ) {
        commonCoexpressedQueryGenes.add( coexpressed.getQueryGene() );
        if ( coexpressed.getCoexpressionData().getPositiveLinkSupport() != 0 )
            commonPositiveCoexpressedQueryGenes.add( coexpressed.getQueryGene() );
        if ( coexpressed.getCoexpressionData().getNegativeLinkSupport() != 0 )
            commonNegativeCoexpressedQueryGenes.add( coexpressed.getQueryGene() );

        positiveExperimentIds.addAll( coexpressed.getCoexpressionData().getEEContributing2PositiveLinks() );
        negativeExperimentIds.addAll( coexpressed.getCoexpressionData().getEEContributing2NegativeLinks() );
        // commonCoexpressionData.add( coexpressed );
    }

    /**
     * Initialize the vector of 'bits'.
     * 
     * @param eeIds
     */
    public void computeExperimentBits( List<Long> eeIds ) {
        experimentBitList.clear();
        for ( Long long1 : eeIds ) {
            if ( positiveExperimentIds.contains( long1 ) || negativeExperimentIds.contains( long1 ) ) {
                experimentBitList.add( 20 );
            } else {
                experimentBitList.add( 1 );
            }
        }
    }

    /**
     * @return the collection of query genes this gene was coexpressed with
     */
    public Collection<Long> getCommonCoexpressedQueryGenes() {
        return commonCoexpressedQueryGenes;
    }

    /**
     * @return the collection of query genes this gene was positively coexpressed with
     */
    public Collection<Long> getCommonNegativeCoexpressedQueryGenes() {
        return commonNegativeCoexpressedQueryGenes;
    }

    /**
     * @return the collection of query genes this gene was positively coexpressed with
     */
    public Collection<Long> getCommonPositiveCoexpressedQueryGenes() {
        return commonPositiveCoexpressedQueryGenes;
    }

    /**
     * @return a collection of EE ids that contributed to this genes negative expression
     */
    public Collection<Long> getEEContributing2NegativeLinks() {
        return negativeExperimentIds;
    }

    /**
     * @return a collectino of EEids that contributed to this genes positive expression
     */
    public Collection<Long> getEEContributing2PositiveLinks() {
        return positiveExperimentIds;
    }

    public String getExperimentBitList() {
        return StringUtils.join( experimentBitList, "," );
    }

    /**
     * @return the geneId
     */
    public Long getGeneId() {
        return gene.getId();
    }

    /**
     * @return the geneName
     */
    public String getGeneName() {
        return gene.getName();
    }

    // /**
    // * @return the collection of CoexpressionCollectionValueObjects representing the query genes this gene was
    // coexpressed with
    // */
    // public Collection<QueryGeneCoexpressionDataPair> getCommonCoexpressionData() {
    // return commonCoexpressionData;
    // }

    /**
     * @return the geneOfficialName
     */
    public String getGeneOfficialName() {
        return gene.getOfficialName();
    }

    /**
     * @return the geneType
     */
    public String getGeneType() {
        return Gene.class.getName();
    }

    /**
     * Function to return the max of negative or positive link count. This is used for sorting.
     * 
     * @return the max of negative or positive link count
     */
    public Integer getMaxLinkCount() {
        return Math.max( commonPositiveCoexpressedQueryGenes.size(), commonNegativeCoexpressedQueryGenes.size() );
    }
}
