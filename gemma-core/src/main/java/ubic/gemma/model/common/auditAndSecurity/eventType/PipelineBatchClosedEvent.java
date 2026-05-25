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

/**
 * All child jobs in a batch reached a terminal state and the batch moved to
 * {@code CLOSED}. Fired by the service when the last non-terminal job
 * transitions. The {@code AuditEvent.detail} carries the done/failed/cancelled
 * counts.
 */
public class PipelineBatchClosedEvent extends PipelineBatchEvent {

}
