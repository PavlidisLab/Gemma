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
package ubic.gemma.util.monitor;

/**
 * @author paul
 * @version $Id$
 */
public interface HibernateMonitor {

    /**
     * Log some statistics.
     */
    public abstract String getStats();

    /**
     * Log some statistics. Parameters control the section that are populated.
     * 
     * @param showEntityStats
     * @param showCollectionStats
     * @param showSecondLevelCacheDetails
     */
    public abstract String getStats( boolean showEntityStats, boolean showCollectionStats,
            boolean showSecondLevelCacheDetails );

    /**
     * Clear all statistics.
     */
    public abstract void resetStats();

}