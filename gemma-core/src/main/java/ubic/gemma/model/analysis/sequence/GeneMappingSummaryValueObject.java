/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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
package ubic.gemma.model.analysis.sequence;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ubic.gemma.model.genome.gene.GeneReferenceValueObject;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultValueObject;

import java.io.Serializable;
import java.util.List;

/**
 * One probe-to-gene mapping, as it goes over the wire: a single alignment plus the genes it
 * supports.
 * <p>
 * The wire counterpart of {@link GeneMappingSummary}, which cannot be serialized as-is. That class
 * predates the REST API and was shaped for a DWR/javascript client: it carries the same genes three
 * times over ({@code geneProductMap} keyed by object, plus {@code geneProductIdMap} and
 * {@code geneProductIdGeneMap}, two string-keyed mirrors that exist because "javascript clients
 * cannot marshal maps unless the keys are strings"), and it holds a {@code compositeSequence}
 * back-reference to the very value object that would contain it. Serializing it would emit each
 * gene three times, key one of those maps by {@code Object.toString()}, and nest the parent inside
 * its own child.
 * <p>
 * Alignment scores and the biological-sequence metadata both live on {@link #blatResult} — its
 * {@code identity} / {@code score} and its {@code querySequence} respectively — rather than being
 * repeated at this level as {@link GeneMappingSummary} does.
 *
 * @author paul
 * @see GeneMappingSummary
 */
@Getter
@Setter
@NoArgsConstructor
public class GeneMappingSummaryValueObject implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The alignment this mapping rests on, carrying the scores ({@code identity}, {@code score}),
     * the genomic coordinates, and the probe's biological sequence under {@code querySequence}.
     * <p>
     * Null only for mappings that come from an annotation association rather than a real alignment.
     */
    @Nullable
    private BlatResultValueObject blatResult;

    /**
     * Genes this alignment supports, deduplicated — a gene reached through several of its gene
     * products appears once. Empty for an alignment that maps to no gene, which is a real and
     * reportable outcome rather than a missing value.
     */
    private List<GeneReferenceValueObject> genes;

    public GeneMappingSummaryValueObject( @Nullable BlatResultValueObject blatResult, List<GeneReferenceValueObject> genes ) {
        this.blatResult = blatResult;
        this.genes = genes;
    }
}
