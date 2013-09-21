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
     * Constructs new instances of {@link ubic.gemma.model.genome.sequenceAnalysis.BlatResult}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.genome.sequenceAnalysis.BlatResult}.
         */
        public static ubic.gemma.model.genome.sequenceAnalysis.BlatResult newInstance() {
            return new ubic.gemma.model.genome.sequenceAnalysis.BlatResultImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 3110768245871858012L;
    private Integer matches;

    private Integer mismatches;

    private Integer repMatches;

    private Integer ns;

    private Integer queryGapBases;

    private Integer queryGapCount;

    private Integer targetGapBases;

    private Integer targetGapCount;

    private String strand;

    private Integer queryStart;

    private Integer queryEnd;

    private Long targetStart;

    private Long targetEnd;

    private Integer blockCount;

    private String blockSizes;

    private String queryStarts;

    private String targetStarts;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public BlatResult() {
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