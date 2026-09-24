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
package ubic.gemma.model.expression.experiment;

/**
 * Discriminator on {@code AgentProposal} rows: which kind of agent activity a
 * row records — a forward-looking proposal (pre-curation suggestion), an audit
 * (a post-hoc review of an existing dataset, reporting findings to act on), or
 * an evaluation (a scored assessment of curation that already exists).
 *
 * <p>Forward-compat naming: the enum is {@code AgentCurationKind} even though
 * the entity remains {@code AgentProposal} for now — the step-5 rename to
 * {@code AgentCuration} will not need to re-touch this enum.</p>
 *
 * <p>The Hibernate persistence (see {@code AgentProposal.hbm.xml}) stores the
 * enum {@link #name()} verbatim (uppercase) via {@code EnumType useNamed=true},
 * matching the repo's convention for VARCHAR-mapped enums (Ticket, etc.). The
 * {@link #getDbValue()} / {@link #fromDbValue(String)} helpers expose a
 * lowercase form for DTOs / external surfaces where lowercase
 * {@code "proposal"} / {@code "audit"} is preferred.</p>
 */
public enum AgentCurationKind {
    PROPOSAL,
    AUDIT,

    /**
     * A scored assessment of curation that already exists — an agent grading
     * annotations against a reference, or comparing two runs, rather than
     * suggesting a change ({@link #PROPOSAL}) or reporting defects to act on
     * ({@link #AUDIT}).
     *
     * <p>Distinct from {@link #AUDIT} in what the payload is for: an audit
     * names findings a curator dispositions one by one, and an evaluation
     * reports a measurement over a run. An evaluation set carries no
     * per-finding dispositions, which is why the two cannot share one value
     * without the review panel having to guess which it is holding.</p>
     */
    EVALUATION;

    /**
     * @return the lowercase external form, for use in JSON DTOs / API surfaces.
     */
    public String getDbValue() {
        return name().toLowerCase();
    }

    /**
     * Parse the lowercase external form back into the enum. Accepts either
     * case (delegates to {@link #valueOf(String)} after uppercasing).
     */
    public static AgentCurationKind fromDbValue( String v ) {
        return AgentCurationKind.valueOf( v.toUpperCase() );
    }
}
