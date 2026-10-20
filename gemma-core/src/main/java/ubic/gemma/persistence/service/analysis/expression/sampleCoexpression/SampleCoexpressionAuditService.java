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
package ubic.gemma.persistence.service.analysis.expression.sampleCoexpression;

import org.springframework.lang.Nullable;
import ubic.gemma.core.security.audit.payload.SampleCorrelationAnalysisPayload;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Co-bean facade for the {@code SampleCorrelationAnalysisEvent} success-path
 * audit emission carried out by
 * {@link SampleCoexpressionAnalysisServiceImpl#compute(ExpressionExperiment, PreparedCoexMatrices)}.
 * <p>
 * The event used to be written by an {@code @Audited} annotation on
 * {@code compute} itself, which the aspect could satisfy from that method's
 * own arguments. The payload is produced inside {@code prepare}, not passed
 * in, so it cannot reach the aspect that way; routing the write through this
 * proxy is the same shape as
 * {@code ProcessedExpressionDataVectorAuditService}.
 */
public interface SampleCoexpressionAuditService {

    /**
     * Emit a {@code SampleCorrelationAnalysisEvent} for {@code ee}, carrying
     * the filter attrition recorded while the matrix was built.
     *
     * @param payload null when the matrix was loaded without the filter having
     *                run, in which case the event is written with no payload
     */
    void recordSampleCorrelationAnalysis( ExpressionExperiment ee, @Nullable SampleCorrelationAnalysisPayload payload );
}
