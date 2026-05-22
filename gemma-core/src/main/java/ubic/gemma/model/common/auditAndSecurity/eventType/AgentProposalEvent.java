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
 * Emitted when an {@code AgentProposal} row is appended to a
 * {@code PreboardingExperiment} (or, for the private curation API, to a
 * loaded {@code ExpressionExperiment}).
 *
 * <p>See {@code HANDOFF_PROPOSED_EXPERIMENT_WORKFLOW.md} §"Audit-event hooks".
 * The handoff is explicit: the event does NOT inline the JSON payload; it
 * references the {@code AgentProposal} row by id (carried in the audit
 * row's {@code NOTE} until the structured {@code AUDIT_EVENT.PAYLOAD}
 * column lands).</p>
 */
public class AgentProposalEvent extends AuditEventType {
}
