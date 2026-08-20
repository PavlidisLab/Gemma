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

    public ObsoleteTermCorrectionTaskCommand( Collection<String> uris, boolean dryRun, int timeoutSeconds ) {
        super();
        this.uris = uris != null ? new LinkedHashSet<>( uris ) : new LinkedHashSet<>();
        this.dryRun = dryRun;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public Class<? extends Task<? extends TaskCommand>> getTaskClass() {
        return ObsoleteTermCorrectionTask.class;
    }
}
