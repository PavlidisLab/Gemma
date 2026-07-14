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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

/**
 * An incremental slice of a job's log, read via {@link PipelineScheduler#readLog} (§3.5). The
 * {@link #nextOffset} cursor lets a caller poll {@code tail -f}-style without re-fetching what it
 * already has; {@link #eof} is true once the slice reaches the end of the log as it stands now.
 *
 * <p>Logs are never persisted in Gemma — this is a pure proxy over the runtime's file. Wire shape is
 * snake_case for the curation UI (§1.3).</p>
 */
@Value
public class LogChunk {

    @JsonProperty("text")
    String text;

    /** Byte offset to pass as {@code offset} on the next read to continue where this slice ended. */
    @JsonProperty("next_offset")
    long nextOffset;

    /** True when this slice reached the current end of the log (no more bytes right now). */
    @JsonProperty("eof")
    boolean eof;
}
