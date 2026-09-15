/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.persistence.service.expression.experiment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.security.audit.Audited;
import ubic.gemma.model.common.auditAndSecurity.eventType.GeeqEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Implementation of {@link GeeqAuditService}: a thin co-bean that exists so
 * {@link #recordGeeqScoring(ExpressionExperiment, String, String)} is invoked
 * through a Spring proxy and the {@link Audited} aspect can intercept its
 * return path and emit the {@link GeeqEvent}.
 * <p>
 * Previously this emission lived in a {@code private createGeeqEvent(...)}
 * member of {@code GeeqServiceImpl} self-invoked via {@code this.} from
 * {@code calculateScore()} -- Spring AOP cannot intercept either case, so the
 * imperative {@code auditTrailService.addUpdateEvent(ee, GeeqEvent.class, note, details)}
 * 4-arg call could not be migrated to {@link Audited} during bucket 2g.
 *
 * <p>The legacy emission split the human-readable summary across NOTE (short
 * "Geeq scoring (mode: X)") and DETAIL ("Issues noted: ..."). The {@link Audited}
 * aspect routes through {@code addUpdateEventWithPayload}, which writes the
 * note to {@code AUDIT_EVENT.NOTE} and uses {@code AUDIT_EVENT.PAYLOAD} for
 * typed {@code AuditEventPayload}-marked arguments (none here). The two
 * strings are concatenated into a single NOTE column entry to preserve the
 * diagnostic text -- no downstream consumer of {@code GeeqEvent} reads the
 * old DETAIL column (only the timestamp is surfaced, via
 * {@code GeeqValueObject.lastRun}).
 *
 * <p>The {@link Transactional} default propagation ({@code REQUIRED}) joins
 * the outer transaction opened by {@code GeeqServiceImpl.calculateScore},
 * matching the pre-hoist behaviour.
 */
@Service
public class GeeqAuditServiceImpl implements GeeqAuditService {

    private static final Log log = LogFactory.getLog( GeeqAuditServiceImpl.class );

    /**
     * {@inheritDoc}
     *
     * <p>The {@link Audited} annotation drives emission through the
     * {@code AuditedAspect}; this method body is intentionally a logging-only
     * marker so the proxy-intercepted return triggers exactly one
     * {@link GeeqEvent}. The {@code messageSpel} concatenates note + detail
     * into the {@code AUDIT_EVENT.NOTE} column.
     */
    @Override
    @Transactional
    @Audited(value = GeeqEvent.class, messageSpel = "#note + '. ' + #detail")
    public void recordGeeqScoring( ExpressionExperiment ee, String note, String detail ) {
        log.debug( "Geeq scoring audit for " + ee + ": " + note );
    }
}
