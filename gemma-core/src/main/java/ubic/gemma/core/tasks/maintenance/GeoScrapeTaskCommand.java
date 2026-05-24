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

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ubic.gemma.core.geoscrape.GeoScrapeService;
import ubic.gemma.core.job.Task;
import ubic.gemma.core.job.TaskCommand;

import java.util.Collection;
import java.util.Date;

/**
 * Task command for the GEO scrape & preboard pipeline. Submitted by
 * {@code POST /admin/tasks/geo-scrape}; runs
 * {@link GeoScrapeService#scrape(GeoScrapeService.ScrapeRequest)} on the
 * task-runner worker.
 *
 * @author phase 3 geo-scrape pipeline
 */
@Getter
@Setter
public class GeoScrapeTaskCommand extends TaskCommand {
    private static final long serialVersionUID = 1L;

    @Nullable
    private Date since;
    @Nullable
    private Date until;
    @Nullable
    private Integer maxRecords;
    @Nullable
    private Collection<String> criteria;
    private boolean dryRun;

    public GeoScrapeTaskCommand() {
        super();
    }

    @Override
    public Class<? extends Task<? extends TaskCommand>> getTaskClass() {
        return GeoScrapeTask.class;
    }
}
