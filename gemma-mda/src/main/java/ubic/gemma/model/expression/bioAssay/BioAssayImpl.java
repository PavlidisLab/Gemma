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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.expression.bioAssay;

/**
 * @see ubic.gemma.model.expression.bioAssay.BioAssay
 */
public class BioAssayImpl extends ubic.gemma.model.expression.bioAssay.BioAssay {

    @Override
    public boolean equals( Object object ) {

        if ( !( object instanceof BioAssay ) ) {
            return false;
        }
        final BioAssay that = ( BioAssay ) object;
        if ( this.getId() != null && that.getId() != null ) return this.getId().equals( that.getId() );

        if ( this.getName() != null && that.getName() != null && !this.getName().equals( that.getName() ) )
            return false;

        if ( this.getDescription() != null && that.getDescription() != null
                && !this.getDescription().equals( that.getDescription() ) ) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;

        if ( this.getId() != null ) {
            return 29 * getId().hashCode();
        } else {
            int nameHash = this.getName() == null ? 0 : getName().hashCode();

            int descHash = this.getDescription() == null ? 0 : getDescription().hashCode();
            hashCode = 29 * nameHash + descHash;
        }
        return hashCode;
    }

    /**
     * 
     */
    private static final long serialVersionUID = -984217953142208083L;
}