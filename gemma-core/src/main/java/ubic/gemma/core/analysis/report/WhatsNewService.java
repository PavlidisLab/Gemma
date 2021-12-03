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

import java.util.Date;

/**
 * Creates reports that can be shown on the web pages or in social media feeds.
 *
 * @author paul
 */
public interface WhatsNewService {

    /**
     * save the report from last week.
     */
    @Secured({ "GROUP_AGENT" })
    void generateWeeklyReport();

    /**
     * save the report from the date specified.
     *
     * @param date the date of the report
     */
    void saveReport( Date date );

    /**
     * @param date the date of the report
     * @return representing the updated or new objects.
     */
    WhatsNew getReport( Date date );

    /**
     * @return new or updated objects from within one week ago.
     */
    WhatsNew getWeeklyReport();

    /**
     * Retrieve the latest WhatsNew report.
     *
     * @return WhatsNew the latest WhatsNew report cache, or null if it hasn't been computed
     */
    WhatsNew getLatestWeeklyReport();
}