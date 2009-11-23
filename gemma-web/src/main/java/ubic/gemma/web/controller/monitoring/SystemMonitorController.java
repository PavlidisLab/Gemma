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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ubic.gemma.grid.javaspaces.util.SpacesEnum;
import ubic.gemma.grid.javaspaces.util.SpacesUtil;
import ubic.gemma.util.monitor.CacheMonitor;
import ubic.gemma.util.monitor.HibernateMonitor;

/**
 * Provide statistics about the system: hibernate, caches etc.
 * 
 * @author paul
 * @version $Id$
 */
@Controller
public class SystemMonitorController {

    @Autowired
    HibernateMonitor hibernateMonitor;

    @Autowired
    CacheMonitor cacheMonitor;

    /**
     * Flush (clear) all caches. Expose to AJAX
     */
    public void flushAllCaches() {
        this.cacheMonitor.flushAllCaches();
    }

    @RequestMapping(value = "/admin/systemStats.html", method = RequestMethod.GET)
    public String show() {
        return "/admin/systemStats";
    }

    /**
     * Flush (clear) a cache.
     * 
     * @param cache name Expose to AJAX
     */
    public void flushCache( String name ) {
        this.cacheMonitor.flushCache( name );
    }

    public String getCacheStatus() {
        return cacheMonitor.getStats();
    }

    /**
     * Expose to AJAX
     * 
     * @return
     */
    public String getHibernateStatus() {
        return this.hibernateMonitor.getStats( true, true, true );
    }

    public String getSpaceStatus() {
        return SpacesUtil.logSpaceStatistics( SpacesEnum.DEFAULT_SPACE.getSpaceUrl() );
    }

    /**
     * @param cacheMonitor the cacheMonitor to set
     */
    public void setCacheMonitor( CacheMonitor cacheMonitor ) {
        this.cacheMonitor = cacheMonitor;
    }

    /**
     * @param hibernateMonitor the hibernateMonitor to set
     */
    public void setHibernateMonitor( HibernateMonitor hibernateMonitor ) {
        this.hibernateMonitor = hibernateMonitor;
    }
}
