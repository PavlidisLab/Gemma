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
package ubic.gemma.core.pipeline;

/**
 * Wraps any scheduler-side failure (network, auth, HTTP non-2xx, malformed
 * response, etc.). Checked so the service layer is forced to decide between
 * fail-the-job, retry, or surface-as-degraded.
 */
public class PipelineSchedulerException extends Exception {

    public PipelineSchedulerException( String message ) {
        super( message );
    }

    public PipelineSchedulerException( String message, Throwable cause ) {
        super( message, cause );
    }
}
