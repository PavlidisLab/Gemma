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
package edu.columbia.gemma.genome.sequenceAnalysis;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.sequence.sequenceAnalysis.BlatResult
 */
public class BlatResultImpl extends edu.columbia.gemma.genome.sequenceAnalysis.BlatResult {

    /**
     * Based on the JKSrc method in psl.c. However, we do not double-penalize for mismatches (they are not subtracted
     * from the matches). The gap penalties are implemented as in psl.c.
     * 
     * @return Value between 0 and 1, representing the fraction of matches, minus a gap penalty.
     * @see edu.columbia.gemma.sequence.sequenceAnalysis.BlatResult#score()
     */
    @Override
    public Double score() {
        assert this.getQuerySequence() != null;
        return ( ( double ) this.getMatches() - ( double ) this.getQueryGapCount() - this.getTargetGapCount() )
                / this.getQuerySequence().getLength();
    }

    /**
     * Fraction identity computation, as in psl.c.
     * 
     * @return Value between 0 and 1.
     * @see http:// genome.ucsc.edu/FAQ/FAQblat#blat5
     */
    @Override
    public Double identity() {
        int sizeMul = 1; // assuming DNA; use 3 for protein.
        long qAliSize = sizeMul * this.getQueryEnd() - this.getQueryStart();
        long tAliSize = this.getTargetEnd() - this.getTargetStart();
        long aliSize = Math.min( qAliSize, tAliSize );

        if ( aliSize <= 0 ) return 0.0;

        long sizeDif = qAliSize = tAliSize;
        if ( sizeDif < 0 ) {
            sizeDif = -sizeDif; // here assuming "isMrna" is false;
        }
        int insertFactor = this.getQueryGapCount();
        int milliBad = ( 1000 * ( this.getMismatches() * sizeMul + insertFactor + ( int ) Math.round( 3 * Math
                .log( 1.0 + sizeDif ) ) ) )
                / ( sizeMul * ( this.getMatches() + this.getRepMatches() + this.getMismatches() ) );
        assert milliBad >= 0 && milliBad <= 1000;
        return 100.0 - milliBad * 0.1;
    }

}