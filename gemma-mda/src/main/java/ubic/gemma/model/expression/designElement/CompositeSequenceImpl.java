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

import ubic.gemma.model.common.Describable;

/**
 * @see ubic.gemma.model.expression.designElement.CompositeSequence
 * @author pavlidis
 * @version $Id$
 */
public class CompositeSequenceImpl extends ubic.gemma.model.expression.designElement.CompositeSequence {

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        Describable other = ( Describable ) obj;
        if ( getId() == null ) {
            if ( other.getId() != null ) return false;
        } else if ( !getId().equals( other.getId() ) ) return false;
        if ( getName() == null ) {
            if ( other.getName() != null ) return false;
        } else if ( !getName().equals( other.getName() ) ) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( getId() == null ) ? 0 : getId().hashCode() );
        result = prime * result + ( ( getName() == null ) ? 0 : getName().hashCode() );
        return result;
    }

    /**
     * 
     */
    private static final long serialVersionUID = 2144914030275919838L;

}