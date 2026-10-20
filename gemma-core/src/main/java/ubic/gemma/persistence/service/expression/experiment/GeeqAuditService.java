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

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Co-bean carrying the {@code GeeqEvent} emission that
 * {@link GeeqServiceImpl#calculateScore} records at the end of each
 * scoring run.
 * <p>
 * Hoisted out of {@code GeeqServiceImpl} so the emission method is invoked
 * through a Spring proxy and the {@code @Audited} aspect can intercept it --
 * the previous {@code private createGeeqEvent(...)} method was self-invoked
 * via {@code this.} from {@code calculateScore()} and was therefore invisible
 * to AOP, blocking declarative migration.
 *
 * @see ubic.gemma.core.security.audit.Audited
 */
public interface GeeqAuditService {

    /**
     * Record a {@code GeeqEvent} against the given experiment with the
     * supplied note and detail string. Dispatch is via the {@code @Audited}
     * aspect.
     *
     * @param ee     experiment to audit
     * @param note   short summary stored in {@code AUDIT_EVENT.NOTE}
     * @param detail issues / diagnostic text appended to the note
     */
    void recordGeeqScoring( ExpressionExperiment ee, String note, String detail );
}
