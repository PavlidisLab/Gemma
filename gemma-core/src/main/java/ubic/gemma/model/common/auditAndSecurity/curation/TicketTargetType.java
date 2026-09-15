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
 * Type of entity a {@link TicketTarget} points at. Stored as a
 * {@code VARCHAR(32)} so new types can be introduced without a schema
 * migration (Decision 2 of {@code AUDIT_AS_WORKFLOW_RECCE.md}).
 *
 * @author paul
 */
public enum TicketTargetType {
    EXPRESSION_EXPERIMENT,
    ARRAY_DESIGN,
    /**
     * A single {@link ubic.gemma.model.expression.experiment.FactorValue}.
     * Tickets that flag a specific FV typically also target the owning
     * {@link #EXPRESSION_EXPERIMENT} so the EE-level "any open ticket?"
     * lookup picks them up; see
     * {@code FactorValueNeedsAttentionServiceImpl} for the canonical
     * dual-target usage.
     */
    FACTOR_VALUE,
    /**
     * A {@link ubic.gemma.model.expression.experiment.GeoScrapeWatermark}
     * row. Used by the GEO scrape pipeline to file a per-batch ticket when
     * a scrape completes with at least one match, so the curator queue
     * surfaces the new preboarded candidates as a single work item.
     */
    GEO_SCRAPE_WATERMARK,
    /**
     * A {@link ubic.gemma.model.expression.experiment.PreboardedExperiment} — a GEO accession Gemma knows
     * about but has not loaded. Used for the batch triage ticket: ONE ticket per scrape carrying every
     * candidate from that batch as a target.
     * <p>
     * 🛑 One ticket for the batch, never one per candidate (Paul, 2026-09-02) — the point is to keep the
     * curator queue at one work item per scrape. This type exists so that ticket can name what a curator
     * actually acts on. It replaces hanging the ticket off {@link #GEO_SCRAPE_WATERMARK}, which is the
     * scraper's own resume cursor: once the agent scrapes on its own side, the cursor is the agent's
     * bookkeeping and Gemma has no row to point at.
     */
    PREBOARDED_EXPERIMENT,
    /**
     * A {@link ubic.gemma.model.common.description.BibliographicReference}
     * (PubMed-style publication). Used by agent-driven literature workflows
     * — e.g. an agent files a {@link TicketType#LITERATURE_SEARCH} ticket
     * with the candidate publication as one target and the originating
     * {@link #EXPRESSION_EXPERIMENT} as a co-target so an EE-level "any
     * open ticket?" lookup picks it up.
     */
    BIBLIOGRAPHIC_REFERENCE
}
