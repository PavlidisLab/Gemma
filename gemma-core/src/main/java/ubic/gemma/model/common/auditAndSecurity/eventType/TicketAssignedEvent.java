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
 * {@code assignee} is set or cleared. Companion to the {@code ASSIGNED}
 * {@link ubic.gemma.model.common.auditAndSecurity.curation.TicketEventType} on
 * the domain-workflow stream.
 */
@Entity
@DiscriminatorValue("TicketAssignedEvent")
public class TicketAssignedEvent extends AuditEventType {
}
