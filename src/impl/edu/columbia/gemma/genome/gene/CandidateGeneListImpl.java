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

package edu.columbia.gemma.genome.gene;

import java.util.ArrayList;
import java.util.Iterator;

import edu.columbia.gemma.genome.Gene;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004, 2005 Columbia University
 * 
 * @author daq2101
 * @version $Id$
 */
public class CandidateGeneListImpl extends edu.columbia.gemma.genome.gene.CandidateGeneList {
    /**
     * Adds a candidate to the list, intitializing the list as an ArrayList if none exists. Lists are ranked in
     * ascending order starting at 0.
     * 
     * @param gene the Gene to create as a CandidateGene. <b>Note:</b> The ranking has no inherent meaning. The user
     *        should not have to figure out the new ranking explicitly. This is why you pass in a Gene instead of having
     *        to create a CandidateGene and set a ranking on it.
     * @return the CandidateGene created
     * @see edu.columbia.gemma.genome.gene.CandidateGeneList#addCandidate(edu.columbia.gemma.genome.Gene)
     */
    @SuppressWarnings("unchecked")
    public CandidateGene addCandidate( Gene gene ) {

        if ( gene == null ) throw new IllegalArgumentException( "Parameter gene cannot be null" );
        assert this.getCandidates() != null;

        java.util.Collection<CandidateGene> candidates = this.getCandidates();
        CandidateGene cg = null;
        int maxRank = -1;

        for ( Iterator iter = candidates.iterator(); iter.hasNext(); ) {
            cg = ( CandidateGene ) iter.next();
            if ( cg.getRank().intValue() > maxRank ) maxRank = cg.getRank().intValue();
        }
        // new candidate gene comes at end of list
        maxRank = maxRank + 1;

        // create new candidate gene and set rank accordingly
        CandidateGene cgNew = CandidateGene.Factory.newInstance();
        cgNew.setGene( gene );
        cgNew.setRank( new Integer( maxRank ) );

        if ( this.getCandidates() == null ) this.setCandidates( new ArrayList() );
        this.getCandidates().add( cgNew );

        return cgNew;
    }

    /**
     * Moves the passed candidate down one space on the CandidateList
     * 
     * @param candidateGene The gene to move
     * @see edu.columbia.gemma.genome.gene.CandidateGeneList#decreaseRanking(edu.columbia.gemma.genome.gene.CandidateGene)
     */
    public void decreaseRanking( edu.columbia.gemma.genome.gene.CandidateGene candidateGene ) {
        if ( candidateGene == null ) throw new IllegalArgumentException( "Parameter candidate cannot be null" );
        if ( !this.getCandidates().contains( candidateGene ) )
            throw new IllegalArgumentException( "This candidate not found on this list." );

        CandidateGene cg = null;
        CandidateGene cgR = null;

        int nextHighest = Integer.MAX_VALUE; // initialize to MAX so it will be set immediately
        for ( java.util.Iterator iter = this.getCandidates().iterator(); iter.hasNext(); ) {
            cg = ( CandidateGene ) iter.next();
            if ( cg.getRank().intValue() > candidateGene.getRank().intValue() && cg.getRank().intValue() < nextHighest ) {
                cgR = cg;
            }
        }
        if ( cgR != null ) {
            Integer tmp = cgR.getRank();
            cgR.setRank( candidateGene.getRank() );
            candidateGene.setRank( tmp );
        }
    }

    /**
     * Moves the passed candidate up one space on the CandidateList
     * 
     * @param candidate The gene to move
     * @see edu.columbia.gemma.genome.gene.CandidateGeneList#increaseRanking(CandidateGene)
     */
    public void increaseRanking( CandidateGene candidateGene ) {
        if ( candidateGene == null ) throw new IllegalArgumentException( "Parameter candidate cannot be null" );
        if ( !this.getCandidates().contains( candidateGene ) )
            throw new IllegalArgumentException( "This candidate not found on this list." );

        CandidateGene cg = null;
        CandidateGene cgR = null;

        int nextLowest = -1; // initialize to -1 so it will be set immediately
        for ( java.util.Iterator iter = this.getCandidates().iterator(); iter.hasNext(); ) {
            cg = ( CandidateGene ) iter.next();
            if ( cg.getRank().intValue() < candidateGene.getRank().intValue() && cg.getRank().intValue() > nextLowest ) {
                cgR = cg;
            }
        }
        if ( cgR != null ) {
            Integer tmp = cgR.getRank();
            cgR.setRank( candidateGene.getRank() );
            candidateGene.setRank( tmp );
        }
    }

    /**
     * Adds removes the passed candidate from the list.
     * 
     * @param candidateGene The gene to remove
     * @see edu.columbia.gemma.genome.gene.CandidateGeneList#removeCandidate(CandidateGene)
     */
    public void removeCandidate( CandidateGene candidateGene ) {
        if ( candidateGene == null ) throw new IllegalArgumentException( "Parameter candidateGene cannot be null" );
        if ( this.getCandidates() == null ) throw new IllegalArgumentException( "Cannot remove from an empty list." );
        if ( !this.getCandidates().contains( candidateGene ) )
            throw new IllegalArgumentException( "This candidate not found on this list." );
        this.getCandidates().remove( candidateGene );
    }

}