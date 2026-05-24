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
package ubic.gemma.cli.audit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.security.audit.Audited;
import ubic.gemma.core.security.audit.AuditedOnError;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedSampleCorrelationAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.MakePrivateEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.MakePublicEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;

/**
 * Default {@link CliExpressionExperimentAuditService} implementation. The
 * {@code recordMade*} methods are pure annotation-driven audit emitters
 * (empty body); {@code computeSampleCorrelation} actually runs the work
 * because the audit emission shape is {@link AuditedOnError}, which only
 * fires when the wrapped invocation throws.
 *
 * <p>The two distinct {@code @AuditedOnError} declarations match the legacy
 * shape verbatim: {@link FilteringException} produced a typed audit row in
 * the CLI's first catch, every other {@link Exception} produced one in the
 * second. The aspect's most-specific-instanceof dispatch chooses the
 * filtering-exception variant when it applies; otherwise the default
 * {@link Throwable} declaration acts as the catch-all.
 */
@Service
public class CliExpressionExperimentAuditServiceImpl implements CliExpressionExperimentAuditService {

    @Autowired
    private SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService;

    @Override
    @Audited(value = MakePrivateEvent.class, messageSpel = "#note")
    public void recordMadePrivate( ExpressionExperiment ee, String note ) {
    }

    @Override
    @Audited(value = MakePublicEvent.class, messageSpel = "#note")
    public void recordMadePublic( ExpressionExperiment ee, String note ) {
    }

    @Override
    @AuditedOnError(value = FailedSampleCorrelationAnalysisEvent.class,
            exception = FilteringException.class)
    @AuditedOnError(value = FailedSampleCorrelationAnalysisEvent.class)
    public void computeSampleCorrelation( ExpressionExperiment ee ) throws FilteringException {
        sampleCoexpressionAnalysisService.compute( ee,
                sampleCoexpressionAnalysisService.prepare( ee ) );
    }
}
