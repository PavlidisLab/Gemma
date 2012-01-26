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
package ubic.gemma.job.grid.util;

import java.util.List;

import net.jini.core.lease.Lease;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.job.grid.worker.SpacesRegistrationEntry;

/**
 * @author paul
 * @version $Id$
 */
public interface SpacesUtil extends ApplicationContextAware {

    /** The amount of time an entry will stay in the space (forever) */
    public static final long ENTRY_TTL = Lease.FOREVER;
    /** The amount of time to wait for an entry before timing out */
    public static final int WAIT_TIMEOUT = 1000;

    /**
     * If space is running, adds the gigaspaces beans to the context if they do not exist. If the space is not running,
     * returns the original context.
     * 
     * @return ApplicatonContext
     */
    public abstract ApplicationContext addGemmaSpacesToApplicationContext();

    /**
     * Refresh the gigaspaces configuration. This should be done if the space is restarted. If the space is not running,
     * this doesn't do anything.
     * 
     * @return
     */
    public abstract void forceRefreshSpaceBeans();

    /**
     * Attempt to cancel a task.
     * 
     * @param taskId
     * @return true if the job was confirmed as cancelled or at least not running on the grid, false otherwise
     */
    public abstract boolean cancel( String taskId );

    /**
     * Returns true if the task can be serviced by the space at the given url. The task may be serviced now or later,
     * depending on whether or not it is busy.
     * 
     * @param taskName The name of the task to be serviced.
     * @return boolean
     */
    public abstract boolean canServiceTask( String taskName );

    /**
     * Can service the task with taskName later.
     * 
     * @param taskName
     * @return
     */
    public abstract boolean canServiceTaskLater( String taskName );

    /**
     * Can service the task with taskName now.
     * 
     * @param taskName
     * @return
     */
    public abstract boolean canServiceTaskNow( String taskName );

    /**
     * @return
     */
    public abstract List<SpacesRegistrationEntry> getBusyWorkers();

    /**
     * @return template or null if space is not running.
     */
    public abstract GigaSpacesTemplate getGigaspacesTemplate();

    /**
     * @return proxy of interface for a task
     */
    public abstract Object getProxy();

    /**
     * Returns the number of busy workers.
     * 
     * @return
     */
    public abstract int numBusyWorkers();

    /**
     * Returns the number of idle workers.
     * 
     * @return int
     */
    public abstract int numIdleWorkers();

    /**
     * Returns the list of tasks that can currently be serviced at the space url based on the currently registered
     * workers.
     * 
     * @return List <String>
     */
    public abstract List<String> tasksThatCanBeServiced();

    /**
     * Returns a list of tasks that can be serviced later (are currently busy).
     * 
     * @return
     */
    public abstract List<String> tasksThatCanBeServicedLater();

}