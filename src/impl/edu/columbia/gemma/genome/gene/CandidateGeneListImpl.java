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
package edu.columbia.gemma.genome.gene;

import java.util.Collection;


/**
 * @see edu.columbia.gemma.genome.gene.CandidateGeneList
 */
public class CandidateGeneListImpl
    extends edu.columbia.gemma.genome.gene.CandidateGeneList
{
    public void addCandidate(CandidateGene candidateGene){
        // add new candidate to end of list.
        // In one transaction, we want to calculate the next highest 
        // ranking and add the candidate with that ranking.
        // Maybe DAO just needs an "Add at end" function.
        throw new java.lang.UnsupportedOperationException("edu.columbia.gemma.genome.gene.CandidateGeneList.addCandidate(CandidateGene candidate) Not implemented!");
    }
    public void removeCandidate(CandidateGene candidateGene){
        throw new java.lang.UnsupportedOperationException("edu.columbia.gemma.genome.gene.CandidateGeneList.removeCandidate(CandidateGene candidate) Not implemented!");
    }
    public Collection getCandidates(){
        throw new java.lang.UnsupportedOperationException("edu.columbia.gemma.genome.gene.CandidateGeneList.increaseRanking(CandidateGene candidate) Not implemented!");
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