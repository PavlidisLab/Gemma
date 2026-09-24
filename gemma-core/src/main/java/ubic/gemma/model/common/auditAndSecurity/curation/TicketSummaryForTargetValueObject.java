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

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * One open ticket as it bears on ONE target &mdash; the per-ticket summary plus that target's own
 * {@link TicketTargetStatus}.
 *
 * <h2>Why this is a subtype rather than a field on {@link TicketSearchHitValueObject}</h2>
 *
 * <p>{@code targetStatus} is per-TARGET and a ticket can hold many, so it is not a property of the
 * ticket. {@link TicketSearchHitValueObject} is also produced by
 * {@code TicketDaoImpl.buildSearchHitHql}, which selects from {@code Ticket} with no join to
 * {@code t.targets} and so has no target to report a status for. A nullable field on the shared VO
 * would therefore be populated on the bulk summaries route and null on every search route, and that
 * null would be indistinguishable from a {@link TicketTargetStatus#NOT_DONE} that failed to load
 * &mdash; the reading that {@code NOT_DONE} default exists to make impossible.</p>
 *
 * <p>As a subtype the field exists only where it is always populated. Callers wanting the ticket
 * alone keep the base type, and the JSON is the same object with one more key.</p>
 *
 * <p>Note the shape this carries: {@code findOpenSummariesForTargets} returns one of these per
 * (ticket, target) pair, keyed by target id, so within one target's list every entry is that
 * target's own status. Nothing folds across a ticket's other targets.</p>
 *
 * @author gembro
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TicketSummaryForTargetValueObject extends TicketSearchHitValueObject {

    private static final long serialVersionUID = 1L;

    /**
     * This target's status on this ticket. Never null: {@link TicketTarget} defaults it to
     * {@link TicketTargetStatus#NOT_DONE}.
     */
    private TicketTargetStatus targetStatus;

    public TicketSummaryForTargetValueObject() {
    }

    public TicketSummaryForTargetValueObject( Long id, String title, TicketState state, TicketType type,
            long targetCount, Date updatedAt, TicketPriority priority, TicketTargetStatus targetStatus ) {
        super( id, title, state, type, targetCount, updatedAt, priority );
        this.targetStatus = targetStatus;
    }
}
