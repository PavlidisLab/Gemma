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
*
*
* <hr>
* <p>Copyright (c) 2004, 2005 Columbia University
* @author daq2101
* @version $Id$
*/
package edu.columbia.gemma.genome.gene;

import java.util.Collection;
import java.util.HashSet;

 
public class CandidateGeneListImpl
    extends edu.columbia.gemma.genome.gene.CandidateGeneList
{
    public void addCandidate(CandidateGene candidateGene){
        if(this.getCandidates()==null)
            this.setCandidates(new HashSet());
        this.getCandidates().add(candidateGene);
        
    }
    public void removeCandidate(CandidateGene candidateGene){
        Collection c = this.getCandidates();
        if(c != null && c.contains(candidateGene))
            c.remove(candidateGene);
    }

    /**
     * @see edu.columbia.gemma.genome.gene.CandidateGeneList#increaseRanking(CandidateGene)
     */
    public void increaseRanking(CandidateGene candidate)
    {
        //@todo implement public void increaseRanking(CandidateGene candidate)
        throw new java.lang.UnsupportedOperationException("edu.columbia.gemma.genome.gene.CandidateGeneList.increaseRanking(CandidateGene candidate) Not implemented!");
    }

    /**
     * @see edu.columbia.gemma.genome.gene.CandidateGeneList#decreaseRanking(edu.columbia.gemma.genome.gene.CandidateGene)
     */
    public void decreaseRanking(edu.columbia.gemma.genome.gene.CandidateGene candidateGene)
    {
        //@todo implement public void decreaseRanking(edu.columbia.gemma.genome.gene.CandidateGene candidateGene)
        throw new java.lang.UnsupportedOperationException("edu.columbia.gemma.genome.gene.CandidateGeneList.decreaseRanking(edu.columbia.gemma.genome.gene.CandidateGene candidateGene) Not implemented!");
    }

}