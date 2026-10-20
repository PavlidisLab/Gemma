/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.model.common.auditAndSecurity.curation;

/**
 * Lifecycle state of a {@link Ticket}. Decision 2 of
 * {@code AUDIT_AS_WORKFLOW_RECCE.md}: state is stored explicitly, NOT derived
 * from the event log.
 *
 * @author paul
 */
public enum TicketState {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CANCELLED
}
