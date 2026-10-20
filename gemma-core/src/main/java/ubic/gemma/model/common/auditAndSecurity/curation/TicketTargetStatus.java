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
 *   <li>{@link #UNDERWAY} — work has started on this target. Set by runners when an
 *       async action begins, and by a curation commit on the target dataset: an edit
 *       is evidence somebody started the ask, never that they finished it. The UI
 *       renders this as a spinner / in-flight badge.</li>
 *   <li>{@link #DONE} — the ask was done, decided or finished (Paul, 2026-09-05).
 *       The parent ticket can advance to {@link TicketState#RESOLVED} when all
 *       targets are DONE (or earlier, at curator discretion).
 *       <p>
 *       🛑 <b>Only a person sets this.</b> It used to read "the per-target work the
 *       ticket required is committed", and on that reading
 *       {@code advanceLinkedCurationTickets} moved a target straight to DONE on any
 *       commit — which asserts that the commit WAS the required work. It need not be:
 *       it may be unrelated to the ask, partial, or a test edit. Two commits made to
 *       exercise a lock comparison closed a target on the reference-500 queue on
 *       2026-09-05, and the revert did not reopen it. That path now sets UNDERWAY.</li>
 * </ul>
 *
 * @author paul
 */
public enum TicketTargetStatus {
    NOT_DONE,
    UNDERWAY,
    DONE
}
