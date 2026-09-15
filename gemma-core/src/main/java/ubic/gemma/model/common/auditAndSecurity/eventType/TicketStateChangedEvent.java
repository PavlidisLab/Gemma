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
 * {@link ubic.gemma.model.common.auditAndSecurity.curation.Ticket}
 * transitions to a different {@link
 * ubic.gemma.model.common.auditAndSecurity.curation.TicketState} (OPEN /
 * IN_PROGRESS / RESOLVED / CANCELLED). Companion to the {@code STATE_CHANGED}
 * / {@code RESOLVED} / {@code CANCELLED} / {@code REOPENED} {@link
 * ubic.gemma.model.common.auditAndSecurity.curation.TicketEventType} on the
 * domain-workflow stream — distinct from
 * {@link WorkflowStateChangedEvent}, which tracks the {@code Investigation}
 * workflow state (Loaded / Analyzed / Released …).
 */
@Entity
@DiscriminatorValue("TicketStateChangedEvent")
public class TicketStateChangedEvent extends AuditEventType {
}
