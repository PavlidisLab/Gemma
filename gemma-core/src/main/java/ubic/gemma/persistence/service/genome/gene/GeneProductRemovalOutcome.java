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
package ubic.gemma.persistence.service.genome.gene;

import org.springframework.lang.Nullable;

import java.util.List;

/**
 * What {@link GeneWriteService#replayGeneProductRemoval} did with one recorded removal, or would have done in a dry
 * run.
 *
 * @param skipReason                    why nothing was deleted, or null if the removal was applied
 * @param detail                        a readable account of the skip reason or of what was deleted
 * @param productDeleted                whether the gene product itself was deleted
 * @param blatAssociationsDeleted       number of BLAT associations deleted
 * @param annotationAssociationsDeleted number of annotation associations deleted
 * @param platforms                     sorted short names of every platform with an element whose sequence had one of
 *                                      the deleted associations
 */
public record GeneProductRemovalOutcome(
        @Nullable SkipReason skipReason,
        String detail,
        boolean productDeleted,
        long blatAssociationsDeleted,
        long annotationAssociationsDeleted,
        List<String> platforms ) {

    /**
     * Why a recorded removal no longer holds. Nothing is deleted for such a removal.
     */
    public enum SkipReason {
        PRODUCT_NOT_FOUND,
        DUMMY_PRODUCT,
        GENE_CHANGED,
        GI_CHANGED
    }

    public GeneProductRemovalOutcome {
        platforms = List.copyOf( platforms );
    }

    static GeneProductRemovalOutcome skipped( SkipReason reason, String detail ) {
        return new GeneProductRemovalOutcome( reason, detail, false, 0, 0, List.of() );
    }

    public boolean isSkipped() {
        return skipReason != null;
    }
}
