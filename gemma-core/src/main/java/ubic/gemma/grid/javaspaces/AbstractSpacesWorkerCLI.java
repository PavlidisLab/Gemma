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

import ubic.gemma.util.AbstractSpringAwareCLI;
import ubic.gemma.util.grid.javaspaces.SpacesEnum;
import ubic.gemma.util.grid.javaspaces.SpacesUtil;
import ubic.gemma.util.grid.javaspaces.entry.SpacesCancellationEntry;
import ubic.gemma.util.grid.javaspaces.entry.SpacesRegistrationEntry;

import com.j_spaces.core.IJSpace;
import com.j_spaces.core.client.EntryArrivedRemoteEvent;
import com.j_spaces.core.client.ExternalEntry;
import com.j_spaces.core.client.NotifyModifiers;

/**
 * @author keshav
 * @version $Id$
 */
public abstract class AbstractSpacesWorkerCLI extends AbstractSpringAwareCLI implements RemoteEventListener {

    protected GigaSpacesTemplate template;

    protected CustomDelegatingWorker worker;

    protected Thread itbThread;

    protected IJSpace space = null;

    protected SpacesRegistrationEntry registrationEntry = null;

    protected Long workerRegistrationId = null;

    protected ApplicationContext updatedContext = null;

    protected SpacesUtil spacesUtil = null;

    /**
     * Sets the worker to be used. The worker is a {@link CustomDelegatingWorker} which is wired with a task in the bean
     * factory.
     */
    protected abstract void setWorker();

    /**
     * Sets the task for the registration entry. This should be done by setting
     * registrationEntry.message=YourTask.class.getName()
     * 
     * @throws Exception
     */
    protected abstract void setRegistrationEntryTask() throws Exception;

    /**
     * Starts the thread for this worker.
     */
    protected abstract void start();

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

        template = ( GigaSpacesTemplate ) updatedContext.getBean( "gigaspacesTemplate" );

        template.addNotifyDelegatorListener( this, new SpacesCancellationEntry(), null, true, Lease.FOREVER,
                NotifyModifiers.NOTIFY_ALL );

        space = ( IJSpace ) template.getSpace();

        registrationEntry = new SpacesRegistrationEntry();
        setRegistrationEntryTask();
        workerRegistrationId = RandomUtils.nextLong();
        registrationEntry.registrationId = workerRegistrationId;
        worker.setGemmaSpacesRegistrationEntry( registrationEntry );
        Lease lease = space.write( registrationEntry, null, 600000000 );
        log.info( this.getClass().getSimpleName() + " registered with space " + template.getUrl() );
        if ( lease == null ) log.error( "Null Lease returned" );

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
                } catch ( Exception e ) {

                    log.error( "Error clearing the generic entry " + registrationEntry + "for task "
                            + registrationEntry.message + "from space." );
                    e.printStackTrace();
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see net.jini.core.event.RemoteEventListener#notify(net.jini.core.event.RemoteEvent)
     */
    public void notify( RemoteEvent remoteEvent ) throws UnknownEventException, RemoteException {
        log.info( "notified ..." );

        try {
            EntryArrivedRemoteEvent arrivedRemoteEvent = ( EntryArrivedRemoteEvent ) remoteEvent;

            log.debug( "event: " + arrivedRemoteEvent );
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

    @Override
    protected void processOptions() {
        super.processOptions();
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @Override
    protected void buildOptions() {
        // no-op
    }
}
