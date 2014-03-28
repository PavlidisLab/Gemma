/*
 * The gemma-model project
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
 * @version $Id$
 */
public abstract class SupportDetails extends IdArray {

    // auto-generated. Gene2Gene links can navigate here.
    private Long id;

    // these are only used for bookkeeeping, not persistent.
    private Long firstGeneId = null;
    private Long secondGeneId = null;
    private Boolean isPositive = null;

    /**
     * Note that the gene information and isPositive is only used for bookkeeping during creation; it is not part of the
     * persistent entity.
     * 
     * @param firstGene
     * @param secondGene
     * @param isPositive
     */
    public SupportDetails( Gene firstGene, Gene secondGene, Boolean isPositive ) {
        if ( firstGene != null ) this.firstGeneId = firstGene.getId();
        if ( secondGene != null ) this.secondGeneId = secondGene.getId();
        if ( isPositive != null ) this.isPositive = isPositive;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( getClass() != obj.getClass() ) return false;
        SupportDetails other = ( SupportDetails ) obj;

        if ( this.id != null ) return this.id.equals( other.getId() );

        if ( this.firstGeneId != null && !this.firstGeneId.equals( other.firstGeneId ) ) return false;
        if ( this.secondGeneId != null && !this.secondGeneId.equals( other.secondGeneId ) ) return false;
        if ( this.isPositive != null && !this.isPositive.equals( other.isPositive ) ) return false;

        return true;

    }

    public Long getId() {
        return id;
    }

    @Override
    public int hashCode() {
        if ( id != null ) return id.hashCode();
        int hashCode = 0;
        hashCode = 29 * hashCode + ( firstGeneId == null ? 0 : firstGeneId.hashCode() );
        hashCode = 29 * hashCode + ( secondGeneId == null ? 0 : secondGeneId.hashCode() );
        hashCode = 29 * hashCode + ( isPositive == null ? 0 : isPositive.hashCode() );

        return hashCode;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "SupportDetails [id=" + id + ", g1=" + firstGeneId + ", g2=" + secondGeneId + ", ispos=" + isPositive
                + ", data=" + data + "]";
    }

}
