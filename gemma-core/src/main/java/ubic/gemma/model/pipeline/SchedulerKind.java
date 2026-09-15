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
package ubic.gemma.model.pipeline;

/**
 * External scheduler that owns the runtime of a {@link PipelineJob}.
 *
 * <p>Wire / DB representation is lowercase (matches the {@code CurationReview.kind}
 * discriminator convention). {@code @JsonValue} on the enum exposes the lowercase
 * form to clients.</p>
 */
public enum SchedulerKind {
    LUIGI,
    NEXTFLOW,
    /** In-JVM mock used for local-mode smoke testing. Never set by a real scheduler. */
    MOCK;

    @com.fasterxml.jackson.annotation.JsonValue
    public String wireValue() {
        return name().toLowerCase( java.util.Locale.ROOT );
    }
}
