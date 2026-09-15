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
 * All child jobs in a batch reached a terminal state and the batch moved to
 * {@code CLOSED}. Fired by the service when the last non-terminal job
 * transitions. The {@code AuditEvent.detail} carries the done/failed/cancelled
 * counts.
 */
@Entity
@DiscriminatorValue("PipelineBatchClosedEvent")
public class PipelineBatchClosedEvent extends PipelineBatchEvent {

}
