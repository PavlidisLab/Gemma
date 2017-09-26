/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.model.genome.sequenceAnalysis;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents the result of a BLAT search. The column names follow the convention of Kent et al.
 */
public class BlatResult extends SequenceSimilaritySearchResult {

    private static final long serialVersionUID = 5703130745858235525L;
    private static final Log log = LogFactory.getLog( BlatResult.class.getName() );
    private Integer blockCount;
    private String blockSizes;
    private Integer matches;
    private Integer mismatches;
    private Integer ns;
    private Integer queryEnd;
    private Integer queryGapBases;
    private Integer queryGapCount;
    private Integer queryStart;
    private String queryStarts;
    private Integer repMatches;
    private String strand;
    private Long targetEnd;
    private Integer targetGapBases;
    private Integer targetGapCount;
    private Long targetStart;
    private String targetStarts;

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ( this.getId() == null ? " " : " Id:" + this.getId() + " " ) + "query="
                + this.getQuerySequence().getName() + " " + "target=" + this.getTargetChromosome().getName() + ":"
                + this.getTargetStart() + "-" + this.getTargetEnd();
    }

    /**
     * Fraction identity computation, as in psl.c. Modified to INCLUDE repeat matches in the match count.
     * See <a href="http://genome.ucsc.edu/FAQ/FAQblat#blat4">Blat4 at UCSC</a>.
     *
     * @return Value between 0 and 1.
     */
    public Double identity() {
        int sizeMul = 1; // assuming DNA; use 3 for protein.
        long qAliSize = sizeMul * this.getQueryEnd() - this.getQueryStart();
        long tAliSize = this.getTargetEnd() - this.getTargetStart();
        long aliSize = Math.min( qAliSize, tAliSize );

        if ( aliSize <= 0 )
            return 0.0;

        long sizeDif = qAliSize - tAliSize;
        if ( sizeDif < 0 ) {
            sizeDif = 0; // here assuming "isMRna" is true. ("The parameter isMRna should be set to TRUE, regardless
            // of whether the input sequence is mRNA or protein")
        }
        int insertFactor = this.getQueryGapCount(); // assumes isMRna is true.
        int total = ( sizeMul * ( this.getMatches() + this.getRepMatches() + this.getMismatches() ) );
        int milliBad = 0;
        if ( total != 0 ) {
            milliBad = ( 1000 * ( this.getMismatches() * sizeMul + insertFactor + ( int ) Math
                    .round( 3.0 * Math.log( 1.0 + sizeDif ) ) ) ) / total;
        }
        assert milliBad >= 0 && milliBad <= 1000 :
                "MilliBad was outside of range 0-1000: " + milliBad + " for result " + this;
        return ( 100.0 - milliBad * 0.1 ) / 100.0;
    }

    /**
     * Based on the JKSrc method in psl.c, but without double-penalizing for mismatches. We also consider repeat matches
     * to be the same as regular matches.
     *
     * @return Value between 0 and 1, representing the fraction of matches, minus a gap penalty.
     */
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
                throw new IllegalArgumentException(
                        "Sequence is missing; cannot compute score for " + this.getQuerySequence() );
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
            log.warn( "Blat result for " + this.getQuerySequence() + " More matches than sequence length: " + this
                    .getMatches() + " match + " + this.getRepMatches() + " repMatch = " + matches + " > " + length );
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
            throw new IllegalStateException(
                    "Score was " + score + "; matches=" + matches + " repMatches=" + this.getRepMatches()
                            + " queryGaps=" + this.getQueryGapCount() + " targetGaps=" + this.getTargetGapCount()
                            + " length=" + length + " sequence=" + this.getQuerySequence() + " id=" + this.getId() );
        }

        assert score >= 0.0 && score <= 1.0 :
                "Score was " + score + "; matches=" + matches + " queryGaps=" + this.getQueryGapCount() + " targetGaps="
                        + this.getTargetGapCount() + " length=" + length + " sequence=" + this.getQuerySequence()
                        + " id=" + this.getId();

        return score;
    }

    public Integer getBlockCount() {
        return this.blockCount;
    }

    public void setBlockCount( Integer blockCount ) {
        this.blockCount = blockCount;
    }

    public String getBlockSizes() {
        return this.blockSizes;
    }

    public void setBlockSizes( String blockSizes ) {
        this.blockSizes = blockSizes;
    }

    public Integer getMatches() {
        return this.matches;
    }

    public void setMatches( Integer matches ) {
        this.matches = matches;
    }

    public Integer getMismatches() {
        return this.mismatches;
    }

    public void setMismatches( Integer mismatches ) {
        this.mismatches = mismatches;
    }

    public Integer getNs() {
        return this.ns;
    }

    public void setNs( Integer ns ) {
        this.ns = ns;
    }

    public Integer getQueryEnd() {
        return this.queryEnd;
    }

    public void setQueryEnd( Integer queryEnd ) {
        this.queryEnd = queryEnd;
    }

    public Integer getQueryGapBases() {
        return this.queryGapBases;
    }

    public void setQueryGapBases( Integer queryGapBases ) {
        this.queryGapBases = queryGapBases;
    }

    public Integer getQueryGapCount() {
        return this.queryGapCount;
    }

    public void setQueryGapCount( Integer queryGapCount ) {
        this.queryGapCount = queryGapCount;
    }

    public Integer getQueryStart() {
        return this.queryStart;
    }

    public void setQueryStart( Integer queryStart ) {
        this.queryStart = queryStart;
    }

    public String getQueryStarts() {
        return this.queryStarts;
    }

    public void setQueryStarts( String queryStarts ) {
        this.queryStarts = queryStarts;
    }

    public Integer getRepMatches() {
        return this.repMatches;
    }

    public void setRepMatches( Integer repMatches ) {
        this.repMatches = repMatches;
    }

    public String getStrand() {
        return this.strand;
    }

    public void setStrand( String strand ) {
        this.strand = strand;
    }

    public Long getTargetEnd() {
        return this.targetEnd;
    }

    public void setTargetEnd( Long targetEnd ) {
        this.targetEnd = targetEnd;
    }

    public Integer getTargetGapBases() {
        return this.targetGapBases;
    }

    public void setTargetGapBases( Integer targetGapBases ) {
        this.targetGapBases = targetGapBases;
    }

    public Integer getTargetGapCount() {
        return this.targetGapCount;
    }

    public void setTargetGapCount( Integer targetGapCount ) {
        this.targetGapCount = targetGapCount;
    }

    public Long getTargetStart() {
        return this.targetStart;
    }

    public void setTargetStart( Long targetStart ) {
        this.targetStart = targetStart;
    }

    public String getTargetStarts() {
        return this.targetStarts;
    }

    public void setTargetStarts( String targetStarts ) {
        this.targetStarts = targetStarts;
    }

    public static final class Factory {
        public static BlatResult newInstance() {
            return new BlatResult();
        }
    }

}