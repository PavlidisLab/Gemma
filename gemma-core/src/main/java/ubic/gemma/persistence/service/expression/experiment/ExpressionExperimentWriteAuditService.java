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

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;

/**
 * Co-bean for {@link ExpressionExperimentWriteServiceImpl}: a thin layer
 * that exists so the preferred-{@link QuantitationType} change-audit
 * methods are invoked through a Spring proxy, allowing the {@code AuditedAspect}
 * to intercept the return path.
 *
 * <p>The two emission cases (inventory #15/#16 in
 * {@code AUDIT_RESIDUAL_INVENTORY.md}) live in
 * {@link ExpressionExperimentWriteServiceImpl#updateQuantitationType} and
 * pick the {@code PreferredDataChangedEvent} subclass at runtime from the
 * data-vector type of the affected QT. The migration uses the {@code
 * valueSpel} attribute on {@code @Audited} to express this runtime dispatch
 * declaratively.
 */
public interface ExpressionExperimentWriteAuditService {

    /**
     * Audit that the preferred {@link QuantitationType} for the given vector
     * type changed to {@code qt}. The recorded event class is the
     * {@code PreferredDataChangedEvent} subclass matching {@code vectorType}
     * (see {@link
     * ExpressionExperimentWriteServiceImpl#getPreferredDataChangedEventForVectorType});
     * the caller is responsible for not invoking this method when no event
     * class exists for the vector type (e.g. processed data).
     *
     * @param ee                  the experiment whose preferred QT changed
     * @param qt                  the new preferred QT
     * @param previousPreferredQt the prior preferred QT (may be null)
     * @param vectorType          the data-vector type that drives event-class dispatch
     */
    void recordPreferredQtChanged( ExpressionExperiment ee, QuantitationType qt,
            @Nullable QuantitationType previousPreferredQt,
            Class<? extends DataVector> vectorType );

    /**
     * Audit that the preferred {@link QuantitationType} for the given vector
     * type was cleared (the previously-preferred QT is no longer preferred).
     * Same dispatch rule as {@link #recordPreferredQtChanged}.
     *
     * @param ee                  the experiment whose preferred QT was cleared
     * @param previousPreferredQt the QT that used to be preferred (non-null)
     * @param vectorType          the data-vector type that drives event-class dispatch
     */
    void recordPreferredQtCleared( ExpressionExperiment ee, QuantitationType previousPreferredQt,
            Class<? extends DataVector> vectorType );
}
