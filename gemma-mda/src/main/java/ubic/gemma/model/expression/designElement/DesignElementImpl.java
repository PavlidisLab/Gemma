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
package ubic.gemma.model.expression.designElement;

/**
 * @see ubic.gemma.model.expression.designElement.DesignElement
 * @author pavlidis
 * @version $Id$
 */
public abstract class DesignElementImpl extends ubic.gemma.model.expression.designElement.DesignElement {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -3705432443805719171L;

    @Override
    public boolean equals( Object object ) {

        if ( !( object instanceof DesignElement ) ) {
            return false;
        }
        final DesignElement that = ( DesignElement ) object;
        if ( this.getId() != null && that.getId() != null ) return this.getId().equals( that.getId() );

        if ( this.getName() != null && that.getName() != null && !this.getName().equals( that.getName() ) )
            return false;

        if ( this.getDescription() != null && that.getDescription() != null
                && !this.getDescription().equals( that.getDescription() ) ) return false;

        if ( this.getArrayDesign() != null && that.getArrayDesign() != null
                && !this.getArrayDesign().equals( that.getArrayDesign() ) ) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;

        if ( this.getId() != null ) {
            return 29 * getId().hashCode();
        }
        int nameHash = this.getName() == null ? 0 : getName().hashCode();

        int descHash = this.getDescription() == null ? 0 : getDescription().hashCode();
        int adHash = this.getArrayDesign() == null ? 0 : getArrayDesign().hashCode();
        hashCode = 29 * nameHash + descHash + adHash;

        return hashCode;
    }

}