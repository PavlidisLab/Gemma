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

import java.util.ArrayList;
import java.util.Collection;

import java.util.Iterator;

import edu.columbia.gemma.genome.Gene;

public class CandidateGeneListImpl
    extends edu.columbia.gemma.genome.gene.CandidateGeneList
{
    public CandidateGene addCandidate(Gene gene){
        
        if ( gene == null ) throw new IllegalArgumentException( "Parameter gene cannot be null" );
        assert this.getCandidates() != null;
        
        Collection candidates = this.getCandidates();
        
        // figure out the highest rank in this candidate list 
        // note that if the list is empty the first item has rank of 0
        int maxRank=-1;
        if(candidates!=null){
            Iterator iter = candidates.iterator();
            while(iter.hasNext()){
                CandidateGene cg = (CandidateGene) iter.next();
                if(cg.getRank().intValue()>maxRank)
                    maxRank=cg.getRank().intValue();
            }
        }
    
        // new candidate gene comes at end of list
        maxRank = maxRank+1;
        
        // create new candidate gene and set rank accordingly
        CandidateGene cgNew = CandidateGene.Factory.newInstance();
        cgNew.setGene(gene);
        cgNew.setRank(new Integer(maxRank));
        
        if(this.getCandidates()==null)
            this.setCandidates(new ArrayList());
        this.getCandidates().add(cgNew);
        
        return cgNew;
        
    }
    
    public void removeCandidate(CandidateGene candidateGene){
        if ( candidateGene == null ) throw new IllegalArgumentException( "Parameter candidateGene cannot be null" );
        assert this.getCandidates() != null;
        
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
        if ( candidate == null ) throw new IllegalArgumentException( "Parameter candidate cannot be null" );
        assert this.getCandidates() != null;
        throw new java.lang.UnsupportedOperationException("edu.columbia.gemma.genome.gene.CandidateGeneList.increaseRanking(CandidateGene candidate) Not implemented!");
    }

    /**
     * @see edu.columbia.gemma.genome.gene.CandidateGeneList#decreaseRanking(edu.columbia.gemma.genome.gene.CandidateGene)
     */
    public void decreaseRanking(edu.columbia.gemma.genome.gene.CandidateGene candidateGene)
    {
        //@todo implement public void decreaseRanking(edu.columbia.gemma.genome.gene.CandidateGene candidateGene)
        if ( candidateGene == null ) throw new IllegalArgumentException( "Parameter candidateGene cannot be null" );
        assert this.getCandidates() != null;
        throw new java.lang.UnsupportedOperationException("edu.columbia.gemma.genome.gene.CandidateGeneList.decreaseRanking(edu.columbia.gemma.genome.gene.CandidateGene candidateGene) Not implemented!");
    }

}