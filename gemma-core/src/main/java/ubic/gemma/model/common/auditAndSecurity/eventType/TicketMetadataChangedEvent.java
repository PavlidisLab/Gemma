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

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Emitted on the inherited audit trail when a
 * {@link ubic.gemma.model.common.auditAndSecurity.curation.Ticket}'s
 * non-workflow metadata changes — priority, dueDate, title, body, or mode.
 * Unlike state transitions / assignments / comments, metadata edits do NOT
 * append a row to the domain-workflow
 * {@link ubic.gemma.model.common.auditAndSecurity.curation.TicketEvent}
 * stream (Decision 4 of {@code AUDIT_AS_WORKFLOW_RECCE.md} — "no event
 * log spam for fact-of-update edits"); the governance audit trail is the
 * one and only place these mutations are recorded.
 *
 * <p>The audit row's NOTE column carries a short comma-separated list of
 * the fields that actually changed (e.g. {@code "priority, dueDate"}),
 * supplied by the caller.
 */
@Entity
@DiscriminatorValue("TicketMetadataChangedEvent")
public class TicketMetadataChangedEvent extends AuditEventType {
}
