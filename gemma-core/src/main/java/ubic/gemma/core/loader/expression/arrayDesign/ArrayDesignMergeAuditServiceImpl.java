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
package ubic.gemma.core.loader.expression.arrayDesign;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.security.audit.Audited;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignMergeEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

/**
 * Implementation of {@link ArrayDesignMergeAuditService}: a thin co-bean that
 * exists so each {@link #recordMerge(ArrayDesign, String)} call is invoked
 * through a Spring proxy and the {@link Audited} aspect can intercept it and
 * emit the {@link ArrayDesignMergeEvent}.
 * <p>
 * The previous incarnation was a {@code private audit(...)} member of
 * {@code ArrayDesignMergeHelperServiceImpl} self-invoked via {@code this.}
 * from {@code persistMerging()} -- Spring AOP cannot intercept either case,
 * so an imperative {@code auditTrailService.addUpdateEvent(...)} call was the
 * only workable shape. Hoisting it onto a separately-injected bean lets the
 * aspect see it and the body collapses to a logging-only marker.
 *
 * <p>The {@link Transactional} default propagation ({@code REQUIRED}) joins
 * the outer transaction opened by
 * {@code ArrayDesignMergeHelperServiceImpl.persistMerging}, matching the
 * pre-hoist behaviour (the legacy
 * {@code auditTrailService.addUpdateEvent(...)} call ran inside the same
 * transaction).
 */
@Service
public class ArrayDesignMergeAuditServiceImpl implements ArrayDesignMergeAuditService {

    private static final Log log = LogFactory.getLog( ArrayDesignMergeAuditServiceImpl.class );

    /**
     * {@inheritDoc}
     *
     * <p>The {@link Audited} annotation drives emission through the
     * {@code AuditedAspect}; this method body is intentionally a logging-only
     * marker so the proxy-intercepted return triggers exactly one
     * {@link ArrayDesignMergeEvent}.
     */
    @Override
    @Transactional
    @Audited(value = ArrayDesignMergeEvent.class, messageSpel = "#note")
    public void recordMerge( ArrayDesign arrayDesign, String note ) {
        log.info( "Array design merge audit for " + arrayDesign + ": " + note );
    }
}
