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

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.core.lang.Nullable;

/**
 * Creates reports that can be shown on the web pages or in social media feeds.
 * <p>
 * Reports are always generated from an anonymous user's perspective.
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
     * Generate and save the report from last week. It can later be retrieved with {@link #getLatestWeeklyReport()}.
     */
    @Secured({ "GROUP_AGENT" })
    WhatsNew generateWeeklyReport();

    /**
     * Retrieve the latest weekly report if available.
     * @return the latest weekly report, or null if unavailable
     */
    @Nullable
    WhatsNew getLatestWeeklyReport();
}