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
 * How a {@link Ticket} advances between actions.
 *
 * <ul>
 *   <li>{@link #MANUAL} (default) — the curator triggers each next action explicitly
 *       (e.g. clicks "run preload" on the ticket detail page after the previous
 *       action finishes).</li>
 *   <li>{@link #AUTO} — when a runner finishes with all targets in
 *       {@link TicketTargetStatus#DONE} and no recorded failures, the server schedules
 *       the next defined action automatically. The pipeline of "what comes next" is
 *       defined per-{@link TicketType} (e.g. PRELOAD → PROPOSE → REVIEW for a
 *       curation ticket).</li>
 * </ul>
 *
 * The flip between modes is itself a curator action; flipping to AUTO mid-ticket
 * schedules the next action immediately if the current step is already complete.
 *
 * @author paul
 */
public enum TicketMode {
    MANUAL,
    AUTO
}
