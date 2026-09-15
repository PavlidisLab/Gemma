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

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.security.audit.Audited;
import ubic.gemma.core.security.audit.payload.SampleCorrelationAnalysisPayload;
import ubic.gemma.model.common.auditAndSecurity.eventType.SampleCorrelationAnalysisEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Implementation of {@link SampleCoexpressionAuditService}: a thin co-bean
 * whose only purpose is to expose an {@link Audited}-annotated method through
 * a Spring proxy, so {@code AuditedAspect} fires, picks up the
 * {@link SampleCorrelationAnalysisPayload} argument, serialises it to JSON and
 * writes it to {@code AUDIT_EVENT.PAYLOAD}.
 */
@Service
@Slf4j
public class SampleCoexpressionAuditServiceImpl implements SampleCoexpressionAuditService {

    @Override
    @Transactional
    @Audited(value = SampleCorrelationAnalysisEvent.class, message = "Sample correlation has been computed.")
    public void recordSampleCorrelationAnalysis( ExpressionExperiment ee, @Nullable SampleCorrelationAnalysisPayload payload ) {
        log.debug( "Recording SampleCorrelationAnalysisEvent for {}", ee );
    }
}
