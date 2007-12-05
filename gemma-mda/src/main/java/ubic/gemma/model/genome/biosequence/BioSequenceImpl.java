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
package ubic.gemma.model.genome.biosequence;

/**
 * @see ubic.gemma.model.genome.biosequence.BioSequence
 * @author pavlidis
 * @version $Id$
 */
public class BioSequenceImpl extends ubic.gemma.model.genome.biosequence.BioSequence {

    /**
     * 
     */
    private static final long serialVersionUID = -6620431603579954167L;

    @Override
    public boolean equals( Object object ) {

        if ( !( object instanceof BioSequence ) ) {
            return false;
        }
        final BioSequence that = ( BioSequence ) object;
        if ( this.getId() != null && that.getId() != null ) return this.getId().equals( that.getId() );

        // The way this is constructed, ALL of the items must be the same.
        if ( this.getSequenceDatabaseEntry() != null && that.getSequenceDatabaseEntry() != null
                && !this.getSequenceDatabaseEntry().equals( that.getSequenceDatabaseEntry() ) ) return false;

        if ( this.getTaxon() != null && that.getTaxon() != null && !this.getTaxon().equals( that.getTaxon() ) )
            return false;

        if ( this.getName() != null && that.getName() != null && !this.getName().equals( that.getName() ) )
            return false;

        if ( this.getSequence() != null && that.getSequence() != null
                && !this.getSequence().equals( that.getSequence() ) ) return false;

        if ( this.getLength() != null && that.getLength() != null && !this.getLength().equals( that.getLength() ) )
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;

        if ( this.getId() != null ) {
            return 29 * getId().hashCode();
        } else {
            int nameHash = this.getName() == null ? 0 : getName().hashCode();
            int taxonHash = this.getTaxon() == null ? 0 : getTaxon().hashCode();
            int lengthHash = this.getLength() == null ? 0 : getLength().hashCode();
            int dbHash = this.getSequenceDatabaseEntry() == null ? 0 : getSequenceDatabaseEntry().hashCode();
            int seqHash = 0;
            if ( dbHash == 0 && nameHash == 0 && lengthHash == 0 && this.getSequence() != null )
                seqHash = this.getSequence().hashCode();
            hashCode = 29 * nameHash + seqHash + dbHash + taxonHash + lengthHash;
        }
        return hashCode;
    }
}