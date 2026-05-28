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
 * Domain category of a {@link Ticket}. Mirrors the typical curator workflows
 * called out in {@code AUDIT_AS_WORKFLOW_RECCE.md}. New values can be added
 * without a schema migration (the column is {@code VARCHAR(64)}).
 *
 * @author paul
 */
public enum TicketType {
    /** Batch information is missing or ambiguous and the EE can't be analyzed until it's resolved. */
    BATCH_INFO_NEEDED,
    /** A re-alignment to a new genome / annotation set is required. */
    REALIGNMENT_NEEDED,
    /** General quality-review request (geeq follow-up, suspect outliers, etc.). */
    QUALITY_REVIEW,
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
     */
    CURATION,
    /** Catch-all for tickets that don't fit a more specific category. */
    GENERIC
}
