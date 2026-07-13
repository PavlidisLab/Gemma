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

import org.springframework.lang.Nullable;

import java.util.Map;

/**
 * Dev-only control surface for {@link ScriptedMockScheduler} — deliberately OFF the
 * production {@link PipelineScheduler} SPI so the Luigi/Nextflow impls never see it.
 *
 * <p>The only implementation is {@code @Profile("scheduler-mock")}; the REST resource
 * ({@code AdminPipelineMockWebService}) and integration tests depend on this interface
 * via {@code @Autowired(required = false)} and no-op / 404 when it is absent.</p>
 */
public interface MockSchedulerControl {

    /**
     * Step the deterministic virtual clock forward and fire every scripted stage that
     * has become due. Synchronous on the caller's thread — for {@code PUSH} scenarios
     * this drives {@code recordEvent}, whose audit / ACL writes need the caller's
     * security context, so it must never run on a background executor.
     *
     * @param ms virtual milliseconds to advance (>= 0)
     */
    void advance( long ms );

    /**
     * Register the scenario for an experiment. {@code experimentId == null} sets the
     * fallback applied to any experiment without a specific scenario.
     */
    void setScenario( @Nullable Long experimentId, Scenario scenario );

    /** Currently-registered per-experiment scenarios (excludes the fallback). */
    Map<Long, Scenario> listScenarios();

    /** Clear all scenarios, in-flight jobs, attempt counters, and reset the clock to 0. */
    void reset();
}
