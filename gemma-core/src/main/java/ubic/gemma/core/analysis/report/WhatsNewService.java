/*
 * The Gemma_sec1 project
 *
 * Copyright (c) 2009 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.analysis.report;

import java.util.Date;

/**
 * Creates reports that can be shown on the web pages or in social media feeds.
 * <p>
 * Reports are always generated from an anonymous user's perspective, so the counts match
 * what a logged-out visitor can actually see.
 * <p>
 * <b>"New" means created in Gemma</b> — the {@code action='C'} row on the entity's audit
 * trail, i.e. when the dataset was first loaded. It is deliberately not "made public": the
 * {@code MakePublicEvent} / {@code DatasetPublishedEvent} types exist but have effectively
 * never fired (0 occurrences across a 200-dataset / ~5,000-event sample of prod on
 * 2026-08-21), so there is no publication date to report. Callers that surface these counts
 * to visitors should label them "added to Gemma" rather than "made public".
 * <p>
 * These reports are expensive enough that callers should cache them rather than compute per
 * request; {@link HomeStats} carries the corpus-wide counts in its daily snapshot.
 *
 * @author paul
 */
public interface WhatsNewService {

    /**
     * Generate the report from yesterday.
     * @return new or updated objects from within a day ago.
     */
    WhatsNew getDailyReport();

    /**
     * Generate the report from last week.
     * @return new or updated objects from within one week ago.
     */
    WhatsNew getWeeklyReport();

    /**
     * Generate the report covering everything that changed since a given date.
     * <p>
     * This is the full report: it loads the new and updated experiments and platforms, splits
     * them by taxon, and counts their biomaterials. Cost scales with the size of the window —
     * a year-wide window touches most of the corpus. When only the headline count is needed,
     * use {@link #countNewExpressionExperiments(Date)} instead.
     *
     * @param since start of the reporting window
     * @return new and updated objects since {@code since}, from an anonymous user's perspective
     */
    WhatsNew getReport( Date since );

    /**
     * Count the public experiments first created in Gemma since a given date.
     * <p>
     * The cheap counterpart to {@link #getReport(Date)}: it resolves ids through the audit
     * trail's creation events and applies the same anonymous-user ACL filter, without loading
     * the associated platforms, taxa or biomaterials.
     *
     * @param since start of the counting window
     * @return the number of experiments an anonymous visitor can see that were created on or
     *         after {@code since}
     */
    long countNewExpressionExperiments( Date since );
}
