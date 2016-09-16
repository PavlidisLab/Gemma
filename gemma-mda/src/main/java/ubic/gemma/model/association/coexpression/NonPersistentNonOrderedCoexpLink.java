/*
 * The gemma-mda project
 * 
 * Copyright (c) 2014 University of British Columbia
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

import ubic.gemma.model.genome.Gene;

/**
 * Wrapper object used to track and eliminate duplicates. Crucial: for the purposes of "equals" and "hashcode", ignores
 * the ID of the link, and the gene order. Sign is used. Sort order is by ID of the first gene only.
 * 
 * @author Paul
 * @version $Id$
 */
public class NonPersistentNonOrderedCoexpLink implements Comparable<NonPersistentNonOrderedCoexpLink> {

    // used internally only; g1 is the one with the lower ID.
    final private Long g1;
    final private Long g2;

    final private Gene2GeneCoexpression link;

    final private boolean positive;

    /**
     * @param g1
     * @param g2
     * @param b
     */
    public NonPersistentNonOrderedCoexpLink( Gene g1, Gene g2, boolean b ) {
        if ( g1.getId() < g2.getId() ) {
            this.g1 = g1.getId();
            this.g2 = g2.getId();
        } else {
            this.g1 = g2.getId();
            this.g2 = g1.getId();
        }
        this.positive = b;
        this.link = null;
    }

    public NonPersistentNonOrderedCoexpLink( Long g1, Long g2, boolean b ) {
        if ( g1 < g2 ) {
            this.g1 = g1;
            this.g2 = g2;
        } else {
            this.g1 = g2;
            this.g2 = g1;
        }
        this.positive = b;
        this.link = null;
    }

    /**
     * @param link
     */
    public NonPersistentNonOrderedCoexpLink( Gene2GeneCoexpression link ) {
        this.link = link;
        if ( link.getFirstGene() < link.getSecondGene() ) {
            this.g1 = link.getFirstGene();
            this.g2 = link.getSecondGene();
        } else {
            this.g1 = link.getSecondGene();
            this.g2 = link.getFirstGene();
        }
        this.positive = link.isPositiveCorrelation();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo( NonPersistentNonOrderedCoexpLink o ) {
        return getFirstGene().compareTo( o.getFirstGene() );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        NonPersistentNonOrderedCoexpLink other = ( NonPersistentNonOrderedCoexpLink ) obj;
        if ( g1 == null ) {
            if ( other.g1 != null ) return false;
        } else if ( !g1.equals( other.g1 ) ) return false;
        if ( g2 == null ) {
            if ( other.g2 != null ) return false;
        } else if ( !g2.equals( other.g2 ) ) return false;
        if ( positive != other.positive ) return false;

        return true;
    }

    /**
     * The first gene for the underlying link; this is always the lower ID.
     * 
     * @return
     */
    public Long getFirstGene() {
        // if ( link == null ) throw new IllegalStateException();
        // return link.getFirstGene();
        return this.g1;
    }

    /**
     * The underlying link (may be null depending on how this was constructed)
     * 
     * @return
     */
    public Gene2GeneCoexpression getLink() {
        // if ( link == null ) throw new IllegalStateException();
        return link;
    }

    /**
     * The second gene for the underlying link; this is always the higher ID - not necesssarily the secondGene
     * 
     * @return
     */
    public Long getSecondGene() {
        // if ( link == null ) throw new IllegalStateException();
        // return link.getSecondGene();
        return this.g2;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( g1 == null ) ? 0 : g1.hashCode() );
        result = prime * result + ( ( g2 == null ) ? 0 : g2.hashCode() );
        result = prime * result + ( positive ? 1231 : 1237 );
        return result;
    }

    /**
     * @return
     */
    public boolean isPositiveCorrelation() {
        return positive;
    }

    @Override
    public String toString() {
        return "NPNOCL [g1=" + g1 + ", g2=" + g2 + ", pos=" + positive + "]";
    }

}
