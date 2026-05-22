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
 */
package ubic.gemma.model.common.auditAndSecurity.eventType;

/**
 * Emitted on every successful workflow-state transition of an
 * {@link ubic.gemma.model.analysis.Investigation} (currently:
 * {@link ubic.gemma.model.expression.experiment.ExpressionExperiment}; a
 * forthcoming {@code PreboardedExperiment} subclass will join the same
 * stream).
 *
 * <p>Authored via the declarative {@code @Audited(WorkflowStateChangedEvent.class)}
 * pattern. The audit row's {@code NOTE} carries a human-readable summary of
 * the transition (previous, target, optional reason) built by the SpEL
 * expression on the service method.</p>
 *
 * <p>See {@code HANDOFF_WORKFLOW_STATE_STORAGE.md} §"Audit-event hooks".</p>
 */
public class WorkflowStateChangedEvent extends AuditEventType {
}
