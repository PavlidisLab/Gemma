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
package ubic.gemma.persistence.service.pipeline;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-pipeline dispatch defaults (handoff D2 / resolutions O8+R12). Today the only default is the
 * batch {@code maxConcurrent} cap, which reconciles the built per-batch throttle (§3.4) with D2's
 * "per-pipeline-kind cap": when a caller submits without an explicit cap, the service stamps the
 * pipeline's default onto the batch so it never launches unbounded.
 *
 * <p>Keyed by pipeline name. {@code sc-annotation} defaults to <b>25</b> — chosen to mirror the
 * pipeline's {@code queueSize=25}, though at this layer it caps <em>concurrent runs</em> (= concurrent
 * nextflow head jobs, R11/R13), not tasks within a run. Override via
 * {@code gemma.pipeline.sc-annotation.maxConcurrent}. Unknown pipelines return {@code null} (= no
 * default → unlimited, the pre-existing behaviour), so non-sc-annotation callers are unaffected.</p>
 *
 * <p>The global cross-batch ceiling D2 also mentions is deliberately deferred (needs cluster-admin
 * numbers); Slurm's own per-user submit limits are the backstop until then.</p>
 */
@Component
public class PipelineDefaults {

    /** Canonical pipeline name for the single-cell annotation pipeline (task 7). */
    public static final String SC_ANNOTATION = "sc-annotation";

    private final Map<String, Integer> maxConcurrentByPipeline;

    @Autowired
    public PipelineDefaults(
            @Value("${gemma.pipeline.sc-annotation.maxConcurrent:25}") int scAnnotationMaxConcurrent ) {
        Map<String, Integer> m = new HashMap<>();
        m.put( SC_ANNOTATION, scAnnotationMaxConcurrent );
        this.maxConcurrentByPipeline = Map.copyOf( m );
    }

    /**
     * Default concurrent-run cap for a pipeline, or {@code null} if it has none (⇒ unlimited). Applied
     * by {@code submit} only when the caller didn't pass an explicit {@code maxConcurrent}.
     */
    @Nullable
    public Integer maxConcurrentFor( @Nullable String pipeline ) {
        return pipeline == null ? null : maxConcurrentByPipeline.get( pipeline );
    }
}
