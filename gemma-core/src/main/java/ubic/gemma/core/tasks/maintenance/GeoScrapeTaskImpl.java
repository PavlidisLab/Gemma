/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.tasks.maintenance;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.core.geoscrape.GeoScrapeService;
import ubic.gemma.core.job.AbstractTask;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.model.expression.experiment.GeoScrapeWatermark;

/**
 * Async runner for the GEO scrape & preboard pipeline. Submitted by
 * {@code POST /admin/tasks/geo-scrape}.
 *
 * @author phase 3 geo-scrape pipeline
 */
@Component
@Scope("prototype")
public class GeoScrapeTaskImpl extends AbstractTask<GeoScrapeTaskCommand>
        implements GeoScrapeTask {

    private final Log log = LogFactory.getLog( GeoScrapeTask.class.getName() );

    @Autowired
    private GeoScrapeService geoScrapeService;

    @Override
    public TaskResult call() {
        GeoScrapeTaskCommand cmd = getTaskCommand();
        GeoScrapeService.ScrapeRequest req = new GeoScrapeService.ScrapeRequest();
        req.setSince( cmd.getSince() );
        req.setUntil( cmd.getUntil() );
        req.setMaxRecords( cmd.getMaxRecords() );
        req.setCriteria( cmd.getCriteria() );
        req.setDryRun( cmd.isDryRun() );
        log.info( "Starting GEO scrape: since=" + cmd.getSince()
                + " until=" + cmd.getUntil()
                + " maxRecords=" + cmd.getMaxRecords()
                + " criteria=" + cmd.getCriteria()
                + " dryRun=" + cmd.isDryRun() );
        GeoScrapeWatermark wm = geoScrapeService.scrape( req );
        log.info( "GEO scrape complete: status=" + wm.getStatus()
                + " scanned=" + wm.getRecordsScanned()
                + " matched=" + wm.getRecordsMatched() );
        return newTaskResult( wm );
    }
}
