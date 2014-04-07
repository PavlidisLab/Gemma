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

/**
 * Represents the result of a BLAT search. The column names follow the convention of Kent et al.
 */
public abstract class BlatResult extends SequenceSimilaritySearchResult {

    /**
     * Constructs new instances of {@link BlatResult}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link BlatResult}.
         */
        public static BlatResult newInstance() {
            return new BlatResultImpl();
        }

    }

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
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( !super.equals( obj ) ) return false;
        if ( getClass() != obj.getClass() ) return false;
        BlatResult other = ( BlatResult ) obj;
        if ( blockCount == null ) {
            if ( other.blockCount != null ) return false;
        } else if ( !blockCount.equals( other.blockCount ) ) return false;
        if ( blockSizes == null ) {
            if ( other.blockSizes != null ) return false;
        } else if ( !blockSizes.equals( other.blockSizes ) ) return false;
        if ( matches == null ) {
            if ( other.matches != null ) return false;
        } else if ( !matches.equals( other.matches ) ) return false;
        if ( mismatches == null ) {
            if ( other.mismatches != null ) return false;
        } else if ( !mismatches.equals( other.mismatches ) ) return false;
        if ( ns == null ) {
            if ( other.ns != null ) return false;
        } else if ( !ns.equals( other.ns ) ) return false;
        if ( queryEnd == null ) {
            if ( other.queryEnd != null ) return false;
        } else if ( !queryEnd.equals( other.queryEnd ) ) return false;
        if ( queryGapBases == null ) {
            if ( other.queryGapBases != null ) return false;
        } else if ( !queryGapBases.equals( other.queryGapBases ) ) return false;
        if ( queryGapCount == null ) {
            if ( other.queryGapCount != null ) return false;
        } else if ( !queryGapCount.equals( other.queryGapCount ) ) return false;
        if ( queryStart == null ) {
            if ( other.queryStart != null ) return false;
        } else if ( !queryStart.equals( other.queryStart ) ) return false;
        if ( queryStarts == null ) {
            if ( other.queryStarts != null ) return false;
        } else if ( !queryStarts.equals( other.queryStarts ) ) return false;
        if ( repMatches == null ) {
            if ( other.repMatches != null ) return false;
        } else if ( !repMatches.equals( other.repMatches ) ) return false;
        if ( strand == null ) {
            if ( other.strand != null ) return false;
        } else if ( !strand.equals( other.strand ) ) return false;
        if ( targetEnd == null ) {
            if ( other.targetEnd != null ) return false;
        } else if ( !targetEnd.equals( other.targetEnd ) ) return false;
        if ( targetGapBases == null ) {
            if ( other.targetGapBases != null ) return false;
        } else if ( !targetGapBases.equals( other.targetGapBases ) ) return false;
        if ( targetGapCount == null ) {
            if ( other.targetGapCount != null ) return false;
        } else if ( !targetGapCount.equals( other.targetGapCount ) ) return false;
        if ( targetStart == null ) {
            if ( other.targetStart != null ) return false;
        } else if ( !targetStart.equals( other.targetStart ) ) return false;
        if ( targetStarts == null ) {
            if ( other.targetStarts != null ) return false;
        } else if ( !targetStarts.equals( other.targetStarts ) ) return false;
        return true;
    }

    /**
     * 
     */
    public Integer getBlockCount() {
        return this.blockCount;
    }

    /**
     * 
     */
    public String getBlockSizes() {
        return this.blockSizes;
    }

    /**
     * 
     */
    public Integer getMatches() {
        return this.matches;
    }

    /**
     * 
     */
    public Integer getMismatches() {
        return this.mismatches;
    }

    /**
     * 
     */
    public Integer getNs() {
        return this.ns;
    }

    /**
     * 
     */
    public Integer getQueryEnd() {
        return this.queryEnd;
    }

    /**
     * 
     */
    public Integer getQueryGapBases() {
        return this.queryGapBases;
    }

    /**
     * 
     */
    public Integer getQueryGapCount() {
        return this.queryGapCount;
    }

    /**
     * 
     */
    public Integer getQueryStart() {
        return this.queryStart;
    }

    /**
     * 
     */
    public String getQueryStarts() {
        return this.queryStarts;
    }

    /**
     * 
     */
    public Integer getRepMatches() {
        return this.repMatches;
    }

    /**
     * 
     */
    public String getStrand() {
        return this.strand;
    }

    /**
     * 
     */
    public Long getTargetEnd() {
        return this.targetEnd;
    }

    /**
     * 
     */
    public Integer getTargetGapBases() {
        return this.targetGapBases;
    }

    /**
     * 
     */
    public Integer getTargetGapCount() {
        return this.targetGapCount;
    }

    /**
     * 
     */
    public Long getTargetStart() {
        return this.targetStart;
    }

    /**
     * 
     */
    public String getTargetStarts() {
        return this.targetStarts;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( blockCount == null ) ? 0 : blockCount.hashCode() );
        result = prime * result + ( ( blockSizes == null ) ? 0 : blockSizes.hashCode() );
        result = prime * result + ( ( matches == null ) ? 0 : matches.hashCode() );
        result = prime * result + ( ( mismatches == null ) ? 0 : mismatches.hashCode() );
        result = prime * result + ( ( ns == null ) ? 0 : ns.hashCode() );
        result = prime * result + ( ( queryEnd == null ) ? 0 : queryEnd.hashCode() );
        result = prime * result + ( ( queryGapBases == null ) ? 0 : queryGapBases.hashCode() );
        result = prime * result + ( ( queryGapCount == null ) ? 0 : queryGapCount.hashCode() );
        result = prime * result + ( ( queryStart == null ) ? 0 : queryStart.hashCode() );
        result = prime * result + ( ( queryStarts == null ) ? 0 : queryStarts.hashCode() );
        result = prime * result + ( ( repMatches == null ) ? 0 : repMatches.hashCode() );
        result = prime * result + ( ( strand == null ) ? 0 : strand.hashCode() );
        result = prime * result + ( ( targetEnd == null ) ? 0 : targetEnd.hashCode() );
        result = prime * result + ( ( targetGapBases == null ) ? 0 : targetGapBases.hashCode() );
        result = prime * result + ( ( targetGapCount == null ) ? 0 : targetGapCount.hashCode() );
        result = prime * result + ( ( targetStart == null ) ? 0 : targetStart.hashCode() );
        result = prime * result + ( ( targetStarts == null ) ? 0 : targetStarts.hashCode() );
        return result;
    }

    /**
     * 
     */
    public abstract Double identity();

    /**
     * 
     */
    public abstract Double score();

    public void setBlockCount( Integer blockCount ) {
        this.blockCount = blockCount;
    }

    public void setBlockSizes( String blockSizes ) {
        this.blockSizes = blockSizes;
    }

    public void setMatches( Integer matches ) {
        this.matches = matches;
    }

    public void setMismatches( Integer mismatches ) {
        this.mismatches = mismatches;
    }

    public void setNs( Integer ns ) {
        this.ns = ns;
    }

    public void setQueryEnd( Integer queryEnd ) {
        this.queryEnd = queryEnd;
    }

    public void setQueryGapBases( Integer queryGapBases ) {
        this.queryGapBases = queryGapBases;
    }

    public void setQueryGapCount( Integer queryGapCount ) {
        this.queryGapCount = queryGapCount;
    }

    public void setQueryStart( Integer queryStart ) {
        this.queryStart = queryStart;
    }

    public void setQueryStarts( String queryStarts ) {
        this.queryStarts = queryStarts;
    }

    public void setRepMatches( Integer repMatches ) {
        this.repMatches = repMatches;
    }

    public void setStrand( String strand ) {
        this.strand = strand;
    }

    public void setTargetEnd( Long targetEnd ) {
        this.targetEnd = targetEnd;
    }

    public void setTargetGapBases( Integer targetGapBases ) {
        this.targetGapBases = targetGapBases;
    }

    public void setTargetGapCount( Integer targetGapCount ) {
        this.targetGapCount = targetGapCount;
    }

    public void setTargetStart( Long targetStart ) {
        this.targetStart = targetStart;
    }

    public void setTargetStarts( String targetStarts ) {
        this.targetStarts = targetStarts;
    }

}