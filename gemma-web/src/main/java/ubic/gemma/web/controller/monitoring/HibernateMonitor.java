/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.web.controller.monitoring;

/**
 * @author paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
public interface HibernateMonitor {

    /**
     * @return Log some statistics.
     */
    String getStats();

    /**
     * Log some statistics. Parameters control the sections that are populated.
     *
     * @param showEntityStats             whether to show entity statistics
     * @param showCollectionStats         whether to show collection statistics
     * @param showSecondLevelCacheDetails whether to show level cache details
     * @return log
     */
    String getStats( boolean showEntityStats, boolean showCollectionStats, boolean showSecondLevelCacheDetails );

    /**
     * Clear all statistics.
     */
    void resetStats();

}