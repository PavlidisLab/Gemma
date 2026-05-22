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
 */
package ubic.gemma.model.common.auditAndSecurity.eventType;

/**
 * Emitted by {@code POST /preboarding} when a {@code PreboardingExperiment}
 * is created for a previously-unknown accession.
 *
 * <p>See {@code HANDOFF_PROPOSED_EXPERIMENT_WORKFLOW.md} §"Audit-event hooks".
 * Authored via the declarative {@code @Audited(PreboardingCreatedEvent.class)}
 * pattern; the audit row's {@code NOTE} carries the accession.</p>
 */
public class PreboardingCreatedEvent extends AuditEventType {
}
