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

import org.springframework.stereotype.Service;
import ubic.gemma.core.security.audit.AuditedConditional;
import ubic.gemma.model.common.auditAndSecurity.eventType.ExpressionExperimentUpdateFromGEOEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Implementation of {@link GeoUpdateAuditService}: a thin co-bean that exists
 * so {@link #recordGeoUpdate} is invoked through a Spring proxy and the
 * {@link AuditedConditional} aspect can intercept its return path.
 * <p>
 * Previously this emission lived inline at the tail of the {@code private}
 * {@code updateFromGEO(ExpressionExperiment, String, ExpressionExperiment, GeoUpdateConfig)}
 * helper on {@code GeoServiceImpl} and was reached by self-invocation from the
 * two public overloads -- Spring AOP cannot intercept either case, so the
 * imperative {@code auditTrailService.addUpdateEvent(...)} call could not be
 * migrated to {@link AuditedConditional} in bucket 2c.
 *
 * <p>The method body is intentionally empty. Emission semantics live entirely
 * in the {@link AuditedConditional} declaration: the {@code when=} SpEL gates
 * whether a row is written, and the {@code messageSpel=} expression builds the
 * note string verbatim matching the legacy
 * {@code " Updated from GEO; N characteristics added/replaced[; Publication added]"}.
 * The leading space in the message is intentional and preserved for byte-for-byte
 * compatibility with the previous emission.
 */
@Service
public class GeoUpdateAuditServiceImpl implements GeoUpdateAuditService {

    @Override
    @AuditedConditional(value = ExpressionExperimentUpdateFromGEOEvent.class,
            when = "#numNewCharacteristics > 0 || #pubUpdate",
            messageSpel = "' Updated from GEO; ' + #numNewCharacteristics + ' characteristics added/replaced' + (#pubUpdate ? '; Publication added' : '')")
    public void recordGeoUpdate( ExpressionExperiment ee, int numNewCharacteristics, boolean pubUpdate ) {
        // Audit emission handled by AuditedConditional aspect via @AfterReturning.
    }
}
