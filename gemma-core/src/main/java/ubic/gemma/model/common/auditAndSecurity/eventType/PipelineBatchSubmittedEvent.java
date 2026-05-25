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
 * Curator submitted a new pipeline batch. Fired by {@code @Audited} on
 * {@code PipelineJobBatchService.submit}.
 */
public class PipelineBatchSubmittedEvent extends PipelineBatchEvent {

}
