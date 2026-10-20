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
package ubic.gemma.persistence.service.expression.bioAssayData;

import ubic.gemma.core.security.audit.payload.ProcessedVectorComputationPayload;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Co-bean facade for the {@code ProcessedVectorComputationEvent} success-path
 * audit emission carried out by
 * {@link ProcessedExpressionDataVectorServiceImpl#createProcessedDataVectors(ExpressionExperiment, boolean, boolean)}.
 * <p>
 * The work itself stays in {@code createProcessedDataVectors}; this bean exists
 * only so the typed payload write goes through a Spring proxy (so the
 * {@code AuditedAspect} can fire on its {@code @Audited} method). Phase C
 * bucket 2f — see {@code AUDIT_PHASE_C_RECCE.md} §4d.
 */
public interface ProcessedExpressionDataVectorAuditService {

    /**
     * Emit a {@code ProcessedVectorComputationEvent} for {@code ee} with the
     * typed {@link ProcessedVectorComputationPayload}. The note string is
     * built by the aspect's {@code messageSpel} so it can reference the
     * {@code ee} parameter at runtime.
     */
    void recordProcessedVectorComputation( ExpressionExperiment ee, ProcessedVectorComputationPayload payload );
}
