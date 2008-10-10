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
 * @see ubic.gemma.model.genome.GeneticLocation
 */
public class GeneticLocationImpl extends ubic.gemma.model.genome.GeneticLocation {

    /**
     * 
     */
    private static final long serialVersionUID = 5806006409735224836L;

    /**
     * @see java.lang.Comparable#compareTo(Object)
     */
    @Override
    public int compareTo( Object object ) {
        GeneticLocationImpl other = ( GeneticLocationImpl ) object;
        return new CompareToBuilder().append( this.getChromosome().getName(), other.getChromosome().getName() ).append(
                this.getCentimorgans(), other.getCentimorgans() ).toComparison();
    }
}