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
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletContext;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Locale;

/**
 * Provide statistics about the system: hibernate, caches etc.
 *
 * @author paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
@Controller
public class SystemMonitorController {

    @Autowired
    private CacheMonitor cacheMonitor;

    @Autowired
    private HibernateMonitor hibernateMonitor;

    @Autowired
    private ServletContext servletContext;

    /**
     * Flush (clear) all caches. Exposed to AJAX
     */
    public void clearAllCaches() {
        this.cacheMonitor.clearAllCaches();
    }

    /**
     * Flush (clear) a cache.
     *
     * @param name of cache Exposed to AJAX
     */
    public void clearCache( String name ) {
        this.cacheMonitor.clearCache( name );
    }

    /**
     * Exposed to AJAX
     */
    public void disableStatistics() {
        this.cacheMonitor.disableStatistics();
    }

    /**
     * Exposed to AJAX
     */
    public void enableStatistics() {
        this.cacheMonitor.enableStatistics();
    }

    public String getCacheStatus() {
        Locale locale = LocaleContextHolder.getLocale();
        return cacheMonitor.getStats( locale );
    }

    public String getHibernateStatus() {
        return this.hibernateMonitor.getStats( false, false, false ) + "<br/>" + this.getSystemStatus();
    }

    public void resetHibernateStatus() {
        this.hibernateMonitor.resetStats();
    }

    @RequestMapping(value = "/admin/systemStats.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public String show() {
        return "/admin/systemStats";
    }

    private String getSystemStatus() {
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        return String.format( "<p>System load average = %.2f</p>", operatingSystemMXBean.getSystemLoadAverage() );
    }
}
