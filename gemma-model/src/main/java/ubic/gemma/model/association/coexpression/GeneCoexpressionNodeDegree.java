/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.association.coexpression;

import java.io.Serializable;

import ubic.gemma.model.genome.Gene;

/**
 * Represents the coexpression node degree for a gene summarized across experiments, at each level of support.
 * 
 * @author paul
 */
public abstract class GeneCoexpressionNodeDegree implements Serializable {

    public static final class Factory {
        public static GeneCoexpressionNodeDegree newInstance( Gene g ) {
            return new GeneCoexpressionNodeDegreeImpl( g );
        }
    }

    // our primary key
    private Long geneId = null;

    /**
     * Byte format of a int array. the first value is 0; the other values is the number of links at support=index.
     * Unlike the relativeLinkRanks these are not cumulative.
     */
    private byte[] linkCountsNegative;

    /**
     * Byte format of a int array. the first value is 0; the other values is the number of links at support=index.
     * Unlike the relativeLinkRanks these are not cumulative.
     */
    private byte[] linkCountsPositive;

    /**
     * Normalized rank values for the node degree of this gene at each threshold of support; that is, "at or above" the
     * threshold. The ranking is among all other genes for the taxon; the normalization factor is the node degree of the
     * most hubby gene (computed separately for each support threshold).
     */
    private byte[] relativeLinkRanksNegative;

    /**
     * Normalized rank values for the node degree of this gene at each threshold of support; that is, "at or above" the
     * threshold. The ranking is among all other genes for the taxon; the normalization factor is the node degree of the
     * most hubby gene (computed separately for each support threshold).
     */
    private byte[] relativeLinkRanksPositive;

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        GeneCoexpressionNodeDegree other = ( GeneCoexpressionNodeDegree ) obj;
        if ( geneId == null ) {
            if ( other.geneId != null ) return false;
        } else if ( !geneId.equals( other.geneId ) ) return false;
        return true;
    }

    /**
     * @return
     */
    public Long getGeneId() {
        return geneId;
    }

    public Long getId() {
        return geneId;
    }

    public byte[] getLinkCountsNegative() {
        return linkCountsNegative;
    }

    public byte[] getLinkCountsPositive() {
        return linkCountsPositive;
    }

    /**
     * Note that these values are for support thresholds, not support levels - so "at or above" the given threshold
     * support.
     * 
     * @return
     */
    public byte[] getRelativeLinkRanksNegative() {
        return relativeLinkRanksNegative;
    }

    /**
     * Note that these values are for support thresholds, not support levels - so "at or above" the given threshold
     * support.
     * 
     * @return
     */
    public byte[] getRelativeLinkRanksPositive() {
        return relativeLinkRanksPositive;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( geneId == null ) ? 0 : geneId.hashCode() );
        return result;
    }

    /**
     * @param geneId
     */
    public void setGeneId( Long geneId ) {
        this.geneId = geneId;
    }

    public void setLinkCountsNegative( byte[] linkCountsNegative ) {
        this.linkCountsNegative = linkCountsNegative;
    }

    public void setLinkCountsPositive( byte[] linkCountsPositive ) {
        this.linkCountsPositive = linkCountsPositive;
    }

    public void setRelativeLinkRanksNegative( byte[] relativeLinkRanksNegative ) {
        this.relativeLinkRanksNegative = relativeLinkRanksNegative;
    }

    public void setRelativeLinkRanksPositive( byte[] relativeLinkRanksPositive ) {
        this.relativeLinkRanksPositive = relativeLinkRanksPositive;
    }

}