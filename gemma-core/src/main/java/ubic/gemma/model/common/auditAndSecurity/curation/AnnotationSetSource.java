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
 * Source discriminator on {@link AnnotationSet} rows. Identifies who
 * produced the payload, orthogonal to the row's
 * {@link AnnotationSetRole role}.
 *
 * <ul>
 *   <li>{@link #AGENT} &mdash; emitted by a curation-agents run. Pairs
 *       with the {@link AnnotationSet#getAgentVersion() agentVersion} /
 *       {@link AnnotationSet#getModel() model} /
 *       {@link AnnotationSet#getRanAt() ranAt} provenance fields.</li>
 *   <li>{@link #CURATOR} &mdash; produced by a human curator (typically
 *       DRAFT rows or curator-blessed SNAPSHOTs).</li>
 *   <li>{@link #GEMMA_INTAKE} &mdash; produced by the Gemma intake
 *       pipeline (e.g. GEO scrape harvest, MINiML parse). Reserves a slot
 *       for the case where the platform itself emits a starting-point
 *       annotation set for an experiment.</li>
 *   <li>{@link #EXTERNAL_IMPORT} &mdash; imported from an external curation
 *       source (CRAFT, Rogic, partner-lab handoff, &hellip;).</li>
 * </ul>
 */
public enum AnnotationSetSource {
    AGENT,
    CURATOR,
    GEMMA_INTAKE,
    EXTERNAL_IMPORT;

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
    public static AnnotationSetSource fromDbValue( String v ) {
        return AnnotationSetSource.valueOf( v.toUpperCase() );
    }
}
