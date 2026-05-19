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
    FACTOR_VALUE
}
