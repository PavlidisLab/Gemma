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

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

/**
 * Co-bean carrying the {@code ArrayDesignMergeEvent} emission that
 * {@link ArrayDesignMergeHelperServiceImpl#persistMerging} records against
 * each participating platform.
 * <p>
 * Hoisted out of {@code ArrayDesignMergeHelperServiceImpl} so the emission
 * method is invoked through a Spring proxy and the {@code @Audited} aspect
 * can intercept it -- the previous {@code private audit(...)} method was
 * self-invoked via {@code this.} from {@code persistMerging()} and was
 * therefore invisible to AOP, blocking declarative migration.
 *
 * @see ubic.gemma.core.security.audit.Audited
 */
public interface ArrayDesignMergeAuditService {

    /**
     * Record an {@code ArrayDesignMergeEvent} against the given platform with
     * the supplied note. Dispatch is via the {@code @Audited} aspect.
     *
     * @param arrayDesign array design to audit
     * @param note        free-text note stored in {@code AUDIT_EVENT.NOTE}
     */
    void recordMerge( ArrayDesign arrayDesign, String note );
}
