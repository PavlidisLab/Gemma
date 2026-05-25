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
 * A batch crossed a major pipeline milestone (e.g. "alignment phase complete",
 * "all jobs entered QC stage"). The phase name + counters live in the
 * {@code AuditEvent.message} / {@code .detail} fields. Fired by the service
 * when aggregate child-job state crosses a threshold.
 */
public class PipelineBatchPhaseEvent extends PipelineBatchEvent {

}
