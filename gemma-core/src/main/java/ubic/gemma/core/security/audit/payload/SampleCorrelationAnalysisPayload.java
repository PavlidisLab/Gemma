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
package ubic.gemma.core.security.audit.payload;

import org.springframework.lang.Nullable;
import ubic.gemma.core.security.audit.AuditEventPayload;

import java.util.Collections;
import java.util.List;

/**
 * Structured payload for {@code SampleCorrelationAnalysisEvent} writes: the
 * row-and-column attrition of the expression matrix as each filter was
 * applied, plus the filter configuration that produced it.
 *
 * <h2>Why this event carries it</h2>
 *
 * <p>Filtering is not a step of preprocessing and is not stored on the
 * experiment. {@code ExpressionExperimentFilterConfig} is built fresh at each
 * callsite and {@code ExpressionExperimentFilterResult} was, until this
 * payload, computed and discarded. The sample-correlation leg is where the
 * filter runs during every preprocessing run, so recording the result here
 * costs no extra work and no extra audit event.
 *
 * <h2>🛑 These numbers describe the sample-correlation filter, not the download
 * filter</h2>
 *
 * <p>The "Data (filtered)" download builds its own config with different
 * settings, so its attrition can differ — most visibly through
 * {@code requireSequences}, which drops rows. That is why {@code config} is
 * serialised alongside {@code stages}: the numbers are only interpretable
 * against the settings that produced them, and a reader who has only the
 * counts will assume they describe every filtered view of the dataset.
 *
 * <p>Written only from the run that computes the matrix. Datasets whose
 * correlation matrix predates this payload carry {@code null}, which means
 * "not recorded", never "nothing was filtered".
 *
 * <h2>🛑 {@code dataSource} is how two matrices that disagree explain themselves</h2>
 *
 * <p>There are two ways this matrix gets its data, and they are not interchangeable.
 * {@code unmasked-rebuild} reprocesses the raw vectors from scratch so a flagged outlier's values are present
 * — quantile normalization included, against whatever set of rows that rebuild covered.
 * {@code stored-vectors} filters the processed data as stored, which was normalized at some earlier time
 * against whatever rows existed then. The numbers differ between the two, immaterially for a sample-sample
 * correlation and not at all interpretably if you do not know which you are holding.</p>
 *
 * <p>Nothing on {@code SAMPLE_COEXPRESSION_MATRIX} records this, and it cannot be recovered from the matrix
 * afterwards: it is a fact about how the bytes were made, knowable only at write time. So it is recorded here,
 * beside {@code startingRows}, which gives the width the rebuild covered. Matrices written before this field
 * existed carry {@code null}, meaning "not recorded", never "stored-vectors".</p>
 *
 * @see ubic.gemma.core.analysis.preprocess.filter.ExpressionExperimentFilterResult
 */
public record SampleCorrelationAnalysisPayload(
        @Nullable FilterConfig config,
        List<FilterStage> stages,
        int startingRows,
        int startingColumns,
        int finalRows,
        int finalColumns,
        @Nullable String dataSource
) implements AuditEventPayload {

    public SampleCorrelationAnalysisPayload {
        stages = stages != null ? Collections.unmodifiableList( stages ) : Collections.emptyList();
    }

    /**
     * The settings the filter ran under. Mirrors the fields of
     * {@code ExpressionExperimentFilterConfig} that change which rows or
     * columns survive; the threshold-bypass flags are included because they
     * are the reason a stage reports {@code applied=false}.
     */
    public record FilterConfig(
            boolean requireSequences,
            boolean maskOutliers,
            boolean ignoreMinimumSamplesThreshold,
            boolean ignoreMinimumDesignElementsThreshold,
            double lowExpressionCut,
            double highExpressionCut,
            double lowVarianceCut,
            double lowDistinctValueCut,
            double minPresentFraction,
            int minPresentCount,
            int maxDesignElements
    ) {
    }

    /**
     * One rung of the attrition funnel, in the order the filters ran.
     *
     * @param filter      one of {@code noSequences}, {@code affyControls}, {@code outliers}, {@code minPresent},
     *                    {@code zeroVariance}, {@code lowExpression}, {@code lowVariance}
     * @param applied     false when the filter was skipped — configuration turned it off, or the platform did not
     *                    call for it (Affymetrix control probes on a non-Affymetrix platform). A skipped stage
     *                    still reports {@code rowsAfter} so the funnel reads continuously.
     * @param rowsAfter   design elements remaining after this stage
     * @param columnsAfter samples remaining, for the one stage that can drop samples ({@code outliers}); null elsewhere
     */
    public record FilterStage(
            String filter,
            boolean applied,
            int rowsAfter,
            @Nullable Integer columnsAfter
    ) {
    }
}
