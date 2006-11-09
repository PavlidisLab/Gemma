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

import ubic.gemma.model.common.Securable;

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
        if ( this.getId() == null || that.getId() == null || !this.getId().equals( that.getId() ) ) {
            if ( this.getName() == null || that.getName() == null ) {
                return false;
            }
            return this.getName().equals( that.getName() );
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        int nameHash = this.getName() == null ? 0 : getName().hashCode();
        hashCode = 29 * hashCode + ( getId() == null ? nameHash : getId().hashCode() );
        return hashCode;
    }
}