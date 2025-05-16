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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents the result of a BLAT search. The column names follow the convention of Kent et al.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
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