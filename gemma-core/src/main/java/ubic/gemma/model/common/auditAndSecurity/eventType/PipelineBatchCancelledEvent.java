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
import jakarta.persistence.DiscriminatorValue;

/**
 * Curator requested cancellation of a batch (or of one job mid-batch — the
 * latter still emits a batch-level event so the timeline reads as one stream).
 * Fired by {@code @Audited} on {@code cancelBatch} / {@code cancelJob}.
 */
@Entity
@DiscriminatorValue("PipelineBatchCancelledEvent")
public class PipelineBatchCancelledEvent extends PipelineBatchEvent {

}
