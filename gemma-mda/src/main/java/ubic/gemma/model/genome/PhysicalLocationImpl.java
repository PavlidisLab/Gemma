/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package ubic.gemma.model.genome;

import org.apache.commons.lang.builder.CompareToBuilder;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.genome.PhysicalLocation
 */
public class PhysicalLocationImpl extends ubic.gemma.model.genome.PhysicalLocation implements Comparable {

    /**
     * 
     */
    private static final long serialVersionUID = -6580769809003779541L;

    /**
     * @see java.lang.Comparable#compareTo(Object)
     */
    @Override
    public int compareTo( Object object ) {
        PhysicalLocationImpl other = ( PhysicalLocationImpl ) object;
        return new CompareToBuilder().append( this.getChromosome().getName(), other.getChromosome().getName() ).append(
                this.getNucleotide(), other.getNucleotide() ).toComparison();
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof PhysicalLocation ) ) {
            return false;
        }
        final PhysicalLocation that = ( PhysicalLocation ) object;

        if ( this.getId() == null || that.getId() == null || !this.getId().equals( that.getId() ) ) {
            return this.getChromosome().equals( that.getChromosome() )
                    && this.getNucleotide().equals( that.getNucleotide() );
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.PhysicalLocation#nearlyEquals(java.lang.Object)
     */
    @Override
    public boolean nearlyEquals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof PhysicalLocation ) ) {
            return false;
        }
        final PhysicalLocation that = ( PhysicalLocation ) object;

        if ( this.getId() == null || that.getId() == null || !this.getId().equals( that.getId() ) ) {
            if ( !this.getChromosome().equals( that.getChromosome() ) ) return false;

            // FIXME this needs to check for overlaps etc...
            if ( Math.abs( this.getNucleotide() - that.getNucleotide() ) < 1000L ) return true;

            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29
                * hashCode
                + ( this.getId() == null ? this.getChromosome().hashCode() + this.getNucleotide().hashCode() : this
                        .getId().hashCode() );

        return hashCode;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        if ( this.getId() != null ) {
            buf.append( " Id = " + this.getId() );
        }
        buf.append( this.getChromosome().getTaxon().getScientificName() + " chromosome "
                + this.getChromosome().getName() + ":" + this.getNucleotide() + " on " + this.getStrand() + " strand" );

        return buf.toString();
    }

}