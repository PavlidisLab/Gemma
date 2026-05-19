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
package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.persistence.service.BaseDao;

import java.util.List;

/**
 * Persistence operations for {@link Ticket} (Phase B-1 of
 * {@code AUDIT_AS_WORKFLOW_RECCE.md}).
 *
 * @author paul
 */
public interface TicketDao extends BaseDao<Ticket> {

    /**
     * Find all tickets that are not in a terminal state and target the
     * given entity. Used to answer "are there any open curation tickets
     * for this EE / array design?". Implements the lookup path described
     * in Decision 2: a composite index over {@code (target_type, target_id)}
     * serves it without joining through {@code ticket_id}.
     */
    List<Ticket> findOpenForTarget( TicketTargetType targetType, Long targetId );

    /**
     * Find all tickets currently assigned to the given contact, regardless
     * of state. Used by curator dashboards.
     */
    List<Ticket> findAssignedTo( Contact assignee );
}
