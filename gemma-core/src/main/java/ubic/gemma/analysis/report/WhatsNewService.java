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
package ubic.gemma.analysis.report;

import java.util.Date;

import org.springframework.security.access.annotation.Secured;

/**
 * Creates reports that can be shown on the web pages or in social media feeds.
 * 
 * @author paul
 * @version $Id$
 */
public interface WhatsNewService {

    /**
     * save the report from last week.
     * 
     * @param date
     */
    @Secured({ "GROUP_AGENT" })
    public void generateWeeklyReport();

    /**
     * save the report from the date specified.
     * 
     * @param date
     */
    public void saveReport( Date date );

    /**
     * @param date
     * @return representing the updated or new objects.
     */
    public WhatsNew getReport( Date date );

    /**
     * Retrieve the latest WhatsNew report.
     * 
     * @return WhatsNew the latest WhatsNew report cache.
     */
    public WhatsNew retrieveReport();
}