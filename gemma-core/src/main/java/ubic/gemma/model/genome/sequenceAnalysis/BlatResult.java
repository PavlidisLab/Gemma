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
public class BlatResult extends SequenceSimilaritySearchResult {

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

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ( this.getId() == null ? " " : " Id:" + this.getId() + " " ) + "query="
                + this.getQuerySequence().getName() + " " + "target=" + this.getTargetChromosome().getName() + ":"
                + this.getTargetStart() + "-" + this.getTargetEnd();
    }

    public static final class Factory {
        public static BlatResult newInstance() {
            return new BlatResult();
        }
    }

}