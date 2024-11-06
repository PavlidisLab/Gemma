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

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.genome.Gene;

import java.util.Objects;

/**
 * Represents the coexpression node degree for a gene summarized across experiments, at each level of support.
 *
 * @author paul
 */
public class GeneCoexpressionNodeDegree implements Identifiable {

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

    public GeneCoexpressionNodeDegree() {
    }

    GeneCoexpressionNodeDegree( Gene g ) {
        this.setGeneId( g.getId() );

        this.setLinkCountsNegative( new byte[] {} );
        this.setLinkCountsPositive( new byte[] {} );
        this.setRelativeLinkRanksNegative( new byte[] {} );
        this.setRelativeLinkRanksPositive( new byte[] {} );

    }

    @Override
    public int hashCode() {
        return Objects.hash( geneId );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( !( obj instanceof GeneCoexpressionNodeDegree ) )
            return false;
        GeneCoexpressionNodeDegree other = ( GeneCoexpressionNodeDegree ) obj;
        return Objects.equals( geneId, other.geneId );
    }

    public Long getGeneId() {
        return geneId;
    }

    public void setGeneId( Long geneId ) {
        this.geneId = geneId;
    }

    @Override
    public Long getId() {
        return geneId;
    }

    public void setId( Long id ) {
        this.geneId = id;
    }

    public byte[] getLinkCountsNegative() {
        return linkCountsNegative;
    }

    public void setLinkCountsNegative( byte[] linkCountsNegative ) {
        this.linkCountsNegative = linkCountsNegative;
    }

    public byte[] getLinkCountsPositive() {
        return linkCountsPositive;
    }

    public void setLinkCountsPositive( byte[] linkCountsPositive ) {
        this.linkCountsPositive = linkCountsPositive;
    }

    /**
     * @return Note that these values are for support thresholds, not support levels - so "at or above" the given
     *         threshold
     *         support.
     */
    public byte[] getRelativeLinkRanksNegative() {
        return relativeLinkRanksNegative;
    }

    public void setRelativeLinkRanksNegative( byte[] relativeLinkRanksNegative ) {
        this.relativeLinkRanksNegative = relativeLinkRanksNegative;
    }

    /**
     * @return Note that these values are for support thresholds, not support levels - so "at or above" the given
     *         threshold
     *         support.
     */
    public byte[] getRelativeLinkRanksPositive() {
        return relativeLinkRanksPositive;
    }

    public void setRelativeLinkRanksPositive( byte[] relativeLinkRanksPositive ) {
        this.relativeLinkRanksPositive = relativeLinkRanksPositive;
    }

    public static final class Factory {
        public static GeneCoexpressionNodeDegree newInstance( Gene g ) {
            return new GeneCoexpressionNodeDegree( g );
        }
    }

}