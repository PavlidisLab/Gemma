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
package ubic.gemma.job.grid.util;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.jini.core.entry.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.job.grid.worker.SpacesRegistrationEntry;
import ubic.gemma.util.SpringContextUtil;

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
@Component
public class SpacesUtilImpl implements SpacesUtil {

    private static final String GIGASPACES_TEMPLATE = "gigaspacesTemplate";

    private static Log log = LogFactory.getLog( SpacesUtilImpl.class );

    /**
     * First checks to see if the space. If the space is running, returns the {@link IJSpaceContainerAdmin}, which is
     * useful to obtain space information such as the runtime configuration report. If the space is not running, returns
     * null.
     * 
     * @return {@link IJSpaceContainerAdmin}
     */
    public static IJSpaceContainerAdmin getContainerSpaceAdmin() {
        if ( !isSpaceRunning() ) {
            return null;
        }
        try {
            IJSpace space = getSpace();
            IJSpaceContainerAdmin admin = ( IJSpaceContainerAdmin ) space.getContainer();
            return admin;
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * First checks if the space is running. If it is running, returns the {@link StatisticsAdmin}, which is useful for
     * administration statistics. If the space is not running, returns null.
     */
    public static StatisticsAdmin getStatisticsAdmin() {
        if ( !isSpaceRunning() ) {
            return null;
        }
        try {
            IJSpace space = getSpace();
            StatisticsAdmin admin = ( StatisticsAdmin ) space.getAdmin();

            return admin;
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * Checks if space is running
     * 
     * @return boolean
     */
    public static boolean isSpaceRunning() {

        try {
            IJSpace space = ( IJSpace ) SpaceFinder.find( SpacesEnum.DEFAULT_SPACE.getSpaceUrl() );
            space.ping();
            return true;
        } catch ( FinderException e ) {
            log.debug( "No space" );
            return false;
        } catch ( RemoteException e ) {
            log.debug( "no ping" );
            return false;
        }
    }

    /**
     * Logs the runtime configuration report. This report contains information about the space, including the system
     * environment configuration.
     */
    public static void logRuntimeConfigurationReport() {
        IJSpaceContainerAdmin admin = getContainerSpaceAdmin();

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
     * Logs the space statistics from the {@link StatisticsAdmin}.
     */
    public static String logSpaceStatistics() {

        if ( !isSpaceRunning() ) {
            return "Space not running";
        }

        StatisticsAdmin admin = getStatisticsAdmin();

        try {
            if ( !admin.isStatisticsAvailable() ) {
                return "Space is running but there are no statistics available";
            }
        } catch ( RemoteException e ) {
            return "Error while checking for statistics: " + e.getMessage();
        }

        try {
            Map<Integer, StatisticsContext> statsMap = admin.getStatistics();

            if ( statsMap.isEmpty() ) {
                return "No statistics!";
            }

            StringBuilder buf = new StringBuilder();
            for ( Integer key : statsMap.keySet() ) {
                StatisticsContext message = statsMap.get( key );
                buf.append( message + "\n" );
                log.debug( message );
            }
            return buf.toString();
        } catch ( StatisticsNotAvailable e ) {
            throw new RuntimeException( e );
        } catch ( RemoteException e ) {
            throw new RuntimeException( e );
        }

    }

    private static IJSpace getSpace() throws FinderException {
        return ( IJSpace ) SpaceFinder.find( SpacesEnum.DEFAULT_SPACE.getSpaceUrl() );
    }

    private ApplicationContext applicationContext = null;

    /* (non-Javadoc)
     * @see ubic.gemma.job.grid.util.SpacesUtil#addGemmaSpacesToApplicationContext()
     */
    @Override
    public ApplicationContext addGemmaSpacesToApplicationContext() {

        if ( this.applicationContext == null ) {
            throw new IllegalStateException( "Context is null. Service not correctly initialized" );
        }

        if ( !isSpaceRunning() ) {
            return this.applicationContext;
        }

        if ( !contextContainsGigaspaces() ) {
            forceRefreshSpaceBeans();
        }

        return this.applicationContext;

    }

    /* (non-Javadoc)
     * @see ubic.gemma.job.grid.util.SpacesUtil#forceRefreshSpaceBeans()
     */
    @Override
    public void forceRefreshSpaceBeans() {

        if ( !isSpaceRunning() ) {
            return;
        }

        try {
            this.applicationContext = SpringContextUtil.addResourceToContext( applicationContext,
                    new ClassPathResource( SpringContextUtil.GRID_SPRING_BEAN_CONFIG ) );
        } catch ( Exception e ) {
            log.error( e, e );
        }
    }

    /* (non-Javadoc)
     * @see ubic.gemma.job.grid.util.SpacesUtil#cancel(java.lang.String)
     */
    @Override
    public boolean cancel( String taskId ) {

        if ( !isSpaceRunning() ) {
            return false;
        }

        if ( !taskIsRunningOnGrid( taskId ) ) {
            log.debug( "Task  " + taskId + " is no longer (or perhaps was never) running on the grid." );
            return true;
        }

        GigaSpacesTemplate template = getGigaspacesTemplate();

        IJSpace space = ( IJSpace ) template.getSpace();
        try {
            for ( SpacesRegistrationEntry e : getRegisteredWorkers() ) {
                SpacesCancellationEntry cancellationEntry = new SpacesCancellationEntry();
                cancellationEntry.registrationId = e.registrationId;
                cancellationEntry.taskId = taskId;
                space.write( cancellationEntry, null, ENTRY_TTL );
            }

            // wait for confirmation - the task should be gone from the reg entry.
            final int CANCEL_CHECK_TRIES = 6;
            final int MILLIS_BETWEEN_CANCEL_CHECKS = 200;
            for ( int i = 0; i < CANCEL_CHECK_TRIES; i++ ) {
                Thread.sleep( MILLIS_BETWEEN_CANCEL_CHECKS * i );

                boolean stillRunning = false;
                List<SpacesRegistrationEntry> busyWorkers = this.getBusyWorkers();
                for ( SpacesRegistrationEntry e : busyWorkers ) {
                    if ( e.taskId.equals( taskId ) ) {
                        stillRunning = true;
                    }
                }

                if ( !stillRunning ) {
                    log.debug( "Task " + taskId + " was successfullly cancelled" );
                    return true;
                }
            }

            return false;

        } catch ( Exception e ) {
            log.error( "Error when attempting to cancel task " + taskId, e );
            return false;
        }

    }

    /* (non-Javadoc)
     * @see ubic.gemma.job.grid.util.SpacesUtil#canServiceTask(java.lang.String)
     */
    @Override
    public boolean canServiceTask( String taskName ) {
        if ( canServiceTaskNow( taskName ) ) {
            log.debug( "Can service " + taskName );
            return true;
        } else if ( canServiceTaskLater( taskName ) ) {
            log.debug( "Cannot service " + taskName + " at this time but can service later.  Task will be queued." );
            return false;
        } else {
            log.debug( "Cannot service " + taskName + " at this time." );
            return false;
        }
    }

    /* (non-Javadoc)
     * @see ubic.gemma.job.grid.util.SpacesUtil#canServiceTaskLater(java.lang.String)
     */
    @Override
    public boolean canServiceTaskLater( String taskName ) {
        boolean serviceable = false;

        List<String> busyTasks = this.tasksThatCanBeServicedLater();

        if ( busyTasks.contains( taskName ) ) {
            serviceable = true;
        }

        return serviceable;
    }

    /* (non-Javadoc)
     * @see ubic.gemma.job.grid.util.SpacesUtil#canServiceTaskNow(java.lang.String)
     */
    @Override
    public boolean canServiceTaskNow( String taskName ) {

        boolean serviceable = false;

        List<String> serviceableTasks = this.tasksThatCanBeServiced();

        if ( serviceableTasks.contains( taskName ) ) {
            serviceable = true;
        }

        return serviceable;
    }

    /* (non-Javadoc)
     * @see ubic.gemma.job.grid.util.SpacesUtil#getBusyWorkers()
     */
    @Override
    public List<SpacesRegistrationEntry> getBusyWorkers() {

        List<SpacesRegistrationEntry> workerEntries = new ArrayList<SpacesRegistrationEntry>();
        if ( !isSpaceRunning() ) {
            return workerEntries;
        }

        try {
            IJSpace space = getSpace();

            Entry[] commandObjects = space
                    .readMultiple( new SpacesRegistrationEntry(), null, 120000 /* magic number */);

            workerEntries = new ArrayList<SpacesRegistrationEntry>();
            for ( int i = 0; i < commandObjects.length; i++ ) {
                SpacesRegistrationEntry entry = ( SpacesRegistrationEntry ) commandObjects[i];
                if ( entry.taskId != null ) {
                    workerEntries.add( entry );
                }
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return workerEntries;
    }

    /* (non-Javadoc)
     * @see ubic.gemma.job.grid.util.SpacesUtil#getGigaspacesTemplate()
     */
    @Override
    public GigaSpacesTemplate getGigaspacesTemplate() {
        if ( !isSpaceRunning() ) {
            return null;
        }
        addGemmaSpacesToApplicationContext();
        return ( GigaSpacesTemplate ) applicationContext.getBean( GIGASPACES_TEMPLATE );
    }

    /* (non-Javadoc)
     * @see ubic.gemma.job.grid.util.SpacesUtil#getProxy()
     */
    @Override
    public Object getProxy() {
        addGemmaSpacesToApplicationContext();
        return applicationContext.getBean( "javaspaceProxyInterfaceFactory" );
    }

    /**
     * Returns a list of all the workers that have registered themselves with the grid.
     * 
     * @return List<SpacesGenericEntry>
     */
    public static List<SpacesRegistrationEntry> getRegisteredWorkers() {

        List<SpacesRegistrationEntry> workerEntries = new ArrayList<SpacesRegistrationEntry>();
        if ( !isSpaceRunning() ) {
            return workerEntries;
        }

        try {
            IJSpace space = getSpace();

            Object[] commandObjects = space
                    .readMultiple( new SpacesRegistrationEntry(), null, 120000 /* magic number */);

            for ( int i = 0; i < commandObjects.length; i++ ) {
                SpacesRegistrationEntry entry = ( SpacesRegistrationEntry ) commandObjects[i];
                workerEntries.add( entry );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return workerEntries;
    }

    /* (non-Javadoc)
     * @see ubic.gemma.job.grid.util.SpacesUtil#numBusyWorkers()
     */
    @Override
    public int numBusyWorkers() {
        return this.getBusyWorkers().size();
    }

    /* (non-Javadoc)
     * @see ubic.gemma.job.grid.util.SpacesUtil#numIdleWorkers()
     */
    @Override
    public int numIdleWorkers() {
        return getRegisteredWorkers().size() - this.getBusyWorkers().size();
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.
     * ApplicationContext)
     */
    /* (non-Javadoc)
     * @see ubic.gemma.job.grid.util.SpacesUtil#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    @Override
    public void setApplicationContext( ApplicationContext applicationContext ) throws BeansException {
        this.applicationContext = applicationContext;

    }

    /**
     * See if a worker is busy with the given task.
     * 
     * @param taskId
     * @return true if the job is confirmed to be running on the grid, false otherwise
     */
    public static boolean taskIsRunningOnGrid( String taskId ) {
        for ( SpacesRegistrationEntry e : getRegisteredWorkers() ) {
            if ( taskId.equals( e.getTaskId() ) ) return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see ubic.gemma.job.grid.util.SpacesUtil#tasksThatCanBeServiced()
     */
    @Override
    public List<String> tasksThatCanBeServiced() {

        List<String> taskNames = new ArrayList<String>();

        List<SpacesRegistrationEntry> workerEntries = getRegisteredWorkers();

        if ( workerEntries == null ) {
            return taskNames;
        }

        for ( SpacesGenericEntry entry : workerEntries ) {
            String taskName = entry.getMessage();
            log.debug( "Can service task " + taskName + " now." );
            taskNames.add( taskName );
        }

        return taskNames;
    }

    /* (non-Javadoc)
     * @see ubic.gemma.job.grid.util.SpacesUtil#tasksThatCanBeServicedLater()
     */
    @Override
    public List<String> tasksThatCanBeServicedLater() {

        List<String> taskNames = new ArrayList<String>();

        List<SpacesRegistrationEntry> busyEntries = this.getBusyWorkers();
        if ( busyEntries == null ) {
            return taskNames;
        }
        for ( SpacesRegistrationEntry entry : busyEntries ) {
            String taskName = entry.getMessage();
            log.debug( "Can service task " + taskName + " later." );
            taskNames.add( taskName );
        }
        return taskNames;
    }

    /**
     * Determines if the {@link ApplicationContext} contains gigaspaces beans.
     */
    private boolean contextContainsGigaspaces() {
        return applicationContext.containsBean( GIGASPACES_TEMPLATE );
    }
}
