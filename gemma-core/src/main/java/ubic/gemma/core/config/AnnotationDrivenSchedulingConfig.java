/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import ubic.gemma.core.context.EnvironmentProfiles;

/**
 * Turns on annotation-driven scheduling ({@code @Scheduled}) for real deployments.
 * <p>
 * This used to live on {@link SchedulerConfig}, which is gated to the {@code scheduler} profile
 * because it wires Quartz triggers. That coupled two unrelated things: Quartz job wiring, and
 * whether {@code @Scheduled} methods run at all. Production nodes run
 * {@code spring.profiles.active=production} without {@code scheduler}, so every {@code @Scheduled}
 * method in the codebase was silently dormant there — including the nightly search reindex, the
 * pipeline reconciler, submitted-task maintenance, the DEA warm-up, and the HomeStats daily
 * refresh. Several of those are written as though they run nightly (the HomeStats cron is set to
 * 4 AM specifically to follow the 3 AM reindex), so the intent was always that they fire.
 * <p>
 * Scoped to {@link EnvironmentProfiles#PRODUCTION} and {@link EnvironmentProfiles#SCHEDULER}
 * deliberately. It is NOT active under {@code test} — tests must not have background crons firing
 * mid-run — and not under {@code cli}, where a process runs one command and exits.
 * <p>
 * Individual jobs stay independently controllable through their own cron / interval / enabled
 * properties; this only decides whether the scheduling machinery exists at all.
 */
@Configuration
@Profile({ EnvironmentProfiles.PRODUCTION, EnvironmentProfiles.SCHEDULER })
@EnableScheduling
public class AnnotationDrivenSchedulingConfig {
}
