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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.core.job.AbstractTask;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.ontology.ObsoleteTermCorrectionResult;
import ubic.gemma.core.ontology.ObsoleteTermCorrectionService;

import java.util.concurrent.TimeUnit;

/**
 * Async wrapper for {@link ObsoleteTermCorrectionService#apply}. Async because a live run rewrites annotations
 * across thousands of experiments and resyncs the denormalizations for each; a dry run is quick but goes through
 * the same path so the rehearsal and the real thing cannot diverge.
 *
 * @author phase 3 ontology maintenance
 */
@Component
@Scope("prototype")
public class ObsoleteTermCorrectionTaskImpl extends AbstractTask<ObsoleteTermCorrectionTaskCommand>
        implements ObsoleteTermCorrectionTask {

    private static final Logger log = LoggerFactory.getLogger( ObsoleteTermCorrectionTaskImpl.class );

    @Autowired
    private ObsoleteTermCorrectionService obsoleteTermCorrectionService;

    @Override
    public TaskResult call() throws Exception {
        ObsoleteTermCorrectionTaskCommand cmd = getTaskCommand();
        log.info( "Starting obsolete-term correction (dryRun={}, uris={}).", cmd.isDryRun(),
                cmd.getUris().isEmpty() ? "ALL auto-correctable" : cmd.getUris() );
        ObsoleteTermCorrectionResult result = obsoleteTermCorrectionService
                .apply( cmd.getUris(), cmd.isDryRun(), cmd.getTimeoutSeconds(), TimeUnit.SECONDS );
        return newTaskResult( result );
    }
}
