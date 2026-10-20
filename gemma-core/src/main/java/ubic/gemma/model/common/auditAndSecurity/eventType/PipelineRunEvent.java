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
 * A pipeline job reached terminal success on an experiment.
 *
 * <p>Written by {@code PipelineJobBatchService} to the experiment's audit trail
 * when a {@code PipelineJob} transitions to {@code DONE}. The message/payload
 * carries the pipeline name + batch id + job id so downstream consumers can
 * reconstruct provenance.</p>
 *
 * <p>Distinct from {@code PipelineJobEvent} (per-job push telemetry) and from
 * {@code PipelineJobBatch.auditTrail} (batch-level governance). This event
 * lives on the EXPERIMENT and answers "what was last done to this experiment."</p>
 */
@Entity
@DiscriminatorValue("PipelineRunEvent")
public class PipelineRunEvent extends ExpressionExperimentAnalysisEvent {

}
