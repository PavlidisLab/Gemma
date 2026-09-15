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
package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;

/**
 * Abstract base for audit events that live on a {@code PipelineJobBatch}'s
 * audit trail. Subtypes mark batch-level governance + milestones distinct
 * from per-job runtime telemetry ({@code PipelineJobEvent}).
 */
@Entity
public abstract class PipelineBatchEvent extends AuditEventType {

}
