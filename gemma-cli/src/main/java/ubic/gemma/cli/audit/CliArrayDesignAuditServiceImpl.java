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

import org.springframework.stereotype.Service;
import ubic.gemma.core.security.audit.Audited;
import ubic.gemma.model.common.auditAndSecurity.eventType.AlignmentBasedGeneMappingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AnnotationBasedGeneMappingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignProbeRenamingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignRepeatAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceRemoveEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceUpdateEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSubsumeCheckEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

/**
 * Default {@link CliArrayDesignAuditService} implementation. Each method body
 * is intentionally empty: the work is done by {@code AuditedAspect}, which
 * picks up the {@code @Audited} annotation, locates the {@link ArrayDesign}
 * argument, and writes the audit row through {@code AuditTrailService}.
 * Resolving the audit note through {@code messageSpel} keeps the note
 * verbatim from the caller's argument.
 *
 * <p>Lives under {@code ubic.gemma.cli.audit} so it is picked up by the
 * stereotype scan declared in {@code CliComponentScanConfig} (the
 * {@code ubic.gemma.apps} scan is restricted to {@link ubic.gemma.cli.util.CLI}
 * assignable types and would not include this co-bean).
 *
 * <p>The legacy {@code private void audit(...)} helpers in the CLI tools
 * paired audit-event emission with an
 * {@code arrayDesignReportService.generateArrayDesignReport(...)} call. The
 * report refresh is preserved at the CALL site -- it has no relation to the
 * audit event and stays in the CLI's own code path.
 */
@Service
public class CliArrayDesignAuditServiceImpl implements CliArrayDesignAuditService {

    @Override
    @Audited(value = ArrayDesignSequenceAnalysisEvent.class, messageSpel = "#note")
    public void recordSequenceAnalysis( ArrayDesign arrayDesign, String note ) {
    }

    @Override
    @Audited(value = ArrayDesignSequenceUpdateEvent.class, messageSpel = "#note")
    public void recordSequenceUpdate( ArrayDesign arrayDesign, String note ) {
    }

    @Override
    @Audited(value = ArrayDesignSequenceRemoveEvent.class, messageSpel = "#note")
    public void recordSequenceRemove( ArrayDesign arrayDesign, String note ) {
    }

    @Override
    @Audited(value = ArrayDesignProbeRenamingEvent.class, messageSpel = "#note")
    public void recordProbeRenaming( ArrayDesign arrayDesign, String note ) {
    }

    @Override
    @Audited(value = ArrayDesignRepeatAnalysisEvent.class, messageSpel = "#note")
    public void recordRepeatAnalysis( ArrayDesign arrayDesign, String note ) {
    }

    @Override
    @Audited(value = ArrayDesignSubsumeCheckEvent.class, messageSpel = "#note")
    public void recordSubsumeCheck( ArrayDesign arrayDesign, String note ) {
    }

    @Override
    @Audited(value = AnnotationBasedGeneMappingEvent.class, messageSpel = "#note")
    public void recordAnnotationBasedGeneMapping( ArrayDesign arrayDesign, String note ) {
    }

    @Override
    @Audited(value = AlignmentBasedGeneMappingEvent.class, messageSpel = "#note")
    public void recordAlignmentBasedGeneMapping( ArrayDesign arrayDesign, String note ) {
    }
}
