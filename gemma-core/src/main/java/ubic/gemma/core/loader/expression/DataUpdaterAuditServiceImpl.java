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
package ubic.gemma.core.loader.expression;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.security.audit.Audited;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataReplacedEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Implementation of {@link DataUpdaterAuditService}: a thin co-bean that exists
 * so {@link #recordDataReplaced(ExpressionExperiment, String)} is invoked
 * through a Spring proxy and the {@link Audited} aspect can intercept its
 * return path and emit the {@link DataReplacedEvent}.
 * <p>
 * Previously this emission lived in a {@code private audit(ee, note, replace)}
 * member of {@code DataUpdaterImpl} self-invoked via {@code this.} from three
 * end-of-method callers ({@code addAffyDataFromAPTOutput},
 * {@code reprocessAffyDataFromCel}, {@code replaceData}) -- Spring AOP cannot
 * intercept self-invocation, so the imperative
 * {@code auditTrailService.addUpdateEvent(ee, DataReplacedEvent.class, note)}
 * call could not be migrated to {@link Audited} during bucket 2g. The {@code
 * replace=false} branch (picking {@code DataAddedEvent}) was retired when
 * {@code DataUpdaterImpl.addData} migrated to {@code @Audited} directly in
 * commit {@code 40dd662883}; only the {@code replace=true} path survives, so
 * this hoist drops the boolean and writes {@code DataReplacedEvent}
 * unconditionally.
 *
 * <p>The {@link Transactional} default propagation ({@code REQUIRED}) honours
 * whichever transaction the caller is in. The three callers above run with
 * {@code @Transactional(propagation = NEVER)}, so each call to
 * {@code recordDataReplaced} opens a fresh transaction here -- matching the
 * pre-hoist behaviour (the imperative {@code auditTrailService.addUpdateEvent}
 * also crossed a transactional service boundary on its way to the audit DAO).
 */
@Service
public class DataUpdaterAuditServiceImpl implements DataUpdaterAuditService {

    private static final Log log = LogFactory.getLog( DataUpdaterAuditServiceImpl.class );

    /**
     * {@inheritDoc}
     *
     * <p>The {@link Audited} annotation drives emission through the
     * {@code AuditedAspect}; this method body is intentionally a logging-only
     * marker so the proxy-intercepted return triggers exactly one
     * {@link DataReplacedEvent}.
     */
    @Override
    @Transactional
    @Audited(value = DataReplacedEvent.class, messageSpel = "#note")
    public void recordDataReplaced( ExpressionExperiment ee, String note ) {
        log.debug( "Data replaced audit for " + ee + ": " + note );
    }
}
