/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.util.grid.javaspaces;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.ClassPathResource;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.util.SpringContextUtil;
import ubic.gemma.util.grid.javaspaces.entry.SpacesBusyEntry;
import ubic.gemma.util.grid.javaspaces.entry.SpacesCancellationEntry;
import ubic.gemma.util.grid.javaspaces.entry.SpacesGenericEntry;
import ubic.gemma.util.grid.javaspaces.entry.SpacesRegistrationEntry;

import com.j_spaces.core.IJSpace;
import com.j_spaces.core.admin.IJSpaceContainerAdmin;
import com.j_spaces.core.admin.StatisticsAdmin;
import com.j_spaces.core.client.FinderException;
import com.j_spaces.core.client.SpaceFinder;
import com.j_spaces.core.exception.StatisticsNotAvailable;
import com.j_spaces.core.filters.StatisticsContext;

/**
 * A utility class to test javaspaces features such as if the space is running, whether to add the gigaspaces beans to
 * the spring context, whether workers are available, etc. This class is {@link ApplicationContextAware} and therefore
 * knows about the context that creates it.
 * 
 * @author keshav
 * @version $Id$
 */
public class SpacesUtil implements ApplicationContextAware {

    /** The amount of time an entry will stay in the space (forever) */
    public static final int VERY_BIG_NUMBER_FOR_SOME_REASON = 600000000;

    /** The amount of time to wait for an entry before timing out */
    public static final int WAIT_TIMEOUT = 1000;

    private static final String GIGASPACES_TEMPLATE = "gigaspacesTemplate";

    private static Log log = LogFactory.getLog( SpacesUtil.class );

    private ApplicationContext applicationContext = null;

    /**
     * Determines if the {@link ApplicationContext} contains gigaspaces beans.
     */
    private boolean contextContainsGigaspaces() {
        return applicationContext.containsBean( GIGASPACES_TEMPLATE );
    }

    /**
     * Checks if space is running at specified url.
     * 
     * @param ctx
     * @return boolean
     */
    public static boolean isSpaceRunning( String url ) {
        if ( url == null ) return false;
        try {
            SpaceFinder.find( url );
            return true;
        } catch ( FinderException e ) {
            return false;
        }
    }

    /**
     * First checks if the space is running at url. If space is running, adds the gigaspaces beans to the context if
     * they do not exist. If the space is not running, returns the original context.
     * 
     * @param url
     * @return ApplicatonContext
     */
    public ApplicationContext addGemmaSpacesToApplicationContext( String url ) {
        if ( !isSpaceRunning( url ) ) {
            log.warn( "Cannot add Gigaspaces to application context. Space not started at " + url
                    + ". Returning context without gigaspaces beans." );
            return applicationContext;
        }

        if ( !contextContainsGigaspaces() ) {
            return SpringContextUtil.addResourceToContext( applicationContext, new ClassPathResource(
                    SpringContextUtil.GRID_SPRING_BEAN_CONFIG ) );
        }
        log.info( "Application context unchanged. Gigaspaces beans already exist." );

        return applicationContext;

    }

