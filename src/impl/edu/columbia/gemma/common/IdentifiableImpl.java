/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.common;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @see edu.columbia.gemma.common.Identifiable
 * @author pavlidis
 * @version $Id$
 */
public class IdentifiableImpl extends edu.columbia.gemma.common.Identifiable implements Comparable {

    /**
     * This method is here to make Andromda put the implementation class in the right place
     * 
     * @see edu.columbia.gemma.common.Identifiable#dummy()
     */
    public void dummy() {
        ; // nothing.
    }

    /**
     * @see java.lang.Object#equals(Object)
     */
    public boolean equals( Object object ) {
        if ( !( object instanceof Identifiable ) ) {
            return false;
        }
        Identifiable rhs = ( Identifiable ) object;
        return rhs.getIdentifier().equals( this.getIdentifier() );
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return new HashCodeBuilder( -1867551415, -168940413 ).append( this.getIdentifier().hashCode() ).toHashCode();
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return new ToStringBuilder( this ).append( "name", this.getName() ).append( "identifier", this.getIdentifier() )
                .toString();
    }

    /**
     * @see java.lang.Comparable#compareTo(Object)
     */
    public int compareTo( Object object ) {
        if ( !( object instanceof Identifiable ) ) return -1;
        Identifiable myClass = ( Identifiable ) object;
        return myClass.getIdentifier().compareTo( this.getIdentifier() );
    }

}