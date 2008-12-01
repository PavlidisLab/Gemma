/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.web.controller.monitoring;

import ubic.gemma.util.grid.javaspaces.SpacesEnum;
import ubic.gemma.util.grid.javaspaces.SpacesUtil;
import ubic.gemma.util.monitor.CacheMonitor;
import ubic.gemma.util.monitor.HibernateMonitor;

/**
 * @spring.bean id="hibernateMonitorController"
 * @spring.property name="hibernateMonitor" ref="hibernateMonitor"
 * @spring.property name="cacheMonitor" ref="cacheMonitor"
 * @author paul
 * @version $Id$
 */
public class HibernateMonitorController {

    HibernateMonitor hibernateMonitor;

    CacheMonitor cacheMonitor;

    /**
     * Expose to AJAX
     * 
     * @return
     */
    public String getHibernateStatus() {
        return this.hibernateMonitor.getStats( true, true, true );
    }

    /**
     * @param cacheMonitor the cacheMonitor to set
     */
    public void setCacheMonitor( CacheMonitor cacheMonitor ) {
        this.cacheMonitor = cacheMonitor;
    }

    public String getSpaceStatus() {
        return SpacesUtil.logSpaceStatistics( SpacesEnum.DEFAULT_SPACE.getSpaceUrl() );
    }

    public String getCacheStatus() {
        return cacheMonitor.getStats();
    }

    /**
     * @param hibernateMonitor the hibernateMonitor to set
     */
    public void setHibernateMonitor( HibernateMonitor hibernateMonitor ) {
        this.hibernateMonitor = hibernateMonitor;
    }

}