    /**
     * First checks if the space is running at the given url. If it is running, returns the {@link StatisticsAdmin},
     * which is useful for administration statistics. If the space is not running, returns null.
     * 
     * @param url
     */
    public static StatisticsAdmin getStatisticsAdmin( String url ) {
        if ( !isSpaceRunning( url ) ) {
            return null;
        }
        try {
            IJSpace space = ( IJSpace ) SpaceFinder.find( url );
            StatisticsAdmin admin = ( StatisticsAdmin ) space.getAdmin();
            return admin;
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * Logs the space statistics from the {@link StatisticsAdmin}.
     * 
     * @param url
     */
    @SuppressWarnings("unchecked")
    public static String logSpaceStatistics( String url ) {
        StatisticsAdmin admin = getStatisticsAdmin( url );

        if ( admin != null ) {
            try {
                if ( !admin.isStatisticsAvailable() ) {
                    return "Space is running but there are no statistics available";
                }
            } catch ( RemoteException e ) {
                return "Error while checking for statistics: " + e.getMessage();
            }

            StringBuilder buf = new StringBuilder();
            try {
                Map<Integer, StatisticsContext> statsMap = admin.getStatistics();
                Collection<Integer> keys = statsMap.keySet();
                Iterator<Integer> iter = keys.iterator();
                while ( iter.hasNext() ) {
                    StatisticsContext message = statsMap.get( iter.next() );
                    buf.append( message + "\n" );
                    log.debug( message );
                }
                if ( buf.length() == 0 ) {
                    return "No statistics!";
                }
                return buf.toString();
            } catch ( StatisticsNotAvailable e ) {
                throw new RuntimeException( e );
            } catch ( RemoteException e ) {
                throw new RuntimeException( e );
            }
        }
        return "Space not running";
    }

    /**
     * First checks to see if the space is running at the given url. If the space is running, returns the
     * {@link IJSpaceContainerAdmin}, which is useful to obtain space information such as the runtime configuration
     * report. If the space is not running, returns null.
     * 
     * @param url
     * @return {@link IJSpaceContainerAdmin}
     */
    public static IJSpaceContainerAdmin getContainerSpaceAdmin( String url ) {
        if ( !isSpaceRunning( url ) ) {
            log.error( "Space not started at " + url + ". Cannot get container admin." );
            return null;
        }
        try {
            IJSpace space = ( IJSpace ) SpaceFinder.find( url );
            IJSpaceContainerAdmin admin = ( IJSpaceContainerAdmin ) space.getContainer();
            return admin;
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Logs the runtime configuration report. This report contains information about the space, including the system
     * environment configuration.
     */
    public static void logRuntimeConfigurationReport() {
        IJSpaceContainerAdmin admin = getContainerSpaceAdmin( SpacesEnum.DEFAULT_SPACE.getSpaceUrl() );

        if ( admin != null ) {
            try {
                log.info( "Runtime configuration report: " + admin.getRuntimeConfigReport() );
            } catch ( RemoteException e ) {
                e.printStackTrace();
            }
        }

        log.error( "Runtime configuration report unavailable." );
    }

    /**
     * Returns the number of idle workers.
     * 
     * @param url
     * @return int
     */
    public int numIdleWorkers( String url ) {
        int count = 0;

        if ( !isSpaceRunning( url ) ) {
            log.error( "Space not started at " + url + ". Returning a count of 0 (idle workers)." );
            return count;
        }

        try {
            IJSpace space = ( IJSpace ) SpaceFinder.find( url );
            count = space.count( new SpacesRegistrationEntry(), null );
            log.debug( "Number of idle workers: " + count );
        } catch ( Exception e ) {
            log.error( "Could not check for idle workers.  Assuming 0 workers are idle." );
            e.printStackTrace();
            return 0;
        }

        return count;
    }

    /**
     * Returns the number of busy workers.
     * 
     * @param url
     * @return
     */
    public int numBusyWorkers( String url ) {
        int count = 0;

        if ( !isSpaceRunning( url ) ) {
            log.error( "Space not started at " + url + ". Returning a count of 0 (busy workers)." );
            return count;
        }

        try {
            IJSpace space = ( IJSpace ) SpaceFinder.find( url );
            count = space.count( new SpacesBusyEntry(), null );
            log.debug( "Number of busy workers: " + count );
        } catch ( Exception e ) {
            log.error( "Could not check for busy workers.  Assuming 0 workers are busy." );
            e.printStackTrace();
            return 0;
        }

        return count;
    }

    /**
     * Returns a list of all the registered workers.
     * 
     * @param url
     * @return List<SpacesGenericEntry>
     */
    public List<SpacesRegistrationEntry> getRegisteredWorkers( String url ) {

        List<SpacesRegistrationEntry> workerEntries = null;
        if ( !isSpaceRunning( url ) ) {
            log.error( "Space not started at " + url + ". Returning a count of 0 (workers registered)." );
            return null;
        }

        try {
            IJSpace space = ( IJSpace ) SpaceFinder.find( url );

            Object[] commandObjects = space.readMultiple( new SpacesRegistrationEntry(), null, 120000 );

            workerEntries = new ArrayList<SpacesRegistrationEntry>();
            for ( int i = 0; i < commandObjects.length; i++ ) {
                SpacesRegistrationEntry entry = ( SpacesRegistrationEntry ) commandObjects[i];
                workerEntries.add( entry );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            return null;
        }
        return workerEntries;
    }

    public List<SpacesBusyEntry> getBusyWorkers( String url ) {

        List<SpacesBusyEntry> workerEntries = null;
        if ( !isSpaceRunning( url ) ) {
            log.error( "Space not started at " + url + ". Returning a count of 0 (workers registered)." );
            return null;
        }

        try {
            IJSpace space = ( IJSpace ) SpaceFinder.find( url );

            Object[] commandObjects = space.readMultiple( new SpacesBusyEntry(), null, 120000 );

            workerEntries = new ArrayList<SpacesBusyEntry>();
            for ( int i = 0; i < commandObjects.length; i++ ) {
                SpacesBusyEntry entry = ( SpacesBusyEntry ) commandObjects[i];
                workerEntries.add( entry );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            return null;
        }
        return workerEntries;
    }

    /**
     * Returns true there exist idle workers. A worker with a {@link SpacesRegistrationEntry} in the space is considered
     * idle (when workers are busy, they take the {@link SpacesRegistrationEntry} from the space and write a
     * {@link SpacesBusyEntry} in the space).
     * 
     * @param url
     * @return boolean
     */
    public boolean areWorkersIdle( String url ) {
        boolean registered = false;

        if ( numIdleWorkers( url ) > 0 ) {
            registered = true;
        }

        return registered;

    }

    /**
     * Returns true if there are workers registered with the space but busy.
     * 
     * @param url
     * @return
     */
    public boolean areWorkersBusy( String url ) {
        boolean registered = false;

        if ( numBusyWorkers( url ) > 0 ) {
            registered = true;
        }
        return registered;
    }

    /**
     * Returns the list of tasks that can currently be serviced at the space url based on the currently registered
     * workers.
     * 
     * @param url
     * @return List <String>
     */
    public List<String> tasksThatCanBeServiced( String url ) {

        List<String> taskNames = new ArrayList<String>();

        if ( !this.areWorkersIdle( url ) ) {
            log.info( "No idle workers registered with space at " + url + ".  No tasks can be serviced right now." );
        }

        else {
            List<SpacesRegistrationEntry> workerEntries = this.getRegisteredWorkers( url );
            for ( SpacesGenericEntry entry : workerEntries ) {
                String taskName = entry.getMessage();
                log.debug( "Can service task " + taskName + " now." );
                taskNames.add( taskName );
            }
        }

        return taskNames;
    }

    /**
     * Returns a list of tasks that can be serviced later (are currently busy).
     * 
     * @param url
     * @return
     */
    public List<String> tasksThatCanBeServicedLater( String url ) {
        List<String> taskNames = new ArrayList<String>();

        if ( !this.areWorkersBusy( url ) ) {
            log.info( "No busy entries in the space at " + url + " (so no tasks will be queued)." );
        }

        List<SpacesBusyEntry> busyEntries = this.getBusyWorkers( url );
        for ( SpacesBusyEntry entry : busyEntries ) {
            String taskName = entry.getMessage();
            log.debug( "Can service task " + taskName + " later." );
            taskNames.add( taskName );
        }
        return taskNames;
    }

    /**
     * Returns true if the task can be serviced by the space at the given url. The task may be serviced now or later,
     * depending on whether or not it is busy.
     * 
     * @param taskName The name of the task to be serviced.
     * @param url The space url.
     * @return boolean
     */
    public boolean canServiceTask( String taskName, String url ) {
        boolean serviceable = false;

        List<String> serviceableTasks = this.tasksThatCanBeServiced( url );

        List<String> busyTasks = this.tasksThatCanBeServicedLater( url );

        if ( serviceableTasks.contains( taskName ) ) {
            serviceable = true;
            log.info( "Can service task with name " + taskName );
        } else if ( busyTasks.contains( taskName ) ) {
            log.info( "Cannot service task with name " + taskName
                    + " at this time but can service later.  Task will be queued." );
            serviceable = true;
        } else {
            log.warn( "Cannot service task " + taskName + " at this time." );
        }

        return serviceable;
    }

    /**
     * Cancels the task.
     * 
     * @param taskId
     */
    public void cancel( Object taskId ) {

        if ( !isSpaceRunning( SpacesEnum.DEFAULT_SPACE.getSpaceUrl() ) ) {
            return;
        }

        ApplicationContext updatedContext = addGemmaSpacesToApplicationContext( SpacesEnum.DEFAULT_SPACE.getSpaceUrl() );

        if ( !updatedContext.containsBean( GIGASPACES_TEMPLATE ) ) {
            log.warn( "Cannot cancel space task because the space is not running. This might be benign." );
            return;
        }

        GigaSpacesTemplate template = ( GigaSpacesTemplate ) updatedContext.getBean( GIGASPACES_TEMPLATE );

        IJSpace space = ( IJSpace ) template.getSpace();

        SpacesCancellationEntry cancellationEntry = new SpacesCancellationEntry();
        cancellationEntry.taskId = taskId;
        try {
            space.write( cancellationEntry, null, VERY_BIG_NUMBER_FOR_SOME_REASON );
        } catch ( Exception e ) {
            throw new RuntimeException( "Cannot cancel task " + taskId + ".  Exception is: " + e );
        }
    }

    /*
     * (non-Javadoc)
     * @seeorg.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.
     * ApplicationContext)
     */
    public void setApplicationContext( ApplicationContext applicationContext ) throws BeansException {
        this.applicationContext = applicationContext;

    }

}
