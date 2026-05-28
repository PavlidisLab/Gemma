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
 * Emitted on the inherited audit trail when one of a
 * {@link ubic.gemma.model.common.auditAndSecurity.curation.Ticket}'s
 * {@link ubic.gemma.model.common.auditAndSecurity.curation.TicketTarget}s
 * changes status (NOT_DONE / UNDERWAY / DONE). Companion to the
 * {@link ubic.gemma.model.common.auditAndSecurity.curation.TicketEventType#TARGET_STATUS_CHANGED}
 * row on the domain-workflow stream — typical agent flow on a multi-target
 * ticket emits one of each per target completion.
 *
 * <p>The audit NOTE column carries a short summary of the change
 * ({@code "target 42 (EXPRESSION_EXPERIMENT): UNDERWAY -> DONE"}).
 */
@Entity
@DiscriminatorValue("TicketTargetStatusChangedEvent")
public class TicketTargetStatusChangedEvent extends AuditEventType {
}
