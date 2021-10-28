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

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.core.job.grid.util.JMSBrokerMonitor;
import ubic.gemma.persistence.util.Settings;
import ubic.gemma.persistence.util.monitor.HibernateMonitor;
import ubic.gemma.web.util.CacheMonitor;

import javax.jms.JMSException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

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
    private JMSBrokerMonitor jmsBrokerMonitor;

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
        return cacheMonitor.getStats();
    }

    public String getHibernateStatus() {
        return this.hibernateMonitor.getStats( false, false, false ) + "<br/>" + this.getSystemStatus();
    }

    public String getJMSBrokerStatus() {
        StringBuilder buf = new StringBuilder();

        buf.append( "<p>" );
        buf.append( "JMS Broker is " );
        buf.append( jmsBrokerMonitor.isRemoteTasksEnabled() ? "enabled" : "disabled" );
        buf.append( " in the configuration file." );
        buf.append( "</p>" );
        if ( jmsBrokerMonitor.isRemoteTasksEnabled() ) {
            buf.append( "<p>" );
            buf.append( "Number of worker applications running: " );
            try {
                int numWorkers = jmsBrokerMonitor.getNumberOfWorkerHosts();
                if ( numWorkers == 0 ) {
                    buf.append( "<img src=\"" ).append( Settings.getRootContext() )
                            .append( "/images/icons/exclamation.png\" /> No workers found!" );
                } else {
                    buf.append( "(" ).append( numWorkers ).append( ")" );
                    for ( int i = 0; i < numWorkers; i++ ) {
                        buf.append( "<img src=\"" ).append( Settings.getRootContext() )
                                .append( "/images/icons/server.png\" />" );
                    }
                }
            } catch ( JMSException e ) {
                buf.append( "Got exception: " ).append( e.getMessage() );
            }
            buf.append( "</p>" );
        }
        return buf.toString();
    }

    /**
     * Used for external monitoring (e.g. Nagios)
     *
     * @param  request  request
     * @param  response response
     * @return          model and view
     */
    @RequestMapping("/gridStatus.html")
    public ModelAndView gridStatus( HttpServletRequest request, HttpServletResponse response ) {
        String brokerStatus = this.getJMSBrokerStatus();
        return new ModelAndView( "systemNotices" ).addObject( "status", brokerStatus );
    }

    public void resetHibernateStatus() {
        this.hibernateMonitor.resetStats();
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

    @RequestMapping(value = "/admin/systemStats.html", method = RequestMethod.GET)
    public String show() {
        return "/admin/systemStats";
    }

    private String getSystemStatus() {
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        return String.format( "<p>System load average = %.2f</p>", operatingSystemMXBean.getSystemLoadAverage() );
    }
}
