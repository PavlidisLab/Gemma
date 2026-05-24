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
 *
 */
package ubic.gemma.cli.audit;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

/**
 * Co-bean that owns the audit-emission step for the ArrayDesign-targeted CLI
 * tools. Each method is annotated with the appropriate
 * {@link ubic.gemma.core.security.audit.Audited} so the {@code AuditedAspect}
 * fires when a CLI calls through this bean.
 *
 * <p>The CLIs themselves are registered as Spring prototype beans (see
 * {@code CliComponentScanConfig}), but the imperative
 * {@code auditTrailService.addUpdateEvent(...)} sites they previously housed
 * lived inside {@code private void audit(...)} helpers invoked from same-class
 * methods. Spring AOP cannot intercept either path: the {@code private}
 * modifier hides the join point from the proxy, and the self-invocation
 * targets the underlying {@code this} reference rather than the proxy. Hoisting
 * the emission step onto this separately-injected bean restores a proxy
 * boundary so the aspect can do its work.
 *
 * <p>Each {@code record*} method additionally regenerates the array-design
 * report -- preserving the side effect of the legacy {@code audit(...)}
 * helpers, which paired the audit-event emission with a report refresh.
 */
public interface CliArrayDesignAuditService {

    /**
     * Records an {@code ArrayDesignSequenceAnalysisEvent} on the platform.
     * Used by {@code ArrayDesignBlatCli} after a successful BLAT alignment
     * pass (or after re-using results from a PSL file).
     */
    void recordSequenceAnalysis( ArrayDesign arrayDesign, String note );

    /**
     * Records an {@code ArrayDesignSequenceUpdateEvent} on the platform.
     * Used by {@code ArrayDesignSequenceAssociationCli} after attaching or
     * looking up biological sequences for the platform's composite sequences.
     */
    void recordSequenceUpdate( ArrayDesign arrayDesign, String note );

    /**
     * Records an {@code ArrayDesignSequenceRemoveEvent} on the platform.
     * Used by {@code ArrayDesignBioSequenceDetachCli} after detaching the
     * sequences associated with the platform.
     */
    void recordSequenceRemove( ArrayDesign arrayDesign, String note );

    /**
     * Records an {@code ArrayDesignProbeRenamingEvent} on the platform. Used
     * by {@code ArrayDesignProbeRenamerCli} after applying a probe-renaming
     * map.
     */
    void recordProbeRenaming( ArrayDesign arrayDesign, String note );

    /**
     * Records an {@code ArrayDesignRepeatAnalysisEvent} on the platform.
     * Used by {@code ArrayDesignRepeatScanCli} after running a repeat-masker
     * scan over the platform's sequences.
     */
    void recordRepeatAnalysis( ArrayDesign arrayDesign, String note );

    /**
     * Records an {@code ArrayDesignSubsumeCheckEvent} on the platform. Used
     * by {@code ArrayDesignSubsumptionTesterCli} after testing whether the
     * platform subsumes (or is subsumed by) other platforms.
     */
    void recordSubsumeCheck( ArrayDesign arrayDesign, String note );

    /**
     * Records an {@code AnnotationBasedGeneMappingEvent} on the platform.
     * Used by {@code GenericGenelistDesignGenerator} after rebuilding the
     * platform from a list of NCBI gene IDs.
     */
    void recordAnnotationBasedGeneMapping( ArrayDesign arrayDesign, String note );

    /**
     * Records an {@code AlignmentBasedGeneMappingEvent} on the platform. Used
     * by {@code ArrayDesignProbeMapperCli} for runs whose gene mapping came
     * from BLAT alignment / probe sequence analysis (the default mode and the
     * batch path).
     */
    void recordAlignmentBasedGeneMapping( ArrayDesign arrayDesign, String note );
}
