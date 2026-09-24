/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.tasks.maintenance;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.core.job.Task;
import ubic.gemma.core.job.TaskCommand;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * Rewrite annotations using obsolete ontology terms to the successors their ontologies assert.
 *
 * @author phase 3 ontology maintenance
 */
@Getter
@Setter
public class ObsoleteTermCorrectionTaskCommand extends TaskCommand {
    private static final long serialVersionUID = 1L;

    private final Collection<String> uris;
    private final boolean dryRun;
    private final int timeoutSeconds;

    /**
     * Six hours. {@link TaskCommand#MAX_RUNTIME_MILLIS} defaults to 60 seconds and
     * {@code SubmittedTasksMaintenance} cancels anything that overruns — which this task would, comfortably. A
     * single-term dry run measured 27 s on the live corpus; a blanket run covers 105 terms and rewrites
     * annotations across ~9,200 experiments, resyncing EE2C and ANNOTATION_RELATION per experiment. Left at the
     * default it would be killed part-way, and a cancelled run reports nothing about what it had already written.
     */
    private static final long MAX_RUNTIME = 6 * 60 * 60 * 1000L;

    public ObsoleteTermCorrectionTaskCommand( Collection<String> uris, boolean dryRun, int timeoutSeconds ) {
        super();
        this.uris = uris != null ? new LinkedHashSet<>( uris ) : new LinkedHashSet<>();
        this.dryRun = dryRun;
        this.timeoutSeconds = timeoutSeconds;
        setMaxRuntimeMillis( MAX_RUNTIME );
    }

    @Override
    public Class<? extends Task<? extends TaskCommand>> getTaskClass() {
        return ObsoleteTermCorrectionTask.class;
    }
}
