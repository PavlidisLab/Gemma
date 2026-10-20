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
package ubic.gemma.core.analysis.singleCell.aggregate;

import ubic.gemma.core.security.audit.payload.SingleCellSubSetsCreatedPayload;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Co-bean facade for the {@code SingleCellSubSetsCreatedEvent} audit emission
 * carried out at the end of
 * {@link SingleCellExpressionExperimentSubSetServiceImpl#createSubSets}. The
 * proxy hop is required so the {@code AuditedAspect} can intercept the
 * {@code @Audited}-annotated method on the implementation bean. Phase C
 * bucket 2f — see {@code AUDIT_PHASE_C_RECCE.md} §4d.
 */
public interface SingleCellExpressionExperimentSubSetAuditService {

    /**
     * Emit a {@code SingleCellSubSetsCreatedEvent} for {@code ee} carrying the
     * typed {@link SingleCellSubSetsCreatedPayload}. The note string is
     * supplied by the caller via {@code note} and used verbatim through the
     * aspect's {@code messageSpel}.
     */
    void recordSubSetsCreated( ExpressionExperiment ee, String note, SingleCellSubSetsCreatedPayload payload );
}
