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
package ubic.gemma.core.loader.expression.geo.service;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Co-bean carrying the audit emission for
 * {@link GeoServiceImpl#updateFromGEO(ExpressionExperiment, GeoUpdateConfig)}.
 * <p>
 * Hoisted out of {@code GeoServiceImpl} so the emission can carry an
 * {@code @AuditedConditional} declaration and be intercepted by Spring AOP --
 * the previous emission lived inside a {@code private} self-invoked
 * {@code updateFromGEO(...)} helper on {@code GeoServiceImpl} and was therefore
 * invisible to the proxy, blocking a direct annotation migration.
 *
 * @see ubic.gemma.core.security.audit.AuditedConditional
 */
public interface GeoUpdateAuditService {

    /**
     * Record an {@code ExpressionExperimentUpdateFromGEOEvent} audit row for
     * {@code ee}, but only when the update actually changed something --
     * either a new characteristic was added or the primary publication was
     * populated. A no-op call (both inputs zero/false) writes no audit row.
     */
    void recordGeoUpdate( ExpressionExperiment ee, int numNewCharacteristics, boolean pubUpdate );
}
