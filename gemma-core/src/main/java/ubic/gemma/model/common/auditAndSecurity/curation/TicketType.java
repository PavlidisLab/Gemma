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
 * Domain category of a {@link Ticket}. Mirrors the typical curator and
 * agent-driven workflows called out in {@code AUDIT_AS_WORKFLOW_RECCE.md}.
 * New values can be added without a schema migration (the column is
 * {@code VARCHAR(64)}); the entries below are illustrative and meant to
 * grow as new workflow categories surface.
 *
 * @author paul
 */
public enum TicketType {
    /** Batch information is missing or ambiguous and the EE can't be analyzed until it's resolved. */
    BATCH_INFO_NEEDED,
    /**
     * Sequencing data needs to be (re-)aligned — either first-time alignment of
     * uploaded RNA-seq reads, or re-alignment of an existing dataset to a new
     * genome / annotation set.
     */
    REALIGNMENT_NEEDED,
    /** General quality-review request (geeq follow-up, suspect outliers, etc.). */
    QUALITY_REVIEW,
    /**
     * Pre-import triage: a curator decides include / exclude on a candidate GEO
     * accession before it is loaded into Gemma at all. Distinct from {@link #PRELOAD},
     * which populates metadata for a candidate that has already been preboarded — a
     * SCREENING ticket is finalized first, and only an "include" decision leads to the
     * dataset being imported.
     * <p>
     * Targets may therefore name accessions Gemma has not imported. The curation store
     * models those with a synthetic {@code GEO_ACCESSION} target type that Gemma
     * deliberately does not have; a screening ticket reaching Gemma carries targets it
     * can actually address.
     */
    SCREENING,
    /**
     * GEO deep-fetch / metadata pre-population work. Filed by the scrape pipeline
     * or by a curator before manual annotation begins; targets are typically
     * preboarded EE candidates whose sample-level metadata still needs to be
     * populated from GEO eutils.
     */
    PRELOAD,
    /**
     * Manual curation of one or more EEs — annotate factor values, write factor
     * descriptions, tag baseline relevance, etc. The default work-item type for
     * curator-assigned tickets that don't fit a more specific category.
     * <p>
     * The curation store's own {@code REVIEW} type maps here (Paul, 2026-08-29). The
     * store classifies a review ticket as curation work and drives the edit-vs-review
     * affordance from a separate {@code flow} field, so {@code REVIEW} and this value
     * denote the same category and only one of the two names is kept.
     */
    CURATION,
    /**
     * Agent-driven literature search — find publications relevant to a dataset
     * (or a candidate set of publications for triage). Illustrative of the
     * non-EE-curation work items the ticket framework supports; the agent
     * opens, comments with what it found, and transitions to RESOLVED.
     */
    LITERATURE_SEARCH,
    /** Catch-all for tickets that don't fit a more specific category. */
    GENERIC
}
