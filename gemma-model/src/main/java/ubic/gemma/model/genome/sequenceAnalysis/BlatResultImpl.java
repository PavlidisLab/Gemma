/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.genome.sequenceAnalysis;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.sequence.sequenceAnalysis.BlatResult
 */
public class BlatResultImpl extends BlatResult {

    /**
     * 
     */
    private static final long serialVersionUID = -8157023595754885730L;

    private static Log log = LogFactory.getLog( BlatResultImpl.class.getName() );

    /**
     * Fraction identity computation, as in psl.c. Modified to INCLUDE repeat matches in the match count.
     * 
     * @return Value between 0 and 1.
     * @see http://genome.ucsc.edu/FAQ/FAQblat#blat4.
     */
    @Override
    public Double identity() {
        int sizeMul = 1; // assuming DNA; use 3 for protein.
        long qAliSize = sizeMul * this.getQueryEnd() - this.getQueryStart();
        long tAliSize = this.getTargetEnd() - this.getTargetStart();
        long aliSize = Math.min( qAliSize, tAliSize );

        if ( aliSize <= 0 ) return 0.0;

        long sizeDif = qAliSize - tAliSize;
        if ( sizeDif < 0 ) {
            sizeDif = 0; // here assuming "isMrna" is true. ("The parameter isMrna should be set to TRUE, regardless
            // of whether the input sequence is mRNA or protein")
        }
        int insertFactor = this.getQueryGapCount(); // assumes isMrna is true.
        int total = ( sizeMul * ( this.getMatches() + this.getRepMatches() + this.getMismatches() ) );
        int milliBad = 0;
        if ( total != 0 ) {
            milliBad = ( 1000 * ( this.getMismatches() * sizeMul + insertFactor + ( int ) Math.round( 3.0 * Math
                    .log( 1.0 + sizeDif ) ) ) ) / total;
        }
        assert milliBad >= 0 && milliBad <= 1000 : "Millibad was ourside of range 0-1000: " + milliBad + " for result "
                + this;
        return 100.0 - milliBad * 0.1;
    }

    /**
     * Based on the JKSrc method in psl.c, but without double-penalizing for mismatches. We also consider repeat matches
     * to be the same as regular matches.
     * 
     * @return Value between 0 and 1, representing the fraction of matches, minus a gap penalty.
     * @see ubic.gemma.model.sequence.sequenceAnalysis.BlatResult#score()
     */
    @Override
    public Double score() {

        long length;
        if ( this.getQuerySequence() == null ) {
            throw new IllegalArgumentException( "Sequence cannot be null" );
        }

        if ( this.getQuerySequence().getLength() != null && this.getQuerySequence().getLength() != 0 ) {
            length = this.getQuerySequence().getLength();
        } else {
            if ( StringUtils.isNotBlank( this.getQuerySequence().getSequence() ) ) {
                length = this.getQuerySequence().getSequence().length();
            } else {
                throw new IllegalArgumentException( "Sequence is missing; cannot compute score for "
                        + this.getQuerySequence() );
            }
        }

        assert length > 0;

        // Note: we count repeat matches just like regular matches.
        long matches = this.getMatches() + this.getRepMatches();

        /*
         * This might happen if the sequence in our system was polyA/T trimmed, which we don't do any more, but there
         * could be remnants. When blat results come back from goldenpath (rather than computed by us) the lengths can
         * disagree. Other reasons for this unclear.
         */
        if ( matches > length ) {
            log.warn( "Blat result for " + this.getQuerySequence() + " More matches than sequence length: "
                    + this.getMatches() + " match + " + this.getRepMatches() + " repmatch = " + matches + " > "
                    + length );
            matches = length;
        }

        /*
         * return sizeMul (psl->match + ( psl->repMatch>>1)) - sizeMul psl->misMatch - psl->qNumInsert -
         * psl->tNumInsert; Note that: "Currently the program does not distinguish between matches and repMatches.
         * repMatches is always zero." (http://genome.ucsc.edu/goldenPath/help/blatSpec.html)
         */
        double score = ( double ) ( matches - this.getQueryGapCount() - this.getTargetGapCount() ) / ( double ) length;

        // because of repeat matches, score _can_ be negative in some situations (typically, lots of gaps).
        if ( score < 0.0 && this.getRepMatches() == 0 ) {
            throw new IllegalStateException( "Score was " + score + "; matches=" + matches + " repMatches="
                    + this.getRepMatches() + " queryGaps=" + this.getQueryGapCount() + " targetGaps="
                    + this.getTargetGapCount() + " length=" + length + " sequence=" + this.getQuerySequence() + " id="
                    + this.getId() );
        }

        assert score >= 0.0 && score <= 1.0 : "Score was " + score + "; matches=" + matches + " queryGaps="
                + this.getQueryGapCount() + " targetGaps=" + this.getTargetGapCount() + " length=" + length
                + " sequence=" + this.getQuerySequence() + " id=" + this.getId();

        return score;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append( this.getClass().getSimpleName() );
        buf.append( this.getId() == null ? " " : " Id:" + this.getId() + " " );
        buf.append( "query=" + this.getQuerySequence().getName() + " " );
        buf.append( "target=" + this.getTargetChromosome().getName() + ":" + this.getTargetStart() + "-"
                + this.getTargetEnd() );
        return buf.toString();
    }
}