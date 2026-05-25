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
 * A pipeline job reached terminal failure on an experiment.
 *
 * <p>Written by {@code PipelineJobBatchService} to the experiment's audit trail
 * when a {@code PipelineJob} transitions to {@code FAILED} or {@code CANCELLED}.
 * Sibling of {@link FailedProcessedVectorComputationEvent} in spirit.</p>
 */
public class FailedPipelineRunEvent extends ExpressionExperimentAnalysisEvent {

}
