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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.core.analysis.service.GeneMultifunctionalityPopulationService;
import ubic.gemma.core.job.AbstractTask;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.model.genome.Taxon;

/**
 * Async port of {@code MultifunctionalityCli}: recompute per-gene multifunctionality
 * scores for a single taxon (or all taxa, if {@code taxon == null}).
 *
 * @author phase 3 admin-panel wiring
 */
@Component
@Scope("prototype")
public class MultifunctionalityTaskImpl extends AbstractTask<MultifunctionalityTaskCommand>
        implements MultifunctionalityTask {

    private final Log log = LogFactory.getLog( MultifunctionalityTask.class.getName() );

    @Autowired
    private GeneMultifunctionalityPopulationService geneMultifunctionalityPopulationService;

    @Override
    public TaskResult call() {
        TaskResult result = newTaskResult( null );
        Taxon taxon = getTaskCommand().getTaxon();
        if ( taxon != null ) {
            log.info( "Recomputing multifunctionality for taxon " + taxon );
            geneMultifunctionalityPopulationService.updateMultifunctionality( taxon );
        } else {
            log.info( "Recomputing multifunctionality for all taxa" );
            geneMultifunctionalityPopulationService.updateMultifunctionality();
        }
        return result;
    }
}
