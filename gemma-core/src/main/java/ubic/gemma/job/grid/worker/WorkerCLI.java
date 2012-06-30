/*
 * The Gemma projec
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
package ubic.gemma.job.grid.worker;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.Lease;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.annotation.geommtx.ExpressionExperimentAnnotator;
import ubic.gemma.job.grid.util.SpacesCancellationEntry;
import ubic.gemma.job.grid.util.SpacesUtil;
import ubic.gemma.job.grid.util.SpacesUtilImpl;
import ubic.gemma.util.AbstractSpringAwareCLI;
import ubic.gemma.util.ConfigUtils;

import com.j_spaces.core.IJSpace;
import com.j_spaces.core.client.EntryArrivedRemoteEvent;
import com.j_spaces.core.client.EntryNotInSpaceException;
import com.j_spaces.core.client.ExternalEntry;
import com.j_spaces.core.client.NotifyDelegator;
import com.j_spaces.core.client.NotifyModifiers;

/**
 * Generic tool for starting a remote worker. Multiple types of tasks (business interfaces) can be simultaneously
 * handled by one running Worker, just think about memory requirements if multiple tasks are running at the same time.
 * <p>
 * Workers advertise their existence to the space by sending a short-lived registration entry. This entry also contains
 * the current task id (or null) so it can be used to track which workers are doing what. Worker that have cancelled
 * tasks get restarted.
 * 
 * @author keshav
 * @version $Id$
 */
public class WorkerCLI extends AbstractSpringAwareCLI implements RemoteEventListener {

    /**
     * Gracefully take down a worker.
     */
    public class ShutdownHook extends Thread {
        @Override
        public void run() {
            log.info( "Worker shut down.  Cleaning up registered entries." );

            for ( SpacesCancellationEntry cancellationEntry : cancellationEntries ) {
                try {
                    space.clear( cancellationEntry, null );
                } catch ( Exception e ) {
                    log.warn( "Error clearing worker " + cancellationEntry.message + " from space: " + e.getMessage() );
                }

            }

            for ( SpacesRegistrationEntry registrationEntry : registrationEntries ) {
                try {
                    space.clear( registrationEntry, null );
                } catch ( Exception e ) {
                    log.warn( "Error clearing worker " + registrationEntry.message + " from space: " + e.getMessage() );
                }
            }

        }
    }

    public static void main( String[] args ) {
        WorkerCLI b = new WorkerCLI();
        b.doWork( args );
    }

    /*
     * How often we refresh the status of the worker. Don't make it too infrequent. If the worker dies at the beginning
     * of the cycle, this is how long clients will still see its registration.
     */
    final Long HEARTBEAT_INTERVAL_MILLIS = 15000L;

    /*
     * A little extra time we allow the entry to live, so there is overlap (milliseconds)
     */
    final Long WAIT = 10000L;

    private Collection<SpacesCancellationEntry> cancellationEntries = new HashSet<SpacesCancellationEntry>();

    private Collection<SpacesRegistrationEntry> registrationEntries = new HashSet<SpacesRegistrationEntry>();

    private IJSpace space = null;

    private SpacesUtil spacesUtil;

    private GigaSpacesTemplate template;

    private Map<String, NotifyDelegator> workerCancellationNotifiers = new HashMap<String, NotifyDelegator>();

    private String[] workerNames;

    private Collection<CustomDelegatingWorker> workers = new HashSet<CustomDelegatingWorker>();

