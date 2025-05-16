/*
 * The gemma project
 *
 * Copyright (c) 2013 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.analysis.expression.coexpression;

import ubic.gemma.model.genome.Gene;

/**
 * Represents the datasets in which a link was found in ("supported").
 *
 * @author Paul
 */
public abstract class SupportDetails extends IdArray {

    // auto-generated. Gene2Gene links can navigate here.
    private Long id;

    // these are only used for book keeeping, not persistent.
    private Long firstGeneId = null;
    private Long secondGeneId = null;
    private Boolean isPositive = null;

    /*
     * Note that making the firstGene persistent will not help much in queries because the support details is used by
     * two links in the LINK tables - the genea-geneb and geneb-genea versions. Thus we fetch SupportDetails using a
     * join (seems slower) or by a direct query for the support details ids. We would have to store both gene ids, and
     * store the supportDetails in 'both directions', in which case we might as well store it in the LINK table in the
     * first place. In terms of performance it is not clear what would be best (and it's a huge effort to test this
     * properly). See CoexpressionDaoImpl for details -- PP
     */

    /**
     * Note that the gene information and isPositive is only used for bookkeeping during creation; it is not part of the
     * persistent entity.
     *
     * @param firstGene  first gene
     * @param isPositive value of isPositive
     * @param secondGene second gene
     */
    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public SupportDetails( Gene firstGene, Gene secondGene, Boolean isPositive ) {
        if ( firstGene != null )
            this.firstGeneId = firstGene.getId();
        if ( secondGene != null )
            this.secondGeneId = secondGene.getId();
        if ( isPositive != null )
            this.isPositive = isPositive;
    }

    /**
     * Note that the gene information and isPositive is only used for bookkeeping during creation; it is not part of the
     * persistent entity. Used by LinkCreator.
     *
     * @param firstGene  first gene
     * @param isPositive value of isPositive
     * @param secondGene second gene
     */
    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public SupportDetails( Long firstGene, Long secondGene, Boolean isPositive ) {
        if ( firstGene != null )
            this.firstGeneId = firstGene;
        if ( secondGene != null )
            this.secondGeneId = secondGene;
        if ( isPositive != null )
            this.isPositive = isPositive;
    }

    public Long getId() {
        return id;
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public void setId( Long id ) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        if ( id != null )
            return id.hashCode();
        int hashCode = 0;
        hashCode = 29 * hashCode + ( firstGeneId == null ? 0 : firstGeneId.hashCode() );
        hashCode = 29 * hashCode + ( secondGeneId == null ? 0 : secondGeneId.hashCode() );
        hashCode = 29 * hashCode + ( isPositive == null ? 0 : isPositive.hashCode() );

        return hashCode;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( this.getClass() != obj.getClass() )
            return false;
        SupportDetails other = ( SupportDetails ) obj;

        if ( this.id != null )
            return this.id.equals( other.getId() );

        if ( this.firstGeneId != null && !this.firstGeneId.equals( other.firstGeneId ) )
            return false;
        //noinspection SimplifiableIfStatement // Better readability
        if ( this.secondGeneId != null && !this.secondGeneId.equals( other.secondGeneId ) )
            return false;
        return this.isPositive == null || this.isPositive.equals( other.isPositive );

    }

    @Override
    public String toString() {
        return "SupportDetails [id=" + id + ", g1=" + firstGeneId + ", g2=" + secondGeneId + ", ispos=" + isPositive
                + ", data=" + data + "]";
    }

}
