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
package ubic.gemma.model.common.auditAndSecurity.curation;

/**
 * Role discriminator on {@link AnnotationSet} rows. Distinguishes four
 * lifecycle shapes:
 *
 * <ul>
 *   <li>{@link #PROPOSAL} &mdash; immutable hypothesis emitted by an agent
 *       run or external import. Subsumes both the forward-looking proposal
 *       (kind={@code proposal}) and the post-hoc audit (kind={@code audit})
 *       that {@code AgentProposal} historically split via its
 *       {@code AgentCurationKind} column. The sub-discriminator continues
 *       to live on the {@link AnnotationSet#getKind() kind} field.</li>
 *   <li>{@link #DRAFT} &mdash; mutable curator work-in-progress buffer.
 *       Typically seeded from a {@code PROPOSAL} (the
 *       {@link AnnotationSet#getParent() parent} link), with the curator's
 *       per-element decisions derived at read time by diffing this row's
 *       payload against its parent's.</li>
 *   <li>{@link #SNAPSHOT} &mdash; immutable capture of the experiment's
 *       annotation state at a moment in time. A SNAPSHOT with
 *       {@link AnnotationSet#getFinalizedAt() finalizedAt} set is the
 *       "polished" view a curator has blessed as canonical; without it,
 *       the row is a raw capture (promotion artifact, comparison probe,
 *       etc.). <b>The applied annotations themselves live on the EE
 *       entity graph</b> (Characteristic, Statement, FactorValue, &hellip;);
 *       this row stores only a JSON description of that state for
 *       comparison / history.</li>
 *   <li>{@link #COMMIT} &mdash; immutable record that a curation was
 *       <em>applied</em> to the dataset, and by which run. Proposed-versus-
 *       applied is the distinction the whole provenance surface rests on:
 *       "an agent suggested this" and "this is what the data says" are
 *       different claims about the world, and a curator reading a trace has
 *       to know which one they are looking at. Overloading {@link #PROPOSAL}
 *       would force every consumer that branches on role to re-derive
 *       appliedness from somewhere else, and they would disagree about how.
 *       Like SNAPSHOT, the applied annotations themselves live on the EE
 *       entity graph; this row records <em>who applied them and from which
 *       build</em>. Sparse on purpose &mdash; a commit with no run reference
 *       mints no row at all.</li>
 * </ul>
 *
 * <p>Persisted as the enum {@link #name()} (uppercase). The
 * {@link #getDbValue()} / {@link #fromDbValue(String)} helpers expose a
 * lowercase form for DTOs / wire surfaces, matching the convention
 * established by {@code AgentCurationKind}.</p>
 */
public enum AnnotationSetRole {
    PROPOSAL,
    DRAFT,
    SNAPSHOT,
    COMMIT;

    /**
     * @return the lowercase external form, for use in JSON DTOs / API
     *         surfaces.
     */
    public String getDbValue() {
        return name().toLowerCase();
    }

    /**
     * Parse the lowercase external form back into the enum. Accepts either
     * case (delegates to {@link #valueOf(String)} after uppercasing).
     */
    public static AnnotationSetRole fromDbValue( String v ) {
        return AnnotationSetRole.valueOf( v.toUpperCase() );
    }
}
