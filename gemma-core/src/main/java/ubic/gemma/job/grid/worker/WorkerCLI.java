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

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import net.jini.core.lease.Lease;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.job.grid.util.SpacesCancellationEntry;
import ubic.gemma.util.AbstractSpringAwareCLI;

import com.j_spaces.core.IJSpace;
import com.j_spaces.core.client.EntryArrivedRemoteEvent;
import com.j_spaces.core.client.ExternalEntry;
import com.j_spaces.core.client.NotifyModifiers;

/**
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
     * (non-Javadoc)
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */

    private Collection<SpacesCancellationEntry> cancellationEntries = new HashSet<SpacesCancellationEntry>();

    private Collection<SpacesRegistrationEntry> registrationEntries = new HashSet<SpacesRegistrationEntry>();

    private IJSpace space = null;

    private Collection<CustomDelegatingWorker> workers = new HashSet<CustomDelegatingWorker>();

    private String[] workerNames;

    /*
     * (non-Javadoc)
     * @see net.jini.core.event.RemoteEventListener#notify(net.jini.core.event.RemoteEvent)
     */
    public void notify( RemoteEvent remoteEvent ) throws UnknownEventException, RemoteException {

        try {
            EntryArrivedRemoteEvent arrivedRemoteEvent = ( EntryArrivedRemoteEvent ) remoteEvent;
            ExternalEntry entry = ( ExternalEntry ) arrivedRemoteEvent.getEntry( true );
            Object taskId = entry.getFieldValue( "taskId" );
            assert taskId != null;

            /*
             * Cancellation. NOTE this assumes that the only notifications we get are about cancellations.
             */
            if ( entry.m_ClassName.equals( SpacesCancellationEntry.class.getName() ) ) {
                for ( CustomDelegatingWorker worker : workers ) {
                    assert worker != null;
                    if ( taskId.equals( worker.getCurrentTaskId() ) ) {
                        log.info( "Stopping execution of task: " + taskId );
                        /*
                         * FIXME this kills the worker! Can't we just abort the current call?
                         */
                        worker.stop();
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

    }

    /*
     * (non-Javadoc)
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
     * @see ubic.gemma.util.AbstractSpringAwareCLI#processOptions()
     */
    @Override
    protected void processOptions() {
        this.setForceGigaSpacesOn( true );
        super.processOptions();

        String workS = this.getOptionValue( "workers" );
        this.workerNames = workS.split( "," );
    }

    /**
     * Initializes beans, adds a shutdown hook, adds a notification listener, writes registration entry to the space.
     * 
     * @throws Exception
     */
    private void init() throws Exception {
        GigaSpacesTemplate template = ( GigaSpacesTemplate ) this.getBean( "gigaspacesTemplate" );
        space = ( IJSpace ) template.getSpace();

        for ( String workerName : workerNames ) {

            ShutdownHook shutdownHook = new ShutdownHook();
            Runtime.getRuntime().addShutdownHook( shutdownHook );

            /*
             * Pick up the worker
             */

            CustomDelegatingWorker worker = ( CustomDelegatingWorker ) this.getBean( workerName );

            this.workers.add( worker );

            String workerRegistrationId = RandomStringUtils.randomAlphanumeric( 8 ).toUpperCase() + "_" + workerName;
            worker.setRegistrationId( workerRegistrationId );

            /*
             * Register
             */
            SpacesRegistrationEntry registrationEntry = new SpacesRegistrationEntry();
            registrationEntry.registrationId = workerRegistrationId;
            registrationEntry.message = workerName;

            /* register this as a listener for cancellation entries */
            SpacesCancellationEntry cancellationEntry = new SpacesCancellationEntry();
            cancellationEntry.registrationId = workerRegistrationId;
            template.addNotifyDelegatorListener( this, cancellationEntry, null, true, Lease.FOREVER,
                    NotifyModifiers.NOTIFY_ALL );

            /*
             * Start.
             */
            Thread itbThread = new Thread( worker );
            itbThread.start();

            startHeartbeatThread( template, registrationEntry, worker );

            log.info( registrationEntry.message + " registered with space " + template.getUrl() + "[WorkerId="
                    + workerRegistrationId + "]" );
        }

    }

    /**
     * Periodically write a registration entry to the space. The entry times out after a while, so we can tell when
     * workers are no longer alive (and beating)
     * 
     * @param template
     * @param registrationEntry
     */
    private void startHeartbeatThread( final GigaSpacesTemplate template,
            final SpacesRegistrationEntry registrationEntry, final CustomDelegatingWorker worker ) {

        /*
         * How often we refresh the status of the worker. Don't make it too infrequent. If the worker dies at the
         * beginning of the cycle, this is how long clients will still see its registration.
         */
        final Long HEARTBEAT_INTERVAL_MILLIS = 2000L;

        /*
         * A little extra time we allow the entry to live, so there is overlap
         */
        final Long WAIT = 100L;

        /*
         * Initialize
         */
        template.write( registrationEntry, HEARTBEAT_INTERVAL_MILLIS + WAIT );

        ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit( new FutureTask<Object>( new Callable<Object>() {
            public Object call() throws Exception {

                while ( true ) {
                    Thread.sleep( HEARTBEAT_INTERVAL_MILLIS );
                    if ( log.isDebugEnabled() )
                        log.debug( "Refresh " + registrationEntry.message + " " + registrationEntry.registrationId );

                    /*
                     * Note: we fill in the taskid so we can tell the worker is 'busy'. This gets cleared out by
                     * HEARTBEAT_INTERVAL_MILLIS milliseconds of the job finishing. So HEARTBEAT_INTERVAL_MILLIS should
                     * be not too long. However, not that this has no impact on whether the worker is actually busy and
                     * whether it will take jobs immediately or not.
                     */
                    registrationEntry.taskId = worker.getCurrentTaskId();

                    template.update( registrationEntry, HEARTBEAT_INTERVAL_MILLIS + WAIT, 100 );
                }

            }
        } ) );

        service.shutdown();

    }
}
