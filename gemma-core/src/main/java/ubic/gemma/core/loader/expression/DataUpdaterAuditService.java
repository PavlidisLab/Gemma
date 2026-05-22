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

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Co-bean carrying the {@code DataReplacedEvent} emission written at the end of
 * the data-replacement public methods on {@link DataUpdaterImpl}
 * ({@code addAffyDataFromAPTOutput}, {@code reprocessAffyDataFromCel},
 * {@code replaceData}).
 * <p>
 * Hoisted out of {@code DataUpdaterImpl} so the emission method is invoked
 * through a Spring proxy and the {@code @Audited} aspect can intercept it --
 * the previous {@code private audit(...)} method was self-invoked via
 * {@code this.} from those callers and was therefore invisible to AOP,
 * blocking declarative migration.
 *
 * <p>The legacy private helper carried a {@code boolean replace} switch that
 * picked {@code DataReplacedEvent} when {@code true} and
 * {@code DataAddedEvent} when {@code false}. All three remaining call sites
 * pass {@code true}; the {@code false} branch went away when {@code addData}
 * migrated directly to {@code @Audited(DataAddedEvent.class)} in commit
 * {@code 40dd662883}. The hoisted method records {@code DataReplacedEvent}
 * unconditionally.
 *
 * @see ubic.gemma.core.security.audit.Audited
 */
public interface DataUpdaterAuditService {

    /**
     * Record a {@code DataReplacedEvent} against the given experiment with the
     * supplied note. Dispatch is via the {@code @Audited} aspect.
     *
     * @param ee   experiment to audit
     * @param note free-text note stored in {@code AUDIT_EVENT.NOTE}
     */
    void recordDataReplaced( ExpressionExperiment ee, String note );
}
