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
 * Sync state of a {@link Ticket}'s optional GitHub-issue mirror (Decision 7
 * of {@code AUDIT_AS_WORKFLOW_RECCE.md}). The actual sync is deferred — this
 * enum + its column are provisioned in the schema so that turning the
 * integration on later is purely additive.
 *
 * @author paul
 */
public enum ExternalIssueSyncState {
    /** Not mirrored externally. Default. */
    NONE,
    /** A sync is requested but hasn't completed. */
    PENDING,
    /** External issue is in agreement with the local ticket. */
    SYNCED,
    /** External issue and local ticket disagree (reconciliation needed). */
    DRIFTED
}
