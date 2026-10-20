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
 * The outcome a curator or agent recorded for one {@link TicketTarget} when screening it.
 * <p>
 * Deliberately generic and interpreted by the ticket's situation rather than fixed to one
 * workflow (Paul, 2026-08-29). On a "which experiments should we include" screen,
 * {@link #INCLUDE} / {@link #REJECT} mean admit / do not admit; on a ticket that forwards
 * experiments to some other process, the same three read as "yes, act" / "no, skip" /
 * "not yet decided". What the result then drives — a load, a blacklist entry, a re-run —
 * is not encoded here and not wired up in Gemma yet: the results are exported and CLI tools
 * take the action. This enum is only the recorded decision.
 * <p>
 * It is intentionally NOT coupled to {@link TicketTargetStatus}: a REJECT that means "does
 * not need this process" is not the same as the target being DONE, so the two are set
 * independently. {@code null} means nobody has screened this target yet — distinct from
 * {@link #UNDECIDED}, which is a reviewer's recorded "looked, cannot resolve".
 * <p>
 * Not called a "triage" result on purpose — that word already names the audit verdict
 * ({@link TriageVerdict}: fine / wont_fix / might_fix / must_fix on an annotation set), a
 * different decision on a different thing.
 */
public enum ScreeningResult {
    /** Admit / accept / act on this target. */
    INCLUDE,
    /** Do not admit / reject / this target needs no further action. */
    REJECT,
    /**
     * Reviewed but could not be resolved — a curator or agent looked and could not decide.
     * <p>
     * 🛑 This is NOT the same state as {@code null}. {@code null} means nobody has looked yet;
     * {@code UNDECIDED} means someone looked and it is genuinely open. The curation store keeps
     * these apart as {@code undecided} (never looked) vs {@code unsure} (reviewed, unresolved),
     * and its own summary warns that folding them together hides exactly the rows a curator most
     * needs to pick up. So an untouched target is {@code null}, and a target a reviewer parked is
     * {@code UNDECIDED}; do not default one to the other.
     */
    UNDECIDED
}
