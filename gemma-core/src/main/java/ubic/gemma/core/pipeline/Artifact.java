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

import lombok.Value;

/**
 * A whitelisted output file streamed from a job's workdir via {@link PipelineScheduler#readArtifact}
 * (§3.5) — e.g. Cell Ranger's {@code web_summary.html}, killing the {@code scp lisa:…} loop. The
 * REST layer streams {@link #content} raw with {@link #contentType}; never persisted.
 *
 * <p>{@code byte[]} content is adequate for the artifacts we serve today (small HTML/report files);
 * large-file streaming is a later refinement for the real Nextflow scheduler.</p>
 */
@Value
public class Artifact {
    String name;
    String contentType;
    byte[] content;
}
