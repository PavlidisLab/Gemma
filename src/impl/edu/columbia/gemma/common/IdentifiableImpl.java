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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package edu.columbia.gemma.common;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @see edu.columbia.gemma.common.Identifiable
 */
public class IdentifiableImpl extends edu.columbia.gemma.common.Identifiable {

    /**
     * @see edu.columbia.gemma.common.Identifiable#compareTo(java.lang.Object)
     */
    public int compareTo( java.lang.Object object ) {
        if ( !( object instanceof Identifiable ) ) return -1;
        Identifiable myClass = ( Identifiable ) object;
        return myClass.getIdentifier().compareTo( this.getIdentifier() );

    }

    /**
     * @see edu.columbia.gemma.common.Identifiable#equals(java.lang.Object)
     */
    public boolean equals( java.lang.Object object ) {
        if ( !( object instanceof Identifiable ) ) {
            return false;
        }
        Identifiable rhs = ( Identifiable ) object;
        return rhs.getIdentifier().equals( this.getIdentifier() );

    }

    /* (non-Javadoc)
     * @see edu.columbia.gemma.common.Identifiable#findByIdentifier(java.lang.String)
     */
    public Identifiable findByIdentifier( String identifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see edu.columbia.gemma.common.Identifiable#hashCode()
     */
    public int hashCode() {

        return new HashCodeBuilder( -1867551415, -168940413 ).append( this.getIdentifier().hashCode() ).toHashCode();
    }
    /**
     * @see edu.columbia.gemma.common.Identifiable#toString()
     */
    public java.lang.String toString() {
        return new ToStringBuilder( this ).append( "name", this.getName() ).append( "identifier", this.getIdentifier() )
                .toString();

    }

}