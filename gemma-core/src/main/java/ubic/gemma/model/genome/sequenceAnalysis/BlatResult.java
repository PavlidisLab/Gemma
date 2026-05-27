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

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents the result of a BLAT search. The column names follow the convention of Kent et al.
 * <p>
 * Equality is inherited from {@link SequenceSimilaritySearchResult}, which uses an id-aware,
 * proxy-safe identifier hash via {@code IdentifiableUtils}. We deliberately do NOT override
 * equals/hashCode here: the previous {@code @EqualsAndHashCode(callSuper = true)} pulled in all
 * 17 subclass fields plus lazy associations from the superclass, which is unsafe inside a Set
 * or Map (would trigger lazy-init / N+1 on hash) and produced unstable hashes when nullable
 * primitives flipped.
 */
@Getter
@Setter
@Entity
@DiscriminatorValue("BlatResultImpl")
public class BlatResult extends SequenceSimilaritySearchResult {

    @Column(name = "BLOCK_COUNT", columnDefinition = "INTEGER")
    private Integer blockCount;
    @Lob
    @Column(name = "BLOCK_SIZES", columnDefinition = "text")
    private String blockSizes;
    @Column(name = "MATCHES", columnDefinition = "INTEGER")
    private Integer matches;
    @Column(name = "MISMATCHES", columnDefinition = "INTEGER")
    private Integer mismatches;
    @Column(name = "NS", columnDefinition = "INTEGER")
    private Integer ns;
    @Column(name = "QUERY_END", columnDefinition = "INTEGER")
    private Integer queryEnd;
    @Column(name = "QUERY_GAP_BASES", columnDefinition = "INTEGER")
    private Integer queryGapBases;
    @Column(name = "QUERY_GAP_COUNT", columnDefinition = "INTEGER")
    private Integer queryGapCount;
    @Column(name = "QUERY_START", columnDefinition = "INTEGER")
    private Integer queryStart;
    @Lob
    @Column(name = "QUERY_STARTS", columnDefinition = "text")
    private String queryStarts;
    @Column(name = "REP_MATCHES", columnDefinition = "INTEGER")
    private Integer repMatches;
    @Column(name = "STRAND", columnDefinition = "VARCHAR(255)")
    private String strand;
    @Column(name = "TARGET_END", columnDefinition = "BIGINT")
    private Long targetEnd;
    @Column(name = "TARGET_GAP_BASES", columnDefinition = "INTEGER")
    private Integer targetGapBases;
    @Column(name = "TARGET_GAP_COUNT", columnDefinition = "INTEGER")
    private Integer targetGapCount;
    @Column(name = "TARGET_START", columnDefinition = "BIGINT")
    private Long targetStart;
    @Lob
    @Column(name = "TARGET_STARTS", columnDefinition = "text")
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