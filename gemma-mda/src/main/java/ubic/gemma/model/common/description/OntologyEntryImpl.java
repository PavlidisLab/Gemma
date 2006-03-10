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
package ubic.gemma.model.common.description;

/**
 * @see ubic.gemma.model.common.description.OntologyEntry
 */
public class OntologyEntryImpl
    extends ubic.gemma.model.common.description.OntologyEntry
{
    /**
     * @see ubic.gemma.model.common.description.OntologyEntry#getParents()
     */
    public java.util.Collection getParents()
    {
        //@todo implement public java.util.Collection getParents()
        return null;
    }

    /**
     * @see ubic.gemma.model.common.description.OntologyEntry#getChildren()
     */
    public java.util.Collection getChildren()
    {
        //@todo implement public java.util.Collection getChildren()
        return null;
    }
    
    public String toString() {
        return "Id: " + this.getId() + " Category:" + this.getCategory() + " Value:" + this.getValue();
    }

}