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
import org.springframework.lang.Nullable;
import ubic.gemma.core.job.Task;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.model.genome.Taxon;

/**
 * Task command for recomputing per-gene multifunctionality scores for a single taxon.
 * Port of {@code MultifunctionalityCli}'s taxon-scoped path.
 *
 * @author phase 3 admin-panel wiring
 */
@Getter
@Setter
public class MultifunctionalityTaskCommand extends TaskCommand {
    private static final long serialVersionUID = 1L;

    @Nullable
    private Taxon taxon;

    public MultifunctionalityTaskCommand( Taxon taxon ) {
        super();
        this.taxon = taxon;
    }

    @Override
    public Class<? extends Task<? extends TaskCommand>> getTaskClass() {
        return MultifunctionalityTask.class;
    }
}
