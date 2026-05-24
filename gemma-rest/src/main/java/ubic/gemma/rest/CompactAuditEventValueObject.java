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
package ubic.gemma.rest;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;

import java.util.Date;

/**
 * Wire payload for {@code GET /datasets/{id}/auditEvents?compact=true}.
 * <p>
 * Wraps an {@link AuditEventValueObject} (unwrapped at the JSON level so the shape stays a flat
 * superset of the legacy entry) and adds two run-collapsing fields:
 * <ul>
 *   <li>{@code collapsedCount} — number of consecutive audit events sharing the same
 *       (eventType, performer) pair that this entry represents. {@code 1} for a solo event.</li>
 *   <li>{@code lastOccurrence} — timestamp of the LAST event in the run; equals the entry's
 *       {@code date} for a solo event.</li>
 * </ul>
 * Compression happens within the response page only; runs are never merged across cursor
 * boundaries.
 */
@Getter
@Setter
@Schema(description = "Audit event entry with run-collapsing metadata. Wraps the standard AuditEventValueObject.")
public class CompactAuditEventValueObject {

    @JsonUnwrapped
    private final AuditEventValueObject event;

    @Schema(description = "Number of consecutive same-(eventType, performer) audit events folded into this entry. 1 for a solo event.")
    private int collapsedCount;

    @Schema(description = "Timestamp of the LAST event in the collapsed run. Equals `date` for a solo event.")
    private Date lastOccurrence;

    public CompactAuditEventValueObject( AuditEventValueObject event, int collapsedCount, Date lastOccurrence ) {
        this.event = event;
        this.collapsedCount = collapsedCount;
        this.lastOccurrence = lastOccurrence;
    }
}
