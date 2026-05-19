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
 * Kind of a workflow event appended to a {@link Ticket}'s
 * {@link Ticket#getEvents() events} log. Append-only; in-place edits are
 * deferred per Decision 4 of {@code AUDIT_AS_WORKFLOW_RECCE.md}.
 *
 * @author paul
 */
public enum TicketEventType {
    OPENED,
    ASSIGNED,
    COMMENTED,
    STATE_CHANGED,
    RESOLVED,
    CANCELLED,
    REOPENED,
    /** Deferred — see Decision 4 in the recce doc. Provisioned in the enum so the column never has to grow. */
    COMMENT_EDITED
}
