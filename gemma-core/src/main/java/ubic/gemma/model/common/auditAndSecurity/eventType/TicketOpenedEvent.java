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
 * Emitted on the inherited audit trail when a new
 * {@link ubic.gemma.model.common.auditAndSecurity.curation.Ticket} is created.
 * Companion to the {@code OPENED} {@link
 * ubic.gemma.model.common.auditAndSecurity.curation.TicketEventType} on the
 * domain-workflow stream — the two streams are written in lockstep on
 * {@code TicketService.openTicket}.
 */
@Entity
@DiscriminatorValue("TicketOpenedEvent")
public class TicketOpenedEvent extends AuditEventType {
}