    private Map<String, Future<?>> workerThreads = new HashMap<String, Future<?>>();

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractSpringAwareCLI#getShortDesc()
     */
    @Override
    public String getShortDesc() {
        return "Start one or more worker threads for processing jobs sent by the compute grid";
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.jini.core.event.RemoteEventListener#notify(net.jini.core.event.RemoteEvent)
     */
    @Override
    public void notify( RemoteEvent remoteEvent ) {

        try {
            EntryArrivedRemoteEvent arrivedRemoteEvent = ( EntryArrivedRemoteEvent ) remoteEvent;
            ExternalEntry entry = ( ExternalEntry ) arrivedRemoteEvent.getEntry( true );

            Object taskId = entry.getFieldValue( "taskId" );
            Object registrationId = entry.getFieldValue( "registrationId" );
            assert taskId != null;
            assert registrationId != null;

            log.debug( "Event " + entry );

            if ( entry.m_ClassName.equals( SpacesCancellationEntry.class.getName() ) ) {
                for ( CustomDelegatingWorker worker : workers ) {

                    if ( worker.getCurrentTaskId() == null ) continue;

                    if ( registrationId.equals( worker.getRegistrationId() )
                            && taskId.equals( worker.getCurrentTaskId() ) ) {
                        log.info( "Stopping execution of task: " + taskId + " running in " + worker.getRegistrationId() );
                        cancelCurrentJob( worker );
                        return;
                    }
                }
            }

        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option proxyBeanName = OptionBuilder
                .isRequired()
                .hasArg( true )
                .withDescription(
                        "Comma-separated list of worker business interfaces to run (as Spring ProxyBean name, e.g. arrayDesignRepeatScanWorker)" )
                .create( "workers" );
        super.addOption( proxyBeanName );

        Option mmtxOption = OptionBuilder.withDescription( "Set to force MMTX to be initialized" ).create( "mmtx" );
        super.addOption( mmtxOption );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( this.getClass().getName(), args );
        if ( err != null ) {
            return err;
        }

        /*
         * Important to ensure that threads get permissions from their context - not global!
         */
        SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_INHERITABLETHREADLOCAL );

        try {
            init();
        } catch ( Exception e ) {
            log.error( e, e );
        }
        return err;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractSpringAwareCLI#processOptions()
     */
    @Override
    protected void processOptions() {
        super.processOptions();

        if ( this.hasOption( "mmtx" ) ) {
            if ( !ConfigUtils.getBoolean( ExpressionExperimentAnnotator.MMTX_ACTIVATION_PROPERTY_KEY ) ) {
                this.getBean( ExpressionExperimentAnnotator.class ).init();
            }
        }

        String workS = this.getOptionValue( "workers" );
        this.workerNames = workS.split( "," );

        this.spacesUtil = this.getBean( SpacesUtil.class );
        this.ctx = this.spacesUtil.addGemmaSpacesToApplicationContext();

        try {
            this.template = this.getBean( GigaSpacesTemplate.class );
        } catch ( NoSuchBeanDefinitionException e ) {
            throw new RuntimeException( "You must have javaspaces enabled to use the worker cli" );
        }

    }

    /**
     * Stop a worker -- effectively cancelling the job -- and then restart it.
     * 
     * @param worker
     */
    private synchronized void cancelCurrentJob( CustomDelegatingWorker worker ) {
        stop( worker );
        startWorkerThread( worker );
    }

    /**
     * Initializes beans, adds a shutdown hook, adds a notification listener, writes registration entry to the space.
     * 
     * @throws Exception
     */
    private void init() throws Exception {

        space = ( IJSpace ) template.getSpace();
        ShutdownHook shutdownHook = new ShutdownHook();
        Runtime.getRuntime().addShutdownHook( shutdownHook );

        for ( String workerName : workerNames ) {
            startWorker( workerName );
        }

    }

    /**
     * Periodically write a registration entry to the space. The entry times out after a while, so we can tell when
     * workers are no longer alive (and beating)
     * 
     * @param template
     * @param registrationEntry
     * @param workerName
     * @return
     */
    private FutureTask<Object> startHeartbeatThread( final SpacesRegistrationEntry registrationEntry,
            final CustomDelegatingWorker worker, final String workerName ) {

        /*
         * Initialize
         */
        template.write( registrationEntry, HEARTBEAT_INTERVAL_MILLIS + WAIT );

        ExecutorService service = Executors.newSingleThreadExecutor();
        FutureTask<Object> heartBeatTask = new FutureTask<Object>( new Callable<Object>() {
            @Override
            public Object call() throws Exception {

                return workerCheckLoop( registrationEntry, worker, workerName );

            }

        } );
        service.submit( heartBeatTask );
        service.shutdown(); // blocks until all submitted tasks complete

        return heartBeatTask;

    }

    /**
     * Create and start a worker instance.
     * 
     * @param workerName bean name e.g. monitorWorker
     * @throws ClassNotFoundException
     */
    private void startWorker( String workerName ) throws ClassNotFoundException {

        /*
         * In case we need a refresh of connection
         */
        while ( !SpacesUtilImpl.isSpaceRunning() ) {
            log.warn( "No space, waiting for it before trying to start " + workerName );
            try {
                Thread.sleep( 10000 );
            } catch ( InterruptedException e ) {
                log.error( "Bailing out" );
                return;
            }
        }

        this.ctx = spacesUtil.addGemmaSpacesToApplicationContext();
        this.template = this.getBean( GigaSpacesTemplate.class );

        /*
         * Pick up the worker
         */
        CustomDelegatingWorker worker = this.getBean( workerName, CustomDelegatingWorker.class );

        this.workers.add( worker );

        String workerRegistrationId = RandomStringUtils.randomAlphanumeric( 8 ).toUpperCase() + "_" + workerName;
        worker.setRegistrationId( workerRegistrationId );

        /*
         * Start.
         */
        startWorkerThread( worker );

        /*
         * Register
         */
        SpacesRegistrationEntry registrationEntry = new SpacesRegistrationEntry();
        registrationEntry.registrationId = workerRegistrationId;
        registrationEntry.message = workerName;
        startHeartbeatThread( registrationEntry, worker, workerName );

        /* register this as a listener for cancellation entries */
        SpacesCancellationEntry cancellationEntry = new SpacesCancellationEntry();
        cancellationEntry.registrationId = workerRegistrationId;
        NotifyDelegator cancellationNotifier = template.addNotifyDelegatorListener( this, cancellationEntry, null,
                false, Lease.FOREVER, NotifyModifiers.NOTIFY_ALL );
        /*
         * Currently this is not used. The idea is to clean up, but we probably don't need to - even after a
         * cancel-restart cycle, this notifier can still be used. You will see one
         * "Daemon Thread [NotifyDelegator:FifoDelegatorThread]" in your debugger for each worker you start, but this
         * should not grow as the application runs.
         */
        workerCancellationNotifiers.put( workerRegistrationId, cancellationNotifier );

        log.info( registrationEntry.message + " registered with space " + template.getUrl() + " [WorkerId="
                + workerRegistrationId + "]" );
    }

    /**
     * Start or restart a worker.
     * 
     * @param worker
     */
    private void startWorkerThread( CustomDelegatingWorker worker ) {
        String workerRegistrationId = worker.getRegistrationId();

        assert !workerThreads.containsKey( workerRegistrationId ) || workerThreads.get( workerRegistrationId ).isDone();

        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<?> future = service.submit( worker );
        workerThreads.put( workerRegistrationId, future );

        service.shutdown();
    }

    /**
     * Abort (cancel) a worker. It can be restarted.
     * 
     * @param worker
     */
    private void stop( CustomDelegatingWorker worker ) {
        Future<?> futureTask = this.workerThreads.get( worker.getRegistrationId() );
        if ( !futureTask.isDone() ) {
            boolean cancelled = futureTask.cancel( true );
            if ( cancelled ) {
                log.info( worker.getRegistrationId() + " cancelled:" + worker.getCurrentTaskId() );

                this.workerThreads.remove( worker.getRegistrationId() );
            } else {
                log.info( worker.getRegistrationId() + " not cancelled: " + worker.getCurrentTaskId() );
            }
        } else {
            log.info( worker.getRegistrationId() + " was already done with " + worker.getCurrentTaskId() );
            this.workerThreads.remove( worker.getRegistrationId() );
        }
    }

    /**
     * Periodically refresh the lease on the registration entry, and restart the worker if possible when things go
     * wrong. Note that I belatedly realized that this overlaps with functionality provided by Jini
     * (http://www.gigaspaces.com/docs/JiniApi/net/jini/lease/LeaseRenewalManager.html), but that doesn't handle the
     * worker restart. Switching to LeaseRenewalManager in the future might be a good idea if the worker restart issue
     * can be handled.
     * 
     * @param registrationEntry
     * @param worker
     * @param workerName
     * @return
     * @throws InterruptedException
     * @throws ClassNotFoundException
     */
    private Object workerCheckLoop( final SpacesRegistrationEntry registrationEntry,
            final CustomDelegatingWorker worker, String workerName ) throws InterruptedException,
            ClassNotFoundException {
        log.info( "Starting heartbeat for " + registrationEntry.registrationId );
        while ( true ) {

            try {
                Thread.sleep( HEARTBEAT_INTERVAL_MILLIS );
                if ( log.isDebugEnabled() )
                    log.debug( "Refresh " + registrationEntry.message + " " + registrationEntry.registrationId );

                /*
                 * Note: we fill in the taskid so we can tell the worker is 'busy'. This gets cleared out by
                 * HEARTBEAT_INTERVAL_MILLIS milliseconds of the job finishing. So HEARTBEAT_INTERVAL_MILLIS should be
                 * not too long. However, not that this has no impact on whether the worker is actually busy and whether
                 * it will take jobs immediately or not.
                 */
                registrationEntry.taskId = worker.getCurrentTaskId();

                if ( this.workerThreads.get( worker.getRegistrationId() ).isDone() ) {
                    log.warn( "Worker " + worker.getRegistrationId() + " is dead, trying to restart" );
                    this.workers.remove( worker );
                    startWorker( workerName );
                    return null;
                }

                // Refresh the TTL for the entry and continue.
                template.update( registrationEntry, HEARTBEAT_INTERVAL_MILLIS + WAIT, 500 /* ifexists timeout */);
            } catch ( RemoteAccessException e ) {
                if ( e.getCause() instanceof EntryNotInSpaceException ) {

                    /*
                     * If we're out of memory, we're in trouble.
                     */

                    long freeBytes = Runtime.getRuntime().freeMemory()
                            + ( Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() );
                    log.info( String.format( "%.2f Mbytes memory remaining", ( double ) freeBytes / ( 1024 * 1024 ) ) );

                    if ( freeBytes < 1024L * 1024 * ConfigUtils.getInt( "gemma.grid.minworkermemory" ) ) {
                        log.fatal( "Insufficient memory left (" + freeBytes + " bytes), panicking " );
                        System.exit( 1 ); // ???
                    }
                    /*
                     * Maybe we need more overlap...
                     */
                    log.warn( "Entry expired but worker is still alive, renewing; "
                            + String.format( "%.2f Mbytes memory remaining", freeBytes / ( 1024 * 1024 ) ) );

                    template.write( registrationEntry, HEARTBEAT_INTERVAL_MILLIS + WAIT );

                } else {

                    /*
                     * Try to reconnect to the space.
                     */

                    if ( !SpacesUtilImpl.isSpaceRunning() ) {
                        log.info( "Space is gone. Will try to restart the worker when it comes back." );
                    }

                    while ( !SpacesUtilImpl.isSpaceRunning() ) {
                        Thread.sleep( 10000 ); // could have maxretries.
                        log.info( "Still no space, waiting  ..." );
                    }

                    /*
                     * When this happens, the worker has died as well (always ?)
                     */
                    if ( this.workerThreads.get( worker.getRegistrationId() ).isDone() ) {
                        log.warn( "Worker " + worker.getRegistrationId() + " is dead, trying to restart" );
                        this.workers.remove( worker );
                        startWorker( workerName );
                        return null;
                    }
                    throw new IllegalStateException( "I wasn't expecting the worker to still be alive." );

                }

            } catch ( Exception e ) {
                log.error( "Worker: " + worker.getRegistrationId() + " heartbeat error, stopping worker", e );
                worker.stop();
                this.workers.remove( worker );
                return null; // possibly restart?
            } finally {
                // .. maybe nothing to do here.
            }
        }

    }
}
