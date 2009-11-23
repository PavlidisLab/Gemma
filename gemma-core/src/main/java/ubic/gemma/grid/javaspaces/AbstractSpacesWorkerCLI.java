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
package ubic.gemma.grid.javaspaces;

import java.rmi.RemoteException;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import net.jini.core.lease.Lease;

import org.apache.commons.lang.math.RandomUtils;
import org.springframework.context.ApplicationContext;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.grid.javaspaces.util.SpacesEnum;
import ubic.gemma.grid.javaspaces.util.SpacesUtil;
import ubic.gemma.grid.javaspaces.util.entry.SpacesBusyEntry;
import ubic.gemma.grid.javaspaces.util.entry.SpacesCancellationEntry;
import ubic.gemma.grid.javaspaces.util.entry.SpacesRegistrationEntry;
import ubic.gemma.util.AbstractSpringAwareCLI;

import com.j_spaces.core.IJSpace;
import com.j_spaces.core.client.EntryArrivedRemoteEvent;
import com.j_spaces.core.client.ExternalEntry;
import com.j_spaces.core.client.NotifyModifiers;

/**
 * @author keshav
 * @version $Id$
 */
public abstract class AbstractSpacesWorkerCLI extends AbstractSpringAwareCLI implements RemoteEventListener {

    /**
     * A worker shutdown hook.
     * 
     * @author keshav
     */
    public class ShutdownHook extends Thread {
        @Override
        public void run() {
            log.info( "Worker shut down.  Running shutdown hook ... cleaning up registered entries for this worker." );
            if ( space != null ) {
                try {
                    space.clear( registrationEntry, null );
                    space.clear( busyEntry, null );
                } catch ( Exception e ) {

                    log.error( "Error clearing the generic entry " + registrationEntry + "for task "
                            + registrationEntry.message + "from space." );
                    e.printStackTrace();
                }
            }
        }
    }

    protected SpacesBusyEntry busyEntry = null;

    protected SpacesCancellationEntry cancellationEntry = null;

    protected Thread itbThread;

    protected SpacesRegistrationEntry registrationEntry = null;

    protected IJSpace space = null;

    protected SpacesUtil spacesUtil = null;

    protected GigaSpacesTemplate template;

    protected ApplicationContext updatedContext = null;

    protected CustomDelegatingWorker worker;

    protected Long workerRegistrationId = null;

    /*
     * (non-Javadoc)
     * @see net.jini.core.event.RemoteEventListener#notify(net.jini.core.event.RemoteEvent)
     */
    public void notify( RemoteEvent remoteEvent ) throws UnknownEventException, RemoteException {
        log.debug( "Worker received notification: " + remoteEvent );

        try {
            EntryArrivedRemoteEvent arrivedRemoteEvent = ( EntryArrivedRemoteEvent ) remoteEvent;
            ExternalEntry entry = ( ExternalEntry ) arrivedRemoteEvent.getEntry( true );
            Object taskId = entry.getFieldValue( "taskId" );

            if ( taskId.equals( worker.getTaskId() ) ) {
                log.info( "Stopping execution of task: " + taskId );
                log.debug( itbThread.getState() );
                itbThread.stop();
            }

        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @Override
    protected void buildOptions() {
        // no-op
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
        try {
            preInit();
            init();
            start();
        } catch ( Exception e ) {
            log.error( "transError problem..." + e.getMessage() );
            e.printStackTrace();
        }
        return err;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
    }

    /**
     * Sets the task for the registration entry. This should be done by setting
     * registrationEntry.message=YourTask.class.getName()
     * 
     * @throws Exception
     */
    protected abstract void setRegistrationEntryTask() throws Exception;

    /**
     * Sets the worker to be used. The worker is a {@link CustomDelegatingWorker} which is wired with a task in the bean
     * factory.
     */
    protected abstract void setWorker();

    /**
     * Starts the thread for this worker.
     */
    protected abstract void start();

    /**
     * Initializes beans, adds a shutdown hook, adds a notification listener, writes registration entry to the space.
     * 
     * @throws Exception
     */
    private void init() throws Exception {
        /* register the shutdown hook so cleanup occurs even if VM is incorrectly terminated */
        ShutdownHook shutdownHook = new ShutdownHook();
        Runtime.getRuntime().addShutdownHook( shutdownHook );

        if ( !updatedContext.containsBean( "gigaspacesTemplate" ) )
            throw new RuntimeException( "GemmaSpaces beans could not be loaded. Cannot start worker." );

        /* set up the registration entry for this worker */
        registrationEntry = new SpacesRegistrationEntry();
        setRegistrationEntryTask();
        workerRegistrationId = RandomUtils.nextLong();
        registrationEntry.registrationId = workerRegistrationId;
        worker.setGemmaSpacesRegistrationEntry( registrationEntry );

        /* set up the busy entry for this worker */
        busyEntry = new SpacesBusyEntry();
        busyEntry.message = registrationEntry.message;
        busyEntry.registrationId = registrationEntry.registrationId;
        worker.setGemmaSpacesBusyEntry( busyEntry );

        /* set up the cancellation entry for this worker */
        cancellationEntry = new SpacesCancellationEntry();
        // FIXME The cancellationEnty is not being used at the moment since there is no "cancel" button in the extjs
        // user interface (my gemma). When wiring this into the front end, you'll have to expose a cancel method in the
        // AbstractSpacesController that first calls spacesUtil.cancel(taskId) and then
        // taskRunningService.cancel(taskId);

        template = ( GigaSpacesTemplate ) updatedContext.getBean( "gigaspacesTemplate" );

        template.addNotifyDelegatorListener( this, cancellationEntry, null, true, Lease.FOREVER,
                NotifyModifiers.NOTIFY_ALL );

        space = ( IJSpace ) template.getSpace();

        Lease lease = space.write( registrationEntry, null, SpacesUtil.VERY_BIG_NUMBER_FOR_SOME_REASON );
        log.info( this.getClass().getSimpleName() + " registered with space " + template.getUrl() );
        if ( lease == null ) log.error( "Null Lease returned" );

    }

    /**
     * Initializes beans.
     * 
     * @throws Exception
     */
    private void preInit() throws Exception {
        spacesUtil = ( SpacesUtil ) this.getBean( "spacesUtil" );

        updatedContext = spacesUtil.addGemmaSpacesToApplicationContext( SpacesEnum.DEFAULT_SPACE.getSpaceUrl() );
        setWorker();
    }
}
