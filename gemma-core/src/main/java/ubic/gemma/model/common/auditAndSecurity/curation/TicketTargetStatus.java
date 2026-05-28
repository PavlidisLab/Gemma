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
 * Per-target progress through a {@link Ticket}'s work. Tracks one {@link TicketTarget}'s
 * lifecycle independently of the parent {@link Ticket}'s {@link TicketState} so a
 * multi-target ticket can render a status roll-up rather than committing the whole
 * ticket as resolved when only some targets are complete.
 *
 * <ul>
 *   <li>{@link #NOT_DONE} — initial state; the curator (or runner) hasn't touched
 *       this target yet.</li>
 *   <li>{@link #UNDERWAY} — work has started on this target but isn't committed.
 *       Set by runners when an async action begins; the UI renders this as a
 *       spinner / in-flight badge.</li>
 *   <li>{@link #DONE} — the per-target work the ticket required is committed.
 *       The parent ticket can advance to {@link TicketState#RESOLVED} when all
 *       targets are DONE (or earlier, at curator discretion).</li>
 * </ul>
 *
 * @author paul
 */
public enum TicketTargetStatus {
    NOT_DONE,
    UNDERWAY,
    DONE
}
