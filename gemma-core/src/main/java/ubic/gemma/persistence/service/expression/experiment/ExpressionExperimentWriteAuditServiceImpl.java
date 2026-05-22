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
 */
package ubic.gemma.persistence.service.expression.experiment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.security.audit.Audited;
import ubic.gemma.model.common.auditAndSecurity.eventType.PreferredDataChangedEvent;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;

/**
 * Co-bean implementation of {@link ExpressionExperimentWriteAuditService}.
 *
 * <p>Each method body is intentionally a logging-only marker: the
 * {@link Audited#valueSpel()} attribute on the annotation drives the actual
 * audit-row emission through {@code AuditedAspect}, which resolves the
 * concrete {@link PreferredDataChangedEvent} subclass from the
 * {@code vectorType} parameter via
 * {@link ExpressionExperimentWriteServiceImpl#getPreferredDataChangedEventForVectorType}
 * (now a {@code public static} helper so it is reachable from SpEL).
 *
 * <p>Replaces the imperative {@code auditTrailService.addUpdateEvent(...)}
 * calls in {@link ExpressionExperimentWriteServiceImpl#updateQuantitationType}
 * (inventory #15/#16 in {@code AUDIT_RESIDUAL_INVENTORY.md}).
 */
@Service
public class ExpressionExperimentWriteAuditServiceImpl implements ExpressionExperimentWriteAuditService {

    private static final Log log = LogFactory.getLog( ExpressionExperimentWriteAuditServiceImpl.class );

    /**
     * {@inheritDoc}
     *
     * <p>The {@code valueSpel} expression resolves to a concrete subclass of
     * {@link PreferredDataChangedEvent} chosen by {@code vectorType}; the
     * {@code messageSpel} formats the historical
     * "The preferred quantitation type for X changed [from Y] to Z."
     * note.
     */
    @Override
    @Transactional
    @Audited(
            valueSpel = "T(ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentWriteServiceImpl).getPreferredDataChangedEventForVectorType(#vectorType)",
            messageSpel = "'The preferred quantitation type for ' + #vectorType.simpleName + ' changed' + (#previousPreferredQt != null ? ' from ' + #previousPreferredQt : '') + ' to ' + #qt + '.'" )
    public void recordPreferredQtChanged( ExpressionExperiment ee, QuantitationType qt,
            @Nullable QuantitationType previousPreferredQt,
            Class<? extends DataVector> vectorType ) {
        log.debug( "Preferred QT changed audit for " + ee + " (vectorType=" + vectorType.getSimpleName() + ", qt=" + qt + ")" );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Same dispatch as {@link #recordPreferredQtChanged}; the
     * {@code messageSpel} formats the historical
     * "The preferred quantitation type for X was cleared (previously Y)."
     * note.
     */
    @Override
    @Transactional
    @Audited(
            valueSpel = "T(ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentWriteServiceImpl).getPreferredDataChangedEventForVectorType(#vectorType)",
            messageSpel = "'The preferred quantitation type for ' + #vectorType.simpleName + ' was cleared (previously ' + #previousPreferredQt + ').'" )
    public void recordPreferredQtCleared( ExpressionExperiment ee, QuantitationType previousPreferredQt,
            Class<? extends DataVector> vectorType ) {
        log.debug( "Preferred QT cleared audit for " + ee + " (vectorType=" + vectorType.getSimpleName() + ", previousPreferredQt=" + previousPreferredQt + ")" );
    }
}
