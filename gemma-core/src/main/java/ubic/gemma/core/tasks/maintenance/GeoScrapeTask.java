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

import ubic.gemma.core.job.Task;

/**
 * @deprecated Supplanted by the curation agent's own GEO scraping, currently
 * {@code scrape_geo_and_open_triage.py}. The agent discovers candidates, decides what is worth
 * preboarding, and opens its own triage ticket, so Gemma no longer needs to run the scrape itself.
 * <p>
 * Nothing here is removed yet and it still works. What it does that the agent must therefore also do:
 * it creates the {@code PreboardedExperiment} rows, writes the {@code GeoScrapeWatermark} audit row,
 * and opens ONE {@code SCREENING} ticket per batch targeting that watermark -- deliberately not one
 * per candidate.
 * <p>
 * The preboarded write API the agent uses instead is {@code POST /preboarded} +
 * {@code POST /preboarded/{id}/annotation-sets}, with {@code GET /workflow/queue?datasetType=preboarded_experiment}
 * for the queue.
 */
@Deprecated
public interface GeoScrapeTask extends Task<GeoScrapeTaskCommand> {
}
